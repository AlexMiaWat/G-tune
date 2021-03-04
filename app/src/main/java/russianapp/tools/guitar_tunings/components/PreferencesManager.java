package russianapp.tools.guitar_tunings.components;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class PreferencesManager {

    public static final String CACHE = "share";

    public static final String ANIMATION_ON = "animation_on";
    public static final String TRAFFIC_SAVE_ON = "traffic_save";
    public static final String RANDOM_PICTURE_ON = "random_picture";
    public static final String ADS_OFF = "ads_off";
    public static final String APP_VERSION = "APP_VERSION";

    public static String getStringPreference(Context context, String key, String defValue) {
        return context.getSharedPreferences(CACHE, Context.MODE_PRIVATE).getString(key, defValue);
    }

    public static void setStringPreference(Context context, String key, String value) {
        context.getSharedPreferences(CACHE, Context.MODE_PRIVATE)
                .edit()
                .putString(key, value)
                .apply();
    }

    public static boolean getBooleanPreference(Context context, String key, boolean defValue) {
        return context.getSharedPreferences(CACHE, Context.MODE_PRIVATE).getBoolean(key, defValue);
    }

    public static void setBooleanPreference(Context context, String key, boolean value) {
        context.getSharedPreferences(CACHE, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(key, value)
                .apply();
    }

    public static void restartApp(Context context) {

        try {
            TimeUnit.MILLISECONDS.sleep(400);
        } catch (InterruptedException ignored) {
        }

        Global global = (Global) context.getApplicationContext();
        Intent intentMain = global.mainActivity.getIntent();
        ((Activity) context).finish();
        global.mainActivity.finish();
        context.startActivity(intentMain);
    }

    public boolean deleteFiles_Cache(Context context) {

        Global global = (Global) context.getApplicationContext();
        try {
            File cacheDirectory = global.mainActivity.getCacheDir();
            if (cacheDirectory != null && cacheDirectory.getParent() != null) {
                File applicationDirectory = new File(cacheDirectory.getParent());
                deleteDir(applicationDirectory);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            boolean success = true;

            String[] children = dir.list();
            if (children != null)
                for (String f : children) {
                    if (f.contains(".db") || f.contains("google")) {
                        success = false;
                        continue;
                    }

                    if (!deleteDir(new File(dir, f)))
                        success = false;
                }

            if (success)
                return dir.delete();
            else
                return true;

        } else if (dir != null && dir.isFile())
            return dir.delete();
        else
            return false;
    }
}