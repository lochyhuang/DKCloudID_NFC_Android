package com.huang.lochy;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.DKCloudID.crypt.ApduTransceive;
import com.DKCloudID.crypt.MsgCrypt;

import java.io.IOException;
import java.util.Arrays;

import javax.xml.transform.sax.TransformerHandler;

public class MainActivity extends Activity {
    private EditText msgText = null;

    private NfcAdapter mAdapter;
    private PendingIntent pendingIntent;
    private IntentFilter[] intentFiltersArray;
    private String[][] techListsArray;

    private MsgCrypt msgCrypt;

    private static volatile StringBuffer msgBuffer;
    private ProgressDialog readWriteDialog = null;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        msgBuffer = new StringBuffer();
        msgText = (EditText)findViewById(R.id.msgText);
        Button clearButton = (Button) findViewById(R.id.clearButton);

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

        /* get Tag Intent during application starting(1/3) */
        mAdapter = NfcAdapter.getDefaultAdapter(this);
        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
                getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        IntentFilter tech = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        intentFiltersArray = new IntentFilter[]{tech,};
        techListsArray = new String[][]{new String[]{NfcB.class.getName()}};

        //获取存储权限
        MainActivity.isGrantExternalRW(MainActivity.this);

        msgCrypt = new MsgCrypt();

        msgText.setText("请把身份证放到卡片识别区域");
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
    }

    /* perform when it brings close to TAG or after write button click */
    @Override
    public void onNewIntent(Intent intent_nfc) {
        byte[] r_data = new byte[30];
        final Tag tag = (Tag) intent_nfc.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        final NfcB nfcB = NfcB.get(tag);
        /* Type B */
        if (nfcB != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    int cnt = 0;
                    int status = 0;
                    do {
                        status = dkcloudid_crypt(nfcB);
                    }while ( (nfcB.isConnected()) && (status != 0) && (cnt++ < 5) );
                }
            }).start();
        }
    }

    private synchronized int dkcloudid_crypt(final NfcB nfcB) {
        //NfcB nfcB = NfcB.get(tag);
        msgBuffer.delete(0, msgBuffer.length());
        msgBuffer.append("读到身份证！\r\n");
        handler.sendEmptyMessage(0);
        byte[] msgReturnBytes;

        try {
            if (!nfcB.isConnected()) {
                nfcB.connect();
            }

            ApduTransceive.setTag(nfcB);

            byte[] initData = msgReturnBytes = msgCrypt.getDnData();
            if (msgReturnBytes == null) {
                msgBuffer.delete(0, msgBuffer.length());
                msgBuffer.append("身份证读取错误").append("\r\n");
                handler.sendEmptyMessage(0);
                if ( !nfcB.isConnected() ) {
                    msgBuffer.append("身份证已拿开");
                    handler.sendEmptyMessage(0);
                }
                return 1;
            }

            DKCloudID dkCloudID = new DKCloudID();
            if ( !dkCloudID.isConnected() ) {
                msgBuffer.append("服务器连接失败");
                handler.sendEmptyMessage(0);
                return 1;
            }
            System.out.println("向服务器发送数据：" + StringTool.byteHexToSting(msgReturnBytes));
            byte[] cloudReturnByte = dkCloudID.dkCloudTcpDataExchange(msgReturnBytes);
            System.out.println("接收到服务器数据：" + StringTool.byteHexToSting(cloudReturnByte));
            msgBuffer.append("正在解析:1%");
            handler.sendEmptyMessage(0);
            int schedule = 1;
            if ( (cloudReturnByte != null) && (cloudReturnByte.length >= 2)
                    && ((cloudReturnByte[0] == 0x03) || (cloudReturnByte[0] == 0x04)) ) {
                showReadWriteDialog("正在读取身份证信息,请不要移动身份证", 1);
            }

            byte[] nfcReturnBytes;
            while (true) {
                if ( (cloudReturnByte == null) || (cloudReturnByte.length < 2)
                        || ((cloudReturnByte[0] != 0x03) && (cloudReturnByte[0] != 0x04)) ) {

                    msgBuffer.delete(0, msgBuffer.length());
                    if ( cloudReturnByte == null ) {
                        msgBuffer.append("服务器返回数据为空").append("\r\n");
                    }
                    else if (cloudReturnByte[0] == 0x05) {
                        msgBuffer.append("解析失败, 请重新读卡").append("\r\n");
                    }
                    else if (cloudReturnByte[0] == 0x06) {
                        msgBuffer.append("该设备未授权, 请联系www.derkiot.com获取授权").append("\r\n");
                    }
                    else if (cloudReturnByte[0] == 0x07) {
                        msgBuffer.append("该设备已被禁用, 请联系www.derkiot.com").append("\r\n");
                    }
                    else if (cloudReturnByte[0] == 0x08) {
                        msgBuffer.append("该账号已被禁用, 请联系www.derkiot.com").append("\r\n");
                    }
                    else if (cloudReturnByte[0] == 0x09) {
                        msgBuffer.append("余额不足, 请联系www.derkiot.com充值").append("\r\n");
                    }
                    else {
                        msgBuffer.append("未知错误").append("\r\n");
                    }
                    handler.sendEmptyMessage(0);
                    dkCloudID.Close();
                    return 1;
                }
                else if ((cloudReturnByte[0] == 0x04) && (cloudReturnByte.length > 300)) {
                    byte[] decrypted = new byte[cloudReturnByte.length - 3];
                    System.arraycopy(cloudReturnByte, 3, decrypted, 0, decrypted.length);

                    final IDCardData idCardData = new IDCardData(decrypted, MainActivity.this);
                    System.out.println("解析成功：" + idCardData.toString());

                    //从服务器获取照片
                    initData[0] = (byte)0xA0;
                    if ( dkCloudID != null ) {
                        dkCloudID.Close();
                    }
                    dkCloudID = new DKCloudID();
                    cloudReturnByte = dkCloudID.dkCloudTcpDataExchange(initData);
                    dkCloudID.Close();
                    if ( (cloudReturnByte == null) || (cloudReturnByte.length < 4)) {
                        msgBuffer.append("获取图片失败！");
                        handler.sendEmptyMessage(0);
                    }
                    else {
                        byte[] imageBytes = Arrays.copyOfRange( cloudReturnByte, 3, cloudReturnByte.length );
                        System.out.println("获取到的照片路径：" + StringTool.byteHexToSting(imageBytes));
                        //idCardData.PhotoBmp = GetNetPicture.getURLimage("http://www.dkcloudid.cn:8090/image/" + StringTool.byteHexToSting(imageBytes) + ".bmp");
                        //idCardData.PhotoBmp = GetNetPicture.getURLimage("http://yjm1.dkcloudid.cn:8080/image/" + StringTool.byteHexToSting(imageBytes) + ".bmp");
                    }

                    msgBuffer.delete(0, msgBuffer.length());
                    msgBuffer.append("解析成功：" + idCardData.toString());
                    handler.sendEmptyMessage(0);

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
                    break;
                }

                msgReturnBytes = msgCrypt.analyze(cloudReturnByte);

                if (msgReturnBytes == null) {
                    msgBuffer.delete(0, msgBuffer.length());
                    msgBuffer.append("解析出错").append("\r\n");
                    handler.sendEmptyMessage(0);
                    if ( !nfcB.isConnected() ) {
                        msgBuffer.append("身份证已拿开");
                        handler.sendEmptyMessage(0);
                    }
                    dkCloudID.Close();
                    return 1;
                }
                if (msgReturnBytes.length == 2) {
                    msgBuffer.delete(0, msgBuffer.length());
                    msgBuffer.append("解析出错：").append(String.format("%d", ((msgReturnBytes[0] & 0xff) << 8) | (msgReturnBytes[1] & 0xff) )).append("\r\n");
                    handler.sendEmptyMessage(0);

                    dkCloudID.Close();
                    return 1;
                }

                System.out.println("向服务器发送数据：" + StringTool.byteHexToSting(msgReturnBytes));
                cloudReturnByte = dkCloudID.dkCloudTcpDataExchange(msgReturnBytes);
                System.out.println("接收到服务器数据：" + StringTool.byteHexToSting(cloudReturnByte));
                msgBuffer.delete(0, msgBuffer.length());
                msgBuffer.append(String.format("正在解析%%%d", (int)((++schedule) * 100 / 4.0)));
                handler.sendEmptyMessage(0);
                showReadWriteDialog("正在读取身份证信息,请不要移动身份证", (int)(schedule * 100 / 4.0));
            }
            dkCloudID.Close();
        } catch (IOException e) {
            e.printStackTrace();
            msgBuffer.append("未检测到身份证，请将身份证放置于感应区域");
            handler.sendEmptyMessage(0);
            try {
                nfcB.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        finally {
            //关闭进度条
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    readWriteDialog.dismiss();
                    readWriteDialog.setProgress(0);
                }
            });
        }

        return 0;
    }

    //清空显示按键监听
    private class claerButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            msgBuffer.delete(0, msgBuffer.length());
            handler.sendEmptyMessage(0);
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
}
