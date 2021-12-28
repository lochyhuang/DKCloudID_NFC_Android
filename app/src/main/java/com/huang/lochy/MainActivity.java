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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.DKCloudID.crypt.DKCloudID;
import com.DKCloudID.crypt.DKCloudIDException;
import com.DKCloudID.crypt.DKDeviceRegister;
import com.DKCloudID.crypt.IDCard;
import com.DKCloudID.crypt.IDCardData;
import com.DKCloudID.crypt.MsgCrypt;
import com.DKCloudID.crypt.StringTool;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    private EditText msgText = null;

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

    IDCard idCard;
    static long time_start = 0;
    static long time_end = 0;

    private static volatile StringBuffer msgBuffer;
    private ProgressDialog readWriteDialog = null;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        appID = "60273839";                                  //注意：此账号为样机账号，随时可能会被关闭。请向供应商询问正式账号密码
        key = "VwQC9MzMY5hVx/Ky61IYRgP3q/ZRujTjvZfcJAnC+1w=";//注意：此账号为样机账号，随时可能会被关闭。请向供应商询问正式账号密码
        msgCrypt = new MsgCrypt(this, appID, key);
        device_id = msgCrypt.getDeviceId();
        app_id = msgCrypt.getAppId();
        System.out.println("设备ID=" + StringTool.byteHexToSting(device_id) + "\r\nappID=" + new String(app_id));

        //初始化IDCard
        idCard = new IDCard(MainActivity.this, msgCrypt);

        //UI初始化
        initUI();
        msgText.setText("请把身份证放到卡片识别区域\r\n注意：此账号为样机账号，随时可能会被关闭。请向供应商询问正式账号密码\r\n");
    }

    /* perform when it brings close to TAG or after write button click */
    @Override
    public void onNewIntent(Intent intent_nfc) {
        final Tag tag = intent_nfc.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        final NfcB nfcB = NfcB.get(tag);
        /* Type B */
        if (nfcB != null) {
            synchronized (this) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        int cnt = 0;
                        boolean read_ok;
                        msgBuffer.delete(0, msgBuffer.length());
                        time_start = System.currentTimeMillis();

                        myTTS.speak("正在读卡，请勿移动身份证");
                        do {
                            try {
                                /*获取身份证数据，带进度回调，如果不需要进度回调可以去掉进度回调参数或者传入null*/
                                IDCardData idCardData = idCard.getIDCardData(nfcB, new IDCard.onReceiveScheduleListener() {
                                    @Override
                                    public void onReceiveSchedule(int rate) {  //读取进度回调
                                        showReadWriteDialog("正在读取身份证信息,请不要移动身份证", rate);
                                        if (rate == 100) {
                                            time_end = System.currentTimeMillis();
                                            /**
                                             * 这里已经完成读卡，可以开身份证了，在此提示用户读取成功或者打开蜂鸣器提示可以拿开身份证了
                                             */

                                            myTTS.speak("读取成功");
                                        }
                                    }
                                });

                                //显示身份证数据
                                showIDCardData(idCardData);
                                read_ok = true;
                            } catch (DKCloudIDException e) {
                                e.printStackTrace();
                                read_ok = false;

                                //显示错误信息
                                msgBuffer.delete(0, msgBuffer.length());
                                msgBuffer.append(e.getMessage()).append("\r\n");
                                handler.sendEmptyMessage(0);
                                myTTS.speak("请不要移动身份证");
                            } finally {
                                //读卡结束关闭进度条显示
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
                        } while ((nfcB.isConnected()) && !read_ok && (cnt++ < 5));  //如果失败则重复读5次直到成功
                    }
                }).start();
            }
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

    //UI初始化
    private void initUI() {
        msgBuffer = new StringBuffer();
        msgText = (EditText) findViewById(R.id.msgText);
        Button clearButton = (Button) findViewById(R.id.clearButton);
        Button getDeviceIdBt = (Button)findViewById(R.id.getDeviceIdBt);

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
                                            msgBuffer.append("注册成功");
                                        }
                                        else {
                                            msgBuffer.append("注册失败：" + dkDeviceRegister.getErrMsg());
                                        }
                                        handler.sendEmptyMessage(0);
                                    }
                                }).start();
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }
        });
    }

    //发送读写进度条显示Handler
    private void showReadWriteDialog(String msg, int rate) {
        Message message = new Message();
        message.what = 4;
        message.arg1 = rate;
        message.obj = msg;
        handler.sendMessage(message);
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

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            msgText.setText(/*"解析成功次数：" + suc_cnt + "\r\n解析失败次数：" + err_cnt + */ msgBuffer + "\r\n");

            switch (msg.what) {
                case 1:
                    break;
                case 2:
                    break;
                case 3:
                    break;

                case 4:
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

        DKCloudID.Close();
    }
}
