# DKCloudID_NFC_Android

#### 介绍
深圳市德科物联技术有限公司的手机NFC读取身份证明文信息Demo。更多信息请访问[德科官网](http://www.derkiot.com/)。

### 如何集成到项目中
 **Step 1. Add the JitPack repository to your build file**
 
打开根build.gradle文件，将maven { url 'https://jitpack.io' }添加到repositories的末尾

```
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```
 **Step 2. 添加 implementation 'com.gitee.lochy:dkcloudid-nfc-android-sdk:v2.0.1' 到dependency** 

```

dependencies {
    implementation 'com.gitee.lochy:dkcloudid-nfc-android-sdk:v2.0.1'
        
    //注册设备POST请求要用到
    implementation "com.squareup.okhttp3:okhttp:4.9.0"
    implementation 'com.squareup.okio:okio:2.8.0'
}
```

 **Step 3. 在AndroidManifest.xml中添加网络权限、获取手机IMEI权限、访问网络权限** 
 
 ```

    <uses-permission android:name="android.permission.NFC" />
    <uses-permission android:name="android.permission.READ_PHONE_STATE" />
    <uses-permission android:name="android.permission.INTERNET" />
```
 
 
 **Step 4. NFC、云解码、IDCard初始化以及IMEI权限请求** 

```

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

    //云解码初始化.注意：此账号为样机账号，随时可能会被关闭。请向供应商询问正式账号密码
    MsgCrypt msgCrypt = new MsgCrypt(this, "60273839", "VwQC9MzMY5hVx/Ky61IYRgP3q/ZRujTjvZfcJAnC+1w="); 

    //初始化设备
    dkNfcDevice = new DKNfcDevice(msgCrypt);
    dkNfcDevice.setCallBack(deviceManagerCallback);
```

 **Step 5. 添加读卡回调** 

```

    @Override
    public void onNewIntent(Intent intent_nfc) {
        final Tag tag = intent_nfc.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        final NfcB nfcB = NfcB.get(tag);
        if (nfcB != null) {
            dkNfcDevice.onFinCard(nfcB);
        }
    }
    
    //设备操作类回调
    private DeviceManagerCallback deviceManagerCallback = new DeviceManagerCallback() {
        //身份证开始请求云解析回调
        @Override
        public void onReceiveSamVIdStart(byte[] initData) {
            super.onReceiveSamVIdStart(initData);

            Log.d(TAG, "开始解析");
        }

        //身份证云解析进度回调
        @Override
        public void onReceiveSamVIdSchedule(int rate) {
            super.onReceiveSamVIdSchedule(rate);
        }

        //身份证云解析异常回调
        @Override
        public void onReceiveSamVIdException(String msg) {
            super.onReceiveSamVIdException(msg);

            //显示错误信息
            //logViewln(msg);
        }

        //身份证云解析明文结果回调
        @Override
        public void onReceiveIDCardData(IDCardData idCardData) {
            super.onReceiveIDCardData(idCardData);

            //显示身份证数据
            showIDMsg(idCardData);
        }
    };
```

 **Step 6. 在onResue和onPause中添加如下代码** 

```

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
```