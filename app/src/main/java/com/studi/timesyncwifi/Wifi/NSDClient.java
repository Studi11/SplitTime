package com.studi.timesyncwifi.Wifi;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

public class NSDClient {

    private final String TAG = "TimeSync:NSDClient";

    private String ServiceName = "Service";
    private String ServiceType = "service";

    private WifiManager.MulticastLock multicastLock = null;
    private NsdManager nsdManager = null;
    private NSDClientListener resolvedListener = null;
    private Context mContext;

    private boolean discover = false;
    private boolean discovering = false;

    public NSDClient(Context context, String serviceName, String serviceType, NSDClientListener resolvedListener) {
        mContext = context;
        this.resolvedListener = resolvedListener;
        if (nsdManager == null) {
            nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        }
        ServiceName = serviceName;
        ServiceType = serviceType;
    }

    public void discoverServices() {
        if (!discovering) {
            discoverServicesP();
        }
    }

    private void discoverServicesP() {
        WifiManager wifi = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        if (wifi!=null) {
            multicastLock = wifi.createMulticastLock("myLock");
            multicastLock.acquire();
            Log.i(TAG, "multicastLock acquired");
        }
        nsdManager.discoverServices("_"+ServiceType+"._tcp.", NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
        discovering = true;
    }

    public void stopDiscoverServices() {
        if (discovering) {
            stopDiscoverServicesP();
        }
        discover = false;
    }

    private void stopDiscoverServicesP() {
        try {
            nsdManager.stopServiceDiscovery(mDiscoveryListener);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "stopping discovery, already stopped");
        }
        if (multicastLock !=null && multicastLock.isHeld()) {
            multicastLock.release();
            Log.i(TAG, "multicastLock released");
            multicastLock = null;
        }
        discovering = false;
    }

    public void resolve(NsdServiceInfo service) {
        if (service.getServiceName()==null) {
            Log.e(TAG, "resolve name null");
            return;
        }
        if (service.getServiceType()==null) {
            service.setServiceType("_"+ServiceType+"._tcp.");
        }
        nsdManager.resolveService(service, mResolveListener);
    }


    private NsdManager.DiscoveryListener mDiscoveryListener = new NsdManager.DiscoveryListener() {

        // Called as soon as service discovery begins.
        @Override
        public void onDiscoveryStarted(String regType) {
            Log.i(TAG, "Service discovery started "+regType);
        }

        @Override
        public void onServiceFound(NsdServiceInfo service) {
            String exactServiceType = "_"+ServiceType+"._tcp.";
            if (!service.getServiceType().equals(exactServiceType)) {
                Log.d(TAG, "Other Service Type: " + service.getServiceType());
            } else {
                Log.d(TAG, "service found " + service);
                resolvedListener.onServiceFound(service.getServiceName());
            }
        }

        @Override
        public void onServiceLost(NsdServiceInfo service) {
            Log.e(TAG, "service lost " + service);
            resolvedListener.onServiceLost(service.getServiceName());
        }

        @Override
        public void onDiscoveryStopped(String serviceType) {
            if (multicastLock!=null && multicastLock.isHeld()) {
                multicastLock.release();
            }
            Log.i(TAG, "Discovery stopped: " + serviceType+", released multicast lock");
        }

        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, "Discovery failed: Error code:" + errorCode+", serviceType: "+serviceType);
            nsdManager.stopServiceDiscovery(this);
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, "Discovery failed: Error code:" + errorCode+", with "+serviceType);
            nsdManager.stopServiceDiscovery(this);
        }
    };

    private NsdManager.ResolveListener mResolveListener = new NsdManager.ResolveListener() {

        @Override
        public void onServiceResolved(NsdServiceInfo serviceInfo) {
            Log.d(TAG, "service found resolved: "+serviceInfo);
            resolvedListener.onResolved(serviceInfo);
        }

        @Override
        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.e(TAG, "Resolve failed " + errorCode);
            Log.e(TAG, "serivce = " + serviceInfo);
        }
    };

    public void onResume() {
        Log.d(TAG, "onResume");
        if (discover && !discovering) {
            discoverServicesP();
        }
    }

    public void onPause() {
        if (discover) {
            stopDiscoverServicesP();
        }
    }

    public void onDestroy() {
        stopDiscoverServices();
    }

    public interface NSDClientListener {
        void onResolved(NsdServiceInfo service);
        void onServiceFound(String service);
        void onServiceLost(String service);
    }
}
