package com.jiangdg.usbcamera.application;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;


import com.jiangdg.usbcamera.R;
import com.jiangdg.usbcamera.utils.CameraUtil;
import com.jiangdg.usbcamera.utils.FaceSdkUtil;
import com.jiangdg.usbcamera.utils.SysProp;
import com.jiangdg.usbcamera.utils.UsbCameraHolder;
import com.seeta.face.FaceInfo;

import java.util.List;


public class CameraDialog extends AlertDialog implements SurfaceHolder.Callback{
    private static final String TAG = "CameraDialog";

    private static final int PREVIEW_STOPPED = 0;
    private static final int IDLE = 1;
    private static final int FOCUSING = 2;
    private static final int SNAPSHOT_IN_PROGRESS = 3;
    private static final int RECORDING_IN_PROGRESS = 4;

    private int mCameraState = PREVIEW_STOPPED;

    private SurfaceView mSurfaceView;
    private Camera mCameraDevice;
    private SurfaceHolder mSurfaceHolder;
    private int mCameraId = 0;

    private int mWidth = 640;
    private int mHeight = 480;

    private GetFaceResult mGetFaceCallback;
    private HandlerThread faceDetectHandlerThread = new HandlerThread("PingAnFace");

    private FaceDetectHandler cameraHandler;
    private class FaceDetectHandler extends Handler{

        public FaceDetectHandler(Looper looper){
            super(looper);
        }

        private boolean isProcssing = false;

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.d(TAG,"handle Message");

            byte[] data = (byte[]) msg.obj;


            if (data != null){

                Log.d(TAG,"detectFace");
                final Bitmap bmp =  CameraUtil.convertToBitmap(data,mWidth,mHeight);
                FaceSdkUtil.detectSingleFace(mContext, bmp, new FaceSdkUtil.FaceDetectCallback() {
                    @Override
                    public void onSuccess(FaceInfo[] faceInfo) {
                        Log.d(TAG,"onSuccess ！faceInfo size: "+faceInfo.length +" face bmp: "+faceInfo[0].getFaceBitmap());

//                        uiHandler.sendEmptyMessage(0);
                        if (mGetFaceCallback != null){
                            mGetFaceCallback.onDetectedSuccess(bmp);
                        }

                        dismiss();

                    }

                    @Override
                    public void onFail(int error, String msg) {
                        Log.d(TAG,"onFail "+msg);
                        isProcssing = false;
                    }
                });
            }
        }
    }


    public interface GetFaceResult{
        void onDetectedSuccess(Bitmap bmp);
    }

    public void setGetFaceCallback(GetFaceResult callback){
        mGetFaceCallback = callback;
    }

    private Context mContext;
    public CameraDialog(Context context) {
        super(context, R.style.Theme_AppCompat_Light_Dialog_Alert);
        mContext = context;
    }




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_camera);

        setCancelable(false);
        setCanceledOnTouchOutside(false);

        if (UsbCameraHolder.instance().getNumberOfCameras() > 0) {

            mSurfaceView = (SurfaceView)findViewById(R.id.cameraSurfaceView);
            mSurfaceHolder = mSurfaceView.getHolder();
            mSurfaceHolder.addCallback(this);
            mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

            Log.d(TAG,"users: "+UsbCameraHolder.instance().getmUsers());
            try {
                mCameraDevice = UsbCameraHolder.instance().open(mCameraId);
            } catch (Exception e) {
                //Do nothing.
            }

            faceDetectHandlerThread.start();
            cameraHandler = new FaceDetectHandler(faceDetectHandlerThread.getLooper());
//            boolean result = FaceSdkUtil.initSdk(getContext());
            Log.d(TAG,"FaceSDK init: "+FaceSdkUtil.isInit());

            setOnDismissListener(null);
        } else {

            Toast.makeText(getContext(),"未检测到摄像头",Toast.LENGTH_LONG).show();
            dismiss();

            return;
        }

    }


    @Override
    public void show() {
        super.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mCameraDevice == null) {
            try {
                mCameraDevice = UsbCameraHolder.instance().open(mCameraId);
                if (mCameraState == PREVIEW_STOPPED) {
                    //Always stop preview regardless of ZSL mode. ZSL mode case is handled in onPictureTaken()
                    //~punits
                    startPreview();
                }
            } catch (Exception e) {
                //Do nothing.
            }
        }
    }

    private void startPreview() {

        mCameraDevice.setErrorCallback(mCameraErrorCb);
        if (SysProp.get("ro.build.product","").equals("e11")) {
            mCameraDevice.setDisplayOrientation(180);
        }
        // If we're previewing already, stop the preview first (this will blank
        // the screen).
        if (mCameraState != PREVIEW_STOPPED) {
            //Always stop preview regardless of ZSL mode. ZSL mode case is handled in onPictureTaken()
            //~punits
            stopPreview();
        }

        setCameraParameters();
        setPreviewDisplay(mSurfaceHolder);

        try {
            mCameraDevice.startPreview();
        } catch (Throwable ex) {
            Log.d(TAG,"throwable: "+ex.getMessage());
            dismiss();
        }
        setCameraState(IDLE);
    }

    private void setPreviewDisplay(SurfaceHolder holder) {
        try {
            mCameraDevice.setPreviewDisplay(holder);
            mCameraDevice.setPreviewCallback(mPreviewCallback);
        } catch (Throwable ex) {
            closeCamera();
            throw new RuntimeException("setPreviewDisplay failed", ex);
        }
    }

    private final android.hardware.Camera.ErrorCallback mCameraErrorCb =
            new android.hardware.Camera.ErrorCallback() {
                @Override
                public void onError(int error, android.hardware.Camera camera) {
                    Log.e(TAG, "Got camera error callback. error=" + error);
                    dismiss();
                }
            };

    private final Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            if (cameraHandler.isProcssing)return;
            cameraHandler.isProcssing = true;
            Message msg = Message.obtain();
            msg.obj = data;
            cameraHandler.handleMessage(msg);
        }
    };

    @Override
    public void setOnDismissListener(DialogInterface.OnDismissListener listener) {


        super.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                Log.d(TAG,"onDismiss users: "+UsbCameraHolder.instance().getmUsers());
                stopCamera();
                Log.d(TAG,"onDismiss users_: "+UsbCameraHolder.instance().getmUsers());
            }
        });

    }

    private void closeCamera() {
        if (mCameraDevice != null) {
            UsbCameraHolder.instance().release();
            mCameraDevice.setErrorCallback(null);
            mCameraDevice = null;
            setCameraState(PREVIEW_STOPPED);
        }
    }

    private void stopCamera() {
//        mHandler.sendEmptyMessage(MESSAGE_UPDATE_STOP);
        stopPreview();
        closeCamera();
    }


    private void stopPreview() {
        if (mCameraDevice != null
                && mCameraState != PREVIEW_STOPPED) {
            mCameraDevice.cancelAutoFocus();
            mCameraDevice.stopPreview();
        }

        setCameraState(PREVIEW_STOPPED);
    }

    private void setCameraState(int state) {
        mCameraState = state;
    }


    private void setCameraParameters() {
        Camera.Parameters mParameters = mCameraDevice.getParameters();

        //Preview, Picture size.
        mParameters.setPreviewSize(mWidth, mHeight);
        mParameters.setPictureSize(mWidth, mHeight);

        //Picture quality.
        if (!SysProp.get("ro.build.product","").equals("e11")) {

            int jpegQuality = 85;
            mParameters.setJpegQuality(jpegQuality);

            //Effect.
            String colorEffect = "none";
            Log.e(TAG, "New effect =" + colorEffect);
            if (isSupported(colorEffect, mParameters.getSupportedColorEffects())) {
                mParameters.setColorEffect(colorEffect);
            }

            //Whitebalance.
            String whiteBalance = "auto";
            Log.e(TAG, "New whiteBalance =" + whiteBalance);
            if (isSupported(whiteBalance, mParameters.getSupportedWhiteBalance())) {
                mParameters.setWhiteBalance(whiteBalance);
            }

            //Antibanding.
            String antibanding ="off";
            Log.e(TAG, "New antibanding =" + antibanding);
            if (isSupported(antibanding, mParameters.getSupportedAntibanding())) {
                mParameters.setAntibanding(antibanding);
            }
        }

        mCameraDevice.setParameters(mParameters);
    }

    private static boolean isSupported(String value, List<String> supported) {
        return supported != null && supported.indexOf(value) >= 0;
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mSurfaceHolder = holder;

        if (mCameraDevice == null) return;
        if (!isShowing()) return;

        if (mCameraState == PREVIEW_STOPPED) {
            startPreview();
        } else {
            if (holder.isCreating()) {
                // Set preview display if the surface is being created and preview
                // was already started. That means preview display was set to null
                // and we need to set it now.
                setPreviewDisplay(holder);
            }
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopPreview();
        mSurfaceHolder = null;
    }
}
