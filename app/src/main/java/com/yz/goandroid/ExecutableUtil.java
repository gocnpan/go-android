package com.yz.goandroid;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ExecutableUtil {

    private static final String TAG = "ExecutableUtil";
    private static final String EXECUTABLE_NAME = "android.x86_64"; // 替换为你的可执行文件名
    private static final String TMP_ROOT = "/data/local/tmp"; // 可改变权限的tmp dir
//    private static final String EXECUTABLE_ROOT = "/data/local/tmp/yz"; // 目录
    private static String EXECUTABLE_PATH = TMP_ROOT + "/" + EXECUTABLE_NAME; // 执行程序

    private static String executable_data_dir = ""; // 执行程序数据目录

    public static void extractAndRunExecutable(Context context) {
        try {
            executable_data_dir = context.getDataDir().getAbsolutePath() +"/data";
            // 创建目录
            makeDir(context);
            File data = new File(executable_data_dir);
            if (!data.exists()){
                Log.i(TAG,"data不存在: "+executable_data_dir);
                return;
            } else{
                Log.i(TAG, "已创建程序data目录");
            }

            // 复制assets目录下的可执行文件到设备的内部存储
            copyExecutableFromAssetsToInternalStorage(context);
            File outputFile = new File(EXECUTABLE_PATH);
            if (!outputFile.exists()){
                Log.i(TAG, "文件不存在");
                return;
            }

            // 使可执行文件具有执行权限
            makeExecutable(EXECUTABLE_PATH);


            // 执行可执行文件
            executeCommand(EXECUTABLE_PATH);

        } catch (IOException | InterruptedException e) {
            Toast.makeText(context, "出现错误: `" + e, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error executing the executable", e);
        }
    }

    // 复制assets目录下的可执行文件到设备的内部存储
    private static void copyExecutableFromAssetsToInternalStorage(Context context) throws IOException {
        // 检查文件是否已经存在
        File outputFile = new File(EXECUTABLE_PATH);
        if (!outputFile.exists()) {
            Log.i(TAG, "开始复制文件");
            // 从assets复制到内部存储
            InputStream fis = context.getAssets().open(EXECUTABLE_NAME);
            // FileInputStream fis = new FileInputStream();
            FileOutputStream fos = new FileOutputStream(outputFile);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            fos.flush();
            fos.close();
            fis.close();
            Log.i(TAG,"可执行文件复制完成: " + EXECUTABLE_PATH);
        } else{
            Log.i(TAG, "文件已存在");
        }
    }

    // 执行权限
    private static void makeExecutable(String path) throws IOException {
        Log.i(TAG, "开始授权");
        // 设置可执行权限
        String command = "chmod 777 " + path;
        DataOutputStream os = new DataOutputStream(Runtime.getRuntime().exec("sh").getOutputStream());
        os.writeBytes(command);
        os.flush();
        os.close();
        Log.i(TAG, "授予权限: "+path);
    }

    // 创建文件夹
    private static void makeDir(Context context){
        File data = new File(executable_data_dir);
        if (!data.exists()){
            Log.i(TAG, "创建: "+ executable_data_dir);
            data.mkdir();
        }
    }

    // 执行
    private static void executeCommand(String command) throws IOException, InterruptedException {
//        ProcessBuilder processBuilder = new ProcessBuilder(command);
//        Process process = processBuilder.start();
//        final  Map<String, String> env = processBuilder.environment();
//        env.put("AP_NDK_BASE_DIR", executable_data_dir);
//
//        // 读取命令的输出
//        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//        String line;
//        while ((line = reader.readLine()) != null) {
//            Log.i(TAG, line);
//        }

        Log.i(TAG, "command: " + command);
        String[] env = {"AP_NDK_BASE_DIR=" +executable_data_dir};

        Process process = Runtime.getRuntime().exec(command, env);
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        String line;
        while ((line = reader.readLine()) != null) {
            Log.i(TAG, line);
        }

        int exitCode = process.waitFor();
        if (exitCode == 0) {
            Log.i(TAG, "Success");
        } else {
            Log.e(TAG, "Failed with exit code " + exitCode);
        }
    }
}
