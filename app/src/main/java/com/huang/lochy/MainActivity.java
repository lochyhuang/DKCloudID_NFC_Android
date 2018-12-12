package com.huang.lochy;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.dk.bleNfc.BleManager.BleManager;
import com.dk.bleNfc.BleManager.Scanner;
import com.dk.bleNfc.BleManager.ScannerCallback;
import com.dk.bleNfc.BleNfcDeviceService;
import com.dk.bleNfc.DeviceManager.BleNfcDevice;
import com.dk.bleNfc.DeviceManager.ComByteManager;
import com.dk.bleNfc.DeviceManager.DeviceManager;
import com.dk.bleNfc.DeviceManager.DeviceManagerCallback;
import com.dk.bleNfc.Exception.CardNoResponseException;
import com.dk.bleNfc.Exception.DeviceNoResponseException;
import com.dk.bleNfc.Tool.StringTool;
import com.dk.bleNfc.card.CpuCard;
import com.dk.bleNfc.card.FeliCa;
import com.dk.bleNfc.card.Iso14443bCard;
import com.dk.bleNfc.card.Iso15693Card;
import com.dk.bleNfc.card.Mifare;
import com.dk.bleNfc.card.Ntag21x;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MainActivity extends Activity {
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    BleNfcDeviceService mBleNfcDeviceService;
    private BleNfcDevice bleNfcDevice;
    private Scanner mScanner;
    private Button searchButton = null;
    private EditText msgText = null;
    private ProgressDialog readWriteDialog = null;

    private static volatile StringBuffer msgBuffer;
    private BluetoothDevice mNearestBle = null;
    private Lock mNearestBleLock = new ReentrantLock();// 锁对象
    private int lastRssi = -100;

    private int err_cnt = 0;
    private int suc_cnt = 0;

    //private String ipString = "113.104.238.251";
    private String ipString = "derk.tpddns.cn";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        msgBuffer = new StringBuffer();

        searchButton = (Button)findViewById(R.id.searchButton);
        Button changeBleNameButton = (Button) findViewById(R.id.changeBleNameButton);
        msgText = (EditText)findViewById(R.id.msgText);
        Button clearButton = (Button) findViewById(R.id.clearButton);

        msgText.setFocusable(false);

        readWriteDialog = new ProgressDialog(MainActivity.this);
        readWriteDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        // 设置ProgressDialog 标题
        readWriteDialog.setTitle("请稍等");
        // 设置ProgressDialog 提示信息
        readWriteDialog.setMessage("正在读写数据……");
        // 设置ProgressDialog 标题图标
        readWriteDialog.setIcon(R.drawable.ic_launcher);

        clearButton.setOnClickListener(new claerButtonListener());
        searchButton.setOnClickListener(new StartSearchButtonListener());
        changeBleNameButton.setOnClickListener(new changeBleNameButtonListener());

        if ( !gpsIsOPen(MainActivity.this) ) {
            System.out.println("log:" + "GPS未打开！");

            final AlertDialog.Builder normalDialog =
                    new AlertDialog.Builder(MainActivity.this);
            normalDialog.setTitle("请打开GPS");
            normalDialog.setMessage("搜索蓝牙需要打开GPS");
            normalDialog.setPositiveButton("确定",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //...To-do
                            // 转到手机设置界面，用户设置GPS
                            Intent intent = new Intent(
                                    Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivityForResult(intent, 0); // 设置完成后返回到原来的界面
                        }
                    });
            normalDialog.setNegativeButton("关闭",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //...To-do
                        }
                    });
            // 显示
            normalDialog.show();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //未拥有权限，提示用户允许app使用定位权限
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
            }
            else {  //已经拥有权限
                //ble_nfc服务初始化
                Intent gattServiceIntent = new Intent(this, BleNfcDeviceService.class);
                bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
            }
        }
        else {    //API 23以下的，不需要动态申请权限
            //ble_nfc服务初始化
            Intent gattServiceIntent = new Intent(this, BleNfcDeviceService.class);
            bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        }

        //获取存储权限
        MainActivity.isGrantExternalRW(MainActivity.this);

        //ble_nfc服务初始化
        Intent gattServiceIntent = new Intent(this, BleNfcDeviceService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        msgText.setText("BLE_NFC Demo v2.2.0 20180409");
    }

    /**
     * 判断GPS是否开启，GPS或者AGPS开启一个就认为是开启的
     * @param context
     * @return true 表示开启
     */
    public static final boolean gpsIsOPen(final Context context) {
        LocationManager locationManager
                = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        // 通过GPS卫星定位，定位级别可以精确到街（通过24颗卫星定位，在室外和空旷的地方定位准确、速度快）
        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        // 通过WLAN或移动网络(3G/2G)确定的位置（也称作AGPS，辅助GPS定位。主要用于在室内或遮盖物（建筑群或茂密的深林等）密集的地方定位）
        boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (gps || network) {
            return true;
        }

        return false;
    }

    /**
     * 获取储存权限
     * @param activity
     * @return
     */

    public static boolean isGrantExternalRW(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && activity.checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            activity.requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, 1);

            return false;
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {  //申请权限成功
                    // TODO request success
                    //ble_nfc服务初始化
                    Intent gattServiceIntent = new Intent(this, BleNfcDeviceService.class);
                    bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
                }
                break;
        }
    }

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            BleNfcDeviceService mBleNfcDeviceService = ((BleNfcDeviceService.LocalBinder) service).getService();
            bleNfcDevice = mBleNfcDeviceService.bleNfcDevice;
            mScanner = mBleNfcDeviceService.scanner;
            mBleNfcDeviceService.setDeviceManagerCallback(deviceManagerCallback);
            mBleNfcDeviceService.setScannerCallback(scannerCallback);

            //开始搜索设备
            searchNearestBleDevice();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBleNfcDeviceService = null;
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (mBleNfcDeviceService != null) {
            mBleNfcDeviceService.setScannerCallback(scannerCallback);
            mBleNfcDeviceService.setDeviceManagerCallback(deviceManagerCallback);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (readWriteDialog != null) {
            readWriteDialog.dismiss();
        }

        unbindService(mServiceConnection);
    }

    //Scanner 回调
    private ScannerCallback scannerCallback = new ScannerCallback() {
        @Override
        public void onReceiveScanDevice(BluetoothDevice device, int rssi, byte[] scanRecord) {
            super.onReceiveScanDevice(device, rssi, scanRecord);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { //StringTool.byteHexToSting(scanRecord.getBytes())
                System.out.println("Activity搜到设备：" + device.getName()
                        + " 信号强度：" + rssi
                        + " scanRecord：" + StringTool.byteHexToSting(scanRecord));
            }

            //搜索蓝牙设备并记录信号强度最强的设备
            if ( ( (scanRecord != null) && (StringTool.byteHexToSting(scanRecord).contains("017f5450"))) || ( (device.getName() != null) && (device.getName().contains("HZ-1501"))) ) {  //从广播数据中过滤掉其它蓝牙设备
                if (rssi < -55) {
                    return;
                }
                msgBuffer.append("搜到设备：").append(device.getName()).append(" 信号强度：").append(rssi).append("\r\n");
                handler.sendEmptyMessage(0);
                if (mNearestBle != null) {
                    if (rssi > lastRssi) {
                        mNearestBleLock.lock();
                        try {
                            mNearestBle = device;
                        }finally {
                            mNearestBleLock.unlock();
                        }
                    }
                }
                else {
                    mNearestBleLock.lock();
                    try {
                        mNearestBle = device;
                    }finally {
                        mNearestBleLock.unlock();
                    }
                    lastRssi = rssi;
                }
            }
        }

        @Override
        public void onScanDeviceStopped() {
            super.onScanDeviceStopped();
        }
    };

    //设备操作类回调
    private DeviceManagerCallback deviceManagerCallback = new DeviceManagerCallback() {
        @Override
        public void onReceiveConnectBtDevice(boolean blnIsConnectSuc) {
            super.onReceiveConnectBtDevice(blnIsConnectSuc);
            if (blnIsConnectSuc) {
                System.out.println("Activity设备连接成功");
                msgBuffer.delete(0, msgBuffer.length());
                msgBuffer.append("设备连接成功!\r\n");
                if (mNearestBle != null) {
                    msgBuffer.append("设备名称：").append(bleNfcDevice.getDeviceName()).append("\r\n");
                }
                msgBuffer.append("信号强度：").append(lastRssi).append("dB\r\n");
                msgBuffer.append("SDK版本：" + BleNfcDevice.SDK_VERSIONS + "\r\n");

                //连接上后延时500ms后再开始发指令
                try {
                    Thread.sleep(500L);
                    handler.sendEmptyMessage(3);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onReceiveDisConnectDevice(boolean blnIsDisConnectDevice) {
            super.onReceiveDisConnectDevice(blnIsDisConnectDevice);
            System.out.println("Activity设备断开链接");
            msgBuffer.delete(0, msgBuffer.length());
            msgBuffer.append("设备断开链接!");
            handler.sendEmptyMessage(0);
        }

        @Override
        public void onReceiveConnectionStatus(boolean blnIsConnection) {
            super.onReceiveConnectionStatus(blnIsConnection);
            System.out.println("Activity设备链接状态回调");
        }

        @Override
        public void onReceiveInitCiphy(boolean blnIsInitSuc) {
            super.onReceiveInitCiphy(blnIsInitSuc);
        }

        @Override
        public void onReceiveDeviceAuth(byte[] authData) {
            super.onReceiveDeviceAuth(authData);
        }

        @Override
        //寻到卡片回调
        public void onReceiveRfnSearchCard(boolean blnIsSus, int cardType, byte[] bytCardSn, byte[] bytCarATS) {
            super.onReceiveRfnSearchCard(blnIsSus, cardType, bytCardSn, bytCarATS);
            if (!blnIsSus || cardType == BleNfcDevice.CARD_TYPE_NO_DEFINE) {
                return;
            }

            System.out.println("Activity接收到激活卡片回调：UID->" + StringTool.byteHexToSting(bytCardSn) + " ATS->" + StringTool.byteHexToSting(bytCarATS));

            final int cardTypeTemp = cardType;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    boolean isReadWriteCardSuc;
                    try {
                        if (bleNfcDevice.isAutoSearchCard()) {
                            //如果是自动寻卡的，寻到卡后，先关闭自动寻卡
                            bleNfcDevice.stoptAutoSearchCard();
                            isReadWriteCardSuc = readWriteCardDemo(cardTypeTemp);

                            //bleNfcDevice.closeRf();
                            //读卡结束，重新打开自动寻卡
                            startAutoSearchCard();
                        }
                        else {
                            isReadWriteCardSuc = readWriteCardDemo(cardTypeTemp);

                            //如果不是自动寻卡，读卡结束,关闭天线
                            bleNfcDevice.closeRf();
                        }

                        //打开蜂鸣器提示读卡完成
                        if (isReadWriteCardSuc) {
                            bleNfcDevice.openBeep(50, 50, 3);  //读写卡成功快响3声
                        }
                        else {
                            bleNfcDevice.openBeep(100, 100, 2); //读写卡失败慢响2声
                        }
                    } catch (DeviceNoResponseException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

        @Override
        public void onReceiveRfmSentApduCmd(byte[] bytApduRtnData) {
            super.onReceiveRfmSentApduCmd(bytApduRtnData);

            System.out.println("Activity接收到APDU回调：" + StringTool.byteHexToSting(bytApduRtnData));
        }

        @Override
        public void onReceiveRfmClose(boolean blnIsCloseSuc) {
            super.onReceiveRfmClose(blnIsCloseSuc);
        }

        @Override
        //按键返回回调
        public void onReceiveButtonEnter(byte keyValue) {
            if (keyValue == DeviceManager.BUTTON_VALUE_SHORT_ENTER) { //按键短按
                System.out.println("Activity接收到按键短按回调");
                msgBuffer.append("按键短按\r\n");
                handler.sendEmptyMessage(0);
            }
            else if (keyValue == DeviceManager.BUTTON_VALUE_LONG_ENTER) { //按键长按
                System.out.println("Activity接收到按键长按回调");
                msgBuffer.append("按键长按\r\n");
                handler.sendEmptyMessage(0);
            }
        }
    };

    //搜索按键监听
    private class StartSearchButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if ( (bleNfcDevice.isConnection() == BleManager.STATE_CONNECTED) ) {
                bleNfcDevice.requestDisConnectDevice();
                return;
            }

            searchNearestBleDevice();
        }
    }

    //修改蓝牙名称按键监听
    private class changeBleNameButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if ( (bleNfcDevice.isConnection() != BleManager.STATE_CONNECTED) ) {
                msgText.setText("设备未连接，请先连接设备！");
                return;
            }

            final EditText inputEditText = new EditText(MainActivity.this);
            //inputEditText.setInputType(InputType.TYPE_CLASS_TEXT);
            //inputEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(16)});
            inputEditText.setText(ipString);
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("修改IP")
                    .setMessage("请输入新IP地址")
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setView(inputEditText)
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ipString = inputEditText.getText().toString();
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
        }
    }

    //清空显示按键监听
    private class claerButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            msgBuffer.delete(0, msgBuffer.length());
            handler.sendEmptyMessage(0);
        }
    }

    //打开防丢器功能监听
    private class OpenAntiLostButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if ( (bleNfcDevice.isConnection() != BleManager.STATE_CONNECTED) ) {
                msgText.setText("设备未连接，请先连接设备！");
                return;
            }
            bleNfcDevice.requestAntiLostSwitch(true, new DeviceManager.onReceiveAntiLostSwitchListener() {
                @Override
                public void onReceiveAntiLostSwitch(boolean isSuc) {
                    if (isSuc) {
                        msgBuffer.delete(0, msgBuffer.length());
                        msgBuffer.append("打开防丢器功能成功");
                        handler.sendEmptyMessage(0);
                    }
                }
            });
        }
    }

    //开始自动寻卡
    private boolean startAutoSearchCard() throws DeviceNoResponseException {
        //打开自动寻卡，200ms间隔，寻M1/UL卡
        boolean isSuc = false;
        int falseCnt = 0;
        do {
            isSuc = bleNfcDevice.startAutoSearchCard((byte) 20, ComByteManager.ISO14443_P4);
        }while (!isSuc && (falseCnt++ < 10));
        if (!isSuc){
            //msgBuffer.delete(0, msgBuffer.length());
            msgBuffer.append("不支持自动寻卡！\r\n");
            handler.sendEmptyMessage(0);
        }

        return isSuc;
    }

    //读写卡Demo
    private boolean readWriteCardDemo(int cardType) {
        switch (cardType) {
            case DeviceManager.CARD_TYPE_ISO4443_B:  //寻到 B cpu卡
                final Iso14443bCard iso14443bCard = (Iso14443bCard) bleNfcDevice.getCard();
                if (iso14443bCard != null) {
                    msgBuffer.delete(0, msgBuffer.length());
                    msgBuffer.append("寻到ISO14443-B卡->UID:(身份证发送0036000008指令获取UID)\r\n");

                    //客户端请求与本机在20006端口建立TCP连接
                    Socket client = null;
                    try {
                        client = new Socket(ipString, 20006);
                        client.setTcpNoDelay(true);
                        client.setSoTimeout(2000);
                        //获取Socket的输出流，用来发送数据到服务端
                        PrintStream out = new PrintStream(client.getOutputStream());
                        //获取Socket的输入流，用来接收从服务端发送过来的数据
                        BufferedReader buf =  new BufferedReader(new InputStreamReader(client.getInputStream()));

                        msgBuffer.delete(0, msgBuffer.length());
                        msgBuffer.append("\r\n开始解析");
                        handler.sendEmptyMessage(0);

                        out.println("1,123456");
                        String echo = buf.readLine();

                        System.out.println("接收到服务器数据：" + echo);
                        msgBuffer.append("\r\n接收到服务器数据：" + echo);
                        handler.sendEmptyMessage(0);

                        if ( (echo != null) && (echo.indexOf(',') == 1) ) {
                            String statusStr = echo.substring(0, 1);
                            String dataStr = echo.substring(2);
                            byte[] dataBytes = StringTool.hexStringToBytes(dataStr);

                            while (!statusStr.equals("0")) {
                                if (statusStr.equals("3")) {
                                    if (dataBytes == null) {
                                        break;
                                    }
                                    try {
                                        byte[] dataBytes1 = iso14443bCard.transceive(dataBytes);
                                        out.println("3,"+StringTool.byteHexToSting(dataBytes1));
                                        //System.out.println("向服务器发送数据：" + "3,"+StringTool.byteHexToSting(dataBytes1));
                                    } catch (CardNoResponseException e) {
                                        e.printStackTrace();
                                    }

                                    echo = buf.readLine();
                                    //System.out.println("接收到服务器数据：" + echo);
                                    if ( (echo != null) && (echo.indexOf(',') == 1) ) {
                                        statusStr = echo.substring(0, 1);
                                        dataStr = echo.substring(2);
                                        dataBytes = StringTool.hexStringToBytes(dataStr);
                                        msgBuffer.append("\r\n接收到服务器数据：" + dataStr);
                                        handler.sendEmptyMessage(0);
                                    }
                                    else {
                                        break;
                                    }
                                }
                                else if (statusStr.equals("4")) {
                                    final String finalDataStr = dataStr;
                                    if (dataStr.length() < 800) {
                                        err_cnt++;
                                    }
                                    else {
                                        suc_cnt++;
                                        final byte idCardBytes[] = StringTool.hexStringToBytes(finalDataStr);
                                        final IDCardData idCardData = new IDCardData(idCardBytes, this);
                                        msgBuffer.delete(0, msgBuffer.length());
                                        msgBuffer.append("解析成功：" + idCardData.toString());
                                        //显示信息
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                msgText.setText(/*"解析成功次数：" + suc_cnt + "\r\n解析失败次数：" + err_cnt + "\r\n" + */msgBuffer + "\r\n\r\n");
                                            }
                                        });

                                        //显示照片和指纹
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                //获取指纹数据
                                                String fingerprintString = "";
                                                if (idCardData.fingerprintBytes != null && idCardData.fingerprintBytes.length > 0) {
                                                    fingerprintString = "\r\n指纹数据：\r\n" + StringTool.byteHexToSting(idCardData.fingerprintBytes);
                                                }

                                                SpannableString ss = new SpannableString(msgText.getText().toString()+"[smile]");
                                                //得到要显示图片的资源
                                                Drawable d = new BitmapDrawable(idCardData.PhotoBmp); //Drawable.createFromPath("mnt/sdcard/photo.bmp");
                                                if (d != null) {
                                                    //设置高度
                                                    d.setBounds(0, 0, d.getIntrinsicWidth() * 10, d.getIntrinsicHeight() * 10);
                                                    //跨度底部应与周围文本的基线对齐
                                                    ImageSpan span = new ImageSpan(d, ImageSpan.ALIGN_BASELINE);
                                                    //附加图片
                                                    ss.setSpan(span, msgText.getText().length(),msgText.getText().length()+"[smile]".length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                                                    msgText.setText(ss);
                                                }

                                                //显示指纹数据
                                                msgText.append(fingerprintString);
                                            }
                                        });
                                    }
                                    break;
                                }
                            }

                            if (!statusStr.equals("4")) {
                                err_cnt++;
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        err_cnt++;
                    }finally {
                        if(client != null){
                            //如果构造函数建立起了连接，则关闭套接字，如果没有建立起连接，自然不用关闭
                            try {
                                client.close(); //只关闭socket，其关联的输入输出流也会被关闭
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                break;
            case DeviceManager.CARD_TYPE_ISO4443_A:   //寻到A CPU卡
                final CpuCard cpuCard = (CpuCard) bleNfcDevice.getCard();
                if (cpuCard != null) {
                    msgBuffer.delete(0, msgBuffer.length());
                    msgBuffer.append("寻到CPU卡->UID:").append(cpuCard.uidToString()).append("\r\n");
                    handler.sendEmptyMessage(0);
                    try{
                        //选择深圳通主文件
                        byte[] bytApduRtnData = cpuCard.transceive(SZTCard.getSelectMainFileCmdByte());
                        if (bytApduRtnData.length <= 2) {
                            System.out.println("不是深圳通卡，当成银行卡处理！");
                            //选择储蓄卡交易文件
                            String cpuCardType;
                            bytApduRtnData = cpuCard.transceive(FinancialCard.getSelectDepositCardPayFileCmdBytes());
                            if (bytApduRtnData.length <= 2) {
                                System.out.println("不是储蓄卡，当成借记卡处理！");
                                //选择借记卡交易文件
                                bytApduRtnData = cpuCard.transceive(FinancialCard.getSelectDebitCardPayFileCmdBytes());
                                if (bytApduRtnData.length <= 2) {
                                    msgBuffer.append("未知CPU卡！");
                                    handler.sendEmptyMessage(0);
                                    return false;
                                }
                                else {
                                    cpuCardType = "储蓄卡";
                                }
                            }
                            else {
                                cpuCardType = "借记卡";
                            }

                            //读交易记录
                            System.out.println("发送APDU指令-读10条交易记录");
                            for (int i = 1; i <= 10; i++) {
                                bytApduRtnData = cpuCard.transceive(FinancialCard.getTradingRecordCmdBytes((byte) i));
                                msgBuffer.append(FinancialCard.extractTradingRecordFromeRturnBytes(bytApduRtnData));
                                handler.sendEmptyMessage(0);
                            }
                        }
                        else {  //深圳通处理流程
                            bytApduRtnData = cpuCard.transceive(SZTCard.getBalanceCmdByte());
                            if (SZTCard.getBalance(bytApduRtnData) == null) {
                                msgBuffer.append("未知CPU卡！");
                                handler.sendEmptyMessage(0);
                                System.out.println("未知CPU卡！");
                                return false;
                            }
                            else {
                                msgBuffer.append("深圳通余额：").append(SZTCard.getBalance(bytApduRtnData));
                                handler.sendEmptyMessage(0);
                                System.out.println("余额：" + SZTCard.getBalance(bytApduRtnData));
                                //读交易记录
                                System.out.println("发送APDU指令-读10条交易记录");
                                for (int i = 1; i <= 10; i++) {
                                    bytApduRtnData = cpuCard.transceive(SZTCard.getTradeCmdByte((byte) i));
                                    msgBuffer.append("\r\n").append(SZTCard.getTrade(bytApduRtnData));
                                    handler.sendEmptyMessage(0);
                                }
                            }
                        }
                    } catch (CardNoResponseException e) {
                        e.printStackTrace();
                        return false;
                    }
                }
                break;
            case DeviceManager.CARD_TYPE_FELICA:  //寻到FeliCa
                FeliCa feliCa = (FeliCa) bleNfcDevice.getCard();
                if (feliCa != null) {
                    msgBuffer.delete(0, msgBuffer.length());
                    msgBuffer.append("读取服务008b中数据块0000的数据：\r\n");
                    handler.sendEmptyMessage(0);
                    byte[] pServiceList = {(byte) 0x8b, 0x00};
                    byte[] pBlockList = {0x00, 0x00, 0x00};
                    try {
                        byte[] pBlockData = feliCa.read((byte) 1, pServiceList, (byte) 1, pBlockList);
                        msgBuffer.append(StringTool.byteHexToSting(pBlockData)).append("\r\n");
                        handler.sendEmptyMessage(0);
                    } catch (CardNoResponseException e) {
                        e.printStackTrace();
                        return false;
                    }
                }
                break;
            case DeviceManager.CARD_TYPE_ULTRALIGHT: //寻到Ultralight卡
                String writeText = System.currentTimeMillis() + "深圳市德科物联技术有限公司，专业非接触式智能卡读写器方案商！深圳市德科物联技术有限公司，专业非接触式智能卡读写器方案商！";
                if (msgText.getText().toString().length() > 0) {
                    writeText = msgText.getText().toString();
                }

                final Ntag21x ntag21x = (Ntag21x) bleNfcDevice.getCard();
                if (ntag21x != null) {
                    msgBuffer.delete(0, msgBuffer.length());
                    msgBuffer.append("寻到Ultralight卡 ->UID:").append(ntag21x.uidToString()).append("\r\n");
                    handler.sendEmptyMessage(0);
                    try {
                        //读写单个块Demo
                        msgBuffer.append("开始读取块0数据：\r\n");
                        handler.sendEmptyMessage(0);
                        byte[] readTempBytes = ntag21x.read((byte) 0);
                        msgBuffer.append("返回：").append(StringTool.byteHexToSting(readTempBytes)).append("\r\n");
                        handler.sendEmptyMessage(0);

                        msgBuffer.append("开始读100个字节数据").append("\r\n");
                        handler.sendEmptyMessage(0);
                        readTempBytes = ntag21x.longReadWithScheduleCallback((byte) 4, (byte) (100 / 4), new Ntag21x.onReceiveScheduleListener() {
                            @Override
                            public void onReceiveSchedule(int rate) {
                                showReadWriteDialog("正在读取数据", rate);
                            }
                        });
                        msgBuffer.append("读取成功：\r\n").append(StringTool.byteHexToSting(readTempBytes)).append("\r\n");
                        showReadWriteDialog("正在读取数据", 100);

                        //读写文本Demo
                        ntag21x.NdefTextWrite("123");
                        String readText = ntag21x.NdefTextRead();
                        msgBuffer.append(readText + "\r\n");
                        handler.sendEmptyMessage(0);

                    } catch (CardNoResponseException e) {
                        e.printStackTrace();
                        msgBuffer.append(e.getMessage()).append("\r\n");
                        showReadWriteDialog("正在写入数据", 0);
                        return false;
                    }
                }
                break;
            case DeviceManager.CARD_TYPE_MIFARE:   //寻到Mifare卡
                final Mifare mifare = (Mifare) bleNfcDevice.getCard();
                if (mifare != null) {
                    msgBuffer.delete(0, msgBuffer.length());
                    msgBuffer.append("寻到Mifare卡->UID:").append(mifare.uidToString()).append("\r\n");
                    msgBuffer.append("开始验证第3块密码\r\n");
                    handler.sendEmptyMessage(0);
                    byte[] key = {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};
                    try {
                        boolean anth = mifare.authenticate((byte) 0x10, Mifare.MIFARE_KEY_TYPE_B, key);
                        if (anth) {
                            byte[] readDataBytes = mifare.read((byte) 0x10);
                            msgBuffer.append("块16数据:").append(StringTool.byteHexToSting(readDataBytes)).append("\r\n");
                            handler.sendEmptyMessage(0);

                            readDataBytes = mifare.read((byte) 0x11);
                            msgBuffer.append("块17数据:").append(StringTool.byteHexToSting(readDataBytes)).append("\r\n");
                            handler.sendEmptyMessage(0);
                        }
                        else {
                            msgBuffer.append("验证密码失败\r\n");
                            handler.sendEmptyMessage(0);
                            return false;
                        }
                    } catch (CardNoResponseException e) {
                        e.printStackTrace();
                        return false;
                    }
                }
                break;
            case DeviceManager.CARD_TYPE_ISO15693: //寻到15693卡
                final Iso15693Card iso15693Card = (Iso15693Card) bleNfcDevice.getCard();
                if (iso15693Card != null) {
                    msgBuffer.delete(0, msgBuffer.length());
                    msgBuffer.append("寻到15693卡->UID:").append(iso15693Card.uidToString()).append("\r\n");
                    msgBuffer.append("读块0数据\r\n");
                    handler.sendEmptyMessage(0);
                    try {
                        byte[] cmdBytes = new byte[11];
                        cmdBytes[0] = 0x22;
                        cmdBytes[1] = 0x20;
                        System.arraycopy(iso15693Card.uid, 0, cmdBytes, 2, 8);
                        cmdBytes[10] = 0x01;

                        msgBuffer.append("指令透传发送：" + StringTool.byteHexToSting(cmdBytes) + "\r\n");
                        handler.sendEmptyMessage(0);
                        byte[] returnBytes = iso15693Card.transceive(cmdBytes);
                        msgBuffer.append("返回：" + StringTool.byteHexToSting(returnBytes)).append("\r\n");

                        //读写单个块Demo
                        msgBuffer.append("写数据01020304到块4").append("\r\n");
                        handler.sendEmptyMessage(0);
                        boolean isSuc = iso15693Card.write((byte)4, new byte[] {0x01, 0x02, 0x03, 0x04});
                        if (isSuc) {
                            msgBuffer.append("写数据成功！").append("\r\n");
                            handler.sendEmptyMessage(0);
                        }
                        else {
                            msgBuffer.append("写数据失败！").append("\r\n");
                            handler.sendEmptyMessage(0);
                        }
                        msgBuffer.append("读块4数据").append("\r\n");
                        handler.sendEmptyMessage(0);
                        byte[] bytes = iso15693Card.read((byte) 4);
                        msgBuffer.append("块4数据：").append(StringTool.byteHexToSting(bytes)).append("\r\n");
                        handler.sendEmptyMessage(0);

                        //读写多个块Demo
                        msgBuffer.append("写数据0102030405060708到块5、6").append("\r\n");
                        handler.sendEmptyMessage(0);
                        isSuc = iso15693Card.writeMultiple((byte)5, (byte)2, new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08});
                        if (isSuc) {
                            msgBuffer.append("写数据成功！").append("\r\n");
                            handler.sendEmptyMessage(0);
                        }
                        else {
                            msgBuffer.append("写数据失败！").append("\r\n");
                            handler.sendEmptyMessage(0);
                        }
                        msgBuffer.append("读块5、6数据").append("\r\n");
                        handler.sendEmptyMessage(0);
                        bytes = iso15693Card.ReadMultiple((byte) 5, (byte)2);
                        msgBuffer.append("块5、6数据：").append(StringTool.byteHexToSting(bytes)).append("\r\n");
                        handler.sendEmptyMessage(0);
                    } catch (CardNoResponseException e) {
                        e.printStackTrace();
                        return false;
                    }
                }
                break;
        }
        return true;
    }

    //搜索最近的设备并连接
    private void searchNearestBleDevice() {
        msgBuffer.delete(0, msgBuffer.length());
        msgBuffer.append("正在搜索设备...");
        handler.sendEmptyMessage(0);
        if (!mScanner.isScanning() && (bleNfcDevice.isConnection() == BleManager.STATE_DISCONNECTED)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    synchronized (this) {
                        mScanner.startScan(0);
                        mNearestBleLock.lock();
                        try {
                            mNearestBle = null;
                        }finally {
                            mNearestBleLock.unlock();
                        }
                        lastRssi = -100;

                        int searchCnt = 0;
                        while ((mNearestBle == null)
                                && (searchCnt < 10000)
                                && (mScanner.isScanning())
                                && (bleNfcDevice.isConnection() == BleManager.STATE_DISCONNECTED)) {
                            searchCnt++;
                            try {
                                Thread.sleep(1);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        if (mScanner.isScanning() && (bleNfcDevice.isConnection() == BleManager.STATE_DISCONNECTED)) {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            mScanner.stopScan();
                            mNearestBleLock.lock();
                            try {
                                if (mNearestBle != null) {
                                    mScanner.stopScan();
                                    msgBuffer.delete(0, msgBuffer.length());
                                    msgBuffer.append("正在连接设备...");
                                    handler.sendEmptyMessage(0);
                                    bleNfcDevice.requestConnectBleDevice(mNearestBle.getAddress());
                                } else {
                                    msgBuffer.delete(0, msgBuffer.length());
                                    msgBuffer.append("未找到设备！");
                                    handler.sendEmptyMessage(0);
                                }
                            }finally {
                                mNearestBleLock.unlock();
                            }
                        } else {
                            mScanner.stopScan();
                        }
                    }
                }
            }).start();
        }
    }

    //发送读写进度条显示Handler
    private void showReadWriteDialog(String msg, int rate) {
        Message message = new Message();
        message.what = 4;
        message.arg1 = rate;
        message.obj = msg;
        handler.sendMessage(message);
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            msgText.setText(/*"解析成功次数：" + suc_cnt + "\r\n解析失败次数：" + err_cnt + */ msgBuffer + "\r\n");

            if ( (bleNfcDevice.isConnection() == BleManager.STATE_CONNECTED) || ((bleNfcDevice.isConnection() == BleManager.STATE_CONNECTING)) ) {
                searchButton.setText("断开连接");
            }
            else {
                searchButton.setText("搜索设备");
            }

            switch (msg.what) {
                case 1:
                    break;
                case 2:
                    break;
                case 3:
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                /*获取设备版本号*/
                                byte versions = bleNfcDevice.getDeviceVersions();
                                msgBuffer.append("设备版本:").append(String.format("%02x", versions)).append("\r\n");
                                handler.sendEmptyMessage(0);

                                /*获取设备电池电压*/
                                double voltage = bleNfcDevice.getDeviceBatteryVoltage();
                                msgBuffer.append("设备电池电压:").append(String.format("%.2f", voltage)).append("\r\n");
                                if (voltage < 3.61) {
                                    msgBuffer.append("设备电池电量低，请及时充电！");
                                } else {
                                    msgBuffer.append("设备电池电量充足！");
                                }
                                handler.sendEmptyMessage(0);
                                boolean isSuc;
//                                /*设置快速传输参数*/
//                                boolean isSuc = bleNfcDevice.androidFastParams(true);
//                                if (isSuc) {
//                                    msgBuffer.append("\r\n蓝牙快速传输参数设置成功!");
//                                }
//                                else {
//                                    msgBuffer.append("\n不支持快速传输参数设置!");
//                                }
//                                handler.sendEmptyMessage(0);

                                /*修改设备序列号*/
                                msgBuffer.append("\n修改设备序列号为：00112233445566778899AABBCCDDEEFF00112233445566778899AABBCCDDEEFF");
                                handler.sendEmptyMessage(0);
                                byte[] newSerialNumberBytes = new byte[] {
                                        0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, (byte)0x88, (byte)0x99, (byte)0xaa, (byte)0xbb, (byte)0xcc, (byte)0xdd, (byte)0xee, (byte)0xff,
                                        0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, (byte)0x88, (byte)0x99, (byte)0xaa, (byte)0xbb, (byte)0xcc, (byte)0xdd, (byte)0xee, (byte)0xff
                                };
                                isSuc = bleNfcDevice.changeSerialNumber(newSerialNumberBytes);
                                if (isSuc) {
                                    msgBuffer.append("\r\n设备序列号修改成功!");
                                }
                                else {
                                    msgBuffer.append("\r\n设备序列号修改失败!");
                                }
                                handler.sendEmptyMessage(0);

                                /*获取设备序列号*/
                                try {
                                    msgBuffer.append("\r\n开始获取设备序列号...\r\n");
                                    handler.sendEmptyMessage(0);
                                    byte[] serialNumberBytes = bleNfcDevice.getSerialNumber();
                                    msgBuffer.append("设备序列号为：").append(StringTool.byteHexToSting(serialNumberBytes));
                                }catch (DeviceNoResponseException e) {
                                    e.printStackTrace();
                                }

                                /*开启自动寻卡*/
                                msgBuffer.append("\n开启自动寻卡...\r\n");
                                handler.sendEmptyMessage(0);
                                //开始自动寻卡
                                startAutoSearchCard();
                            } catch (DeviceNoResponseException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                    break;

                case 4:   //读写进度条
                    if ((msg.arg1 == 0) || (msg.arg1 == 100)) {
                        readWriteDialog.dismiss();
                        readWriteDialog.setProgress(0);
                    } else {
                        readWriteDialog.setMessage((String) msg.obj);
                        readWriteDialog.setProgress(msg.arg1);
                        if (!readWriteDialog.isShowing()) {
                            readWriteDialog.show();
                        }
                    }
                    break;
                case 7:  //搜索设备列表
                    break;
            }
        }
    };
}
