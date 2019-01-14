package com.example.tallys.blinder;

public class OpenCvMaker {

    private static boolean canny;
    private static boolean dilate;
    private static boolean erode;
    private static boolean gray;
    private static boolean normal;




    public static void toggleCanny() {
        canny = !canny;
    }

    public static void toggleDilate() {
        dilate = !dilate;
    }

    public static void toggleErode() {
        erode = !erode;
    }

    public static void toggleGray() {
        gray = !gray;
    }

    // normal resets all the other modifier
    public static void setNormal() {
        canny = false;
        dilate = false;
        erode = false;
        gray = false;
        normal = true;
    }

    public static boolean isCanny() {
        return canny;
    }

    public static boolean isDilate() {
        return dilate;
    }

    public static boolean isErode() {
        return erode;
    }

    public static boolean isGray() {
        return gray;
    }

    public static boolean isNormal() {
        return normal;
    }
}
