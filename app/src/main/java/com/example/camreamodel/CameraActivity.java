package com.example.camreamodel;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import static android.content.ContentValues.TAG;

public class CameraActivity extends Activity implements View.OnClickListener, View.OnTouchListener,SensorEventListener {

    public final static int REQUEST_CODE = 0x01;

    private CameraView cameraView;
    private View containerLayout;
    private ImageView cameraCrop;

    private ImageView back;
    private ImageView light;
    private ImageView setting;
    private ImageView camera_discern;

    private int isOpen = 0;
    private String imagePath;

    private SensorManager sensorManager;
    private Sensor sensor;

    public boolean isFocusing = false;
    public boolean isAutoFocus = true;

    public static void startCameraActivity(Context context) {
        ((Activity) context).startActivityForResult(new Intent(context, CameraActivity.class), REQUEST_CODE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_camera);
        init();
    }

    private void init() {

        containerLayout = (LinearLayout) findViewById(R.id.container_layout);
        cameraCrop = (ImageView) findViewById(R.id.camera_crop);
        back = (ImageView) findViewById(R.id.back);
        light = (ImageView) findViewById(R.id.light);
        setting = (ImageView) findViewById(R.id.camera_setting);
        camera_discern = (ImageView) findViewById(R.id.camera_discern);
        cameraView = (CameraView) findViewById(R.id.camera_view);

        back.setOnClickListener(this);
        light.setOnClickListener(this);
        setting.setOnClickListener(this);
        camera_discern.setOnClickListener(this);
        camera_discern.setOnTouchListener(this);

        cameraView.setInstence(this);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.camera_discern:
                takePhoto();
                break;
            case R.id.back:
                back.setImageResource(R.mipmap.back_2);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                back.setImageResource(R.mipmap.back);
                                finish();
                            }
                        });
                    }
                }).start();
                break;
            case R.id.camera_setting:
                setting.setImageResource(R.mipmap.setting_2);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(250);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setting.setImageResource(R.mipmap.setting);
                            }
                        });
                    }
                }).start();
                break;
            case R.id.light:
                if (isOpen == 0) {
                    isOpen = 1;
                    light.setImageResource(R.mipmap.light_open);
                    cameraView.openLight();
                } else {
                    isOpen = 0;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(125);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    light.setImageResource(R.mipmap.lightg_close);
                                    cameraView.closeLight();
                                }
                            });
                        }
                    }).start();
                }
        }
    }


    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        switch (view.getId()) {
            case R.id.camera_discern:
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    camera_discern.setImageResource(R.mipmap.camera_discern_white);
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    camera_discern.setImageResource(R.mipmap.camera_discern);
                }
        }
        return false;
    }


    /**
     * 拍照
     */
    public  void takePhoto() {
        cameraView.takePhoto(new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(final byte[] bytes, final Camera camera) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Bitmap bitmap = null;
                        if (bytes != null) {
                            //读取照片
                            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            camera.stopPreview();
                        }
                        if (bitmap != null) {
                            //bitmap旋转90度
                            Matrix matrix  = new Matrix();
                            matrix.setRotate(90, bitmap.getWidth() / 2, bitmap.getHeight() / 2);
                            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                            //裁剪
                            Bitmap resBitmap = Bitmap.createBitmap(bitmap,
                                    (int) ((float)1/20 * (float) bitmap.getWidth()),
                                    (int) ((float)1/13*(float) (bitmap.getHeight())),
                                    (int) ( (float)19/20*(float) bitmap.getWidth()),
                                    (int) ((float)10/13*(float) (bitmap.getHeight()))
                            );
                            saveBitmap(resBitmap);
                            if(!bitmap.isRecycled()){
                                bitmap.recycle();
                            }

                            Intent intent = new Intent();
                            intent.putExtra("result",getImagePath());
                            setResult(RESULT_OK,intent);
                            finish();
                        }
                        try {
                            Thread.sleep(1000);
                            isAutoFocus = true;
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this,sensor,SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    /**
     * 将图片保存到本地文件夹
     * @param bitmap
     */
    private  void saveBitmap(Bitmap bitmap){
        String path = Environment.getExternalStorageDirectory().getAbsoluteFile()+ File.separator+"Mybitmap";
        File f = new File(path);
        if(!f.exists()){
            f.mkdir();
        }
        imagePath=path+File.separator+"imageCache.jpg";
        File imageCache=new File(imagePath);
        if(imageCache.exists()){
            imageCache.delete();
        }
        try {
            FileOutputStream fout = new FileOutputStream(imagePath);
            BufferedOutputStream  bos= new BufferedOutputStream(fout);
            //.compress 把压缩后的图片放入bos中,第二个为100表示不压缩
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,bos);
            bos.flush();
            bos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public String getImagePath(){
        Log.d(TAG, "getImagePath: 图片"+imagePath);
        return imagePath;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        int[] location = new int[2];
    }

    /**
     * 判断触摸点是否在Image对话框内
     *
     *
     * @param event
     * @return
     */
    public boolean isCropView(MotionEvent event){
        Rect frame = new Rect();
        ((View)findViewById(R.id.camera_top_view)).getHitRect(frame);
        float x = event.getX();
        float y = event.getY();
        if(frame.contains((int)x,(int)y)){
            return false;
        }
        cameraCrop.getHitRect(frame);
        x = event.getX();
        y = event.getY();
        return frame.contains((int)x,(int)y);
    }

    /**
     * 自动对焦
     */
    private int STATUS = 0;
    private int STATUS_NONE = 0;
    private int STATUS_MOVE = 1;
    private int STATUS_STATIC = 2;
    private int last_x,last_y,last_z;
    private final double moveIs = 1.4;
    private long last_time;
    private boolean isCanFosus=false;

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if (sensorEvent.sensor == null||isFocusing||!isAutoFocus)
            return;

        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            int x = (int) sensorEvent.values[0];
            int y = (int) sensorEvent.values[1];
            int z = (int) sensorEvent.values[2];
            long time = System.currentTimeMillis();
            if (STATUS != STATUS_NONE){
                int dx = Math.abs(x-last_x);
                int dy = Math.abs(y-last_y);
                int dz = Math.abs(z-last_z);
                double value = Math.sqrt(dx*dx+dy*dy+dz*dz);
                if (value > moveIs){
                    STATUS = STATUS_MOVE;
                    isCanFosus = false;
                }else{
                    if(STATUS == STATUS_MOVE){
                        if(!isCanFosus){
                            isCanFosus = true;
                            last_time = time;
                        }
                        else{
                            if(time - last_time>500){
                                cameraView.focus();
                                STATUS = STATUS_STATIC;
                            }
                        }
                    }
                }
            }else{
                last_time = time;
                STATUS = STATUS_STATIC;
            }
            last_x = x;
            last_y = y;
            last_z = z;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
