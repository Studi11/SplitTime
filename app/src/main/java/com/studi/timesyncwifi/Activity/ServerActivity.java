package com.studi.timesyncwifi.Activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.studi.timesyncwifi.R;
import com.studi.timesyncwifi.Utility.BoundFilterService;
import com.studi.timesyncwifi.Utility.LogFile;
import com.studi.timesyncwifi.Utility.OpenCVCamera;
import com.studi.timesyncwifi.Utility.OpenCVHandler;
import com.studi.timesyncwifi.Utility.Stopwatch;
import com.studi.timesyncwifi.Utility.SystemUtilities;
import com.studi.timesyncwifi.Utility.TimeSpan;
import com.studi.timesyncwifi.Wifi.Client;
import com.studi.timesyncwifi.Wifi.NSDServer;
import com.studi.timesyncwifi.Wifi.Server;

import org.opencv.android.OpenCVLoader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;


public class ServerActivity extends AppCompatActivity {

    private static final String TAG = "TimeSync:ServerA";
    private NSDServer nsdServer = null;
    private Server timeSyncServer = null;
    private Client timeSyncClient = null;


    private TextView secView;
    private TextView msecView;

    private Stopwatch watch;
    private TimeSpan timeSpan;
    Handler timeOutHandler;

    private boolean newActivity = true;
    private Handler handler = new Handler(Looper.getMainLooper());


    private RelativeLayout rootView;
    private TextView serviceId;
    private int serviceNr;

    private int traverses = 0;


    static {
        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "opencv not loaded");
        }  else {
            Log.i(TAG, "opencv loaded");
        }
    }

    private OpenCVHandler openCVHandler;
    private OpenCVCamera javaCameraView;

    private BoundFilterService mService;


    private LogFile logFile;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        Log.d(TAG, "server");

        if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("dev", false)) {
            setContentView(R.layout.server_low_layout);
            javaCameraView = (OpenCVCamera) findViewById(R.id.cameraView);
            javaCameraView.setVisibility(SurfaceView.VISIBLE);
            javaCameraView.enableView();

            timeSpan = new TimeSpan();
            timeOutHandler = new Handler();

            RelativeLayout wholeView = (RelativeLayout) findViewById(R.id.wholeView);
            wholeView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mService!=null) {
                        mService.setServiceId(serviceNr+"");
                        mService.setServiceIdEnabled(true);
                        Handler h = new Handler();
                        h.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mService.setServiceIdEnabled(false);
                            }
                        },3000);
                    }
                }
            });


            Intent i = new Intent(this, BoundFilterService.class);
            bindService(i, mConnection, Context.BIND_AUTO_CREATE);

            timeSyncServer = new Server(this, this, serverListener, Looper.getMainLooper());
            timeSyncServer.start();

            openCVHandler = new OpenCVHandler(javaCameraView, new OpenCVHandler.CameraListener() {
                @Override
                public void onTraversed() {
                    Log.i(TAG, "traversing " + System.currentTimeMillis());

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (timeSpan.isRunning()) {
                                updateFilterTimer(timeSpan.getTime());
                                traverses++;
                                Log.d(TAG, "stopping");
                            }
                        }
                    });

                }

                @Override
                public void onFailed(int code) {
                    switch (code) {
                        case OpenCVHandler.SLOW_DEVICE: {
                            Log.w(TAG, "slow device, low fps and low resolution");
                            break;
                        }
                        default: {
                            Log.w(TAG, "other error");
                        }
                    }
                }
            });

        } else {
            setContentView(R.layout.server_layout);
            rootView = (RelativeLayout) findViewById(R.id.rootView);
            secView = (TextView) findViewById(R.id.Seconds);
            msecView = (TextView) findViewById(R.id.hSeconds);
            timeSyncServer = new Server(this, this, serverListener, Looper.getMainLooper());
            timeSyncServer.start();
            Button stopButton = (Button) findViewById(R.id.stopButton);

            serviceId = (TextView) findViewById(R.id.serverID);
            stopButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    watch.stop();
                }
            });
            watch = new Stopwatch(10, new Stopwatch.Listener() {
                @Override
                public void onUpdate(long dt) {
                    updateTimer(dt);
                }
            });


            javaCameraView = (OpenCVCamera) findViewById(R.id.cameraView);
            javaCameraView.setVisibility(SurfaceView.VISIBLE);
            javaCameraView.enableView();
            openCVHandler = new OpenCVHandler(javaCameraView, new OpenCVHandler.CameraListener() {
                @Override
                public void onTraversed() {
                    Log.i(TAG, "traversing " + System.currentTimeMillis());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            watch.stop();
                            traverses++;
                        }
                    });
                }

                @Override
                public void onFailed(int code) {
                    switch (code) {
                        case OpenCVHandler.SLOW_DEVICE: {
                            Log.w(TAG, "slow device, low fps and low resolution");
                            break;
                        }
                        default: {
                            Log.w(TAG, "other error");
                        }
                    }
                }
            });
        }

    }

    public void updateTimer(long dT) {
        long mins = dT / 1000 / 60;
        long secs = (dT - mins * 1000 * 60) / 1000;
        long msecs = (dT - mins * 1000 * 60 - secs * 1000);
        String seconds = String.format(Locale.US, "%02d", secs);
        String milliseconds = String.format(Locale.US, "%02d", (int) Math.floor(msecs / 10));
        secView.setText(seconds);
        msecView.setText(milliseconds);
    }

    private void updateFilterTimer(long dT) {
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("use_split_time_limits", false)) {
            if (dT < 1000 * Float.parseFloat(PreferenceManager.getDefaultSharedPreferences(this).getString("min_split", "0"))) {
                return;
            }
            if (dT > 1000 * Float.parseFloat(PreferenceManager.getDefaultSharedPreferences(this).getString("max_split", "10"))) {
                timeSpan.stop();
                return;
            }
        }
        timeSpan.stop();
        long mins = dT / 1000 / 60;
        long secs = (dT - mins * 1000 * 60) / 1000;
        long msecs = (dT - mins * 1000 * 60 - secs * 1000);
        String seconds = String.format(Locale.US, "%02d", secs);
        String hundrethsseconds = String.format(Locale.US, "%02d", (int) Math.floor(msecs / 10));
        String t = seconds+":"+hundrethsseconds;
        if (mService!=null) {
            mService.setTime(t);
            mService.setTimeEnabled(true);
            timeOutHandler.removeCallbacksAndMessages(null);
            timeOutHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mService!=null);
                    mService.setTimeEnabled(false);
                }
            }, 1000*Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("split_timeout", "10")));
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (mService!=null) {
            mService.stopFilter();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*
        if (!newActivity) {
            Intent intent = new Intent(this, TimeSyncActivity.class);
            startActivity(intent);
            finish();
        }
        */

        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("log", false)) {
            logFile = new LogFile();
            logFile.log("Server started, "+ SystemUtilities.getBatteryLevel(this));
        }
    }

    @Override
    protected void onPause() {
        if (nsdServer != null) {
            nsdServer.onPause();
        }
        if (timeSyncServer != null) {
            timeSyncServer.onPause();
        }
        if (timeSyncClient != null) {
            timeSyncClient.onPause();
        }
        if (mConnection !=null) {
            unbindService(mConnection);
            mService = null;
        }
        if (timeOutHandler!=null) {
            timeOutHandler.removeCallbacksAndMessages(null);
        }
        if (handler!=null) {
            handler.removeCallbacksAndMessages(null);
        }
        if (logFile !=null) {
            logFile.log("Server finished, " + SystemUtilities.getBatteryLevel(this) + ", " + traverses);
            logFile.close();
        }
        newActivity = false;
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (nsdServer != null) {
            nsdServer.onDestroy();
        }
        if (timeSyncServer != null) {
            timeSyncServer.onDestroy();
            timeSyncServer = null;
        }
        if (timeSyncClient != null) {
            timeSyncClient.onDestroy();
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.main_settings_id: {
                Intent i = new Intent(this, SettingsActivity.class);
                startActivity(i);
                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    private Server.ServerListener serverListener = new Server.ServerListener() {
        @Override
        public void onServerStarted(int port) {
            Log.d(TAG, "server started");
            if (nsdServer != null) {
                Log.e(TAG, "server restarted?");
                nsdServer.onDestroy();
            }
            nsdServer = new NSDServer(getApplicationContext(), getString(R.string.NSDServiceName), getString(R.string.NSDServiceType), port, nsdServerListener);
            nsdServer.registerService();
        }
        @Override
        public void setStartTime(long t) {
            if (PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("dev",false)) {
                if (!watch.isRunning()) {
                    watch.start(t);
                }
            } else {
                if (!timeSpan.isRunning()) {
                    timeSpan.start(t);
                    if (timeOutHandler!=null) {
                        //timeOutHandler.removeCallbacksAndMessages(null);
                    }
                }
            }
        }
    };

    private NSDServer.NSDServerListener nsdServerListener = new NSDServer.NSDServerListener() {
        @Override
        public void onRegistered(final int id) {
            Log.d(TAG, "NSD service registered");

            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (serviceId != null) {
                        serviceId.setText((id + ""));
                    }
                    serviceNr=id;
                }
            });
        }

        @Override
        public void onUnregistered() {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "nsd service unregistered");
                    if (serviceId !=null) {
                        serviceId.setText(("-"));
                    }
                    serviceNr=-1;
                }
            });
        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "service bound");
            BoundFilterService.LocalBinder binder = (BoundFilterService.LocalBinder) service;
            mService = binder.getService();
            mService.startFilter();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

}
