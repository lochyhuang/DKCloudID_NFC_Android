package com.dk.bleNfc.Exception;

/**
 * Created by Administrator on 2017/5/1.
 */

public class CardNoResponseException  extends Exception {
    public CardNoResponseException(){
    }

    //卡片超时无响应异常
    public CardNoResponseException(String detailMessage){
        super(detailMessage);
    }
}
