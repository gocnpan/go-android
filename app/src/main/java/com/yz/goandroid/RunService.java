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
    private static String dirAndroid;
    private static String fileSysinfo;

    // 文件名 这里用变量而不是常量是因为后续根据系统架构会有不同的程序名
    private static String exeIPFS = "ipfs.x86_64";
    private static String exeCluster = "cluster.x86_64";
    private static String exeEFamily = "e-family.x86_64";
    private static String exeAndroid = "android.x86_64";
    private  static String sysinfo = "sysinfo.json";

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

    private void run()  {
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
        dirAndroid = dirData + "/android";
        makeDir(dirAndroid);

        // 准备 sysinfo.json 文件
        fileSysinfo = dirData + "/" + sysinfo;

        try {
            RunExecutable.copyFileToStorage(this, fileSysinfo, sysinfo);
        } catch (IOException e){
            Log.e(TAG, "复制sysinfo文件出错" + e);
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                runAndroid();
            }
        }).start();

//        // 运行 ipfs
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                runIPFS();
//            }
//        }).start();
//
//        sleep(5000); // 暂停 5 秒
//        // 运行 ipfs cluster
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                runCluster();
//            }
//        }).start();
//
//        sleep(5000); // 暂停 5 秒
//        // 运行 e family
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                runEFamily();
//            }
//        }).start();
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
            Log.e(TAG, "启动IPFS出错" + e);
        }
    }
    
    private boolean initIPFS(String exePath, Map<String, String>  envs) {
        try {
            // 复制程序
            RunExecutable.copyExecutableFromAssetsToInternalStorage(this, exePath, exeIPFS);

            // init
            RunExecutable.executeCommand(exePath + " init --profile=lowpower", envs);

            // RunExecutable.executeCommand(exePath + " config --json API.HTTPHeaders.Access-Control-Allow-Methods '[\"PUT\", \"GET\", \"POST\"]'", envs);
            // RunExecutable.executeCommand(exePath + " config --json API.HTTPHeaders.Access-Control-Allow-Origin '[\"http://127.0.0.1:5001\", \"http://localhost:5001\"]'", envs);
            // 配置跨域
            RunExecutable.executeCommand(exePath + " config --json API.HTTPHeaders.Access-Control-Allow-Methods '[\"PUT\", \"GET\", \"POST\"]'", envs);
            RunExecutable.executeCommand(exePath + " config --json API.HTTPHeaders.Access-Control-Allow-Origin '[\"*\"]'", envs);
            RunExecutable.executeCommand(exePath + " config --json Addresses.API '[\"/ip4/0.0.0.0/tcp/5001\"]'", envs);

            RunExecutable.executeCommand(exePath + " config --json Bootstrap []", envs);
            RunExecutable.executeCommand(exePath + " config --json Discovery.MDNS.Enabled false", envs);

            return true;
        } catch (IOException | InterruptedException e){
            return false;
        }
    }

    private void runCluster(){
        // 如果 ipfs 未运行则不运行 ipfs cluster
        if (!isIPFSStarted() || isClusterStarted()) return;

        Map<String, String> envs = new HashMap<String, String>();
        envs.put("IPFS_CLUSTER_PATH", dirCluster); // 数据存放路径

        
        String exePath = dirRoot+"/"+exeCluster;
        
        File cfg = new File(dirCluster + "/service.json");
        if (!cfg.exists()){
            if (!initCluster(exePath, envs)) return;
        }

        
        try {
            RunExecutable.executeCommand(exePath + " daemon", envs);
        }catch (IOException | InterruptedException e){
            Log.e(TAG, "启动IPFS Cluster出错" + e);
        }
    }

    private boolean initCluster(String exePath, Map<String, String>  envs){
        try {
            // 复制程序
            RunExecutable.copyExecutableFromAssetsToInternalStorage(this, exePath, exeCluster);

            Map<String, String> ne = new HashMap<String, String>();
            ne.putAll(envs);
            ne.put("CLUSTER_REPLICATIONFACTORMIN", "2");
            ne.put("CLUSTER_REPLICATIONFACTORMAX", "3");
            ne.put("CLUSTER_DISABLEREPINNING", "false");
            ne.put("CLUSTER_SECRET", "fa6aa72ad61b6218453059a7b311999e1fd67f5d07b7db3ac7e908c9a4a47bcd");
            // ne.put("CLUSTER_RESTAPI_HTTPLISTENMULTIADDRESS", "/ip4/127.0.0.1/tcp/9094");
            // ne.put("CLUSTER_RESTAPI_CORSALLOWEDORIGINS", "[\"http://127.0.0.1:9094\", \"http://localhost:9094\"]'");
            // 用于压力测试
            ne.put("CLUSTER_RESTAPI_HTTPLISTENMULTIADDRESS", "/ip4/0.0.0.0/tcp/9094");
            ne.put("CLUSTER_RESTAPI_CORSALLOWEDORIGINS", "[\"*\"]'");

            ne.put("CLUSTER_RESTAPI_CORSALLOWEDMETHODS", "[\"PUT\", \"GET\", \"POST\"]'");
            ne.put("CLUSTER_IPFSPROXY_NODEMULTIADDRESS", "/ip4/127.0.0.1/tcp/5001");
            ne.put("CLUSTER_IPFSHTTP_NODEMULTIADDRESS", "/ip4/127.0.0.1/tcp/5001");
            ne.put("CLUSTER_CRDT_TRUSTEDPEERS", "*");
            ne.put("CLUSTER_PEBBLE_PEBBLEOPTIONS_MEMTABLESIZE", "41943040");
            ne.put("CLUSTER_PEBBLE_PEBBLEOPTIONS_LBASEMAXBYTES", "83886080");
            ne.put("CLUSTER_PEBBLE_PEBBLEOPTIONS_CACHESIZEBYTES", "335544320");

            // init --force: 只会重新生成配置项, id 和其他数据不会发生变化
            RunExecutable.executeCommand(exePath + " init --force", ne);

            return true;
        } catch (IOException | InterruptedException e){
            return false;
        }
    }

    private void runEFamily(){
        // 如果 ipfs 未运行 或 当前程序已运行 则不运行
        if (!isIPFSStarted() || isEFStarted()) return;

        Map<String, String> envs = new HashMap<String, String>();
        String pubIpfsAddr = "/dns4/ipfs-wss.hw.isecsp.com/tcp/443/wss/p2p/12D3KooWMhh4AjeT6Wv7t3GJ5YY5UpyABjK5GtZVxaxd35GLg9CR";
        // 基础
        envs.put("OPERATOR_SERVICE_PATH", dirEFamily);
        envs.put("OPERATOR_SERVICE_MODE", "storage");
        envs.put("OPERATOR_SERVICE_IPFSADDR", pubIpfsAddr);
        envs.put("OPERATOR_SERVICE_CONCURRENTEXEMAX", "30");
        envs.put("OPERATOR_SERVICE_CHECKNUMMAX", "50");
        envs.put("OPERATOR_SERVICE_DISPUTENUMMAX", "51");

        // 连接
        envs.put("OPERATOR_SERVICE_DEVICE_EOSADDRESS", "http://111.67.196.206:8888");
        envs.put("OPERATOR_SERVICE_EOSIO_EOSADDRESS", "http://111.67.196.206:8888");
        // ipfs 
        envs.put("OPERATOR_SERVICE_IPFS_TIMEOUT", "25m");
        envs.put("OPERATOR_SERVICE_IPFS_APIADDRESS", "http://127.0.0.1:5001");
        envs.put("OPERATOR_SERVICE_IPFS_REPEAT", "2");
        envs.put("OPERATOR_SERVICE_IPFS_CLUSTER_MULTIADDRESS", "/ip4/127.0.0.1/tcp/9094");
        // device
        envs.put("OPERATOR_SERVICE_DEVICE_HTTPPORT", "8100");
        envs.put("OPERATOR_SERVICE_DEVICE_RELAYURL", "https://platform.storage.svconcloud.com");
        envs.put("OPERATOR_SERVICE_DEVICE_USERROOTDIR", dirEFamily);
        envs.put("OPERATOR_SERVICE_DEVICE_SYSINFOPATH", fileSysinfo);
        envs.put("OPERATOR_SERVICE_DEVICE_IPFSCFGPATH", dirIPFS + "/config");
        envs.put("OPERATOR_SERVICE_DEVICE_CLUSTERIDPATH", dirCluster + "/service.json");
        envs.put("OPERATOR_SERVICE_DEVICE_POINTSYMBOL", "SYS");
        envs.put("OPERATOR_SERVICE_DEVICE_POINTCODE", "eosio.token");
        envs.put("OPERATOR_SERVICE_DEVICE_STARTUPASSET", "50000.0000 SYS");
        envs.put("OPERATOR_SERVICE_DEVICE_INTRVCHECKSTARTUP", "5s");
        envs.put("OPERATOR_SERVICE_DEVICE_MONNETINT", "3");
        // 针对 eos
        envs.put("OPERATOR_SERVICE_DEVICE_CONTRACTNAME", "storeorder");
        envs.put("OPERATOR_SERVICE_DEVICE_PERMISSION", "@active");
        envs.put("OPERATOR_SERVICE_DEVICE_RECORDLIMIT", "30");
        // 其他角色配置 避免报错
        envs.put("OPERATOR_SERVICE_CHALLENGER_IPFSADDR", pubIpfsAddr);
        envs.put("OPERATOR_SERVICE_PLATFORM_REPLICATIONFACTORMIN", "2");
        envs.put("OPERATOR_SERVICE_PLATFORM_REPLICATIONFACTORMAX", "3");
        envs.put("OPERATOR_SERVICE_PLATFORM_IPFSPUBADDRS", pubIpfsAddr);
        // envs.put("OPERATOR_SERVICE_PLATFORM_IPFSGATEWAY", "http://111.67.196.206:8080");
        envs.put("OPERATOR_SERVICE_PLATFORM_IPFSGATEWAY", "https://ipfs-gateway.hw.isecsp.com");
        envs.put("OPERATOR_SERVICE_PLATFORM_CLUSTERCFGCID", "QmPgzgdV1yjkFJEzpu5npCJegXPdCXFwD9jcD7drfEF2Sf");

        // storage
        envs.put("OPERATOR_SERVICE_STORAGE_DELAFTERUPLOADED", "true");
        envs.put("OPERATOR_SERVICE_STORAGE_REFRESHPLATFORMINTERVAL", "2m");
        envs.put("OPERATOR_SERVICE_STORAGE_REFRESHSERVICEINTERVAL", "1m");
        envs.put("OPERATOR_SERVICE_STORAGE_REFRESHPLATFORDERINTERVAL", "1m");
        envs.put("OPERATOR_SERVICE_STORAGE_REFRESHSELFORDERINTERVAL", "1m");
        envs.put("OPERATOR_SERVICE_STORAGE_QUESTIONINTERVAL", "15s");
        envs.put("OPERATOR_SERVICE_STORAGE_ANSWERIPNSNAME", "question_answer");
        envs.put("OPERATOR_SERVICE_STORAGE_CONCURRENTEXEMAX", "10");
        envs.put("OPERATOR_SERVICE_STORAGE_IPFSADDR", pubIpfsAddr);
        envs.put("OPERATOR_SERVICE_STORAGE_CHECKNUMMAX", "50");
        envs.put("OPERATOR_SERVICE_STORAGE_DISPUTENUMMAX", "5");

        // sqlite
        envs.put("OPERATOR_SERVICE_SQLITE_FOLDER", "s3");
        envs.put("OPERATOR_SERVICE_SQLITE_FILENAME", "yz.s3db");
        envs.put("OPERATOR_SERVICE_SQLITE_ENABLECRYPT", "true");
        envs.put("OPERATOR_SERVICE_SQLITE_USERNAME", "yz");
        envs.put("OPERATOR_SERVICE_SQLITE_PASSWORD", "yz@2024");
        envs.put("OPERATOR_SERVICE_SQLITE_CRYPTTYPE", "SSHA1");
        envs.put("OPERATOR_SERVICE_SQLITE_CRYPTSALT", "1afs2DsH3@56ar");
        // 日志
        envs.put("OPERATOR_SERVICE_LOG_LEVEL", "debug"); // debug, info, warn, error, dpanic, panic, fatal
        envs.put("OPERATOR_SERVICE_LOG_FOLDER", "logs");
        envs.put("OPERATOR_SERVICE_LOG_MAXSIZE", "5");
        envs.put("OPERATOR_SERVICE_LOG_MAXBACKUPS", "60");
        envs.put("OPERATOR_SERVICE_LOG_MAXAGE", "30");
        envs.put("OPERATOR_SERVICE_LOG_LOCALTIME", "true");
        envs.put("OPERATOR_SERVICE_LOG_COMPRESS", "true");
        envs.put("OPERATOR_SERVICE_LOG_LOGINCONSOLE", "false");
        envs.put("OPERATOR_SERVICE_RELAY_SERVERADDR", "111.67.196.206:8523");
        envs.put("OPERATOR_SERVICE_RELAY_TCPTIMEOUT", "90");

        envs.put("GIN_MODE", "release");

        String exePath = dirRoot+"/"+exeEFamily;


        try {
            // 复制程序
            File exe = new File(exePath);
            if (!exe.exists()){
                RunExecutable.copyExecutableFromAssetsToInternalStorage(this, exePath, exeEFamily);
            }

            RunExecutable.executeCommand(exePath + " daemon", envs);
        }catch (IOException | InterruptedException e){
            Log.e(TAG, "启动 e-family 出错" + e);
        }
    }

    private void runAndroid(){
        Map<String, String> envs = new HashMap<String, String>();
        envs.put("AP_NDK_BASE_DIR", dirAndroid);

        String exePath = dirRoot+"/"+exeAndroid;

        try {
            RunExecutable.copyExecutableFromAssetsToInternalStorage(this, exePath, exeAndroid);
            RunExecutable.executeCommand(exePath, envs);
        } catch (IOException | InterruptedException e){
            Log.e(TAG, "执行Android出错" + e);
        }
    }

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

    // ipfs 是否启动
    private boolean isIPFSStarted(){
        return  checkApi("http://127.0.0.1:5001/version");
    }

    // ipfs cluster 是否启动
    private  boolean isClusterStarted(){
        return checkApi("http://127.0.0.1:9094/version");
    }

    // e家存储服务是否启动 e-family
    private  boolean isEFStarted(){
        return checkApi("http://127.0.0.1:8100/api/check");
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

    // sleep 毫秒
    private void sleep(long millis){
        try {
            // 暂停5秒
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // 如果线程在sleep期间被中断，会抛出InterruptedException异常
            Log.e(TAG, "sleep err：", e);
        }
    }
}