package com.dk.bleNfc.DeviceManager;

import android.content.Context;

import com.dk.bleNfc.Exception.CardNoResponseException;
import com.dk.bleNfc.Exception.DeviceNoResponseException;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by Administrator on 2017/5/15.
 */

public class BleNfcDevice extends DeviceManager{
    final static int DEVICE_NO_RESPONSE_TIME = 500;

    public BleNfcDevice(Context context) {
        super(context);
    }

    /**
     * 获取设备名称
     * @return         设备名称
     */
    public String getDeviceName() {
        return bleManager.mBluetoothGatt.getDevice().getName();
    }

    /**
     * 是否正在自动寻卡
     * @return         true - 正在自动寻卡
     *                  false - 自动寻卡已经关闭
     */
    public boolean isAutoSearchCard() {
        return super.autoSearchCardFlag;
    }

    /**
     * 获取设备当前电池电压，同步阻塞方式，注意：不能在主线程里运行
     * @return         设备电池电压，单位：V
     * @throws DeviceNoResponseException
     *                  操作无响应时会抛出异常
     */
    public double getDeviceBatteryVoltage() throws DeviceNoResponseException {
        final double[] returnVoltage = new double[1];

        final Semaphore semaphore = new Semaphore(0);

        requestBatteryVoltageDevice(new DeviceManager.onReceiveBatteryVoltageDeviceListener() {
            @Override
            public void onReceiveBatteryVoltageDevice(double voltage) {
                returnVoltage[0] = voltage;
                semaphore.release();
            }
        });

        try {
            semaphore.tryAcquire(DEVICE_NO_RESPONSE_TIME, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new DeviceNoResponseException("");
        }
        return returnVoltage[0];
    }

    /**
     * 获取设备版本号，同步阻塞方式，注意：不能在主线程里运行
     * @return         设备版本号，1 字节
     * @throws DeviceNoResponseException
     *                  操作无响应时会抛出异常
     */
    public byte getDeviceVersions() throws DeviceNoResponseException {
        final byte[] returnBytes = new byte[1];

        final Semaphore semaphore = new Semaphore(0);

        requestVersionsDevice(new DeviceManager.onReceiveVersionsDeviceListener() {
            @Override
            public void onReceiveVersionsDevice(byte versions) {
                returnBytes[0] = versions;
                semaphore.release();
            }
        });

        try {
            semaphore.tryAcquire(DEVICE_NO_RESPONSE_TIME, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new DeviceNoResponseException("");
        }
        return returnBytes[0];
    }

    /**
     * 打开关闭防丢器功能，同步阻塞方式，注意：不能在主线程里运行
     * @param s         true - 打开
     *                  false - 关闭
     * @return         true - 防丢器功能已打开
     *                  false - 防丢器功能已关闭
     * @throws DeviceNoResponseException
     *                  操作无响应时会抛出异常
     */
    public boolean antiLostSwitch(boolean s) throws DeviceNoResponseException {
        final byte[][] returnBytes = new byte[1][1];
        final boolean[] isCmdRunSucFlag = {false};

        final Semaphore semaphore = new Semaphore(0);
        returnBytes[0] = null;

        requestAntiLostSwitch(s, new onReceiveAntiLostSwitchListener() {
            @Override
            public void onReceiveAntiLostSwitch(boolean isSuc) {
                if (isSuc) {
                    isCmdRunSucFlag[0] = true;
                }
                else {
                    isCmdRunSucFlag[0] = false;
                }
                semaphore.release();
            }
        });

        try {
            semaphore.tryAcquire(DEVICE_NO_RESPONSE_TIME, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new DeviceNoResponseException("设备无响应");
        }

        return isCmdRunSucFlag[0];
    }

    /**
     * 开启/关闭快速传输，只对安卓手机有效，同步阻塞方式，注意：不能在主线程里运行
     * @param s         true - 打开
     *                  false - 关闭
     * @return         true - 快速传输已打开
     *                  false - 快速传已关闭
     * @throws DeviceNoResponseException
     *                  操作无响应时会抛出异常
     */
    public boolean androidFastParams(boolean s) throws DeviceNoResponseException {
        final byte[][] returnBytes = new byte[1][1];
        final boolean[] isCmdRunSucFlag = {false};

        final Semaphore semaphore = new Semaphore(0);
        returnBytes[0] = null;

        requestAndroidFastParams(s, new onReceiveAndroidFastParamsListener() {
            @Override
            public void onReceiveAndroidFastParams(boolean isSuc) {
                if (isSuc) {
                    isCmdRunSucFlag[0] = true;
                }
                else {
                    isCmdRunSucFlag[0] = false;
                }
                semaphore.release();
            }
        });

        try {
            semaphore.tryAcquire(DEVICE_NO_RESPONSE_TIME, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new DeviceNoResponseException("设备无响应");
        }

        return isCmdRunSucFlag[0];
    }

    /**
     * 关闭蜂鸣器，同步阻塞方式，注意：不能在主线程里运行
     * @return           true - 操作成功
     *                    false - 操作失败
     * @throws DeviceNoResponseException
     *                  操作无响应时会抛出异常
     */
    public boolean closeBeep(int onDelayMs, int offDelayMs, int n) throws DeviceNoResponseException {
        final byte[][] returnBytes = new byte[1][1];
        final boolean[] isCmdRunSucFlag = {false};

        final Semaphore semaphore = new Semaphore(0);
        returnBytes[0] = null;

        requestOpenBeep(0, 0, 0, new onReceiveOpenBeepCmdListener() {
            @Override
            public void onReceiveOpenBeepCmd(boolean isSuc) {
                if (isSuc) {
                    isCmdRunSucFlag[0] = true;
                }
                else {
                    isCmdRunSucFlag[0] = false;
                }
                semaphore.release();
            }
        });

        try {
            semaphore.tryAcquire(DEVICE_NO_RESPONSE_TIME, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new DeviceNoResponseException("设备无响应");
        }

        return isCmdRunSucFlag[0];
    }

    /**
     * 打开蜂鸣器指令，同步阻塞方式，注意：不能在主线程里运行
     * @param onDelayMs  打开蜂鸣器时间：0~0xffff，单位ms
     * @param offDelayMs 关闭蜂鸣器时间：0~0xffff，单位ms
     * @param n          蜂鸣器响多少声：0~255
     * @return           true - 操作成功
     *                    false - 操作失败
     * @throws DeviceNoResponseException
     *                  操作无响应时会抛出异常
     */
    public boolean openBeep(int onDelayMs, int offDelayMs, int n) throws DeviceNoResponseException {
        final byte[][] returnBytes = new byte[1][1];
        final boolean[] isCmdRunSucFlag = {false};

        final Semaphore semaphore = new Semaphore(0);
        returnBytes[0] = null;

        requestOpenBeep(onDelayMs, offDelayMs, n, new onReceiveOpenBeepCmdListener() {
            @Override
            public void onReceiveOpenBeepCmd(boolean isSuc) {
                if (isSuc) {
                    isCmdRunSucFlag[0] = true;
                }
                else {
                    isCmdRunSucFlag[0] = false;
                }
                semaphore.release();
            }
        });

        try {
            semaphore.tryAcquire(DEVICE_NO_RESPONSE_TIME, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new DeviceNoResponseException("设备无响应");
        }

        return isCmdRunSucFlag[0];
    }

    /**
     * 修改蓝牙名，同步阻塞方式，注意：不能在主线程里运行
     * @param bleName   新蓝牙名称
     * @return         true - 操作成功
     *                  false - 操作失败
     * @throws DeviceNoResponseException
     *                  操作无响应时会抛出异常
     */
    public boolean changeBleName(String bleName) throws DeviceNoResponseException {
        final byte[][] returnBytes = new byte[1][1];
        final boolean[] isCmdRunSucFlag = {false};

        final Semaphore semaphore = new Semaphore(0);
        returnBytes[0] = null;

        requestChangeBleName(bleName, new onReceiveChangeBleNameListener() {
            @Override
            public void onReceiveChangeBleName(boolean isSuc) {
                if (isSuc) {
                    isCmdRunSucFlag[0] = true;
                }
                else {
                    isCmdRunSucFlag[0] = false;
                }
                semaphore.release();
            }
        });

        try {
            semaphore.tryAcquire(DEVICE_NO_RESPONSE_TIME, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new DeviceNoResponseException("设备无响应");
        }

        return isCmdRunSucFlag[0];
    }

    /**
     * 开始自动寻卡，同步阻塞方式，注意：不能在主线程里运行
     * @param delayMs      寻卡间隔,单位 10毫秒
     * @param bytCardType  ISO14443_P3 - 寻M1/UL卡
     *                      ISO14443_P4-寻CPU卡
     * @return             true - 操作成功
     *                      false - 操作失败
     * @throws DeviceNoResponseException
     *                  操作无响应时会抛出异常
     */
    public boolean startAutoSearchCard(byte delayMs, byte bytCardType) throws DeviceNoResponseException {
        final byte[][] returnBytes = new byte[1][1];
        final boolean[] isCmdRunSucFlag = {false};

        final Semaphore semaphore = new Semaphore(0);
        returnBytes[0] = null;

        requestRfmAutoSearchCard(true, delayMs, bytCardType, new onReceiveAutoSearchCardListener() {
            @Override
            public void onReceiveAutoSearchCard(boolean isSuc) {
                if (isSuc) {
                    isCmdRunSucFlag[0] = true;
                }
                else {
                    isCmdRunSucFlag[0] = false;
                }
                semaphore.release();
            }
        });

        try {
            semaphore.tryAcquire(DEVICE_NO_RESPONSE_TIME, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new DeviceNoResponseException("设备无响应");
        }

        return isCmdRunSucFlag[0];
    }

    /**
     * 停止自动寻卡，同步阻塞方式，注意：不能在主线程里运行
     * @return             true - 操作成功
     *                      false - 操作失败
     * @throws DeviceNoResponseException
     *                  操作无响应时会抛出异常
     */
    public boolean stoptAutoSearchCard() throws DeviceNoResponseException {
        final byte[][] returnBytes = new byte[1][1];
        final boolean[] isCmdRunSucFlag = {false};

        final Semaphore semaphore = new Semaphore(0);
        returnBytes[0] = null;

        requestRfmAutoSearchCard(false, (byte) 100, (byte) 0, new onReceiveAutoSearchCardListener() {
            @Override
            public void onReceiveAutoSearchCard(boolean isSuc) {
                if (isSuc) {
                    isCmdRunSucFlag[0] = true;
                }
                else {
                    isCmdRunSucFlag[0] = false;
                }
                semaphore.release();
            }
        });

        try {
            semaphore.tryAcquire(DEVICE_NO_RESPONSE_TIME, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new DeviceNoResponseException("设备无响应");
        }

        return !isCmdRunSucFlag[0];
    }

    /**
     * 关闭RF天线，同步阻塞方式，注意：不能在主线程里运行
     * @return             true - 操作成功
     *                      false - 操作失败
     * @throws DeviceNoResponseException
     *                  操作无响应时会抛出异常
     */
    public boolean closeRf() throws DeviceNoResponseException {
        final byte[][] returnBytes = new byte[1][1];
        final boolean[] isCmdRunSucFlag = {false};

        final Semaphore semaphore = new Semaphore(0);
        returnBytes[0] = null;

        requestRfmClose(new onReceiveRfmCloseListener() {
            @Override
            public void onReceiveRfmClose(boolean isSuc) {
                if (isSuc) {
                    isCmdRunSucFlag[0] = true;
                }
                else {
                    isCmdRunSucFlag[0] = false;
                }
                semaphore.release();
            }
        });

        try {
            semaphore.tryAcquire(DEVICE_NO_RESPONSE_TIME, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new DeviceNoResponseException("设备无响应");
        }

        return !isCmdRunSucFlag[0];
    }

    /**
     * 修改设备保存的序列号，同步阻塞方式，注意：不能在主线程里运行
     * @param serialNumberBytes  要修改的序列号，必须8字节
     * @return         true - 操作成功
     *                  false - 操作失败
     * @throws DeviceNoResponseException
     *                  操作无响应时会抛出异常
     */
    public boolean changeSerialNumber(byte[] serialNumberBytes) throws DeviceNoResponseException {
        final byte[][] returnBytes = new byte[1][1];
        final boolean[] isCmdRunSucFlag = {false};

        final Semaphore semaphore = new Semaphore(0);
        returnBytes[0] = null;

        requestSaveSerialNumber(serialNumberBytes, new onReceiveSaveSerialNumberListener() {
            @Override
            public void onReceiveSaveSerialNumber(boolean isSuc) {
                if (isSuc) {
                    isCmdRunSucFlag[0] = true;
                }
                else {
                    isCmdRunSucFlag[0] = false;
                }
                semaphore.release();
            }
        });

        try {
            semaphore.tryAcquire(DEVICE_NO_RESPONSE_TIME, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new DeviceNoResponseException("设备无响应");
        }

        return isCmdRunSucFlag[0];
    }

    /**
     * 获取设备保存的序列号，设备默认的序列号是FFFFFFFFFFFFFFFF，同步阻塞方式，注意：不能在主线程里运行
     * @return         返回的序列号，8字节
     * @throws DeviceNoResponseException
     *                  操作无响应时会抛出异常
     */
    public byte[] getSerialNumber() throws DeviceNoResponseException {
        synchronized(this) {
            final byte[][] returnBytes = new byte[1][1];
            final boolean[] isCmdRunSucFlag = {false};

            final Semaphore semaphore = new Semaphore(0);
            returnBytes[0] = null;

            requestGetSerialNumber(new onReceiveGetSerialNumberListener() {
                @Override
                public void onReceiveGetSerialNumber(boolean isCmdRunSuc, byte[] bytApduRtnData) {
                    if (isCmdRunSuc) {
                        returnBytes[0] = bytApduRtnData;
                        isCmdRunSucFlag[0] = true;
                    } else {
                        returnBytes[0] = null;
                        isCmdRunSucFlag[0] = false;
                    }
                    semaphore.release();
                }
            });

            try {
                semaphore.tryAcquire(DEVICE_NO_RESPONSE_TIME, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new DeviceNoResponseException("设备无响应");
            }

            if (!isCmdRunSucFlag[0]) {
                throw new DeviceNoResponseException("获取序列号失败");
            }
            return returnBytes[0];
        }
    }
}