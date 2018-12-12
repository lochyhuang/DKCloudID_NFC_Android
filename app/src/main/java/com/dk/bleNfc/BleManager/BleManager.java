package com.dk.bleNfc.BleManager;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.dk.bleNfc.Exception.CardNoResponseException;
import com.dk.bleNfc.Exception.DeviceNoResponseException;
import com.dk.bleNfc.card.Card;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by lochy on 15/10/7.
 */
public class BleManager {
    private final static String TAG = "BleManager";
    private Context mContext;
    private BluetoothAdapter mBAdapter = BluetoothAdapter.getDefaultAdapter();
    public BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic mcharacteristic = null;

    public int mConnectionState = STATE_DISCONNECTED;
    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
    public static final int STATE_DISCONNECTING = 3;

    public onReceiveDataListener mOnReceiveDataListener;
    public onBleConnectListener mOnBleConnectListener;
    public onBleDisconnectListener mOnBleDisconnectListener;
    public onBleReadListener monBleReadListener;
    public onWriteSuccessListener mOnWriteSuccessListener;

    public BleManager(Context c) {
        mContext = c;
    }

    public interface onReceiveDataListener {
        public void OnReceiverData(byte[] data);
    }

    public interface onBleConnectListener {
        public void onBleConnect(boolean isConnectSucceed);
    }

    public interface onBleDisconnectListener {
        public void onBleDisconnect();
    }

    public interface onBleReadListener {
        public void onBleRead(byte[] value);
    }


    public interface onWriteSuccessListener {
        public void onWriteSuccess();
    }

    public void setOnReceiveDataListener(onReceiveDataListener l) {
        this.mOnReceiveDataListener = l;
    }

    public void setOnBleConnectListener(onBleConnectListener l) {
        this.mOnBleConnectListener = l;
    }

    public void setOnBledisconnectListener(onBleDisconnectListener l) {
        this.mOnBleDisconnectListener = l;
    }

    public void setOnWriteSuccessListener(onWriteSuccessListener l) {
        this.mOnWriteSuccessListener = l;
    }

    public void setOnBleReadListener(onBleReadListener l) {
        this.monBleReadListener = l;
    }

    public boolean connect(String mDeviceAddress, onBleConnectListener l) {
        if (mBAdapter == null || mDeviceAddress == null) {
            return false;
        }

        final BluetoothDevice device = mBAdapter.getRemoteDevice(mDeviceAddress);
        if (device == null) {
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(mContext, false, mGattCallback);
        setOnBleConnectListener(l);
        return true;
    }

    public boolean connect(String mDeviceAddress) {
        if (mBAdapter == null || mDeviceAddress == null) {
            return false;
        }

        final BluetoothDevice device = mBAdapter.getRemoteDevice(mDeviceAddress);
        if (device == null) {
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(mContext, false, mGattCallback);
        return true;
    }

    public class BluetoothReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);

                switch (state){
                    case BluetoothAdapter.STATE_OFF:
                        //Indicates the local Bluetooth adapter is off.
                        break;

                    case BluetoothAdapter.STATE_TURNING_ON:
                        //Indicates the local Bluetooth adapter is turning on. However local clients should wait for STATE_ON before attempting to use the adapter.
                        break;

                    case BluetoothAdapter.STATE_ON:
                        //Indicates the local Bluetooth adapter is on, and ready for use.
                        break;

                    case BluetoothAdapter.STATE_TURNING_OFF:
                        //Indicates the local Bluetooth adapter is turning off. Local clients should immediately attempt graceful disconnection of any remote links.
                        break;
                }
            }
        }
    };

    // connection change and services discovered.
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {      //设备连接成功
                mConnectionState = STATE_CONNECTED;
                mBluetoothGatt.discoverServices();  //开始搜索服务
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {   //设备断开连接
                mConnectionState = STATE_DISCONNECTED;
                mcharacteristic = null;
                if (mOnBleDisconnectListener != null) {
                    mOnBleDisconnectListener.onBleDisconnect();
                }
                close();
            }
            else if (newState == BluetoothProfile.STATE_CONNECTING) {
                mConnectionState = STATE_CONNECTING;
            }
            else if (newState == BluetoothProfile.STATE_DISCONNECTING){
                mConnectionState = STATE_DISCONNECTING;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
//                System.out.println("发现服务:");

                Boolean searchFlag = false;
                //获取服务列表
                //List<BluetoothGattService> serviceList = mBluetoothGatt.getServices();
                List<BluetoothGattService> serviceList = new ArrayList<BluetoothGattService>(mBluetoothGatt.getServices());
                //搜索FFF0服务
                for (BluetoothGattService gattService : serviceList) {
                    System.out.println(gattService.getUuid().toString());
                    if ( gattService.getUuid().toString().contains("fff0") || gattService.getUuid().toString().contains("FFF0") ) {
//                        System.out.println("搜到服务 FFF0");

                        if (mBluetoothGatt.getDevice().getName().contains("UNISMES")) {
                            //获取特征值列表
                            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                                if (gattCharacteristic.getUuid().timestamp() == 0xFFF1) {
//                                System.out.println("搜到特征值");
                                    searchFlag = true;
                                    mcharacteristic = gattCharacteristic;     //保存特征值
                                    //打开通知
                                    setCharacteristicNotification(mcharacteristic, true);
                                    if (mOnBleConnectListener != null) {
                                        mOnBleConnectListener.onBleConnect(true);
                                    }
                                }
                            }
                        }
                        else {
                            //获取特征值列表
                            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                                if (gattCharacteristic.getUuid().timestamp() == 0xFFF2) {
//                                System.out.println("搜到特征值");
                                    searchFlag = true;
                                    mcharacteristic = gattCharacteristic;     //保存特征值
                                    //打开通知
                                    setCharacteristicNotification(mcharacteristic, true);
                                    if (mOnBleConnectListener != null) {
                                        mOnBleConnectListener.onBleConnect(true);
                                    }
                                }
                            }
                        }
                    }
                }

                //未找到对应服务,断开当前连接
                if (!searchFlag) {
                    if (mConnectionState == STATE_CONNECTED) {
                        mBluetoothGatt.disconnect();
                        mConnectionState = STATE_DISCONNECTED;
                        if (mOnBleConnectListener != null) {
                            mOnBleConnectListener.onBleConnect(false);
                        }
                    }
                }
            } else {
                System.out.println("onServicesDiscovered received: " + status);
                if (mOnBleConnectListener != null) {
                    mOnBleConnectListener.onBleConnect(false);
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (monBleReadListener != null)
                    monBleReadListener.onBleRead(characteristic.getValue());
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
//            Log.d(TAG, "发送数据完毕");
            if (mOnWriteSuccessListener != null) {
                mOnWriteSuccessListener.onWriteSuccess();
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            byte[] return_data = characteristic.getValue();

            StringBuffer stringBuffer = new StringBuffer();
            for (int i=0; i<return_data.length; i++) {
                stringBuffer.append(String.format("%02x", return_data[i]));
            }
            //System.out.println("onCharacteristicChanged：" + stringBuffer);

            if (mOnReceiveDataListener != null) {
                mOnReceiveDataListener.OnReceiverData(return_data);
            }
        }
    };

    //设置通知开关
    private void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                               boolean enabled) {
        if (mBAdapter == null || mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
        for (BluetoothGattDescriptor dp : descriptors) {
            dp.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(dp);
        }
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    //断开连接
    public boolean cancelConnect() {
//        if (mConnectionState == STATE_CONNECTED) {
        if ( (mBluetoothGatt != null) && (mConnectionState == STATE_CONNECTED)) {
            mBluetoothGatt.disconnect();
        }
            //mConnectionState = STATE_DISCONNECTED;
//            if (mOnBleDisconnectListener != null) {
//                mOnBleDisconnectListener.onBleDisconnect();
//            }
//        }
        return true;
    }

    public boolean cancelConnect(onBleDisconnectListener l) {
//        if (mConnectionState == STATE_CONNECTED) {
            setOnBledisconnectListener(l);
        if ( (mBluetoothGatt != null) && (mConnectionState == STATE_CONNECTED)) {
            mBluetoothGatt.disconnect();
        }
            //mConnectionState = STATE_DISCONNECTED;
//            if (mOnBleDisconnectListener != null) {
//                mOnBleDisconnectListener.onBleDisconnect();
//            }
//        }
        return true;
    }
    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    public void readCharacteristic(BluetoothGattCharacteristic characteristic, onBleReadListener l) {
        if (mBAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        Log.d(TAG, "BluetoothAdapter readCharacteristic");
        setOnBleReadListener(l);
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    //写特征值
    public boolean writeDataToCharacteristic(byte[] writeData) {
        final Semaphore semaphore = new Semaphore(1);
        //获取信号量
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        synchronized(this) {
            if ((mConnectionState == STATE_CONNECTED) && (mcharacteristic != null) && (writeData != null)) {
                mcharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                if (writeData.length <= 20) {
                    mcharacteristic.setValue(writeData);
                    mBluetoothGatt.writeCharacteristic(mcharacteristic);
                    StringBuffer stringBuffer = new StringBuffer();
                    for (int i = 0; i < writeData.length; i++) {
                        stringBuffer.append(String.format("%02x", writeData[i]));
                    }
                    //System.out.println("发送数据：" + stringBuffer);
                } else {
                    final byte[] writeBytes = writeData;

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            int i = 0;
                            try {
                                for (i=0; i<= (writeBytes.length / 20 - 1); i++) {
                                    byte[] bytesTemp = new byte[20];
                                    System.arraycopy(writeBytes, i * 20, bytesTemp, 0, 20);
                                    writeCharacteristic(bytesTemp);
                                }
                                int len = writeBytes.length % 20;
                                if (len > 0) {
                                    byte[] bytes = new byte[len];
                                    System.arraycopy(writeBytes, writeBytes.length - len, bytes, 0, len);
                                    writeCharacteristic(bytes);
                                }
                            }catch (DeviceNoResponseException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                }
            } else {
                return false;
            }
            return true;
        }
    }

    public void writeCharacteristic(byte[] s, onWriteSuccessListener l) {
        if (mcharacteristic != null) {
            setOnWriteSuccessListener(l);
            mcharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            mcharacteristic.setValue(s);
            mBluetoothGatt.writeCharacteristic(mcharacteristic);
            StringBuffer stringBuffer = new StringBuffer();
            for (int i=0; i<s.length; i++) {
                stringBuffer.append(String.format("%02x", s[i]));
            }
            System.out.println("BleManager发送数据：" + stringBuffer);
        }
    }

    public Boolean writeCharacteristic(byte[] s) throws DeviceNoResponseException {
        final byte[][] returnBytes = new byte[1][1];
        final boolean[] isCmdRunSucFlag = {false};

        final Semaphore semaphore = new Semaphore(0);
        returnBytes[0] = null;

        writeCharacteristic(s, new onWriteSuccessListener() {
            @Override
            public void onWriteSuccess() {
                isCmdRunSucFlag[0] = true;
                semaphore.release();
            }
        });

        try {
            semaphore.tryAcquire(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new DeviceNoResponseException("设备无响应");
        }

        return isCmdRunSucFlag[0];
    }

    //获取服务
    public List<BluetoothGattService> getServices() {
        if (mBluetoothGatt == null) return null;
        return mBluetoothGatt.getServices();
    }
}

