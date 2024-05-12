package com.yz.goandroid;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    // 按钮
    private Button btnAuth;
    private Button btnRun;
    private Button btnCheck;

    // 访问网络
    private static final String API_IPFS = "http://127.0.0.1:5001/version";
    private static final String API_Cluster = "http://127.0.0.1:9094/version";
//    private static final String API_EFamily = "http://127.0.0.1:8100/api/check"; // /common/http_api
    private static final String API_EFamily = "http://127.0.0.1:8100/api/common/http_api";
    private static final String API_AndroidAddr = "http://127.0.0.1:8365/api/addr";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 获取按钮的引用
        btnAuth = findViewById(R.id.btn_auth);
        // 获取运行按钮引用
        btnRun = findViewById(R.id.btn_run);
        // 测试服务是否正常开启
        btnCheck = findViewById(R.id.btn_check);

        // 授权按钮
        btnAuth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // 前台服务权限
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.FOREGROUND_SERVICE)
                        != PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "获取前台运行服务的权限");

                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.FOREGROUND_SERVICE},
                            101); // 这里的常量可以自定义
                } else{
                    Log.i(TAG, "已有前台运行服务的权限");
                }

                // 文件管理权限
                if (!Environment.isExternalStorageManager()) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    Log.i(TAG, "获取文件管理权限 package: "+ getPackageName());
                    // intent.setData(Uri.parse("package:" + activity.getPackageName()));
                    intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                    startActivity(intent);
                } else{
                    Toast.makeText(MainActivity.this, "已完成外部文件夹访问授权", Toast.LENGTH_SHORT).show();
                }

                // 判断并获取 root 权限
//                CommandExecution.getRoot(MainActivity.this);
            }
        });

        // 运行按钮
        btnRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!Environment.isExternalStorageManager()) {
                    Toast.makeText(MainActivity.this, "请授予外部文件夹访问权限", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    Log.i(TAG, "获取文件管理权限 package: "+ getPackageName());
                    intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                    startActivity(intent);
                } else{
                    Log.i(TAG, "已完成外部文件夹访问授权");
                }

                // 通过 service 来启动程序
                Intent si = new Intent(MainActivity.this, RunService.class);
                // startService(si);
                startForegroundService(si);

                // 按钮被点击时执行的代码
                Toast.makeText(MainActivity.this, "完成运行程序", Toast.LENGTH_SHORT).show();
            }
        });

        // 测试按钮
        btnCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        callLocalApi(API_IPFS);
                        callLocalApi(API_Cluster);
                        callLocalApi(API_EFamily);
//                        callLocalApi(API_AndroidAddr);
                    }
                }).start();
            }
        });
    }

    // 访问api
    private void callLocalApi(String apiUrl){
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
            String responseBody = response.body().string();
            Log.i(TAG, "请求返回结果: "+responseBody);
            // ... 进一步处理或解析响应 ...
        } catch (IOException e) {
            Log.e(TAG, "请求api" + apiUrl + "出错：", e);
        }
    }
    private void doSU() {
        try {
            // 获取 root 权限
            Process process = Runtime.getRuntime().exec("su");// (这里执行是系统已经开放了root权限，而不是说通过执行这句来获得root权限)
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("exit\n");
            os.flush();
        } catch (IOException e) {
            Log.e(TAG, "获取su权限出错：", e);
        }
    }
}