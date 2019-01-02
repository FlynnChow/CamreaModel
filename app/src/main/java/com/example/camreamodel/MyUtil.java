package com.example.camreamodel;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MyUtil {
    /**
     * 把图片转换为tflite所需的数据格式
     * @param bitmap
     * @param ddims
     * @return
     */
    public static ByteBuffer getScaledMatrix(Bitmap bitmap, int[] ddims) {
        ByteBuffer imgData = ByteBuffer.allocateDirect(ddims[0] * ddims[1] * ddims[2] * ddims[3] * 4);
        imgData.order(ByteOrder.nativeOrder());
        // get image pixel
        int[] pixels = new int[ddims[2] * ddims[3]];
        Bitmap bm = Bitmap.createScaledBitmap(bitmap, ddims[2], ddims[3], false);
        bm.getPixels(pixels, 0, bm.getWidth(), 0, 0, ddims[2], ddims[3]);
        int pixel = 0;
        for (int i = 0; i < ddims[2]; ++i) {
            for (int j = 0; j < ddims[3]; ++j) {
                final int val = pixels[pixel++];
                imgData.putFloat(((((val >> 16) & 0xFF) - 128f) / 128f));
                imgData.putFloat(((((val >> 8) & 0xFF) - 128f) / 128f));
                imgData.putFloat((((val & 0xFF) - 128f) / 128f));
            }
        }

        if (bm.isRecycled()) {
            bm.recycle();
        }
        return imgData;
    }

    /**
     * 打开相册
     * @param activity
     * @param requestCode
     */
    public static void openPhoto(Activity activity, int requestCode){
        Intent intent=new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        activity.startActivityForResult(intent,requestCode);
    }

    /**
     * LRU 图片压缩
     * @param path
     * @param context
     * @return
     */
    public  static Bitmap getScaleBitmap(String path,Context context){
        BitmapFactory.Options options=new BitmapFactory.Options();
        options.inJustDecodeBounds=true;
        BitmapFactory.decodeFile(path,options);
        int width=options.outWidth;
        int height=options.outHeight;
        int mWidth=500;
        int mHeight=500;
        options.inSampleSize=1;
        while(width/options.inSampleSize>mWidth||height/options.inSampleSize>mHeight){
            options.inSampleSize*=2;
        }
        options.inJustDecodeBounds=false;
        return BitmapFactory.decodeFile(path,options);
    }
}
