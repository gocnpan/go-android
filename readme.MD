# 怎么测试

## 文件夹准备

需要创建`/data/yz`文件夹
```shell
adb shell

# 以 super user 身份运行
su

# 创建 /data/yz
mkdir /data/yz

# 赋予程序可执行权限
chmod 777 -R /data/yz
```

## root相关

当前项目使用 [genymotion](https://www.genymotion.com/) 虚拟机，该Android虚拟机已经Root

该虚拟机不需要Magisk来管理应用的Root权限，因为应用本身可以申请Root权限(仅限已Root的系统)

判断设备是否Root
```java
    // 判断是否具有ROOT权限
    public static boolean isRoot() {
        boolean res = false;
        try {
            res = (new File("/system/bin/su").exists()) ||
                    (new File("/system/xbin/su").exists());
        } catch (Exception e) {
            Log.e(TAG, "判断是否是root时出错: ", e);
        }
        return res;
    }
```

获取Root权限代码
```java
        try {
            //progress_dialog = ProgressDialog.show(context,"ROOT", "正在获取ROOT权限...", true, false);
            Runtime.getRuntime().exec("su");
        } catch (Exception e){
            Log.e(TAG, "获取ROOT权限时出错: ", e);
            Toast.makeText(context, "获取ROOT权限时出错!", Toast.LENGTH_LONG).show();
        }
```

# 将应用配置为系统应用

需要确保手机已经`root`, 通过`adb`连接手机(usb 或 wifi)

准备APK和so文件：首先，确保你的应用APK已经构建完成。如果你的应用包含so库，需要准备对应目标系统平台的so文件。

卸载旧版本：如果之前已经安装过该应用，需要先卸载。可以通过手机自带的软件管理卸载，或者使用adb命令进行卸载：
```bat
adb uninstall <your package name>
```
其中 <your package name> 是你的应用包名。

推送APK和so文件：使用adb命令将APK文件推送到系统的 `/priv-app` 或 `/app` 目录，对于Android 4.4及以上版本，通常是 `/priv-app`。如果包含so库，也需要推送到 `/system/lib` 或 `/system/lib64` 目录下：
```bat
adb root
adb remount
adb push <your apk path> /system/priv-app/
```
如果包含so库，还需要：
```bat
adb push <your so path> /system/lib/ 或 /system/lib64/
```

修改权限：确保APK和so文件的权限正确设置，通常APK的权限是644。

重启设备：为了使更改生效，需要重启设备：
```bat
adb reboot
```

验证安装：重启后，使用adb命令来验证应用是否已经安装为系统应用：
```bat
adb shell pm list packages
```

搜索你的应用包名，如果看到，说明安装成功。
