package russianapp.tools.guitar_tunings.components;

import android.annotation.SuppressLint;
import android.os.AsyncTask;

public class AsyncMethods {

    public int Circle = 0;

    public void newWave() {
        Circle += 1;
    }

    @SafeVarargs
    @SuppressLint("NewApi")
    public static <P, T extends AsyncTask<P, ?, ?>> void execute(T task, Boolean parallel, P... params) {
        if (parallel) {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
        } else {
            task.execute(params);
        }
    }
}