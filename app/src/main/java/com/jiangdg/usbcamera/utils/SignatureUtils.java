package com.jiangdg.usbcamera.utils;

import java.security.MessageDigest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by apple on 2018/11/1.
 */

public class SignatureUtils {


    public static final String ENCODE_TYPE = "SHA-256";

    public static String sha256(String input){

        String result = "";
        //是否是有效字符串
        if (input != null && input.length()>0){
            try {
                //SHA加密开始
                MessageDigest messageDigest = MessageDigest.getInstance(ENCODE_TYPE);
                messageDigest.update(input.getBytes("UTF-8"));
                byte byteBuffer[] = messageDigest.digest();

                StringBuffer strHexString = new StringBuffer();
                for (int i=0;i<byteBuffer.length;i++){
                    String hex = Integer.toHexString(0xff & byteBuffer[i]);
                    if (hex.length() == 1){
                        strHexString.append('0');
                    }
                    strHexString.append(hex);
                }
                result = strHexString.toString();

            }catch (Exception e){

            }
        }
        return  result;
    }


    public static String sha256HMAC(String input,String appkey){

        String result = "";

        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(appkey.getBytes("UTF-8"),"HmacSHA256");
            sha256_HMAC.init(secret_key);
            result = bytesToHexString(sha256_HMAC.doFinal(input.getBytes("UTF-8")));

        }catch (Exception e){

        }
        return result;

    }

    public static String bytesToHexString(byte[] src){

        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length<=0){
            return null;
        }
        for (int i=0;i<src.length;i++){
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length()<2){
                stringBuilder.append(0);
            }
            stringBuilder.append(v);
        }
        return stringBuilder.toString();
    }

}
