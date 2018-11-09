package com.jiangdg.usbcamera.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.support.v4.content.ContextCompat;

import com.seeta.face.DetectParams;
import com.seeta.face.FaceCode;
import com.seeta.face.FaceInfo;
import com.seeta.face.FaceRecgApi;

public class FaceSdkUtil {

    public final static int DETECTFAIL=101; //检测失败
    public final static int NOREFACES=102; //多个人脸
    public final static int TOOBLUR=103; //图片的人脸太模糊
    public final static int INCLINED=104; //人脸不正
    public final static int TOOFAR=105;  //人脸离摄像机太远
    public final static int TOODARK=106;  //图片过于灰暗
    public final static int TOOBRIGHT=107; //图片过于明亮


    /**
     * 初始化
     * @param activity
     * @return
     */
    private static boolean initResult = false;
    public static boolean initSdk(Activity activity){
        if (!initResult){
            initResult = FaceRecgApi.getInstance().initResource(activity);
            if(!initResult){
                //初始化失败
                return false;
            }else{
                //初始化成功
                initParams();
                return true;
            }
        }

        return false;
    }




    public static boolean isInit(){
        return initResult;
    }

    /**
     * 设置检测参数
     */
    public static void initParams(){
        DetectParams params=new DetectParams();
        params.setCheckPersons(true);//true表示支持多个人脸，默认为false
        params.setMinFace(60); //人脸检测大小最小值设置，默认大小为20
        params.setCheck(true);
        params.setCheckBlur(true);
        params.setCheckBright(true);
        params.setCheckFaceQuality(true);
        FaceRecgApi.getInstance().setDetectParams(params);
    }

    /**
     * 外部存储权限
     * @param activity
     * @return
     */
    public static boolean hasStoragePermission(Activity activity){
        return ContextCompat.checkSelfPermission(activity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 人脸检测信息
     * @param context
     * @param bmp
     * @return
     */
    public static void detectSingleFace(Context context, Bitmap bmp,FaceDetectCallback callback){
        if(callback!=null){
            FaceInfo[] faceInfo=FaceRecgApi.getInstance().detectFaces(context,bmp);
            if(faceInfo!=null && faceInfo.length>0){
                if(faceInfo.length>1){
                    callback.onFail(NOREFACES,"多个人脸");
                }else{
                    FaceInfo info=faceInfo[0];
                    if(info.isFace()){
                        callback.onSuccess(faceInfo);
                    }else{
                        FaceCode faceCode = info.getFaceCode();
                        if (faceCode != null) {
                            callback.onFail(faceCode.getId(),faceCode.getDescription());

                        }
                    }
                }
            }else{
                callback.onFail(DETECTFAIL,"人脸检测失败");
            }
        }
    }

    /**
     * 释放
     */
    public static void release(){
        FaceRecgApi.getInstance().release();
    }

    public interface FaceDetectCallback{
        void onSuccess(FaceInfo[] faceInfo);
        void onFail(int error, String msg);
    }
}
