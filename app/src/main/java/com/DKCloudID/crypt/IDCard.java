package com.DKCloudID.crypt;

import android.content.Context;
import android.nfc.tech.NfcB;
import android.util.Log;

import java.io.IOException;

public class IDCard {
    private final static String TAG = "IDCard";
    private MsgCrypt msgCrypt;
    private Context mContext;

    private onReceiveScheduleListener mOnReceiveScheduleListener;

    public IDCard(Context context) {
        mContext = context;
        msgCrypt = new MsgCrypt(mContext);
    }

    //进度回调
    public interface onReceiveScheduleListener{
        void onReceiveSchedule(int rate);
    }

    /**
     * 获取身份证数据，带进度回调
     * @param nfcB - 系统NFC获取到的B类型的tag
     * @return 身份证数据
     * @throws DKCloudIDException 解析出错会进此异常
     */
    public IDCardData getIDCardData(final NfcB nfcB) throws DKCloudIDException {
        return getIDCardData(nfcB, null);
    }

    /**
     * 获取身份证数据，带进度回调
     * @param nfcB - 系统NFC获取到的B类型的tag
     * @param listener - 进度回调
     * @return 身份证数据
     * @throws DKCloudIDException 解析出错会进此异常
     */
    public IDCardData getIDCardData(final NfcB nfcB, onReceiveScheduleListener listener) throws DKCloudIDException {
        byte[] msgReturnBytes;

        mOnReceiveScheduleListener = listener;

        try {
            if (!nfcB.isConnected()) {
                nfcB.connect();
            }

            ApduTransceive.setTag(nfcB);

            msgReturnBytes = msgCrypt.getDnData();
            if (msgReturnBytes == null) {
                if ( !nfcB.isConnected() ) {
                    throw new DKCloudIDException("身份证已拿开");
                }
                else {
                    throw new DKCloudIDException("身份证读取错误");
                }
            }

            DKCloudID dkCloudID = new DKCloudID();
            if ( !dkCloudID.isConnected() ) {
                throw new DKCloudIDException("服务器连接失败");
            }
            Log.d(TAG, "向服务器发送数据：" + StringTool.byteHexToSting(msgReturnBytes));
            byte[] cloudReturnByte = dkCloudID.dkCloudTcpDataExchange(msgReturnBytes);
            Log.d(TAG, "接收到服务器数据：" + StringTool.byteHexToSting(cloudReturnByte));

            Log.d(TAG, "正在解析:1%");
            int schedule = 1;
            if ( (cloudReturnByte != null) && (cloudReturnByte.length >= 2)
                    && ((cloudReturnByte[0] == 0x03) || (cloudReturnByte[0] == 0x04)) ) {
                if ( mOnReceiveScheduleListener != null ) {
                    mOnReceiveScheduleListener.onReceiveSchedule(schedule);
                }
            }

            while (true) {
                if ( (cloudReturnByte == null) || (cloudReturnByte.length < 2)
                        || ((cloudReturnByte[0] != 0x03) && (cloudReturnByte[0] != 0x04)) ) {

                    dkCloudID.Close();
                    if ( cloudReturnByte == null ) {
                        throw new DKCloudIDException("服务器返回数据为空");
                    }
                    else if (cloudReturnByte[0] == 0x05) {
                        throw new DKCloudIDException("解析失败, 请重新读卡");
                    }
                    else if (cloudReturnByte[0] == 0x06) {
                        throw new DKCloudIDException("该设备未授权, 请联系www.derkiot.com获取授权");
                    }
                    else if (cloudReturnByte[0] == 0x07) {
                        throw new DKCloudIDException("该设备已被禁用, 请联系www.derkiot.com");
                    }
                    else if (cloudReturnByte[0] == 0x08) {
                        throw new DKCloudIDException("该账号已被禁用, 请联系www.derkiot.com");
                    }
                    else if (cloudReturnByte[0] == 0x09) {
                        throw new DKCloudIDException("余额不足, 请联系www.derkiot.com充值");
                    }
                    else {
                        throw new DKCloudIDException("未知错误");
                    }
                }
                else if ((cloudReturnByte[0] == 0x04) && (cloudReturnByte.length > 300)) {
                    byte[] decrypted = new byte[cloudReturnByte.length - 3];
                    System.arraycopy(cloudReturnByte, 3, decrypted, 0, decrypted.length);

                    final IDCardData idCardData = new IDCardData(decrypted, mContext);
                    Log.d(TAG, "解析成功：" + idCardData.toString());

                    dkCloudID.Close();
                    return idCardData;
                }

                msgReturnBytes = msgCrypt.analyze(cloudReturnByte);

                if (msgReturnBytes == null) {
                    dkCloudID.Close();
                    if ( !nfcB.isConnected() ) {
                        throw new DKCloudIDException("身份证已拿开");
                    }
                    else {
                        throw new DKCloudIDException("身份证读取错误");
                    }
                }
                else if (msgReturnBytes.length == 2) {
                    dkCloudID.Close();
                    throw new DKCloudIDException("解析出错：" + String.format("%d", ((msgReturnBytes[0] & 0xff) << 8) | (msgReturnBytes[1] & 0xff) ));
                }

                Log.d(TAG, "向服务器发送数据：" + StringTool.byteHexToSting(msgReturnBytes));
                cloudReturnByte = dkCloudID.dkCloudTcpDataExchange(msgReturnBytes);
                Log.d(TAG, "接收到服务器数据：" + StringTool.byteHexToSting(cloudReturnByte));

                Log.d(TAG, String.format("正在解析%%%d", (int)((++schedule) * 100 / 4.0)));
                if ( mOnReceiveScheduleListener != null ) {
                    mOnReceiveScheduleListener.onReceiveSchedule((int)(schedule * 100 / 4.0));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            try {
                nfcB.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            throw new DKCloudIDException("未检测到身份证，请将身份证放置于感应区域");
        }
    }
}
