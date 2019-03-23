package dev.tae;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
/*
import com.google.firebase.FirebaseApp;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.text.FirebaseVisionCloudTextRecognizerOptions;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.google.firebase.ml.vision.text.RecognizedLanguage;
*/
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;

import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;


public class MainActivity_OPENCV extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, SensorEventListener {
    static {
        System.loadLibrary("opencv_java3");
    }


    ImageView imageView;
    CameraBridgeViewBase surfaceView;
    CameraBridgeViewBase surfaceView2;

    Mat rgb;
    Mat gray;

    Bitmap bmp;

    TextToSpeech tts;
    TextView textView, textView2;
    Vibrator v;

    CameraSource cameraSource;

    //image holder
    Mat bwIMG, hsvIMG, lrrIMG, urrIMG, dsIMG, usIMG, cIMG, hovIMG;
    MatOfPoint2f approxCurve;


    int threshold;

    private void initVar() {
        bwIMG = new Mat();
        dsIMG = new Mat();
        hsvIMG = new Mat();
        lrrIMG = new Mat();
        urrIMG = new Mat();
        usIMG = new Mat();
        cIMG = new Mat();
        hovIMG = new Mat();
        approxCurve = new MatOfPoint2f();

        threshold = 100;


    }


    BaseLoaderCallback mBaseLoaderCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case BaseLoaderCallback.SUCCESS:
                    surfaceView.setMaxFrameSize(1280, 1024);

                    initVar();

                    surfaceView.enableView();

                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
            super.onManagerConnected(status);

        }
    };

    private static double angle(Point pt1, Point pt2, Point pt0) {
        double dx1 = pt1.x - pt0.x;
        double dy1 = pt1.y - pt0.y;
        double dx2 = pt2.x - pt0.x;
        double dy2 = pt2.y - pt0.y;
        return (dx1 * dx2 + dy1 * dy2) / Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2) + 1e-10);
    }

    private void setLabel(Mat im, String label, MatOfPoint contour) {
        int fontface = Core.FONT_HERSHEY_SIMPLEX;
        double scale = 3;//0.4;
        int thickness = 3;//1;
        int[] baseline = new int[1];
        Size text = Imgproc.getTextSize(label, fontface, scale, thickness, baseline);
        Rect r = Imgproc.boundingRect(contour);
        Point pt = new Point(r.x + ((r.width - text.width) / 2), r.y + ((r.height + text.height) / 2));
        Imgproc.putText(im, label, pt, fontface, scale, new Scalar(255, 0, 0), thickness);
    }

    private void findRectangle(Mat src) {
        Mat blurred = src.clone();
        Imgproc.medianBlur(src, blurred, 9);

        Mat gray0 = new Mat(blurred.size(), CvType.CV_8U), gray = new Mat();

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

        List<Mat> blurredChannel = new ArrayList<Mat>();
        blurredChannel.add(blurred);
        List<Mat> gray0Channel = new ArrayList<Mat>();
        gray0Channel.add(gray0);

        MatOfPoint2f approxCurve;

        double maxArea = 0;
        int maxId = -1;

        for (int c = 0; c < 3; c++) {
            int ch[] = {c, 0};
            Core.mixChannels(blurredChannel, gray0Channel, new MatOfInt(ch));

            int thresholdLevel = 1;
            for (int t = 0; t < thresholdLevel; t++) {
                if (t == 0) {
                    Imgproc.Canny(gray0, gray, 10, 20, 3, true); // true ?
                    Imgproc.dilate(gray, gray, new Mat(), new Point(-1, -1), 1); // 1
                    // ?
                } else {
                    Imgproc.adaptiveThreshold(gray0, gray, thresholdLevel,
                            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                            Imgproc.THRESH_BINARY,
                            (src.width() + src.height()) / 200, t);
                }

                Imgproc.findContours(gray, contours, new Mat(),
                        Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

                for (MatOfPoint contour : contours) {
                    MatOfPoint2f temp = new MatOfPoint2f(contour.toArray());

                    double area = Imgproc.contourArea(contour);
                    approxCurve = new MatOfPoint2f();
                    Imgproc.approxPolyDP(temp, approxCurve,
                            Imgproc.arcLength(temp, true) * 0.02, true);

                    if (approxCurve.total() == 4 && area >= maxArea) {
                        double maxCosine = 0;

                        List<Point> curves = approxCurve.toList();
                        for (int j = 2; j < 5; j++) {

                            double cosine = Math.abs(angle(curves.get(j % 4),
                                    curves.get(j - 2), curves.get(j - 1)));
                            maxCosine = Math.max(maxCosine, cosine);
                        }

                        if (maxCosine < 0.3) {
                            maxArea = area;
                            maxId = contours.indexOf(contour);
                        }
                    }
                }
            }
        }

        if (maxId >= 0) {
            Imgproc.drawContours(src, contours, maxId, new Scalar(255, 0, 0,
                    .8), 8);

        }
    }

    Mat rect = new Mat();

    ArrayList<Mat> rects = new ArrayList<Mat>();

    private Mat detectRect(Mat dst) {

        Mat gray = new Mat(), rgb1 = dst.clone();
        Mat rgbGaussianBlur = new Mat(), rgbThres = new Mat();
  //      Imgproc.GaussianBlur(dst,rgbGaussianBlur,new Size(3,3),0);
//        Imgproc.adaptiveThreshold(rgbGaussianBlur,rgbThres,255,Imgproc.ADAPTIVE_THRESH_MEAN_C,Imgproc.THRESH_BINARY,11,3);

        Imgproc.cvtColor(dst, gray, Imgproc.COLOR_RGBA2GRAY);


        Imgproc.pyrDown(gray, dsIMG, new Size(gray.cols() / 2, gray.rows() / 2));

        Imgproc.pyrUp(dsIMG, usIMG, gray.size());

        Imgproc.Canny(usIMG, bwIMG, 0, threshold);

        Imgproc.dilate(bwIMG, bwIMG, new Mat(), new Point(-1, 1), 1);

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

        cIMG = bwIMG.clone();

        Imgproc.findContours(cIMG, contours, hovIMG, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        for (MatOfPoint cnt : contours) {

            MatOfPoint2f curve = new MatOfPoint2f(cnt.toArray());

            Imgproc.approxPolyDP(curve, approxCurve, 0.02 * Imgproc.arcLength(curve, true), true);

            int numberVertices = (int) approxCurve.total();

            double contourArea = Imgproc.contourArea(cnt);

            if (Math.abs(contourArea) < 100) {
                continue;
            }

            //Rectangle detected
            if (numberVertices >= 4 && numberVertices <= 6) {

                List<Double> cos = new ArrayList<>();

                for (int j = 2; j < numberVertices + 1; j++) {
                    cos.add(angle(approxCurve.toArray()[j % numberVertices], approxCurve.toArray()[j - 2], approxCurve.toArray()[j - 1]));
                }

                Collections.sort(cos);

                double mincos = cos.get(0);
                double maxcos = cos.get(cos.size() - 1);

                if (numberVertices == 4 && mincos >= -0.1 && maxcos <= 0.3) {
                    setLabel(dst, "X", cnt);
                    try {
                        Rect rectR = Imgproc.boundingRect(cnt);
                        rectR.x = rectR.x - 10;
                        rectR.y = rectR.y - 10;
                        rectR.height = rectR.height + 50;
                        rectR.width = rectR.width + 50;

                        rect = new Mat(rgb1, rectR);
                        final ByteBuffer buffer = null;
                        //FirebaseVisionImage image = FirebaseVisionImage.fromByteBuffer(buffer, metadata);

                        rects.add(rect);
                        if (rects.size() > 15) rects.clear();


                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Log.i("MAT RECT", String.valueOf(rect));
                    Log.i("MAT RECT STRING", String.valueOf(rect.toString()));
                }

            }


        }

        return rect;
    }

    private Bitmap getBmp(Mat mat) {
        Mat tmp = new Mat(mat.height(), mat.width(), CvType.CV_8U, new Scalar(4));

        try {
            Imgproc.cvtColor(mat, tmp, Imgproc.COLOR_RGBA2RGB, 4);
            bmp = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Utils.matToBitmap(tmp, bmp);

        return bmp;
    }

    Point pt1, pt2;

    private Mat detectLines(Mat src) {
        Mat dst = new Mat(), cdst = new Mat(), rgb = src, cdstP = new Mat();

        // Edge detection
        Imgproc.Canny(src, dst, 50, 250, 3, false);
        // Copy edges to the images that will display the results in BGR
        Imgproc.cvtColor(dst, cdst, Imgproc.COLOR_GRAY2BGR);


        // Standard Hough Line Transform
        Mat lines = new Mat(); // will hold the results of the detection
        Imgproc.HoughLinesP(dst, lines, 1, Math.PI / 180, 150); // runs the actual detection

        // Draw the lines
        if (lines.rows() < 15) {
            for (int x = 0; x < lines.rows(); x++) {
                double rho = lines.get(x, 0)[0],
                        theta = lines.get(x, 0)[1];
                double a = Math.cos(theta), b = Math.sin(theta);
                double x0 = a * rho, y0 = b * rho;
                pt1 = new Point(Math.round(x0 + 1000 * (-b)), Math.round(y0 + 1000 * (a)));
                pt2 = new Point(Math.round(x0 - 1000 * (-b)), Math.round(y0 - 1000 * (a)));
                Imgproc.line(rgb, pt1, pt2, new Scalar(0, 0, 255), 3, Imgproc.LINE_AA, 0);

            }
        }

        return rgb;
    }

    ArrayList<Point> listPoints = new ArrayList<Point>();

    private Pair<Mat, double[]> detectLinesP(CameraBridgeViewBase.CvCameraViewFrame frame) {
        Mat dst = new Mat(), cdst = new Mat(), rgb = frame.rgba(), cdstP = new Mat(), src = frame.gray();

        // Edge detection
        Imgproc.Canny(src, dst, 50, 200, 3, false);
        // Copy edges to the images that will display the results in BGR
        Imgproc.cvtColor(dst, cdst, Imgproc.COLOR_GRAY2BGR);

        // Probabilistic Line Transform
        Mat linesP = new Mat(); // will hold the results of the detection
        Imgproc.HoughLinesP(dst, linesP, 1, Math.PI / 180, 50, 150, 10); // runs the actual detection
        // Draw the lines
        double[] l = new double[3];
        if (linesP.rows() < 10) {

            for (int x = 0; x < linesP.rows(); x++) {
                l = linesP.get(x, 0);
                Log.i("ROWS", String.valueOf(linesP.rows()));
                Point pt1 = new Point(l[0], l[1]);
                Point pt2 = new Point(l[2], l[3]);
                Imgproc.line(rgb, pt1, pt2, new Scalar(0, 0, 255), 3, Imgproc.LINE_AA, 0);
                listPoints.add(pt1);
                listPoints.add(pt2);

                if (listPoints.size() == 6) {
                    listPoints.clear();
                }
            }

        }
        Pair<Mat, double[]> matPair = new Pair<Mat, double[]>(rgb, l);
        return matPair;
    }

    private Mat detectLines3(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat lines = new Mat();
        Imgproc.HoughLinesP(inputFrame.gray(), lines, 1, Math.PI / 180, 50,
                150, 8);
        Mat rgb = inputFrame.rgba();
        for (int x = 0; x < lines.rows(); x++) {
            double[] vec = lines.get(x, 0);
            double x1 = vec[0],
                    y1 = vec[1],
                    x2 = vec[2],
                    y2 = vec[3];
            Point start = new Point(x1, y1);
            Point end = new Point(x2, y2);
            double dx = x1 - x2;
            double dy = y1 - y2;

            double dist = Math.sqrt(dx * dx + dy * dy);

            if (dist > 300.d)  // show those lines that have length greater than 300
                Imgproc.line(rgb, start, end, new Scalar(0, 255, 0, 255), 5);// here initimg is the original image.

        }
        return rgb;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        rgb = new Mat(height, width, CvType.CV_8UC4);
        gray = new Mat(height, width, CvType.CV_8UC4);

    }

    @Override
    public void onCameraViewStopped() {
        rgb.release();
        gray.release();
    }

    float factor = 1.0f;

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (action == KeyEvent.ACTION_DOWN) {
                    factor = factor + 0.2f;
                    Log.i("CIMA", String.valueOf(factor));
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN) {

                    factor = factor - 0.2f;
                    Log.i("BAIXO", String.valueOf(factor));
                }
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }


    private Pair<String, Integer> detection2(Mat rect) {
        int size = 0;

        bmp = getBmp(rect);

        Frame frame = new Frame.Builder().setBitmap(bmp).build();

        SparseArray items = textRecognizer.detect(frame);


        StringBuilder stringBuilder = new StringBuilder();

        String str = null;
        for (int i = 0; i < items.size(); i++) {
            TextBlock item = (TextBlock) items.valueAt(i);
            size = items.size();
            if(letter(item.getValue())>10) str = item.getValue();
            stringBuilder.append(item.getValue());
            stringBuilder.append("\n");
            textView.setText(stringBuilder.toString());
        }
        Log.i("DETECTION2", stringBuilder.toString());
        return new Pair<String, Integer>(str, size);
    }

    Mat resized = new Mat();
    Size size = new Size(10, 10);
    Mat adThres = new Mat();
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        rgb = inputFrame.rgba();
        gray = inputFrame.gray();

        try {

            final Mat gaussianBlur = new Mat(), grayResized = new Mat(), adaptiveThreshold = new Mat();


            rect = detectRect(rgb);

            Imgproc.cvtColor(rect,grayResized,Imgproc.COLOR_RGBA2GRAY);
            Log.i("CONVERTED",grayResized.toString());
            final Mat resized1 = new Mat();
            size.height = rect.height() * 4;
            size.width = rect.width() * 4;
            Imgproc.resize(grayResized, resized1, size);

            Imgproc.GaussianBlur(resized1,gaussianBlur, new Size(3,3), 0);
            //Imgproc.cvtColor(gaussianBlur,grayResized,Imgproc.COLOR_RGBA2GRAY);
            Imgproc.adaptiveThreshold(gaussianBlur,adaptiveThreshold,255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY,11,3);
            runOnUiThread(new Runnable() {
                public void run() {

                    try {
                        Mat mRgba = new Mat();
                         Imgproc.cvtColor(adaptiveThreshold, mRgba,Imgproc.COLOR_GRAY2RGBA);


/*
                        resized = new Mat();
                        size.height = rect.height() * 4;
                        size.width = rect.width() * 4;
                        Imgproc.resize(rect, resized, size);
*/
                        Pair<String, Integer> txt = detection2(mRgba);
                        //Mat imgGaussianBlur = new Mat();
                        //Imgproc.GaussianBlur(gray,imgGaussianBlur,new Size(3, 3),0);
                        Imgproc.resize(mRgba, mRgba, new Size(rect.width(),rect.height()));
                        imageView.setImageBitmap(getBmp(mRgba));
                        //Mat imgAdaptiveThreshold = new Mat();
                        //Imgproc.adaptiveThreshold(imgGaussianBlur, imgAdaptiveThreshold, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C ,Imgproc.THRESH_BINARY, 11, 4);
                        //Pair<String, Integer> txt1 = detection2(adaptiveThreshold);

                        
                        //textView2.setText(txt1.first);


                        String str = txt.first;
                        String lines[] = str.split("\\r?\\n");
                        String str2 = (String) textView.getText();

                        if(letter(lines[0])>letter(str2) && digits(lines[0])<3) textView.setText(lines[0]);
                        else if (letter(lines[0])>10 && lines.length<=2) textView.setText(lines[0]);

                        //adThres = imgAdaptiveThreshold.clone();
                        Log.i("TEXTDETECTOR", str);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            });
            return rgb;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return rgb;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (OpenCVLoader.initDebug()) {
            Log.i("OPENCV", "OpenCV carregado com sucesso.");
            mBaseLoaderCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);

        } else {
            Log.i("OPENCV", "Erro ao carregar OpenCV.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mBaseLoaderCallBack);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (surfaceView != null) {
            surfaceView.disableView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (surfaceView != null) {
            surfaceView.disableView();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1001: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                }
            }
            break;
        }
    }

    TextRecognizer textRecognizer;

    private void detect(Mat rect) {
        bmp = getBmp(rect);
        Frame frame = new Frame.Builder().setBitmap(bmp).build();
        SparseArray items = textRecognizer.detect(frame);
        StringBuilder stringBuilder = new StringBuilder();

        //Log.i("LINE p",String.valueOf("x1-> "+p[0]+" y1-> "+p[1]+" x2-> "+p[2]+" y2-> "+p[3]));
        for (int i = 0; i < items.size(); i++) {
            TextBlock item = (TextBlock) items.valueAt(i);
            //android.graphics.Rect r = item.getBoundingBox();

            android.graphics.Rect r = item.getBoundingBox();


            //double largura = p[0]+p[2];

            //int px0 = (int)listPoints.get(0).x, py0 = (int)listPoints.get(0).y, px1 = (int)listPoints.get(1).x,
            //py1 = (int)listPoints.get(1).y;
            //android.graphics.Rect rLine = new android.graphics.Rect(py0+py1-30,px0+px1-30,py1+px0-30,px1+px0-30);

            //CONTINUAR DAQUI!!!!
            //if(pLine[0]<p[0].x && pLine[2]>p[1].x && pLine[1]<p[0].y && pLine[3]){
            stringBuilder.append(item.getValue());
            stringBuilder.append("\n");

        }

        textView.setText(stringBuilder.toString());
    }

    Util utils;
    Frame frame = null;
    SparseArray items = null;

    private int detection(Mat rect) {
        TextBlock it = null;
        Bitmap bmp = getBmp(rect);
        StringBuilder stringBuilder = null;

        try {
            frame = new Frame.Builder().setBitmap(bmp).build();
            items = textRecognizer.detect(frame);

            stringBuilder = new StringBuilder();
            Log.i("DETsize", String.valueOf(size));

            //Log.i("LINE p",String.valueOf("x1-> "+p[0]+" y1-> "+p[1]+" x2-> "+p[2]+" y2-> "+p[3]));
            for (int i = 0; i < items.size(); i++) {


                TextBlock item = (TextBlock) items.valueAt(i);
                stringBuilder.append(item.getValue());
                stringBuilder.append("\n");
                if (digits(item.getValue()) <= 5) {
                    textView.setText(item.getValue());
                }

                return digits(item.getValue());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //tts.speak(stringBuilder.toString(),TextToSpeech.QUEUE_FLUSH,null);
        //textView.setText(stringBuilder.toString()+"\n"+
        //        "REGIÕES DE TEXTO -----> "+tess.getTextRegions(bmp).size());


        //textView2.setText("TESSERACT\n"+tess.getResults(bmp));

        return 0;
    }


    public int letter(String s) {
        int count = 0;
        for (int i = 0, len = s.length(); i < len; i++) {
            if (Character.isLetter(s.charAt(i))) {
                count++;
            }
        }

        return count;
    }

    public int digits(String s) {
        int count = 0;
        for (int i = 0, len = s.length(); i < len; i++) {
            if (Character.isDigit(s.charAt(i))) {
                count++;
            }
        }

        return count;
    }

    //GOOGLE ML KIT
    /*
    FirebaseVisionTextRecognizer recognizer;
    private void runTextRecognition(Bitmap bmp) throws IOException {
        bmp2 = bmp;
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bmp2);

        recognizer = FirebaseVision.getInstance()
                .getOnDeviceTextRecognizer();
        if(!mov) {
            recognizer.processImage(image)
                    .addOnSuccessListener(
                            new OnSuccessListener<FirebaseVisionText>() {
                                @Override
                                public void onSuccess(FirebaseVisionText texts) {

                                    processTextRecognitionResult(texts);
                                }
                            })
                    .addOnFailureListener(
                            new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // Task failed with an exception

                                    e.printStackTrace();
                                }
                            });

        }else{
            recognizer.close();
        }

    }
    ArrayList<String> dLines = new ArrayList<String>();

    List<FirebaseVisionText.Line> lines;
    String lin = "";
    private void processTextRecognitionResult(FirebaseVisionText texts) {
        List<FirebaseVisionText.TextBlock> blocks = texts.getTextBlocks();
        if (blocks.size() == 0 || mov) {
            return;
        }

        for (int i = 0; i < blocks.size(); i++) {
            lines = blocks.get(i).getLines();
            if(mov) break;
            for (int j = 0; j < lines.size(); j++) {
                if(mov) break;
                String line = lines.get(j).getText();
                Log.i("GOOGLEMLKIT",line);
                Log.i("LINELEN",String.valueOf(line.length()));
                if(letter(line)>3){
                    dLines.add(line);
                    if(dLines.size()>6){
                        dLines.clear();
                        dLines.add("t");
                    }
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(dLines.get(dLines.size()-1));
                    stringBuilder.append("\n");
                    String txt1 = dLines.get(dLines.size()-1);
                    String txt2 = "";
                    try{
                       txt2 = dLines.get(dLines.size()-2);
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                    Log.i("TXT1",txt1);
                    Log.i("TXT2",txt2);
                    if(textView.getText().toString().length()>50) textView.setText("");
                    if(textView.getFreezesText()) textView.setText("");
                    if(txt1.length()>txt2.length() || digits(txt1)>5){
                        //textView.setText(txt1);

                        textView.append(txt1+"\n");

                    }else{
                        textView.append(txt2+"\n");

                    }



                    //textView.setText(stringBuilder.toString());

                    Log.i("dLines",dLines.get(dLines.size()-1));

                }else{
                    break;
                }

                /*
                List<FirebaseVisionText.Element> elements = lines.get(j).getElements();
                for (int k = 0; k < elements.size(); k++) {

                    //textView2.setText(texts.getText());
                }

            }

        }

    }
                */
    private int compare(String str1, String str2) {
        int n = 0;
        try {
            if (str1.length() >= str2.length()) {
                for (int i = 0; i <= str1.length(); i++) {
                    if (str1.charAt(i) == '\0' || str2.charAt(i) == '\0') break;
                    if (!(str1.charAt(i) == str2.charAt(i))) {
                        n++;

                    }
                }
            } else {
                for (int i = 0; i <= str2.length(); i++) {
                    if (str1.charAt(i) == '\0' || str2.charAt(i) == '\0') break;
                    if (!(str2.charAt(i) == str1.charAt(i))) {
                        n++;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return n;
    }


    TessOCR2 tess;

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);


        //dLines.add("te\n");


        setContentView(R.layout.activity_main);
        AssetManager assets = getAssets();
        tess = new TessOCR2(assets);

        //tess = new TessOCR(getApplicationContext());
        textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();

        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    tts.setLanguage(Locale.getDefault());
                }
            }
        });
        surfaceView = findViewById(R.id.java_camera_view);
        //surfaceView2 = (CameraBridgeViewBase) findViewById(R.id.java_camera_view2);
        textView = findViewById(R.id.text_view);
        textView2 = findViewById(R.id.text_view2);
        imageView = findViewById(R.id.imageView);
        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        sensorMan = (SensorManager) getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAccel = 0.00f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;

        sensorMan.registerListener(this, accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL);
        sensorRegistered = true;

        surfaceView.setVisibility(View.VISIBLE);
        surfaceView.setCvCameraViewListener(this);





        /*
        //verificar rotação
        metadata = new FirebaseVisionImageMetadata.Builder()
                .setWidth(480)   // 480x360 is typically sufficient for
                .setHeight(360)  // image recognition
                .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
                .setRotation(FirebaseVisionImageMetadata.ROTATION_180)
                .build();



        FirebaseVisionTextRecognizer detector = null;
        try{
             detector = FirebaseVision.getInstance()
                    .getCloudTextRecognizer();
        }catch(Exception e){
            e.printStackTrace();
        }

        FirebaseVisionImage image;


        try {
            image = FirebaseVisionImage.fromFilePath(getApplicationContext(), "");
        } catch (IOException e) {
            e.printStackTrace();
        }

        final Task<FirebaseVisionText> result =
                detector.processImage(FirebaseVisionImage.fromBitmap(bmp2))
                        .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                            @Override
                            public void onSuccess(FirebaseVisionText firebaseVisionText) {
                                // Task completed successfully
                                // ...


                            }
                        })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        // ...
                                    }
                                });


        FirebaseVisionCloudTextRecognizerOptions options = new FirebaseVisionCloudTextRecognizerOptions.Builder()
                .setLanguageHints(Arrays.asList("pt", "br"))
                .build();

        String resultText = result.getResult().getText();
        for (FirebaseVisionText.TextBlock block: result.getResult().getTextBlocks()) {
            String blockText = block.getText();
            Float blockConfidence = block.getConfidence();
            List<RecognizedLanguage> blockLanguages = block.getRecognizedLanguages();
            android.graphics.Point[] blockCornerPoints = block.getCornerPoints();
            android.graphics.Rect blockFrame = block.getBoundingBox();
            for (FirebaseVisionText.Line line: block.getLines()) {
                String lineText = line.getText();
                Float lineConfidence = line.getConfidence();
                List<RecognizedLanguage> lineLanguages = line.getRecognizedLanguages();
                android.graphics.Point[] lineCornerPoints = line.getCornerPoints();
                android.graphics.Rect lineFrame = line.getBoundingBox();
                for (FirebaseVisionText.Element element: line.getElements()) {
                    String elementText = element.getText();
                    textView2.setText(elementText);
                    Float elementConfidence = element.getConfidence();
                    List<RecognizedLanguage> elementLanguages = element.getRecognizedLanguages();
                    android.graphics.Point[] elementCornerPoints = element.getCornerPoints();
                    android.graphics.Rect elementFrame = element.getBoundingBox();
                }
            }
        */
        //final OpenCvPre pre = new OpenCvPre();
        final ArrayList<String> detections = new ArrayList<String>();
        surfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //textView.setText(detection2(resized));

                try {


                } catch (Exception e) {
                    e.printStackTrace();
                }


                //Imgproc.GaussianBlur(resized2, gaussian, new Size(3,3),0);
                //Imgproc.cvtColor(gaussian,gray,Imgproc.COLOR_RGBA2GRAY);
                //Imgproc.cvtColor(resized2,resized2,Imgproc.COLOR_BGR2GRAY);

                //Imgproc.adaptiveThreshold(gaussian,mat,255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 99,4);
                //bmp = getBmp(rect);
                //textView.setText(p.second);


                //FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bmp);
                //  tts.speak(detection(rect),TextToSpeech.QUEUE_FLUSH,null);

            }
        });

        //surfaceView.setVisibility(View.VISIBLE);
        /*
        cameraSource = new CameraSource.Builder(getApplicationContext(),textRecognizer)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedFps(1.0f)
                .setAutoFocusEnabled(true)
                .setRequestedPreviewSize(1280,1024)
                .build();

        surfaceView2.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {

                    if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                            Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                        ActivityCompat.requestPermissions(MainActivity_OPENCV.this,
                                new String[]{Manifest.permission.CAMERA},
                                1001);
                        return;
                    }
                    cameraSource.start(surfaceView2.getHolder());

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });
*/


        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(MainActivity_OPENCV.this,
                    new String[]{Manifest.permission.CAMERA},
                    1001);

        }


        Scalar CONTOUR_COLOR = new Scalar(255);
        List<KeyPoint> listpoint;
        KeyPoint kpoint;
        Mat mask;
        int rectanx1;
        int rectany1;
        int rectanx2;
        int rectany2;
        int imgsize;
        Scalar zeos = new Scalar(0, 0, 0);
    /*
    private void detectText() {
        MatOfKeyPoint keypoint = new MatOfKeyPoint();

        mask = Mat.zeros(gray.size(), CvType.CV_8UC1);

        imgsize = gray.height() * gray.width();


        List<MatOfPoint> contour2 = new ArrayList<MatOfPoint>();
        Mat kernel = new Mat(1, 50, CvType.CV_8UC1, Scalar.all(255));
        Mat morbyte = new Mat();
        Mat hierarchy = new Mat();

        Rect rectan3;
        //
        FeatureDetector detector = FeatureDetector
                .create(FeatureDetector.MSER);
        detector.detect(gray, keypoint);
        listpoint = keypoint.toList();
        //
        for (int ind = 0; ind < listpoint.size(); ind++) {
            kpoint = listpoint.get(ind);
            rectanx1 = (int) (kpoint.pt.x - 0.5 * kpoint.size);
            rectany1 = (int) (kpoint.pt.y - 0.5 * kpoint.size);
            rectanx2 = (int) (kpoint.size);
            rectany2 = (int) (kpoint.size);
            if (rectanx1 <= 0)
                rectanx1 = 1;
            if (rectany1 <= 0)
                rectany1 = 1;
            if ((rectanx1 + rectanx2) > gray.width())
                rectanx2 = gray.width() - rectanx1;
            if ((rectany1 + rectany2) > gray.height())
                rectany2 = gray.height() - rectany1;
            Rect rectant = new Rect(rectanx1, rectany1, rectanx2, rectany2);
            try {
                Mat roi = new Mat(mask, rectant);
                roi.setTo(CONTOUR_COLOR);
            } catch (Exception ex) {
                Log.d("mylog", "mat roi error " + ex.getMessage());
            }
        }

        Imgproc.morphologyEx(mask, morbyte, Imgproc.MORPH_DILATE, kernel);
        Imgproc.findContours(morbyte, contour2, hierarchy,
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);
        for (int ind = 0; ind < contour2.size(); ind++) {

            rectan3 = Imgproc.boundingRect(contour2.get(ind));

            if (rectan3.area() > 0.5 * imgsize || rectan3.area() < 100
                    || rectan3.width / rectan3.height < 2) {
                Mat roi = new Mat(morbyte, rectan3);
                roi.setTo(zeos);
                Log.i("CONTOUR2", String.valueOf(contour2.size()));

            } else
                Imgproc.rectangle(rgb, rectan3.br(), rectan3.tl(),
                        CONTOUR_COLOR);
                v.vibrate(contour2.size()*15);
        }
    }


*/


    }

    private SensorManager sensorMan;
    private Sensor accelerometer;

    private float[] mGravity;
    private double mAccel;
    private double mAccelCurrent;
    private double mAccelLast;

    private boolean sensorRegistered = false;

    private int hitCount = 0;
    private double hitSum = 0;
    private double hitResult = 0;

    private final int SAMPLE_SIZE = 50; // change this sample size as you want, higher is more precise but slow measure.
    private final double THRESHOLD = 0.2; // change this threshold as you want, higher is more spike movement

    boolean mov;

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mGravity = event.values.clone();
            // Shake detection
            double x = mGravity[0];
            double y = mGravity[1];
            double z = mGravity[2];
            mAccelLast = mAccelCurrent;
            mAccelCurrent = Math.sqrt(x * x + y * y + z * z);
            double delta = mAccelCurrent - mAccelLast;
            mAccel = mAccel * 0.9f + delta;

            if (hitCount <= SAMPLE_SIZE) {
                hitCount++;
                hitSum += Math.abs(mAccel);
            } else {
                hitResult = hitSum / SAMPLE_SIZE;

                Log.d("MOVIMENTO", String.valueOf(hitResult));


                if (hitResult > THRESHOLD) {
                    mov = true;

                    Log.d("MOVIMENTO", "1");

//                    lines.clear();

                } else {
                    Log.d("MOVIMENTO", "0");
                    mov = false;
                }

                hitCount = 0;
                hitSum = 0;
                hitResult = 0;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}



