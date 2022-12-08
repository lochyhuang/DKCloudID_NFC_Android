package com.huang.lochy;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcB;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.DKCloudID.crypt.MsgCrypt;
import com.dk.log.DKLog;
import com.dk.log.DKLogCallback;
import com.dk.nfc.DKCloudID.DKCloudID;
import com.dk.nfc.DKCloudID.DKDeviceRegister;
import com.dk.nfc.DKCloudID.IDCardData;
import com.dk.nfc.DeviceManager.DKNfcDevice;
import com.dk.nfc.DeviceManager.DeviceManagerCallback;
import com.dk.nfc.Exception.DKCloudIDException;
import com.dk.nfc.Tool.StringTool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    private EditText msgText = null;
    private TextView delayTextView = null;

    private MyTTS myTTS;
    private NfcAdapter mAdapter;
    private PendingIntent pendingIntent;
    private IntentFilter[] intentFiltersArray;
    private String[][] techListsArray;

    private String appID;
    private String key;
    private MsgCrypt msgCrypt;
    private byte[] device_id;
    private byte[] app_id;

    DKNfcDevice dkNfcDevice;
    static long time_start = 0;
    static long time_end = 0;

    private static String server_delay = "";
    private static int net_status = 1;

    private static volatile StringBuffer msgBuffer;
    private ProgressDialog readWriteDialog = null;

    final Semaphore semaphore = new Semaphore(1);

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //UI初始化
        initUI();
        msgText.setText("请把身份证放到卡片识别区域\r\n注意：此账号为样机账号，随时可能会被关闭。请向供应商询问正式账号密码\r\n");

        //日志初始化
        DKLog.setLogCallback(logCallback);

        //语音初始化
        myTTS = new MyTTS(this);

        //NFC初始化
        mAdapter = NfcAdapter.getDefaultAdapter(this);
        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
                getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        IntentFilter tech = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        intentFiltersArray = new IntentFilter[]{tech,};
        techListsArray = new String[][]{new String[]{NfcB.class.getName()}};

        //获取IMEI权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                MainActivity.this.requestPermissions(new String[]{
                        Manifest.permission.READ_PHONE_STATE,
                }, 1);
            }
        }

        //云解码初始化
        appID = "60273839";                                     //注意：此账号为样机账号，随时可能会被关闭。请向供应商询问正式账号密码
        key = "VwQC9MzMY5hVx/Ky61IYRgP3q/ZRujTjvZfcJAnC+1w=";   //注意：此账号为样机账号，随时可能会被关闭。请向供应商询问正式账号密码
        msgCrypt = new MsgCrypt(this, appID, key);
        device_id = msgCrypt.getDeviceId();
        app_id = msgCrypt.getAppId();
        Log.d(TAG, "设备ID=" + StringTool.byteHexToSting(device_id) + "\r\nappID=" + new String(app_id));

        //初始化设备
        dkNfcDevice = new DKNfcDevice(msgCrypt);
        dkNfcDevice.setCallBack(deviceManagerCallback);

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    String lost = new String();
                    String delay = new String();

                    try {
                        Process p = Runtime.getRuntime().exec("ping -c 1 -w 10 " + "www.dkcloudid.cn");
                        net_status = p.waitFor();
                        //DKLog.d(TAG, "Process:" + net_status );

                        if (net_status == 0) {
                            BufferedReader buf = new BufferedReader(new InputStreamReader(p.getInputStream()));
                            String str = new String();
                            while (true) {
                                try {
                                    if (!((str = buf.readLine()) != null)) break;
                                } catch (IOException e) {
                                    DKLog.e(TAG, e);
                                }

                                if (str.contains("avg")) {
                                    int i = str.indexOf("/", 20);
                                    int j = str.indexOf(".", i);

                                    delay = str.substring(i + 1, j);
                                    server_delay = delay;
                                }
                            }

                            //DKLog.d(TAG, "延迟:" + delay + "ms");
                        }
                        else {
                            //DKLog.d(TAG, "网络未连接！");
                        }
                    } catch (Exception e) {
                        DKLog.e(TAG, e);
                    }

                    showNETDelay();

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }).start();
    }

    /* perform when it brings close to TAG or after write button click */
    @Override
    public void onNewIntent(Intent intent_nfc) {
        final Tag tag = intent_nfc.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        final NfcB nfcB = NfcB.get(tag);
        if (nfcB != null) {
            dkNfcDevice.onFinCard(nfcB);
        }
    }

    //日志回调
    private DKLogCallback logCallback = new DKLogCallback() {
        @Override
        public void onReceiveLogI(String tag, String msg) {
            super.onReceiveLogI(tag, msg);
            Log.i(tag, msg);
            logViewln("[I] " + msg);
        }

        @Override
        public void onReceiveLogD(String tag, String msg) {
            super.onReceiveLogD(tag, msg);
            Log.d(tag, msg);
            logViewln("[D] " + msg);
        }

        @Override
        public void onReceiveLogE(String tag, String msg) {
            super.onReceiveLogE(tag, msg);
            Log.e(tag, msg);
            logViewln("[E] " + msg);
        }
    };

    //设备操作类回调
    private DeviceManagerCallback deviceManagerCallback = new DeviceManagerCallback() {
        //身份证开始请求云解析回调
        @Override
        public void onReceiveSamVIdStart(byte[] initData) {
            super.onReceiveSamVIdStart(initData);

            Log.d(TAG, "开始解析");
            logViewln(null);
            logViewln("正在读卡，请勿移动身份证!");
            myTTS.speak("正在读卡，请勿移动身份证");

            time_start = System.currentTimeMillis();
        }

        //身份证云解析进度回调
        @Override
        public void onReceiveSamVIdSchedule(int rate) {
            super.onReceiveSamVIdSchedule(rate);
            showReadWriteDialog("正在读取身份证信息,请不要移动身份证", rate);
            if (rate == 100) {
                time_end = System.currentTimeMillis();

                /**
                 * 这里已经完成读卡，可以拿开身份证了，在此提示用户读取成功或者打开蜂鸣器提示可以拿开身份证了
                 */
                myTTS.speak("读取成功");
            }
        }

        //身份证云解析异常回调
        @Override
        public void onReceiveSamVIdException(String msg) {
            super.onReceiveSamVIdException(msg);

            //显示错误信息
            logViewln(msg);

            //读卡结束关闭进度条显示
            hidDialog();
        }

        //身份证云解析明文结果回调
        @Override
        public void onReceiveIDCardData(IDCardData idCardData) {
            super.onReceiveIDCardData(idCardData);

            showIDCardData(idCardData);
        }
    };

    //清空显示按键监听
    private class claerButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            logViewln(null);
        }
    }

    //UI初始化
    private void initUI() {
        msgBuffer = new StringBuffer();
        msgText = (EditText) findViewById(R.id.msgText);
        Button clearButton = (Button) findViewById(R.id.clearButton);
        Button getDeviceIdBt = (Button)findViewById(R.id.getDeviceIdBt);
        delayTextView = findViewById(R.id.delayTextView);

        msgText.setKeyListener(null);
        msgText.setTextIsSelectable(true);

        clearButton.setOnClickListener(new claerButtonListener());

        //进度条初始化
        readWriteDialog = new ProgressDialog(MainActivity.this);
        readWriteDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        readWriteDialog.setMax(100);
        // 设置ProgressDialog 标题
        readWriteDialog.setTitle("请稍等");
        // 设置ProgressDialog 提示信息
        readWriteDialog.setMessage("正在读取数据……");

        //激活设备按键
        getDeviceIdBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                msgBuffer.delete(0, msgBuffer.length());
                msgText.setText("设备ID=" + StringTool.byteHexToSting(device_id) + "\r\nappID=" + new String(app_id));

                LayoutInflater factory = LayoutInflater.from(MainActivity.this);
                final View textEntryView = factory.inflate(R.layout.layout, null);

                final EditText devceNameEditText = (EditText) textEntryView.findViewById(R.id.nameInputEditText);
                devceNameEditText.setHint(new SpannableString("请输入设备名称"));
//                devceNameEditText.setText("深圳市德科物联技术有限公司");
                final EditText licenseEditText =  (EditText) textEntryView.findViewById(R.id.licenseInputEditText);
                licenseEditText.setHint(new SpannableString("请输入授权码"));
//                licenseEditText.setText("784a5a5255724d756a5766486b4a7164");
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("设备注册")
                        .setMessage("请输入设备名称和授权码")
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setView(textEntryView)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                final String name = devceNameEditText.getText().toString();
                                final String license = licenseEditText.getText().toString();
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        DKDeviceRegister dkDeviceRegister = new DKDeviceRegister();
                                        boolean status = dkDeviceRegister.register(msgCrypt, license, name, key);
                                        if (status) {
                                            logViewln("注册成功");
                                        }
                                        else {
                                            logViewln("注册失败：" + dkDeviceRegister.getErrMsg());
                                        }
                                    }
                                }).start();
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }
        });
    }

    private void logViewln(String string) {
        final String msg = string;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (msg == null) {
                    msgText.setText("");
                    return;
                }

//                if (msgText.length() > 1000) {
//                    msgText.setText("");
//                }
                msgText.append(msg + "\r\n");
                int offset = msgText.getLineCount() * msgText.getLineHeight();
                if(offset > msgText.getHeight()){
                    msgText.scrollTo(0,offset-msgText.getHeight());
                }
            }
        });
    }

    //进度条显示
    private void showReadWriteDialog(String msg, int rate) {
        final int theRate = rate;
        final String theMsg = msg;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if ((theRate == 0) || (theRate == 100)) {
                    readWriteDialog.dismiss();
                    readWriteDialog.setProgress(0);
                } else {
                    readWriteDialog.setMessage(theMsg);
                    readWriteDialog.setProgress(theRate);
                    if (!readWriteDialog.isShowing()) {
                        readWriteDialog.show();
                    }
                }
            }
        });
    }

    //显示网络延迟
    private void showNETDelay() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if ( (net_status == 0) && (server_delay != null) && (server_delay.length() > 0) ) {
                    int delay = Integer.parseInt(server_delay);
                    String pj = "优";
                    if (delay < 30) {
                        pj = "优";
                        delayTextView.setTextColor(0xF000ff00);
                    } else if (delay < 50) {
                        pj = "良";
                        delayTextView.setTextColor(0xF0EEC900);
                    } else if (delay < 100) {
                        pj = "差";
                        delayTextView.setTextColor(0xF0FF0000);
                    } else {
                        pj = "极差";
                        delayTextView.setTextColor(0xF0B22222);
                    }
                    delayTextView.setText("网络延迟：" + server_delay + "ms " + " 等级：" + pj);
                }
                else {
                    delayTextView.setTextColor(0xF0B22222);
                    delayTextView.setText("网络未连接！");
                }
            }
        });
    }

    //显示身份证数据
    private void showIDCardData(IDCardData idCardData) {
        final IDCardData theIDCardData = idCardData;

        //显示照片和指纹
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                msgText.setText("解析成功：(读卡用时" + (time_end - time_start) + "ms)" + theIDCardData.toString() + "\r\n");

                //获取指纹数据
                String fingerprintString = "";
                if (theIDCardData.fingerprintBytes != null && theIDCardData.fingerprintBytes.length > 0) {
                    fingerprintString = "\r\n指纹数据：\r\n" + StringTool.byteHexToSting(theIDCardData.fingerprintBytes);
                }

                SpannableString ss = new SpannableString(msgText.getText().toString()+"[smile]");
                //得到要显示图片的资源
                Drawable d = new BitmapDrawable(theIDCardData.PhotoBmp); //Drawable.createFromPath("mnt/sdcard/photo.bmp");
                //设置高度
                d.setBounds(0, 0, d.getIntrinsicWidth() * 10, d.getIntrinsicHeight() * 10);
                //跨度底部应与周围文本的基线对齐
                ImageSpan span = new ImageSpan(d, ImageSpan.ALIGN_BASELINE);
                //附加图片
                ss.setSpan(span, msgText.getText().length(),msgText.getText().length()+"[smile]".length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                msgText.setText(ss);

                //显示指纹数据
                msgText.append(fingerprintString);
            }
        });
    }

    //隐藏进度条
    private void hidDialog() {
        //关闭进度条显示
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (readWriteDialog.isShowing()) {
                    readWriteDialog.dismiss();
                }
                readWriteDialog.setProgress(0);
            }
        });
    }

    @Override
    protected void onResume() {
        mAdapter.enableForegroundDispatch(
                this, pendingIntent, intentFiltersArray, techListsArray);
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (this.isFinishing()) {
            mAdapter.disableForegroundDispatch(this);
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        dkNfcDevice.destroy();
    }
}
