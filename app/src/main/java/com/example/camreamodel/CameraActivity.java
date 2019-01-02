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
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

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

    private View view_1;
    private View view_2;
    private View view_3;
    private int values_view[][]=new int[][]{{0,0},{0,0},{0,0}};

    public boolean isDialogShow = false;

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

        view_1 = (View)findViewById(R.id.view_1);
        view_2 = (View)findViewById(R.id.view_2);
        view_3 = (View)findViewById(R.id.view_3);
    }

    @Override
    public void onClick(View view) {
        closeDialog();
        switch (view.getId()) {
            case R.id.camera_discern:
                cameraView.focus(false);
                break;
            case R.id.back:
                back.setImageResource(R.mipmap.back_w);
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
                                back.setImageResource(R.mipmap.back_g);
                                finish();
                            }
                        });
                    }
                }).start();
                break;
            case R.id.camera_setting:
                setting.setImageResource(R.mipmap.setting_0);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(100);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    setting.setImageResource(R.mipmap.setting_1);
                                }
                            });
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setting.setImageResource(R.mipmap.setting_2);
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
                                    light.setImageResource(R.mipmap.light_close);
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
                    camera_discern.setImageResource(R.mipmap.eye_close);
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    camera_discern.setImageResource(R.mipmap.eye_open);
                }
                break;
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

                            camera.startPreview();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if(isDialogShow){
                                        showDialog(getImagePath(),view_1,10,0);
                                        showDialog(getImagePath(),view_2,10,1);
                                        showDialog(getImagePath(),view_3,10,2);
                                    }

                                }
                            });
                        }
                        try {
                            Thread.sleep(750);
                            isAutoFocus = true;
                            isFocusing = false;
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
        ((ImageView)findViewById(R.id.camera_discern)).setImageResource(R.mipmap.eye_open);
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

    public boolean isDialogView(MotionEvent event,int id){
        Rect frame = new Rect();
        ((View)findViewById(id)).getHitRect(frame);
        float x = event.getX();
        float y = event.getY();
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
                    closeDialog();
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
                                cameraView.focus(true);
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

    private void showDialog(String path,View view,int random,int tag){
        ((ImageView)(view.findViewById(R.id.dialog_image))).setImageBitmap(BitmapFactory.decodeFile(path));
        int r_x=(int)(Math.random()*10)+random;
        int r_y=(int)(Math.random()*15);
        if(((int)(Math.random()*2))>=1)
            r_x=-r_x;
        if(((int)(Math.random()*2))>=1)
            r_y=-r_y;
        r_x-=values_view[tag][0];values_view[tag][0]=r_x;
        r_y-=values_view[tag][1];values_view[tag][1]=r_y;
        view.setTranslationX(r_x);
        view.setTranslationY(r_y);
        view.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    public void closeDialog(){
        if(isDialogShow){
            isDialogShow=false;
            view_1.setVisibility(View.GONE);
            view_2.setVisibility(View.GONE);
            view_3.setVisibility(View.GONE);
        }
    }



}
