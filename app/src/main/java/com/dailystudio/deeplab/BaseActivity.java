package com.dailystudio.deeplab;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {

    private ProgressDialog pdialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pdialog = new ProgressDialog(this);

    }

    public void showDialog(){
        pdialog.setMessage("Loading..");
        pdialog.show();
    }

    public void dismissDialog(){
        if(pdialog!=null && pdialog.isShowing()){
            pdialog.dismiss();
        }
    }
}
