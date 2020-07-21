package com.DKCloudID.crypt;

public class MsgCrypt {
    static {
        System.loadLibrary("dkcloudid_crypt");
    }

    public native byte[] getDnData();
    public native byte[] analyze(byte[] dnBytes);
    public native byte[] getApduData(byte[] dnBytes);
}
