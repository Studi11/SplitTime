package com.studi.timesyncwifi.Utility;


public class OpenCVNative {

    static {
        System.loadLibrary("jnilib");
    }

    public static native String function();
    public static native boolean cameraFrame(long rgbInAddr, long fgMaskAddr, long bgModelAddr, long calcAddr);

}
