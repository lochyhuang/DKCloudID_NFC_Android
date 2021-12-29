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
 **Step 2. 添加 implementation 'com.gitee.lochy:dkcloudid-nfc-android-sdk:v1.0.0' 到dependency** 

```

dependencies {
	implementation 'com.gitee.lochy:dkcloudid-nfc-android-sdk:v1.0.0'
		
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

    //云解码初始化
    appID = "60273839";                                  //注意：此账号为样机账号，随时可能会被关闭。请向供应商询问正式账号密码
    key = "VwQC9MzMY5hVx/Ky61IYRgP3q/ZRujTjvZfcJAnC+1w=";//注意：此账号为样机账号，随时可能会被关闭。请向供应商询问正式账号密码
    msgCrypt = new MsgCrypt(this, appID, key);
    device_id = msgCrypt.getDeviceId();
    app_id = msgCrypt.getAppId();

    //初始化IDCard
    idCard = new IDCard(MainActivity.this, msgCrypt);
```

 **Step 5. 添加读卡代码** 

```

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
                        do {
                            try {
                                /*获取身份证数据*/
                                IDCardData idCardData = idCard.getIDCardData(nfcB);

                                //显示身份证数据
                                showIDCardData(idCardData);
                                
                                read_ok = true;
                            } catch (DKCloudIDException e) {
                                e.printStackTrace();
                                read_ok = false;
                            }
                        } while ((nfcB.isConnected()) && !read_ok && (cnt++ < 5));  //如果失败则重复读5次直到成功
                    }
                }).start();
            }
        }
    }
```
