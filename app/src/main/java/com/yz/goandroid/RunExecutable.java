package com.yz.goandroid;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

public class RunExecutable {

    private static final String TAG = "RunExecutable";

    public static void extractAndRunExecutable(Context context, String roorDir, String dataDir, String executableName, Map<String, String> envs) {
        try {
            String executablePath = roorDir + "/" + executableName;

            // 复制assets目录下的可执行文件到设备的内部存储, 配置执行权限
            copyExecutableFromAssetsToInternalStorage(context, executablePath, executableName);

            // 执行可执行文件
            executeCommand(executablePath, envs);

        } catch (IOException | InterruptedException e) {
            Log.e(TAG, "执行[ "+executableName+" ]出现错误", e);
            Toast.makeText(context, "执行[ "+executableName+" ]出现错误: " + e, Toast.LENGTH_SHORT).show();
        }
    }

    // 复制assets目录下的可执行文件到设备的内部存储
    public static void copyExecutableFromAssetsToInternalStorage(Context context, String executablePath, String executableName) throws IOException {
        // 检查文件是否已经存在
        File outputFile = new File(executablePath);
        if (!outputFile.exists()) {
            Log.i(TAG, "开始复制文件");
            // 从assets复制到内部存储
            InputStream fis = context.getAssets().open(executableName);
            
            FileOutputStream fos = new FileOutputStream(outputFile);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            fos.flush();
            fos.close();
            fis.close();
            Log.i(TAG,"可执行文件复制完成: " + executablePath);

            // 使可执行文件具有执行权限
            CommandExecution.execCommand("chmod 755 " + executablePath, false);
    
            Log.i(TAG, "授予执行权限: "+executablePath);
        } else{
            Log.i(TAG, "文件已存在");
        }
    }

    // 执行
    public static void executeCommand(String command, Map<String, String> envs) throws IOException, InterruptedException {
        Log.i(TAG, "command: " + command);

        // 创建ProcessBuilder实例
        ProcessBuilder processBuilder = new ProcessBuilder();
        // 设置要执行的命令和参数
        processBuilder.command("sh", "-c", command);

        // 通过environment方法获取环境变量的Map
        Map<String, String> env = processBuilder.environment();
        env.putAll(envs);

        Process process = processBuilder.start();

//        Process process = Runtime.getRuntime().exec(command, envs);

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
}
