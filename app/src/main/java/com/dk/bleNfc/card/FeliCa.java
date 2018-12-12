package com.dk.bleNfc.card;

import com.dk.bleNfc.DeviceManager.DeviceManager;
import com.dk.bleNfc.Exception.CardNoResponseException;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by Administrator on 2016/9/21.
 */
public class FeliCa extends Card{
    public onReceiveRequestResponseListener mOnReceiveRequestResponseListener;
    public onReceiveRequestServiceListener mOnReceiveRequestServiceListener;
    public onReceiveReadListener mOnReceiveReadListener;
    public onReceiveWriteListener mOnReceiveWriteListener;

    public final static byte  PHAL_FELICA_CMD_REQUEST_RESPONSE   = 0x04;   /**< Get the PICCs current mode. */
    public final static byte  PHAL_FELICA_CMD_REQUEST_SERVICE    = 0x02;   /**< Get area key version and service key version. */
    public final static byte  PHAL_FELICA_CMD_READ               = 0x06;   /**< Read the record value of the specified service. */
    public final static byte  PHAL_FELICA_CMD_WRITE              = 0x08;   /**< Write records of the specified service. */
    public final static byte  PHAL_FELICA_RSP_REQUEST_RESPONSE   = 0x05;   /**< Response code to the Request Response command. */
    public final static byte  PHAL_FELICA_RSP_REQUEST_SERVICE    = 0x03;   /**< Response code to the Request Service command. */
    public final static byte  PHAL_FELICA_RSP_READ               = 0x07;   /**< Response code to the Read command. */
    public final static byte  PHAL_FELICA_RSP_WRITE              = 0x09;   /**< Response code to the Write command. */

    public final static int  PH_EXCHANGE_DEFAULT = 0x0000;
    public final static int  PH_EXCHANGE_LEAVE_BUFFER_BIT = 0x4000;
    public final static int  PH_EXCHANGE_BUFFERED_BIT = 0x8000;
    public final static int PH_EXCHANGE_BUFFER_FIRST = PH_EXCHANGE_DEFAULT | PH_EXCHANGE_BUFFERED_BIT;
    public final static int PH_EXCHANGE_BUFFER_CONT = PH_EXCHANGE_DEFAULT | PH_EXCHANGE_BUFFERED_BIT | PH_EXCHANGE_LEAVE_BUFFER_BIT;
    public final static int PH_EXCHANGE_BUFFER_LAST = PH_EXCHANGE_DEFAULT | PH_EXCHANGE_LEAVE_BUFFER_BIT;

    public FeliCa(DeviceManager deviceManager) {
        super(deviceManager);
    }

    public FeliCa(DeviceManager deviceManager, byte[] uid, byte[] atr) {
        super(deviceManager, uid, atr);
    }

    //When receiving the RequestResponse command, the VICC shall respond.
    public void requestResponse (onReceiveRequestResponseListener listener){
        mOnReceiveRequestResponseListener = listener;
        byte[] cmdBytes = {PHAL_FELICA_CMD_REQUEST_RESPONSE};
        deviceManager.requestRfmFelicaCmd(PH_EXCHANGE_DEFAULT, 0, cmdBytes, new DeviceManager.onReceiveRfmFelicaCmdListener() {
            @Override
            public void onReceiveRfmFelicaCmd(boolean isSuc, byte[] returnBytes) {
                if (mOnReceiveRequestResponseListener != null) {
                    mOnReceiveRequestResponseListener.onReceiveRequestResponse(isSuc, returnBytes[0]);
                }
            }
        });
    }
    public interface onReceiveRequestResponseListener {
        //mode - Current Card Mode. (0, 1, 2).
        public void onReceiveRequestResponse(boolean isSuc, byte mode);
    }

    //When receiving the RequestService command, the VICC shall respond.
    //[in]  bTxNumServices  Number of services or areas within the command message.
    //[in]  pTxServiceList  Service code or area code list within the command message.
    public void requestService (byte bTxNumServices, byte[] pTxServiceList, onReceiveRequestServiceListener listener) {
        mOnReceiveRequestServiceListener = listener;
        if (pTxServiceList.length != bTxNumServices * 2) {
            if (mOnReceiveRequestServiceListener != null) {
                mOnReceiveRequestServiceListener.onReceiveRequestService(false, (byte) 0, null);
            }
            return;
        }
        byte[] cmdBytes = {PHAL_FELICA_CMD_REQUEST_SERVICE, bTxNumServices};
        final byte txNumServicesTemp = bTxNumServices;
        final byte[] txServiceListTemp = pTxServiceList;
        deviceManager.requestRfmFelicaCmd(PH_EXCHANGE_BUFFER_FIRST, bTxNumServices, cmdBytes, new DeviceManager.onReceiveRfmFelicaCmdListener() {
            @Override
            public void onReceiveRfmFelicaCmd(boolean isSuc, byte[] returnBytes) {
                if (!isSuc) {
                    if (mOnReceiveRequestServiceListener != null) {
                        mOnReceiveRequestServiceListener.onReceiveRequestService(false, (byte) 0, null);
                    }
                    return;
                }
                deviceManager.requestRfmFelicaCmd(PH_EXCHANGE_BUFFER_LAST, txNumServicesTemp, txServiceListTemp, new DeviceManager.onReceiveRfmFelicaCmdListener() {
                    @Override
                    public void onReceiveRfmFelicaCmd(boolean isSuc, byte[] returnBytes) {
                        if (!isSuc || (returnBytes.length < 1) || (returnBytes.length != (returnBytes[0] * 2) + 1 )) {
                            if (mOnReceiveRequestServiceListener != null) {
                                mOnReceiveRequestServiceListener.onReceiveRequestService(false, (byte) 0, null);
                            }
                            return;
                        }
                        if (mOnReceiveRequestServiceListener != null) {
                            byte[] rxServiceList = new byte[returnBytes[0] * 2];
                            System.arraycopy(returnBytes, 1, rxServiceList, 0, returnBytes[0] * 2);
                            mOnReceiveRequestServiceListener.onReceiveRequestService(true, returnBytes[0], rxServiceList);
                        }
                    }
                });
            }
        });
    }
    public interface onReceiveRequestServiceListener {
        //[out]  pRxNumServices  Number of received services or areas.
        //[out]  pRxServiceList  Received Service Key version or area version list, max 64 bytes.
        public void onReceiveRequestService(boolean isSuc, byte pRxNumServices, byte[] pRxServiceList);
    }


    //When receiving the Read command, the VICC shall respond.
    //[in]  bNumServices  Number of Services.
    //[in]  pServiceList  List of Services.
    //[in]  bTxNumBlocks  Number of Blocks to send.
    //[in]  pBlockList  List of Blocks to read.
    public void read(byte bNumServices,
                     byte[] pServiceList,
                     byte bTxNumBlocks,
                     byte[] pBlockList,
                     onReceiveReadListener listener) {
        mOnReceiveReadListener = listener;
        if (pServiceList.length != bNumServices * 2) {
            if (mOnReceiveReadListener != null) {
                mOnReceiveReadListener.onReceiveRead(false, (byte) 0, null);
            }
            return;
        }

        /* check correct number of services / blocks */
        if ((bNumServices < 1) || (bTxNumBlocks < 1)) {
            if (mOnReceiveReadListener != null) {
                mOnReceiveReadListener.onReceiveRead(false, (byte) 0, null);
            }
            return;
        }

        /* check blocklistlength against numblocks */
        if (pBlockList.length < (bTxNumBlocks * 2)  || pBlockList.length > (bTxNumBlocks * 3)) {
            if (mOnReceiveReadListener != null) {
                mOnReceiveReadListener.onReceiveRead(false, (byte) 0, null);
            }
            return;
        }

        byte[] cmdBytes = {PHAL_FELICA_CMD_READ, bNumServices};
        final byte txNumBlocksTemp = bTxNumBlocks;
        final byte[] serviceListTemp = pServiceList;
        final byte[] blockListTemp = pBlockList;
        /* Exchange command and the number of services ... */
        deviceManager.requestRfmFelicaCmd(PH_EXCHANGE_BUFFER_FIRST, txNumBlocksTemp, cmdBytes, new DeviceManager.onReceiveRfmFelicaCmdListener() {
            @Override
            public void onReceiveRfmFelicaCmd(boolean isSuc, byte[] returnBytes) {
                if (!isSuc) {
                    if (mOnReceiveReadListener != null) {
                        mOnReceiveReadListener.onReceiveRead(false, (byte) 0, null);
                    }
                    return;
                }
                /* ... the service code list ... */
                deviceManager.requestRfmFelicaCmd(PH_EXCHANGE_BUFFER_CONT, txNumBlocksTemp, serviceListTemp, new DeviceManager.onReceiveRfmFelicaCmdListener() {
                    @Override
                    public void onReceiveRfmFelicaCmd(boolean isSuc, byte[] returnBytes) {
                        if (!isSuc) {
                            if (mOnReceiveReadListener != null) {
                                mOnReceiveReadListener.onReceiveRead(false, (byte) 0, null);
                            }
                            return;
                        }
                        /* ... the number of blocks ... */
                        deviceManager.requestRfmFelicaCmd(PH_EXCHANGE_BUFFER_CONT, txNumBlocksTemp, new byte[]{txNumBlocksTemp}, new DeviceManager.onReceiveRfmFelicaCmdListener() {
                            @Override
                            public void onReceiveRfmFelicaCmd(boolean isSuc, byte[] returnBytes) {
                                if (!isSuc) {
                                    if (mOnReceiveReadListener != null) {
                                        mOnReceiveReadListener.onReceiveRead(false, (byte) 0, null);
                                    }
                                    return;
                                }
                                /* ... and the block list. */
                                deviceManager.requestRfmFelicaCmd(PH_EXCHANGE_BUFFER_LAST, txNumBlocksTemp, blockListTemp, new DeviceManager.onReceiveRfmFelicaCmdListener() {
                                    @Override
                                    public void onReceiveRfmFelicaCmd(boolean isSuc, byte[] returnBytes) {
                                        /* on a felica error save the status flags */
                                        if ( !isSuc || (returnBytes.length < 2) || (returnBytes[0] != 0) || (returnBytes.length != (3 + (16 * returnBytes[2]))) ) {
                                            if (mOnReceiveReadListener != null) {
                                                mOnReceiveReadListener.onReceiveRead(false, (byte) 0, null);
                                            }
                                            return;
                                        }
                                        if (mOnReceiveReadListener != null) {
                                            byte[] blockData = new byte[16 * returnBytes[2]];
                                            System.arraycopy(returnBytes, 3, blockData, 0, 16 * returnBytes[2]);
                                            mOnReceiveReadListener.onReceiveRead(isSuc, returnBytes[2], blockData);
                                        }
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }
    public interface onReceiveReadListener {
        //[out]  pRxNumBlocks  Number of received blocks.
        //[out]  pBlockData  Received Block data.
        public void onReceiveRead(boolean isSuc, byte pRxNumBlocks, byte[] pBlockData);
    }

    /**
     * When receiving the Read command, the VICC shall respond，同步阻塞方式，注意：不能在蓝牙初始化的线程里运行
     * @param bNumServices  Number of Services.
     * @param pServiceList  List of Services.
     * @param bTxNumBlocks  Number of Blocks to send.
     * @param pBlockList  List of Blocks to read.
     * @return         读取到的数据
     * @throws CardNoResponseException
     *                  操作无响应时会抛出异常
     */
    public byte[] read(byte bNumServices,
                       byte[] pServiceList,
                       byte bTxNumBlocks,
                       byte[] pBlockList) throws CardNoResponseException {
        final byte[][] returnBytes = new byte[1][1];
        final boolean[] isCmdRunSucFlag = {false};

        final Semaphore semaphore = new Semaphore(0);
        returnBytes[0] = null;

        read(bNumServices, pServiceList, bTxNumBlocks, pBlockList, new onReceiveReadListener() {
            @Override
            public void onReceiveRead(boolean isSuc, byte pRxNumBlocks, byte[] pBlockData) {
                if (isSuc) {
                    returnBytes[0] = pBlockData;
                    isCmdRunSucFlag[0] = isSuc;
                }
                else {
                    returnBytes[0] = null;
                    isCmdRunSucFlag[0] = isSuc;
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

    //When receiving the Write command, the VICC shall respond.
    //[in]  bNumServices  Number of Services.
    //[in]  pServiceList  List of Services.
    //[in]  bNumBlocks  Number of Blocks to send.
    //[in]  pBlockList  List of Blocks to write.
    //[in]  bBlockListLength  Number of Blocks to write.
    //[in]  pBlockData  Block data to write.
    public void write( byte bNumServices,
                       byte[]  pServiceList,
                       byte  bNumBlocks,
                       byte[]  pBlockList,
                       byte[]  pBlockData,
                       onReceiveWriteListener listener) {
        mOnReceiveWriteListener = listener;
        /* check correct number of services / blocks */
        if ((bNumServices < 1) || (bNumBlocks < 1)) {
            if (mOnReceiveWriteListener != null) {
                mOnReceiveWriteListener.onReceiveWrite(false, null);
            }
        }

        /* check blocklistlength against numblocks */
        if ((pBlockList.length < (bNumBlocks * 2))  || (pBlockList.length > (bNumBlocks * 3))) {
            if (mOnReceiveWriteListener != null) {
                mOnReceiveWriteListener.onReceiveWrite(false, null);
            }
        }

        byte[] cmdBytes = {PHAL_FELICA_CMD_WRITE, bNumServices};
        final byte numBlocksTemp = bNumBlocks;
        final byte[] serviceListTemp = pServiceList;
        final byte[] blockListTemp = pBlockList;
        final byte[] blockDataTemp = pBlockData;
        /* Exchange command and the number of services ... */
        deviceManager.requestRfmFelicaCmd(PH_EXCHANGE_BUFFER_FIRST, numBlocksTemp, cmdBytes, new DeviceManager.onReceiveRfmFelicaCmdListener() {
            @Override
            public void onReceiveRfmFelicaCmd(boolean isSuc, byte[] returnBytes) {
                if (!isSuc) {
                    if (mOnReceiveWriteListener != null) {
                        mOnReceiveWriteListener.onReceiveWrite(false, returnBytes);
                    }
                    return;
                }
                /* ... the service code list ... */
                deviceManager.requestRfmFelicaCmd(PH_EXCHANGE_BUFFER_CONT, numBlocksTemp, serviceListTemp, new DeviceManager.onReceiveRfmFelicaCmdListener() {
                    @Override
                    public void onReceiveRfmFelicaCmd(boolean isSuc, byte[] returnBytes) {
                        if (!isSuc) {
                            if (mOnReceiveWriteListener != null) {
                                mOnReceiveWriteListener.onReceiveWrite(false, returnBytes);
                            }
                            return;
                        }
                        /* ... the number of blocks ... */
                        deviceManager.requestRfmFelicaCmd(PH_EXCHANGE_BUFFER_CONT, numBlocksTemp, new byte[]{numBlocksTemp}, new DeviceManager.onReceiveRfmFelicaCmdListener() {
                            @Override
                            public void onReceiveRfmFelicaCmd(boolean isSuc, byte[] returnBytes) {
                                if (!isSuc) {
                                    if (mOnReceiveWriteListener != null) {
                                        mOnReceiveWriteListener.onReceiveWrite(false, returnBytes);
                                    }
                                    return;
                                }
                                /* ... the block list ... */
                                deviceManager.requestRfmFelicaCmd(PH_EXCHANGE_BUFFER_CONT, numBlocksTemp, blockListTemp, new DeviceManager.onReceiveRfmFelicaCmdListener() {
                                    @Override
                                    public void onReceiveRfmFelicaCmd(boolean isSuc, byte[] returnBytes) {
                                        if (!isSuc) {
                                            if (mOnReceiveWriteListener != null) {
                                                mOnReceiveWriteListener.onReceiveWrite(false, returnBytes);
                                            }
                                            return;
                                        }
                                        /* ... and the block data. */
                                        deviceManager.requestRfmFelicaCmd(PH_EXCHANGE_BUFFER_LAST, numBlocksTemp, blockDataTemp, new DeviceManager.onReceiveRfmFelicaCmdListener() {
                                            @Override
                                            public void onReceiveRfmFelicaCmd(boolean isSuc, byte[] returnBytes) {
                                                if ( (!isSuc) || (returnBytes.length < 2) || (returnBytes[0] != 0) ) {
                                                    if (mOnReceiveWriteListener != null) {
                                                        mOnReceiveWriteListener.onReceiveWrite(false, returnBytes);
                                                    }
                                                    return;
                                                }
                                                if (mOnReceiveWriteListener != null) {
                                                    mOnReceiveWriteListener.onReceiveWrite(true, returnBytes);
                                                }
                                            }
                                        });
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }
    public interface onReceiveWriteListener {
        public void onReceiveWrite(boolean isSuc, byte[] returnBytes);
    }

    /**
     * 写，同步阻塞方式，注意：不能在蓝牙初始化的线程里运行
     * @param bNumServices  Number of Services.
     * @param pServiceList  List of Services.
     * @param bNumBlocks  Number of Blocks to send.
     * @param pBlockList  List of Blocks to read.
     * @param pBlockData  Block data to write.
     * @return            true:写入成功   false：写入失败
     * @throws CardNoResponseException
     *                  操作无响应时会抛出异常
     */
    public boolean write(byte bNumServices,
                         byte[]  pServiceList,
                         byte  bNumBlocks,
                         byte[]  pBlockList,
                         byte[]  pBlockData) throws CardNoResponseException {
        final boolean[] isCmdRunSucFlag = {false};

        final Semaphore semaphore = new Semaphore(0);

        write(bNumServices, pServiceList, bNumBlocks, pBlockList, pBlockData, new onReceiveWriteListener() {
            @Override
            public void onReceiveWrite(boolean isCmdRunSuc, byte[] returnBytes) {
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
}
