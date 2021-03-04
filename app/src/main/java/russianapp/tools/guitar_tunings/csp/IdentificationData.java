package russianapp.tools.guitar_tunings.csp;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import russianapp.tools.guitar_tunings.BuildConfig;

public class IdentificationData {

    public static Map<String, String> getAppInfo(Context context) {
        Map<String, String> map = new HashMap<>();

        map.put("versionName", BuildConfig.VERSION_NAME);
        map.put("packageName", BuildConfig.APPLICATION_ID);
        map.put("appVersion", String.valueOf(BuildConfig.VERSION_CODE));

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        String firstInstallTime;
        if (context != null) {
            firstInstallTime = getFirstInstallTimeAsString(context, dateFormat);
            if (!TextUtils.isEmpty(firstInstallTime))
                map.put("installedOn", firstInstallTime);
        }

        String lastUpdateTime;
        if (context != null) {
            lastUpdateTime = getLastUpdateTimeAsString(context, dateFormat);
            if (!TextUtils.isEmpty(lastUpdateTime))
                map.put("installedOn", lastUpdateTime);
        }

        SimpleDateFormat moscowTime = new SimpleDateFormat("HH", Locale.UK);
        moscowTime.setTimeZone(TimeZone.getTimeZone("GMT+3"));
        map.put("serverHour", moscowTime.format(new Date()));

        return map;
    }

    public static Map<String, String> getDeviceInfo() {
        Map<String, String> map = new HashMap<>();

        if (OpenUDID_manager.isInitialized())
            map.put("deviceId", OpenUDID_manager.getOpenUDID());
        else
            map.put("deviceId", "none");

        map.put("sdk", Build.VERSION.SDK);
        map.put("release", Build.VERSION.RELEASE);
        map.put("brand", Build.BRAND);
        map.put("device", Build.DEVICE);
        map.put("model", Build.MODEL);
        map.put("manufacturer", Build.MANUFACTURER);
        map.put("product", Build.PRODUCT);
        map.put("board", Build.BOARD);
        map.put("display", Build.DISPLAY);
        map.put("hardware", Build.HARDWARE);
        map.put("host", Build.HOST);
        map.put("id", Build.ID);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            map.put("abis", Arrays.toString(Build.SUPPORTED_ABIS));
        }

        return map;
    }

    private static String getFirstInstallTimeAsString(Context context, DateFormat dateFormat) {
        long firstInstallTime;
        try {
            firstInstallTime = context
                    .getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0)
                    .firstInstallTime;
            return dateFormat.format(new Date(firstInstallTime));
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    private static String getLastUpdateTimeAsString(Context context, DateFormat dateFormat) {
        long lastUpdateTime;
        try {
            lastUpdateTime = context
                    .getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0)
                    .lastUpdateTime;
            return dateFormat.format(new Date(lastUpdateTime));
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    public static String paramsToString(Map<String, String> map) {

        StringBuilder requestLink = new StringBuilder();
        for (Map.Entry<String, String> entry : map.entrySet())
            if (!entry.getKey().contains("serverHour"))
                requestLink.append("&").append(entry.getKey()).append("=").append(entry.getValue());

        return requestLink.toString();
    }
}