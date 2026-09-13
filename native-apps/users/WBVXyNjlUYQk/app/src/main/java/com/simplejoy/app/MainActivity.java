package com.simplejoy.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Random;

public class MainActivity extends Activity {

    private int count = 0;
    private int currentThemeColor = 0xFF3498DB; // Standard blue

    private TextView tvCounter;
    private TextView tvQuote;
    private Button btnTap;
    private Button btnNewQuote;
    private ScrollView rootScroll;
    private LinearLayout mainContainer;
    private LinearLayout counterCard;
    private LinearLayout quoteCard;

    private Button btnThemeBlue;
    private Button btnThemeGreen;
    private Button btnThemePurple;
    private Button btnThemeOrange;
    private Button btnReset;

    private final String[] quotes = {
        "Remember to smile: it is the simplest act of gratitude.",
        "You are doing incredibly well. Give yourself some credit today!",
        "Every small step counts. Keep moving forward peacefully.",
        "Joy is found in the simplicity of present moments.",
        "You make a beautiful difference in the world just by being you.",
        "Let your light shine. The world is waiting for your kindness.",
        "Take a deep breath. Today is a fresh start.",
        "Happiness is a choice, and today you chose joy.",
        "Good things take time. Be gentle with your journey.",
        "Believe you can, and you are already halfway there.",
        "Do not let yesterday take up too much of today.",
        "A quiet mind is a creative mind. Take some time to sit in silence.",
        "Keep your face always toward the sunshine, and shadows will fall behind you.",
        "The best is yet to come. Enjoy this little moment.",
        "Every single day has its own set of blessings. Spot them!",
        "Simplicity is the ultimate sophistication. Find joy in minimal things.",
        "Your attitude determines your direction. Keep it high and positive!",
        "Success is not final; failure is not fatal: it is the courage to continue that counts.",
        "Make each day your masterpiece of kindness and simple actions.",
        "The power to create beautiful memories is entirely in your hands."
    };

    private final Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind Views
        rootScroll = (ScrollView) findViewById(R.id.root_scroll);
        mainContainer = (LinearLayout) findViewById(R.id.main_container);
        counterCard = (LinearLayout) findViewById(R.id.counter_card);
        quoteCard = (LinearLayout) findViewById(R.id.quote_card);

        tvCounter = (TextView) findViewById(R.id.tv_counter);
        tvQuote = (TextView) findViewById(R.id.tv_quote);
        btnTap = (Button) findViewById(R.id.btn_tap);
        btnNewQuote = (Button) findViewById(R.id.btn_new_quote);

        btnThemeBlue = (Button) findViewById(R.id.btn_theme_blue);
        btnThemeGreen = (Button) findViewById(R.id.btn_theme_green);
        btnThemePurple = (Button) findViewById(R.id.btn_theme_purple);
        btnThemeOrange = (Button) findViewById(R.id.btn_theme_orange);
        btnReset = (Button) findViewById(R.id.btn_reset);

        // Standard Button Tap Action
        btnTap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                incrementCounter();
            }
        });

        // Quote Generation Action
        btnNewQuote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNextQuote();
            }
        });

        // Palette Themes
        btnThemeBlue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyTheme(0xFF3498DB, "#F0F4F8");
            }
        });

        btnThemeGreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyTheme(0xFF2ECC71, "#E8F8F5");
            }
        });

        btnThemePurple.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyTheme(0xFF9B59B6, "#F5EEF8");
            }
        });

        btnThemeOrange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyTheme(0xFFE67E22, "#FDF2E9");
            }
        });

        // Reset Listener
        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetAll();
            }
        });
    }

    private void incrementCounter() {
        count++;
        tvCounter.setText(String.valueOf(count));

        // Sweet notification messages at milestones
        if (count == 1) {
            Toast.makeText(this, "The journey begins! Excellent start.", Toast.LENGTH_SHORT).show();
        } else if (count == 10) {
            Toast.makeText(this, "Fantastic! 10 happy smiles achieved!", Toast.LENGTH_SHORT).show();
            showNextQuote();
        } else if (count == 25) {
            Toast.makeText(this, "Wow! 25 taps of positive energy!", Toast.LENGTH_SHORT).show();
            showNextQuote();
        } else if (count == 50) {
            Toast.makeText(this, "Unstoppable! Halfway to a hundred joyful taps!", Toast.LENGTH_LONG).show();
            showNextQuote();
        } else if (count == 100) {
            Toast.makeText(this, "Amazing! You reached 100 taps of pure joy!", Toast.LENGTH_LONG).show();
            showNextQuote();
        }

        // Auto change or shuffle quotes slightly on taps sometimes
        if (count % 7 == 0) {
            showNextQuote();
        }
    }

    private void showNextQuote() {
        int index = random.nextInt(quotes.length);
        tvQuote.setText(quotes[index]);
    }

    private void applyTheme(int color, String bgColorStr) {
        currentThemeColor = color;

        // Apply background and colors where applicable
        btnTap.setBackgroundColor(currentThemeColor);
        tvCounter.setTextColor(currentThemeColor);
        btnNewQuote.setTextColor(currentThemeColor);

        // Standard background light color parsing
        int parsedBgColor = Color.parseColor(bgColorStr);
        rootScroll.setBackgroundColor(parsedBgColor);

        // Brief toast notification
        Toast.makeText(this, "Theme palette updated!", Toast.LENGTH_SHORT).show();
    }

    private void resetAll() {
        count = 0;
        tvCounter.setText("0");
        tvQuote.setText("Press the button above to start your journey of simple joys!");
        applyTheme(0xFF3498DB, "#F7F9FC");
        Toast.makeText(this, "All stats reset successfully!", Toast.LENGTH_SHORT).show();
    }
}