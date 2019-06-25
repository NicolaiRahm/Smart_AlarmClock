package com.nicolai.alarm_clock;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;

public class AppExecutor {

    //For Singelton instantiation
    private static final Object LOCK = new Object();
    private static AppExecutor sInstance;
    private final Executor diskI0;
    private final Executor mainThread;
    private final Executor networkI0;

    public AppExecutor(Executor diskI0, Executor mainThread, Executor networkI0) {
        this.diskI0 = diskI0;
        this.mainThread = mainThread;
        this.networkI0 = networkI0;
    }

    public static AppExecutor getInstance(){
        if(sInstance == null){
            synchronized (LOCK){
                sInstance = new AppExecutor(Executors.newSingleThreadExecutor(),
                        Executors.newFixedThreadPool(3),
                        new MainThreadExecutor());
            }
        }
        return sInstance;
    }

    public Executor diskI0() {return diskI0; }

    public Executor mainThreat() {return mainThread; }

    public Executor networkI0() {return networkI0; }

    private static class MainThreadExecutor implements Executor{
        private Handler mainThreadHandler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(@NonNull Runnable command) {
            mainThreadHandler.post(command);
        }
    }
}
