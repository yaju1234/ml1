package com.dailystudio.deeplab;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.dailystudio.app.utils.ActivityLauncher;
import com.dailystudio.app.utils.ArrayUtils;
import com.dailystudio.app.utils.BitmapUtils;
import com.dailystudio.deeplab.ml.DeeplabInterface;
import com.dailystudio.deeplab.ml.DeeplabModel;
import com.dailystudio.deeplab.ml.ImageUtils;
import com.dailystudio.deeplab.utils.FilePickUtils;
import com.dailystudio.development.Logger;

///
//final Bitmap cropped = cropBitmapWithMask(bitmap, mask);
public class MaskActivity extends BaseActivity {

 private LinearLayout ll_cut_it,ll_back;
 private ImageView src_img,segment_img;
 private Bitmap mask,bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_mask);
        getSupportActionBar().setElevation(0);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.black)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.black));


        }
        setTitle("");

        View decorView = this.getWindow().getDecorView();
        int systemUiVisibilityFlags = decorView.getSystemUiVisibility();
        systemUiVisibilityFlags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        decorView.setSystemUiVisibility(systemUiVisibilityFlags);

        ll_cut_it = (LinearLayout)findViewById(R.id.ll_cut_it);
        ll_back = (LinearLayout)findViewById(R.id.ll_back);
        src_img = (ImageView)findViewById(R.id.src_img);
         segment_img = (ImageView)findViewById(R.id.segment_img);
        ll_cut_it.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), CutActivity.class));
            }
        });
        ll_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        new MyAsynctask().execute();

    }
    class MyAsynctask extends  AsyncTask<Void,Void,Void>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showDialog();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            setupViews();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            src_img.setImageBitmap(bitmap);
            segment_img.setImageBitmap(mask);
            dismissDialog();
        }
    }

    private void setupViews() {
        DeeplabInterface deeplabInterface = DeeplabModel.getInstance();

        final String filePath = FilePickUtils.getPath(getApplicationContext(), Constant.mImageUri);
        Logger.debug("file to mask: %s", filePath);
        if (TextUtils.isEmpty(filePath)) {
            return;
        }



        boolean vertical = checkAndReportDimen(filePath);
        final Resources res =getResources();
        final int dw = res.getDimensionPixelSize(
                vertical ? R.dimen.image_width_v : R.dimen.image_width_h);
        final int dh = res.getDimensionPixelSize(
                vertical ? R.dimen.image_height_v : R.dimen.image_height_h);
        Logger.debug("display image dimen: [%d x %d]", dw, dh);

         bitmap = decodeBitmapFromFile(filePath, dw, dh);
        if (bitmap == null) {
            return ;
        }


      /*  src_img.setImageBitmap(bitmap);*/


        final int w = bitmap.getWidth();
        final int h = bitmap.getHeight();
        Logger.debug("decoded file dimen: [%d x %d]", w, h);



        float resizeRatio = (float) deeplabInterface.getInputSize() / Math.max(bitmap.getWidth(), bitmap.getHeight());
        int rw = Math.round(w * resizeRatio);
        int rh = Math.round(h * resizeRatio);

        Logger.debug("resize bitmap: ratio = %f, [%d x %d] -> [%d x %d]",
                resizeRatio, w, h, rw, rh);

        Bitmap resized = ImageUtils.tfResizeBilinear(bitmap, rw, rh);

        Bitmap maskarr[] = deeplabInterface.segment(resized);









         mask = BitmapUtils.createClippedBitmap(maskarr[0],
                (maskarr[0].getWidth() - rw) / 2,
                (maskarr[0].getHeight() - rh) / 2,
                rw, rh);
        mask = BitmapUtils.scaleBitmap(mask, w, h);



        Bitmap mask1 = BitmapUtils.createClippedBitmap(maskarr[1],
                (maskarr[1].getWidth() - rw) / 2,
                (maskarr[1].getHeight() - rh) / 2,
                rw, rh);
        mask1 = BitmapUtils.scaleBitmap(mask1, w, h);




       /* segment_img.setImageBitmap(mask);*/

        Bitmap cropped = cropBitmapWithMask(bitmap, mask1);
        Constant.croped = cropped;
    }




    private boolean checkAndReportDimen(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return false;
        }

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);

        final int width = options.outWidth;
        final int height = options.outHeight;
        Logger.debug("original image dimen: %d x %d", width, height);


        return (height > width);
    }


    public static Bitmap decodeBitmapFromFile(String filePath,
                                              int reqWidth,
                                              int reqHeight) {
        if (TextUtils.isEmpty(filePath)) {
            return null;
        }

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filePath, options);
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private Bitmap cropBitmapWithMask(Bitmap original, Bitmap mask) {
        if (original == null
                || mask == null) {
            return null;
        }

        final int w = original.getWidth();
        final int h = original.getHeight();
        if (w <= 0 || h <= 0) {
            return null;
        }

        Bitmap cropped = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);


        Canvas canvas = new Canvas(cropped);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        canvas.drawBitmap(original, 0, 0, null);
        canvas.drawBitmap(mask, 0, 0, paint);
        paint.setXfermode(null);

        return cropped;
    }

}
