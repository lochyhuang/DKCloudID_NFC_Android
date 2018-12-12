package com.dk.bleNfc.card;

import com.dk.bleNfc.DeviceManager.DeviceManager;
import com.dk.bleNfc.Exception.CardNoResponseException;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by Administrator on 2016/9/19.
 */
public class Ultralight extends Card{
    final static byte  UL_GET_VERSION_CMD = (byte)0x60;
    final static byte  UL_READ_CMD = (byte)0x30;
    final static byte  UL_FAST_READ_CMD = (byte)0x3A;
    final static byte  UL_WRITE_CMD = (byte)0xA2;
    final static byte  UL_READ_CNT_CMD = (byte)0x39;
    final static byte  UL_PWD_AUTH_CMD = (byte)0x1B;

    public final static int   UL_MAX_FAST_READ_BLOCK_NUM = 4;
    public final static int  LONG_READ_MAX_NUMBER = 0x30;

    public onReceiveGetVersionListener mOnReceiveGetVersionListener;
    public onReceiveReadListener mOnReceiveReadListener;
    public onReceiveFastReadListener mOnReceiveFastReadListener;
    public onReceiveWriteListener mOnReceiveWriteListener;
    public onReceiveReadCntListener mOnReceiveReadCntListener;
    public onReceivePwdAuthListener mOnReceivePwdAuthListener;
    public onReceiveCmdListener mOnReceiveCmdListener;

    public Ultralight(DeviceManager deviceManager) {
        super(deviceManager);
    }

    public Ultralight(DeviceManager deviceManager, byte[] uid, byte[] atr) {
        super(deviceManager, uid, atr);
    }

    //读取卡片版本回调
    public interface onReceiveGetVersionListener {
        public void onReceiveGetVersion(boolean isSuc, byte[] returnBytes);
    }
    //读块回调
    public interface onReceiveReadListener {
        public void onReceiveRead(boolean isSuc, byte[] returnBytes);
    }
    //快速读回调
    public interface onReceiveFastReadListener {
        public void onReceiveFastRead(boolean isSuc, byte[] returnBytes);
    }
    //写块回调
    public interface onReceiveWriteListener {
        public void onReceiveWrite(boolean isSuc,byte[] returnBytes);
    }
    //读次数回调
    public interface onReceiveReadCntListener {
        public void onReceiveReadCnt(byte[] returnBytes);
    }
    //验证密码回调
    public interface onReceivePwdAuthListener {
        public void onReceivePwdAuth(boolean isSuc);
    }
    //验证密码回调
    public interface onReceiveCmdListener {
        public void onReceiveCmd(byte[] returnBytes);
    }

    //读取卡片版本
    public void getVersion(onReceiveGetVersionListener listener) {
        mOnReceiveGetVersionListener = listener;
        byte[] cmdByte = {UL_GET_VERSION_CMD};
        deviceManager.requestRfmUltralightCmd(cmdByte, new DeviceManager.onReceiveRfmUltralightCmdListener() {
            @Override
            public void onReceiveRfmUltralightCmd(boolean isCmdRunSuc, byte[] bytUlRtnData) {
                if (mOnReceiveGetVersionListener != null) {
                    mOnReceiveGetVersionListener.onReceiveGetVersion(isCmdRunSuc, bytUlRtnData);
                }
            }
        });
    }

    /**
     * 读取卡片版本，同步阻塞方式，注意：不能在蓝牙初始化的线程里运行
     * @return         返回的数据
     * @throws CardNoResponseException
     *                  操作无响应时会抛出异常
     */
    public byte[] getVersion() throws CardNoResponseException {
        final byte[][] returnBytes = new byte[1][1];
        final boolean[] isCmdRunSucFlag = {false};

        final Semaphore semaphore = new Semaphore(0);
        returnBytes[0] = null;

        getVersion(new onReceiveGetVersionListener() {
            @Override
            public void onReceiveGetVersion(boolean isCmdRunSuc, byte[] bytApduRtnData) {
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

    //读块
    public void read(byte address, onReceiveReadListener listener) {
        mOnReceiveReadListener = listener;
        byte[] cmdByte = {UL_READ_CMD, address};
        deviceManager.requestRfmUltralightCmd(cmdByte, new DeviceManager.onReceiveRfmUltralightCmdListener() {
            @Override
            public void onReceiveRfmUltralightCmd(boolean isCmdRunSuc, byte[] bytUlRtnData) {
                if (mOnReceiveReadListener != null) {
                    mOnReceiveReadListener.onReceiveRead(isCmdRunSuc, bytUlRtnData);
                }
            }
        });
    }

    /**
     * 读单个块数据，同步阻塞方式，注意：不能在蓝牙初始化的线程里运行
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

    //快速读
    public void fastRead(byte startAddress, byte endAddress, onReceiveFastReadListener listener) {
        mOnReceiveFastReadListener = listener;
        if (startAddress > endAddress) {
            if (mOnReceiveFastReadListener != null) {
                mOnReceiveFastReadListener.onReceiveFastRead(false, null);
            }
            return;
        }
        byte[] cmdByte = {UL_FAST_READ_CMD, startAddress, endAddress};
        deviceManager.requestRfmUltralightCmd(cmdByte, new DeviceManager.onReceiveRfmUltralightCmdListener() {
            @Override
            public void onReceiveRfmUltralightCmd(boolean isCmdRunSuc, byte[] bytUlRtnData) {
                if (mOnReceiveFastReadListener != null) {
                    mOnReceiveFastReadListener.onReceiveFastRead(isCmdRunSuc, bytUlRtnData);
                }
            }
        });
    }

    /**
     * 快速读，同步阻塞方式，注意：不能在蓝牙初始化的线程里运行
     * @param startAddress     要读起始地址
     * @param endAddress       要读的结束地址
     * @return                 读取到的数据
     * @throws CardNoResponseException
     *                  操作无响应时会抛出异常
     */
    public byte[] fastRead(byte startAddress, byte endAddress) throws CardNoResponseException {
        final byte[][] returnBytes = new byte[1][1];
        final boolean[] isCmdRunSucFlag = {false};

        final Semaphore semaphore = new Semaphore(0);
        returnBytes[0] = null;

        fastRead(startAddress, endAddress, new onReceiveFastReadListener() {
            @Override
            public void onReceiveFastRead(boolean isCmdRunSuc, byte[] bytApduRtnData) {
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

        deviceManager.requestRfmUltralightLongRead(startAddress, number, new DeviceManager.onReceiveUlLongReadListener() {
            @Override
            public void onReceiveUlLongRead(boolean isCmdRunSuc, byte[] bytApduRtnData) {
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

    //写块
    public void write(byte address, byte[] data, onReceiveWriteListener listener) {
        mOnReceiveWriteListener = listener;
        byte[] cmdByte = {UL_WRITE_CMD, address, data[0], data[1], data[2], data[3]};
        deviceManager.requestRfmUltralightCmd(cmdByte, new DeviceManager.onReceiveRfmUltralightCmdListener() {
            @Override
            public void onReceiveRfmUltralightCmd(boolean isCmdRunSuc, byte[] bytUlRtnData) {
                if (mOnReceiveWriteListener != null) {
                    mOnReceiveWriteListener.onReceiveWrite(isCmdRunSuc, bytUlRtnData);
                }
            }
        });
    }

    /**
     * 写一个块，同步阻塞方式，注意：不能在蓝牙初始化的线程里运行
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
            public void onReceiveWrite(boolean isCmdRunSuc, byte[] returnBytes) {
                if (isCmdRunSuc) {
                    if (  (returnBytes == null) || ((returnBytes[0] & 0x0f) != 0x0a) ) {
                        isCmdRunSucFlag[0] = false;
                    }
                    else {
                        isCmdRunSucFlag[0] = true;
                    }
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
     * 写一个块，同步阻塞方式，注意：不能在蓝牙初始化的线程里运行
     * @param startAddress   要写的块的起始地址
     * @param data        要写的数据，必须小于0x3f字节
     * @return            true:写入成功   false：写入失败
     * @throws CardNoResponseException
     *                  操作无响应时会抛出异常
     */
    public boolean longWriteSingle(byte startAddress, byte[] data) throws CardNoResponseException {
        final byte[][] returnBytes = new byte[1][1];
        final boolean[] isCmdRunSucFlag = {false};

        final Semaphore semaphore = new Semaphore(0);
        deviceManager.requestRfmUltralightLongWrite(startAddress, data, new DeviceManager.onReceiveUlLongWriteListener() {
            @Override
            public void onReceiveUlLongWrite(boolean isCmdRunSuc) {
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
            semaphore.tryAcquire(CAR_NO_RESPONSE_TIME_MS * 10, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new CardNoResponseException(CAR_NO_RESPONSE);
        }

        return isCmdRunSucFlag[0];
    }

    //读次数
    public void readCnt(onReceiveReadCntListener listener) {
        mOnReceiveReadCntListener = listener;
        byte[] cmdByte = {UL_READ_CNT_CMD, 0x02};
        deviceManager.requestRfmUltralightCmd(cmdByte, new DeviceManager.onReceiveRfmUltralightCmdListener() {
            @Override
            public void onReceiveRfmUltralightCmd(boolean isCmdRunSuc, byte[] bytUlRtnData) {
                if (mOnReceiveReadCntListener != null) {
                    mOnReceiveReadCntListener.onReceiveReadCnt(bytUlRtnData);
                }
            }
        });
    }

    /**
     * 读次数，同步阻塞方式，注意：不能在蓝牙初始化的线程里运行
     * @return            返回的数据
     * @throws CardNoResponseException
     *                  操作无响应时会抛出异常
     */
    public byte[] readCnt() throws CardNoResponseException {
        final byte[][] returnBytes = new byte[1][1];

        final Semaphore semaphore = new Semaphore(0);
        returnBytes[0] = null;

        readCnt(new onReceiveReadCntListener() {
            @Override
            public void onReceiveReadCnt(byte[] bytApduRtnData) {
                returnBytes[0] = bytApduRtnData;
                semaphore.release();
            }
        });

        try {
            semaphore.tryAcquire(CAR_NO_RESPONSE_TIME_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new CardNoResponseException(CAR_NO_RESPONSE);
        }
        return returnBytes[0];
    }

    //验证密码
    public void pwdAuth(byte[] password, onReceivePwdAuthListener listener) {
        mOnReceivePwdAuthListener = listener;
        if (password.length != 4) {
            if (mOnReceivePwdAuthListener != null) {
                mOnReceivePwdAuthListener.onReceivePwdAuth(false);
            }
        }
        else {
            byte[] cmdByte = {UL_PWD_AUTH_CMD, password[0], password[1], password[2], password[3]};
            deviceManager.requestRfmUltralightCmd(cmdByte, new DeviceManager.onReceiveRfmUltralightCmdListener() {
                @Override
                public void onReceiveRfmUltralightCmd(boolean isCmdRunSuc, byte[] bytUlRtnData) {
                    if (mOnReceivePwdAuthListener != null) {
                        mOnReceivePwdAuthListener.onReceivePwdAuth(isCmdRunSuc);
                    }
                }
            });
        }
    }

    /**
     * 验证密码，同步阻塞方式，注意：不能在蓝牙初始化的线程里运行
     * @param password    要验证的密码，4 Bytes
     * @return            true:验证成功  false:验证失败
     * @throws CardNoResponseException
     *                  操作无响应时会抛出异常
     */
    public boolean pwdAuth(byte[] password) throws CardNoResponseException {
        final byte[][] returnBytes = new byte[1][1];
        final boolean[] isCmdRunSucFlag = {false};

        final Semaphore semaphore = new Semaphore(0);

        pwdAuth(password, new onReceivePwdAuthListener() {
            @Override
            public void onReceivePwdAuth(boolean isCmdRunSuc) {
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

    //指令通道
    public void cmd(byte[] cmdByte, onReceiveCmdListener listener) {
        mOnReceiveCmdListener = listener;
        deviceManager.requestRfmUltralightCmd(cmdByte, new DeviceManager.onReceiveRfmUltralightCmdListener() {
            @Override
            public void onReceiveRfmUltralightCmd(boolean isCmdRunSuc, byte[] bytUlRtnData) {
                if (mOnReceiveCmdListener != null) {
                    mOnReceiveCmdListener.onReceiveCmd(bytUlRtnData);
                }
            }
        });
    }

    /**
     * 指令传输通道，同步阻塞方式，注意：不能在蓝牙初始化的线程里运行
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

        deviceManager.requestRfmUltralightCmd(data, new DeviceManager.onReceiveRfmUltralightCmdListener() {
            @Override
            public void onReceiveRfmUltralightCmd(boolean isCmdRunSuc, byte[] bytUlRtnData) {
                if (isCmdRunSuc) {
                    returnBytes[0] = bytUlRtnData;
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
