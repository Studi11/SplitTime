package com.studi.timesyncwifi.Wifi;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.text.format.Formatter;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Calendar;

public class Client {

    private final String TAG = "TimeSync:Client";

    private Context context;
    private Activity activity;

    private InetAddress address;
    private int port;

    private PrintWriter out;
    private BufferedReader in;

    private ArrayList<AsyncTask> tasks = new ArrayList<>();
    private ClientReceiveThread clientReceiveThread;

    private boolean connected = false;

    private ClientInterface clientInterface = null;
    private Handler handler;

    public Client(Context context, Activity activity, ClientInterface clientInterface, Looper looper) {
        this.context = context;
        this.activity = activity;
        this.clientInterface = clientInterface;
        handler = new Handler(looper);
    }

    public void send(JSONObject o) {
        // Log.d(TAG, "send");
        AsyncTask t = new ITask(o);
        tasks.add(t);
        t.execute();
    }

    public void connect() {
        // send new connection packet

        clientReceiveThread = new ClientReceiveThread();
        clientReceiveThread.start();

        JSONObject o = new JSONObject();
        try {
            o.put("type", "connect");
            o.put("ip", getLocalIpAddress());
            send(o);
        } catch (JSONException e) {
            Log.e(TAG, "JSONException 1 "+e);
            return;
        }


        connected = true;
        clientInterface.onConnected();
    }

    public void disconnect() {
        JSONObject o = new JSONObject();
        try {
            o.put("type", "disconnect");
            send(o);
        } catch (JSONException e) {
            Log.e(TAG, "JSONException 1 "+e);
            return;
        }
        if (clientReceiveThread!=null) {
            clientReceiveThread.interrupt();
        }
        connected = false;
        clientInterface.onDisconnected();
    }

    public boolean isConnected() {
        return connected;
    }

    public void onResume() {

    }

    public void onPause() {
        if (out != null) {
            out.flush();
            out.close();
            out = null;
        }
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                Log.e(TAG, "error 4 closing in stream " + e);
            }
            in = null;
        }
        disconnect();
    }

    public void onDestroy() {
        if (out != null) {
            out.flush();
            out.close();
            out = null;
        }
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                Log.e(TAG, "error closing in stream " + e);
            }
            in = null;
        }
    }

    public void setAddress(InetAddress address) {
        this.address = address;
    }

    public void setPort(int port) {
        this.port = port;
    }


    private String getLocalIpAddress() {
        WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        return Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
    }


    private class ITask extends AsyncTask {
        private JSONObject o;

        public ITask(JSONObject o) {
            this.o = o;
        }

        @Override
        protected Object doInBackground(Object[] params) {
            Socket socket = null;
            PrintWriter out = null;
            BufferedReader in = null;

            if (address==null || port==0) {
                Log.e(TAG, "no host found yet");
                return new Object();
            }

            try {
                socket = new Socket(address, port);
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                switch (o.getString("type")) {
                    case "starttime":
                        JSONObject syncO = new JSONObject();
                        syncO.put("type", "sync");
                        syncO.put("T1", Calendar.getInstance().getTimeInMillis());
                        out.println(syncO.toString());

                        String messageFromServer = in.readLine();
                        Log.d(TAG, "received sync: "+messageFromServer);
                        long T4 = Calendar.getInstance().getTimeInMillis();
                        syncO = new JSONObject(messageFromServer);
                        syncO.put("t2", T4-syncO.getLong("T3"));
                        syncO.remove("T3");
                        long diff = (syncO.getLong("t2")-syncO.getLong("t1"))/2;
                        syncO = null;

                        JSONObject startO = new JSONObject();
                        long T0 = o.getLong("T0")-diff;
                        startO.put("type", "starttime");
                        startO.put("time", T0);
                        out.println(startO.toString());
                        break;
                    default:
                        out.println(o.toString());
                }

                Log.d(TAG, "closing out socket task");
                JSONObject o = new JSONObject();
                o.put("type", "closed_connection");
                out.println(o.toString());
                socket.close();

            } catch (IOException e) {
                Log.e(TAG, "IOException 1 "+e);
                e.printStackTrace();
            } catch (JSONException ex) {
                Log.e(TAG, "JSONExeption "+ex);
                ex.printStackTrace();
            }

            socket = null;
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    Log.e(TAG, "IOException2");
                    e.printStackTrace();
                }
                in = null;
            }

            if (out != null) {
                out.close();
                out = null;
            }
            return null;
        }
    }

    private class ClientReceiveThread extends Thread {

        Socket socket = null;
        PrintWriter out = null;
        BufferedReader in = null;
        String input = "";

        @Override
        public void run() {
            try {
                socket = new Socket(address, port);
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                try {
                    JSONObject ob = new JSONObject();
                    ob.put("type", "connect");
                    ob.put("ip", getLocalIpAddress());
                    out.println(ob.toString());

                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            input = in.readLine();
                            Log.i(TAG, "client received:  " + input);
                            JSONObject o = new JSONObject(input);
                            if (o.get("type").equals("server_disconnect")) {
                                Log.d(TAG, "server_disconnect");
                                Thread.currentThread().interrupt();
                                disconnect();
                            /*
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    disconnect();
                                }
                            });
                            */
                            } else {
                                Log.d(TAG, "client server received " + o.toString());
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "jsonexcep " + e);
                        }
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "json except "+e);
                }

            } catch (IOException e) {
                Log.e(TAG, "IOException 3 "+e);
            }
        }
    }

    public interface ClientInterface {
        void onConnected();
        void onDisconnected();
    }

}
