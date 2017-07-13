package com.studi.timesyncwifi.Utility;


import android.hardware.Camera;
import android.util.Log;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class OpenCVHandler implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static String TAG = "TimeSync:OpenCVHandler";

    public static final int NO_ERROR = 0;
    public static final int OPENCV_NOT_LOADED = 1;
    public static final int SLOW_DEVICE = 2;

    private OpenCVCamera camera;
    private boolean enabled = false;
    private CameraListener listener;

    private long lastFrameT = 0;

    public OpenCVHandler(OpenCVCamera camera, CameraListener listener) {
        this.camera = camera;
        this.listener = listener;

        camera.setCvCameraViewListener(this);

        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "opencv not loaded");
            listener.onFailed(OPENCV_NOT_LOADED);
        }  else {
            Log.i(TAG, "opencv loaded");
            enabled = true;
        }
    }


    Mat rgba;
    Mat fgMask, bgModel, calc;
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        rgba = inputFrame.rgba();
        if (System.currentTimeMillis()-lastFrameT>100) {
            listener.onFailed(SLOW_DEVICE);
        }
        lastFrameT = System.currentTimeMillis();

        boolean trav = OpenCVNative.cameraFrame(rgba.getNativeObjAddr(),
                fgMask.getNativeObjAddr(), bgModel.getNativeObjAddr(), calc.getNativeObjAddr());

        if (trav) {
            listener.onTraversed();
        }
        return fgMask;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        List<Camera.Size> resolutionList;
        resolutionList = camera.getResolutionList();
        Collections.sort(resolutionList, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size lhs, Camera.Size rhs) {
                return lhs.height*lhs.width-rhs.height*rhs.width;
            }
        });
        Log.d(TAG, "resolutions:");
        for (int i=0; i< resolutionList.size(); i++) {
            Log.d(TAG, " "+resolutionList.get(i).width + "x"+ resolutionList.get(i).height);
        }

        ArrayList<Camera.Size> smaller = new ArrayList<>();
        ArrayList<Camera.Size> bigger = new ArrayList<>();
        ArrayList<Camera.Size> perf = new ArrayList<>();
        int a;
        int upper = 640 * 480;
        int lower = 320 * 280;
        for (int i=0; i< resolutionList.size(); i++) {
            a = resolutionList.get(i).width*resolutionList.get(i).height;

            if (a > upper) {
                bigger.add(resolutionList.get(i));
            } else if (a < lower) {
                smaller.add(resolutionList.get(i));
            } else {
                perf.add(resolutionList.get(i));
            }
        }
        Camera.Size resolution = resolutionList.get(resolutionList.size()-1);
        if (perf.size() > 0) {
            resolution = perf.get(0);
        } else if (smaller.size()>0) {
            resolution = smaller.get(smaller.size()-1);
        } else if (bigger.size()>0) {
            resolution = bigger.get(0);
        }

        camera.setResolution(resolution);



        rgba = new Mat(height, width, CvType.CV_8UC4);
        fgMask = new Mat(height, width, CvType.CV_8UC4);
        bgModel = new Mat(height, width, CvType.CV_8UC4);
        calc = new Mat(height, width, CvType.CV_8UC4);

        lastFrameT = System.currentTimeMillis();
    }


    @Override
    public void onCameraViewStopped() {
        rgba.release();
        fgMask.release();
        bgModel.release();
        calc.release();
    }

    public interface CameraListener {
        public void onTraversed();
        public void onFailed(int code);
    }
}
