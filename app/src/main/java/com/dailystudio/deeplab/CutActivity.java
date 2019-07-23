package com.dailystudio.deeplab;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.dailystudio.deeplab.view.TryonView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class CutActivity extends BaseActivity {

    private RelativeLayout cut_img;
    double mDensity;

    int viewWidth;
    int viewheight;
    int bmWidth;
    int bmHeight;

    int actionBarHeight;
    int bottombarHeight;
    double bmRatio;
    double viewRatio;

    //	ImageView bodyImageView;
    TryonView mTryOnView;
    private Bitmap headBitmap;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_cut);
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
        cut_img = (RelativeLayout)findViewById(R.id.cut_img);


        //cut_img.setImageBitmap(Constant.croped);

        mDensity = getResources().getDisplayMetrics().density;
        actionBarHeight = (int)(110*mDensity);
        bottombarHeight = (int)(60*mDensity);
        viewWidth = getResources().getDisplayMetrics().widthPixels;
        viewheight = getResources().getDisplayMetrics().heightPixels - actionBarHeight - bottombarHeight;
        headBitmap =Constant.croped;
        mTryOnView = new TryonView(this, headBitmap, viewWidth, viewheight);
        mTryOnView.setLayoutParams(new ViewGroup.LayoutParams(viewWidth, viewheight));
        cut_img.addView(mTryOnView);



    }

}
