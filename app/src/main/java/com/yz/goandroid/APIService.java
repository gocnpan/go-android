package com.yz.goandroid;// MyService.java

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.core.app.NotificationCompat;

public class APIService extends Service {
    public static final String TAG = "APIService";

    public static final String CHANNEL_ID = "service_01";
    private Notification notification;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(TAG, "APIService -> onCreate");

        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                "startForeground",
                NotificationManager.IMPORTANCE_DEFAULT);

        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("")
                .setContentText("").build();

        startForeground(1, notification);

        Context ctx = this;
        new Thread(new Runnable() {
            @Override
            public void run() {
                ExecutableUtil.extractAndRunExecutable(ctx);
            }
        }).start();

        Log.i(TAG,"APIService -> onCreate, Thread ID: " + Thread.currentThread().getId());
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "APIService -> onBind, Thread ID: " + Thread.currentThread().getId());
        // 如果Service不支持绑定，则返回null
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "APIService -> onStartCommand, startId: " + startId + ", Thread ID: " + Thread.currentThread().getId());

        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                "startForeground",
                NotificationManager.IMPORTANCE_DEFAULT);

        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("")
                .setContentText("").build();

        startForeground(1, notification);

        // 在此处处理具体的逻辑，例如执行耗时操作或处理异步任务
        return START_STICKY; // 如果Service被意外终止，系统会尝试重新启动Service
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "APIService -> onDestroy, Thread ID: " + Thread.currentThread().getId());
        // 在Service被销毁前执行的操作
        super.onDestroy();
    }
}