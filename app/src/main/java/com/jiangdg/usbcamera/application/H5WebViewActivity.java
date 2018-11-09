package com.jiangdg.usbcamera.application;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.app.VoiceInteractor;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.util.IOUtils;
import com.jiangdg.usbcamera.R;
import com.jiangdg.usbcamera.UVCCameraHelper;
import com.jiangdg.usbcamera.utils.SignatureUtils;
import com.jiangdg.usbcamera.view.PalmCameraActivity;
import com.jiangdg.usbcamera.view.USBCameraActivity;
import com.serenegiant.usb.common.AbstractUVCCameraHandler;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

/**
 * Created by apple on 2018/10/25.
 */


public class H5WebViewActivity extends Activity {

    private static final String TAG = "H5WebViewActivity";
    private WebView mWebView;
    private static final String CHARSET = "utf-8";

    private ProgressDialog progressDialog;
    private EditText editText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);

        mWebView = (WebView) findViewById(R.id.webView);
        initSettings();
        mWebView.setWebChromeClient(new MyChromeClient());
        mWebView.setWebViewClient(new MyWebViewClient());
        mWebView.addJavascriptInterface(new JavaInterface(), "appClient");
        mWebView.loadUrl("file:///android_asset/core/vertical_main.html");
//        mWebView.loadUrl("file:///android_asset/core/book_info.html");


        editText = (EditText) findViewById(R.id.editText);
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                Toast.makeText(getBaseContext(), v.getText().toString(), Toast.LENGTH_SHORT).show();
                mWebView.loadUrl("javascript:getGoodsCode(\"" + editText.getText().toString() + "\");");

                editText.getText().clear();
                return false;
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();

        mWebView.setFocusable(false);
        mWebView.setFocusableInTouchMode(false);
        mWebView.clearFocus();
        //editText.setInputType(InputType.TYPE_NULL);

        editText.setFocusable(true);
        editText.setFocusableInTouchMode(true);
        editText.requestFocus();


    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initSettings() {
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);//支持js脚本
//        webSettings. setPluginsEnabled(true) ;//支持插件
//        webSettings.setUserWideViewPort(false) ;//将图片调整到适合webview的大小
        webSettings.setSupportZoom(true);//支持缩放
//        webSettings. setLayoutAlgorithm(LayoutAlgrithm.SINGLE_COLUMN) ;//支持内容从新布局
        webSettings.supportMultipleWindows();//多窗口
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);//关闭webview中缓存
        webSettings.setAllowFileAccess(true);//设置可以访问文件
        webSettings.setNeedInitialFocus(true);//当webview调用requestFocus时为webview设置节点

//        webSettings. setjavaScriptCanOpenWindowsAutomatically(true) ;//支持通过JS打开新窗口
        webSettings.setLoadsImagesAutomatically(true);//支持自动加载图片
        webSettings.setBuiltInZoomControls(true);//支持缩放
        mWebView.setInitialScale(35);//设置缩放比例
        mWebView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);//设置滚动条隐藏
        mWebView.getSettings().setGeolocationEnabled(true);//启用地理定位
        mWebView.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);//设置渲染优先级

    }

    private class MyChromeClient extends WebChromeClient {

        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            Log.d(TAG, "js console: " + consoleMessage.message());
            return super.onConsoleMessage(consoleMessage);
        }
    }

    private class MyWebViewClient extends WebViewClient {

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            Log.d(TAG, "onPageStarted url:" + url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            Log.d(TAG, "onPageFinished url: " + url);
        }
    }

    private class JavaInterface {

        @JavascriptInterface
        public String callJavaMethod(String info) {

            return "hihi ";
        }

        @JavascriptInterface
        public String userPhotoCapture(String info) {

            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(H5WebViewActivity.this, USBCameraActivity.class);
                    startActivityForResult(intent, 1001);
                }
            }, 1000);
            return "doFaceVerity ";
        }

        @JavascriptInterface
        public String userPalmCapture(HashMap map) {
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {

                    Intent intent = new Intent(H5WebViewActivity.this, PalmCameraActivity.class);
                    startActivityForResult(intent, 1002);
                }
            }, 1000);
            return "doPalmVerity ";

        }

        @JavascriptInterface
        public String confirmPay(String vCode) {

            Log.d(TAG, "confirmPay start = JavaInterface=3333333333");
            String imageCode = "";

            //友宝APPID
//            String uBoxId = "840d14593f7a402a1b624f7d4699d3b1";
            String uBoxId = "1a2b3c";
            String uBoxKey = "2df743fd9e761f6a471f18f42104d937";

            //友宝商户号
//            String merchantId = "900000023433";
//            String merchantKey = "5140761fff134f29b309c0ef020deb64";
            //测试商户号
            String merchantId = "900000012345";
            String merchantKey = "5140761fff134f29b309c0ef020deb64";

            //供应商ID
            String supplierId = "a2b077c66056401f8a3c19af2a59679d";
            String app_uid = "paic_face";
            String order_num = System.currentTimeMillis() + "";

            //调用超市云服务接口
            String postUrl = "https://iem-vmms-dmzstg.pingan.com:10798/iem-vmms/merchantPay/authenticaAndPay";


            String resultStr = "";
            DataOutputStream wr = null;
            BufferedReader in = null;
            try {
//                SSLContext sslContext = SSLContext.getInstance("SSL");
//                TrustManager[] tm = {new MyX509TrustManager()};
//                sslContext.init(null,tm,new SecureRandom());
                HostnameVerifier ignoreHostnameVerifier = new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                };
                HttpsURLConnection.setDefaultHostnameVerifier(ignoreHostnameVerifier);
//                HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

                URL obj = new URL(postUrl);
                HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
                con.setRequestMethod("POST");
                String datasWithSign = "placeholder=" +
                        "&uBoxId=" + uBoxId +
                        "&uBoxKey=" + uBoxKey +
                        "&merchantId=" + merchantId +
                        "&v_code=" + vCode;

                Log.d(TAG, datasWithSign);

                con.setDoOutput(true);
                wr = new DataOutputStream(con.getOutputStream());
                wr.writeUTF(datasWithSign);
                wr.flush();
                in = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
                String inputLine;
                StringBuffer respStr = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    respStr.append(inputLine);
                }


                Log.d(TAG, respStr.toString());
//                Map<String,Object> resultMap = JSONObject.parseObject(respStr.toString());
//                String respCode = (String)resultMap.get("code");
//                String userLevel = (String)resultMap.get("consumptionSchema");
//                String image = (String)resultMap.get("image");
//                String userName = (String)resultMap.get("userName");
//
//                resultStr = respCode+"_"+userName+"_"+userLevel+"_"+image;
//
//                Log.d(TAG,resultStr);
            } catch (Exception e) {


            } finally {
                try {
                    if (null != wr) {
                        wr.close();
                    }
                    if (null != in) {
                        in.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            Log.d(TAG, "confirmPay end JavaInterface=444444444");
            return resultStr;

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //Toast.makeText(getBaseContext(), "识别失败requestCode=" + requestCode + "resultCode" + resultCode, Toast.LENGTH_LONG).show();

        if (resultCode == -1) {
            //1001 调用人脸拍照activity返回
            if (requestCode == 1001) {
                String picPath = "路径";
                try {
                    picPath = data.getStringExtra("cuttentPicPath");

                } catch (Exception e) {
                    Toast.makeText(getBaseContext(), "识别出错=" + e, Toast.LENGTH_LONG).show();
                }
                final String path = picPath;
                if (!picPath.equals("路径")) {
                    //得到正确图片路径 开始调用人脸识别
                    //新线程
                    progressDialog = new ProgressDialog(this);
                    progressDialog.setCanceledOnTouchOutside(false);
                    progressDialog.setMessage("人脸识别中···");
                    progressDialog.show();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Looper.prepare();
                            doFaceVerity(path);
                            Looper.loop();
                        }
                    }).start();

                } else {
                    Toast.makeText(getBaseContext(), "识别失败=" + picPath, Toast.LENGTH_LONG).show();
                }
            } else if (requestCode == 1002) {
                String picPath = "路径";
                String sn = "sn";
                String um = "um";
                String url = "url";
                try {
                    sn = data.getStringExtra("sn");
                    um = data.getStringExtra("um");
                    url = data.getStringExtra("url");
                    picPath = data.getStringExtra("cuttentPicPath");


                } catch (Exception e) {



                }


            }
        } else {
            Toast.makeText(getBaseContext(), "人脸识别失败resultCode=" + resultCode, Toast.LENGTH_LONG).show();
        }
    }

    //开始发送识别请求
    public void doFaceVerity(String picPath) {


        File file11 = new File(picPath);
        if (file11 != null) {
        } else {
            Toast.makeText(H5WebViewActivity.this, "图片有误", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String imageBase64 = fileToBase64URL(picPath);
        HttpURLConnection conn = null;
        String result = "waite";

        try {
            URL url = new URL("http://30.4.166.211/xface-pms/v1/faceSearchall.do?channelId=900014");
            conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(6000);
            conn.setConnectTimeout(6000);
            conn.setDoInput(true);  //允许输入流
            conn.setDoOutput(true); //允许输出流
            conn.setUseCaches(false);  //不允许使用缓存
            conn.setRequestMethod("POST");  //请求方式
            conn.setRequestProperty("Charset", CHARSET);  //设置编码
            conn.setRequestProperty("connection", "keep-alive");

            String timestamp = String.valueOf(System.currentTimeMillis());
            String s = String.valueOf(new Random().nextInt());
            String boundid = "XFACEPMS";
            String content = timestamp + s;
            String hmac_key = "bioauth" + boundid;
            String sign = getSignsture(content, hmac_key);

            conn.setRequestProperty("apiKey", "xfacepms");
            conn.setRequestProperty("apiSecret", "xfacepms");
            conn.setRequestProperty("Timestamp", timestamp);
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            conn.setRequestProperty("Boundid", boundid);
            conn.setRequestProperty("None", s);
            conn.setRequestProperty("Authorization", sign);

            UUID uuid1 = UUID.randomUUID();
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date curDate = new Date(System.currentTimeMillis());
            String timeStr = formatter.format(curDate);

            JSONObject json1 = new JSONObject();

            json1.put("msg_type", "recgall");
            json1.put("device_id", "07139");
            json1.put("msg_id", (uuid1.toString()).toUpperCase().replaceAll("-", ""));
            json1.put("time", timeStr);

            JSONObject json2 = new JSONObject();
            json2.put("track_id", "803");

            JSONObject json3 = new JSONObject();
            json3.put("image_data", imageBase64);
            json3.put("image_index", 0);

            JSONObject json4 = new JSONObject();
            json4.put("image_data", imageBase64);
            json4.put("image_index", 1);

            JSONObject json5 = new JSONObject();
            json5.put("image_data", imageBase64);
            json5.put("image_index", 2);

            JSONArray array = new JSONArray();
            array.put(json3);
            array.put(json4);
            array.put(json5);
            json2.put("face_image", array);
            json1.put("data", json2);
            DataOutputStream out = new DataOutputStream(conn.getOutputStream());

            out.writeBytes(json1.toString());
            out.flush();
            out.close();

            // 读取响应
            if (conn.getResponseCode() == 200) {
                StringBuilder buffer = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String liner;
                while ((liner = reader.readLine()) != null) {
                    buffer.append(liner);
                }
                result = buffer.toString();
                reader.close();
            } else {
                Toast.makeText(H5WebViewActivity.this, "识别失败code=" + conn.getResponseCode(), Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            Toast.makeText(H5WebViewActivity.this, "请求出错" + e, Toast.LENGTH_LONG).show();

        } finally {
            if (conn != null) {
                // 断开连接
                conn.disconnect();
            }
            progressDialog.dismiss();
            if (result.endsWith("waite")) {
                Toast.makeText(H5WebViewActivity.this, "请重新检测", Toast.LENGTH_LONG).show();
            } else {

                //解析人脸识别返回的数据 并按照book_info.html 需求的格式传递过去
                //人脸识别成功 带参数给 book_info.html
                //book_info.html页面所需要的参数
                //"face&uid=e11&idPay=face"
                try {
                    JSONObject json = new JSONObject(result);
                    String err_code = json.getString("err_code");

                    if (err_code.endsWith("0")) {
                        JSONObject data = json.getJSONObject("data");
                        if (data != null) {
                            String face_result = data.getString("face_result");
                            Toast.makeText(H5WebViewActivity.this, "检测结果="+face_result, Toast.LENGTH_LONG).show();
                            String userid;
                            String split = "#";
                            StringTokenizer token = new StringTokenizer(face_result, split);
                            userid = token.nextToken();
                            userid = userid.substring(userid.indexOf("_") + 1);

                            final String identify_id = "face&uid=" + userid + "&idPay=face";

                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mWebView.loadUrl("javascript:getPicCallback(\"" + identify_id + "\");");
                                }
                            }, 500);

                        } else {
                            Toast.makeText(H5WebViewActivity.this, "请重新检测", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(H5WebViewActivity.this, "未检测到人脸或未注册="+result, Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(H5WebViewActivity.this, "解析出错=" + e, Toast.LENGTH_LONG).show();
                }
            }
        }



        //尝试删除本地的图片文件

        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        ContentResolver mContentResolver = getBaseContext().getContentResolver();
        String where = MediaStore.Images.Media.DATA + "='" + picPath + "'";
        //删除图片
        mContentResolver.delete(uri, where, null);

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            new MediaScanner(PreviewActivity.this, path);
//            //new MediaScannerConnection()
//        } else {
//            sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory())));
//        }





    }

    public static String byteArray2String(byte[] bs) {

        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < bs.length; i++) {

            String inTmp = null;
            String text = Integer.toHexString(bs[i]);
            if (text.length() >= 2) {
                inTmp = text.substring(text.length() - 2, text.length());
            } else {

                char[] array = new char[2];
                Arrays.fill(array, 0, 2 - text.length(), '0');
                System.arraycopy(text.toCharArray(), 0, array, 2 - text.length(), text.length());
                inTmp = new String(array);
            }
            sb.append(inTmp);
        }

        return sb.toString().toUpperCase();
    }

    public String fileToBase64URL(String path) {
        InputStream in = null;
        byte[] data = null;
        try {
            in = new FileInputStream(path);
            data = new byte[in.available()];
            in.read(data);

        } catch (IOException e) {

        } finally {
            IOUtils.close(in);
        }
        String str = new String(Base64.encode(data, Base64.NO_WRAP | Base64.URL_SAFE));
        return str;
    }

    public static String getSignsture(String content, String key) {
        String hmacSha1 = null;
        try {
            String message = URLEncoder.encode(content, "UTF-8");
            Mac mac = Mac.getInstance("HmacSHA1");
            SecretKeySpec spec = new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA1");
            mac.init(spec);
            byte[] byteHMAC = mac.doFinal(message.getBytes("UTF-8"));
            hmacSha1 = byteArray2String(byteHMAC);

        } catch (Exception e) {

        }
        return hmacSha1;
    }

    /*@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        Toast.makeText(getBaseContext(), "文本=" + editText.getText(), Toast.LENGTH_LONG).show();
        Log.d(" 扫码枪 键盘事件", "文本=" + editText.getText());
        mWebView.loadUrl("javascript:getGoodsCode(\"" + editText.getText().toString() + "\");");

        editText.getText().clear();
        return super.onKeyDown(keyCode, event);
    }*/


}



