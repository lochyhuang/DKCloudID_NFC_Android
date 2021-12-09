package com.huang.lochy;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

public class MyTTS implements TextToSpeech.OnInitListener {
    private TextToSpeech tts;
    private Context mContext;

    public MyTTS(Context context) {
        mContext = context;

        //初始化tts监听对象
        tts = new TextToSpeech(mContext, this);

        //设置语音播报速度
        tts.setSpeechRate(3);
    }

    @Override
    public void onInit(int status){
        // 判断是否转化成功
        if (status == TextToSpeech.SUCCESS){
            //默认设定语言为中文，原生的android貌似不支持中文。
            int result = tts.setLanguage(Locale.CHINESE);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED){
                //Toast.makeText(MainActivity.this, R.string.notAvailable, Toast.LENGTH_SHORT).show();
            }else{
                //不支持中文就将语言设置为英文
                tts.setLanguage(Locale.US);
            }
        }
    }

    //播放语音
    public void speak(String s) {
        tts.speak(s, TextToSpeech.QUEUE_FLUSH, null);
    }
}
