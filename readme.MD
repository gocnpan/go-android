# 怎么测试

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