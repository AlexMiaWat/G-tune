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
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.CompoundButton;
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

import org.billthefarmer.mididriver.GeneralMidiConstants;
import org.billthefarmer.mididriver.MidiConstants;
import org.billthefarmer.mididriver.MidiDriver;
import org.billthefarmer.mididriver.ReverbConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import russianapp.tools.guitar_tunings.audio.CaptureThread;
import russianapp.tools.guitar_tunings.components.Global;
import russianapp.tools.guitar_tunings.csp.CentralServicePortalManager;
import russianapp.tools.guitar_tunings.csp.OpenUDID_manager;
import russianapp.tools.guitar_tunings.graphics.DialView;

import static russianapp.tools.guitar_tunings.R.id.adView;

public class MainActivity extends Activity
        implements View.OnTouchListener, View.OnClickListener,
        CompoundButton.OnCheckedChangeListener,
        MidiDriver.OnMidiStartListener {

    private DialView dial;
    private TextView topBar, tuner_txt, aim, hz;
    private float targetFrequency;
    private CaptureThread mCapture;
    Handler mHandler;
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

    // Sounds sounds;
    protected MidiDriver midi;
    ImageButton recBtn, playBtn;
    Map<Float, Integer> map;

    Activity main;

    public Global globalVariable;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // нет заголовка
        setContentView(R.layout.main);

        // admob
        MobileAds.initialize(this, initializationStatus -> {
        });
        mAdView = findViewById(adView);
        adRequest = new AdRequest.Builder().build();

        // Create midi driver
        midi = MidiDriver.getInstance(this);

        map = new HashMap<>();
        map.put(30.87f, 23);     // B0

        map.put(41.20f, 28);     // E1
        map.put(55.00f, 33);     // A1
        map.put(61.74f, 35);     // B1

        map.put(65.41f, 36);     // C2
        map.put(73.42f, 38);     // D2
        map.put(82.41f, 40);     // E2
        map.put(98.00f, 43);     // G2
        map.put(110.00f, 45);    // A2
        map.put(123.47f, 47);    // B2

        map.put(130.81f, 48);    // C3
        map.put(146.83f, 50);    // D3
        map.put(164.81f, 52);    // E3
        map.put(174.61f, 53);    // F3
        map.put(185.00f, 54);    // F#3
        map.put(196.00f, 55);    // G3
        map.put(207.65f, 56);    // G#3
        map.put(220.00f, 57);    // A3
        map.put(246.94f, 59);    // B3

        map.put(261.63f, 60);    // C4
        map.put(277.18f, 61);    // C#4
        map.put(293.66f, 62);    // D4
        map.put(329.63f, 64);    // E4
        map.put(440.00f, 69);    // A4

        // rec and play btn
        playBtn = findViewById(R.id.playBtn);
        recBtn = findViewById(R.id.recBtn);

        if (recBtn != null && playBtn != null) {
            recBtn.setSelected(true);

            recBtn.setOnClickListener(button -> {
                button.setSelected(true);
                playBtn.setSelected(false);
                if (mCapture == null)
                    TargetFrequencyStart();
            });

            playBtn.setOnClickListener(button -> {
                int note = getNote();
                sendMidi(MidiConstants.NOTE_ON, note, 63);
                button.setSelected(true);
                recBtn.setSelected(false);
                if (mCapture != null) {
                    mCapture.setRunning(false);
                    mCapture = null;
                }
            });
        }

        // Global
        main = this;

        // Set global variables
        globalVariable = (Global) getApplicationContext();
        globalVariable.mainActivity = this;

        dial = findViewById(R.id.dial);
        topBar = findViewById(R.id.textView1);
        tuner_txt = findViewById(R.id.tuning_text);

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

        try {
            cspMng.doServiceTask("languageSelected", "Selected: " + languageToLoad);
        } catch (Exception ignored) {
        }

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

        // Stop midi
        if (midi != null)
            midi.stop();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        //android.os.Process.killProcess(android.os.Process.myPid());

        if (mCapture != null) {
            mCapture.setRunning(false);
            mCapture = null;
        }

        // Stop midi
        if (midi != null)
            midi.stop();
    }
    
    @Override
    protected void onResume() {
        super.onResume();

        updateTargetFrequency(); // Get radio button selection

        if (mCapture == null)
            TargetFrequencyStart();

        // Start midi
        if (midi != null)
            midi.start();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @SuppressLint("SetTextI18n")
    private void e_std_Clicked() {
        RadioButton rb;
        rb = findViewById(R.id.radio0);
        rb.setText(getString(R.string._1st) + " E4");
        rb.setTag("329.63");
        rb = findViewById(R.id.radio1);
        rb.setText(getString(R.string._2nd) + " B3");
        rb.setTag("246.94");
        rb = findViewById(R.id.radio2);
        rb.setText(getString(R.string._3rd) + " G3");
        rb.setTag("196.00");
        rb = findViewById(R.id.radio3);
        rb.setText(getString(R.string._4th) + " D3");
        rb.setTag("146.83");
        rb = findViewById(R.id.radio4);
        rb.setText(getString(R.string._5th) + " A2");
        rb.setTag("110.00");
        rb = findViewById(R.id.radio5);
        rb.setText(getString(R.string._6th) + " E2");
        rb.setTag("82.41");

        updateTargetFrequency();

        tuner_txt.setText(R.string.e_std);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        RadioGroup rg = findViewById(R.id.radioGroup1);
        int selected = rg.getCheckedRadioButtonId();
        RadioButton rb = findViewById(selected);
        int position = rg.indexOfChild(rb);

        try {
            cspMng.doServiceTask("tuneProperties", "Selected: " + item.getTitle() + " : " + (position + 1));
        } catch (Exception ignored) {
        }

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
                rb.setText(getString(R.string._1st) + " B3");
                rb.setTag("246.94");
                rb = findViewById(R.id.radio1);
                rb.setText(getString(R.string._2nd) + " F#3");
                rb.setTag("185.00");
                rb = findViewById(R.id.radio2);
                rb.setText(getString(R.string._3rd) + " D3");
                rb.setTag("146.83");
                rb = findViewById(R.id.radio3);
                rb.setText(getString(R.string._4th) + " A2");
                rb.setTag("110.00");
                rb = findViewById(R.id.radio4);
                rb.setText(getString(R.string._5th) + " E2");
                rb.setTag("82.41");
                rb = findViewById(R.id.radio5);
                rb.setText(getString(R.string._6th) + " B1");
                rb.setTag("61.74");
                updateTargetFrequency();
                tuner_txt.setText(R.string.b_std);
                return true;

            case R.id.open_a_std:
                rb = findViewById(R.id.radio0);
                rb.setText(getString(R.string._1st) + " E4");
                rb.setTag("329.63");

                rb = findViewById(R.id.radio1);
                rb.setText(getString(R.string._2nd) + " C#4");
                rb.setTag("277.18");

                rb = findViewById(R.id.radio2);
                rb.setText(getString(R.string._3rd) + " A3");
                rb.setTag("220.00");

                rb = findViewById(R.id.radio3);
                rb.setText(getString(R.string._4th) + " E3");
                rb.setTag("164.81");

                rb = findViewById(R.id.radio4);
                rb.setText(getString(R.string._5th) + " A2");
                rb.setTag("110.00");

                rb = findViewById(R.id.radio5);
                rb.setText(getString(R.string._6th) + " E2");
                rb.setTag("82.41");

                updateTargetFrequency();
                tuner_txt.setText(R.string.open_a_std);
                return true;

            case R.id.open_c_std:
                rb = findViewById(R.id.radio0);
                rb.setText(getString(R.string._1st) + " E4");
                rb.setTag("329.63");

                rb = findViewById(R.id.radio1);
                rb.setText(getString(R.string._2nd) + " C4");
                rb.setTag("261.63"); //вверх 0,5 до До С

                rb = findViewById(R.id.radio2);
                rb.setText(getString(R.string._3rd) + " G3");
                rb.setTag("196.00");

                rb = findViewById(R.id.radio3);
                rb.setText(getString(R.string._4th) + " C3");
                rb.setTag("130.81");// вниз 1 до До С

                rb = findViewById(R.id.radio4);
                rb.setText(getString(R.string._5th) + " G2");
                rb.setTag("98.00");// вниз 1 до Соль G

                rb = findViewById(R.id.radio5);
                rb.setText(getString(R.string._6th) + " C2");
                rb.setTag("65.41"); //вниз 2 до До С

                updateTargetFrequency();
                tuner_txt.setText(R.string.open_c_std);
                return true;

            case R.id.open_c6_std: // Открытый C "Led Zeppelin"
                rb = findViewById(R.id.radio0);
                rb.setText(getString(R.string._1st) + " E4");
                rb.setTag("329.63");

                rb = findViewById(R.id.radio1);
                rb.setText(getString(R.string._2nd) + " C4");
                rb.setTag("261.63"); //вверх 0,5 до До С

                rb = findViewById(R.id.radio2);
                rb.setText(getString(R.string._3rd) + " G3");
                rb.setTag("196.00");

                rb = findViewById(R.id.radio3);
                rb.setText(getString(R.string._4th) + " C3");
                rb.setTag("130.81");// вниз 1 до До С

                rb = findViewById(R.id.radio4);
                rb.setText(getString(R.string._5th) + " A2");
                rb.setTag("110.00");

                rb = findViewById(R.id.radio5);
                rb.setText(getString(R.string._6th) + " C2");
                rb.setTag("65.41"); //вниз 2 до До С

                updateTargetFrequency();
                tuner_txt.setText(R.string.open_c6_std);
                return true;

            case R.id.open_d_std: // Открытый D "Dylan"
                rb = findViewById(R.id.radio0);
                rb.setText(getString(R.string._1st) + " D4");
                rb.setTag("293.66");

                rb = findViewById(R.id.radio1);
                rb.setText(getString(R.string._2nd) + " A3");
                rb.setTag("220.00");

                rb = findViewById(R.id.radio2);
                rb.setText(getString(R.string._3rd) + " F#3");
                rb.setTag("185.00");

                rb = findViewById(R.id.radio3);
                rb.setText(getString(R.string._4th) + " D3");
                rb.setTag("146.83");

                rb = findViewById(R.id.radio4);
                rb.setText(getString(R.string._5th) + " A2");
                rb.setTag("110.00");

                rb = findViewById(R.id.radio5);
                rb.setText(getString(R.string._6th) + " D2");
                rb.setTag("73.42");

                updateTargetFrequency();
                tuner_txt.setText(R.string.open_d_std);
                return true;

            case R.id.open_d6_std:
                rb = findViewById(R.id.radio0);
                rb.setText(getString(R.string._1st) + " D4");
                rb.setTag("293.66");

                rb = findViewById(R.id.radio1);
                rb.setText(getString(R.string._2nd) + " B3");
                rb.setTag("246.94");

                rb = findViewById(R.id.radio2);
                rb.setText(getString(R.string._3rd) + " F#3");
                rb.setTag("185.00");

                rb = findViewById(R.id.radio3);
                rb.setText(getString(R.string._4th) + " D3");
                rb.setTag("146.83");

                rb = findViewById(R.id.radio4);
                rb.setText(getString(R.string._5th) + " A2");
                rb.setTag("110.00");

                rb = findViewById(R.id.radio5);
                rb.setText(getString(R.string._6th) + " D2");
                rb.setTag("73.42");

                updateTargetFrequency();
                tuner_txt.setText(R.string.open_d6_std);
                return true;

            case R.id.open_e_std:
                rb = findViewById(R.id.radio0);
                rb.setText(getString(R.string._1st) + " E4");
                rb.setTag("329.63");

                rb = findViewById(R.id.radio1);
                rb.setText(getString(R.string._2nd) + " B3");
                rb.setTag("246.94");

                rb = findViewById(R.id.radio2);
                rb.setText(getString(R.string._3rd) + " G#3");
                rb.setTag("207.65");

                rb = findViewById(R.id.radio3);
                rb.setText(getString(R.string._4th) + " E3");
                rb.setTag("164.81");

                rb = findViewById(R.id.radio4);
                rb.setText(getString(R.string._5th) + " B2");
                rb.setTag("123.47");

                rb = findViewById(R.id.radio5);
                rb.setText(getString(R.string._6th) + " E2");
                rb.setTag("82.41");

                updateTargetFrequency();
                tuner_txt.setText(R.string.open_e_std);
                return true;

            case R.id.open_g_std:
                rb = findViewById(R.id.radio0);
                rb.setText(getString(R.string._1st) + " D4");
                rb.setTag("293.66");

                rb = findViewById(R.id.radio1);
                rb.setText(getString(R.string._2nd) + " B3");
                rb.setTag("246.94");

                rb = findViewById(R.id.radio2);
                rb.setText(getString(R.string._3rd) + " G3");
                rb.setTag("196.00");

                rb = findViewById(R.id.radio3);
                rb.setText(getString(R.string._4th) + " D3");
                rb.setTag("146.83");

                rb = findViewById(R.id.radio4);
                rb.setText(getString(R.string._5th) + " G2");
                rb.setTag("98.00");

                rb = findViewById(R.id.radio5);
                rb.setText(getString(R.string._6th) + " D2");
                rb.setTag("73.42");

                updateTargetFrequency();
                tuner_txt.setText(R.string.open_g_std);
                return true;

            case R.id.drop_d_std:
                rb = findViewById(R.id.radio0);
                rb.setText(getString(R.string._1st) + " E4");
                rb.setTag("329.63");
                rb = findViewById(R.id.radio1);
                rb.setText(getString(R.string._2nd) + " B3");
                rb.setTag("246.94");
                rb = findViewById(R.id.radio2);
                rb.setText(getString(R.string._3rd) + " G3");
                rb.setTag("196.00");
                rb = findViewById(R.id.radio3);
                rb.setText(getString(R.string._4th) + " D3");
                rb.setTag("146.83");
                rb = findViewById(R.id.radio4);
                rb.setText(getString(R.string._5th) + " A2");
                rb.setTag("110.00");
                rb = findViewById(R.id.radio5);
                rb.setText(getString(R.string._6th) + " D2");
                rb.setTag("73.42");
                updateTargetFrequency();
                tuner_txt.setText(R.string.drop_d_std);
                return true;

            case R.id.drop_c_std:
                rb = findViewById(R.id.radio0);
                rb.setText(getString(R.string._1st) + " D4");
                rb.setTag("293.66");
                rb = findViewById(R.id.radio1);
                rb.setText(getString(R.string._2nd) + " A3");
                rb.setTag("220.00");
                rb = findViewById(R.id.radio2);
                rb.setText(getString(R.string._3rd) + " F3");
                rb.setTag("174.61");
                rb = findViewById(R.id.radio3);
                rb.setText(getString(R.string._4th) + " C3");
                rb.setTag("130.81");
                rb = findViewById(R.id.radio4);
                rb.setText(getString(R.string._5th) + " G2");
                rb.setTag("98.00");
                rb = findViewById(R.id.radio5);
                rb.setText(getString(R.string._6th) + " C2");
                rb.setTag("65.41");
                updateTargetFrequency();
                tuner_txt.setText(R.string.drop_c_std);
                return true;

            case R.id.low_c_std: // Celtic
                rb = findViewById(R.id.radio0);
                rb.setText(getString(R.string._1st) + " D4");
                rb.setTag("293.66");
                rb = findViewById(R.id.radio1);
                rb.setText(getString(R.string._2nd) + " A3");
                rb.setTag("220.00");
                rb = findViewById(R.id.radio2);
                rb.setText(getString(R.string._3rd) + " G3");
                rb.setTag("196.00");
                rb = findViewById(R.id.radio3);
                rb.setText(getString(R.string._4th) + " D3");
                rb.setTag("146.83");
                rb = findViewById(R.id.radio4);
                rb.setText(getString(R.string._5th) + " G2");
                rb.setTag("98.00");
                rb = findViewById(R.id.radio5);
                rb.setText(getString(R.string._6th) + " C2");
                rb.setTag("65.41");
                updateTargetFrequency();
                tuner_txt.setText(R.string.low_c_std);
                return true;

            case R.id.double_drop_d_std:
                rb = findViewById(R.id.radio0);
                rb.setText(getString(R.string._1st) + " D4");
                rb.setTag("293.66");
                rb = findViewById(R.id.radio1);
                rb.setText(getString(R.string._2nd) + " B3");
                rb.setTag("246.94");
                rb = findViewById(R.id.radio2);
                rb.setText(getString(R.string._3rd) + " G3");
                rb.setTag("196.00");
                rb = findViewById(R.id.radio3);
                rb.setText(getString(R.string._4th) + " F3");
                rb.setTag("174.61");
                rb = findViewById(R.id.radio4);
                rb.setText(getString(R.string._5th) + " A2");
                rb.setTag("110.00");
                rb = findViewById(R.id.radio5);
                rb.setText(getString(R.string._6th) + " D2");
                rb.setTag("73.42");
                updateTargetFrequency();
                tuner_txt.setText(R.string.double_drop_d_std);
                return true;

            case R.id.cross_a:
                rb = findViewById(R.id.radio0);
                rb.setText(getString(R.string._1st) + " A4");
                rb.setTag("440.00");
                rb = findViewById(R.id.radio1);
                rb.setText(getString(R.string._2nd) + " E4");
                rb.setTag("329.63");
                rb = findViewById(R.id.radio2);
                rb.setText(getString(R.string._3rd) + " A3");
                rb.setTag("220.00");
                rb = findViewById(R.id.radio3);
                rb.setText(getString(R.string._4th) + " E3");
                rb.setTag("164.81");
                rb = findViewById(R.id.radio4);
                rb.setText(getString(R.string._5th) + " A2");
                rb.setTag("110.00");
                rb = findViewById(R.id.radio5);
                rb.setText(getString(R.string._6th) + " E2");
                rb.setTag("82.41");
                updateTargetFrequency();
                tuner_txt.setText(R.string.cross_a);
                return true;

            case R.id.bass:
                rb = findViewById(R.id.radio0);
                rb.setText(getString(R.string._1st) + " C3");
                rb.setTag("130.81");
                rb = findViewById(R.id.radio1);
                rb.setText(getString(R.string._2nd) + " G2");
                rb.setTag("98.00");
                rb = findViewById(R.id.radio2);
                rb.setText(getString(R.string._3rd) + " D2");
                rb.setTag("73.42");
                rb = findViewById(R.id.radio3);
                rb.setText(getString(R.string._4th) + " A1");
                rb.setTag("55.00");
                rb = findViewById(R.id.radio4);
                rb.setText(getString(R.string._5th) + " E1");
                rb.setTag("41.20");
                rb = findViewById(R.id.radio5);
                rb.setText(getString(R.string._6th) + " B0");
                rb.setTag("30.87");
                updateTargetFrequency();
                tuner_txt.setText(R.string.bass);
                return true;
            default:
                return true;
        }
    }

    @SuppressLint("HandlerLeak")
    private void TargetFrequencyStart() {
        updateTargetFrequency(); // Get radio button selection

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message m) {
                if (mCapture != null)
                    updateDisplay(m.getData().getFloat("Freq"));
                else {
                    mHandler.removeCallbacksAndMessages(null);
                    mHandler = null;
                }
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

        // ad mob
        if (mAdView != null)
            try {
                if (position > 1) {
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

        try {
            cspMng.doServiceTask("tuneString", "Selected: " + tuner_txt.getText() + " : " + (int) (position + 1));
        } catch (Exception ignored) {
        }

        if (playBtn.isSelected()) {
            int note = getNote();
            sendMidi(MidiConstants.NOTE_ON, note, 63);
        }
    }

    public void onMenuClicked(View v) {
        openOptionsMenu();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
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
        return super.onKeyLongPress(keyCode, event);
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

    @Override
    public void onClick(View v) {

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return false;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

    }

    // Listener for sending initial midi messages when the Sonivox
    // synthesizer has been started, such as program change.
    @Override
    public void onMidiStart() {
        // Program change - harpsichord
        sendMidi(MidiConstants.PROGRAM_CHANGE, GeneralMidiConstants.HARPSICHORD);

        midi.setReverb(ReverbConstants.CHAMBER);
        // midi.setReverb(ReverbConstants.OFF);
    }

    // Send a midi message, 2 bytes
    protected void sendMidi(int m, int n) {
        byte[] msg = new byte[2];

        msg[0] = (byte) m;
        msg[1] = (byte) n;

        midi.write(msg);
    }

    // Send a midi message, 3 bytes
    protected void sendMidi(int m, int n, int v) {
        byte[] msg = new byte[3];

        msg[0] = (byte) m;
        msg[1] = (byte) n;
        msg[2] = (byte) v;

        midi.write(msg);
    }

    protected int getNote() {

        try {
            return map.get(targetFrequency);
        } catch (Exception ignored) {
        }

        return 0;
    }
}