package com.example.tallys.blinder;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;

import static com.googlecode.tesseract.android.TessBaseAPI.PageSegMode.PSM_AUTO;

public class TessOCR {
    private static TessBaseAPI mTess = new TessBaseAPI();
    Context context;
    private String datapath;

    public TessOCR(Context context) {
        this.context = context;
        //datapath = context.getExternalFilesDir("/").getPath() + "/";
        datapath = "/mnt/sdcard";
        mTess.setDebug(true);

        mTess.init(datapath, "por");
        mTess.setPageSegMode(PSM_AUTO);
    }

    public void stopRecognition() {
        mTess.stop();
    }

    public String getOCRResult(Bitmap bitmap) {

        String whitelist = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-,.?!:'\"áéíóúç";
        mTess.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, whitelist);
        mTess.setImage(bitmap);
        String result = mTess.getUTF8Text();
        return result;
    }

    public void onDestroy() {
        if (mTess != null)
            mTess.end();
    }

}
