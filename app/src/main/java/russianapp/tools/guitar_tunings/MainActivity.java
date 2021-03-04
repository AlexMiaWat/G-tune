package russianapp.tools.guitar_tunings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Locale;

import russianapp.tools.guitar_tunings.audio.CaptureThread;
import russianapp.tools.guitar_tunings.components.Global;
import russianapp.tools.guitar_tunings.csp.CentralServicePortalManager;
import russianapp.tools.guitar_tunings.csp.OpenUDID_manager;
import russianapp.tools.guitar_tunings.graphics.DialView;

import static russianapp.tools.guitar_tunings.R.id.adView;

public class MainActivity extends Activity {
    private DialView dial;
    private TextView topBar, tuner_txt, aim, hz;
    private double targetFrequency;
    private CaptureThread mCapture;
    private ImageButton language;
    String lang;
    ArrayList<String> locations;
    AdView mAdView;
    AdRequest adRequest;

    public boolean isItLastAppVersion = true;
    public boolean firstSessionMessage = true;
    public String token = "";
    public CentralServicePortalManager cspMng;

    int idMenuSelected = 2131230892;

    //Sounds sounds;
    //PerfectTune perfectTune;
//    private PlaySound mPlaySound;

    Activity main;

    public Global globalVariable;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // нет заголовка
        setContentView(R.layout.main);

        // Sounds play
        //sounds = new Sounds();
        //perfectTune = new PerfectTune();

        main = this;

        // Set global variables
        globalVariable = (Global) getApplicationContext();
        globalVariable.mainActivity = this;

        dial = findViewById(R.id.dial);
        topBar = findViewById(R.id.textView1);
        tuner_txt = findViewById(R.id.tuning_text);

        // admob
        MobileAds.initialize(this, initializationStatus -> {
        });
        mAdView = findViewById(adView);
        adRequest = new AdRequest.Builder().build();

//        List<String> testDeviceIds = Collections.singletonList("6DE5FDE9C640128401A5C097587D9909");
//        RequestConfiguration configuration = new RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build();
//        MobileAds.setRequestConfiguration(configuration);

        // На передний план:
        FrameLayout bar = findViewById(R.id.bar);
        try{if (bar != null) {
            bar.bringToFront();
        }}catch (NullPointerException e) {Log.d("PTuneActivity", "bar bringToFront called.");}

        aim = findViewById(R.id.aim);
        try{if (aim != null) {
            aim.bringToFront();
        }
        } catch (NullPointerException e) {
            Log.d("PTuneActivity", "aim bringToFront called.");
        }

        hz = findViewById(R.id.Hz);
        try {
            if (hz != null) {
                hz.bringToFront();
            }
        } catch (NullPointerException e) {
            Log.d("PTuneActivity", "bar bringToFront called.");
        }
        // Все эти элементы <----

        //Запускаем измерение частоты звука:

        // Работаем с языками программы:
        languageSettings();

        // Класический строй
        e_std_Clicked();

        TargetFrequencyStart();

        // All components:

        // device id
        OpenUDID_manager.sync(this);

        // Central Service Portal
        try {
            cspMng = new CentralServicePortalManager(this);
            cspMng.mainActivity = this;
            cspMng.doServiceTask("firstConnection", "First connection");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Работаем с языками программы:
    final void languageSettings() {

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        locations = new ArrayList<>();
        locations.add("en");
        locations.add("es");
        locations.add("ru");

        // Текущий язык системы
        lang = Locale.getDefault().getLanguage();

        if (lang.equals("") || !locations.contains(lang))
            lang = "en";

        // Создаем обработчик нажатия смены языка
        language = findViewById(R.id.language);
        View.OnClickListener oclBtnOk = v -> {
            for (int i = 0; i < locations.size(); i++) {
                if (locations.get(i).equals(lang) && i < locations.size() - 1) {
                    setLocate(locations.get(i + 1));
                    lang = locations.get(i + 1);

                    break;
                } else if (locations.get(i).equals(lang) && i == locations.size() - 1) {
                    setLocate(locations.get(0));
                    lang = locations.get(0);

                    break;
                }
            }
        };

        try {
            if (language != null) {
                language.setOnClickListener(oclBtnOk);
                setLocate(lang);
            }
        } catch (Exception e) {
            Log.println(Log.ERROR, "errrr:", e.getMessage());
        }

    }

    public void setLocate(String languageToLoad) {

        cspMng.doServiceTask("languageSelected", "Selected: " + languageToLoad);

        // Flag
        language.setImageResource(this.getResources().getIdentifier("drawable/" + languageToLoad + "_", null, this.getPackageName()));

        // Get Locale from string "en", "ru"
        Locale locale = new Locale(languageToLoad.toLowerCase());
        Locale.setDefault(locale);

        // Change locale settings in the Resources:
        Resources res = getBaseContext().getResources();
        DisplayMetrics dm = res.getDisplayMetrics();

        // Apply to config
        android.content.res.Configuration config = res.getConfiguration();
        config.locale = locale;

        // Use conf.locale = new Locale(...) if targeting lower versions
        res.updateConfiguration(config, dm);

        // Обновляем меню
        this.invalidateOptionsMenu();

        selectMenu();
    }

    @Override
    protected void onDestroy() {
    	super.onDestroy();
        android.os.Process.killProcess(android.os.Process.myPid());

    	if (mCapture != null) {
    		mCapture.setRunning(false);
    		mCapture = null;
    	}
    	
    	Log.d("PTuneActivity", "onDestroy called.");
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
        android.os.Process.killProcess(android.os.Process.myPid());

    	mCapture.setRunning(false);
    	
    	Log.d("PTuneActivity", "onPause called.");
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	updateTargetFrequency(); // Get radio button selection
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @SuppressLint("SetTextI18n")
    private void e_std_Clicked(){
        RadioButton rb;
        rb = findViewById(R.id.radio0);
        rb.setText(getString(R.string._1st) + (" e"));
        rb.setTag("329.628");
        rb = findViewById(R.id.radio1);
        rb.setText(getString(R.string._2nd) + " B");
        rb.setTag("246.942");
        rb = findViewById(R.id.radio2);
        rb.setText(getString(R.string._3rd) + " G");
        rb.setTag("195.998");
        rb = findViewById(R.id.radio3);
        rb.setText(getString(R.string._4th) + " D");
        rb.setTag("146.832");
        rb = findViewById(R.id.radio4);
        rb.setText(getString(R.string._5th) + " A");
        rb.setTag("110.000");
        rb = findViewById(R.id.radio5);
        rb.setText(getString(R.string._6th) + " E");
        rb.setTag("82.4069");

        updateTargetFrequency();

        tuner_txt.setText(R.string.e_std);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        RadioGroup rg = findViewById(R.id.radioGroup1);
        int selected = rg.getCheckedRadioButtonId();
        RadioButton rb = findViewById(selected);
        int position = rg.indexOfChild(rb);

        cspMng.doServiceTask("tuneProperties", "Selected: " + item.getTitle() + " : " + (int) (position + 1));
        idMenuSelected = item.getItemId();

        return selectMenu();
    }

    @SuppressLint({"NonConstantResourceId", "SetTextI18n"})
    private boolean selectMenu() {
        RadioButton rb;
        switch (idMenuSelected) {
            case R.id.e_std:
                e_std_Clicked();
                return true;

            case R.id.b_std:
                rb = findViewById(R.id.radio0);
                rb.setText(getString(R.string._1st) + " B");
                rb.setTag("246.942");
                rb = findViewById(R.id.radio1);
                rb.setText(getString(R.string._2nd) + " G");
                rb.setTag("185.000");
                rb = findViewById(R.id.radio2);
                rb.setText(getString(R.string._3rd) + " D");
                rb.setTag("147.832");
                rb = findViewById(R.id.radio3);
                rb.setText(getString(R.string._4th) + " A");
                rb.setTag("110.000");
                rb = findViewById(R.id.radio4);
                rb.setText(getString(R.string._5th) + " E");
                rb.setTag("82.4069");
                rb = findViewById(R.id.radio5);
                rb.setText(getString(R.string._6th) + " B");
                rb.setTag("61.7354");
                updateTargetFrequency();
                tuner_txt.setText(R.string.b_std);
                return true;

            case R.id.open_a_std:
                rb = findViewById(R.id.radio0);
                rb.setText(getString(R.string._1st) + " e");
                rb.setTag("329.628");

                rb = findViewById(R.id.radio1);
                rb.setText(getString(R.string._2nd) + " C#");
                rb.setTag("277.200");

                rb = findViewById(R.id.radio2);
                rb.setText(getString(R.string._3rd) + " A");
                rb.setTag("219.998");

                rb = findViewById(R.id.radio3);
                rb.setText(getString(R.string._4th) + " E");
                rb.setTag("164.800");

                rb = findViewById(R.id.radio4);
                rb.setText(getString(R.string._5th) + " A");
                rb.setTag("110.000");

                rb = findViewById(R.id.radio5);
                rb.setText(getString(R.string._6th) + " E");
                rb.setTag("82.410");

                updateTargetFrequency();
                tuner_txt.setText(R.string.open_a_std);
                return true;

            case R.id.open_c_std:
                rb = findViewById(R.id.radio0);
                rb.setText(getString(R.string._1st) + " E4");
                rb.setTag("329.628");

                rb = findViewById(R.id.radio1);
                rb.setText(getString(R.string._2nd) + " C");
                rb.setTag("261.629"); //вверх 0,5 до До С

                rb = findViewById(R.id.radio2);
                rb.setText(getString(R.string._3rd) + " G3");
                rb.setTag("195.998");

                rb = findViewById(R.id.radio3);
                rb.setText(getString(R.string._4th) + " C");
                rb.setTag("130.819");// вниз 1 до До С

                rb = findViewById(R.id.radio4);
                rb.setText(getString(R.string._5th) + " G");
                rb.setTag("98.000");// вниз 1 до Соль G

                rb = findViewById(R.id.radio5);
                rb.setText(getString(R.string._6th) + " C");
                rb.setTag("65.410"); //вниз 2 до До С

                updateTargetFrequency();
                tuner_txt.setText(R.string.open_c_std);
                return true;

            case R.id.open_c6_std: // Открытый C "Led Zeppelin"
                rb = findViewById(R.id.radio0);
                rb.setText(getString(R.string._1st) + " e");
                rb.setTag("329.628");

                rb = findViewById(R.id.radio1);
                rb.setText(getString(R.string._2nd) + " C");
                rb.setTag("261.629"); //вверх 0,5 до До С

                rb = findViewById(R.id.radio2);
                rb.setText(getString(R.string._3rd) + " G");
                rb.setTag("195.998");

                rb = findViewById(R.id.radio3);
                rb.setText(getString(R.string._4th) + " C");
                rb.setTag("130.819");// вниз 1 до До С

                rb = findViewById(R.id.radio4);
                rb.setText(getString(R.string._5th) + " A");
                rb.setTag("110.000");

                rb = findViewById(R.id.radio5);
                rb.setText(getString(R.string._6th) + " C");
                rb.setTag("65.410"); //вниз 2 до До С

                updateTargetFrequency();
                tuner_txt.setText(R.string.open_c6_std);
                return true;

            case R.id.open_d_std: // Открытый D "Dylan"
                rb = findViewById(R.id.radio0);
                rb.setText(getString(R.string._1st) + " D");
                rb.setTag("293.660");

                rb = findViewById(R.id.radio1);
                rb.setText(getString(R.string._2nd) + " A");
                rb.setTag("220.000");

                rb = findViewById(R.id.radio2);
                rb.setText(getString(R.string._3rd) + " F#");
                rb.setTag("185.000");

                rb = findViewById(R.id.radio3);
                rb.setText(getString(R.string._4th) + " D");
                rb.setTag("147.830");

                rb = findViewById(R.id.radio4);
                rb.setText(getString(R.string._5th) + " A");
                rb.setTag("110.000");

                rb = findViewById(R.id.radio5);
                rb.setText(getString(R.string._6th) + " D");
                rb.setTag("73.910");

                updateTargetFrequency();
                tuner_txt.setText(R.string.open_d_std);
                return true;

            case R.id.open_d6_std:
                rb = findViewById(R.id.radio0);
                rb.setText(getString(R.string._1st) + " D");
                rb.setTag("293.660");

                rb = findViewById(R.id.radio1);
                rb.setText(getString(R.string._2nd) + " B");
                rb.setTag("246.900");

                rb = findViewById(R.id.radio2);
                rb.setText(getString(R.string._3rd) + " F#");
                rb.setTag("185.000");

                rb = findViewById(R.id.radio3);
                rb.setText(getString(R.string._4th) + " D");
                rb.setTag("146.830");

                rb = findViewById(R.id.radio4);
                rb.setText(getString(R.string._5th) + " A");
                rb.setTag("110.000");

                rb = findViewById(R.id.radio5);
                rb.setText(getString(R.string._6th) + " D");
                rb.setTag("73.420");

                updateTargetFrequency();
                tuner_txt.setText(R.string.open_d6_std);
                return true;

            case R.id.open_e_std:
                rb = findViewById(R.id.radio0);
                rb.setText(getString(R.string._1st) + " e");
                rb.setTag("329.600");

                rb = findViewById(R.id.radio1);
                rb.setText(getString(R.string._2nd) + " B");
                rb.setTag("246.900");

                rb = findViewById(R.id.radio2);
                rb.setText(getString(R.string._3rd) + " G#");
                rb.setTag("207.700");

                rb = findViewById(R.id.radio3);
                rb.setText(getString(R.string._4th) + " E");
                rb.setTag("164.800");

                rb = findViewById(R.id.radio4);
                rb.setText(getString(R.string._5th) + " B");
                rb.setTag("123.500");

                rb = findViewById(R.id.radio5);
                rb.setText(getString(R.string._6th) + " E");
                rb.setTag("82.410");

                updateTargetFrequency();
                tuner_txt.setText(R.string.open_e_std);
                return true;

            case R.id.open_g_std:
                rb = findViewById(R.id.radio0);
                rb.setText(getString(R.string._1st) + " D");
                rb.setTag("293.660");

                rb = findViewById(R.id.radio1);
                rb.setText(getString(R.string._2nd) + " B");
                rb.setTag("246.940");

                rb = findViewById(R.id.radio2);
                rb.setText(getString(R.string._3rd) + " G");
                rb.setTag("196.000");

                rb = findViewById(R.id.radio3);
                rb.setText(getString(R.string._4th) + " D");
                rb.setTag("146.830");

                rb = findViewById(R.id.radio4);
                rb.setText(getString(R.string._5th) + " G");
                rb.setTag("98.000");

                rb = findViewById(R.id.radio5);
                rb.setText(getString(R.string._6th) + " D");
                rb.setTag("73.420");

                updateTargetFrequency();
                tuner_txt.setText(R.string.open_g_std);
                return true;

            case R.id.drop_d_std:
                rb = findViewById(R.id.radio0);
                rb.setText(getString(R.string._1st) + " E");
                rb.setTag("329.628");
                rb = findViewById(R.id.radio1);
                rb.setText(getString(R.string._2nd) + " B");
                rb.setTag("246.942");
                rb = findViewById(R.id.radio2);
                rb.setText(getString(R.string._3rd) + " G");
                rb.setTag("195.998");
                rb = findViewById(R.id.radio3);
                rb.setText(getString(R.string._4th) + " D");
                rb.setTag("146.832");
                rb = findViewById(R.id.radio4);
                rb.setText(getString(R.string._5th) + " A");
                rb.setTag("110.000");
                rb = findViewById(R.id.radio5);
                rb.setText(getString(R.string._6th) + " D");
                rb.setTag("73.910");
                updateTargetFrequency();
                tuner_txt.setText(R.string.drop_d_std);
                return true;

            case R.id.drop_c_std:
                rb = findViewById(R.id.radio0);
                rb.setText(getString(R.string._1st) + " D");
                rb.setTag("293.700");
                rb = findViewById(R.id.radio1);
                rb.setText(getString(R.string._2nd) + " A");
                rb.setTag("220.000");
                rb = findViewById(R.id.radio2);
                rb.setText(getString(R.string._3rd) + " F");
                rb.setTag("174.610");
                rb = findViewById(R.id.radio3);
                rb.setText(getString(R.string._4th) + " C");
                rb.setTag("130.810");
                rb = findViewById(R.id.radio4);
                rb.setText(getString(R.string._5th) + " G");
                rb.setTag("98.000");
                rb = findViewById(R.id.radio5);
                rb.setText(getString(R.string._6th) + " C");
                rb.setTag("65.410");
                updateTargetFrequency();
                tuner_txt.setText(R.string.drop_c_std);
                return true;

            case R.id.low_c_std: // Celtic
                rb = findViewById(R.id.radio0);
                rb.setText(getString(R.string._1st) + " D");
                rb.setTag("293.700");
                rb = findViewById(R.id.radio1);
                rb.setText(getString(R.string._2nd) + " A");
                rb.setTag("220.000");
                rb = findViewById(R.id.radio2);
                rb.setText(getString(R.string._3rd) + " G");
                rb.setTag("196.000");

                rb = findViewById(R.id.radio3);
                rb.setText(getString(R.string._4th) + " D");
                rb.setTag("146.800");
                rb = findViewById(R.id.radio4);
                rb.setText(getString(R.string._5th) + " G");
                rb.setTag("98.000");
                rb = findViewById(R.id.radio5);
                rb.setText(getString(R.string._6th) + " C");
                rb.setTag("65.410");
                updateTargetFrequency();
                tuner_txt.setText(R.string.low_c_std);
                return true;

            case R.id.double_drop_d_std:
                rb = findViewById(R.id.radio0);
                rb.setText(getString(R.string._1st) + " D");
                rb.setTag("293.650");
                rb = findViewById(R.id.radio1);
                rb.setText(getString(R.string._2nd) + " B");
                rb.setTag("246.942");
                rb = findViewById(R.id.radio2);
                rb.setText(getString(R.string._3rd) + " G");
                rb.setTag("196.000");
                rb = findViewById(R.id.radio3);
                rb.setText(getString(R.string._4th) + " F");
                rb.setTag("147.830");
                rb = findViewById(R.id.radio4);
                rb.setText(getString(R.string._5th) + " A");
                rb.setTag("110.000");
                rb = findViewById(R.id.radio5);
                rb.setText(getString(R.string._6th) + " D");
                rb.setTag("73.910");
                updateTargetFrequency();
                tuner_txt.setText(R.string.double_drop_d_std);
                return true;

            case R.id.cross_a:
                rb = findViewById(R.id.radio0);
                rb.setText(getString(R.string._1st) + " a");
                rb.setTag("440.000");
                rb = findViewById(R.id.radio1);
                rb.setText(getString(R.string._2nd) + " e");
                rb.setTag("329.630");
                rb = findViewById(R.id.radio2);
                rb.setText(getString(R.string._3rd) + " a");
                rb.setTag("220.000");
                rb = findViewById(R.id.radio3);
                rb.setText(getString(R.string._4th) + " e");
                rb.setTag("164.810");
                rb = findViewById(R.id.radio4);
                rb.setText(getString(R.string._5th) + " A");
                rb.setTag("110.000");
                rb = findViewById(R.id.radio5);
                rb.setText(getString(R.string._6th) + " E");
                rb.setTag("82.407");
                updateTargetFrequency();
                tuner_txt.setText(R.string.cross_a);
                return true;

            case R.id.bass:
                rb = findViewById(R.id.radio0);
                rb.setText(getString(R.string._1st) + " C");
                rb.setTag("130.810");
                rb = findViewById(R.id.radio1);
                rb.setText(getString(R.string._2nd) + " G");
                rb.setTag("98.000");
                rb = findViewById(R.id.radio2);
                rb.setText(getString(R.string._3rd) + " D");
                rb.setTag("73.420");
                rb = findViewById(R.id.radio3);
                rb.setText(getString(R.string._4th) + " A");
                rb.setTag("55.000");
                rb = findViewById(R.id.radio4);
                rb.setText(getString(R.string._5th) + " E");
                rb.setTag("41.200");
                rb = findViewById(R.id.radio5);
                rb.setText(getString(R.string._6th) + " B");
                rb.setTag("30.870");
                updateTargetFrequency();
                tuner_txt.setText(R.string.bass);
                return true;
            default:
                return true;
        }
    }

    private void TargetFrequencyStart() {
        updateTargetFrequency(); // Get radio button selection

        @SuppressLint("HandlerLeak") Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message m) {
                updateDisplay(m.getData().getFloat("Freq"));
            }
        };

        mCapture = new CaptureThread(mHandler);
        mCapture.setRunning(true);
        mCapture.start();
    }

    @SuppressLint("DefaultLocale")
    private void updateTargetFrequency() {
        // Grab the selected radio button tag.
        RadioGroup rg = findViewById(R.id.radioGroup1);
        int selected = rg.getCheckedRadioButtonId();
        RadioButton rb = findViewById(selected);

        // Update TextView
        targetFrequency = Float.parseFloat((String)rb.getTag());
        if (targetFrequency < 1000f)
            aim.setText(String.format("%.1f Hz", targetFrequency));
        else
            aim.setText(String.format("%.2f kHz", targetFrequency/1000));
    }

    @SuppressLint("DefaultLocale")
    public void updateDisplay(double frequency) {
        // Calculate difference between target and measured frequency,
        // given that the measured frequency can be a factor of target.
        double difference;
        if (frequency > targetFrequency) {
            int divisions = (int) (frequency / targetFrequency);
            double modified = targetFrequency * (double) divisions;
            if (frequency - modified > targetFrequency / 2) {
                modified += targetFrequency;
                divisions++;
            }
            difference = (frequency - modified) / (double) divisions;
        } else {
            // If target is greater than measured, just use difference.
            difference = frequency - targetFrequency;
        }

        double relativeFrequency = targetFrequency + difference;

        // Update TextView
        if (relativeFrequency < 1000f)
            topBar.setText(String.format("%.1f Hz", relativeFrequency));
        else
            topBar.setText(String.format("%.2f kHz", relativeFrequency / 1000));

        // Update DialView
        double value = difference / (targetFrequency / 2) * 90;
        dial.update(value, 0, 0);
    }

    @SuppressLint("DefaultLocale")
    public void onRadioButtonClicked(View v) {
        // Perform action on clicks
        RadioButton rb = (RadioButton) v;

        hz.setText(rb.getText());

        RadioGroup radioGroup = findViewById(R.id.radioGroup1);
        int position = radioGroup.indexOfChild(rb);

        // Update TextView
        targetFrequency = Float.parseFloat((String)rb.getTag());
        //perfectTune.setTuneFreq(targetFrequency);
        if (targetFrequency < 1000f)
            aim.setText(String.format("%.1f Hz", targetFrequency));
        else
            aim.setText(String.format("%.2f kHz", targetFrequency/1000));

        //start the tune
//        if (mPlaySound != null) {
////            mPlaySound.stop();
////            mPlaySound = null;
//            mPlaySound.mOutputFreq = targetFrequency;
//        } else {
//            mPlaySound = new PlaySound();
//            mPlaySound.mOutputFreq = targetFrequency;
//            mPlaySound.start();
//        }

        // ad mob
        if (mAdView != null)
            try {
                if ((position == 2) || (position == 3) || (position == 5)) {
                    mAdView.loadAd(adRequest);
                    mAdView.setVisibility(View.VISIBLE);

                } else {
                    mAdView.destroy();
                    mAdView.setVisibility(View.GONE);

                    View parentLayout = findViewById(android.R.id.content);
                    if (!isItLastAppVersion && firstSessionMessage)
                        Snackbar.make(parentLayout, getResources().getString(R.string.update_to_latest_version), Snackbar.LENGTH_INDEFINITE)
                                .setAction(getResources().getString(R.string.Update_now), view -> {
                                            CentralServicePortalManager.getLastPackage(main, BuildConfig.APPLICATION_ID);
                                            firstSessionMessage = false;
                                        }
                                ).show();
                }
            } catch (Exception ignored) {
            }


        cspMng.doServiceTask("tuneString", "Selected: " + tuner_txt.getText() + " : " + (int) (position + 1));
    }

    public void onMenuClicked(View v) {
        openOptionsMenu();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {

            // Build an AlertDialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            // Set a title for alert dialog
            builder.setTitle(getResources().getString(R.string.exit));

            // Ask the final question
            builder.setMessage(getResources().getString(R.string.exit_frase));

            // Set click listener for alert dialog buttons
            DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        // User clicked the Yes button

                        finish();
                        ActivityCompat.finishAffinity(main);
                        System.exit(0);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        // User clicked the No button
                        break;
                }
            };

            // Set the alert dialog yes button click listener
            builder.setPositiveButton("Yes", dialogClickListener);

            // Set the alert dialog no button click listener
            builder.setNegativeButton("No",dialogClickListener);

            //builder.setView(image);

            AlertDialog dialog = builder.create();
            // Display the alert dialog on interface
            dialog.show();

        }
        return true;
    }

    @Override
    public String toString() {

        StringBuilder result = new StringBuilder();

        result.append("MAIN ACTIVITY INFO: " + "\n");
        result.append("Current lang: ").append(lang).append("\n");
        result.append("Current targetFrequency: ").append(targetFrequency).append("\n");
        if (hz != null)
            result.append("Current rb: ").append(hz.getText()).append("\n");

        return result.toString();
    }
}