package com.studi.timesyncwifi.Utility;


public class TimeSpan {

    private boolean running = false;
    private long startTime = 0;
    private long lastTotalTime = 0;

    public void start() {
        startTime = System.currentTimeMillis();
        running = true;
    }

    public void start(long t) {
        startTime = t;
        running = true;
    }

    public long getTime() {
        return System.currentTimeMillis()-startTime;
    }

    public long stop() {
        lastTotalTime = System.currentTimeMillis()-startTime;
        running = false;
        return lastTotalTime;
    }

    public boolean isRunning() {
        return running;
    }

}
