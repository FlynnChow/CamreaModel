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
import android.widget.TextView;

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
    public  boolean AutoFocusing =false;
    private View view_1;
    private View view_2;
    private View view_3;
    private int values_x[]=new int[]{0,0,0},values_y=0;
    private TFLiteUtil tflite;

    public boolean isDialogShow = false;
    ImageView dialog_image[]=new ImageView[3];
    TextView dialog_text[][]=new TextView[3][3];

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
        view_1 = (View)findViewById(R.id.view_1);
        view_2 = (View)findViewById(R.id.view_2);
        view_3 = (View)findViewById(R.id.view_3);
        initDialog(view_1,0,true);
        initDialog(view_2,1,true);
        initDialog(view_3,2,false);

        back.setOnClickListener(this);
        light.setOnClickListener(this);
        setting.setOnClickListener(this);
        camera_discern.setOnClickListener(this);
        cameraView.setInstence(this);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        tflite=new TFLiteUtil(getApplicationContext());
        tflite.init();
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
                            Thread.sleep(50);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    setting.setImageResource(R.mipmap.setting_1);
                                }
                            });
                            Thread.sleep(50);
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
                            //camera.stopPreview();
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
                                        float temp[][]=tflite.predict_image(getImagePath());
                                        if(AutoFocusing){
                                            AutoFocusing=false;
                                            if (temp[1][1]<=0.03){
                                                isDialogShow=false;
                                                return;
                                            }
                                        }
                                        showDialog(getImagePath(),(int)temp[0][1],temp[1][1],0);
                                        showDialog(getImagePath(),(int)temp[0][2],temp[1][2],1);
                                        showDialog(getImagePath(),(int)temp[0][3],temp[1][3],2);
                                        TransLation();
                                    }

                                }
                            });
                        }
                        try {
                            Thread.sleep(400);
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
                            if(time - last_time>1200){
                                AutoFocusing=true;
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

    private void showDialog(String path,int id,float maybe,int flag){
        dialog_image[flag].setImageBitmap(BitmapFactory.decodeFile(path));
        dialog_text[flag][0].setText("名称："+tflite.mylabel[id]);
        dialog_text[flag][1].setText("概率："+String.format("%.2f",(maybe*100))+"%");
    }

    private void TransLation(){
        int right=0;
        if(Math.random()>=0.5)
            right=100;
        int a=-((int)(Math.random()*50)+150)+right;
        int b=-((int)(Math.random()*25)+25)+right;
        int c=(int)(Math.random()*50)+50+right;

        view_1.setTranslationX(a);
        view_2.setTranslationX(b);
        view_3.setTranslationX(c);

        int y=(int)(Math.random()*125)+5;
        if(Math.random()>=0.5){
            y=-y-100;
        }

        view_1.setTranslationY(y);
        view_2.setTranslationY(y);
        view_3.setTranslationY(y);
        view_1.setVisibility(View.VISIBLE);
        view_2.setVisibility(View.VISIBLE);
        view_3.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    public void closeDialog(){
        if(view_1.getVisibility()==View.VISIBLE||
                view_2.getVisibility()==View.VISIBLE||
                view_3.getVisibility()==View.VISIBLE){
            isDialogShow=false;
            view_1.setVisibility(View.GONE);
            view_2.setVisibility(View.GONE);
            view_3.setVisibility(View.GONE);
        }
    }

    private void initDialog(View view,int flag,boolean isT){
        if(isT){
            dialog_image[flag]=(ImageView)view.findViewById(R.id.oval_image);
            dialog_text[flag][0]=(TextView) view.findViewById(R.id.oval_1);
            dialog_text[flag][1]=(TextView) view.findViewById(R.id.oval_2);
            dialog_text[flag][2]=(TextView) view.findViewById(R.id.oval_3);
            return;
        }
        dialog_image[flag]=(ImageView)view.findViewById(R.id.dialog_image);
        dialog_text[flag][0]=(TextView) view.findViewById(R.id.label_1);
        dialog_text[flag][1]=(TextView) view.findViewById(R.id.label_2);
        dialog_text[flag][2]=(TextView) view.findViewById(R.id.label_3);
    }
}