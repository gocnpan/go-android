package com.yz.goandroid;// MyService.java

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RunService extends Service {
    public static final String TAG = "RunService";

    public static final String CHANNEL_ID = "yz_service";

    // 文件夹
    private static String dirRoot;
    private static String dirData;
    private static String dirIPFS;
    private static String dirCluster;
    private static String dirEFamily;

    // 文件名 这里用变量而不是常量是因为后续根据系统架构会有不同的程序名
    private static String exeIPFS = "ipfs.x86_64";
    private static String exeCluster = "cluster.x86_64";
    private static String exeEFamily = "e-family.x86_64";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "RunService -> onCreate");

        sfNote();
        
        run();

        Log.i(TAG,"RunService -> onCreate, Thread ID: " + Thread.currentThread().getId());
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "RunService -> onBind, Thread ID: " + Thread.currentThread().getId());
        // 如果Service不支持绑定，则返回null
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "RunService -> onStartCommand, startId: " + startId + ", Thread ID: " + Thread.currentThread().getId());

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
        Log.i(TAG, "RunService -> onDestroy, Thread ID: " + Thread.currentThread().getId());
        // 在Service被销毁前执行的操作
        super.onDestroy();
    }

    private void sfNote(){
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                "startForeground",
                NotificationManager.IMPORTANCE_DEFAULT);

        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("")
                .setContentText("").build();

        startForeground(1, notification);
    }

    private void run(){
        Context ctx = this;

        dirRoot = getFilesDir().getAbsolutePath();

        // 文件夹准备
        dirData = dirRoot + "/data";
        makeDir(dirData);
        dirIPFS = dirData + "/ipfs";
        makeDir(dirIPFS);
        dirCluster = dirData + "/cluster";
        makeDir(dirCluster);
        dirEFamily = dirData + "/e-family";
        makeDir(dirEFamily);


        // 这里运行程序
        new Thread(new Runnable() {
            @Override
            public void run() {
                runIPFS();
            }
        }).start();

    }

    // 通过 http get 请求判断服务是否开启
    private boolean checkApi(String apiUrl){
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS) // 设置连接超时时间
                .readTimeout(5, TimeUnit.SECONDS) // 设置读取超时时间
                .build();

        Request request = new Request.Builder()
                .url(apiUrl)
                .build();
        try {
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            // 处理响应体
            assert response.body() != null;
            String responseBody = response.body().string();
            Log.i(TAG, "请求返回结果: "+responseBody);
            return response.code() < 400;
            // ... 进一步处理或解析响应 ...
        } catch (IOException e) {
            Log.i(TAG, "请求api出错：", e);
            return false;
        }
    }

    // ipfs 是否启动
    private boolean isIPFSStarted(){
        return  checkApi("http://localhost:5001/version");
    }

    // ipfs cluster 是否启动
    private  boolean isClusterStarted(){
        return checkApi("http://localhost:9094/version");
    }

    // e家存储服务是否启动 e-family
    private  boolean isEFStarted(){
        return checkApi("http://localhost:8100/api/check");
    }

    private void runIPFS(){
        // 如果已经启动 ipfs 则不再启动
        if (isIPFSStarted()) return;

        Map<String, String> envs = new HashMap<String, String>();
        envs.put("IPFS_PATH", dirIPFS);

        String exePath = dirRoot+"/"+exeIPFS;

        File cfg = new File(dirIPFS + "/config");
        if (!cfg.exists()){ // 不存在则需要初始化
            if (!initIPFS(exePath, envs)) return; // 如果初始化失败则退出
        }

        try {
            RunExecutable.executeCommand(exePath + " daemon", envs);
        }catch (IOException | InterruptedException e){
            Log.e(TAG, "启动IPFS出错");
        }
    }
    
    private boolean initIPFS(String exePath, Map<String, String>  envs){
        try {
            // 复制程序
            RunExecutable.copyExecutableFromAssetsToInternalStorage(this, exePath, exeIPFS);

            // init
            RunExecutable.executeCommand(exePath + " init", envs);

            RunExecutable.executeCommand(exePath + " config --json API.HTTPHeaders.Access-Control-Allow-Methods '[\"PUT\", \"GET\", \"POST\"]'", envs);
            RunExecutable.executeCommand(exePath + " config --json API.HTTPHeaders.Access-Control-Allow-Origin '[\"*\"]'", envs);
            RunExecutable.executeCommand(exePath + " config --json Bootstrap []", envs);
            RunExecutable.executeCommand(exePath + " config --json Discovery.MDNS.Enabled false", envs);

            return true;
        } catch (IOException | InterruptedException e){
            return false;
        }
    }

    private static void executeCommand(String command, Map<String, String> envs) throws IOException, InterruptedException {
        Log.i(TAG, "command: " + command);

        // 创建ProcessBuilder实例
        ProcessBuilder processBuilder = new ProcessBuilder();
        // 设置要执行的命令和参数
        processBuilder.command("sh", "-c", command);

        // 通过environment方法获取环境变量的Map
        Map<String, String> env = processBuilder.environment();
        env.putAll(envs);

        Process process = processBuilder.start();

        BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader err = new BufferedReader(new InputStreamReader(process.getErrorStream()));

        String line;
        while ((line = in.readLine()) != null) {
            Log.i(TAG, line); // 打印输出结果
        }
        while ((line = err.readLine()) != null) {
            System.out.println(line); // 打印错误输出结果
        }

        int exitCode = process.waitFor();
        if (exitCode == 0) {
            Log.i(TAG, "Success");
        } else {
            Log.e(TAG, "Failed with exit code " + exitCode);
        }
        in.close();
        err.close();
        process.destroy();
    }

    private void runCluster(){}

    private void runEFamily(){}

    // 创建文件夹
    private static void makeDir(String path){
        File data = new File(path);
        if (!data.exists()){
            Log.i(TAG, "创建: "+ path);
            data.mkdir();
        } else{
            Log.i(TAG, path + "文件夹已存在");
        }
    }
}