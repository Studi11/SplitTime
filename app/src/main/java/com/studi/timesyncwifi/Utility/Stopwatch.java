package com.studi.timesyncwifi.Utility;


import android.os.Handler;

import java.util.Calendar;

public class Stopwatch {

    private boolean running = false;
    private long startTime = 0;
    private Handler handler = new Handler();
    private long dt = 0;
    private long refresh_time = 0;
    private Listener listener = null;
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (running) {
                update();
                if (refresh_time>0) {
                    handler.postDelayed(this, refresh_time);
                }
            }
        }
    };

    public Stopwatch(long refresh_time, Listener listener) {
        this.refresh_time = refresh_time;
        this.listener = listener;
    }

    public Stopwatch(Listener listener) {
        this.listener = listener;
        this.refresh_time = 0;
    }

    public Stopwatch() {
        this.refresh_time = 0;
    }

    public void start() {
        startTime = Calendar.getInstance().getTimeInMillis();
        reset();
        running = true;
        if (refresh_time>0) {
            handler.postDelayed(runnable, 0);
        }
    }

    public boolean isRunning() {
        return running;
    }

    public void start(long t) {
        reset();
        startTime = t;
        running = true;
        if (refresh_time>0) {
            handler.postDelayed(runnable, 5);
        }
    }

    public long stop() {
        if (running) {
            handler.removeCallbacksAndMessages(null);
            update();
            running = false;
        }
        return dt;
    }

    public long reset() {
        stop();
        dt = 0;
        if (listener!=null) {
            listener.onUpdate(dt);
        }
        return dt;
    }

    private long update() {
        if (running) {
            dt = Calendar.getInstance().getTimeInMillis() - startTime;
        }
        if (listener!=null) {
            listener.onUpdate(dt);
        }
        return dt;
    }

    public interface Listener {
        public void onUpdate(long dt);
    }

}
