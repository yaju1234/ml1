package com.dailystudio.deeplab.ml;

import android.content.Context;
import android.graphics.Bitmap;

import java.io.IOException;

public interface DeeplabInterface {

    boolean initialize(Context context);

    boolean isInitialized();

    int getInputSize();

    Bitmap segment(Bitmap bitmap);

}


