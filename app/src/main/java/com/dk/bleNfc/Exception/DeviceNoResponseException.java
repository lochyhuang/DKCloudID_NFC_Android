package com.dk.bleNfc.Exception;

/**
 * Created by Administrator on 2017/5/15.
 */

public class DeviceNoResponseException extends Exception {
    public DeviceNoResponseException(){
    }

    //设备超时无响应异常
    public DeviceNoResponseException(String detailMessage){
        super(detailMessage);
    }
}
