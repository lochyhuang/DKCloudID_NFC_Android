package com.DKCloudID.crypt;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 获取网络图片工具类
 * url:图片url，可网络地址，可服务器路径
 */
public class GetNetPicture {
    //加载图片
    public static Bitmap getURLimage(String url) throws Exception{
        Bitmap bmp = null;
//        try {
            URL myurl = new URL(url);
            // 获得连接
            HttpURLConnection conn = (HttpURLConnection) myurl.openConnection();
            conn.setConnectTimeout(6000);//设置超时
            conn.setDoInput(true);
            conn.setUseCaches(false);//不缓存
            conn.connect();
            InputStream is = conn.getInputStream();//获得图片的数据流
            bmp = BitmapFactory.decodeStream(is);//读取图像数据
            //读取文本数据
            //byte[] buffer = new byte[100];
            //inputStream.read(buffer);
            //text = new String(buffer);
            is.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        return bmp;
    }
}