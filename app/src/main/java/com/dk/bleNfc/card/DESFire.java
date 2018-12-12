package com.dk.bleNfc.card;

import com.dk.bleNfc.DeviceManager.DeviceManager;
import com.dk.bleNfc.Exception.CardNoResponseException;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by Administrator on 2016/9/21.
 */
public class DESFire extends Card {
    public onReceiveCmdListener mOnReceiveCmdListener;

    public DESFire(DeviceManager deviceManager) {
        super(deviceManager);
    }

    public DESFire(DeviceManager deviceManager, byte[] uid, byte[] atr) {
        super(deviceManager, uid, atr);
    }

    //DESFire卡指令通道回调
    public interface onReceiveCmdListener{
        public void onReceiveCmdExchange(boolean isCmdRunSuc, byte[] bytCmdRtnData);
    }

    //DESFire卡指令通道
    public void cmd(byte[] cmdBytes, onReceiveCmdListener listener) {
        mOnReceiveCmdListener = listener;
        deviceManager.requestRfmSentApduCmd(cmdBytes, new DeviceManager.onReceiveRfmSentApduCmdListener() {
            @Override
            public void onReceiveRfmSentApduCmd(boolean isCmdRunSuc, byte[] bytApduRtnData) {
                if (mOnReceiveCmdListener != null) {
                    mOnReceiveCmdListener.onReceiveCmdExchange(isCmdRunSuc, bytApduRtnData);
                }
            }
        });
    }

    /**
     * B cpu卡指令传输，同步阻塞方式，注意：不能在蓝牙初始化的线程里运行
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

        deviceManager.requestRfmSentApduCmd(data, new DeviceManager.onReceiveRfmSentApduCmdListener() {
            @Override
            public void onReceiveRfmSentApduCmd(boolean isCmdRunSuc, byte[] bytApduRtnData) {
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
