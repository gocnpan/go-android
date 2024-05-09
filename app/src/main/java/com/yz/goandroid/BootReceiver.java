package com.yz.goandroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // 启动 api 服务
            // 通过 service 来启动程序
            Intent si = new Intent(context, RunService.class);
            // startService(si);
            context.startForegroundService(si);
        }
    }
}
