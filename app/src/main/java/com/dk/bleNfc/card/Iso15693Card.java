package com.dk.bleNfc.card;

import com.dk.bleNfc.DeviceManager.DeviceManager;
import com.dk.bleNfc.Exception.CardNoResponseException;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by Administrator on 2016/9/21.
 */
public class Iso15693Card extends Card{
    public final static int  LONG_READ_MAX_NUMBER = 0x15;

    public onReceiveReadListener mOnReceiveReadListener;
    public onReceiveReadMultipleListener mOnReceiveReadMultipleListener;
    public onReceiveWriteListener mOnReceiveWriteListener;
    public onReceiveLockBlockListener mOnReceiveLockBlockListener;
    public onReceiveCmdListener mOnReceiveCmdListener;
    public onReceiveWriteMultipleListener mOnReceiveWriteMultipleListener;
    public onReceiveScheduleListener mOnReceiveReadScheduleListener;

    public Iso15693Card(DeviceManager deviceManager) {
        super(deviceManager);
    }

    public Iso15693Card(DeviceManager deviceManager, byte[] uid, byte[] atr) {
        super(deviceManager, uid, atr);
    }

    //ISO15693读单个块数据回调接口
    public interface onReceiveReadListener {
        public void onReceiveRead(boolean isSuc, byte[] returnBytes);
    }

    //ISO15693读多个块数据回调接口
    public interface onReceiveReadMultipleListener{
        void onReceiveReadMultiple(boolean isSuc, byte[] returnData);
    }

    //ISO15693写一个块数据接口
    public interface onReceiveWriteListener {
        public void onReceiveWrite(boolean isSuc);
    }

    //ISO15693写多个块函数接口
    public interface onReceiveWriteMultipleListener {
        void onReceiveWriteMultiple(boolean isSuc);
    }

    //ISO15693锁住一个块回调接口
    public interface onReceiveLockBlockListener{
        void onReceiveLockBlock(boolean isSuc);
    }

    //ISO1569指令通到调到接口
    public interface onReceiveCmdListener{
        void onReceiveCmd(boolean isSuc, byte returnData[]);
    }

    //读写进度回调
    public interface onReceiveScheduleListener{
        void onReceiveSchedule(int rate);
    }

    //ISO15693读单个块数据指令
    //addr：要读的块地址
    public void read(byte addr, onReceiveReadListener l) {
        mOnReceiveReadListener = l;
        deviceManager.requestRfmIso15693ReadSingleBlock(uid, addr, new DeviceManager.onReceiveRfIso15693ReadSingleBlockListener() {
            @Override
            public void onReceiveRfIso15693ReadSingleBlock(boolean isSuc, byte[] returnData) {
                if (returnData.length < 5) {
                    if (mOnReceiveReadListener != null) {
                        mOnReceiveReadListener.onReceiveRead(false, returnData);
                    }
                }
                else {
                    if (mOnReceiveReadListener != null) {
                        byte[] readData = new byte[4];
                        System.arraycopy(returnData, 1, readData, 0, 4);
                        mOnReceiveReadListener.onReceiveRead(isSuc, readData);
                    }
                }
            }
        });
    }

    //ISO15693读多个块数据指令
    //addr：要读的块地址
    //number:要读的块数量,必须大于0
    public void ReadMultiple(byte addr, byte number, onReceiveReadMultipleListener l) {
        mOnReceiveReadMultipleListener = l;
        deviceManager.requestRfmIso15693ReadMultipleBlock(uid, addr, number, new DeviceManager.onRecevieRfIso15693ReadMultipleBlockListener() {
            @Override
            public void onRecevieRfIso15693ReadMultipleBlock(boolean isSuc, byte[] returnData) {
                if ((returnData == null) || (returnData.length < 5)) {
                    if (mOnReceiveReadMultipleListener != null) {
                        mOnReceiveReadMultipleListener.onReceiveReadMultiple(false, returnData);
                    }
                }
                else {
                    if (mOnReceiveReadMultipleListener != null) {
                        byte[] readData = new byte[returnData.length - 1];
                        System.arraycopy(returnData, 1, readData, 0, readData.length);
                        mOnReceiveReadMultipleListener.onReceiveReadMultiple(isSuc, readData);
                    }
                }
            }
        });
    }

    //ISO15693写一个块
    //addr：要写卡片的块地址
    //writeData:要写的数据，必须4个字节
    public void write(byte addr, byte writeData[], onReceiveWriteListener l) {
        mOnReceiveWriteListener = l;
        deviceManager.requestRfmIso15693WriteSingleBlock(uid, addr, writeData, new DeviceManager.onReceiveRfIso15693WriteSingleBlockListener() {
            @Override
            public void onReceiveRfIso15693WriteSingleBlock(boolean isSuc) {
                if (mOnReceiveWriteListener != null) {
                    mOnReceiveWriteListener.onReceiveWrite(isSuc);
                }
            }
        });
    }

    //ISO15693写多个块
    //addr：要写的块地址
    //number:要写的块数量,必须大于0
    //writeData: 要写的数据，必须(number+1) * 4字节
    public void writeMultiple(byte addr, byte number, byte writeData[], onReceiveWriteMultipleListener l) {
        mOnReceiveWriteMultipleListener = l;
        deviceManager.requestRfmIso15693WriteMultipleBlock(this.uid, addr, number, writeData, new DeviceManager.onReceiveRfIso15693WriteMultipleBlockListener() {
            @Override
            public void onReceiveRfIso15693WriteMultipleBlock(boolean isSuc) {
                if (mOnReceiveWriteMultipleListener != null) {
                    mOnReceiveWriteMultipleListener.onReceiveWriteMultiple(isSuc);
                }
            }
        });
    }

    //ISO15693锁住一个块
    //addr：要锁住的块地址
    public void lockBlock(byte addr, onReceiveLockBlockListener l) {
        mOnReceiveLockBlockListener = l;
        deviceManager.requestRfmIso15693LockBlock(uid, addr, new DeviceManager.onReceiveRfIso15693LockBlockListener() {
            @Override
            public void onReceiveRfIso15693LockBlock(boolean isSuc) {
                if (mOnReceiveLockBlockListener != null) {
                    mOnReceiveLockBlockListener.onReceiveLockBlock(isSuc);
                }
            }
        });
    }

    /**
     * ISO15693锁住一个块，同步阻塞方式，注意：不能在蓝牙初始化的线程里运行
     * @param addr        要锁的块的起始地址
     * @return            true:写入成功   false：写入失败
     * @throws CardNoResponseException
     *                  操作无响应时会抛出异常
     */
    public boolean lockBlock(byte addr) throws CardNoResponseException {
        final byte[][] returnBytes = new byte[1][1];
        final boolean[] isCmdRunSucFlag = {false};

        final Semaphore semaphore = new Semaphore(0);

        lockBlock(addr, new onReceiveLockBlockListener() {
            @Override
            public void onReceiveLockBlock(boolean isCmdRunSuc) {
                if (isCmdRunSuc) {
                    isCmdRunSucFlag[0] = true;
                }
                else {
                    isCmdRunSucFlag[0] = false;
                }
                semaphore.release();
            }
        });

        try {
            semaphore.tryAcquire(CAR_NO_RESPONSE_TIME_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new CardNoResponseException(CAR_NO_RESPONSE);
        }

        return isCmdRunSucFlag[0];
    }

    //ISO15693指令通道
    public void cmd(byte cmdBytes[], onReceiveCmdListener l) {
        mOnReceiveCmdListener = l;
        deviceManager.requestRfmIso15693CmdBytes(cmdBytes, new DeviceManager.onReceiveRfIso15693CmdListener() {
            @Override
            public void onReceiveRfIso15693Cmd(boolean isSuc, byte[] returnData) {
                if (mOnReceiveCmdListener != null) {
                    mOnReceiveCmdListener.onReceiveCmd(isSuc, returnData);
                }
            }
        });
    }

    /**
     * ISO15693读单个块数据，同步阻塞方式，注意：不能在蓝牙初始化的线程里运行
     * @param addr     要读的地址
     * @return         读取到的数据
     * @throws CardNoResponseException
     *                  操作无响应时会抛出异常
     */
    public byte[] read(byte addr) throws CardNoResponseException {
        final byte[][] returnBytes = new byte[1][1];
        final boolean[] isCmdRunSucFlag = {false};

        final Semaphore semaphore = new Semaphore(0);
        returnBytes[0] = null;

        read(addr, new onReceiveReadListener() {
            @Override
            public void onReceiveRead(boolean isCmdRunSuc, byte[] bytApduRtnData) {
                if (isCmdRunSuc) {
                    returnBytes[0] = bytApduRtnData;
                    isCmdRunSucFlag[0] = true;
                }
                else {
                    returnBytes[0] = null;
                    isCmdRunSucFlag[0] = false;
                }
                semaphore.release();
            }
        });

        try {
            semaphore.tryAcquire(CAR_NO_RESPONSE_TIME_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new CardNoResponseException(CAR_NO_RESPONSE);
        }
        if (!isCmdRunSucFlag[0]) {
            throw new CardNoResponseException(CAR_RUN_CMD_FAIL);
        }
        return returnBytes[0];
    }

    /**
     * 快速读，同步阻塞方式，注意：不能在蓝牙初始化的线程里运行
     * @param startAddress     要读起始地址
     * @param number           要读的块数量（一个块4 byte）， 0 < number < 0x3f
     * @return                 读取到的数据
     * @throws CardNoResponseException
     *                  操作无响应时会抛出异常
     */
    public byte[] longReadSingle(byte startAddress, int number) throws CardNoResponseException {
        final byte[][] returnBytes = new byte[1][1];
        final boolean[] isCmdRunSucFlag = {false};

        final Semaphore semaphore = new Semaphore(0);
        returnBytes[0] = null;

        deviceManager.requestRfmIso15693LongRead(startAddress, number, new DeviceManager.onReceiveIso15693LongReadListener() {
            @Override
            public void onReceiveIso15693LongRead(boolean isCmdRunSuc, byte[] bytApduRtnData) {
                if (isCmdRunSuc) {
                    returnBytes[0] = bytApduRtnData;
                    isCmdRunSucFlag[0] = true;
                }
                else {
                    returnBytes[0] = null;
                    isCmdRunSucFlag[0] = false;
                }
                semaphore.release();
            }
        });

        try {
            semaphore.tryAcquire(CAR_NO_RESPONSE_TIME_MS * 5, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new CardNoResponseException(CAR_NO_RESPONSE);
        }
        if (!isCmdRunSucFlag[0]) {
            throw new CardNoResponseException(CAR_RUN_CMD_FAIL);
        }
        return returnBytes[0];
    }

    /**
     * 任意长度读，同步阻塞方式，注意：不能在蓝牙初始化的线程里运行
     * @param startAddress 要读取的起始地址
     * @param endAddress 要读取的结束地址
     * @return         读取到的数据
     * @throws CardNoResponseException
     *                  卡片无响应时会抛出异常
     */
    public byte[] longRead(byte startAddress, byte endAddress) throws CardNoResponseException {
        if ( (startAddress & 0x00ff) > (endAddress & 0x00ff) ) {
            throw new CardNoResponseException("Start Address must be smaller than end Address");
        }

        //获取一个块的长度
        byte[] bytes = longReadSingle((byte) 0, 1);
        if (bytes == null) {
            return null;
        }

        byte[] readDataBytes = new byte[((endAddress & 0x00ff) - (startAddress & 0x00ff) + 1) * bytes.length];
        int readDataLen = 0;

        int currentStartAddress = startAddress & 0x00ff;
        int currentEndAddress = currentStartAddress + LONG_READ_MAX_NUMBER - 1;
        byte[] returnBytes;

        if ( ((endAddress & 0x00ff) - (startAddress & 0x00ff) + 1) >=  LONG_READ_MAX_NUMBER) {
            while ((currentEndAddress & 0x00ff) <= (endAddress & 0x00ff)) {
                returnBytes = longReadSingle((byte) currentStartAddress, LONG_READ_MAX_NUMBER);
                System.arraycopy(returnBytes, 0, readDataBytes, readDataLen, returnBytes.length);
                readDataLen += LONG_READ_MAX_NUMBER * bytes.length;
                currentStartAddress = (currentEndAddress & 0x00ff) + 1;
                currentEndAddress += LONG_READ_MAX_NUMBER;
            }
        }

        int surplusBlock = ((endAddress & 0x00ff) - (startAddress & 0x00ff) + 1) % LONG_READ_MAX_NUMBER;
        if ( surplusBlock != 0 ) {
            returnBytes = longReadSingle((byte)(currentStartAddress & 0x00ff), surplusBlock);
            System.arraycopy(returnBytes, 0, readDataBytes, readDataLen, surplusBlock * bytes.length);
        }
        return readDataBytes;
    }

    /**
     * 任意长度读，带读进度回调，同步阻塞方式，注意：不能在蓝牙初始化的线程里运行
     * @param startAddress 要读取的起始地址
     * @param endAddress 要读取的结束地址
     * @param listener 读取过程中会不断回调读取进度
     * @return         读取到的数据
     * @throws CardNoResponseException
     *                  卡片无响应时会抛出异常
     */
    public byte[] longReadWithScheduleCallback(byte startAddress, byte endAddress, onReceiveScheduleListener listener) throws CardNoResponseException {
        mOnReceiveReadScheduleListener = listener;

        if ( (startAddress & 0x00ff) > (endAddress & 0x00ff) ) {
            throw new CardNoResponseException("Start Address must be smaller than end Address");
        }

        //获取一个块的长度
        byte[] bytes = longReadSingle((byte) 0, 1);
        if (bytes == null) {
            return null;
        }

        byte[] readDataBytes = new byte[((endAddress & 0x00ff) - (startAddress & 0x00ff) + 1) * bytes.length];
        int readDataLen = 0;

        int currentStartAddress = startAddress & 0x00ff;
        int currentEndAddress = currentStartAddress + LONG_READ_MAX_NUMBER - 1;
        byte[] returnBytes;

        if ( ((endAddress & 0x00ff) - (startAddress & 0x00ff) + 1) >=  LONG_READ_MAX_NUMBER) {
            while ((currentEndAddress & 0x00ff) <= (endAddress & 0x00ff)) {
                returnBytes = longReadSingle((byte) currentStartAddress, LONG_READ_MAX_NUMBER);
                System.arraycopy(returnBytes, 0, readDataBytes, readDataLen, returnBytes.length);
                readDataLen += LONG_READ_MAX_NUMBER * bytes.length;
                currentStartAddress = (currentEndAddress & 0x00ff) + 1;
                currentEndAddress += LONG_READ_MAX_NUMBER;

                //回调读取进度
                if (mOnReceiveReadScheduleListener != null) {
                    if ((readDataLen != 0) && (readDataLen < readDataBytes.length)) {
                        mOnReceiveReadScheduleListener.onReceiveSchedule(readDataLen * 100 / readDataBytes.length);
                    }
                }
            }
        }

        int surplusBlock = ((endAddress & 0x00ff) - (startAddress & 0x00ff) + 1) % LONG_READ_MAX_NUMBER;
        if ( surplusBlock != 0 ) {
            returnBytes = longReadSingle((byte)(currentStartAddress & 0x00ff), surplusBlock);
            System.arraycopy(returnBytes, 0, readDataBytes, readDataLen, surplusBlock * bytes.length);
        }

        //回调读取进度
        if (mOnReceiveReadScheduleListener != null) {
            mOnReceiveReadScheduleListener.onReceiveSchedule(100);
        }
        return readDataBytes;
    }

    /**
     * ISO15693读多个块数据指令，同步阻塞方式，注意：不能在蓝牙初始化的线程里运行
     * @param addr     要读的块的起始地址
     * @param number   要读块的数量,必须大于0
     * @return         读取到的数据
     * @throws CardNoResponseException
     *                  操作无响应时会抛出异常
     */
    public byte[] ReadMultiple(byte addr, byte number) throws CardNoResponseException {
        final byte[][] returnBytes = new byte[1][1];
        final boolean[] isCmdRunSucFlag = {false};

        final Semaphore semaphore = new Semaphore(0);
        returnBytes[0] = null;

        ReadMultiple(addr, number, new onReceiveReadMultipleListener() {
            @Override
            public void onReceiveReadMultiple(boolean isCmdRunSuc, byte[] bytApduRtnData) {
                if (isCmdRunSuc) {
                    returnBytes[0] = bytApduRtnData;
                    isCmdRunSucFlag[0] = true;
                }
                else {
                    returnBytes[0] = null;
                    isCmdRunSucFlag[0] = false;
                }
                semaphore.release();
            }
        });

        try {
            semaphore.tryAcquire(CAR_NO_RESPONSE_TIME_MS * 2, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new CardNoResponseException(CAR_NO_RESPONSE);
        }
        if (!isCmdRunSucFlag[0]) {
            throw new CardNoResponseException(CAR_RUN_CMD_FAIL);
        }
        return returnBytes[0];
    }

    /**
     * ISO15693写一个块，同步阻塞方式，注意：不能在蓝牙初始化的线程里运行
     * @param addr        要写的块的地址
     * @param writeData   要写的数据，必须4个字节
     * @return            true:写入成功   false：写入失败
     * @throws CardNoResponseException
     *                  操作无响应时会抛出异常
     */
    public boolean write(byte addr, byte writeData[]) throws CardNoResponseException {
        final byte[][] returnBytes = new byte[1][1];
        final boolean[] isCmdRunSucFlag = {false};

        final Semaphore semaphore = new Semaphore(0);

        write(addr, writeData, new onReceiveWriteListener() {
            @Override
            public void onReceiveWrite(boolean isCmdRunSuc) {
                if (isCmdRunSuc) {
                    isCmdRunSucFlag[0] = true;
                }
                else {
                    isCmdRunSucFlag[0] = false;
                }
                semaphore.release();
            }
        });

        try {
            semaphore.tryAcquire(CAR_NO_RESPONSE_TIME_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new CardNoResponseException(CAR_NO_RESPONSE);
        }

        return isCmdRunSucFlag[0];
    }

    /**
     * ISO15693写多个块，同步阻塞方式，注意：不能在蓝牙初始化的线程里运行
     * @param addr        要写的块的地址
     * @param number      要写的块数量,必须大于0
     * @param writeData   要写的数据，必须4个字节
     * @return            true:写入成功   false：写入失败
     * @throws CardNoResponseException
     *                  操作无响应时会抛出异常
     */
    public boolean writeMultiple(byte addr, byte number, byte writeData[]) throws CardNoResponseException {
        final byte[][] returnBytes = new byte[1][1];
        final boolean[] isCmdRunSucFlag = {false};

        final Semaphore semaphore = new Semaphore(0);

        writeMultiple(addr, number, writeData, new onReceiveWriteMultipleListener() {
            @Override
            public void onReceiveWriteMultiple(boolean isCmdRunSuc) {
                if (isCmdRunSuc) {
                    isCmdRunSucFlag[0] = true;
                }
                else {
                    isCmdRunSucFlag[0] = false;
                }
                semaphore.release();
            }
        });

        try {
            semaphore.tryAcquire(CAR_NO_RESPONSE_TIME_MS * 2, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new CardNoResponseException(CAR_NO_RESPONSE);
        }

        return isCmdRunSucFlag[0];
    }

    /**
     * ISO15693指令通道，同步阻塞方式，注意：不能在蓝牙初始化的线程里运行
     * @param data     发送的数据
     * @return         返回的数据
     * @throws CardNoResponseException
     *                  操作无响应时会抛出异常
     */
    public byte[] transceive(byte[] data) throws CardNoResponseException {
        final byte[][] returnBytes = new byte[1][1];
        final boolean[] isCmdRunSucFlag = {false};

        final Semaphore semaphore = new Semaphore(0);
        returnBytes[0] = null;

        cmd(data, new onReceiveCmdListener() {
            @Override
            public void onReceiveCmd(boolean isCmdRunSuc, byte[] bytApduRtnData) {
                if (isCmdRunSuc) {
                    returnBytes[0] = bytApduRtnData;
                    isCmdRunSucFlag[0] = true;
                }
                else {
                    returnBytes[0] = null;
                    isCmdRunSucFlag[0] = false;
                }
                semaphore.release();
            }
        });

        try {
            semaphore.tryAcquire(CAR_NO_RESPONSE_TIME_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new CardNoResponseException(CAR_NO_RESPONSE);
        }
        if (!isCmdRunSucFlag[0]) {
            throw new CardNoResponseException(CAR_RUN_CMD_FAIL);
        }
        return returnBytes[0];
    }
}
