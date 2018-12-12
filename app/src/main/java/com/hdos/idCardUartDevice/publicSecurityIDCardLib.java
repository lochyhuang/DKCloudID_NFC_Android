package com.hdos.idCardUartDevice;

public class publicSecurityIDCardLib {
    static {
        System.loadLibrary("IdentityCardUart");
//        System.loadLibrary("wlt2bmp");
    }

    public final native byte[] HdosIdUnpack(byte[] var1, String var2);
}
