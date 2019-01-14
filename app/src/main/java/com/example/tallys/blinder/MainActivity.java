package com.example.tallys.blinder;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.googlecode.tesseract.android.TessBaseAPI.PageSegMode.PSM_AUTO;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    static {
        System.loadLibrary("opencv_java3");
    }


    private static String TAG = "MainActivity";

    CameraBridgeViewBase javaCameraView;
    TextView textView;
    final int RequestCameraPermissionID = 1001;
    Mat mRgb;
    Mat mGray;



    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgb = new Mat(height,width,CvType.CV_8UC4);
        mGray = new Mat(height,width,CvType.CV_8UC4);

    }

    @Override
    public void onCameraViewStopped() {
        mRgb.release();
    }




    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgb = inputFrame.rgba();
        mGray = inputFrame.gray();

        return inputFrame.rgba();
    }

    BaseLoaderCallback mBaseLoaderCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case BaseLoaderCallback.SUCCESS:
                    javaCameraView.setMaxFrameSize(1280,1024);

                    javaCameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
            super.onManagerConnected(status);

        }
    };

    private void saveImage(Bitmap finalBitmap) {

        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/saved_images");
        myDir.mkdirs();

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fname = "Shutta_"+ timeStamp +".jpg";

        File file = new File(myDir, fname);
        if (file.exists()) file.delete ();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    Bitmap bmp = null;
    TessOCR tess = null;

    public String detectText(Bitmap bitmap) {


        TessBaseAPI tessBaseAPI = new TessBaseAPI();

        String path = "/mnt/sdcard";

        tessBaseAPI.setDebug(true);
        tessBaseAPI.init(path, "por"); //Init the Tess with the trained data file, with english language

        //For example if we want to only detect numbers
        tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "1234567890");
        tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "!@#$%^&*()_+=-qwertyuiop[]}{POIU" +
                "YTREWQasdASDfghFGHjklJKLl;L:'\"\\|~`xcvXCVbnmBNM,./<>?");


        tessBaseAPI.setImage(bitmap);

        String text = tessBaseAPI.getUTF8Text();

        Log.d(TAG, "Got data: " + text);
        tessBaseAPI.end();

        return text;
    }

    @Override
    protected void onCreate(Bundle savedInstance){
        super.onCreate(savedInstance);
        setContentView(R.layout.activity_main);
        tess = new TessOCR(getApplicationContext());

        //cameraView = (SurfaceView)findViewById(R.id.surface_view);
        javaCameraView = (CameraBridgeViewBase) findViewById(R.id.java_camera_view);
        textView = (TextView)findViewById(R.id.text_view);
        javaCameraView.setMaxFrameSize(1280,1024);
        javaCameraView.setVisibility(View.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);

        javaCameraView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                Log.i("TOUCH::::::::::","CLICADO!!!!!!");

                Mat tmp = new Mat (mGray.height(), mGray.width(), CvType.CV_8U, new Scalar(4));
                try {
                    //Imgproc.cvtColor(seedsImage, tmp, Imgproc.COLOR_RGB2BGRA);
                    Imgproc.cvtColor(mGray, tmp, Imgproc.COLOR_GRAY2RGBA, 4);
                    bmp = Bitmap.createBitmap(tmp.cols(), tmp.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(tmp, bmp);
                }
                catch (CvException e){Log.d("Exception",e.getMessage());}
                //saveImage(bmp);

                textView.setText(tess.getOCRResult(bmp));
                //String txt = tess.getOCRResult(bmp);

                //System.out.println(txt);


                return false;
            }

        });

        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.CAMERA},
                    RequestCameraPermissionID);

        }
    }
    @Override
    protected void onPause(){
        super.onPause();
        if(javaCameraView!=null){
            javaCameraView.disableView();
        }
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        if(javaCameraView!=null){
            javaCameraView.disableView();
        }
    }
    @Override
    protected void onResume(){
        super.onResume();
        if(OpenCVLoader.initDebug()){
            Log.i(TAG,"OpenCV carregado com sucesso.");
            mBaseLoaderCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);

        }else{
            Log.i(TAG,"Erro ao carregar OpenCV.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0,this,mBaseLoaderCallBack);
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case RequestCameraPermissionID: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                }
            }
            break;
        }
    }
}
