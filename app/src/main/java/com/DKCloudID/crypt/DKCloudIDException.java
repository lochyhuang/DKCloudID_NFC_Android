package com.DKCloudID.crypt;

/**
 * Created by Administrator on 2017/5/1.
 */

public class DKCloudIDException extends Exception {
    public DKCloudIDException(){
    }

    public DKCloudIDException(String detailMessage){
        super(detailMessage);
    }
}
