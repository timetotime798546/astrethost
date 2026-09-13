package com.simpleapp.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Random;

public class MainActivity extends Activity {

    private int counter = 0;
    private TextView textCounter;
    private TextView textQuote;
    private EditText editNote;

    private static final String PREFS_NAME = "SimpleAppPrefs";
    private static final String KEY_COUNTER = "counter_val";
    private static final String KEY_NOTE = "note_val";

    private final String[] quotes = {
        "“The secret of getting ahead is getting started.” — Mark Twain",
        "“Believe you can and you're halfway there.” — Theodore Roosevelt",
        "“It always seems impossible until it's done.” — Nelson Mandela",
        "“Act as if what you do makes a difference. It does.” — William James",
        "“Success is not final, failure is not fatal: it is the courage to continue that counts.” — Winston Churchill",
        "“Don't watch the clock; do what it does. Keep going.” — Sam Levenson",
        "“You miss 100% of the shots you don't take.” — Wayne Gretzky",
        "“In the middle of difficulty lies opportunity.” — Albert Einstein",
        "“Keep your eyes on the stars, and your feet on the ground.” — Theodore Roosevelt"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textCounter = (TextView) findViewById(R.id.text_counter);
        textQuote = (TextView) findViewById(R.id.text_quote);
        editNote = (EditText) findViewById(R.id.edit_note);

        Button btnIncrement = (Button) findViewById(R.id.btn_increment);
        Button btnDecrement = (Button) findViewById(R.id.btn_decrement);
        Button btnReset = (Button) findViewById(R.id.btn_reset);
        Button btnNewQuote = (Button) findViewById(R.id.btn_new_quote);
        Button btnSaveNote = (Button) findViewById(R.id.btn_save_note);

        // Load Saved State
        final SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        counter = prefs.getInt(KEY_COUNTER, 0);
        textCounter.setText(String.valueOf(counter));

        String savedNote = prefs.getString(KEY_NOTE, "");
        editNote.setText(savedNote);

        // Counter Click Listeners
        btnIncrement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                counter++;
                updateCounter(prefs);
            }
        });

        btnDecrement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (counter > 0) {
                    counter--;
                    updateCounter(prefs);
                } else {
                    Toast.makeText(MainActivity.this, "Counter cannot go below 0", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                counter = 0;
                updateCounter(prefs);
            }
        });

        // Quote Click Listener
        btnNewQuote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Random random = new Random();
                int index = random.nextInt(quotes.length);
                textQuote.setText(quotes[index]);
            }
        });

        // Save Note Click Listener
        btnSaveNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String noteText = editNote.getText().toString();
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(KEY_NOTE, noteText);
                editor.apply();
                Toast.makeText(MainActivity.this, "Note saved successfully!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCounter(SharedPreferences prefs) {
        textCounter.setText(String.valueOf(counter));
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_COUNTER, counter);
        editor.apply();
    }
}