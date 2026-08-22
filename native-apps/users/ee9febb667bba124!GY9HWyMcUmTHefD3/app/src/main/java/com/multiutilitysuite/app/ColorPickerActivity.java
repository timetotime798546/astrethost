package com.multiutilitysuite.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class ColorPickerActivity extends Activity {
    private View colorPreview;
    private SeekBar sbRed, sbGreen, sbBlue;
    private TextView textHex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout parent = new LinearLayout(this);
        parent.setOrientation(LinearLayout.VERTICAL);
        parent.setBackgroundColor(Color.parseColor("#F5F5F5"));

        // Custom Header Bar
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setBackgroundColor(Color.parseColor("#3F51B5"));
        header.setPadding(32, 24, 32, 24);
        header.setGravity(Gravity.CENTER_VERTICAL);

        Button backButton = new Button(this);
        backButton.setText("< Back");
        backButton.setTextColor(Color.WHITE);
        backButton.setBackgroundColor(Color.TRANSPARENT);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        header.addView(backButton);

        TextView headerTitle = new TextView(this);
        headerTitle.setText("Color Palette Mixer");
        headerTitle.setTextColor(Color.WHITE);
        headerTitle.setTextSize(20);
        headerTitle.setPadding(32, 0, 0, 0);
        header.addView(headerTitle);
        parent.addView(header);

        // Color Preview Square Block
        colorPreview = new View(this);
        colorPreview.setBackgroundColor(Color.rgb(128, 128, 128));
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 350);
        previewParams.setMargins(32, 32, 32, 32);
        colorPreview.setLayoutParams(previewParams);
        parent.addView(colorPreview);

        // HEX representation label
        textHex = new TextView(this);
        textHex.setText("HEX: #808080");
        textHex.setTextSize(20);
        textHex.setGravity(Gravity.CENTER);
        textHex.setTextColor(Color.BLACK);
        parent.addView(textHex);

        // Seekbar Sliders Block
        LinearLayout slidersLayout = new LinearLayout(this);
        slidersLayout.setOrientation(LinearLayout.VERTICAL);
        slidersLayout.setPadding(32, 16, 32, 16);

        sbRed = createLabeledSeekBar(slidersLayout, "Red", 128);
        sbGreen = createLabeledSeekBar(slidersLayout, "Green", 128);
        sbBlue = createLabeledSeekBar(slidersLayout, "Blue", 128);

        parent.addView(slidersLayout);
        setContentView(parent);
        updateDynamicPalette();
    }

    private SeekBar createLabeledSeekBar(LinearLayout container, final String colorName, int initial) {
        TextView label = new TextView(this);
        label.setText(colorName + ": " + initial);
        label.setTextSize(14);
        container.addView(label);

        SeekBar seek = new SeekBar(this);
        seek.setMax(255);
        seek.setProgress(initial);
        seek.setPadding(0, 16, 0, 32);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateDynamicPalette();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        container.addView(seek);
        return seek;
    }

    private void updateDynamicPalette() {
        int r = sbRed.getProgress();
        int g = sbGreen.getProgress();
        int b = sbBlue.getProgress();
        
        colorPreview.setBackgroundColor(Color.rgb(r, g, b));
        String hexValue = String.format("#%02X%02X%02X", r, g, b);
        textHex.setText("HEX: " + hexValue);
    }
}