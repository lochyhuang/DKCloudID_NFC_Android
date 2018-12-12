package com.dk.bleNfc.DeviceManager;

/**
 * Created by lochy on 16/1/19.
 */
public abstract class DeviceManagerCallback {
    //获取设备连接回调
    public void onReceiveConnectBtDevice(boolean blnIsConnectSuc) {}

    //断开设备连接回调
    public void onReceiveDisConnectDevice(boolean blnIsDisConnectDevice) {}

    //检测设备状态回调
    public void onReceiveConnectionStatus(boolean blnIsConnection) {}

    //初始化密钥回调
    public void onReceiveInitCiphy (boolean blnIsInitSuc) {}

    //设备认证回调
    //authData：设备返回的8字节认证码
    public void onReceiveDeviceAuth(byte[] authData) {}

//    //设置设备信息回调
//    public void onReceiveSetDeviceInitInfo(BleDeviceInitInfo bleDeviceInitInfo) {}
//
//    //获取设备信息回调
//    public void onReceiveGetDeviceVisualInfo(BleDeviceVisualInfo bleDeviceVisualInfo) {}

    //非接寻卡回调
    public void onReceiveRfnSearchCard(boolean blnIsSus, int cardType, byte[] bytCardSn, byte[] bytCarATS) {}

    //发送APDU指令回调
    public void onReceiveRfmSentApduCmd(byte[] bytApduRtnData) {}

    //发送BPDU指令回调
    public void onReceiveRfmSentBpduCmd(byte[] bytBpduRtnData) {}

    //关闭非接模块回调
    public void onReceiveRfmClose(boolean blnIsCloseSuc) {}

    //SUCA获取余额指令
    public void onReceiveRfmSuicaBalance(boolean blnIsSuc, byte[] bytBalance) {}

    //Felica读指令
    public void onReceiveRfmFelicaRead(boolean blnIsReadSuc, byte[] bytBlockData) {}

    //UL指令通道
    public void onReceiveRfmUltralightCmd(byte[] bytUlRtnData) {}

    //按键返回回调
    public void onReceiveButtonEnter(byte keyValue) {}

}
