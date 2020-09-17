package com.synjones.idcard.android;

import android.graphics.Bitmap;

public class Wlt2Bitmap {
    static {
        System.loadLibrary("wlt2bmp");
        System.loadLibrary("xzxjmk");
    }
    public static int IMG_WIDTH = 102;
    public static int IMG_HEIGHT = 126;
    public static int IMG_LENGTH = 3 * IMG_WIDTH * IMG_HEIGHT;
    public static  native int wlt2Bmp(byte[] inbuf, byte[] outbuf);

    public static Bitmap Bgr2Bitmap(byte[] bgrbuf) {
        Bitmap bmp = Bitmap.createBitmap(IMG_WIDTH, IMG_HEIGHT, Bitmap.Config.RGB_565);
        int row = 0, col = IMG_WIDTH - 1;
        for (int i = bgrbuf.length - 1; i >= 3; i -= 3) {
            bmp.setPixel(col--, row, bgrbuf[i] & 0xFF |(bgrbuf[i - 1] << 8) & 0xFF00 |((bgrbuf[i - 2]) << 16) & 0xFF0000);
            if (col < 0) {
                col = IMG_WIDTH - 1;
                row++;
            }
        }
        return bmp;
    }
}
