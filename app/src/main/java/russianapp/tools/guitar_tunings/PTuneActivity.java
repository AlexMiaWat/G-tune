package russianapp.tools.guitar_tunings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
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

import java.util.ArrayList;
import java.util.Locale;

import russianapp.tools.guitar_tunings.audio.CaptureThread;
import russianapp.tools.guitar_tunings.graphics.DialView;

import static russianapp.tools.guitar_tunings.R.id.adView;

public class PTuneActivity extends Activity {
    private FrameLayout bar;
	private DialView dial;
	private TextView topbar, tuner_txt, aim, hz;
	private float targetFrequency;
	private CaptureThread mCapture;
	private Handler mHandler;
	private ImageButton language;
    String lang;
    ArrayList<String> locs;
    AdView mAdView;
    AdRequest adRequest;

    Activity main;

    public Global globalVariable;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // нет заголовка
        setContentView(R.layout.main);

        main = this;

        // Set global variables
        globalVariable = (Global) getApplicationContext();
        globalVariable.pTuneActivity = this;

        // err
//        bar.bringToFront();
//       aim.bringToFront();
//        hz.bringToFront();
        // err

        dial = findViewById(R.id.dial);
        topbar = findViewById(R.id.textView1);
        tuner_txt = findViewById(R.id.tuning_text);

        // admob
        mAdView = findViewById(adView);
        adRequest = new AdRequest.Builder().build();

        // Работаем с языками программы:
        languageSetings();

        // На передний план:
        bar = findViewById(R.id.bar);
        try{if (bar != null) {
            bar.bringToFront();
        }}catch (NullPointerException e) {Log.d("PTuneActivity", "bar bringToFront called.");}

        aim = findViewById(R.id.aim);
        try{if (aim != null) {
            aim.bringToFront();
        }}catch (NullPointerException e) {Log.d("PTuneActivity", "aim bringToFront called.");}

        hz = findViewById(R.id.Hz);
        try{if (hz != null) {
            hz.bringToFront();
        }}catch (NullPointerException e) {Log.d("PTuneActivity", "bar bringToFront called.");}
        // Все эти элементы <----

        //Запускаем измерение частоты звука:
        TargetFrequencyStart();
    }

    // Работаем с языками программы:
    final void languageSetings() {

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        Resources r = getResources();
        Configuration c = r.getConfiguration();
        String[] loc = r.getAssets().getLocales();

        locs = new ArrayList<String>();
        locs.add("en");
        locs.add("es");

        String temp = "Hello world";

        for (int i = 0; i < loc.length; i++) {
            c.locale = new Locale(loc[i]);
            Resources res = new Resources(getAssets(), metrics, c);
            String s1 = res.getString(R.string.hello_world);

            c.locale = new Locale("");
            Resources res2 = new Resources(getAssets(), metrics, c);
            String s2 = res2.getString(R.string.hello_world);

            if (!s1.equals(s2) && !temp.equals(s1)) {
                locs.add(loc[i]);
                temp += s1;
            }
        }

        // Текущий язык системы
        lang = Locale.getDefault().getLanguage();

        if (lang == "") {
            lang = "en";
        }

        // Создаем обработчик нажатия смены языка
        language = (ImageButton)findViewById(R.id.language);
        View.OnClickListener oclBtnOk = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i < locs.size(); i++) {
                    if (locs.get(i).equals(lang) && i < locs.size()-1) {
                        setLocate(locs.get(i + 1));
                        lang = locs.get(i + 1);

                        break;
                    }
                    else if (locs.get(i).equals(lang) && i == locs.size()-1){
                        setLocate(locs.get(0));
                        lang = locs.get(0);

                        break;
                    }
                }
            }
        };

        try {
            if (language != null) {
                language.setOnClickListener(oclBtnOk);
                setLocate(lang);
            }
        } catch (NullPointerException e) {}
    }

    public void setLocate(String languageToLoad) {
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

        e_std_Clicked();

        updateTargetFrequency();

        language.setImageResource(this.getResources().getIdentifier("drawable/" + languageToLoad + "_", null, this.getPackageName()));
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

    private void e_std_Clicked(){
        RadioButton rb;
        rb = (RadioButton) findViewById(R.id.radio0);
        rb.setText(getString(R.string._1st) + (" e"));
        rb.setTag("329.628");
        rb = (RadioButton) findViewById(R.id.radio1);
        rb.setText(getString(R.string._2nd) + " B");
        rb.setTag("246.942");
        rb = (RadioButton) findViewById(R.id.radio2);
        rb.setText(getString(R.string._3rd) + " G");
        rb.setTag("195.998");
        rb = (RadioButton) findViewById(R.id.radio3);
        rb.setText(getString(R.string._4th) + " D");
        rb.setTag("146.832");
        rb = (RadioButton) findViewById(R.id.radio4);
        rb.setText(getString(R.string._5th) + " A");
        rb.setTag("110.000");
        rb = (RadioButton) findViewById(R.id.radio5);
        rb.setText(getString(R.string._6th) + " E");
        rb.setTag("82.4069");
        updateTargetFrequency();

        tuner_txt.setText(R.string.e_std);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //curentTune = item;

        RadioButton rb;
        switch (item.getItemId()) {
            case R.id.e_std:
                e_std_Clicked();
                return true;

            case R.id.b_std:
                rb = (RadioButton) findViewById(R.id.radio0);
                rb.setText(getString(R.string._1st) + " B");
                rb.setTag("246.942");
                rb = (RadioButton) findViewById(R.id.radio1);
                rb.setText(getString(R.string._2nd) + " G");
                rb.setTag("185.000");
                rb = (RadioButton) findViewById(R.id.radio2);
                rb.setText(getString(R.string._3rd) + " D");
                rb.setTag("147.832");
                rb = (RadioButton) findViewById(R.id.radio3);
                rb.setText(getString(R.string._4th) + " A");
                rb.setTag("110.000");
                rb = (RadioButton) findViewById(R.id.radio4);
                rb.setText(getString(R.string._5th) + " E");
                rb.setTag("82.4069");
                rb = (RadioButton) findViewById(R.id.radio5);
                rb.setText(getString(R.string._6th) + " B");
                rb.setTag("61.7354");
                updateTargetFrequency();
                tuner_txt.setText(R.string.b_std);
                return true;

            case R.id.open_a_std:
                rb = (RadioButton) findViewById(R.id.radio0);
                rb.setText(getString(R.string._1st) + " e");
                rb.setTag("329.628");

                rb = (RadioButton) findViewById(R.id.radio1);
                rb.setText(getString(R.string._2nd) + " C#");
                rb.setTag("277.200");

                rb = (RadioButton) findViewById(R.id.radio2);
                rb.setText(getString(R.string._3rd) + " A");
                rb.setTag("219.998");

                rb = (RadioButton) findViewById(R.id.radio3);
                rb.setText(getString(R.string._4th) + " E");
                rb.setTag("164.800");

                rb = (RadioButton) findViewById(R.id.radio4);
                rb.setText(getString(R.string._5th) + " A");
                rb.setTag("110.000");

                rb = (RadioButton) findViewById(R.id.radio5);
                rb.setText(getString(R.string._6th) + " E");
                rb.setTag("82.410");

                updateTargetFrequency();
                tuner_txt.setText(R.string.open_a_std);
                return true;

            case R.id.open_c_std:
                rb = (RadioButton) findViewById(R.id.radio0);
                rb.setText(getString(R.string._1st) + " E4");
                rb.setTag("329.628");

                rb = (RadioButton) findViewById(R.id.radio1);
                rb.setText(getString(R.string._2nd) + " C");
                rb.setTag("261.629"); //вверх 0,5 до До С

                rb = (RadioButton) findViewById(R.id.radio2);
                rb.setText(getString(R.string._3rd) + " G3");
                rb.setTag("195.998");

                rb = (RadioButton) findViewById(R.id.radio3);
                rb.setText(getString(R.string._4th) + " C");
                rb.setTag("130.819");// вниз 1 до До С

                rb = (RadioButton) findViewById(R.id.radio4);
                rb.setText(getString(R.string._5th) + " G");
                rb.setTag("98.000");// вниз 1 до Соль G

                rb = (RadioButton) findViewById(R.id.radio5);
                rb.setText(getString(R.string._6th) + " C");
                rb.setTag("65.410"); //вниз 2 до До С

                updateTargetFrequency();
                tuner_txt.setText(R.string.open_c_std);
                return true;

            case R.id.open_c6_std: // Открытый C "Led Zeppelin"
                rb = (RadioButton) findViewById(R.id.radio0);
                rb.setText(getString(R.string._1st) + " e");
                rb.setTag("329.628");

                rb = (RadioButton) findViewById(R.id.radio1);
                rb.setText(getString(R.string._2nd) + " C");
                rb.setTag("261.629"); //вверх 0,5 до До С

                rb = (RadioButton) findViewById(R.id.radio2);
                rb.setText(getString(R.string._3rd) + " G");
                rb.setTag("195.998");

                rb = (RadioButton) findViewById(R.id.radio3);
                rb.setText(getString(R.string._4th) + " C");
                rb.setTag("130.819");// вниз 1 до До С

                rb = (RadioButton) findViewById(R.id.radio4);
                rb.setText(getString(R.string._5th) + " A");
                rb.setTag("110.000");

                rb = (RadioButton) findViewById(R.id.radio5);
                rb.setText(getString(R.string._6th) + " C");
                rb.setTag("65.410"); //вниз 2 до До С

                updateTargetFrequency();
                tuner_txt.setText(R.string.open_c6_std);
                return true;

            case R.id.open_d_std: // Открытый D "Dylan"
                rb = (RadioButton) findViewById(R.id.radio0);
                rb.setText(getString(R.string._1st) + " D");
                rb.setTag("293.660");

                rb = (RadioButton) findViewById(R.id.radio1);
                rb.setText(getString(R.string._2nd) + " A");
                rb.setTag("220.000");

                rb = (RadioButton) findViewById(R.id.radio2);
                rb.setText(getString(R.string._3rd) + " F#");
                rb.setTag("185.000");

                rb = (RadioButton) findViewById(R.id.radio3);
                rb.setText(getString(R.string._4th) + " D");
                rb.setTag("147.830");

                rb = (RadioButton) findViewById(R.id.radio4);
                rb.setText(getString(R.string._5th) + " A");
                rb.setTag("110.000");

                rb = (RadioButton) findViewById(R.id.radio5);
                rb.setText(getString(R.string._6th) + " D");
                rb.setTag("73.910");

                updateTargetFrequency();
                tuner_txt.setText(R.string.open_d_std);
                return true;

            case R.id.open_d6_std:
                rb = (RadioButton) findViewById(R.id.radio0);
                rb.setText(getString(R.string._1st) + " D");
                rb.setTag("293.660");

                rb = (RadioButton) findViewById(R.id.radio1);
                rb.setText(getString(R.string._2nd) + " B");
                rb.setTag("246.900");

                rb = (RadioButton) findViewById(R.id.radio2);
                rb.setText(getString(R.string._3rd) + " F#");
                rb.setTag("185.000");

                rb = (RadioButton) findViewById(R.id.radio3);
                rb.setText(getString(R.string._4th) + " D");
                rb.setTag("146.830");

                rb = (RadioButton) findViewById(R.id.radio4);
                rb.setText(getString(R.string._5th) + " A");
                rb.setTag("110.000");

                rb = (RadioButton) findViewById(R.id.radio5);
                rb.setText(getString(R.string._6th) + " D");
                rb.setTag("73.420");

                updateTargetFrequency();
                tuner_txt.setText(R.string.open_d6_std);
                return true;

            case R.id.open_e_std:
                rb = (RadioButton) findViewById(R.id.radio0);
                rb.setText(getString(R.string._1st) + " e");
                rb.setTag("329.600");

                rb = (RadioButton) findViewById(R.id.radio1);
                rb.setText(getString(R.string._2nd) + " B");
                rb.setTag("246.900");

                rb = (RadioButton) findViewById(R.id.radio2);
                rb.setText(getString(R.string._3rd) + " G#");
                rb.setTag("207.700");

                rb = (RadioButton) findViewById(R.id.radio3);
                rb.setText(getString(R.string._4th) + " E");
                rb.setTag("164.800");

                rb = (RadioButton) findViewById(R.id.radio4);
                rb.setText(getString(R.string._5th) + " B");
                rb.setTag("123.500");

                rb = (RadioButton) findViewById(R.id.radio5);
                rb.setText(getString(R.string._6th) + " E");
                rb.setTag("82.410");

                updateTargetFrequency();
                tuner_txt.setText(R.string.open_e_std);
                return true;

            case R.id.open_g_std:
                rb = (RadioButton) findViewById(R.id.radio0);
                rb.setText(getString(R.string._1st) + " D");
                rb.setTag("293.660");

                rb = (RadioButton) findViewById(R.id.radio1);
                rb.setText(getString(R.string._2nd) + " B");
                rb.setTag("246.940");

                rb = (RadioButton) findViewById(R.id.radio2);
                rb.setText(getString(R.string._3rd) + " G");
                rb.setTag("196.000");

                rb = (RadioButton) findViewById(R.id.radio3);
                rb.setText(getString(R.string._4th) + " D");
                rb.setTag("146.830");

                rb = (RadioButton) findViewById(R.id.radio4);
                rb.setText(getString(R.string._5th) + " G");
                rb.setTag("98.000");

                rb = (RadioButton) findViewById(R.id.radio5);
                rb.setText(getString(R.string._6th) + " D");
                rb.setTag("73.420");

                updateTargetFrequency();
                tuner_txt.setText(R.string.open_g_std);
                return true;

            case R.id.drop_d_std:
                rb = (RadioButton) findViewById(R.id.radio0);
                rb.setText(getString(R.string._1st) + " E");
                rb.setTag("329.628");
                rb = (RadioButton) findViewById(R.id.radio1);
                rb.setText(getString(R.string._2nd) + " B");
                rb.setTag("246.942");
                rb = (RadioButton) findViewById(R.id.radio2);
                rb.setText(getString(R.string._3rd) + " G");
                rb.setTag("195.998");
                rb = (RadioButton) findViewById(R.id.radio3);
                rb.setText(getString(R.string._4th) + " D");
                rb.setTag("146.832");
                rb = (RadioButton) findViewById(R.id.radio4);
                rb.setText(getString(R.string._5th) + " A");
                rb.setTag("110.000");
                rb = (RadioButton) findViewById(R.id.radio5);
                rb.setText(getString(R.string._6th) + " D");
                rb.setTag("73.910");
                updateTargetFrequency();
                tuner_txt.setText(R.string.drop_d_std);
                return true;

            case R.id.drop_c_std:
                rb = (RadioButton) findViewById(R.id.radio0);
                rb.setText(getString(R.string._1st) + " D");
                rb.setTag("293.700");
                rb = (RadioButton) findViewById(R.id.radio1);
                rb.setText(getString(R.string._2nd) + " A");
                rb.setTag("220.000");
                rb = (RadioButton) findViewById(R.id.radio2);
                rb.setText(getString(R.string._3rd) + " F");
                rb.setTag("174.610");
                rb = (RadioButton) findViewById(R.id.radio3);
                rb.setText(getString(R.string._4th) + " C");
                rb.setTag("130.810");
                rb = (RadioButton) findViewById(R.id.radio4);
                rb.setText(getString(R.string._5th) + " G");
                rb.setTag("98.000");
                rb = (RadioButton) findViewById(R.id.radio5);
                rb.setText(getString(R.string._6th) + " C");
                rb.setTag("65.410");
                updateTargetFrequency();
                tuner_txt.setText(R.string.drop_c_std);
                return true;

            case R.id.low_c_std: // Celtic
                rb = (RadioButton) findViewById(R.id.radio0);
                rb.setText(getString(R.string._1st) + " D");
                rb.setTag("293.700");
                rb = (RadioButton) findViewById(R.id.radio1);
                rb.setText(getString(R.string._2nd) + " A");
                rb.setTag("220.000");
                rb = (RadioButton) findViewById(R.id.radio2);
                rb.setText(getString(R.string._3rd) + " G");
                rb.setTag("196.000");

                rb = (RadioButton) findViewById(R.id.radio3);
                rb.setText(getString(R.string._4th) + " D");
                rb.setTag("146.800");
                rb = (RadioButton) findViewById(R.id.radio4);
                rb.setText(getString(R.string._5th) + " G");
                rb.setTag("98.000");
                rb = (RadioButton) findViewById(R.id.radio5);
                rb.setText(getString(R.string._6th) + " C");
                rb.setTag("65.410");
                updateTargetFrequency();
                tuner_txt.setText(R.string.low_c_std);
                return true;

            case R.id.double_drop_d_std:
                rb = (RadioButton) findViewById(R.id.radio0);
                rb.setText(getString(R.string._1st) + " D");
                rb.setTag("293.650");
                rb = (RadioButton) findViewById(R.id.radio1);
                rb.setText(getString(R.string._2nd) + " B");
                rb.setTag("246.942");
                rb = (RadioButton) findViewById(R.id.radio2);
                rb.setText(getString(R.string._3rd) + " G");
                rb.setTag("196.000");
                rb = (RadioButton) findViewById(R.id.radio3);
                rb.setText(getString(R.string._4th) + " F");
                rb.setTag("147.830");
                rb = (RadioButton) findViewById(R.id.radio4);
                rb.setText(getString(R.string._5th) + " A");
                rb.setTag("110.000");
                rb = (RadioButton) findViewById(R.id.radio5);
                rb.setText(getString(R.string._6th) + " D");
                rb.setTag("73.910");
                updateTargetFrequency();
                tuner_txt.setText(R.string.double_drop_d_std);
                return true;

            case R.id.cross_a:
                rb = (RadioButton) findViewById(R.id.radio0);
                rb.setText(getString(R.string._1st) + " a");
                rb.setTag("440.000");
                rb = (RadioButton) findViewById(R.id.radio1);
                rb.setText(getString(R.string._2nd) + " e");
                rb.setTag("329.630");
                rb = (RadioButton) findViewById(R.id.radio2);
                rb.setText(getString(R.string._3rd) + " a");
                rb.setTag("220.000");
                rb = (RadioButton) findViewById(R.id.radio3);
                rb.setText(getString(R.string._4th) + " e");
                rb.setTag("164.810");
                rb = (RadioButton) findViewById(R.id.radio4);
                rb.setText(getString(R.string._5th) + " A");
                rb.setTag("110.000");
                rb = (RadioButton) findViewById(R.id.radio5);
                rb.setText(getString(R.string._6th) + " E");
                rb.setTag("82.407");
                updateTargetFrequency();
                tuner_txt.setText(R.string.cross_a);
                return true;

            case R.id.bass:
                rb = (RadioButton) findViewById(R.id.radio0);
                rb.setText(getString(R.string._1st) + " C");
                rb.setTag("130.810");
                rb = (RadioButton) findViewById(R.id.radio1);
                rb.setText(getString(R.string._2nd) + " G");
                rb.setTag("98.000");
                rb = (RadioButton) findViewById(R.id.radio2);
                rb.setText(getString(R.string._3rd) + " D");
                rb.setTag("73.420");
                rb = (RadioButton) findViewById(R.id.radio3);
                rb.setText(getString(R.string._4th) + " A");
                rb.setTag("55.000");
                rb = (RadioButton) findViewById(R.id.radio4);
                rb.setText(getString(R.string._5th) + " E");
                rb.setTag("41.200");
                rb = (RadioButton) findViewById(R.id.radio5);
                rb.setText(getString(R.string._6th) + " B");
                rb.setTag("30.870");
                updateTargetFrequency();
                tuner_txt.setText(R.string.bass);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void TargetFrequencyStart() {
        updateTargetFrequency(); // Get radio button selection

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message m) {
                updateDisplay(m.getData().getFloat("Freq"));
            }
        };

        mCapture = new CaptureThread(mHandler);
        mCapture.setRunning(true);
        mCapture.start();
    }

    private void updateTargetFrequency() {
    	// Grab the selected radio button tag.
        RadioGroup rg = findViewById(R.id.radioGroup1);
        int selected = rg.getCheckedRadioButtonId();
        RadioButton rb = findViewById(selected);

        // err
//        rb = findViewById(456);

        hz.setText(rb.getText());

        // Update TextView
        targetFrequency = Float.parseFloat((String)rb.getTag());
        if (targetFrequency < 1000f)
            aim.setText(String.format("%.1f Hz", targetFrequency));
        else
            aim.setText(String.format("%.2f kHz", targetFrequency/1000));
    }
    
    public void updateDisplay(float frequency) {
    	// Calculate difference between target and measured frequency,
    	// given that the measured frequency can be a factor of target.
    	float difference = 0;
    	if (frequency > targetFrequency) {
    		int divisions = (int) (frequency / targetFrequency);
    		float modified = targetFrequency * (float) divisions;
    		if (frequency - modified > targetFrequency / 2) {
    			modified += targetFrequency;
    			divisions++;
    		}
    		difference = (frequency - modified) / (float) divisions;
    	} else {
    		// If target is greater than measured, just use difference.
    		difference = frequency - targetFrequency;
    	}
    	
    	float relativeFrequency = targetFrequency + difference;
    	
    	// Update TextView
    	if (relativeFrequency < 1000f)
            topbar.setText(String.format("%.1f Hz", relativeFrequency));
		else
            topbar.setText(String.format("%.2f kHz", relativeFrequency/1000));

    	// Update DialView
    	float value = difference / (targetFrequency / 2) * 90;
        dial.update(value, 0, 0);
    }
    
    public void onRadioButtonClicked(View v) {
        // Perform action on clicks
        RadioButton rb = (RadioButton) v;

        hz.setText(rb.getText());

        RadioGroup radioGroup = findViewById(R.id.radioGroup1);
        int position = radioGroup.indexOfChild(rb);

        // Update TextView
        targetFrequency = Float.parseFloat((String)rb.getTag());
        if (targetFrequency < 1000f)
            aim.setText(String.format("%.1f Hz", targetFrequency));
        else
            aim.setText(String.format("%.2f kHz", targetFrequency/1000));

        if (mAdView != null)
            try {
                if ((position > 1) && (position <= 5)) {
                    mAdView.loadAd(adRequest);
                    mAdView.setVisibility(View.VISIBLE);
                } else {
                    mAdView.destroy();
                    mAdView.setVisibility(View.GONE);
                }
            } catch (Exception e) {}
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
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch(which){
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
        result.append("Current lang: " + lang + "\n");
        result.append("Current targetFrequency: " + targetFrequency + "\n");
        result.append("Current rb: " + hz.getText() + "\n");

        return result.toString();
    }
}