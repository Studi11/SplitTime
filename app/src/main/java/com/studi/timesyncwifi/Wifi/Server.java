package com.studi.timesyncwifi.Wifi;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;

public class Server {

    private Context context;
    private Activity activity;
    private ServerSocket serverSocket;
    private Thread socketServerThread = null;
    private int port;
    private static final String TAG = "TimeSync:Server";

    private boolean running = false;
    private boolean shouldRun = false;

    private ServerListener mServerListener;
    private Looper looper;

    private ArrayList<Thread> socketThreads;

    public Server(Activity activity, Context context, ServerListener serverListener, Looper looper) {
        //this.interf = interf;
        this.context = context;
        this.looper = looper;
        this.activity = activity;
        this.mServerListener = serverListener;
    }

    public void start() {
        if (!running) {
            Log.d(TAG, "starting server");
            running = true;
            shouldRun = true;
            socketServerThread = new Thread(new SocketServerThread());
            socketServerThread.start();
        } else {
            Log.e(TAG, "called Server.start twice!");
        }
    }

    public int getPort() {
        return port;
    }

    private void setPort(int port) {
        this.port = port;
    }

    public void onResume() {
        if (shouldRun && !running) {
            start();
        }
    }

    public void onPause() {
        if (socketServerThread != null) {
            socketServerThread.interrupt();
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "error closing socket " + e);
            }
            socketServerThread = null;
        }
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        running = false;
    }

    public void onDestroy() {
        running = false;
        shouldRun = false;
        if (socketServerThread != null) {
            socketServerThread.interrupt();
            socketServerThread = null;
        }
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "server.onDestroy failed");
            }
            serverSocket = null;
        }
        Log.d(TAG, "server destroyed");
    }

    private class SocketServerThread extends Thread {
        Socket socket = null;

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(0);
                Log.d(TAG, "server port: " + serverSocket.getLocalPort());
                setPort(serverSocket.getLocalPort());
                if (mServerListener != null) {
                    mServerListener.onServerStarted(getPort());
                } else {
                    Log.e(TAG, "NO SERVERLISTENER!");
                }
                socketThreads = new ArrayList<>();

                while (!Thread.currentThread().isInterrupted()) {
                    Socket socket = serverSocket.accept();
                    Thread socketThread = new ServerHandler(activity, context, socket, looper);
                    socketThreads.add(socketThread);
                    socketThread.start();

                }
                Log.d(TAG, "interrupted serverthread");

            } catch (IOException e) {
                Log.e(TAG, "IOException " + e);
                e.printStackTrace();
            } finally {
                if (socketThreads!=null && socketThreads.size() > 0) {
                    Iterator<Thread> i = socketThreads.iterator();
                    while (i.hasNext()) {
                        i.next().interrupt();
                    }
                    socketThreads = null;
                }
            }
        }

    }

    public interface ServerListener {
        public void onServerStarted(int port);
        public void setStartTime(long t);
    }

    public void send(JSONObject o, InetAddress address, int port) {
        // Log.d(TAG, "send");
        AsyncTask t = new ITask(o, address, port);
        t.execute();
    }

    public class ServerHandler extends Thread {

        private final String TAG = "TimeSync:ServerHanlder";

        private Socket socket;
        private DataInputStream inputStream;
        private DataOutputStream outputStream;
        private Looper looper;
        private long T2;

        private Activity activity;
        private Context context;

        public ServerHandler(Activity activity, Context context, Socket socket, Looper looper) {
            T2 = Calendar.getInstance().getTimeInMillis();
            //this.interf = interf;
            this.activity = activity;
            this.context = context;
            this.socket = socket;
            this.looper = looper;
            Log.i(TAG, "new Client connected: " + socket.getInetAddress() + ":" + socket.getPort());
        }

        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

                JSONObject rec;
                String input;

                while (!Thread.currentThread().isInterrupted()) {
                    input = in.readLine();
                    if (input==null) {
                        continue;
                    }

                    try {
                        rec = new JSONObject(input);

                        Log.i(TAG, "server received " + rec.toString());

                        if (rec.getString("type").equals("sync")) {
                            Log.d(TAG, "syncrequest");
                            rec.put("t1", T2-rec.getLong("T1"));
                            rec.remove("T1");
                            rec.put("T3", Calendar.getInstance().getTimeInMillis());
                            out.println(rec.toString());
                            Log.d(TAG, "sent timestamp");








                        } else if (rec.getString("type").equals("starttime")) {
                            Log.d(TAG, "starttime received");
                            //interf.setStartTime(rec.getLong("time"));
                            final long startime = rec.getLong("time");
                            new Handler(looper).post(new Runnable() {
                                @Override
                                public void run() {
                                    mServerListener.setStartTime(startime);
                                }
                            });





                        } else if (rec.getString("type").equals("closed_connection")) {
                            Log.d(TAG, "closing in socket");
                            // remove socket
                            interrupt();




                        } else if (rec.getString("type").equals("connect")) {
                            Log.d(TAG, "a client is connecting ("+rec.getString("ip")+")");

                            //send ack packet back
                            JSONObject o = new JSONObject();
                            send(o,socket.getInetAddress(), 80);






                        } else {
                            Log.d(TAG, "socket rec type " + rec.getString("type"));
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "json error "+e);
                    }
                }

                socketThreads.remove(this);

            } catch (IOException e) {
                Log.e(TAG, "error "+e);
            }
            Log.d(TAG, "server to client connection closed");
        }
    }


    private class ITask extends AsyncTask {
        private JSONObject o;
        private InetAddress address;
        private int port;

        public ITask(JSONObject o, InetAddress address, int port) {
            this.o = o;
            this.address = address;
            this.port = port;
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
                Log.e(TAG, "IOException "+e);
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
}
