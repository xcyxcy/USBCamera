package com.jiangdg.usbcamera.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.view.Surface;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Feynman on 2017/1/6.
 */
@SuppressWarnings("deprecation")
public class CameraUtil {
    //camera预览方向
    public final static int CAMERA_ORIENTATION_0 = 0;
    public final static int CAMERA_ORIENTATION_90 = 90;
    public final static int CAMERA_ORIENTATION_180 = 180;
    public final static int CAMERA_ORIENTATION_270 = 270;

    private static final String TAG = CameraUtil.class.getSimpleName();
    private static CameraSizeComparator sizeComparator = new CameraSizeComparator();

    public static int getCameraDisplayOrientation(Context mContext, int cameraId) {
        if (mContext == null) {
            return 0;
        }
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = ((Activity) mContext).getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;

        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = CAMERA_ORIENTATION_0;
                break;
            case Surface.ROTATION_90:
                degrees = CAMERA_ORIENTATION_90;
                break;
            case Surface.ROTATION_180:
                degrees = CAMERA_ORIENTATION_180;
                break;
            case Surface.ROTATION_270:
                degrees = CAMERA_ORIENTATION_270;
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;   // compensate the mirror
        } else {
            // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }


    /**
     * 获得合适的预览尺寸，优先查找指定宽高，当寻找不到指定宽高后，则寻找和指定宽高最接近的一组数据
     *
     * @param list
     * @param propHeight
     * @param propWidth
     * @return
     */
    public static Camera.Size getPropPreviewSize(List<Camera.Size> list, int propHeight, int propWidth) {
        Collections.sort(list, sizeComparator);

        int i = 0;
        for (Camera.Size s : list) {
            if ((s.width == propWidth && s.height == propHeight)) {
                return list.get(i);
            }
            i++;
        }


        return list.get(0);
    }

    private static class CameraSizeComparator implements Comparator<Camera.Size> {
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            // TODO Auto-generated method stub
            if (lhs.width == rhs.width) {
                return 0;
            } else if (lhs.width > rhs.width) {
                return 1;
            } else {
                return -1;
            }
        }
    }

    //camera反复的open>release在个别机型上会引发硬件问题，测试时需要注意
    public static boolean cameraCanUse() {
        boolean isCanUse = true;
        Camera mCamera = null;
        try {
            mCamera = Camera.open();
            Camera.Parameters mParameters = mCamera.getParameters();
            mCamera.setParameters(mParameters);
        } catch (Exception e) {
            isCanUse = false;
        }
        if (mCamera != null) {
            try {
                mCamera.release();
            } catch (Exception e) {
                e.printStackTrace();
                return isCanUse;
            }
        }
        return isCanUse;
    }


    public static ArrayList<HashMap<String, Integer>> getCameraPreviewSize(
            int cameraId) {
        ArrayList<HashMap<String, Integer>> size = new ArrayList<HashMap<String, Integer>>();
        Camera camera = null;
        try {
            camera = Camera.open(cameraId);
            if (camera == null)
                camera = Camera.open(0);

            List<Camera.Size> allSupportedSize = camera.getParameters()
                    .getSupportedPreviewSizes();
            for (Camera.Size tmpSize : allSupportedSize) {
                if (tmpSize.width > tmpSize.height) {
                    HashMap<String, Integer> map = new HashMap<String, Integer>();
                    map.put("width", tmpSize.width);
                    map.put("height", tmpSize.height);
                    size.add(map);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (camera != null) {
                camera.stopPreview();
                camera.setPreviewCallback(null);
                camera.release();
                camera = null;
            }
        }

        return size;
    }



    public static Bitmap convertToBitmap(byte[] data,int Width,int Height){
        YuvImage yuv = new YuvImage(data, ImageFormat.NV21, Width, Height, null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream ();
        yuv.compressToJpeg (new Rect(0, 0, yuv.getWidth(), yuv.getHeight()), 100, baos);
        Bitmap bmp = BitmapFactory.decodeByteArray (baos.toByteArray(), 0, baos.size ());
        return bmp;
    }

}
