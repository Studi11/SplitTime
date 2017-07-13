package com.studi.timesyncwifi.Utility;


import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class LogFile {

    private final String TAG = "TimeSync:Log";
    private BufferedWriter logOut;

    public void log(String s) {
        try {
            if (logOut==null) {
                openLogFileOnDevice();
            }
            logOut.write(System.currentTimeMillis()+", "+s+"\n");
        } catch (IOException e) {
            Log.e(TAG, "cannot log to file");
            e.printStackTrace();
        }
    }

    private void openLogFileOnDevice() throws IOException {
        File rootPath = Environment.getExternalStorageDirectory();
        File AppDir = new File(rootPath, "TimeSync");
        File LogFile = new File(AppDir, "Log.csv");
        ArrayList<String> logEntries = new ArrayList<>();
        if (!LogFile.exists()) {
            Log.d(TAG, "creating logfile "+LogFile.getAbsolutePath());
            if (!AppDir.exists()) {
                if (!AppDir.mkdir()) {
                    Log.e(TAG, "cannot create AppDir");
                } else {
                    logEntries.add("Created AppDir");
                }
            }
            if (!LogFile.createNewFile()) {
                Log.w(TAG, "cannot create logfile, already exists?");
            } else {
                logEntries.add("Created new LogFile");
            }
        }
        logOut = new BufferedWriter(new FileWriter(LogFile, true));
        if (logOut==null) {
            throw new IOException("cannot open log file");
        }
        for (String s: logEntries) {
            log(s);
        }
        log("Log File opened");
    }

    public void close() {
        try {
            log("Closed Log File");
            logOut.close();
            logOut = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
