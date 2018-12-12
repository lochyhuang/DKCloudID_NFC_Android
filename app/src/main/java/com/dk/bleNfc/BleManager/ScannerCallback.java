package com.dk.bleNfc.BleManager;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanRecord;

/**
 * Created by lochy on 16/1/19.
 */
public abstract class ScannerCallback {
    public void onReceiveScanDevice(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
    }

    public void onScanDeviceStopped() {
    }
}
