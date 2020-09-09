package com.DKCloudID.crypt;

import android.content.Context;

public class MsgCrypt {
    static {
        System.loadLibrary("dkcloudid_crypt");
    }

    public MsgCrypt(Context context) {
        init(context);
    }

    public native boolean init(Context context);
    public native byte[] getDnData();
    public native byte[] analyze(byte[] dnBytes);
}
