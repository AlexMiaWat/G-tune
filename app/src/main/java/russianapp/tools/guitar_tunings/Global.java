package russianapp.tools.guitar_tunings;

import android.app.Application;
import android.content.Context;
import android.os.StrictMode;

import androidx.multidex.MultiDex;

import components.UCEHandler;

public class Global extends Application {

    public static Global instance;

    public PTuneActivity pTuneActivity;

    public static Global getInstance() {
        return instance;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // Initialize UCE_Handler Library
        new UCEHandler.Builder(this)
                .setTrackActivitiesEnabled(true)
                .setBackgroundModeEnabled(true)
                .build();
    }

    @Override
    public Context getApplicationContext() {
        return super.getApplicationContext();
    }

}