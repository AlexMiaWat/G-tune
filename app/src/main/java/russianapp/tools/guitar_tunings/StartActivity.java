package russianapp.tools.guitar_tunings;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.Window;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class StartActivity extends Activity {

    Activity main;
    protected MediaPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // нет заголовка
        setContentView(R.layout.activity_start);

        // Play music
        player = MediaPlayer.create(this, R.raw.ants);
        if (player != null)
            player.start();

        main = this;

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            // Permission already Granted
            // Do your work here
            // Perform operations here only which requires permission

            // Запускаем
            RECORD_AUDIO_PERMISSION_GRANTED();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }

        ImageView imageView = findViewById(R.id.imageView);
        imageView.setOnClickListener(v -> ActivityCompat.requestPermissions(main, new String[]{Manifest.permission.RECORD_AUDIO}, 1));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //Permission Granted
                //Do your work here
                //Perform operations here only which requires permission

                //Запускаем
                RECORD_AUDIO_PERMISSION_GRANTED();
            }
        }
    }

    /**
     * Called when the user taps RECORD_AUDIO_PERMISSION_GRANTED
     */
    public void RECORD_AUDIO_PERMISSION_GRANTED() {
        // Задержка
        new Handler().postDelayed(() -> {
            try {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                if (player != null) {
                    player.stop();
                    player = null;
                }

                startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 1500);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Stop player
        if (player != null) {
            player.stop();
            player = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (player == null) {
            player = MediaPlayer.create(this, R.raw.ants);
            player.start();
        }
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
            builder.setNegativeButton("No", dialogClickListener);

            AlertDialog dialog = builder.create();
            // Display the alert dialog on interface
            dialog.show();

            // Stop player
            if (player != null) {
                player.stop();
                player = null;
            }
        }

        return true;
    }
}
