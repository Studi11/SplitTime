package com.studi.timesyncwifi.Wifi;


import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

public class NSDServer {

    private final String TAG = "TimeSync:NSDServer";

    private String ServiceName = "Service";
    private String ServiceType = "service";
    private int ServicePort = 9000;
    private int ServiceID = 0;

    private boolean registered = false;
    private boolean reregistering = false;
    private boolean closing = false;

    private Context context;

    private NsdManager nsdManager = null;
    private NSDServerListener nsdServerListener = null;


    public NSDServer(Context context, String serviceName, String serviceType, int servicePort, NSDServerListener nsdServerListener) {
        this.context = context;
        if (nsdManager == null) {
            nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        }
        ServiceName = serviceName;
        ServiceType = serviceType;
        ServicePort = servicePort;
        this.nsdServerListener = nsdServerListener;
    }

    public void registerService() {
        if (!registered) {
            NsdServiceInfo serviceInfo = new NsdServiceInfo();
            serviceInfo.setServiceName(ServiceName+ServiceID);
            serviceInfo.setServiceType("_" + ServiceType + "._tcp.");
            serviceInfo.setPort(ServicePort);

            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
        } else {
            Log.e(TAG, "already registered");
        }
    }

    public void unregisterService() {
        if (registered) {
            nsdManager.unregisterService(mRegistrationListener);
        } else {
            Log.e(TAG, "nothing registered");
        }
    }

    private void reregisterService() {
        if (!reregistering) {
            reregistering = true;
            unregisterService();
        } else {
            ServiceID++;
            reregistering = false;
            registerService();
        }
    }


    private NsdManager.RegistrationListener mRegistrationListener = new NsdManager.RegistrationListener() {

        @Override
        public void onServiceRegistered(NsdServiceInfo nsdServiceInfo) {
            Log.d(TAG, "Registered service : " + ServiceName+ServiceID+"  "+nsdServiceInfo.getServiceName());
            registered = true;
            if (!nsdServiceInfo.getServiceName().equals(ServiceName+ServiceID)) {
                Log.d(TAG, "ID already exists, trying next");
                reregisterService();
                return;
            }
            ServiceName = nsdServiceInfo.getServiceName();
            nsdServerListener.onRegistered(ServiceID);
        }

        @Override
        public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.e(TAG, "registration failed: "+serviceInfo+" | "+errorCode);
        }

        @Override
        public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
            Log.d(TAG,"Service Unregistered : " + serviceInfo.getServiceName() + " , "+ServiceName+ServiceID);
            registered = false;
            if (reregistering) {
                Log.d(TAG, "reregistering");
                reregisterService();
                return;
            }
            if (closing) {
                mRegistrationListener = null;
                nsdManager = null;
            }
            nsdServerListener.onUnregistered();
        }

        @Override
        public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.e(TAG, "unregistrationFailed code "+errorCode+", info: "+serviceInfo);
        }
    };

    public void onResume() {
        registerService();
    }

    public void onPause(){
        onDestroy();
    }

    public void onDestroy() {
        closing = true;
        unregisterService();
    }


    public interface NSDServerListener {
        void onRegistered(int id);
        void onUnregistered();
    }

}
