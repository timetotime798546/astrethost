package com.procalc.app;

import android.app.Activity;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends Activity {
    private EditText display;
    private double val1 = 0;
    private String op = "";
    private boolean isNewOp = true;
    
    private SoundPool soundPool;
    private int clickSound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        display = findViewById(R.id.display);

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(audioAttributes)
                .build();
        
        // Use a standard system click sound effect ID
        clickSound = soundPool.load("/system/media/audio/ui/KeypressStandard.ogg", 1);
    }

    private void playClick() {
        if (soundPool != null) {
            soundPool.play(clickSound, 1, 1, 0, 0, 1);
        }
    }

    public void onNumClick(View v) {
        playClick();
        String num = ((Button) v).getText().toString();
        if (isNewOp) {
            display.setText(num);
            isNewOp = false;
        } else {
            String current = display.getText().toString();
            if (current.equals("0") && !num.equals(".")) {
                display.setText(num);
            } else {
                display.append(num);
            }
        }
    }

    public void onOpClick(View v) {
        playClick();
        try {
            val1 = Double.parseDouble(display.getText().toString());
            op = ((Button) v).getText().toString();
            isNewOp = true;
        } catch (NumberFormatException e) {}
    }

    public void onClear(View v) {
        playClick();
        display.setText("0");
        val1 = 0;
        op = "";
        isNewOp = true;
    }

    public void onEquals(View v) {
        playClick();
        try {
            double val2 = Double.parseDouble(display.getText().toString());
            double result = 0;
            switch (op) {
                case "+": result = val1 + val2; break;
                case "-": result = val1 - val2; break;
                case "*": result = val1 * val2; break;
                case "/": result = (val2 != 0) ? (val1 / val2) : 0; break;
                default: return;
            }
            display.setText(result % 1 == 0 ? String.valueOf((long)result) : String.valueOf(result));
            isNewOp = true;
        } catch (Exception e) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
}