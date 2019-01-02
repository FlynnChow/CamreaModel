package com.example.camreamodel;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

import static android.content.ContentValues.TAG;


public class CameraView extends SurfaceView implements SurfaceHolder.Callback{

    private Camera mCamera;
    private CameraActivity mActivity;
    private Context context;

    public void setInstence(CameraActivity activity){
        mActivity=activity;
    }

    public CameraView(Context context) {
        super(context);
        this.context=context;
        init();
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context=context;
        init();
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context=context;
        init();
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        mCamera=getCamera();
        if(mCamera!=null){
            startAPreview(surfaceHolder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        mCamera.stopPreview();
        startAPreview(surfaceHolder);
        setCameraDisplayOrientation((Activity) context,CameraActivity.REQUEST_CODE,mCamera);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        release();
    }

    private void startAPreview(SurfaceHolder holder){
        try {
            mCamera.setPreviewDisplay(holder);
            Camera.Parameters parameters = mCamera.getParameters();
            Camera.Size size = getBestSize(parameters.getSupportedPictureSizes());
            if(size==null){
                parameters.setPictureSize(size.width,size.height);
            }else{
                parameters.setPictureSize(1920,1080);
            }
            mCamera.setParameters(parameters);
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * .stopPreview();
     * 拍照后用来定格图片
     */
    private void release(){
        if(mCamera != null){
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    private void init(){
        SurfaceHolder holder=getHolder();
        holder.addCallback(this);
        holder.setKeepScreenOn(true);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    private static Camera getCamera(){
        Camera c = null;
        try{
            c =Camera.open();
        }catch (Exception e){
        }
        return c;
    }

    public static Camera.Size getBestSize(List<Camera.Size> sizes) {
        Camera.Size bestSize = null;
        for (Camera.Size size : sizes) {
            if ((float) size.width / (float) size.height == 16.0f / 9.0f) {
                if (bestSize == null) {
                    bestSize = size;
                } else {
                    if (size.width > bestSize.width) {
                        bestSize = size;
                    }
                }
            }
        }
        return bestSize;
    }

    /**
     * 解决拍照时画面颠倒
     * @param activity
     * @param cameraId
     * @param camera
     */
    public static void setCameraDisplayOrientation(Activity activity, int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    /**
     * 打开闪关灯
     */
    public void openLight(){
        Camera.Parameters parameters;
        parameters =mCamera.getParameters();
        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);//开启
        mCamera.setParameters(parameters);
    }

    /**
     * 关闭闪关灯
     */
    public void closeLight(){
        Camera.Parameters parameters=mCamera.getParameters();
        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);//关闭
        mCamera.setParameters(parameters);
    }

    /**
     * 拍照
     * @param pictureCallback
     */
    public void takePhoto(Camera.PictureCallback pictureCallback){
        if(mCamera!=null){
            mCamera.takePicture(null,null,pictureCallback);
        }
    }
    

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getPointerCount()==1){
            boolean isDialog = false;
            if(mActivity.isDialogView(event,R.id.view_1)){
                isDialog=true;
            }
            if(mActivity.isDialogView(event,R.id.view_2)){
                isDialog=true;
            }
            if(mActivity.isDialogView(event,R.id.view_3)){
                isDialog=true;
            }
            if (mActivity.isCropView(event)&&!isDialog){
                if(mActivity.isDialogShow)
                    mActivity.closeDialog();
                else
                    focus(true);
            }
        }
        return super.onTouchEvent(event);
    }

    public void focus(boolean isShowDialog) {
        mActivity.isDialogShow = isShowDialog;
        if (mCamera != null) {
            mActivity.isFocusing = true;
            mActivity.isAutoFocus = false;
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean b, Camera camera) {
                    mActivity.takePhoto(); //触摸对焦后自动拍照
                }
            });
        }
    }
    
    private Handler mHandler = new Handler();

}
