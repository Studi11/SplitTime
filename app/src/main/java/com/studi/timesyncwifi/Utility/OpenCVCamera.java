package com.studi.timesyncwifi.Utility;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;

import org.opencv.android.JavaCameraView;

import java.util.List;

public class OpenCVCamera extends JavaCameraView {
    public OpenCVCamera(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public OpenCVCamera(Context context, int cameraId) {
        super(context, cameraId);
    }

    public List<Camera.Size> getResolutionList() {
        return mCamera.getParameters().getSupportedPreviewSizes();
    }

    public void setResolution(Camera.Size resolution) {
        disconnectCamera();
        mMaxHeight = resolution.height;
        mMaxWidth = resolution.width;
        connectCamera(getWidth(), getHeight());
    }



}
