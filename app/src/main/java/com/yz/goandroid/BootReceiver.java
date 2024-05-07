package com.yz.goandroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // 启动我们的应用
//            Intent launchIntent = new Intent(context, MainActivity.class);
//            launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            context.startActivity(launchIntent);

            // 启动 api 服务
            // 通过 service 来启动程序
            Intent si = new Intent(context, APIService.class);
            // startService(si);
            context.startForegroundService(si);
        }
    }
}
