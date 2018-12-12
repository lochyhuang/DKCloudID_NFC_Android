package com.dk.bleNfc.card;

import com.dk.bleNfc.DeviceManager.DeviceManager;
import com.dk.bleNfc.Exception.CardNoResponseException;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by Administrator on 2016/9/21.
 */
public class Mifare extends Card {
    public onReceiveAuthenticateListener mOnReceiveAuthenticateListener;
    public onReceiveDataExchangeListener mOnReceiveDataExchangeListener;
    public onReceiveReadListener mOnReceiveReadListener;
    public onReceiveWriteListener mOnReceiveWriteListener;
    public onReceiveIncrementListener mOnReceiveIncrementListener;
    public onReceiveDecrementListener mOnReceiveDecrementListener;
    public onReceiveTransferListener mOnReceiveTransferListener;
    public onReceiveRestoreListener mOnReceiveRestoreListener;
    public onReceiveIncrementTransferListener mOnReceiveIncrementTransferListener;
    public onReceiveDecrementTransferListener mOnReceiveDecrementTransferListener;
    public onReceiveRestoreTransferListener mOnReceiveRestoreTransferListener;
    public onReceivePersonalizeUidListener mOnReceivePersonalizeUidListener;
    public onReceiveReadValueListener mOnReceiveReadValueListener;
    public onReceiveWriteValueListener mOnReceiveWriteValueListener;

    //Mifare Key type
    public final static byte MIFARE_KEY_TYPE_A = 0x0A;
    public final static byte MIFARE_KEY_TYPE_B = 0x0B;

    public final static byte  PHAL_MFC_CMD_RESTORE   = (byte) 0xC2;    /**< MIFARE Classic Restore command byte */
    public final static byte  PHAL_MFC_CMD_INCREMENT = (byte) 0xC1;    /**< MIFARE Classic Increment command byte */
    public final static byte  PHAL_MFC_CMD_DECREMENT = (byte) 0xC0;    /**< MIFARE Classic Decrement command byte */
    public final static byte  PHAL_MFC_CMD_TRANSFER  = (byte) 0xB0;    /**< MIFARE Classic Transfer command byte */
    public final static byte  PHAL_MFC_CMD_READ      = (byte) 0x30;    /**< MIFARE Classic Read command byte */
    public final static byte  PHAL_MFC_CMD_WRITE     = (byte) 0xA0;    /**< MIFARE Classic Write command byte */
    public final static byte  PHAL_MFC_CMD_AUTHA     = (byte) 0x60;    /**< MIFARE Classic Authenticate A command byte */
    public final static byte  PHAL_MFC_CMD_AUTHB     = (byte) 0x61;    /**< MIFARE Classic Authenticate B command byte */
    public final static byte  PHAL_MFC_CMD_PERSOUID  = (byte) 0x40;    /**< MIFARE Classic Personalize UID command */

    private long valueBuffer;

    public Mifare(DeviceManager deviceManager) {
        super(deviceManager);
    }

    public Mifare(DeviceManager deviceManager, byte[] uid, byte[] atr) {
        super(deviceManager, uid, atr);
    }

    //验证回调接口
    public interface onReceiveAuthenticateListener{
        public void onReceiveAuthenticate(boolean isSuc);
    }
    //数据交换回调接口
    public interface onReceiveDataExchangeListener{
        public void onReceiveDataExchange(boolean isSuc, byte[] returnBytes);
    }
    //读块回调
    public interface onReceiveReadListener {
        public void onReceiveRead(boolean isSuc, byte[] returnBytes);
    }
    //写块回调
    public interface onReceiveWriteListener {
        public void onReceiveWrite(boolean isSuc);
    }
    //增值操作回调
    public interface onReceiveIncrementListener {
        public void onReceiveIncrement(boolean isSuc);
    }

    /**
     * Mifare验证密钥，异步回调方式
     * @param bBlockNo - 验证的块
     * @param bKeyType - 验证密码类型：MIFARE_KEY_TYPE_A 或者 MIFARE_KEY_TYPE_B
     * @param pKey - 验证用到的密钥，6个字节
     * @param listener - 验证结果会通过listener回调
     */
    public void authenticate(byte bBlockNo, byte bKeyType, byte[] pKey, onReceiveAuthenticateListener listener) {
        mOnReceiveAuthenticateListener = listener;
        deviceManager.requestRfmMifareAuth(bBlockNo, bKeyType, pKey, uid, new DeviceManager.onReceiveRfmMifareAuthListener() {
            @Override
            public void onReceiveRfmMifareAuth(boolean isSuc) {
                if (mOnReceiveAuthenticateListener != null) {
                    mOnReceiveAuthenticateListener.onReceiveAuthenticate(isSuc);
                }
            }
        });
    }

    /**
     * Mifare验证密钥，同步阻塞方式，注意：不能在蓝牙初始化的线程里运行
     * @param bBlockNo - 验证的块
     * @param bKeyType - 验证密码类型：MIFARE_KEY_TYPE_A 或者 MIFARE_KEY_TYPE_B
     * @param pKey - 验证用到的密钥，6个字节
     * @return         true：验证成功  false：验证失败
     * @throws CardNoResponseException
     *                  操作无响应时会抛出异常
     */
    public boolean authenticate(byte bBlockNo, byte bKeyType, byte[] pKey) throws CardNoResponseException {
        final byte[][] returnBytes = new byte[1][1];
        final boolean[] isCmdRunSucFlag = {false};

        final Semaphore semaphore = new Semaphore(0);

        authenticate(bBlockNo, bKeyType, pKey, new onReceiveAuthenticateListener() {
            @Override
            public void onReceiveAuthenticate(boolean isCmdRunSuc) {
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
    * 方法名：read(byte blockNo, onReceiveReadListener listener)
    * 功    能：Mifare卡读块
    * 参    数：byte[] blockNo - 要读的块地址
    *           onReceiveReadListener listener - 都块结果回调函数
    * 返回值：无
    */
    public void read(byte blockNo, onReceiveReadListener listener){
        mOnReceiveReadListener = listener;
        byte[] aCommand = {PHAL_MFC_CMD_READ, blockNo};
        dataExchange(aCommand, new onReceiveDataExchangeListener() {
            @Override
            public void onReceiveDataExchange(boolean isSuc, byte[] returnBytes) {
                if (mOnReceiveReadListener != null) {
                    mOnReceiveReadListener.onReceiveRead(isSuc, returnBytes);
                }
            }
        });
    }

    /**
     * Mifare卡读块，同步阻塞方式，注意：不能在蓝牙初始化的线程里运行
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

    /*
    * 方法名：write(byte blockNo, onReceiveReadListener listener)
    * 功    能：Mifare卡写块
    * 参    数：byte[] blockNo - 要读的块地址
    *           onReceiveReadListener listener - 都块结果回调函数
    * 返回值：无
    */
    public void write(byte blockNo, byte[] blockData, onReceiveWriteListener listener){
        mOnReceiveWriteListener = listener;
        byte[] aCommand = {PHAL_MFC_CMD_WRITE, blockNo};
        final byte[] blockDataTemp = blockData;
        dataExchange(aCommand, new onReceiveDataExchangeListener() {
            @Override
            public void onReceiveDataExchange(boolean isSuc, byte[] returnBytes) {
                if ( isSuc && (returnBytes != null) && (returnBytes.length == 1) && (returnBytes[0] == (byte)0xA0) ){
                    dataExchange(blockDataTemp, new onReceiveDataExchangeListener() {
                        @Override
                        public void onReceiveDataExchange(boolean isSuc, byte[] returnBytes) {
                            if (mOnReceiveWriteListener != null) {
                                if ( isSuc && (returnBytes != null) && (returnBytes.length == 1) && (returnBytes[0] == (byte)0xA0) ) {
                                    mOnReceiveWriteListener.onReceiveWrite(true);
                                }
                                else {
                                    mOnReceiveWriteListener.onReceiveWrite(false);
                                }
                            }
                        }
                    });
                }
                else {
                    if (mOnReceiveWriteListener != null) {
                        mOnReceiveWriteListener.onReceiveWrite(false);
                    }
                }
            }
        });
    }

    /**
     * Mifare卡写块，同步阻塞方式，注意：不能在蓝牙初始化的线程里运行
     * @param addr        要写的块的地址
     * @param writeData   要写的数据，必须16个字节
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

    /*
    * 方法名：increment(byte bBlockNo, byte[] pValue, onReceiveIncrementListener listener)
    * 功    能：Mifare卡电子钱包增值操作
    * @pr pValue - 增值多少，由4个字节组成
    *           onReceiveReadListener listener - 都块结果回调函数
    * 返回值：无
    */
    public void increment(byte bBlockNo, byte[] pValue, onReceiveIncrementListener listener) {
        mOnReceiveIncrementListener = listener;
//        byte[] aCommand = {PHAL_MFC_CMD_INCREMENT, bBlockNo};
//        final byte[] valueTemp = pValue;
//        dataExchange(aCommand, new onReceiveDataExchangeListener() {
//            @Override
//            public void onReceiveDataExchange(boolean isSuc, byte[] returnBytes) {
//                if (isSuc){
//                    dataExchange(valueTemp, new onReceiveDataExchangeListener() {
//                        @Override
//                        public void onReceiveDataExchange(boolean isSuc, byte[] returnBytes) {
//                            if (mOnReceiveIncrementListener != null) {
//                                mOnReceiveIncrementListener.onReceiveIncrement(isSuc);
//                            }
//                        }
//                    });
//                }
//                else {
//                    if (mOnReceiveWriteListener != null) {
//                        mOnReceiveIncrementListener.onReceiveIncrement(isSuc);
//                    }
//                }
//            }
//        });
    }

    /*
    * 方法名：
    * 功    能：Mifare电子钱包减值操作
    * 参    数：byte[] dataBytes - 用户交换的数据
    *           onReceiveDataExchangeListener listener - 数据交换结果回调函数
    * 返回值：无
    */
    public void decrement(byte bBlockNo, byte[] pValue, onReceiveDecrementListener listener) {
        mOnReceiveDecrementListener = listener;
//        byte[] aCommand = {PHAL_MFC_CMD_DECREMENT, bBlockNo};
//        final byte[] valueTemp = pValue;
//        dataExchange(aCommand, new onReceiveDataExchangeListener() {
//            @Override
//            public void onReceiveDataExchange(boolean isSuc, byte[] returnBytes) {
//                if (isSuc){
//                    dataExchange(valueTemp, new onReceiveDataExchangeListener() {
//                        @Override
//                        public void onReceiveDataExchange(boolean isSuc, byte[] returnBytes) {
//                            if (mOnReceiveDecrementListener != null) {
//                                mOnReceiveDecrementListener.onReceiveDecrement(isSuc);
//                            }
//                        }
//                    });
//                }
//                else {
//                    if (mOnReceiveDecrementListener != null) {
//                        mOnReceiveDecrementListener.onReceiveDecrement(isSuc);
//                    }
//                }
//            }
//        });
    }
    public interface onReceiveDecrementListener {
        public void onReceiveDecrement(boolean isSuc);
    }

    //Perform MIFARE(R) Transfer command with MIFARE Picc.
    //[in]  bBlockNo  block number the transfer buffer shall be transferred to.
    public void transfer(byte bBlockNo, onReceiveTransferListener listener) {
        mOnReceiveTransferListener = listener;
        byte[] aCommand = {PHAL_MFC_CMD_TRANSFER, bBlockNo};
        dataExchange(aCommand, new onReceiveDataExchangeListener() {
            @Override
            public void onReceiveDataExchange(boolean isSuc, byte[] returnBytes) {
                if (mOnReceiveTransferListener != null) {
                    mOnReceiveTransferListener.onReceiveTransfer(isSuc);
                }
            }
        });
    }
    public interface onReceiveTransferListener {
        public void onReceiveTransfer(boolean isSuc);
    }

    //Perform MIFARE(R) Restore command with MIFARE Picc.
    //[in]  bBlockNo  block number the transfer buffer shall be restored from.
    public void restore(byte bBlockNo, onReceiveRestoreListener listener) {
        mOnReceiveRestoreListener = listener;
//        byte[] aCommand = {PHAL_MFC_CMD_RESTORE, bBlockNo};
//        dataExchange(aCommand, new onReceiveDataExchangeListener() {
//            @Override
//            public void onReceiveDataExchange(boolean isSuc, byte[] returnBytes) {
//                if (isSuc){
//                    dataExchange(getValueBytes(0), new onReceiveDataExchangeListener() {
//                        @Override
//                        public void onReceiveDataExchange(boolean isSuc, byte[] returnBytes) {
//                            if (mOnReceiveRestoreListener != null) {
//                                mOnReceiveRestoreListener.onReceiveRestore(isSuc);
//                            }
//                        }
//                    });
//                }
//                else {
//                    if (mOnReceiveRestoreListener != null) {
//                        mOnReceiveRestoreListener.onReceiveRestore(isSuc);
//                    }
//                }
//            }
//        });
    }
    public interface onReceiveRestoreListener {
        public void onReceiveRestore(boolean isSuc);
    }

    //Perform MIFARE(R) Increment Transfer command sequence with MIFARE Picc.
    //[in]  bSrcBlockNo  block number to be incremented.
    //[in]  bDstBlockNo  block number to be transferred to.
    //[in]  pValue  pValue[4] containing value (LSB first) to be incremented on the MIFARE(R) card
    public void incrementTransfer(byte bSrcBlockNo, byte bDstBlockNo, byte[] pValue, onReceiveIncrementTransferListener listener) {
        mOnReceiveIncrementTransferListener = listener;
        final byte dstBlockNoTemp = bDstBlockNo;
        final byte pValueTemp[] = pValue;
        read(bSrcBlockNo, new onReceiveReadListener() {
            @Override
            public void onReceiveRead(boolean isSuc, byte[] returnBytes) {
                if (!isSuc ||  !CheckValueBlockFormat(returnBytes) ) {
                    if (mOnReceiveIncrementTransferListener != null) {
                        mOnReceiveIncrementTransferListener.onReceiveIncrementTransfer(false);
                    }
                    return;
                }

                long value = getValue(returnBytes) & 0x0ffffffffL;
                long incrementValue = getValue(pValueTemp) & 0x0ffffffffL;

                if ( (value + incrementValue) > 0x0ffffffffL) {
                    value = value + incrementValue - 0x0ffffffffL;
                }
                else {
                    value += incrementValue;
                }
                byte valueBytes[] = getValueBytes(value);
                byte valueBlockDataBytes[] = createValueBlock(valueBytes, dstBlockNoTemp);
                write(dstBlockNoTemp, valueBlockDataBytes, new onReceiveWriteListener() {
                    @Override
                    public void onReceiveWrite(boolean isSuc) {
                        if (mOnReceiveIncrementTransferListener != null) {
                            mOnReceiveIncrementTransferListener.onReceiveIncrementTransfer(isSuc);
                        }
                    }
                });
            }
        });
    }
    public interface onReceiveIncrementTransferListener {
        public void onReceiveIncrementTransfer(boolean isSuc);
    }

    /**
     * Perform MIFARE(R) Increment Transfer command sequence with MIFARE Picc.同步阻塞方式，注意：不能在蓝牙初始化的线程里运行
     * @param bSrcBlockNo  block number to be incremented.
     * @param bDstBlockNo  block number to be transferred to.
     * @param pValue  pValue[4] containing value (LSB first) to be incremented on the MIFARE(R) card
     * @return         true：操作成功  false：操作失败
     * @throws CardNoResponseException
     *                  操作无响应时会抛出异常
     */
    public boolean incrementTransfer(byte bSrcBlockNo, byte bDstBlockNo, byte[] pValue) throws CardNoResponseException {
        final byte[][] returnBytes = new byte[1][1];
        final boolean[] isCmdRunSucFlag = {false};

        final Semaphore semaphore = new Semaphore(0);

        incrementTransfer(bSrcBlockNo, bDstBlockNo, pValue, new onReceiveIncrementTransferListener() {
            @Override
            public void onReceiveIncrementTransfer(boolean isCmdRunSuc) {
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

    //Perform MIFARE(R) Decrement Transfer command sequence with MIFARE Picc.
    //[in]  bSrcBlockNo  block number to be decremented.
    //[in]  bDstBlockNo  block number to be transferred to.
    //[in]  pValue  pValue[4] containing value (LSB first) to be decremented on the MIFARE(R) card
    public void decrementTransfer(byte bSrcBlockNo, byte bDstBlockNo, byte[] pValue, onReceiveDecrementTransferListener listener) {
        mOnReceiveDecrementTransferListener = listener;
        final byte dstBlockNoTemp = bDstBlockNo;
        final byte pValueTemp[] = pValue;
        read(bSrcBlockNo, new onReceiveReadListener() {
            @Override
            public void onReceiveRead(boolean isSuc, byte[] returnBytes) {
                if (!isSuc ||  !CheckValueBlockFormat(returnBytes) ) {
                    if (mOnReceiveDecrementTransferListener != null) {
                        mOnReceiveDecrementTransferListener.onReceiveDecrementTransfer(false);
                    }
                    return;
                }

                long value = getValue(returnBytes) & 0x0ffffffffL;
                long decrementValue = getValue(pValueTemp) & 0x0ffffffffL;
                if ( (value - decrementValue) < 0 ) {
                    value = 0x0ffffffffL + (value - decrementValue);
                }
                else {
                    value -= decrementValue;
                }
                byte valueBytes[] = getValueBytes(value);
                byte valueBlockDataBytes[] = createValueBlock(valueBytes, dstBlockNoTemp);
                write(dstBlockNoTemp, valueBlockDataBytes, new onReceiveWriteListener() {
                    @Override
                    public void onReceiveWrite(boolean isSuc) {
                        if (mOnReceiveDecrementTransferListener != null) {
                            mOnReceiveDecrementTransferListener.onReceiveDecrementTransfer(isSuc);
                        }
                    }
                });
            }
        });
    }
    public interface onReceiveDecrementTransferListener {
        public void onReceiveDecrementTransfer(boolean isSuc);
    }

    /**
     * Perform MIFARE(R) Decrement Transfer command sequence with MIFARE Picc.同步阻塞方式，注意：不能在蓝牙初始化的线程里运行
     * @param bSrcBlockNo  block number to be incremented.
     * @param bDstBlockNo  block number to be transferred to.
     * @param pValue  pValue[4] containing value (LSB first) to be incremented on the MIFARE(R) card
     * @return         true：操作成功  false：操作失败
     * @throws CardNoResponseException
     *                  操作无响应时会抛出异常
     */
    public boolean decrementTransfer(byte bSrcBlockNo, byte bDstBlockNo, byte[] pValue) throws CardNoResponseException {
        final byte[][] returnBytes = new byte[1][1];
        final boolean[] isCmdRunSucFlag = {false};

        final Semaphore semaphore = new Semaphore(0);

        decrementTransfer(bSrcBlockNo, bDstBlockNo, pValue, new onReceiveDecrementTransferListener() {
            @Override
            public void onReceiveDecrementTransfer(boolean isCmdRunSuc) {
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

    //Perform MIFARE(R) Restore Transfer command sequence with MIFARE Picc.
    //[in]  bSrcBlockNo  block number to be decremented.
    //[in]  bDstBlockNo  block number to be transferred to.
    public void restoreTransfer(byte bSrcBlockNo, byte bDstBlockNo, onReceiveRestoreTransferListener listener) {
        mOnReceiveRestoreTransferListener = listener;
        final byte dstBlockNoTemp = bDstBlockNo;
        restore(bSrcBlockNo, new onReceiveRestoreListener() {
            @Override
            public void onReceiveRestore(boolean isSuc) {
                if (!isSuc) {
                    if (mOnReceiveRestoreTransferListener != null) {
                        mOnReceiveRestoreTransferListener.onReceiveRestoreTransfer(isSuc);
                    }
                    return;
                }

                transfer(dstBlockNoTemp, new onReceiveTransferListener() {
                    @Override
                    public void onReceiveTransfer(boolean isSuc) {
                        if (mOnReceiveRestoreTransferListener != null) {
                            mOnReceiveRestoreTransferListener.onReceiveRestoreTransfer(isSuc);
                        }
                    }
                });
            }
        });
    }
    public interface onReceiveRestoreTransferListener {
        void onReceiveRestoreTransfer(boolean isSuc);
    }

    /**
     * Perform MIFARE(R) Restore Transfer command sequence with MIFARE Picc.同步阻塞方式，注意：不能在蓝牙初始化的线程里运行
     * @param bSrcBlockNo  block number to be incremented.
     * @param bDstBlockNo  block number to be transferred to.
     * @return         true：操作成功  false：操作失败
     * @throws CardNoResponseException
     *                  操作无响应时会抛出异常
     */
    public boolean restoreTransfer(byte bSrcBlockNo, byte bDstBlockNo) throws CardNoResponseException {
        final byte[][] returnBytes = new byte[1][1];
        final boolean[] isCmdRunSucFlag = {false};

        final Semaphore semaphore = new Semaphore(0);

        restoreTransfer(bSrcBlockNo, bDstBlockNo, new onReceiveRestoreTransferListener() {
            @Override
            public void onReceiveRestoreTransfer(boolean isCmdRunSuc) {
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

    //Perform MIFARE(R) Personalize UID usage command sequence with MIFARE Picc.
    //[in]  bUidType  UID type.
    //      PHAL_MFC_UID_TYPE_UIDF0
    //      PHAL_MFC_UID_TYPE_UIDF1
    //      PHAL_MFC_UID_TYPE_UIDF2
    //      PHAL_MFC_UID_TYPE_UIDF3
    public void personalizeUid(byte bUidType, onReceivePersonalizeUidListener listener) {
        mOnReceivePersonalizeUidListener = listener;
        byte[] aCommand = {PHAL_MFC_CMD_PERSOUID, bUidType};
        dataExchange(aCommand, new onReceiveDataExchangeListener() {
            @Override
            public void onReceiveDataExchange(boolean isSuc, byte[] returnBytes) {
                if (mOnReceivePersonalizeUidListener != null) {
                    mOnReceivePersonalizeUidListener.onReceivePersonalizeUid(isSuc);
                }
            }
        });
    }
    public interface onReceivePersonalizeUidListener {
        public void onReceivePersonalizeUid(boolean isSuc);
    }

    /**
     * Perform MIFARE(R) Restore Transfer command sequence with MIFARE Picc.同步阻塞方式，注意：不能在蓝牙初始化的线程里运行
     * @param bUidType  UID type.
     *                  PHAL_MFC_UID_TYPE_UIDF0
     *                  PHAL_MFC_UID_TYPE_UIDF1
     *                  PHAL_MFC_UID_TYPE_UIDF2
     *                  PHAL_MFC_UID_TYPE_UIDF3
     * @return         true：操作成功  false：操作失败
     * @throws CardNoResponseException
     *                  操作无响应时会抛出异常
     */
    public boolean personalizeUid(byte bUidType) throws CardNoResponseException {
        final byte[][] returnBytes = new byte[1][1];
        final boolean[] isCmdRunSucFlag = {false};

        final Semaphore semaphore = new Semaphore(0);

        personalizeUid(bUidType, new onReceivePersonalizeUidListener() {
            @Override
            public void onReceivePersonalizeUid(boolean isCmdRunSuc) {
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

    /*
    * Perform MIFARE(R) Read Value command with MIFARE Picc.
    * [in]  bBlockNo  block number to be read.
    * [out]  pValue  pValue[4] containing value (LSB first) read from the MIFARE(R) card
    * [out]  pAddrData  pAddrData containing address read from the MIFARE(R) card value block
    */
    public void readValue(byte bBlockNo, onReceiveReadValueListener listener) {
        mOnReceiveReadValueListener = listener;
        read(bBlockNo, new onReceiveReadListener() {
            @Override
            public void onReceiveRead(boolean isSuc, byte[] returnBytes) {
                if (!isSuc || !CheckValueBlockFormat(returnBytes)) {
                    if (mOnReceiveReadValueListener != null) {
                        mOnReceiveReadValueListener.onReceiveReadValue(isSuc, (byte)0, null);
                    }
                    return;
                }
                if (mOnReceiveReadValueListener != null) {
                    byte[] valueBytes = {returnBytes[0],returnBytes[1],returnBytes[2],returnBytes[3]};
                    mOnReceiveReadValueListener.onReceiveReadValue(isSuc, returnBytes[12], valueBytes);
                }
            }
        });
    }
    public interface onReceiveReadValueListener {
        public void onReceiveReadValue(boolean isSuc, byte address, byte[] valueBytes);
    }

    /**
     * Perform MIFARE(R) Read Value command with MIFARE Picc.同步阻塞方式，注意：不能在蓝牙初始化的线程里运行
     * @param addr     block number to be read.
     * @return         pValue[4] containing value (LSB first) read from the MIFARE(R) card
     * @throws CardNoResponseException
     *                  操作无响应时会抛出异常
     */
    public byte[] readValue(byte addr) throws CardNoResponseException {
        final byte[][] returnBytes = new byte[1][1];
        final boolean[] isCmdRunSucFlag = {false};

        final Semaphore semaphore = new Semaphore(0);
        returnBytes[0] = null;

        readValue(addr, new onReceiveReadValueListener() {
            @Override
            public void onReceiveReadValue(boolean isSuc, byte address, byte[] valueBytes) {
                if (isSuc) {
                    returnBytes[0] = valueBytes;
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

    //Perform MIFARE(R) Write Value command with MIFARE Picc.
    //[in]  bBlockNo  block number to be written.
    //[in]  pValue  pValue[4] containing value (LSB first) to be written to the MIFARE(R) card
    //[in]  bAddrData  bAddrData containing address written to the MIFARE(R) card value block
    public void writeValue(byte bBlockNo, byte[] pValue, byte pAddrData, onReceiveWriteValueListener listener) {
        mOnReceiveWriteValueListener = listener;
        byte[] writeByte = createValueBlock(pValue, pAddrData);
        write(bBlockNo, writeByte, new onReceiveWriteListener() {
            @Override
            public void onReceiveWrite(boolean isSuc) {
                if (mOnReceiveWriteValueListener != null) {
                    mOnReceiveWriteValueListener.onReceiveWriteValue(isSuc);
                }
            }
        });
    }
    public interface onReceiveWriteValueListener {
        public void onReceiveWriteValue(boolean isSuc);
    }

    /**
     * Perform MIFARE(R) Write Value command with MIFARE Picc.同步阻塞方式，注意：不能在蓝牙初始化的线程里运行
     * @param bBlockNo  block number to be written.
     * @param pValue  pValue[4] containing value (LSB first) to be written to the MIFARE(R) card
     * @param pAddrData  pAddrData containing address written to the MIFARE(R) card value block
     * @return            true:写入成功   false：写入失败
     * @throws CardNoResponseException
     *                  操作无响应时会抛出异常
     */
    public boolean writeValue(byte bBlockNo, byte[] pValue, byte pAddrData) throws CardNoResponseException {
        final byte[][] returnBytes = new byte[1][1];
        final boolean[] isCmdRunSucFlag = {false};

        final Semaphore semaphore = new Semaphore(0);

        writeValue(bBlockNo, pValue, pAddrData, new onReceiveWriteValueListener() {
            @Override
            public void onReceiveWriteValue(boolean isCmdRunSuc) {
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

    /*
    * 方法名：dataExchange(byte[] dataBytes, onReceiveDataExchangeListener listener)
    * 功    能：Mifare卡指令传输
    * 参    数：byte[] dataBytes - 用户交换的数据
    *           onReceiveDataExchangeListener listener - 数据交换结果回调函数
    * 返回值：无
    */
    public void dataExchange(byte[] dataBytes, onReceiveDataExchangeListener listener) {
        mOnReceiveDataExchangeListener = listener;
        deviceManager.requestRfmMifareDataExchange(dataBytes, new DeviceManager.onReceiveRfmMifareDataExchangeListener() {
            @Override
            public void onReceiveRfmMifareDataExchange(boolean isSuc, byte[] returnData) {
                if (mOnReceiveDataExchangeListener != null) {
                    mOnReceiveDataExchangeListener.onReceiveDataExchange(isSuc, returnData);
                }
            }
        });
    }

    /**
     * Mifare卡指令传输，同步阻塞方式，注意：不能在蓝牙初始化的线程里运行
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

        dataExchange(data, new onReceiveDataExchangeListener() {
            @Override
            public void onReceiveDataExchange(boolean isCmdRunSuc, byte[] bytApduRtnData) {
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

    public boolean CheckValueBlockFormat(byte[] pBlock) {
        if ( (pBlock == null) || (pBlock.length != 16) ) {
            return false;
        }
        /* check format of value block */
        if ((pBlock[0] != pBlock[8]) ||
                (pBlock[1] != pBlock[9]) ||
                (pBlock[2] != pBlock[10]) ||
                (pBlock[3] != pBlock[11]) ||
                (pBlock[4] != (byte)( (pBlock[0] & 0x00ff) ^ 0xFF)) ||
                (pBlock[5] != (byte)( (pBlock[1] & 0x00ff) ^ 0xFF)) ||
                (pBlock[6] != (byte)( (pBlock[2] & 0x00ff) ^ 0xFF)) ||
                (pBlock[7] != (byte)( (pBlock[3] & 0x00ff) ^ 0xFF)) ||
                (pBlock[12] != pBlock[14]) ||
                (pBlock[13] != pBlock[15]) ||
                (pBlock[12] != (byte)( (pBlock[13] & 0x00ff) ^ 0xFF)))
        {
            return false;
        }
        return true;
    }

    public byte[] createValueBlock(byte[] pValue, byte bAddrData) {
        if ( (pValue == null) || (pValue.length < 4) ) {
            return null;
        }
        byte[] pBlock = new byte[16];
        pBlock[0]  = pValue[0];
        pBlock[1]  = pValue[1];
        pBlock[2]  = pValue[2];
        pBlock[3]  = pValue[3];
        pBlock[4]  = (byte) ~((pValue[0] & 0x00ff) & 0x00ff);
        pBlock[5]  = (byte) ~((pValue[1] & 0x00ff) & 0x00ff);
        pBlock[6]  = (byte) ~((pValue[2] & 0x00ff) & 0x00ff);
        pBlock[7]  = (byte) ~((pValue[3] & 0x00ff) & 0x00ff);
        pBlock[8]  = pValue[0];
        pBlock[9]  = pValue[1];
        pBlock[10] = pValue[2];
        pBlock[11] = pValue[3];
        pBlock[12] = bAddrData;
        pBlock[13] = (byte) ~((bAddrData & 0x00ff) & 0x00ff);
        pBlock[14] = bAddrData;
        pBlock[15] = (byte) ~((bAddrData & 0x00ff) & 0x00ff);
        return pBlock;
    }

    public int getValue(byte[] valueByte) {
        int value = 0;
        if (valueByte == null || valueByte.length < 4) {
            return 0;
        }
        value = ( ((valueByte[0] & 0x000000ff) << 24) | ((valueByte[1] & 0x000000ff) << 16) | ((valueByte[2] & 0x000000ff) << 8) | (valueByte[3] & 0x000000ff) );
        return value;
    }

    public byte[] getValueBytes(long value) {
        return new byte[] {(byte) (((value & 0xff000000) >> 24) & 0xff),
                (byte) (((value & 0x00ff0000) >> 16) & 0xff),
                (byte) (((value & 0x0000ff00) >> 8) & 0xff),
                (byte) (((value & 0x000000ff) >> 0) & 0xff)};
    }
}
