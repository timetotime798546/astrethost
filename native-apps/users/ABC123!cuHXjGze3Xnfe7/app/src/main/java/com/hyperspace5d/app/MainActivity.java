package com.hyperspace5d.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Calendar;
import java.util.Random;

public class MainActivity extends Activity {

    private TextView tvChronosHeader;
    private TextView tvProjectionMetrics;
    private TextView tvAtmosphereQuote;
    private Button btnAudioToggle;
    private Button btnExciteAtmosphere;
    private Button btnHarmonize;

    private Button btnShapeSimplex;
    private Button btnShapePenteract;
    private Button btnShapeCross;

    private Button btnToggleXY;
    private Button btnToggleXW;
    private Button btnToggleZV;
    private Button btnToggleYW;
    private Button btnToggleXV;
    private Button btnToggleWV;

    private SeekBar sbWAxis;
    private SeekBar sbVAxis;

    private Hyper5DView hyper5DView;
    private Handler rotationHandler;
    private Runnable rotationRunnable;

    private final String[] atmosphereQuotes = {
        "“Space is the three-dimensional boundary of a four-dimensional realm flowing into 5D eternity.”",
        "“To rotate in the W-V plane is to alter the frequency of coordinate time itself.”",
        "“The Penteract holds 32 vertices, yet projected onto flat paper it weaves a star of paths.”",
        "“Each cross section of the 5-Simplex contains a fleeting moment of stable matter.”",
        "“Observe: shifting W or V alters the pitch and volume of our synthesized dimensional field.”",
        "“Mathematics is the language of spaces we cannot touch, yet can visualize perfectly.”"
    };

    private int quoteIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvChronosHeader = (TextView) findViewById(R.id.tvChronosHeader);
        tvProjectionMetrics = (TextView) findViewById(R.id.tvProjectionMetrics);
        tvAtmosphereQuote = (TextView) findViewById(R.id.tvAtmosphereQuote);
        btnAudioToggle = (Button) findViewById(R.id.btnAudioToggle);
        btnExciteAtmosphere = (Button) findViewById(R.id.btnExciteAtmosphere);
        btnHarmonize = (Button) findViewById(R.id.btnHarmonize);

        btnShapeSimplex = (Button) findViewById(R.id.btnShapeSimplex);
        btnShapePenteract = (Button) findViewById(R.id.btnShapePenteract);
        btnShapeCross = (Button) findViewById(R.id.btnShapeCross);

        btnToggleXY = (Button) findViewById(R.id.btnToggleXY);
        btnToggleXW = (Button) findViewById(R.id.btnToggleXW);
        btnToggleZV = (Button) findViewById(R.id.btnToggleZV);
        btnToggleYW = (Button) findViewById(R.id.btnToggleYW);
        btnToggleXV = (Button) findViewById(R.id.btnToggleXV);
        btnToggleWV = (Button) findViewById(R.id.btnToggleWV);

        sbWAxis = (SeekBar) findViewById(R.id.sbWAxis);
        sbVAxis = (SeekBar) findViewById(R.id.sbVAxis);

        hyper5DView = (Hyper5DView) findViewById(R.id.hyper5DView);

        // Bind Sliders to W/V Shift Coefficients in Custom View
        sbWAxis.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float shift = (progress - 100) / 50.0f; // -2.0 to 2.0
                hyper5DView.setWAxisShift(shift);
                updateMetricsText();
                // Synthesizer dynamic pitch adjustment based on coordinate shifting!
                SoundEngine.setFrequencyModifiers(shift, hyper5DView.getVAxisShift());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                SoundEngine.playClick();
            }
        });

        sbVAxis.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float shift = (progress - 100) / 50.0f; // -2.0 to 2.0
                hyper5DView.setVAxisShift(shift);
                updateMetricsText();
                // Synthesizer dynamic pitch adjustment based on coordinate shifting!
                SoundEngine.setFrequencyModifiers(hyper5DView.getWAxisShift(), shift);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                SoundEngine.playClick();
            }
        });

        // Initialize active plane buttons UI state
        updatePlaneToggleUI();
        updateShapeUI();
        updateAudioButtonUI();
        updateTimeHeader();

        // Register plane selectors
        btnToggleXY.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hyper5DView.togglePlane(Hyper5DView.PLANE_XY);
                updatePlaneToggleUI();
                SoundEngine.playClick();
            }
        });

        btnToggleXW.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hyper5DView.togglePlane(Hyper5DView.PLANE_XW);
                updatePlaneToggleUI();
                SoundEngine.playClick();
            }
        });

        btnToggleZV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hyper5DView.togglePlane(Hyper5DView.PLANE_ZV);
                updatePlaneToggleUI();
                SoundEngine.playClick();
            }
        });

        btnToggleYW.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hyper5DView.togglePlane(Hyper5DView.PLANE_YW);
                updatePlaneToggleUI();
                SoundEngine.playClick();
            }
        });

        btnToggleXV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hyper5DView.togglePlane(Hyper5DView.PLANE_XV);
                updatePlaneToggleUI();
                SoundEngine.playClick();
            }
        });

        btnToggleWV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hyper5DView.togglePlane(Hyper5DView.PLANE_WV);
                updatePlaneToggleUI();
                SoundEngine.playClick();
            }
        });

        // Register active shapes selectors
        btnShapeSimplex.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hyper5DView.setShapeMode(Hyper5DView.SHAPE_SIMPLEX_5D);
                updateShapeUI();
                updateMetricsText();
                SoundEngine.playSweepUp();
                Toast.makeText(MainActivity.this, "Activated 5-Simplex (Hexateron)", Toast.LENGTH_SHORT).show();
            }
        });

        btnShapePenteract.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hyper5DView.setShapeMode(Hyper5DView.SHAPE_PENTERACT_5D);
                updateShapeUI();
                updateMetricsText();
                SoundEngine.playSweepUp();
                Toast.makeText(MainActivity.this, "Activated Penteract (5-Cube)", Toast.LENGTH_SHORT).show();
            }
        });

        btnShapeCross.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hyper5DView.setShapeMode(Hyper5DView.SHAPE_CROSS_POLYTOPE_5D);
                updateShapeUI();
                updateMetricsText();
                SoundEngine.playSweepUp();
                Toast.makeText(MainActivity.this, "Activated 5-Cross Polytope", Toast.LENGTH_SHORT).show();
            }
        });

        btnExciteAtmosphere.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cycleQuotes();
            }
        });

        btnAudioToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean nextState = !SoundEngine.isSoundEnabled();
                SoundEngine.setSoundEnabled(nextState);
                updateAudioButtonUI();
                if (nextState) {
                    SoundEngine.playClick();
                }
            }
        });

        btnHarmonize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Play complex wave synthesized using current W & V coefficients
                SoundEngine.playDissonantResonance(hyper5DView.getWAxisShift(), hyper5DView.getVAxisShift());
                Toast.makeText(MainActivity.this, "Harmonizing 5D sound fields!", Toast.LENGTH_SHORT).show();
            }
        });

        // Setup Continuous Rotation Engine Thread
        rotationHandler = new Handler();
        rotationRunnable = new Runnable() {
            @Override
            public void run() {
                if (hyper5DView != null) {
                    hyper5DView.incrementAngles();
                }
                rotationHandler.postDelayed(this, 30);
            }
        };
        rotationHandler.postDelayed(rotationRunnable, 500);
    }

    private void updateTimeHeader() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) {
            tvChronosHeader.setText("HyperSpace ☕ Morning");
        } else if (hour < 18) {
            tvChronosHeader.setText("HyperSpace ☀️ Mid-Day");
        } else {
            tvChronosHeader.setText("HyperSpace 🌌 Twilight");
        }
    }

    private void updateMetricsText() {
        if (hyper5DView == null) return;
        float wVal = hyper5DView.getWAxisShift();
        float vVal = hyper5DView.getVAxisShift();
        String formatMetrics = String.format("W-Shift: %.2f | V-Shift: %.2f | Mode: %s",
                wVal, vVal, hyper5DView.getShapeModeName());
        tvProjectionMetrics.setText(formatMetrics);
    }

    private void updateAudioButtonUI() {
        if (SoundEngine.isSoundEnabled()) {
            btnAudioToggle.setText("🔊 SYNTH: ON");
            setNeobrutalistAccent(btnAudioToggle, "#06B6D4", "#E0F2FE", "#38BDF8"); // Light electric cyan
        } else {
            btnAudioToggle.setText("🔇 MUTED");
            setNeobrutalistAccent(btnAudioToggle, "#64748B", "#F1F5F9", "#94A3B8"); // Slate gray
        }
    }

    private void updateShapeUI() {
        resetButtonBackground(btnShapeSimplex);
        resetButtonBackground(btnShapePenteract);
        resetButtonBackground(btnShapeCross);

        int mode = hyper5DView.getShapeMode();
        if (mode == Hyper5DView.SHAPE_SIMPLEX_5D) {
            setNeobrutalistAccent(btnShapeSimplex, "#F43F5E", "#FFE4E6", "#FDA4AF"); // Pinkish Rose
        } else if (mode == Hyper5DView.SHAPE_PENTERACT_5D) {
            setNeobrutalistAccent(btnShapePenteract, "#34D399", "#D1FAE5", "#6EE7B7"); // Emerald Green
        } else if (mode == Hyper5DView.SHAPE_CROSS_POLYTOPE_5D) {
            setNeobrutalistAccent(btnShapeCross, "#818CF8", "#E0E7FF", "#A5B4FC"); // Vibrant Indigo
        }
    }

    private void updatePlaneToggleUI() {
        setPlaneButtonStyle(btnToggleXY, hyper5DView.isPlaneEnabled(Hyper5DView.PLANE_XY));
        setPlaneButtonStyle(btnToggleXW, hyper5DView.isPlaneEnabled(Hyper5DView.PLANE_XW));
        setPlaneButtonStyle(btnToggleZV, hyper5DView.isPlaneEnabled(Hyper5DView.PLANE_ZV));
        setPlaneButtonStyle(btnToggleYW, hyper5DView.isPlaneEnabled(Hyper5DView.PLANE_YW));
        setPlaneButtonStyle(btnToggleXV, hyper5DView.isPlaneEnabled(Hyper5DView.PLANE_XV));
        setPlaneButtonStyle(btnToggleWV, hyper5DView.isPlaneEnabled(Hyper5DView.PLANE_WV));
    }

    private void setPlaneButtonStyle(Button btn, boolean enabled) {
        if (enabled) {
            setNeobrutalistAccent(btn, "#A855F7", "#FAF5FF", "#D8B4FE"); // Violet Purple
        } else {
            resetButtonBackground(btn);
        }
    }

    private void resetButtonBackground(Button btn) {
        StateListDrawable sld = new StateListDrawable();
        LayerDrawable pressed = createLayerDrawable("#1E293B", "#334155", 3);
        LayerDrawable normal = createLayerDrawable("#0F172A", "#475569", 3);

        sld.addState(new int[]{android.R.attr.state_pressed}, pressed);
        sld.addState(new int[]{}, normal);

        btn.setBackground(sld);
        btn.setTextColor(Color.parseColor("#94A3B8"));
    }

    private void setNeobrutalistAccent(Button btn, String activeColor, String activeBg, String shadowColor) {
        StateListDrawable sld = new StateListDrawable();
        LayerDrawable pressed = createLayerDrawable(activeBg, activeColor, 2);
        LayerDrawable normal = createLayerDrawableWithShadow(activeBg, shadowColor, activeColor, 3);

        sld.addState(new int[]{android.R.attr.state_pressed}, pressed);
        sld.addState(new int[]{}, normal);

        btn.setBackground(sld);
        btn.setTextColor(Color.parseColor(activeColor));
    }

    private LayerDrawable createLayerDrawable(String colorHex, String strokeHex, int offsetDp) {
        int offset = dpToPx(offsetDp);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor(colorHex));
        gd.setCornerRadius(dpToPx(10));
        gd.setStroke(dpToPx(1.5f), Color.parseColor(strokeHex));

        Drawable[] layers = { gd };
        LayerDrawable ld = new LayerDrawable(layers);
        ld.setLayerInset(0, 0, offset, 0, 0);
        return ld;
    }

    private LayerDrawable createLayerDrawableWithShadow(String bgHex, String shadowHex, String strokeHex, int offsetDp) {
        int offset = dpToPx(offsetDp);

        GradientDrawable shadowGd = new GradientDrawable();
        shadowGd.setColor(Color.parseColor(shadowHex));
        shadowGd.setCornerRadius(dpToPx(10));

        GradientDrawable frontGd = new GradientDrawable();
        frontGd.setColor(Color.parseColor(bgHex));
        frontGd.setCornerRadius(dpToPx(10));
        frontGd.setStroke(dpToPx(1.5f), Color.parseColor(strokeHex));

        Drawable[] layers = { shadowGd, frontGd };
        LayerDrawable ld = new LayerDrawable(layers);
        ld.setLayerInset(0, 0, offset, 0, 0);
        ld.setLayerInset(1, 0, 0, 0, offset);
        return ld;
    }

    private void cycleQuotes() {
        quoteIndex = (quoteIndex + 1) % atmosphereQuotes.length;
        SoundEngine.playClick();

        tvAtmosphereQuote.animate().alpha(0.0f).setDuration(150).withEndAction(new Runnable() {
            @Override
            public void run() {
                tvAtmosphereQuote.setText(atmosphereQuotes[quoteIndex]);
                tvAtmosphereQuote.animate().alpha(1.0f).setDuration(150).start();
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

    private int dpToPx(float dp) {
        float scale = getResources().getDisplayMetrics().density;
        return Math.round(dp * scale);
    }
}