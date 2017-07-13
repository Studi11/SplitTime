package com.studi.timesyncwifi.Utility;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.studi.timesyncwifi.R;


public class BoundFilterService extends Service {

    private final String TAG = "TimeSync:FilterService";
    private View filterView;
    private TextView timeView, serviceId;
    private WindowManager.LayoutParams paramsOn, paramsOff;
    private WindowManager wm;
    private IBinder mIBinder = new LocalBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mIBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        paramsOn = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                PixelFormat.TRANSLUCENT);
        paramsOn.alpha = 1.0F;
        paramsOn.dimAmount = 1.0F;

        paramsOff = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                PixelFormat.TRANSLUCENT);
        paramsOff.alpha = 0.0F;
        paramsOff.dimAmount = 0.0F;

        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        filterView = inflater.inflate(R.layout.filter_layout, null);
        wm.addView(filterView, paramsOff);
        timeView = (TextView) filterView.findViewById(R.id.timeView);
        setTimeEnabled(false);
        serviceId = (TextView) filterView.findViewById(R.id.serviceId);
        setServiceIdEnabled(false);
        //startFilter();
        Log.d(TAG, "service Created");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        wm.removeView(filterView);
    }

    public void startFilter() {
        Log.d(TAG, "startFilter");
        wm.updateViewLayout(filterView, paramsOn);
    }

    public void stopFilter() {
        Log.d(TAG, "stopFilter");
        wm.updateViewLayout(filterView, paramsOff);
    }

    public void setTime(String s) {
        timeView.setText(s);
    }

    public void setTimeEnabled(boolean enabled) {
        if (enabled) {
            timeView.setVisibility(View.VISIBLE);
        } else {
            timeView.setVisibility(View.INVISIBLE);
        }
    }

    public void setServiceId(String s) {
        serviceId.setText(s);
    }

    public void setServiceIdEnabled(boolean enabled) {
        if (enabled) {
            serviceId.setVisibility(View.VISIBLE);
        } else {
            serviceId.setVisibility(View.INVISIBLE);
        }
    }

    public class LocalBinder extends Binder {
        public BoundFilterService getService() {
            return BoundFilterService.this;
        }
    }

}
