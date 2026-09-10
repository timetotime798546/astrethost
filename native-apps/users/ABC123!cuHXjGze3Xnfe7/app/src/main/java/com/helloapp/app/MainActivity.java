package com.helloapp.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Calendar;
import java.util.Random;

public class MainActivity extends Activity {

    private EditText etNameInput;
    private Button actionButton;
    private TextView helloText;
    private TextView tvSubGreeting;
    private TextView tvWelcomeHeader;
    private TextView tvInspiration;
    private Button btnRefreshWisdom;

    private Button btnVibeEnergetic;
    private Button btnVibeCalm;
    private Button btnVibeInspire;

    private Cube3DView cube3DView;
    private android.os.Handler rotationHandler;
    private Runnable rotationRunnable;

    private String currentVibe = "Energetic";

    private final String[] energeticQuotes = {
        "Supercharge your actions! Massive progress is built step by step!",
        "Awesome vibes ahead! The universe is waiting for your creativity!",
        "Unleash your full power! Success is a dynamic journey of bold attempts!"
    };

    private final String[] calmQuotes = {
        "In the midst of movement, keep calm and center yourself.",
        "Take a breath. Step by step, everything falls into place beautifully.",
        "Peace is a journey. Enjoy the tranquil rhythm of creating today."
    };

    private final String[] inspiringQuotes = {
        "Your only limit is your imagination. Create without limits.",
        "Every masterpiece begins with a single bold and humble brush stroke.",
        "Be the light that inspires others to dream bigger and fly higher."
    };

    private final String[] generalWisdom = {
        "“The secret of getting ahead is getting started.” — Mark Twain",
        "“Quality is not an act, it is a habit.” — Aristotle",
        "“Believe you can and you're halfway there.” — Theodore Roosevelt",
        "“The best way to predict the future is to create it.” — Peter Drucker",
        "“Make each day your masterpiece.” — John Wooden",
        "“Action is the foundational key to all success.” — Pablo Picasso"
    };

    private int wisdomIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etNameInput = (EditText) findViewById(R.id.etNameInput);
        actionButton = (Button) findViewById(R.id.actionButton);
        helloText = (TextView) findViewById(R.id.helloText);
        tvSubGreeting = (TextView) findViewById(R.id.tvSubGreeting);
        tvWelcomeHeader = (TextView) findViewById(R.id.tvWelcomeHeader);
        tvInspiration = (TextView) findViewById(R.id.tvInspiration);
        btnRefreshWisdom = (Button) findViewById(R.id.btnRefreshWisdom);

        btnVibeEnergetic = (Button) findViewById(R.id.btnVibeEnergetic);
        btnVibeCalm = (Button) findViewById(R.id.btnVibeCalm);
        btnVibeInspire = (Button) findViewById(R.id.btnVibeInspire);

        cube3DView = (Cube3DView) findViewById(R.id.cube3DView);

        updateTimeOfDayHeader();
        updateVibeUI();

        btnVibeEnergetic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentVibe = "Energetic";
                updateVibeUI();
            }
        });

        btnVibeCalm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentVibe = "Calm";
                updateVibeUI();
            }
        });

        btnVibeInspire.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentVibe = "Inspiring";
                updateVibeUI();
            }
        });

        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerInteraction();
            }
        });

        btnRefreshWisdom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cycleWisdom();
            }
        });

        // Initialize continuous 3D Space rotation engine loop
        rotationHandler = new android.os.Handler();
        rotationRunnable = new Runnable() {
            @Override
            public void run() {
                if (cube3DView != null) {
                    cube3DView.autoRotate();
                }
                rotationHandler.postDelayed(this, 30);
            }
        };
        rotationHandler.postDelayed(rotationRunnable, 1000);
    }

    private void updateTimeOfDayHeader() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) {
            tvWelcomeHeader.setText("Good Morning ☕");
        } else if (hour < 18) {
            tvWelcomeHeader.setText("Good Afternoon ☀️");
        } else {
            tvWelcomeHeader.setText("Good Evening 🌙");
        }
    }

    private void updateVibeUI() {
        resetVibeButtonState(btnVibeEnergetic);
        resetVibeButtonState(btnVibeCalm);
        resetVibeButtonState(btnVibeInspire);

        if ("Energetic".equals(currentVibe)) {
            setActiveVibeStyle(btnVibeEnergetic, "#EC4899", "#FCE7F3", "#FBCFE8");
        } else if ("Calm".equals(currentVibe)) {
            setActiveVibeStyle(btnVibeCalm, "#0EA5E9", "#E0F2FE", "#BAE6FD");
        } else if ("Inspiring".equals(currentVibe)) {
            setActiveVibeStyle(btnVibeInspire, "#8B5CF6", "#EDE9FE", "#DDD6FE");
        }
    }

    private void resetVibeButtonState(Button btn) {
        StateListDrawable sld = new StateListDrawable();
        LayerDrawable pressedLayer = createPressed3DLayer("#F8FAFC", 4);
        LayerDrawable normalLayer = create3DLayer("#F8FAFC", "#94A3B8", "#CBD5E1", 4);

        sld.addState(new int[]{android.R.attr.state_pressed}, pressedLayer);
        sld.addState(new int[]{}, normalLayer);

        btn.setBackground(sld);
        btn.setTextColor(Color.parseColor("#64748B"));
    }

    private void setActiveVibeStyle(Button btn, String activeColor, String bgActiveColor, String shadowColor) {
        StateListDrawable sld = new StateListDrawable();
        LayerDrawable pressedLayer = createPressed3DLayer(bgActiveColor, 4);
        LayerDrawable normalLayer = create3DLayer(bgActiveColor, shadowColor, activeColor, 4);

        sld.addState(new int[]{android.R.attr.state_pressed}, pressedLayer);
        sld.addState(new int[]{}, normalLayer);

        btn.setBackground(sld);
        btn.setTextColor(Color.parseColor(activeColor));
    }

    private LayerDrawable create3DLayer(String topColor, String bottomColor, String strokeColor, int offsetDp) {
        int offset = dpToPx(offsetDp);

        GradientDrawable bottomGd = new GradientDrawable();
        bottomGd.setColor(Color.parseColor(bottomColor));
        bottomGd.setCornerRadius(dpToPx(12));

        GradientDrawable topGd = new GradientDrawable();
        topGd.setColor(Color.parseColor(topColor));
        topGd.setCornerRadius(dpToPx(12));
        if (strokeColor != null) {
            topGd.setStroke(dpToPx(2), Color.parseColor(strokeColor));
        }

        Drawable[] layers = new Drawable[]{bottomGd, topGd};
        LayerDrawable layerDrawable = new LayerDrawable(layers);

        // Adjust offsets to construct a neat 3D drop depth projection
        layerDrawable.setLayerInset(0, 0, offset, 0, 0); // left, top, right, bottom
        layerDrawable.setLayerInset(1, 0, 0, 0, offset);

        return layerDrawable;
    }

    private LayerDrawable createPressed3DLayer(String color, int offsetDp) {
        int offset = dpToPx(offsetDp);

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor(color));
        gd.setCornerRadius(dpToPx(12));
        gd.setStroke(dpToPx(1), Color.parseColor("#CBD5E1"));

        Drawable[] layers = new Drawable[]{gd};
        LayerDrawable layerDrawable = new LayerDrawable(layers);
        layerDrawable.setLayerInset(0, 0, offset, 0, 0);

        return layerDrawable;
    }

    private void triggerInteraction() {
        String name = etNameInput.getText().toString().trim();
        if (name.isEmpty()) {
            name = "Space Architect";
        }

        String customGreeting = "Hello, " + name + "!";
        String subGreeting = "Welcome to a state of absolute potential.";
        Random rnd = new Random();

        if ("Energetic".equals(currentVibe)) {
            customGreeting = "Let's Go, " + name + "! ⚡";
            subGreeting = energeticQuotes[rnd.nextInt(energeticQuotes.length)];
        } else if ("Calm".equals(currentVibe)) {
            customGreeting = "Welcome, " + name + " 🍃";
            subGreeting = calmQuotes[rnd.nextInt(calmQuotes.length)];
        } else if ("Inspiring".equals(currentVibe)) {
            customGreeting = "Be Inspired, " + name + " ✨";
            subGreeting = inspiringQuotes[rnd.nextInt(inspiringQuotes.length)];
        }

        final String finalGreeting = customGreeting;
        final String finalSub = subGreeting;

        helloText.animate().alpha(0.0f).setDuration(100).withEndAction(new Runnable() {
            @Override
            public void run() {
                helloText.setText(finalGreeting);
                helloText.animate().alpha(1.0f).setDuration(150).start();
            }
        }).start();

        tvSubGreeting.animate().alpha(0.0f).setDuration(100).withEndAction(new Runnable() {
            @Override
            public void run() {
                tvSubGreeting.setText(finalSub);
                tvSubGreeting.animate().alpha(1.0f).setDuration(150).start();
            }
        }).start();

        Toast.makeText(this, "3D Workspace modified for " + name, Toast.LENGTH_SHORT).show();
    }

    private void cycleWisdom() {
        wisdomIndex = (wisdomIndex + 1) % generalWisdom.length;

        tvInspiration.animate().alpha(0.0f).setDuration(120).withEndAction(new Runnable() {
            @Override
            public void run() {
                tvInspiration.setText(generalWisdom[wisdomIndex]);
                tvInspiration.animate().alpha(1.0f).setDuration(120).start();
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        if (rotationHandler != null && rotationRunnable != null) {
            rotationHandler.removeCallbacks(rotationRunnable);
        }
        super.onDestroy();
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }
}