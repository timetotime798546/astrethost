package com.tinynotes.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {

    private EditText etNote;
    private TextView tvCharCount;
    private TextView tvStatus;
    private Button btnSave;
    private Button btnClear;

    private TextView tvCounterVal;
    private Button btnIncrement;
    private Button btnDecrement;
    private Button btnResetCounter;

    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "TinyNotesPrefs";
    private static final String KEY_NOTE_CONTENT = "note_content";
    private static final String KEY_COUNTER_VAL = "counter_val";

    private int currentCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // Bind Views
        etNote = (EditText) findViewById(R.id.et_note);
        tvCharCount = (TextView) findViewById(R.id.tv_char_count);
        tvStatus = (TextView) findViewById(R.id.tv_status);
        btnSave = (Button) findViewById(R.id.btn_save);
        btnClear = (Button) findViewById(R.id.btn_clear);

        tvCounterVal = (TextView) findViewById(R.id.tv_counter_val);
        btnIncrement = (Button) findViewById(R.id.btn_increment);
        btnDecrement = (Button) findViewById(R.id.btn_decrement);
        btnResetCounter = (Button) findViewById(R.id.btn_reset_counter);

        // Load Persistent Data
        String savedNote = sharedPreferences.getString(KEY_NOTE_CONTENT, "");
        etNote.setText(savedNote);
        updateCharCount(savedNote.length());

        currentCount = sharedPreferences.getInt(KEY_COUNTER_VAL, 0);
        updateCounterUI();

        // Note Real-time Character Counter
        etNote.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateCharCount(s.length());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Save Note Button Listener
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String content = etNote.getText().toString();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(KEY_NOTE_CONTENT, content);
                editor.apply();

                tvStatus.setText("Note saved successfully!");
                tvStatus.setVisibility(View.VISIBLE);
                
                // Hide status after 2 seconds
                tvStatus.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        tvStatus.setVisibility(View.INVISIBLE);
                    }
                }, 2000);
            }
        });

        // Clear Note Button Listener
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etNote.setText("");
                updateCharCount(0);
                
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(KEY_NOTE_CONTENT, "");
                editor.apply();

                tvStatus.setText("Note cleared!");
                tvStatus.setVisibility(View.VISIBLE);
                
                tvStatus.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        tvStatus.setVisibility(View.INVISIBLE);
                    }
                }, 2000);
            }
        });

        // Increment Counter Listener
        btnIncrement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentCount++;
                saveCounterVal();
                updateCounterUI();
            }
        });

        // Decrement Counter Listener
        btnDecrement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentCount > 0) {
                    currentCount--;
                    saveCounterVal();
                    updateCounterUI();
                }
            }
        });

        // Reset Counter Listener
        btnResetCounter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentCount = 0;
                saveCounterVal();
                updateCounterUI();
            }
        });
    }

    private void updateCharCount(int count) {
        tvCharCount.setText("Characters: " + count);
    }

    private void saveCounterVal() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(KEY_COUNTER_VAL, currentCount);
        editor.apply();
    }

    private void updateCounterUI() {
        tvCounterVal.setText("Count: " + currentCount);
    }
}