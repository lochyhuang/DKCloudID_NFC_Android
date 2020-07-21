package com.huang.lochy;

import java.util.Random;

public class UtilTool {
    public static byte[] mergeByte(byte[] source, byte[] s, int begin, int end) {
        byte[] bytes = new byte[source.length + end - begin];
        System.arraycopy(source, 0, bytes, 0, source.length);
        System.arraycopy(s, begin, bytes, source.length, end);

        return bytes;
    }

    public static byte[] shortToByte(short len) {
        int temp = len;
        byte[] b = new byte[2];
        b[0] = (byte) (temp & 0xff);
        temp = temp >> 8;
        b[1] = (byte) (temp & 0xff);

        return b;
    }

    public static short byteToShort(byte[] b) {
        short s = 0;
        short s0 = (short) (b[0] & 0xff);
        short s1 = (short) (b[1] & 0xff);

        s1 <<= 8;
        s = (short) (s0 | s1);

        return s;
    }

    // 随机生成16位字符串
    public static String getRandomStr(int len) {
        String base = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < len; i++) {
            int number = random.nextInt(base.length());
            sb.append(base.charAt(number));
        }
        return sb.toString();
    }
    
    //异或和校验
    public static byte bcc_check(byte[] bytes) {
    	byte bcc_sum = 0;
    	for ( byte theByte : bytes ) {
    		bcc_sum ^= theByte;
    	}
    	
    	return bcc_sum;
    }
}






