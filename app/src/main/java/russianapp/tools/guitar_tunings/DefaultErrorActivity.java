package russianapp.tools.guitar_tunings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import components.UCEHandler;
import components.xMail;

/**
 * Created by Rohit.
 */
public final class DefaultErrorActivity extends Activity {
    static final String EXTRA_STACK_TRACE = "EXTRA_STACK_TRACE";
    static final String EXTRA_MAIN_ACTIVITY_INFO = "EXTRA_MAIN_ACTIVITY_INFO";
    static final String EXTRA_ITEM_INFO = "EXTRA_ITEM_INFO";
    static final String EXTRA_ACTIVITY_LOG = "EXTRA_ACTIVITY_LOG";
    private File txtFile;
    private String strCurrentErrorLog;
    private String strCurrentStackLog;

    Activity activity;

    // Native passwords:
    static {
        System.loadLibrary("keys");
    }

    public native String getSMTPAUTHUSER();

    public native String getSMTPAUTHPWD();

    public native String getEMAILFROM();

    public native String getEMAILTO();

    public static void startApp(Activity activity) {
        try {
            Intent intent = new Intent(activity, StartActivity.class);
            intent.putExtra("crash", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK
                    | Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(Global.getInstance().getBaseContext(), 0, intent, PendingIntent.FLAG_ONE_SHOT);

            AlarmManager mgr = (AlarmManager) Global.getInstance().getBaseContext().getSystemService(Context.ALARM_SERVICE);
            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 250, pendingIntent);

            activity.finish();
            System.exit(2);

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public String getApplicationName(Context context) {
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        int stringId = applicationInfo.labelRes;
        return stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() : context.getString(stringId);
    }

    public static String getVersionName(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private String getActivityLogFromIntent(Intent intent) {
        return intent.getStringExtra(EXTRA_ACTIVITY_LOG);
    }

    private String getStackTraceFromIntent(Intent intent) {
        return intent.getStringExtra(EXTRA_STACK_TRACE);
    }

    @SuppressLint("PrivateResource")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(android.R.style.Theme_Holo_Light_DarkActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.default_error_activity);

        activity = this;

        // Сразу отправить ошибку по почте
        emailErrorLog();

        findViewById(R.id.button_close_app).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                DefaultErrorActivity.this.finish();
//                android.os.Process.killProcess(android.os.Process.myPid());
//                System.exit(10);

                startApp(activity);
            }
        });
        findViewById(R.id.button_copy_error_log).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyErrorToClipboard();
            }
        });
        findViewById(R.id.button_share_error_log).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareErrorLog();
            }
        });
        findViewById(R.id.button_save_error_log).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveErrorLogToFile(true);
            }
        });
        findViewById(R.id.button_email_error_log).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //emailErrorLog();
                emailErrorLogUser();
            }
        });
        findViewById(R.id.button_view_error_log).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog dialog = new AlertDialog.Builder(DefaultErrorActivity.this)
                        .setTitle("Error Log")
                        .setMessage(getAllErrorDetailsFromIntent(DefaultErrorActivity.this, getIntent()))
                        .setPositiveButton("Copy Log & Close",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        copyErrorToClipboard();
                                        dialog.dismiss();
                                    }
                                })
                        .setNeutralButton("Close",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                        .show();
                TextView textView = dialog.findViewById(android.R.id.message);
                if (textView != null) {
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                }
            }
        });
    }

    private void emailErrorLogUser() {
        saveErrorLogToFile(false);
        String errorLog = getAllErrorDetailsFromIntent(DefaultErrorActivity.this, getIntent());

        //  Отправка сообщения по почте
        String subject = strCurrentStackLog;
        String[] parts = strCurrentStackLog.split("Caused by: ");
        if (parts.length > 1)
            if (parts[1].split("\n").length > 0)
                subject = parts[1].split("\n")[0];
            else if (parts[0].split("\n").length > 0)
                subject = parts[0].split("\n")[0];

        String emailAddress = new String(Base64.decode(getEMAILTO(), Base64.DEFAULT));
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("plain/text");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, emailAddress);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "GTR err: " + getVersionName(DefaultErrorActivity.this) + ": " + subject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, errorLog);
        if (txtFile.exists()) {
            Uri filePath = FileProvider.getUriForFile(this, this.getApplicationContext().getPackageName() + ".provider", txtFile);
            emailIntent.putExtra(Intent.EXTRA_STREAM, filePath);
        }
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(emailIntent, "Email Error Log"));
    }

    private void saveErrorLogToFile(boolean isShowToast) {
        Boolean isSDPresent = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        if (isSDPresent && isExternalStorageWritable()) {
            Date currentDate = new Date();
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            String strCurrentDate = dateFormat.format(currentDate);
            strCurrentDate = strCurrentDate.replace(" ", "_");
            String errorLogFileName = getApplicationName(DefaultErrorActivity.this) + "_Error-Log_" + strCurrentDate;
            String errorLog = getAllErrorDetailsFromIntent(DefaultErrorActivity.this, getIntent());
            String fullPath = Environment.getExternalStorageDirectory() + "/AppErrorLogs_UCEH/";
            FileOutputStream outputStream;
            try {
                File file = new File(fullPath);
                file.mkdir();
                txtFile = new File(fullPath + errorLogFileName + ".txt");
                txtFile.createNewFile();
                outputStream = new FileOutputStream(txtFile);
                outputStream.write(errorLog.getBytes());
                outputStream.close();
                if (txtFile.exists() && isShowToast) {
                    Toast.makeText(this, "File Saved Successfully", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                Log.e("REQUIRED", "This app does not have write storage permission to save log file.");
                if (isShowToast) {
                    Toast.makeText(this, "Storage Permission Not Found", Toast.LENGTH_SHORT).show();
                }
                e.printStackTrace();
            }
        }
    }

    private void shareErrorLog() {
        String errorLog = getAllErrorDetailsFromIntent(DefaultErrorActivity.this, getIntent());
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        share.putExtra(Intent.EXTRA_SUBJECT, "Application Crash Error Log");
        share.putExtra(Intent.EXTRA_TEXT, errorLog);
        startActivity(Intent.createChooser(share, "Share Error Log"));
    }

    private void copyErrorToClipboard() {
        String errorInformation = getAllErrorDetailsFromIntent(DefaultErrorActivity.this, getIntent());
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("View Error Log", errorInformation);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(DefaultErrorActivity.this, "Error Log Copied", Toast.LENGTH_SHORT).show();
        }
    }

    private String getAllErrorDetailsFromIntent(Context context, Intent intent) {
        if (TextUtils.isEmpty(strCurrentErrorLog)) {
            String LINE_SEPARATOR = "\n";
            StringBuilder errorReport = new StringBuilder();

            // APP INFO
            String versionName = getVersionName(context);
            errorReport.append("Ver: ");
            errorReport.append(versionName);
            errorReport.append(";");
            errorReport.append(LINE_SEPARATOR);
            errorReport.append("SDK: ");
            errorReport.append(Build.VERSION.SDK);
            errorReport.append(";");
            errorReport.append(LINE_SEPARATOR);
            errorReport.append("Release: ");
            errorReport.append(Build.VERSION.RELEASE);
            errorReport.append(";");
            errorReport.append(LINE_SEPARATOR);
            errorReport.append("Brand: ");
            errorReport.append(Build.BRAND);
            errorReport.append(";");
            errorReport.append(LINE_SEPARATOR);
            errorReport.append("Device: ");
            errorReport.append(Build.DEVICE);
            errorReport.append(";");
            errorReport.append(LINE_SEPARATOR);
            errorReport.append("Model: ");
            errorReport.append(Build.MODEL);
            errorReport.append(";");
            errorReport.append(LINE_SEPARATOR);
            errorReport.append("Manufacturer: ");
            errorReport.append(Build.MANUFACTURER);
            errorReport.append(";");
            errorReport.append(LINE_SEPARATOR);
            errorReport.append("Product: ");
            errorReport.append(Build.PRODUCT);
            errorReport.append(";");
            errorReport.append(LINE_SEPARATOR);

            // Date
            errorReport.append("\nDATE:\n");
            Date currentDate = new Date();
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            String firstInstallTime = getFirstInstallTimeAsString(context, dateFormat);
            if (!TextUtils.isEmpty(firstInstallTime)) {
                errorReport.append("Installed On: ");
                errorReport.append(firstInstallTime);
                errorReport.append(LINE_SEPARATOR);
            }
            String lastUpdateTime = getLastUpdateTimeAsString(context, dateFormat);
            if (!TextUtils.isEmpty(lastUpdateTime)) {
                errorReport.append("Updated On: ");
                errorReport.append(lastUpdateTime);
                errorReport.append(LINE_SEPARATOR);
            }
            errorReport.append("Current Date: ");
            errorReport.append(dateFormat.format(currentDate));
            errorReport.append(LINE_SEPARATOR);

            // Stack
            errorReport.append("\nERROR LOG:\n");
            strCurrentStackLog = getStackTraceFromIntent(intent);
            errorReport.append(strCurrentStackLog);
            errorReport.append(LINE_SEPARATOR);
            String activityLog = getActivityLogFromIntent(intent);
            errorReport.append(LINE_SEPARATOR);

            // MAIN ACTIVITY INFO
            errorReport.append(intent.getStringExtra(EXTRA_MAIN_ACTIVITY_INFO));
            errorReport.append(LINE_SEPARATOR);
//
//            // ITEM INFO
//            errorReport.append(LINE_SEPARATOR);
//            errorReport.append(intent.getStringExtra(EXTRA_ITEM_INFO));
//            errorReport.append(LINE_SEPARATOR);

            // User activities
            if (activityLog != null) {
                errorReport.append("\nUSER ACTIVITIES:\n");
                errorReport.append("User Activities: ");
                errorReport.append(activityLog);
                errorReport.append(LINE_SEPARATOR);
            }

            return errorReport.toString();

        } else
            return strCurrentErrorLog;
    }

    private String getFirstInstallTimeAsString(Context context, DateFormat dateFormat) {
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

    private String getLastUpdateTimeAsString(Context context, DateFormat dateFormat) {
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

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private void emailErrorLog() {

        // Пример кодировки
        //Base64.encodeToString("".getBytes(), Base64.DEFAULT);
        //new String(Base64.decode("base64", Base64.DEFAULT));

        saveErrorLogToFile(false);
        String errorLog = getAllErrorDetailsFromIntent(DefaultErrorActivity.this, getIntent());

        // Initialize UCE_Handler Library
        new UCEHandler.Builder(this)
                .setUCEHEnabled(false)
                .build();

        //  Отправка сообщения по почте
        String subject = strCurrentStackLog;
        String[] parts = strCurrentStackLog.split("Caused by: ");
        if (parts.length > 1)
            if (parts[1].split("\n").length > 0)
                subject = parts[1].split("\n")[0];
            else if (parts[0].split("\n").length > 0)
                subject = parts[0].split("\n")[0];

        xMail xmail = new xMail();
        xmail.SMTP_AUTH_USER = new String(Base64.decode(getSMTPAUTHUSER(), Base64.DEFAULT));
        xmail.SMTP_AUTH_PWD = new String(Base64.decode(getSMTPAUTHPWD(), Base64.DEFAULT));
        xmail.EMAIL_FROM = new String(Base64.decode(getEMAILFROM(), Base64.DEFAULT));
        xmail.EMAIL_TO = new String(Base64.decode(getEMAILTO(), Base64.DEFAULT));

        xmail.initialize("GTR err: " + getVersionName(DefaultErrorActivity.this) + ": " + subject);
        xMail.sendMessageAsync task = new xMail.sendMessageAsync(DefaultErrorActivity.this, xmail.message);
        task.execute(errorLog);

        // Альтернативная почта GMail:
//        try {
//            GMailSender sender = new GMailSender("", "");
//            sender.sendMail("UKA PORTAL 'errors'",
//                    errorLog,
//                    "",
//                    "");
//            GMailSender.sendMessageAsync task = sender.new sendMessageAsync();
//            task.execute();
//
//        } catch (Exception e) {
//            Log.e("SendMail", e.getMessage(), e);
//        }
    }
}