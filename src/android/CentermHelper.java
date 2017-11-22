package com.centerm.cordovahelper;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;


import com.centerm.smartpos.aidl.printer.AidlPrinter;
import com.centerm.smartpos.aidl.sys.AidlDeviceManager;
import com.centerm.smartpos.constant.Constant;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * Created by 29622 on 2017/11/22.
 */

public class CentermHelper extends CordovaPlugin{
    private final static int defaultWidth = 384;
    private final static int defaultHeight = 50;
    private final static int defaultSize = 23;
    private final static boolean defaultUnderline = false;
    private final static boolean defaultBold = false;
    private final static Typeface defaultTypeFace = Typeface.DEFAULT;
    private final static   String SUCCESS = "SUCCESS";

    public Activity activity;
    public AidlDeviceManager manager = null;
    private AidlPrinter printDev = null;

    /**
     * 服务连接桥
     */
    public ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            manager = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            manager = AidlDeviceManager.Stub.asInterface(service);
            if (null != manager) {
                onDeviceConnected(manager);
            }
        }
    };

    public void onDeviceConnected(AidlDeviceManager deviceManager){
        try {
            printDev = AidlPrinter.Stub.asInterface(deviceManager
                    .getDevice(Constant.DEVICE_TYPE.DEVICE_TYPE_PRINTERDEV));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    //Cordova 必须重写iitialize方法
    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        Log.e("initialize","============================");
        this.activity = cordova.getActivity();
        bindService();
    }

    //cordova execute 方法学习
    //cordova execute核心方法
    //public boolean execute(String action, CordovaArgs args, CallbackContext callbackContext)
    //传入参数后如此转化  CordovaArgs cordovaArgs = new CordovaArgs(args);然后调用核心方法
    //public boolean execute(String action, String rawArgs, CallbackContext callbackContext)
    //传入参数后如此转化  JSONArray args = new JSONArray(rawArgs);然后调用核心方法
    //public boolean execute(String action, JSONArray args, CallbackContext callbackContext)
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext){
        //连接服务
        if (action.equals("connect")){
            ConnectService(callbackContext);
            return true;
        }
        //打印
        if(action.equals("print")){
            try {
                String msg = args.getString(0);
                print(msg,callbackContext);
            } catch (JSONException e) {
                e.printStackTrace();
                callbackContext.error("JsonException:"+e.toString());
            }
            return true;
        }
        //获取卡信息
        if(action.equals("getCardInf")){
            getCardInfo(callbackContext);
            return true;
        }
        //Toast "Hello"
        if(action.equals("sayHello")){
            sayHello(callbackContext);
            return true;
        }
        return false;
    }


    public void ConnectService(CallbackContext callbackContext){
           bindService();
           if(callbackContext!=null){
               callbackContext.success(SUCCESS);
           }
    }

    public void setActivity(Activity activity){
        this.activity = activity;
    }

    public void bindService() {
        Intent intent = new Intent();
        intent.setPackage("com.centerm.smartposservice");
        intent.setAction("com.centerm.smartpos.service.MANAGER_SERVICE");
        if(this.activity!=null){
            this.activity.bindService(intent, conn, Context.BIND_AUTO_CREATE);
        }else{
            System.out.println("this.activity is null");
        }

    }

    public void print(String text,CallbackContext callbackContext){
        //执行打开
        if(printDev==null){
           if(callbackContext!=null){
                callbackContext.error("printer is null");
           }
        }
        try {
            printDev.initPrinter();
        } catch (RemoteException e) {
            e.printStackTrace();
            if(callbackContext!=null){
                callbackContext.error("printer init failed");
            }
        }
        //转化微图片进行打印
        try {
            int ret =  printTextAsImage(text);
            if(ret == -1){
                if(callbackContext!=null){
                    callbackContext.error("Print Error:Invalid content to print");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if(callbackContext!=null){
                callbackContext.error("Print Error:"+e.toString());
            }
        }
    }

    //获取卡号
    public void getCardInfo(CallbackContext callbackContext){
        if(callbackContext!=null){
            callbackContext.success("It is developing");
        }
    }
    //天线宝宝说你好
    public void sayHello(CallbackContext callbackContext){
        Toast.makeText(this.activity, "Hello", Toast.LENGTH_SHORT).show();
        if(callbackContext!=null){
            callbackContext.success(SUCCESS);
        }
    }
    //转化为图片进行打印
    public int printTextAsImage(String textToPrint){
        //内容正确性判断
        if(textToPrint==null){
            return -1;
        }
        if(textToPrint.trim().equals("")){
            return -1;
        }
        //分行
        String[] arrayToPrint;
        if(textToPrint.contains("CHANGE_ONE_LINE")){
             arrayToPrint = textToPrint.split("CHANGE_ONE_LINE");
        }else{
            arrayToPrint = new String[1];
            arrayToPrint[0] = textToPrint;
        }

        for(int i=0;i<arrayToPrint.length;i++){
            //然后进行转化为图片
            //然后进行打印
            try {
                Bitmap bitmap = string2Bitmap(arrayToPrint[i],defaultWidth,defaultHeight,defaultSize,defaultUnderline,defaultBold,defaultTypeFace);
                printDev.printBmpFastSync(bitmap,0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return 0x00;
    }
    //文字转图片函数
    public Bitmap string2Bitmap(String string, int width, int height, int size,
                                boolean underline, boolean bold, Typeface typeFace) {
        // 将字符串转换成Bitmap类型
        if (width == 0) {
            width = 384;
        }
        if (height == 0) {
            height = 50;
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height,
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setElegantTextHeight(true);
        paint.setUnderlineText(underline);
        paint.setFakeBoldText(bold);
        paint.setStrokeJoin(Paint.Join.MITER);// 图片拼接圆滑性
        paint.setStrokeWidth((float) 1.0);
        paint.setTextAlign(Paint.Align.LEFT); // 若设置为center，则文本左半部分显示不全
        // paint.setColor(Color.RED);
        paint.setTypeface(typeFace); // 字体
        paint.setAntiAlias(false);// 消除锯齿,使用抗锯齿功能会小号大资源，绘图速度会变慢
        paint.setDither(true);// 设定图像抖动处理，会让绘制出来的图片颜色更加平滑和饱满，图像更加清晰
        if (size == 0) {
            size = 23;
        }
        paint.setTextSize(size);
        Paint.FontMetrics fm = paint.getFontMetrics();
        canvas.drawText(string, 0, height + fm.top - fm.ascent, paint);// x
        // 是说字符串左边在屏幕的位置，y是baseLine在屏幕的位置，baseLine不是bottom
        canvas.save(Canvas.ALL_SAVE_FLAG);
        canvas.restore();

        String path = Environment.getExternalStorageDirectory() + "/abc.png";
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(new File(path));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        return bitmap;
    }

}
