package com.huang.lochy;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.RemoteException;

import com.hdos.idCardUartDevice.publicSecurityIDCardLib;

import java.io.UnsupportedEncodingException;

/**
 * Created by lochy on 2018-06-08.
 */

public class IDCardData {
    public final static int ID_TYPE_CN = 1;       //身份证类型-居民身份证
    public final static int ID_TYPE_GAT = 2;      //身份证类型-港澳台居民身份证
    public final static int ID_TYPE_FOREIGN = 3;  //身份证类型-外国人永久居留身份证

    private Context mContext;

    public String Name = null;                   // 姓名
    public String Sex = null;                    //性别
    public String Nation = null;                 //名族
    public String Born = null;                   //出生
    public String Address = null;                //住址
    public String IDCardNo = null;               //身份证号
    public String GrantDept = null;              //签发机关
    public String UserLifeBegin = null;          //有效期起始日期
    public String UserLifeEnd = null;            //有效期结束日期
    public String passport = null;               //通行证号码
    public String issueNumber = null;            //签发次数

    public String reserved = null;
    public Bitmap PhotoBmp = null;
    public byte[] fingerprintBytes = null;       //指纹数据
    public int type = 0;

    private String bmpPath =   "mnt/sdcard/dkcloudid/photo.bmp";
    private String wltPath =  "mnt/sdcard/dkcloudid/photo.wlt";

    public IDCardData(byte[] idCardBytes, Context context){
        mContext = context;

        if (idCardBytes.length < 1295) {
            return;
        }

        if ( (idCardBytes[0] == (byte)0xaa)
                && (idCardBytes[1] == (byte)0xaa)
                && (idCardBytes[2] == (byte)0xaa)
                && (idCardBytes[3] == (byte)0x96)
                && (idCardBytes[4] == (byte)0x69)) {

            //int totalLen = ((idCardBytes[5] & 0xff) << 8) | (idCardBytes[6] & 0xff);
            int wordMsgBytesLen = ((idCardBytes[10] & 0xff) << 8) | (idCardBytes[11] & 0xff);
            int photoMsgBytesLen = ((idCardBytes[12] & 0xff) << 8) | (idCardBytes[13] & 0xff);

            byte[] wordMsgBytes = new byte[wordMsgBytesLen];
            byte[] photoMsgBytes = new byte[photoMsgBytesLen];

            if (idCardBytes.length == 1295) {   //不带指纹
                System.arraycopy(idCardBytes, 14, wordMsgBytes, 0, wordMsgBytesLen);
                System.arraycopy(idCardBytes, 14 + wordMsgBytesLen, photoMsgBytes, 0, photoMsgBytesLen);
            }
            else {   //带指纹
                int fingerprintBytesLen = ((idCardBytes[14] & 0xff) << 8) | (idCardBytes[15] & 0xff);   //指纹长度
                fingerprintBytes = new byte[fingerprintBytesLen];
                System.arraycopy(idCardBytes, 16, wordMsgBytes, 0, wordMsgBytesLen);
                System.arraycopy(idCardBytes, 16 + wordMsgBytesLen, photoMsgBytes, 0, photoMsgBytesLen);
                System.arraycopy(idCardBytes, 16 + wordMsgBytesLen + photoMsgBytesLen, fingerprintBytes, 0, fingerprintBytesLen);
            }

            //判断身份证的类型是否为港澳台身份证
            if (wordMsgBytes[248] == 'J') {
                type = ID_TYPE_GAT;
            }
            else if (wordMsgBytes[248] == 'I') {
                type = ID_TYPE_FOREIGN;
            }
            else {
                type = ID_TYPE_CN;
            }

            byte[] bytes;
            String str;
            int index = 0;

            //姓名
            bytes = new byte[30];
            System.arraycopy(wordMsgBytes, index, bytes, 0, bytes.length);
            index += bytes.length;
            try {
                Name = new String(bytes, "UTF_16LE");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            //性别
            if (wordMsgBytes[30] == 0x31) {
                Sex = "男";
            }
            else {
                Sex = "女";
            }
            index += 2;

            //名族
            if (type == ID_TYPE_CN) {
                bytes = new byte[4];
                System.arraycopy(wordMsgBytes, index, bytes, 0, bytes.length);
                try {
                    str = new String(bytes, "UTF_16LE");
                    if (str.length() == 2) {
                        int nationCode = Integer.valueOf(str, 10);
                        Nation = getNation(nationCode);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            index += 4;

            //出生
            bytes = new byte[16];
            System.arraycopy(wordMsgBytes, index, bytes, 0, bytes.length);
            index += bytes.length;
            try {
                Born = new String(bytes, "UTF_16LE");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            //住址
            bytes = new byte[70];
            System.arraycopy(wordMsgBytes, index, bytes, 0, bytes.length);
            index += bytes.length;
            try {
                Address = new String(bytes, "UTF_16LE");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            //身份证号
            bytes = new byte[36];
            System.arraycopy(wordMsgBytes, index, bytes, 0, bytes.length);
            index += bytes.length;
            try {
                IDCardNo = new String(bytes, "UTF_16LE");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            //签发机关
            bytes = new byte[30];
            System.arraycopy(wordMsgBytes, index, bytes, 0, bytes.length);
            index += bytes.length;
            try {
                GrantDept = new String(bytes, "UTF_16LE");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            //有效起始日期
            bytes = new byte[16];
            System.arraycopy(wordMsgBytes, index, bytes, 0, bytes.length);
            index += bytes.length;
            try {
                UserLifeBegin = new String(bytes, "UTF_16LE");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            //有效结束日期
            bytes = new byte[16];
            System.arraycopy(wordMsgBytes, index, bytes, 0, bytes.length);
            index += bytes.length;
            try {
                UserLifeEnd = new String(bytes, "UTF_16LE");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            //港澳台身份证
            if (type == ID_TYPE_GAT) {
                //通行证号码
                bytes = new byte[18];
                System.arraycopy(wordMsgBytes, index, bytes, 0, bytes.length);
                index += bytes.length;
                try {
                    passport = new String(bytes, "UTF_16LE");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                //签发次数
                bytes = new byte[4];
                System.arraycopy(wordMsgBytes, index, bytes, 0, bytes.length);
                index += bytes.length;
                try {
                    issueNumber = new String(bytes, "UTF_16LE");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }

            //照片解码
            if (photoMsgBytesLen > 0) {
                try {
                    PhotoBmp = decode(photoMsgBytes);
                    //System.out.println("解码后的照片为：" + PhotoBmp);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    String getNation(int code){
        switch(code){
            case 01:  return "汉";
            case 02:  return "蒙古";
            case 03:  return "回";
            case 04:  return "藏";
            case 05:  return "维吾尔";
            case 06:  return "苗";
            case 07:  return "彝";
            case 8:   return "壮";
            case 9:   return "布依";
            case 10:  return "朝鲜";
            case 11:  return "满";
            case 12:  return "侗";
            case 13:  return "瑶";
            case 14:  return "白";
            case 15:  return "土家";
            case 16:  return "哈尼";
            case 17:  return "哈萨克";
            case 18:  return "傣";
            case 19:  return "黎";
            case 20:  return "傈僳";
            case 21:  return "佤";
            case 22:  return "畲";
            case 23:  return "高山";
            case 24:  return "拉祜";
            case 25:  return "水";
            case 26:  return "东乡";
            case 27:  return "纳西";
            case 28:  return "景颇";
            case 29:  return "柯尔克孜";
            case 30:  return "土";
            case 31:  return "达斡尔";
            case 32:  return "仫佬";
            case 33:  return "羌";
            case 34:  return "布朗";
            case 35:  return "撒拉";
            case 36:  return "毛南";
            case 37:  return "仡佬";
            case 38:  return "锡伯";
            case 39:  return "阿昌";
            case 40:  return "普米";
            case 41:  return "塔吉克";
            case 42:  return "怒";
            case 43:  return "乌孜别克";
            case 44:  return "俄罗斯";
            case 45:  return "鄂温克";
            case 46:  return "德昂";
            case 47:  return "保安";
            case 48:  return "裕固";
            case 49:  return "京";
            case 50:  return "塔塔尔";
            case 51:  return "独龙";
            case 52:  return "鄂伦春";
            case 53:  return "赫哲";
            case 54:  return "门巴";
            case 55:  return "珞巴";
            case 56:  return "基诺";
            case 97:  return "其他";
            case 98:  return "外国血统中国籍人士";
            default : return "";
        }
    }

    /**
     * 将加密的照片byte数据通过jni解析
     *
     * @param wlt 解密前
     * @return 解密后
     * @throws RemoteException 解密错误
     */
    private Bitmap decode(byte[] wlt) throws RemoteException {
        String pkName = "/data/data/" + mContext.getPackageName() + "/lib/libwlt2bmp.so";
        publicSecurityIDCardLib dw = new publicSecurityIDCardLib();

        byte[] returnBytes = dw.HdosIdUnpack(wlt, pkName);

        //System.out.println("pkName" + pkName + StringTool.byteHexToSting(returnBytes));

        byte[] pBmpFile = new byte[38556];
        int pSex1;

        if (returnBytes != null) {
            byte pName2;
            for(pSex1 = 0; pSex1 < 19278; ++pSex1) {
                pName2 = returnBytes[pSex1];
                returnBytes[pSex1] = returnBytes['際' - pSex1];
                returnBytes['際' - pSex1] = pName2;
            }

            int pNation1;
            for(pSex1 = 0; pSex1 < 126; ++pSex1) {
                for(pNation1 = 0; pNation1 < 153; ++pNation1) {
                    pName2 = returnBytes[pNation1 + pSex1 * 102 * 3];
                    returnBytes[pNation1 + pSex1 * 102 * 3] = returnBytes[305 - pNation1 + pSex1 * 102 * 3];
                    returnBytes[305 - pNation1 + pSex1 * 102 * 3] = pName2;
                }
            }

            System.arraycopy(returnBytes, 0, pBmpFile, 0, 38556);

            int []colors = convertByteToColor(pBmpFile);

            return Bitmap.createBitmap(colors, 102, 126,Bitmap.Config.ARGB_8888);
        }

        return null;

//        Log.i("bmpPath------------",bmpPath);
//
//        try {
//            File wltFile = new File(wltPath);
//            if (!wltFile.exists()) {
//                wltFile.getParentFile().mkdirs();
//                wltFile.createNewFile();
//            }
//            File oldBmpPath = new File(bmpPath);
//            if (!oldBmpPath.exists()) {
//                oldBmpPath.getParentFile().mkdirs();
//                oldBmpPath.createNewFile();
//            }
//            if (oldBmpPath.exists() && oldBmpPath.isFile()) {
//                oldBmpPath.delete();
//            }
//
//            FileOutputStream fos = new FileOutputStream(wltFile);
//            fos.write(wlt);
//            fos.flush();
//            fos.close();
//
//            DecodeWlt dw = new DecodeWlt();
//            dw.Wlt2Bmp(wltPath, bmpPath);
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        return BitmapFactory.decodeFile(bmpPath);
    }

    public final int[] convertByteToColor(byte[] data) {
        int var2;
        if ((var2 = data.length) == 0) {
            return null;
        } else {
            byte var3 = 0;
            if (var2 % 3 != 0) {
                var3 = 1;
            }

            int[] var4 = new int[var2 / 3 + var3];
            int var5;
            if (var3 == 0) {
                for(var5 = 0; var5 < var4.length; ++var5) {
                    var4[var5] = data[var5 * 3] << 16 & 16711680 | data[var5 * 3 + 1] << 8 & '\uff00' | data[var5 * 3 + 2] & 255 | -16777216;
                }
            } else {
                for(var5 = 0; var5 < var4.length - 1; ++var5) {
                    var4[var5] = data[var5 * 3] << 16 & 16711680 | data[var5 * 3 + 1] << 8 & '\uff00' | data[var5 * 3 + 2] & 255 | -16777216;
                }

                var4[var4.length - 1] = -16777216;
            }

            return var4;
        }
    }

    public String toString() {
        if (type == ID_TYPE_GAT) {
            return "\r\n姓        名：" + Name
                    + "\r\n性        别：" + Sex
                    + "\r\n出生日期：" + Born
                    + "\r\n住        址：" + Address
                    + "\r\n身份 证号：" + IDCardNo
                    + "\r\n签发 机关：" + GrantDept
                    + "\r\n有  效  期：" + UserLifeBegin + "-" + UserLifeEnd
                    + "\r\n通行 证号：" + passport
                    + "\r\n签发 次数：" + issueNumber;
        }
        else {
            return "\r\n姓        名：" + Name
                    + "\r\n性        别：" + Sex
                    + "\r\n名        族：" + Nation
                    + "\r\n出生日期：" + Born
                    + "\r\n住        址：" + Address
                    + "\r\n身份 证号：" + IDCardNo
                    + "\r\n签发 机关：" + GrantDept
                    + "\r\n有  效  期：" + UserLifeBegin + "-" + UserLifeEnd;
        }
    }
}
