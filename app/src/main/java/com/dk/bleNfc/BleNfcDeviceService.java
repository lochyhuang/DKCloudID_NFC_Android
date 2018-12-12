package com.dk.bleNfc;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanRecord;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.dk.bleNfc.BleManager.BleManager;
import com.dk.bleNfc.BleManager.Scanner;
import com.dk.bleNfc.BleManager.ScannerCallback;
import com.dk.bleNfc.DeviceManager.BleNfcDevice;
import com.dk.bleNfc.DeviceManager.DeviceManager;
import com.dk.bleNfc.DeviceManager.DeviceManagerCallback;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Administrator on 2017/5/2.
 */

public class BleNfcDeviceService extends Service {
    public final static String ACTION_GATT_CONNECTED           = "com.dk.bleNfc.BleNfcDeviceService.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED        = "com.dk.bleNfc.BleNfcDeviceService.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_TAG_FIND_TAG             = "com.dk.bleNfc.BleNfcDeviceService.ACTION_TAG_FIND_TAG";
    public final static String ACTION_TAG_LOSE_TAG             = "com.dk.bleNfc.BleNfcDeviceService.ACTION_TAG_LOSE_TAG";

    public static final String TAG = "BleNfcDeviceService";
    private final IBinder mBinder = new LocalBinder();

    public BleNfcDevice bleNfcDevice;
    public DeviceManagerCallback deviceManagerCallback;
    public Scanner scanner;
    public ScannerCallback scannerCallback;

    private BluetoothDevice mNearestBle = null;
    private Lock mNearestBleLock = new ReentrantLock();// 锁对象
    private int lastRssi = -100;

    @Override
    public void onCreate() {
        super.onCreate();

        //初始设备操作类
        scanner = new Scanner(BleNfcDeviceService.this, mScannerCallback);
        bleNfcDevice = new BleNfcDevice(BleNfcDeviceService.this);
        bleNfcDevice.setCallBack(mDeviceManagerCallback);

        //开始搜索并连接最近的一个设备
        //startScanAndConnectTheNearestDevice();
    }

    //蓝牙搜索回调
    private ScannerCallback mScannerCallback = new ScannerCallback() {
        @Override
        public void onReceiveScanDevice(BluetoothDevice device, int rssi, byte[] scanRecord) {
            super.onReceiveScanDevice(device, rssi, scanRecord);
            if (scannerCallback != null) {
                scannerCallback.onReceiveScanDevice(device, rssi, scanRecord);
            }
            Log.d(TAG, "Activity搜到设备：" + device.getName() + "信号强度：" + rssi );
            //搜索蓝牙设备并记录信号强度最强的设备
            if ( (device.getName() != null) && (device.getName().contains("UNISMES") || device.getName().contains("BLE_NFC")) ) {
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
            if (scannerCallback != null) {
                scannerCallback.onScanDeviceStopped();
            }
            super.onScanDeviceStopped();
        }
    };

    //设备操作类回调
    private DeviceManagerCallback mDeviceManagerCallback = new DeviceManagerCallback() {
        @Override
        public void onReceiveConnectBtDevice(boolean blnIsConnectSuc) {
            super.onReceiveConnectBtDevice(blnIsConnectSuc);
            if (deviceManagerCallback != null) {
                deviceManagerCallback.onReceiveConnectBtDevice(blnIsConnectSuc);
            }
            if (blnIsConnectSuc) {
                Log.d(TAG, "BleNfcDeviceService设备连接成功");
            }
        }

        @Override
        public void onReceiveDisConnectDevice(boolean blnIsDisConnectDevice) {
            super.onReceiveDisConnectDevice(blnIsDisConnectDevice);
            if (deviceManagerCallback != null) {
                deviceManagerCallback.onReceiveDisConnectDevice(blnIsDisConnectDevice);
            }
            //startScanAndConnectTheNearestDevice();
            Log.d(TAG, "BleNfcDeviceService设备断开链接");
        }

        @Override
        public void onReceiveConnectionStatus(boolean blnIsConnection) {
            super.onReceiveConnectionStatus(blnIsConnection);
            if (deviceManagerCallback != null) {
                deviceManagerCallback.onReceiveConnectionStatus(blnIsConnection);
            }
            Log.d(TAG, "BleNfcDeviceService设备链接状态回调");
        }

        @Override
        public void onReceiveInitCiphy(boolean blnIsInitSuc) {
            super.onReceiveInitCiphy(blnIsInitSuc);
            if (deviceManagerCallback != null) {
                deviceManagerCallback.onReceiveInitCiphy(blnIsInitSuc);
            }
        }

        @Override
        public void onReceiveDeviceAuth(byte[] authData) {
            super.onReceiveDeviceAuth(authData);
            if (deviceManagerCallback != null) {
                deviceManagerCallback.onReceiveDeviceAuth(authData);
            }
        }

        @Override
        public void onReceiveRfnSearchCard(boolean blnIsSus, int cardType, byte[] bytCardSn, byte[] bytCarATS) {
            super.onReceiveRfnSearchCard(blnIsSus, cardType, bytCardSn, bytCarATS);
            if (deviceManagerCallback != null) {
                deviceManagerCallback.onReceiveRfnSearchCard(blnIsSus, cardType, bytCardSn, bytCarATS);
            }
            if (!blnIsSus|| cardType == BleNfcDevice.CARD_TYPE_NO_DEFINE) {
                return;
            }
            StringBuilder stringBuffer = new StringBuilder();
            for (byte aBytCardSn : bytCardSn) {
                stringBuffer.append(String.format("%02x", aBytCardSn));
            }

            StringBuilder stringBuffer1 = new StringBuilder();
            for (byte bytCarAT : bytCarATS) {
                stringBuffer1.append(String.format("%02x", bytCarAT));
            }
            Log.d(TAG, "BleNfcDeviceService接收到激活卡片回调：UID->" + stringBuffer + " ATS->" + stringBuffer1);
        }

        @Override
        public void onReceiveRfmSentApduCmd(byte[] bytApduRtnData) {
            super.onReceiveRfmSentApduCmd(bytApduRtnData);
            if (deviceManagerCallback != null) {
                deviceManagerCallback.onReceiveRfmSentApduCmd(bytApduRtnData);
            }
            StringBuilder stringBuffer = new StringBuilder();
            for (byte aBytApduRtnData : bytApduRtnData) {
                stringBuffer.append(String.format("%02x", aBytApduRtnData));
            }
            Log.d(TAG, "BleNfcDeviceService接收到APDU回调：" + stringBuffer);
        }

        @Override
        public void onReceiveRfmClose(boolean blnIsCloseSuc) {
            super.onReceiveRfmClose(blnIsCloseSuc);
            if (deviceManagerCallback != null) {
                deviceManagerCallback.onReceiveRfmClose(blnIsCloseSuc);
            }
        }

        @Override
        //按键返回回调
        public void onReceiveButtonEnter(byte keyValue) {
            super.onReceiveButtonEnter(keyValue);
            if (deviceManagerCallback != null) {
                deviceManagerCallback.onReceiveButtonEnter(keyValue);
            }
            if (keyValue == DeviceManager.BUTTON_VALUE_SHORT_ENTER) { //按键短按
                Log.d(TAG, "BleNfcDeviceService接收到按键短按回调");
            }
            else if (keyValue == DeviceManager.BUTTON_VALUE_LONG_ENTER) { //按键长按
                Log.d(TAG, "BleNfcDeviceService接收到按键长按回调");
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Log.d(TAG, "onStartCommand() executed");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //Log.d(TAG, "onDestroy() executed");
        disconnectBle();
    }

    public class LocalBinder extends Binder {
        public BleNfcDeviceService getService() {
            return BleNfcDeviceService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    //设置设备管理回调，设备管理回调会回调会在蓝牙状态改变时（断开连接、连接成功等）回调
    public void setDeviceManagerCallback(DeviceManagerCallback deviceManagerCallback) {
        this.deviceManagerCallback = deviceManagerCallback;
        //bleNfcDevice.setCallBack(this.deviceManagerCallback);
    }

    //设置蓝牙搜索回调
    public void setScannerCallback(ScannerCallback scannerCallback) {
        this.scannerCallback = scannerCallback;
        //scanner.setScannerCallback(scannerCallback);
    }

    //直接断开蓝牙的连接，注意：使用此函数不会才生回调
    private void disconnectBle() {
        bleNfcDevice.bleManager.close();
    }

    //开始搜索设备，调用此方法后会一直搜索设备直到调用stopScanDevice方法
    public void startScanDevice() {
        if (!scanner.isScanning()) {
            scanner.startScan(0);
        }
    }

    //开始搜索设备，调用此方法后会一直搜索设备直到调用stopScanDevice方法或者超时
    public void startScanDevice(int delay) {
        if (!scanner.isScanning()) {
            scanner.startScan(delay);
        }
    }

    //停止搜索设备
    public void stopScanDevice() {
        if (scanner.isScanning()) {
            scanner.stopScan();
        }
    }

    //开始搜索设备并连接最近的一个设备
    public void startScanAndConnectTheNearestDevice() {
        if ( (bleNfcDevice.isConnection() == BleManager.STATE_CONNECTED) ) {
            return;
        }

        if (!scanner.isScanning() && (bleNfcDevice.isConnection() == BleManager.STATE_DISCONNECTED)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    synchronized (this) {
                        scanner.startScan(0);
                        mNearestBleLock.lock();
                        try {
                            mNearestBle = null;
                        }finally {
                            mNearestBleLock.unlock();
                        }
                        lastRssi = -100;

                        while ((mNearestBle == null)
                                && (scanner.isScanning())
                                && (bleNfcDevice.isConnection() == BleManager.STATE_DISCONNECTED)) {
                            try {
                                Thread.sleep(1);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        if (scanner.isScanning() && (bleNfcDevice.isConnection() == BleManager.STATE_DISCONNECTED)) {
                            int searchCnt = 0;
                            while (
                                    (Scanner.deviceList.size() < 2)
                                    && (searchCnt < 1000)
                                    && (scanner.isScanning())
                                    && (bleNfcDevice.isConnection() == BleManager.STATE_DISCONNECTED)) {
                                searchCnt++;
                                try {
                                    Thread.sleep(1);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }

                            scanner.stopScan();
                            mNearestBleLock.lock();
                            try {
                                if (mNearestBle != null) {
                                    scanner.stopScan();
                                    bleNfcDevice.requestConnectBleDevice(mNearestBle.getAddress());
                                }
                            }finally {
                                mNearestBleLock.unlock();
                            }
                        } else {
                            scanner.stopScan();
                        }
                    }
                }
            }).start();
        }
    }
}
