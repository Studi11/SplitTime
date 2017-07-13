package com.studi.timesyncwifi.Activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.studi.timesyncwifi.R;
import com.studi.timesyncwifi.Utility.BoundFilterService;
import com.studi.timesyncwifi.Utility.LogFile;
import com.studi.timesyncwifi.Utility.OpenCVCamera;
import com.studi.timesyncwifi.Utility.OpenCVHandler;
import com.studi.timesyncwifi.Utility.SystemUtilities;
import com.studi.timesyncwifi.Wifi.Client;
import com.studi.timesyncwifi.Wifi.NSDClient;
import com.studi.timesyncwifi.Wifi.Server;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;


public class ClientActivity extends AppCompatActivity {

    private static final String TAG = "TimeSync:ClientA";
    private NSDClient nsdClient = null;
    private Server timeSyncServer = null;
    private Client timeSyncClient = null;


    private boolean newActivity = true;


    private ImageView warn;

    private Handler handler = new Handler(Looper.getMainLooper());

    private ArrayList<String> list = new ArrayList<>();
    private ServerIDArrayAdapter adapter;

    private OpenCVHandler openCVHandler;
    private OpenCVCamera javaCameraView;

    private BoundFilterService mService;
    private LogFile logFile;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.client_layout);

        ListView listView = (ListView) findViewById(R.id.serverList);
        findViewById(R.id.clientLayout).setVisibility(View.INVISIBLE);

        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        Intent i = new Intent(this, BoundFilterService.class);
        bindService(i, mConnection, Context.BIND_AUTO_CREATE);

        nsdClient = new NSDClient(this, getString(R.string.NSDServiceName), getString(R.string.NSDServiceType), resolvedListener);
        nsdClient.discoverServices();

        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("dev", false)) {
            list.add("Dev");
        } else {
            list.add("No services found yet");
        }
        adapter = new ServerIDArrayAdapter(this, R.layout.server_list_item_layout, list);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "clicked: "+position);
                if (list.get(position).equals("No services found yet")) {

                } else if (list.get(position).equals("Dev")) {
                    resolvedListener.onResolved(null);
                } else {
                    String selectedName = list.get(position);
                    Log.d(TAG, "clicked on "+selectedName);
                    NsdServiceInfo info = new NsdServiceInfo();
                    info.setServiceName(selectedName);
                    nsdClient.resolve(info);
                }
            }
        });



        timeSyncClient = new Client(this, this, new Client.ClientInterface() {
            @Override
            public void onConnected() {
            }

            @Override
            public void onDisconnected() {
            }
        }, getMainLooper());




        javaCameraView = (OpenCVCamera) findViewById(R.id.cameraView);
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("dev", false)) {
            javaCameraView.setVisibility(SurfaceView.VISIBLE);
            javaCameraView.enableView();
        }
        openCVHandler = new OpenCVHandler(javaCameraView, new OpenCVHandler.CameraListener() {
            @Override
            public void onTraversed() {
                Log.i(TAG, "traversing "+System.currentTimeMillis());
                try {
                    JSONObject o = new JSONObject();
                    o.put("type", "starttime");
                    o.put("T0", System.currentTimeMillis());
                    timeSyncClient.send(o);
                } catch (JSONException e) {
                    Log.e(TAG, "send onTraversed failed " + e);
                }
            }

            @Override
            public void onFailed(int code) {
                switch (code) {
                    case OpenCVHandler.SLOW_DEVICE: {
                        Log.w(TAG, "slow device, low fps and low resolution");
                    }
                }
            }
        });

    }




    @Override
    protected void onResume() {
        super.onResume();
        //OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
        if (!newActivity) {
            Intent intent = new Intent(this, TimeSyncActivity.class);
            startActivity(intent);
            finish();
        } else {
        }

        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("log", false)) {
            logFile = new LogFile();
            logFile.log("Client started, "+ SystemUtilities.getBatteryLevel(this));
        }
    }

    @Override
    protected void onPause() {
        if (nsdClient != null) {
            nsdClient.onPause();
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
        if (logFile !=null) {
            logFile.log("Server finished, " + SystemUtilities.getBatteryLevel(this));
            logFile.close();
        }
        newActivity = false;

        if (javaCameraView != null)
            javaCameraView.disableView();
        //stopFilter();

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (nsdClient != null) {
            nsdClient.onDestroy();
            nsdClient = null;
        }
        if (timeSyncServer != null) {
            timeSyncServer.onDestroy();
            timeSyncServer = null;
        }
        if (timeSyncClient != null) {
            timeSyncClient.onDestroy();
        }

        if (javaCameraView != null)
            javaCameraView.disableView();

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (mService!=null) {
            mService.stopFilter();
        }
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

    NSDClient.NSDClientListener resolvedListener = new NSDClient.NSDClientListener() {

        @Override
        public void onResolved(NsdServiceInfo service) {
            if (service != null) {
                Log.d(TAG, "onResolved " + service.getHost().getHostAddress() + "  " + service.getPort()+"\nconnecting...");
                timeSyncClient.setAddress(service.getHost());
                timeSyncClient.setPort(service.getPort());
                timeSyncClient.connect();
            } else {
                Log.d(TAG, "onResolved Dev");
            }
            nsdClient.stopDiscoverServices();
            // change layout
            Log.i(TAG, "changing view");
            handler.post(new Runnable() {
                @Override
                public void run() {
                    findViewById(R.id.clientLayout).setVisibility(View.VISIBLE);
                    findViewById(R.id.serverList).setVisibility(View.INVISIBLE);
                    if (mService!=null && !PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("dev",false)) {
                        mService.startFilter();
                    }
                    javaCameraView.enableView();
                    Log.i(TAG, "changed view");
                }
            });
        }

        @Override
        public void onServiceFound(String service) {
            addServerId(service);
        }

        @Override
        public void onServiceLost(String service) {
            removeServerId(service);
        }
    };

    private void addServerId(final String service) {
        Log.i(TAG, "Adding server service "+service);
        if (list.size()>0 && list.get(0).equals("No services found yet"))  {
            list.clear();
        }
        handler.post(new Runnable() {
            @Override
            public void run() {
                list.add(service);
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void removeServerId(final String service) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Iterator<String> i = list.iterator();
                while (i.hasNext()) {
                    if (i.next().equals(service)) {
                        i.remove();
                        break;
                    }
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    private class ServerIDArrayAdapter extends ArrayAdapter<String> {

        private ArrayList<String> alist = new ArrayList<>();

        public ServerIDArrayAdapter(Context context, int viewResourceId, ArrayList<String> ids) {
            super(context, viewResourceId, ids);
            alist = ids;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.server_list_item_layout, parent, false);
            TextView id = (TextView) rowView.findViewById(R.id.list_item_serverID);
            id.setText((alist.get(position)));
            return rowView;
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "service bound");
            BoundFilterService.LocalBinder binder = (BoundFilterService.LocalBinder) service;
            mService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

}
