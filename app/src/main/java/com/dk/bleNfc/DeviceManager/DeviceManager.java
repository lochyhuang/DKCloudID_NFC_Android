package com.dk.bleNfc.DeviceManager;

import android.content.Context;

import com.dk.bleNfc.BleManager.BleManager;
import com.dk.bleNfc.card.CpuCard;
import com.dk.bleNfc.card.DESFire;
import com.dk.bleNfc.card.FeliCa;
import com.dk.bleNfc.card.Iso14443bCard;
import com.dk.bleNfc.card.Iso15693Card;
import com.dk.bleNfc.card.Mifare;
import com.dk.bleNfc.card.Ntag21x;
import com.dk.bleNfc.card.Ultralight;
import com.dk.bleNfc.Tool.StringTool;

import java.io.UnsupportedEncodingException;

/**
 * Created by lochy on 16/1/19.
 */
public class DeviceManager {
    public final static String SDK_VERSIONS = "v2.2.0 20180621";
    private DeviceManagerCallback mDeviceManagerCallback = null;
    public BleManager bleManager = null;

    public CpuCard cpuCard;
    public Iso14443bCard iso14443bCard;
    public DESFire desFire;
    public Iso15693Card iso15693Card;
    public Mifare mifare;
    public Ntag21x ntag21x;
    public Ultralight ultralight;
    public FeliCa feliCa;
    public int mCardType;

    public onReceiveBatteryVoltageDeviceListener mOnReceiveBatteryVoltageDeviceListener;
    public onReceiveVersionsDeviceListener mOnReceiveVersionsDeviceListener;
    public onReceiveConnectBtDeviceListener mOnReceiveConnectBtDeviceListener;
    public onReceiveDisConnectDeviceListener mOnReceiveDisConnectDeviceListener;
    public onReceiveConnectionStatusListener mOnReceiveConnectionStatusListener;
    public onReceiveInitCiphyListener mOnReceiveInitCiphyListener;
    public onReceiveDeviceAuthListener mOnReceiveDeviceAuthListener;
    public onReceiveRfnSearchCardListener mOnReceiveRfnSearchCardListener;
    public onReceiveRfmSentApduCmdListener mOnReceiveRfmSentApduCmdListener;
    public onReceiveRfmSentBpduCmdListener mOnReceiveRfmSentBpduCmdListener;
    public onReceiveRfmCloseListener mOnReceiveRfmCloseListener;
    public onReceiveRfmSuicaBalanceListener mOnReceiveRfmSuicaBalanceListener;
    public onReceiveRfmFelicaReadListener mOnReceiveRfmFelicaReadListener;
    public onReceiveRfmFelicaCmdListener mOnReceiveRfmFelicaCmdListener;
    public onReceiveRfmUltralightCmdListener mOnReceiveRfmUltralightCmdListener;
    public onReceiveRfmMifareAuthListener mOnReceiveRfmMifareAuthListener;
    public onReceiveRfmMifareDataExchangeListener mOnReceiveRfmMifareDataExchangeListener;
    public onReceivePalTestChannelListener mOnReceivePalTestChannelListener;
    public onReceiveOpenBeepCmdListener mOnReceiveOpenBeepCmdListener;
    public onReceiveRfIso15693ReadSingleBlockListener mOnReceiveRfIso15693ReadSingleBlockListener;
    public onRecevieRfIso15693ReadMultipleBlockListener mOnRecevieRfIso15693ReadMultipleBlockListener;
    public onReceiveRfIso15693WriteSingleBlockListener mOnReceiveRfIso15693WriteSingleBlockListener;
    public onReceiveRfIso15693WriteMultipleBlockListener mOnReceiveRfIso15693WriteMultipleBlockListener;
    public onReceiveRfIso15693LockBlockListener mOnReceiveRfIso15693LockBlockListener;
    public onReceiveRfIso15693CmdListener mOnReceiveRfIso15693CmdListener;
    public onReceiveIso15693LongReadListener mOnReceiveIso15693LongReadListener;
    public onReceiveAntiLostSwitchListener mOnReceiveAntiLostSwitchListener;
    public onReceiveButtonEnterListener mOnReceiveButtonEnterListener;
    public onReceiveAndroidFastParamsListener mOnReceiveAndroidFastParamsListener;
    public onReceivePSamResetListener mOnReceivePSamResetListener;
    public onReceivePSamPowerDownListener mOnReceivePSamPowerDownListener;
    public onReceivePSamApduListener mOnReceivePSamApduListener;
    public onReceiveAutoSearchCardListener mOnReceiveAutoSearchCardListener;
    public onReceiveChangeBleNameListener mOnReceiveChangeBleNameListener;
    public onReceiveUlLongReadListener mOnReceiveUlLongReadListener;
    public onReceiveUlLongWriteListener mOnReceiveUlLongWriteListener;
    public onReceiveSaveSerialNumberListener mOnReceiveSaveSerialNumberListener;
    public onReceiveGetSerialNumberListener mOnReceiveGetSerialNumberListener;

    public final static byte  CARD_TYPE_NO_DEFINE = 0x00;           //卡片类型：未定义
    public final static byte  CARD_TYPE_ISO4443_A = 0x01;        //卡片类型ISO14443-A
    public final static byte  CARD_TYPE_ISO4443_B = 0x02;        //卡片类型ISO14443-B
    public final static byte  CARD_TYPE_FELICA = 0x03;           //卡片类型Felica
    public final static byte  CARD_TYPE_MIFARE = 0x04;           //卡片类型Mifare卡
    public final static byte  CARD_TYPE_ISO15693 = 0x05;        //卡片类型iso15693卡
    public final static byte  CARD_TYPE_ULTRALIGHT = 0x06;      //RF_TYPE_MF
    public final static byte  CARD_TYPE_DESFire = 0x07;         //DESFire卡

    public final static byte  BUTTON_VALUE_SHORT_ENTER = 1;   //按键短按
    public final static byte  BUTTON_VALUE_LONG_ENTER = 2;    //按键长按按

    public boolean autoSearchCardFlag = false;

    public DeviceManager(Context context) {
        bleManager = new BleManager(context);
        bleManager.setOnReceiveDataListener(new BleManager.onReceiveDataListener() {
            @Override
            public void OnReceiverData(byte[] data) {
                comByteManager.rcvData(data);
            }
        });

        bleManager.setOnBledisconnectListener(new BleManager.onBleDisconnectListener() {
            @Override
            public void onBleDisconnect() {
                autoSearchCardFlag = false;
                if (mDeviceManagerCallback != null) {
                    mDeviceManagerCallback.onReceiveDisConnectDevice(true);
                }
                if (mOnReceiveDisConnectDeviceListener != null) {
                    mOnReceiveDisConnectDeviceListener.onReceiveDisConnectDevice(true);
                }
            }
        });
    }

    public  Object getCard() {
        switch (mCardType) {
            case CARD_TYPE_ISO4443_A:
                return cpuCard;
            case CARD_TYPE_ISO4443_B:
                return iso14443bCard;
            case CARD_TYPE_FELICA:
                return feliCa;
            case CARD_TYPE_MIFARE:
                return mifare;
            case CARD_TYPE_ISO15693:
                return iso15693Card;
            case CARD_TYPE_ULTRALIGHT:
                return ntag21x;
            case CARD_TYPE_DESFire:
                return desFire;
            default:
                return null;
        }
    }

    public void setCallBack(DeviceManagerCallback callBack) {
        mDeviceManagerCallback = callBack;
    }

    //获取设备电池电压（V）回调接口
    public interface onReceiveBatteryVoltageDeviceListener{
        public void onReceiveBatteryVoltageDevice(double voltage);
    }

    //获取设备版本回调接口
    public interface onReceiveVersionsDeviceListener{
        public void onReceiveVersionsDevice(byte versions);
    }

    //获取设备连接回调接口
    public interface onReceiveConnectBtDeviceListener {
        public void onReceiveConnectBtDevice(boolean blnIsConnectSuc);
    }

    //断开设备连接回调接口
    public interface onReceiveDisConnectDeviceListener {
        public void onReceiveDisConnectDevice(boolean blnIsDisConnectDevice);
    }

    //检测设备状态回调接口
    public interface onReceiveConnectionStatusListener {
        public void onReceiveConnectionStatus(boolean blnIsConnection);
    }

    //初始化密钥回调接口
    public interface onReceiveInitCiphyListener {
        public void onReceiveInitCiphy (boolean blnIsInitSuc);
    }

    //设备认证回调接口
    //authData：设备返回的8字节认证码
    public interface onReceiveDeviceAuthListener {
        public void onReceiveDeviceAuth(byte[] authData);
    }

    //非接寻卡回调接口
    public interface onReceiveRfnSearchCardListener {
        public void onReceiveRfnSearchCard(boolean blnIsSus, int cardType, byte[] bytCardSn, byte[] bytCarATS);
    }

    //发送APDU指令回调接口
    public interface onReceiveRfmSentApduCmdListener {
        public void onReceiveRfmSentApduCmd(boolean isCmdRunSuc, byte[] bytApduRtnData);
    }

    //发送BPDU指令回调接口
    public interface onReceiveRfmSentBpduCmdListener {
        public void onReceiveRfmSentBpduCmd(boolean isCmdRunSuc, byte[] bytBpduRtnData);
    }

    //关闭非接模块回调接口
    public interface onReceiveRfmCloseListener {
        public void onReceiveRfmClose(boolean blnIsCloseSuc);
    }

    //获取suica余额回调接口
    public interface onReceiveRfmSuicaBalanceListener {
        public void onReceiveRfmSuicaBalance(boolean blnIsSuc, byte[] bytBalance);
    }

    //读Felica回调接口
    public interface onReceiveRfmFelicaReadListener{
        public void onReceiveRfmFelicaRead(boolean blnIsReadSuc, byte[] bytBlockData);
    }

    //Felica指令通道回调接口
    public interface onReceiveRfmFelicaCmdListener {
        public void onReceiveRfmFelicaCmd(boolean isSuc, byte[] returnBytes);
    }

    //Felica指令通道回调接口
    public interface onReceiveOpenBeepCmdListener {
        public void onReceiveOpenBeepCmd(boolean isSuc);
    }

    //UL卡指令接口
    public interface onReceiveRfmUltralightCmdListener {
        public void onReceiveRfmUltralightCmd(boolean isCmdRunSuc, byte[] bytUlRtnData);
    }

    //Mifare卡验证密码回调接口
    public interface onReceiveRfmMifareAuthListener {
        public void onReceiveRfmMifareAuth(boolean isSuc);
    }

    //开启自动寻卡回调接口
    public interface onReceiveAutoSearchCardListener{
        void onReceiveAutoSearchCard(boolean isSuc);
    }

    //Mifare数据交换通道回调接口
    public interface onReceiveRfmMifareDataExchangeListener{
        public void onReceiveRfmMifareDataExchange(boolean isSuc, byte[] returnData);
    }

    //iso15693读单个块回调接口
    public interface onReceiveRfIso15693ReadSingleBlockListener {
        void onReceiveRfIso15693ReadSingleBlock(boolean isSuc, byte[] returnData);
    }

    //iso15693读多个块回调接口
    public interface onRecevieRfIso15693ReadMultipleBlockListener {
        void onRecevieRfIso15693ReadMultipleBlock(boolean isSuc, byte[] returnData);
    }

    //iso15693写单个块回调接口
    public interface onReceiveRfIso15693WriteSingleBlockListener{
        void onReceiveRfIso15693WriteSingleBlock(boolean isSuc);
    }

    //iso15693写多个块回调接口
    public interface onReceiveRfIso15693WriteMultipleBlockListener{
        void onReceiveRfIso15693WriteMultipleBlock(boolean isSuc);
    }

    //iso15693锁住块回调接口
    public interface onReceiveRfIso15693LockBlockListener{
        void onReceiveRfIso15693LockBlock(boolean isSuc);
    }

    //iso15693指令通道回调接口
    public interface onReceiveRfIso15693CmdListener{
        void onReceiveRfIso15693Cmd(boolean isSuc, byte[] returnData);
    }

    //iso15693卡任意长度读回调接口
    public interface onReceiveIso15693LongReadListener {
        void onReceiveIso15693LongRead(boolean isSuc, byte[] returnData);
    }

    //防丢器功能开关回调接口antiLostSwitchCmdBytes
    public interface onReceiveAntiLostSwitchListener{
        void onReceiveAntiLostSwitch(boolean isSuc);
    }

    //按键回调接口
    public interface onReceiveButtonEnterListener{
        void onReceiveButtonEnter(byte keyValue);
    }

    //协议测试通道回调
    public interface onReceivePalTestChannelListener{
        public void onReceivePalTestChannel(byte[] returnData);
    }

    //开启快速传输回调接口androidFastParamsCmdBytes
    public interface onReceiveAndroidFastParamsListener {
        void onReceiveAndroidFastParams(boolean isSuc);
    }

    //PSam上电复位通道接口
    public interface onReceivePSamResetListener {
        void  onReceivePSamReset(boolean isSuc, byte[] returnData);
    }

    //PSam掉电接口
    public interface onReceivePSamPowerDownListener {
        void onReceivePSamPowerDown(boolean isSuc);
    }

    //PSam apdu传输通道回调接口
    public interface onReceivePSamApduListener {
        void onReceivePSamApdu(boolean isSuc, byte[] returnData);
    }

    //修改蓝牙名称回调接口
    public interface onReceiveChangeBleNameListener{
        void onReceiveChangeBleName(boolean isSuc);
    }

    //ul卡任意长度读回调接口
    public interface onReceiveUlLongReadListener {
        void onReceiveUlLongRead(boolean isSuc, byte[] returnData);
    }

    //ul卡任意长度写回调接口
    public interface onReceiveUlLongWriteListener {
        void onReceiveUlLongWrite(boolean isSuc);
    }

    //获取序列号回调接口
    public interface onReceiveGetSerialNumberListener {
        void onReceiveGetSerialNumber(boolean isSuc, byte[] serialNumberBytes);
    }

    //保存序列号回调接口
    public interface onReceiveSaveSerialNumberListener {
        void onReceiveSaveSerialNumber(boolean isSuc);
    }

    //根据蓝牙MAC地址连接设备接口
    public void requestConnectBleDevice(String strBleAddr) {
        if (strBleAddr == null) {
            return;
        }

        boolean status = bleManager.connect(strBleAddr, new BleManager.onBleConnectListener() {
            @Override
            public void onBleConnect(boolean isConnectSucceed) {
                if (mDeviceManagerCallback != null) {
                    if (isConnectSucceed) {
                        mDeviceManagerCallback.onReceiveConnectBtDevice(true);
                    } else {
                        mDeviceManagerCallback.onReceiveConnectBtDevice(false);
                    }
                }
            }
        });

        if (!status) {
            if (mDeviceManagerCallback != null) {
                mDeviceManagerCallback.onReceiveConnectBtDevice(false);
            }
        }
    }

    //根据蓝牙MAC地址连接设备接口
    public void requestConnectBleDevice(String strBleAddr, onReceiveConnectBtDeviceListener l) {
        mOnReceiveConnectBtDeviceListener = l;
        boolean status = bleManager.connect(strBleAddr, new BleManager.onBleConnectListener() {
            @Override
            public void onBleConnect(boolean isConnectSucceed) {
                if (isConnectSucceed) {
                    if (mDeviceManagerCallback != null) {
                        mDeviceManagerCallback.onReceiveConnectBtDevice(true);
                    }
                    if (mOnReceiveConnectBtDeviceListener != null) {
                        mOnReceiveConnectBtDeviceListener.onReceiveConnectBtDevice(true);
                    }
                }
                else {
                    if (mDeviceManagerCallback != null) {
                        mDeviceManagerCallback.onReceiveConnectBtDevice(false);
                    }
                    if (mOnReceiveConnectBtDeviceListener != null) {
                        mOnReceiveConnectBtDeviceListener.onReceiveConnectBtDevice(false);
                    }
                }
            }
        });

        if (!status) {
            if (mDeviceManagerCallback != null) {
                mDeviceManagerCallback.onReceiveConnectBtDevice(false);
            }

            if (mOnReceiveConnectBtDeviceListener != null) {
                mOnReceiveConnectBtDeviceListener.onReceiveConnectBtDevice(false);
            }
        }
    }

    //断开连接接口
    public void requestDisConnectDevice() {
        bleManager.cancelConnect();
    }
    public void requestDisConnectDevice(onReceiveDisConnectDeviceListener l) {
        mOnReceiveDisConnectDeviceListener = l;
        bleManager.cancelConnect(new BleManager.onBleDisconnectListener() {
            @Override
            public void onBleDisconnect() {
                autoSearchCardFlag = false;
                if (mDeviceManagerCallback != null) {
                    mDeviceManagerCallback.onReceiveDisConnectDevice(true);
                }
                if (mOnReceiveDisConnectDeviceListener != null) {
                    mOnReceiveDisConnectDeviceListener.onReceiveDisConnectDevice(true);
                }
            }
        });
    }

    //检测设备状态接口
    public void requestConnectionStatus() {
        if (mDeviceManagerCallback != null) {
            mDeviceManagerCallback.onReceiveConnectionStatus(bleManager.mConnectionState == BleManager.STATE_CONNECTED);
        }
        if (mOnReceiveConnectionStatusListener != null) {
            mOnReceiveConnectionStatusListener.onReceiveConnectionStatus(bleManager.mConnectionState == BleManager.STATE_CONNECTED);
        }
    }
    public void requestConnectionStatus(onReceiveConnectionStatusListener l) {
        mOnReceiveConnectionStatusListener = l;
        if (mDeviceManagerCallback != null) {
            mDeviceManagerCallback.onReceiveConnectionStatus(bleManager.mConnectionState == BleManager.STATE_CONNECTED);
        }
        if (mOnReceiveConnectionStatusListener != null) {
            mOnReceiveConnectionStatusListener.onReceiveConnectionStatus(bleManager.mConnectionState == BleManager.STATE_CONNECTED);
        }
    }
    public int isConnection(){
        return bleManager.mConnectionState;
    }

    //初始秘钥接口
    public void requestInitCiphy(byte bytKeyType, byte bytKeyVer, byte[] bytKeyValue) {
    }

//    //获取设备信息
//    public void requestGetDeviceVisualInfo(BleDeviceVisualInfo bleDeviceVisualInfo) {
//    }
//
//    //设置设备信息
//    public void requestSetDeviceInitInfo(BleDevInitInfo bleDevInitInfo) {
//    }

    //数据接收完成回调
    private ComByteManagerCallback comByteManagerCallback = new ComByteManagerCallback() {
        @Override
        public void onRcvBytes(boolean isSuc, byte[] rcvBytes) {
            super.onRcvBytes(isSuc, rcvBytes);
            switch ( (byte)(( (int)comByteManager.getCmd() & 0xff ) - 1) ) {
                case ComByteManager.GET_VERSIONS_COM:
                    if (mOnReceiveVersionsDeviceListener != null) {
                        if (rcvBytes != null && rcvBytes.length == 1) {
                            mOnReceiveVersionsDeviceListener.onReceiveVersionsDevice(rcvBytes[0]);
                        }
                        else {
                            mOnReceiveVersionsDeviceListener.onReceiveVersionsDevice((byte) 0);
                        }
                    }
                    break;
                case ComByteManager.GET_BT_VALUE_COM:
                    if (mOnReceiveBatteryVoltageDeviceListener != null) {
                        if (rcvBytes != null && rcvBytes.length == 2) {
                            double v = ( ((rcvBytes[0] & 0x00ff) << 8) | (rcvBytes[1] & 0x00ff) ) / 100.0;
                            mOnReceiveBatteryVoltageDeviceListener.onReceiveBatteryVoltageDevice(v);
                        }
                        else {
                            mOnReceiveBatteryVoltageDeviceListener.onReceiveBatteryVoltageDevice(0.0);
                        }
                    }
                    break;
                case ComByteManager.ANTENNA_OFF_COM:
                    if (mDeviceManagerCallback != null) {
                        mDeviceManagerCallback.onReceiveRfmClose(true);
                    }
                    if (mOnReceiveRfmCloseListener != null) {
                        mOnReceiveRfmCloseListener.onReceiveRfmClose(true);
                    }
                    break;
                case ComByteManager.ACTIVATE_PICC_COM:
                    cpuCard = null;
                    mifare = null;
                    iso15693Card = null;
                    iso14443bCard = null;
                    feliCa = null;
                    ntag21x = null;
                    ultralight = null;
                    desFire = null;
                    if (comByteManager.getCmdRunStatus()) {
                        byte uidBytes[];
                        byte atrBytes[];

                        int cardType = rcvBytes[0];
                        mCardType = cardType;
                        if (cardType == CARD_TYPE_ISO4443_A) {
                            uidBytes = new byte[4];
                            System.arraycopy(rcvBytes, 1, uidBytes, 0, 4);
                            atrBytes = new byte[rcvBytes.length - 5];
                            System.arraycopy(rcvBytes, 5, atrBytes, 0, rcvBytes.length - 5);
                            cpuCard = new CpuCard(DeviceManager.this, uidBytes, atrBytes);
                        }
                        else if (cardType == CARD_TYPE_MIFARE) {
                            uidBytes = new byte[4];
                            System.arraycopy(rcvBytes, 1, uidBytes, 0, 4);
                            atrBytes = new byte[rcvBytes.length - 5];
                            System.arraycopy(rcvBytes, 5, atrBytes, 0, rcvBytes.length - 5);
                            mifare = new Mifare(DeviceManager.this, uidBytes, atrBytes);
                        }
                        else if (cardType == CARD_TYPE_ISO15693) {
                            uidBytes = new byte[8];
                            atrBytes = new byte[1];
                            System.arraycopy(rcvBytes, 1, uidBytes, 0, 8);
                            iso15693Card = new Iso15693Card(DeviceManager.this, uidBytes, atrBytes);
                        }
                        else if (cardType == CARD_TYPE_ULTRALIGHT) {
                            uidBytes = new byte[7];
                            System.arraycopy(rcvBytes, 1, uidBytes, 0, 7);
                            atrBytes = new byte[rcvBytes.length - 8];
                            System.arraycopy(rcvBytes, 8, atrBytes, 0, rcvBytes.length - 8);
                            ultralight = new Ultralight(DeviceManager.this, uidBytes, atrBytes);
                            ntag21x = new Ntag21x(DeviceManager.this, uidBytes, atrBytes);
                        }
                        else if (cardType == CARD_TYPE_DESFire) {
                            uidBytes = new byte[7];
                            System.arraycopy(rcvBytes, 1, uidBytes, 0, 7);
                            atrBytes = new byte[rcvBytes.length - 8];
                            System.arraycopy(rcvBytes, 8, atrBytes, 0, rcvBytes.length - 8);
                            desFire = new DESFire(DeviceManager.this, uidBytes, atrBytes);
                        }
                        else if (cardType == CARD_TYPE_ISO4443_B){
                            uidBytes = new byte[4];
                            atrBytes = new byte[rcvBytes.length - 1];
                            System.arraycopy(rcvBytes, 1, atrBytes, 0, rcvBytes.length - 1);
                            iso14443bCard = new Iso14443bCard(DeviceManager.this, uidBytes, atrBytes);
                        }
                        else if (cardType == CARD_TYPE_FELICA) {
                            uidBytes = new byte[4];
                            atrBytes = new byte[rcvBytes.length - 1];
                            System.arraycopy(rcvBytes, 1, atrBytes, 0, rcvBytes.length - 1);
                            feliCa = new FeliCa(DeviceManager.this, uidBytes, atrBytes);
                        }
                        else {
                            uidBytes = null;
                            atrBytes = null;
                            if (mDeviceManagerCallback != null) {
                                mDeviceManagerCallback.onReceiveRfnSearchCard(false, cardType, uidBytes, atrBytes);
                            }
                            if (mOnReceiveRfnSearchCardListener != null) {
                                mOnReceiveRfnSearchCardListener.onReceiveRfnSearchCard(false, cardType, uidBytes, atrBytes);
                            }
                        }

                        if (mDeviceManagerCallback != null) {
                            mDeviceManagerCallback.onReceiveRfnSearchCard(true, cardType, uidBytes, atrBytes);
                        }
                        if (mOnReceiveRfnSearchCardListener != null) {
                            mOnReceiveRfnSearchCardListener.onReceiveRfnSearchCard(true, cardType, uidBytes, atrBytes);
                        }
                    }
                    else {
                        if (mDeviceManagerCallback != null) {
                            mDeviceManagerCallback.onReceiveRfnSearchCard(false, 0, null, null);
                        }
                        if (mOnReceiveRfnSearchCardListener != null) {
                            mOnReceiveRfnSearchCardListener.onReceiveRfnSearchCard(false, 0, null, null);
                        }
                    }
                    break;
                case ComByteManager.APDU_COM:
                    if (mDeviceManagerCallback != null) {
                        mDeviceManagerCallback.onReceiveRfmSentApduCmd(rcvBytes);
                    }
                    if (mOnReceiveRfmSentApduCmdListener != null) {
                        mOnReceiveRfmSentApduCmdListener.onReceiveRfmSentApduCmd(isSuc,rcvBytes);
                    }
                    break;
                case ComByteManager.BPDU_COM:
                    if (mDeviceManagerCallback != null) {
                        mDeviceManagerCallback.onReceiveRfmSentBpduCmd(rcvBytes);
                    }
                    if (mOnReceiveRfmSentBpduCmdListener != null) {
                        mOnReceiveRfmSentBpduCmdListener.onReceiveRfmSentBpduCmd(isSuc,rcvBytes);
                    }
                    break;
                case ComByteManager.GET_SUICA_BALANCE_COM:
                    if (mDeviceManagerCallback != null) {
                        mDeviceManagerCallback.onReceiveRfmSuicaBalance(isSuc, rcvBytes);
                    }
                    if (mOnReceiveRfmSuicaBalanceListener != null) {
                        mOnReceiveRfmSuicaBalanceListener.onReceiveRfmSuicaBalance(isSuc, rcvBytes);
                    }
                    break;
                case ComByteManager.BEEP_OPEN_COM:
                    if (mOnReceiveOpenBeepCmdListener != null) {
                        mOnReceiveOpenBeepCmdListener.onReceiveOpenBeepCmd(isSuc);
                    }
                    break;
                case ComByteManager.FELICA_READ_COM:
                    if (mDeviceManagerCallback != null) {
                        mDeviceManagerCallback.onReceiveRfmFelicaRead(isSuc, rcvBytes);
                    }
                    if (mOnReceiveRfmFelicaReadListener != null) {
                        mOnReceiveRfmFelicaReadListener.onReceiveRfmFelicaRead(isSuc, rcvBytes);
                    }
                    break;
                case ComByteManager.FELICA_COM:
                    if (mOnReceiveRfmFelicaCmdListener != null) {
                        mOnReceiveRfmFelicaCmdListener.onReceiveRfmFelicaCmd(isSuc, rcvBytes);
                    }
                    break;

                case ComByteManager.ULTRALIGHT_CMD:
                    if (mDeviceManagerCallback != null) {
                        mDeviceManagerCallback.onReceiveRfmUltralightCmd(rcvBytes);
                    }
                    if (mOnReceiveRfmUltralightCmdListener != null) {
                        mOnReceiveRfmUltralightCmdListener.onReceiveRfmUltralightCmd(isSuc, rcvBytes);
                    }
                    break;
                case ComByteManager.ULTRALIGHT_LONG_READ:
                    if (mOnReceiveUlLongReadListener != null) {
                        mOnReceiveUlLongReadListener.onReceiveUlLongRead(isSuc, rcvBytes);
                    }
                    break;
                case ComByteManager.ULTRALIGHT_LONG_WRITE:
                    if (mOnReceiveUlLongWriteListener != null) {
                        mOnReceiveUlLongWriteListener.onReceiveUlLongWrite(isSuc);
                    }
                    break;
                case ComByteManager.MIFARE_AUTH_COM:
                    if (mOnReceiveRfmMifareAuthListener != null) {
                        mOnReceiveRfmMifareAuthListener.onReceiveRfmMifareAuth(isSuc);
                    }
                    break;
                case ComByteManager.MIFARE_COM:
                    if (mOnReceiveRfmMifareDataExchangeListener != null) {
                        mOnReceiveRfmMifareDataExchangeListener.onReceiveRfmMifareDataExchange(isSuc, rcvBytes);
                    }
                    break;
                case ComByteManager.ISO15693_READ_SINGLE_BLOCK_COM:
                    if (mOnReceiveRfIso15693ReadSingleBlockListener != null) {
                        mOnReceiveRfIso15693ReadSingleBlockListener.onReceiveRfIso15693ReadSingleBlock(isSuc, rcvBytes);
                    }
                    break;
                case ComByteManager.ISO15693_READ_MULTIPLE_BLOCK_COM:
                    if (mOnRecevieRfIso15693ReadMultipleBlockListener != null) {
                        mOnRecevieRfIso15693ReadMultipleBlockListener.onRecevieRfIso15693ReadMultipleBlock(isSuc, rcvBytes);
                    }
                    break;
                case ComByteManager.ISO15693_WRITE_SINGLE_BLOCK_COM:
                    if (mOnReceiveRfIso15693WriteSingleBlockListener != null) {
                        mOnReceiveRfIso15693WriteSingleBlockListener.onReceiveRfIso15693WriteSingleBlock(isSuc);
                    }
                    break;
                case ComByteManager.ISO15693_WRITE_MULTIPLE_BLOCK_COM:
                    if (mOnReceiveRfIso15693WriteMultipleBlockListener != null) {
                        mOnReceiveRfIso15693WriteMultipleBlockListener.onReceiveRfIso15693WriteMultipleBlock(isSuc);
                    }
                    break;
                case ComByteManager.ISO15693_CMD:
                    if (mOnReceiveRfIso15693CmdListener != null) {
                        mOnReceiveRfIso15693CmdListener.onReceiveRfIso15693Cmd(isSuc, rcvBytes);
                    }
                    break;
                case ComByteManager.ISO15693_LONG_READ:
                    if (mOnReceiveIso15693LongReadListener != null) {
                        mOnReceiveIso15693LongReadListener.onReceiveIso15693LongRead(isSuc, rcvBytes);
                    }
                    break;
                case ComByteManager.ANTI_LOST_SWITCH_COM:
                    if (mOnReceiveAntiLostSwitchListener != null) {
                        mOnReceiveAntiLostSwitchListener.onReceiveAntiLostSwitch(isSuc);
                    }
                    break;
                case ComByteManager.BUTTON_INPUT_COM:
                    if (mDeviceManagerCallback != null) {
                        mDeviceManagerCallback.onReceiveButtonEnter(rcvBytes[0]);
                    }
                    if (mOnReceiveButtonEnterListener != null) {
                        mOnReceiveButtonEnterListener.onReceiveButtonEnter(rcvBytes[0]);
                    }
                    break;
                case ComByteManager.PAL_TEST_CHANNEL:
                    if (mOnReceivePalTestChannelListener != null) {
                        mOnReceivePalTestChannelListener.onReceivePalTestChannel(rcvBytes);
                    }
                    break;
                case ComByteManager.ANDROID_FAST_PARAMS_COM:
                    if (mOnReceiveAndroidFastParamsListener != null) {
                        mOnReceiveAndroidFastParamsListener.onReceiveAndroidFastParams(isSuc);
                    }
                    break;
                case ComByteManager.ISO7816_RESET_CMD:
                    if (mOnReceivePSamResetListener != null) {
                        mOnReceivePSamResetListener.onReceivePSamReset(isSuc, rcvBytes);
                    }
                    break;
                case ComByteManager.ISO7816_POWE_OFF_CMD:
                    if (mOnReceivePSamPowerDownListener != null) {
                        mOnReceivePSamPowerDownListener.onReceivePSamPowerDown(isSuc);
                    }
                    break;
                case ComByteManager.AUTO_SEARCH_CARD_COM:
                    autoSearchCardFlag = isSuc;
                    if (mOnReceiveAutoSearchCardListener != null) {
                        mOnReceiveAutoSearchCardListener.onReceiveAutoSearchCard(isSuc);
                    }
                    break;
                case ComByteManager.ISO7816_CMD:
                    if (mOnReceivePSamApduListener != null) {
                        mOnReceivePSamApduListener.onReceivePSamApdu(isSuc, rcvBytes);
                    }
                case ComByteManager.CHANGE_BLE_NAME_COM:
                    if (mOnReceiveChangeBleNameListener != null) {
                        mOnReceiveChangeBleNameListener.onReceiveChangeBleName(isSuc);
                    }
                    break;
                case ComByteManager.SAVE_SERIAL_NUMBER_COM:
                    if (mOnReceiveSaveSerialNumberListener != null) {
                        mOnReceiveSaveSerialNumberListener.onReceiveSaveSerialNumber(isSuc);
                    }
                    break;
                case ComByteManager.GET_SERIAL_NUMBER_COM:
                    if (mOnReceiveGetSerialNumberListener != null) {
                        mOnReceiveGetSerialNumberListener.onReceiveGetSerialNumber(isSuc, rcvBytes);
                    }
                    break;
                default:
                    break;
            }
        }
    };
    private ComByteManager comByteManager = new ComByteManager(comByteManagerCallback);


    public void requestBatteryVoltageDevice(onReceiveBatteryVoltageDeviceListener listener) {
        mOnReceiveBatteryVoltageDeviceListener = listener;
        bleManager.writeDataToCharacteristic(comByteManager.getBtValueComByte());
    }

    public void requestVersionsDevice(onReceiveVersionsDeviceListener listener) {
        mOnReceiveVersionsDeviceListener = listener;
        bleManager.writeDataToCharacteristic(comByteManager.getVersionsComByte());
    }

    //非接寻卡接口：（连接成功后收到操作类回调即可开始寻卡）
    //bytCardType 读卡类型
    //0x00：自动寻卡
    //0x01：寻Mifare卡或者Ul卡（CPU卡视为M1卡）
    public void requestRfmSearchCard(byte bytCardType) {
        bleManager.writeDataToCharacteristic(comByteManager.AActivityComByte(bytCardType));
    }
    public void requestRfmSearchCard(byte bytCardType, onReceiveRfnSearchCardListener l) {
        mOnReceiveRfnSearchCardListener = l;
        bleManager.writeDataToCharacteristic(comByteManager.AActivityComByte(bytCardType));
    }

    //自动寻卡
    //en：true-开启自动寻卡，false：关闭自动寻卡
    //delayMs：寻卡间隔,单位 10毫秒
    //bytCardType: ISO14443_P3-寻M1/UL卡，ISO14443_P4-寻CPU卡
    public void requestRfmAutoSearchCard(boolean en, byte delayMs, byte bytCardType) {
        bleManager.writeDataToCharacteristic(comByteManager.autoSearchCardCmdBytes(en, delayMs, bytCardType));
    }
    public void requestRfmAutoSearchCard(boolean en, byte delayMs, byte bytCardType, onReceiveAutoSearchCardListener l) {
        mOnReceiveAutoSearchCardListener = l;
        bleManager.writeDataToCharacteristic(comByteManager.autoSearchCardCmdBytes(en, delayMs, bytCardType));
    }

    //发送APDU指令
    public void requestRfmSentApduCmd(byte[] bytApduData) {
        bleManager.writeDataToCharacteristic(comByteManager.rfApduCmdByte(bytApduData));
    }
    public void requestRfmSentApduCmd(byte[] bytApduData, onReceiveRfmSentApduCmdListener l) {
        mOnReceiveRfmSentApduCmdListener = l;
        bleManager.writeDataToCharacteristic(comByteManager.rfApduCmdByte(bytApduData));
    }

    //发送BPDU指令
    public void requestRfmSentBpduCmd(byte[] bytBpduData) {
        bleManager.writeDataToCharacteristic(comByteManager.rfBpduCmdByte(bytBpduData));
    }
    public void requestRfmSentBpduCmd(byte[] bytBpduData, onReceiveRfmSentBpduCmdListener l) {
        mOnReceiveRfmSentBpduCmdListener = l;
        bleManager.writeDataToCharacteristic(comByteManager.rfBpduCmdByte(bytBpduData));
    }

    //关闭非接模块
    public void requestRfmClose() {
        bleManager.writeDataToCharacteristic(comByteManager.rfPowerOffComByte());
    }
    public void requestRfmClose(onReceiveRfmCloseListener l) {
        mOnReceiveRfmCloseListener = l;
        bleManager.writeDataToCharacteristic(comByteManager.rfPowerOffComByte());
    }

    //读取Suica余额指令
    public void requestRfmSuicaBalance() {
        bleManager.writeDataToCharacteristic(comByteManager.getSuicaBalanceCmdByte());
    }
    public void requestRfmSuicaBalance(onReceiveRfmSuicaBalanceListener l) {
        mOnReceiveRfmSuicaBalanceListener = l;
        bleManager.writeDataToCharacteristic(comByteManager.getSuicaBalanceCmdByte());
    }

    //读Felica指令
    //systemCode: 两字节，高位在前
    //blockAddr： 两字节，高位在前
    public void requestRfmFelicaRead(byte[] systemCode, byte[] blockAddr) {
        bleManager.writeDataToCharacteristic(comByteManager.readFeliCaCmdByte(systemCode, blockAddr));
    }
    //systemCode: 两字节，高位在前
    //blockAddr： 两字节，高位在前
    public void requestRfmFelicaRead(byte[] systemCode, byte[] blockAddr, onReceiveRfmFelicaReadListener l) {
        mOnReceiveRfmFelicaReadListener = l;
        bleManager.writeDataToCharacteristic(comByteManager.readFeliCaCmdByte(systemCode, blockAddr));
    }

    //Felica指令通道
    public void requestRfmFelicaCmd(int wOption, int wN, byte[] cmdBytes) {
        bleManager.writeDataToCharacteristic(comByteManager.felicaCmdByte(wOption, wN, cmdBytes));
    }
    public void requestRfmFelicaCmd(int wOption, int wN, byte[] cmdBytes, onReceiveRfmFelicaCmdListener l) {
        mOnReceiveRfmFelicaCmdListener = l;
        bleManager.writeDataToCharacteristic(comByteManager.felicaCmdByte(wOption, wN, cmdBytes));
    }

    //Ultralight指令通道
    public void requestRfmUltralightCmd(byte[] bytUlCmdData) {
        bleManager.writeDataToCharacteristic(comByteManager.ultralightCmdByte(bytUlCmdData));
    }
    public void requestRfmUltralightCmd(byte[] bytUlCmdData, onReceiveRfmUltralightCmdListener l) {
        mOnReceiveRfmUltralightCmdListener = l;
        bleManager.writeDataToCharacteristic(comByteManager.ultralightCmdByte(bytUlCmdData));
    }

    //UL卡快速读
    //startAddress：要读的起始地址
    //number：要读的块数量（一个块4 byte）， 0 < number < 0x3f
    public void requestRfmUltralightLongRead(byte startAddress, int number) {
        if (number < 0 || number > 0x3f) {
            return;
        }
        bleManager.writeDataToCharacteristic(comByteManager.ultralightLongReadCmdBytes(startAddress, number));
    }
    public void requestRfmUltralightLongRead(byte startAddress, int number, onReceiveUlLongReadListener l) {
        mOnReceiveUlLongReadListener = l;
        if (number < 0 || number > 0x3f) {
            return;
        }
        bleManager.writeDataToCharacteristic(comByteManager.ultralightLongReadCmdBytes(startAddress, number));
    }

    //UL卡快速写
    //startAddress：要写的起始地址
    //data：要写的数据
    public void requestRfmUltralightLongWrite(byte startAddress, byte[] data) {
        bleManager.writeDataToCharacteristic(comByteManager.ultralightLongWriteCmdBytes(startAddress, data));
    }
    public void requestRfmUltralightLongWrite(byte startAddress, byte[] data, onReceiveUlLongWriteListener l) {
        mOnReceiveUlLongWriteListener = l;
        bleManager.writeDataToCharacteristic(comByteManager.ultralightLongWriteCmdBytes(startAddress, data));
    }

    //Mifare卡验证密码
    public void requestRfmMifareAuth(byte bBlockNo, byte bKeyType, byte[] pKey, byte[] pUid) {
        bleManager.writeDataToCharacteristic(comByteManager.rfMifareAuthCmdByte(bBlockNo, bKeyType, pKey, pUid));
    }
    public void requestRfmMifareAuth(byte bBlockNo, byte bKeyType, byte[] pKey, byte[] pUid, onReceiveRfmMifareAuthListener l) {
        mOnReceiveRfmMifareAuthListener = l;
        bleManager.writeDataToCharacteristic(comByteManager.rfMifareAuthCmdByte(bBlockNo, bKeyType, pKey, pUid));
    }

    //Mifare卡数据交换通道
    public void requestRfmMifareDataExchange(byte[] dataByte) {
        bleManager.writeDataToCharacteristic(comByteManager.rfMifareDataExchangeCmdByte(dataByte));
    }
    public void requestRfmMifareDataExchange(byte[] dataByte, onReceiveRfmMifareDataExchangeListener l) {
        mOnReceiveRfmMifareDataExchangeListener = l;
        bleManager.writeDataToCharacteristic(comByteManager.rfMifareDataExchangeCmdByte(dataByte));
    }

    //ISO15693读单个块数据指令
    //uid:要读的卡片的uid，必须4个字节
    //addr：要读的块地址
    public void requestRfmIso15693ReadSingleBlock(byte uid[], byte addr) {
        bleManager.writeDataToCharacteristic(comByteManager.iso15693ReadSingleBlockCmdBytes(uid, addr));
    }
    public void requestRfmIso15693ReadSingleBlock(byte uid[], byte addr, onReceiveRfIso15693ReadSingleBlockListener l) {
        mOnReceiveRfIso15693ReadSingleBlockListener = l;
        bleManager.writeDataToCharacteristic(comByteManager.iso15693ReadSingleBlockCmdBytes(uid, addr));
    }

    //ISO15693读多个块数据指令
    //uid:要读的卡片的uid，必须4个字节
    //addr：要读的块地址
    //number:要读的块数量,必须大于0
    public void requestRfmIso15693ReadMultipleBlock(byte uid[], byte addr, byte number) {
        bleManager.writeDataToCharacteristic(comByteManager.iso15693ReadMultipleBlockCmdBytes(uid, addr, number));
    }
    public void requestRfmIso15693ReadMultipleBlock(byte uid[], byte addr, byte number, onRecevieRfIso15693ReadMultipleBlockListener l) {
        mOnRecevieRfIso15693ReadMultipleBlockListener = l;
        bleManager.writeDataToCharacteristic(comByteManager.iso15693ReadMultipleBlockCmdBytes(uid, addr, number));
    }

    //ISO15693写一个块
    //uid:要写的卡片的uid，必须4个字节
    //addr：要写卡片的块地址
    //writeData:要写的数据，必须4个字节
    public void requestRfmIso15693WriteSingleBlock(byte uid[], byte addr, byte writeData[]) {
        bleManager.writeDataToCharacteristic(comByteManager.iso15693WriteSingleBlockCmdBytes(uid, addr, writeData));
    }
    public void requestRfmIso15693WriteSingleBlock(byte uid[], byte addr, byte writeData[], onReceiveRfIso15693WriteSingleBlockListener l) {
        mOnReceiveRfIso15693WriteSingleBlockListener = l;
        bleManager.writeDataToCharacteristic(comByteManager.iso15693WriteSingleBlockCmdBytes(uid, addr, writeData));
    }

    //ISO15693写多个块
    //uid:要写的卡片的uid，必须4个字节
    //addr：要写的块地址
    //number:要写的块数量,必须大于0
    //writeData: 要写的数据，必须number * 4字节
    public void requestRfmIso15693WriteMultipleBlock(byte uid[], byte addr, byte number, byte writeData[]) {
        bleManager.writeDataToCharacteristic(comByteManager.iso15693WriteMultipleBlockCmdBytes(uid, addr, number, writeData));
    }
    public void requestRfmIso15693WriteMultipleBlock(byte uid[],
                                                     byte addr,
                                                     byte number,
                                                     byte writeData[],
                                                     onReceiveRfIso15693WriteMultipleBlockListener l) {
        mOnReceiveRfIso15693WriteMultipleBlockListener = l;
        bleManager.writeDataToCharacteristic(comByteManager.iso15693WriteMultipleBlockCmdBytes(uid, addr, number, writeData));
    }

    //ISO15693锁住一个块
    //uid：要写的卡片的UID，必须4个字节
    //addr：要锁住的块地址
    public void requestRfmIso15693LockBlock(byte uid[], byte addr) {
        bleManager.writeDataToCharacteristic(comByteManager.iso15693LockBlockCmdBytes(uid, addr));
    }
    public void requestRfmIso15693LockBlock(byte uid[], byte addr, onReceiveRfIso15693LockBlockListener l) {
        mOnReceiveRfIso15693LockBlockListener = l;
        bleManager.writeDataToCharacteristic(comByteManager.iso15693LockBlockCmdBytes(uid, addr));
    }

    //ISO15693指令通道
    public void requestRfmIso15693CmdBytes(byte[] cmdBytes) {
        bleManager.writeDataToCharacteristic(comByteManager.iso15693CmdBytes(cmdBytes));
    }
    public void requestRfmIso15693CmdBytes(byte[] cmdBytes, onReceiveRfIso15693CmdListener l) {
        mOnReceiveRfIso15693CmdListener = l;
        bleManager.writeDataToCharacteristic(comByteManager.iso15693CmdBytes(cmdBytes));
    }

    //ISO15693快速读
    //startAddress：要读的起始地址
    //number：要读的块数量（一个块4 byte）， 0 < number < 0x3f
    public void requestRfmIso15693LongRead(byte startAddress, int number) {
        if (number < 0 || number > 0x3f) {
            return;
        }
        bleManager.writeDataToCharacteristic(comByteManager.iso15693LongReadCmdBytes(startAddress, number));
    }
    public void requestRfmIso15693LongRead(byte startAddress, int number, onReceiveIso15693LongReadListener l) {
        mOnReceiveIso15693LongReadListener = l;
        if (number < 0 || number > 0x3f) {
            return;
        }
        bleManager.writeDataToCharacteristic(comByteManager.iso15693LongReadCmdBytes(startAddress, number));
    }

    //防丢器开关指令
    //s：true：打开防丢器功能 false：关闭防丢器功能
    public void requestAntiLostSwitch(boolean s) {
        bleManager.writeDataToCharacteristic(comByteManager.antiLostSwitchCmdBytes(s));
    }
    public void requestAntiLostSwitch(boolean s, onReceiveAntiLostSwitchListener l) {
        mOnReceiveAntiLostSwitchListener = l;
        bleManager.writeDataToCharacteristic(comByteManager.antiLostSwitchCmdBytes(s));
    }

    //开启/关闭快速传输，只对安卓手机有效
    //s：true：打开快速传输 false：关闭快速传输
    public void requestAndroidFastParams(boolean s) {
        bleManager.writeDataToCharacteristic(comByteManager.androidFastParamsCmdBytes(s));
    }
    public void requestAndroidFastParams(boolean s, onReceiveAndroidFastParamsListener l) {
        mOnReceiveAndroidFastParamsListener = l;
        bleManager.writeDataToCharacteristic(comByteManager.androidFastParamsCmdBytes(s));
    }

    //获取按键键值指令
    public void requestButtonEnter(onReceiveButtonEnterListener l) {
        mOnReceiveButtonEnterListener = l;
    }

    //通讯协议测试通道
    public void requestPalTestChannel(byte[] dataBytes, onReceivePalTestChannelListener l) {
        mOnReceivePalTestChannelListener = l;
        bleManager.writeDataToCharacteristic(comByteManager.getTestChannelBytes(dataBytes));
    }

    //打卡蜂鸣器指令
    //opneTimeMs: 打卡蜂鸣器的时间，0~0xffff，单位ms
    public void requestOpenBeep(int openTimeMs, onReceiveOpenBeepCmdListener l) {
        mOnReceiveOpenBeepCmdListener = l;
        bleManager.writeDataToCharacteristic(comByteManager.openBeepCmdBytes(openTimeMs));
    }

    //打开蜂鸣器指令
    //onDelayMs: 打开蜂鸣器时间：0~0xffff，单位ms
    //offDelayMs：关闭蜂鸣器时间：0~0xffff，单位ms
    //n：蜂鸣器响多少声：0~255
    public void requestOpenBeep(int onDelayMs, int offDelayMs, int n, onReceiveOpenBeepCmdListener l) {
        mOnReceiveOpenBeepCmdListener = l;
        bleManager.writeDataToCharacteristic(comByteManager.openBeepCmdBytes(onDelayMs, offDelayMs, n));
    }

    //PSam上电复位指令
    public void requestPSamReset(onReceivePSamResetListener l) {
        mOnReceivePSamResetListener = l;
        bleManager.writeDataToCharacteristic(comByteManager.resetPSamCmdBytes());
    }

    //PSam掉电
    public void requestPSamPowerDown(onReceivePSamPowerDownListener l) {
        mOnReceivePSamPowerDownListener = l;
        bleManager.writeDataToCharacteristic(comByteManager.PSamPowerDownCmdBytes());
    }

    //修改蓝牙名称
    //bleName：蓝牙名
    public void requestChangeBleName(String bleName,onReceiveChangeBleNameListener l) {
        mOnReceiveChangeBleNameListener = l;
        byte[] bytes = new byte[0];//先把字符串按gb2312转成byte数组
        try {
            String str = java.net.URLEncoder.encode(bleName,"utf-8");
            bytes = StringTool.urlStringToBytes(str);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        bleManager.writeDataToCharacteristic(comByteManager.changeBleNameCmdBytes(bytes));
    }

    //保存序列号
    //serialNumberBytes：序列号，必须8字节
    public void requestSaveSerialNumber(byte[] serialNumberBytes, onReceiveSaveSerialNumberListener l) {
        mOnReceiveSaveSerialNumberListener = l;
        bleManager.writeDataToCharacteristic(comByteManager.saveSerialNumberCmdBytes(serialNumberBytes));
    }

    //获取序列号
    public void requestGetSerialNumber(onReceiveGetSerialNumberListener l) {
        mOnReceiveGetSerialNumberListener = l;
        bleManager.writeDataToCharacteristic(comByteManager.getSerialNumberCmdBytes());
    }

    //PSam apdu指令传输
    public void requestPSamApdu(byte[] data, onReceivePSamApduListener l) {
        mOnReceivePSamApduListener = l;
        bleManager.writeDataToCharacteristic(comByteManager.PSamApduCmdBytes(data));
    }

    //设置APDU传输模式
    //TransferMode Byte---
    //UNENCRYPTED: 明文传输APDU
    //ENCRYPTED: 密文传输APDU(3DES)
    //Keydata Byte[]---- 用于加密的3DES密钥（只在TransferMode = ENCRYPTED时有用）
    public void requestApduTransferMode(byte TransferMode, byte[] Keydata) {
    }

    //通用APDU指令处理
    //ApduCommand: 完整的APDU指令
    public void sendApduCmd (byte[] ApduCommand) {
    }
}
