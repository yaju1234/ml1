package com.dailystudio.deeplab;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.widget.ImageView;

public class CutActivity extends BaseActivity {

    private ImageView cut_img;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_cut);
        getSupportActionBar().setElevation(0);
        cut_img = (ImageView)findViewById(R.id.cut_img);


        cut_img.setImageBitmap(Constant.croped);
    }
}
