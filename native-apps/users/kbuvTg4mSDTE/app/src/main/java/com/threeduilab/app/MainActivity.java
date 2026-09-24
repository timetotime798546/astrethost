package com.threeduilab.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends Activity {

    private Button tabCard;
    private Button tabCube;
    private Button tabSphere;

    private FrameLayout containerCardView;
    private ThreeDCubeView threeDCubeView;
    private ThreeDCardView threeDCardView;
    private LinearLayout cardInnerLayout;

    private TextView controlTitle;
    private LinearLayout controlsCard;
    private LinearLayout controlsPolyhedron;

    private Switch switchCardAuto;
    private SeekBar seekCardTilt;
    private TextView textCardTiltValue;
    private SeekBar seekCardPersp;
    private TextView textCardPerspValue;

    private LinearLayout shapeSelectorGroup;
    private Button btnCubeShape;
    private Button btnPyramidShape;
    private Button btnOctaShape;
    private Switch switchCubeSpin;
    private TextView labelFaceTransparency;
    private LinearLayout rowFaceTransparency;
    private SeekBar seekFaceAlpha;
    private TextView textFaceAlphaValue;
    private SeekBar seekCubeSpeed;
    private TextView textCubeSpeedValue;

    private Button btnThemeNeon;
    private Button btnThemeCosmic;
    private Button btnThemeGold;
    private Button btnThemeCyber;

    private int activeStrokeColor = 0xFF00E676;
    private int activeFillColor = 0xFF122C24;
    private int activeCardColor = 0xFF0A231C;
    private int activeParticleColor = 0xFF00E676;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tabCard = (Button) findViewById(R.id.tabCard);
        tabCube = (Button) findViewById(R.id.tabCube);
        tabSphere = (Button) findViewById(R.id.tabSphere);

        containerCardView = (FrameLayout) findViewById(R.id.containerCardView);
        threeDCubeView = (ThreeDCubeView) findViewById(R.id.threeDCubeView);
        threeDCardView = (ThreeDCardView) findViewById(R.id.threeDCardView);
        cardInnerLayout = (LinearLayout) findViewById(R.id.cardInnerLayout);

        controlTitle = (TextView) findViewById(R.id.controlTitle);
        controlsCard = (LinearLayout) findViewById(R.id.controlsCard);
        controlsPolyhedron = (LinearLayout) findViewById(R.id.controlsPolyhedron);

        switchCardAuto = (Switch) findViewById(R.id.switchCardAuto);
        seekCardTilt = (SeekBar) findViewById(R.id.seekCardTilt);
        textCardTiltValue = (TextView) findViewById(R.id.textCardTiltValue);
        seekCardPersp = (SeekBar) findViewById(R.id.seekCardPersp);
        textCardPerspValue = (TextView) findViewById(R.id.textCardPerspValue);

        shapeSelectorGroup = (LinearLayout) findViewById(R.id.shapeSelectorGroup);
        btnCubeShape = (Button) findViewById(R.id.btnCubeShape);
        btnPyramidShape = (Button) findViewById(R.id.btnPyramidShape);
        btnOctaShape = (Button) findViewById(R.id.btnOctaShape);
        switchCubeSpin = (Switch) findViewById(R.id.switchCubeSpin);
        labelFaceTransparency = (TextView) findViewById(R.id.labelFaceTransparency);
        rowFaceTransparency = (LinearLayout) findViewById(R.id.rowFaceTransparency);
        seekFaceAlpha = (SeekBar) findViewById(R.id.seekFaceAlpha);
        textFaceAlphaValue = (TextView) findViewById(R.id.textFaceAlphaValue);
        seekCubeSpeed = (SeekBar) findViewById(R.id.seekCubeSpeed);
        textCubeSpeedValue = (TextView) findViewById(R.id.textCubeSpeedValue);

        btnThemeNeon = (Button) findViewById(R.id.btnThemeNeon);
        btnThemeCosmic = (Button) findViewById(R.id.btnThemeCosmic);
        btnThemeGold = (Button) findViewById(R.id.btnThemeGold);
        btnThemeCyber = (Button) findViewById(R.id.btnThemeCyber);

        setupTabs();
        setupCardDemoListeners();
        setupPolyhedronListeners();
        setupThemeColors();

        applyThemePreset(0xFF00E676, 0xFF0B2418, 0xFF0D1D16, 0xFF00E676);
        updateTabStyles(tabCard);
    }

    private void setupTabs() {
        tabCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                containerCardView.setVisibility(View.VISIBLE);
                threeDCubeView.setVisibility(View.GONE);
                
                controlsCard.setVisibility(View.VISIBLE);
                controlsPolyhedron.setVisibility(View.GONE);

                controlTitle.setText("3D Card Parameters");
                updateTabStyles(tabCard);
            }
        });

        tabCube.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                containerCardView.setVisibility(View.GONE);
                threeDCubeView.setVisibility(View.VISIBLE);
                threeDCubeView.setMode(ThreeDCubeView.MODE_CUBE);
                
                controlsCard.setVisibility(View.GONE);
                controlsPolyhedron.setVisibility(View.VISIBLE);
                shapeSelectorGroup.setVisibility(View.VISIBLE);
                
                labelFaceTransparency.setVisibility(View.VISIBLE);
                rowFaceTransparency.setVisibility(View.VISIBLE);

                controlTitle.setText("3D Polyhedron Sandbox Settings");
                updateTabStyles(tabCube);
                updateShapeTabStyle(btnCubeShape);
            }
        });

        tabSphere.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                containerCardView.setVisibility(View.GONE);
                threeDCubeView.setVisibility(View.VISIBLE);
                threeDCubeView.setMode(ThreeDCubeView.MODE_SPHERE);
                
                controlsCard.setVisibility(View.GONE);
                controlsPolyhedron.setVisibility(View.VISIBLE);
                shapeSelectorGroup.setVisibility(View.GONE);

                labelFaceTransparency.setVisibility(View.GONE);
                rowFaceTransparency.setVisibility(View.GONE);

                controlTitle.setText("3D Particle Sphere Parameters");
                updateTabStyles(tabSphere);
            }
        });
    }

    private void updateTabStyles(Button activeTab) {
        Button[] tabs = {tabCard, tabCube, tabSphere};
        for (Button tab : tabs) {
            if (tab == activeTab) {
                tab.setBackgroundColor(activeStrokeColor);
                tab.setTextColor(0xFF09090E);
            } else {
                tab.setBackgroundColor(0xFF161622);
                tab.setTextColor(0xFF88888F);
            }
        }
    }

    private void setupCardDemoListeners() {
        switchCardAuto.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                threeDCardView.setAutoTilt(isChecked);
                seekCardTilt.setEnabled(!isChecked);
            }
        });

        seekCardTilt.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 5) progress = 5;
                threeDCardView.setMaxTiltAngle(progress);
                textCardTiltValue.setText(progress + "°");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekCardPersp.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float perspective = progress + 5f;
                threeDCardView.setPerspectiveValue(perspective);
                textCardPerspValue.setText(String.valueOf((int) perspective));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void setupPolyhedronListeners() {
        btnCubeShape.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                threeDCubeView.setMode(ThreeDCubeView.MODE_CUBE);
                updateShapeTabStyle(btnCubeShape);
            }
        });

        btnPyramidShape.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                threeDCubeView.setMode(ThreeDCubeView.MODE_PYRAMID);
                updateShapeTabStyle(btnPyramidShape);
            }
        });

        btnOctaShape.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                threeDCubeView.setMode(ThreeDCubeView.MODE_OCTAHEDRON);
                updateShapeTabStyle(btnOctaShape);
            }
        });

        switchCubeSpin.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                threeDCubeView.setAutoSpin(isChecked);
            }
        });

        seekFaceAlpha.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                threeDCubeView.setFaceAlpha(progress);
                textFaceAlphaValue.setText(String.valueOf(progress));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekCubeSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float factor = progress / 5.0f;
                threeDCubeView.setSpinSpeed(factor);
                textCubeSpeedValue.setText(String.format("%.1fx", factor));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void updateShapeTabStyle(Button activeBtn) {
        Button[] buttons = {btnCubeShape, btnPyramidShape, btnOctaShape};
        for (Button btn : buttons) {
            if (btn == activeBtn) {
                btn.setBackgroundColor(activeStrokeColor);
                btn.setTextColor(0xFF09090E);
            } else {
                btn.setBackgroundColor(0xFF161622);
                btn.setTextColor(0xFF88888F);
            }
        }
    }

    private void setupThemeColors() {
        btnThemeNeon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyThemePreset(0xFF00E676, 0xFF0B2418, 0xFF0D1D16, 0xFF00E676);
            }
        });

        btnThemeCosmic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyThemePreset(0xFF00B0FF, 0xFF0A192F, 0xFF0C1625, 0xFF00E5FF);
            }
        });

        btnThemeGold.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyThemePreset(0xFFFFD700, 0xFF2A240C, 0xFF1D1B10, 0xFFFFE57F);
            }
        });

        btnThemeCyber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyThemePreset(0xFFFF2D55, 0xFF2D0B14, 0xFF200B10, 0xFFFF3B30);
            }
        });
    }

    private void applyThemePreset(int stroke, int fill, int card, int particle) {
        activeStrokeColor = stroke;
        activeFillColor = fill;
        activeCardColor = card;
        activeParticleColor = particle;

        threeDCubeView.setCustomColors(activeStrokeColor, activeFillColor, activeParticleColor);

        GradientDrawable gd = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[] { activeCardColor, adjustColorBrightness(activeCardColor, 0.4f) }
        );
        gd.setCornerRadius(dpToPx(16));
        gd.setStroke(dpToPx(2), activeStrokeColor);
        cardInnerLayout.setBackground(gd);

        if (containerCardView.getVisibility() == View.VISIBLE) {
            updateTabStyles(tabCard);
        } else if (threeDCubeView.getVisibility() == View.VISIBLE) {
            if (shapeSelectorGroup.getVisibility() == View.VISIBLE) {
                updateTabStyles(tabCube);
                if (btnCubeShape.getTextColors().getDefaultColor() == 0xFF09090E) {
                    updateShapeTabStyle(btnCubeShape);
                } else if (btnPyramidShape.getTextColors().getDefaultColor() == 0xFF09090E) {
                    updateShapeTabStyle(btnPyramidShape);
                } else if (btnOctaShape.getTextColors().getDefaultColor() == 0xFF09090E) {
                    updateShapeTabStyle(btnOctaShape);
                }
            } else {
                updateTabStyles(tabSphere);
            }
        }
    }

    private int adjustColorBrightness(int color, float factor) {
        int a = Color.alpha(color);
        int r = Math.min(255, (int) (Color.red(color) * factor));
        int g = Math.min(255, (int) (Color.green(color) * factor));
        int b = Math.min(255, (int) (Color.blue(color) * factor));
        return Color.argb(a, r, g, b);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }
}