package com.dailystudio.deeplab;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import com.dailystudio.app.activity.ActionBarFragmentActivity;
import com.dailystudio.app.utils.ActivityLauncher;
import com.dailystudio.app.utils.ArrayUtils;
import com.dailystudio.app.utils.BitmapUtils;
import com.dailystudio.deeplab.ml.DeeplabInterface;
import com.dailystudio.deeplab.ml.DeeplabModel;
import com.dailystudio.deeplab.ml.ImageUtils;
import com.dailystudio.deeplab.utils.FilePickUtils;
import com.dailystudio.development.Logger;

import org.greenrobot.eventbus.EventBus;
///
//final Bitmap cropped = cropBitmapWithMask(bitmap, mask);
public class MainActivity extends AppCompatActivity {

    private final static int REQUEST_REQUIRED_PERMISSION = 0x01;
    private final static int REQUEST_PICK_IMAGE = 0x02;
    private Uri mImageUri;
    private ImageView fab_pick_image;

    private class InitializeModelAsyncTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            final boolean ret = DeeplabModel.getInstance().initialize(
                    getApplicationContext());
            Logger.debug("initialize deeplab model: %s", ret);

            return ret;
        }

    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        getSupportActionBar().setElevation(0);
        getSupportActionBar().hide();
        fab_pick_image = (ImageView)findViewById(R.id.fab_pick_image);
        /*fab_pick_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(Constant.croped!=null){
                    startActivity(new Intent(getApplicationContext(), CutActivity.class));
                }

            }
        });*/
        setupViews();
    }

    private void setupViews() {
       if (fab_pick_image != null) {
           fab_pick_image.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    Intent intent;

                    if (Build.VERSION.SDK_INT >= 19) {
                        intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
                        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                    } else {
                        intent = new Intent(Intent.ACTION_GET_CONTENT);
                    }

                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.setType("image/*");

                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    ActivityLauncher.launchActivityForResult(MainActivity.this,
                            Intent.createChooser(intent, getString(R.string.app_name)),
                            REQUEST_PICK_IMAGE);
                }

            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        syncUIWithPermissions(true);
    }

    @Override
    protected void onResume() {
        super.onResume();

        syncUIWithPermissions(false);
    }

    private void syncUIWithPermissions(boolean requestIfNeed) {
        final boolean granted = checkRequiredPermissions(requestIfNeed);


        if (granted && !DeeplabModel.getInstance().isInitialized()) {
            initModel();
        }
    }

    private boolean checkRequiredPermissions() {
        return checkRequiredPermissions(false);
    }

    private boolean checkRequiredPermissions(boolean requestIfNeed) {
        final boolean writeStoragePermGranted =
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_GRANTED;

        Logger.debug("storage permission granted: %s", writeStoragePermGranted);

        if (!writeStoragePermGranted
                && requestIfNeed) {
            requestRequiredPermissions();
        }

        return writeStoragePermGranted;
    }

    private void requestRequiredPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                },
                REQUEST_REQUIRED_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Logger.debug("requestCode = 0x%02x, permission = [%s], grant = [%s]",
                requestCode,
                ArrayUtils.stringArrayToString(permissions, ","),
                ArrayUtils.intArrayToString(grantResults));
        if (requestCode == REQUEST_REQUIRED_PERMISSION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Logger.debug("permission granted, initialize model.");
                initModel();


            } else {
                Logger.debug("permission denied, disable fab.");
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Logger.debug("requestCode = %d, resultCode = %d, data = %s",
                requestCode,
                resultCode,
                data);
        if (requestCode == REQUEST_PICK_IMAGE
                && resultCode == RESULT_OK) {
            Uri pickedImageUri = data.getData();
            Logger.debug("picked: %s", pickedImageUri);

            if (pickedImageUri != null) {
                if(Build.VERSION.SDK_INT >= 19){
                    final int takeFlags = data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION;
                    getContentResolver()
                            .takePersistableUriPermission(pickedImageUri, takeFlags);
                }

                segmentImage(pickedImageUri);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void segmentImage(Uri pickedImageUri) {
        mImageUri = pickedImageUri;
        Constant.mImageUri = mImageUri;
        new LoadAsynctask().execute();




    }

    private void initModel() {
        new InitializeModelAsyncTask().execute((Void)null);
    }


    class LoadAsynctask extends AsyncTask<Void,Void,Void>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            startActivity(new Intent(getApplicationContext(),MaskActivity.class));
        }
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
