package com.dk.bleNfc.card;

import com.dk.bleNfc.DeviceManager.DeviceManager;
import com.dk.bleNfc.Exception.CardNoResponseException;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by Administrator on 2016/9/21.
 */
public class Card {
    public final static int CAR_NO_RESPONSE_TIME_MS = 2000;  //卡片无响应等等时间2秒
    public final static String CAR_NO_RESPONSE = "卡片超时无响应！";
    public final static String CAR_RUN_CMD_FAIL = "卡片运行指令失败！";

    public DeviceManager deviceManager;
    public byte[] uid;
    public byte[] atr;
    public onReceiveCloseListener mOnReceiveCloseListener;

    public Card(DeviceManager deviceManager) {
        this.deviceManager = deviceManager;
    }

    public Card(DeviceManager deviceManager, byte[] uid, byte[] atr) {
        this.deviceManager = deviceManager;
        this.uid = uid;
        this.atr = atr;
    }

    public String uidToString() {
        if ( (uid == null) || (uid.length == 0) ) {
            return null;
        }
        StringBuffer stringBuffer = new StringBuffer();
        for (int i=0; i<uid.length; i++) {
            stringBuffer.append(String.format("%02x", uid[i]));
        }
        return stringBuffer.toString();
    }

    //卡片掉电回调
    public interface onReceiveCloseListener {
        public void onReceiveClose(boolean isOk);
    }

    /**
     * 卡片掉电，同步阻塞方式，注意：不能在蓝牙初始化的线程里运行
     * @param listener 操作结果会通过onReceiveCloseListener回调
     */
    public void close(onReceiveCloseListener listener) {
        mOnReceiveCloseListener = listener;
        deviceManager.requestRfmClose(new DeviceManager.onReceiveRfmCloseListener() {
            @Override
            public void onReceiveRfmClose(boolean blnIsCloseSuc) {
                if (mOnReceiveCloseListener != null) {
                    mOnReceiveCloseListener.onReceiveClose(blnIsCloseSuc);
                }
            }
        });
    }

    /**
     * 卡片掉电，同步阻塞方式，注意：不能在蓝牙初始化的线程里运行
     * @return         true - 操作成功
     *                  false - 操作失败
     * @throws CardNoResponseException
     *                  操作无响应时会抛出异常
     */
    public boolean close() throws CardNoResponseException{
        final byte[][] returnBytes = new byte[1][1];
        final boolean[] isCmdRunSucFlag = {false};

        final Semaphore semaphore = new Semaphore(0);
        returnBytes[0] = null;

        close(new onReceiveCloseListener() {
            @Override
            public void onReceiveClose(boolean isOk) {
                if (isOk) {
                    isCmdRunSucFlag[0] = true;
                }
                else {
                    isCmdRunSucFlag[0] = false;
                }
                semaphore.release();
            }
        });

        try {
            semaphore.tryAcquire(CAR_NO_RESPONSE_TIME_MS * 1, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new CardNoResponseException("卡片无响应");
        }

        return isCmdRunSucFlag[0];
    }
}
