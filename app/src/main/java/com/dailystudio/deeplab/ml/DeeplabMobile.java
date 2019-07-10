package com.dailystudio.deeplab.ml;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.TextUtils;

import com.dailystudio.app.utils.ArrayUtils;
import com.dailystudio.development.Logger;

import org.tensorflow.Graph;
import org.tensorflow.Operation;
import org.tensorflow.Output;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

public class DeeplabMobile implements DeeplabInterface {

    private final static String MODEL_FILE = "file:///android_asset/frozen_inference_graph.pb";

    private final static String INPUT_NAME = "ImageTensor";
    private final static String OUTPUT_NAME = "SemanticPredictions";

    public final static int INPUT_SIZE = 513;

    private volatile TensorFlowInferenceInterface sTFInterface = null;

    @Override
    public boolean initialize(Context context) {

        sTFInterface = new TensorFlowInferenceInterface(context.getAssets(),"file:///android_asset/frozen_inference_graph.pb");
        //tensorflow.initializeTensorFlow(getAssets(), "file:///android_asset/model.pb");

        return true;
    }

    @Override
    public boolean isInitialized() {
        return (sTFInterface != null);
    }


    @Override
    public int getInputSize() {
        return INPUT_SIZE;
    }

    @Override
    public Bitmap[] segment(final Bitmap bitmap) {
        if (sTFInterface == null) {
            Logger.warn("tf model is NOT initialized.");
            return null;
        }

        if (bitmap == null) {
            return null;
        }

        final int w = bitmap.getWidth();
        final int h = bitmap.getHeight();
        Logger.debug("bitmap: %d x %d,", w, h);

        if (w > INPUT_SIZE || h > INPUT_SIZE) {
            Logger.warn("invalid bitmap size: %d x %d [should be: %d x %d]",
                    w, h,
                    INPUT_SIZE, INPUT_SIZE);

            return null;
        }

        int[] mIntValues = new int[w * h];
        byte[] mFlatIntValues = new byte[w * h * 3];
        int[] mOutputs = new int[w * h];

        bitmap.getPixels(mIntValues, 0, w, 0, 0, w, h);
        for (int i = 0; i < mIntValues.length; ++i) {
            final int val = mIntValues[i];
            mFlatIntValues[i * 3 + 0] = (byte)((val >> 16) & 0xFF);
            mFlatIntValues[i * 3 + 1] = (byte)((val >> 8) & 0xFF);
            mFlatIntValues[i * 3 + 2] = (byte)(val & 0xFF);
        }

        final long start = System.currentTimeMillis();
        sTFInterface.feed(INPUT_NAME, mFlatIntValues, 1, h, w, 3 );

        sTFInterface.run(new String[] { OUTPUT_NAME }, true);

        sTFInterface.fetch(OUTPUT_NAME, mOutputs);
        final long end = System.currentTimeMillis();
        Logger.debug("%d millis per core segment call.", (end - start));

//        Logger.debug("outputs = %s", ArrayUtils.intArrayToString(mOutputs));

        Bitmap output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Bitmap output1 = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);


        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
               // output.setPixel(x, y, mOutputs[y * w + x] == 0 ? Color.TRANSPARENT : Color.argb(100,255,108,157));
                output.setPixel(x, y, mOutputs[y * w + x] == 0 ? Color.TRANSPARENT : Color.argb(100,255,108,157));
                output1.setPixel(x, y, mOutputs[y * w + x] == 0 ? Color.TRANSPARENT : Color.BLACK);

            }
        }
       Bitmap b[] =  new Bitmap[2];
        b[0] = output;
        b[1] = output1;
        return b;
    }

    public static void printOp(Graph graph, String opName) {
        if (graph == null
                || TextUtils.isEmpty(opName)) {
            return;
        }

        Operation op = graph.operation(opName);

        Logger.debug("op[%s]: %s",
                opName, op);
    }

    public static void printGraph(Graph graph) {
        if (graph == null) {
            return;
        }

        Iterator<Operation> operations = graph.operations();
        if (operations == null) {
            return;
        }

        Operation op;
        Output<?> output;
        int num;
        while (operations.hasNext()) {
            op = operations.next();

            Logger.debug("op: [%s]", op);
            num = op.numOutputs();

            for (int i = 0; i < num; i++) {
                output = op.output(i);

                Logger.debug("%s- [%d]: %s",
                        (i == num - 1) ? "`" : "|",
                        i, output);
            }
        }
    }
}
