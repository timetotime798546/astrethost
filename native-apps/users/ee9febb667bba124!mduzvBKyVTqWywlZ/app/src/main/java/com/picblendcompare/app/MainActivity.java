package com.picblendcompare.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private ImageSandboxView sandboxView;
    private Button btnSideBySide;
    private Button btnSplitSlider;
    private Button btnAlphaBlend;
    
    private View layoutSliderControl;
    private SeekBar seekBarControl;
    private TextView txtSliderLabel;
    
    private Button btnFilterNormal;
    private Button btnFilterGrayscale;
    private Button btnFilterSepia;
    private Button btnFilterInvert;
    
    private Button btnRotate;
    private Button btnSwap;
    private Button btnReset;
    
    private TextView txtInfoA;
    private TextView txtInfoB;
    
    private int currentMode = 0; // 0: side, 1: split, 2: blend
    private int currentFilter = 0; // 0: normal, 1: gray, 2: sepia, 3: invert
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        sandboxView = (ImageSandboxView) findViewById(R.id.sandbox_view);
        btnSideBySide = (Button) findViewById(R.id.btn_mode_side);
        btnSplitSlider = (Button) findViewById(R.id.btn_mode_split);
        btnAlphaBlend = (Button) findViewById(R.id.btn_mode_blend);
        
        layoutSliderControl = findViewById(R.id.layout_slider_control);
        seekBarControl = (SeekBar) findViewById(R.id.seekbar_control);
        txtSliderLabel = (TextView) findViewById(R.id.txt_slider_label);
        
        btnFilterNormal = (Button) findViewById(R.id.btn_filter_normal);
        btnFilterGrayscale = (Button) findViewById(R.id.btn_filter_gray);
        btnFilterSepia = (Button) findViewById(R.id.btn_filter_sepia);
        btnFilterInvert = (Button) findViewById(R.id.btn_filter_invert);
        
        btnRotate = (Button) findViewById(R.id.btn_rotate);
        btnSwap = (Button) findViewById(R.id.btn_swap);
        btnReset = (Button) findViewById(R.id.btn_reset);
        
        txtInfoA = (TextView) findViewById(R.id.txt_info_a);
        txtInfoB = (TextView) findViewById(R.id.txt_info_b);
        
        // Tab listeners
        if (btnSideBySide != null) {
            btnSideBySide.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchMode(0);
                }
            });
        }
        
        if (btnSplitSlider != null) {
            btnSplitSlider.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchMode(1);
                }
            });
        }
        
        if (btnAlphaBlend != null) {
            btnAlphaBlend.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchMode(2);
                }
            });
        }
        
        // Seekbar control with user-triggered checks to avoid feedback loops
        if (seekBarControl != null) {
            seekBarControl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    float val = progress / 100f;
                    if (currentMode == 1) {
                        if (fromUser && sandboxView != null) {
                            sandboxView.setSplitRatio(val);
                        }
                        if (txtSliderLabel != null) {
                            txtSliderLabel.setText("Split Divider: " + progress + "% (Or drag canvas directly)");
                        }
                    } else if (currentMode == 2) {
                        if (fromUser && sandboxView != null) {
                            sandboxView.setBlendAlpha(val);
                        }
                        if (txtSliderLabel != null) {
                            txtSliderLabel.setText("Blend Opacity: " + progress + "%");
                        }
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }
        
        // Touch to drag split slider callback
        if (sandboxView != null) {
            sandboxView.setOnSliderChangeListener(new ImageSandboxView.OnSliderChangeListener() {
                @Override
                public void onSliderChanged(float ratio) {
                    if (currentMode == 1 && seekBarControl != null) {
                        int progress = (int) (ratio * 100);
                        seekBarControl.setProgress(progress);
                        if (txtSliderLabel != null) {
                            txtSliderLabel.setText("Split Divider: " + progress + "% (Or drag canvas directly)");
                        }
                    }
                }
            });
        }
        
        // Color Filters
        if (btnFilterNormal != null) {
            btnFilterNormal.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    applyFilter(0);
                }
            });
        }
        if (btnFilterGrayscale != null) {
            btnFilterGrayscale.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    applyFilter(1);
                }
            });
        }
        if (btnFilterSepia != null) {
            btnFilterSepia.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    applyFilter(2);
                }
            });
        }
        if (btnFilterInvert != null) {
            btnFilterInvert.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    applyFilter(3);
                }
            });
        }
        
        // Rotating & Swap operations
        if (btnRotate != null) {
            btnRotate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (sandboxView != null) {
                        sandboxView.rotateImages();
                        updateMetadata();
                        Toast.makeText(MainActivity.this, "Images Rotated 90°", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
        
        if (btnSwap != null) {
            btnSwap.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (sandboxView != null) {
                        sandboxView.swapImages();
                        updateMetadata();
                        Toast.makeText(MainActivity.this, "Swapped Left/Right positions", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
        
        if (btnReset != null) {
            btnReset.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    resetAll();
                }
            });
        }
        
        switchMode(0);
        applyFilter(0);
        updateMetadata();
    }
    
    private void switchMode(int mode) {
        currentMode = mode;
        if (btnSideBySide != null) btnSideBySide.setSelected(mode == 0);
        if (btnSplitSlider != null) btnSplitSlider.setSelected(mode == 1);
        if (btnAlphaBlend != null) btnAlphaBlend.setSelected(mode == 2);
        
        if (sandboxView != null) {
            sandboxView.setMode(mode);
        }
        
        if (mode == 0) {
            if (layoutSliderControl != null) layoutSliderControl.setVisibility(View.GONE);
        } else if (mode == 1) {
            if (layoutSliderControl != null) layoutSliderControl.setVisibility(View.VISIBLE);
            if (seekBarControl != null) seekBarControl.setProgress(50);
            if (sandboxView != null) sandboxView.setSplitRatio(0.5f);
            if (txtSliderLabel != null) txtSliderLabel.setText("Split Divider: 50% (Or drag canvas directly)");
        } else if (mode == 2) {
            if (layoutSliderControl != null) layoutSliderControl.setVisibility(View.VISIBLE);
            if (seekBarControl != null) seekBarControl.setProgress(50);
            if (sandboxView != null) sandboxView.setBlendAlpha(0.5f);
            if (txtSliderLabel != null) txtSliderLabel.setText("Blend Opacity: 50%");
        }
        updateMetadata();
    }
    
    private void applyFilter(int filter) {
        currentFilter = filter;
        
        if (btnFilterNormal != null) btnFilterNormal.setSelected(filter == 0);
        if (btnFilterGrayscale != null) btnFilterGrayscale.setSelected(filter == 1);
        if (btnFilterSepia != null) btnFilterSepia.setSelected(filter == 2);
        if (btnFilterInvert != null) btnFilterInvert.setSelected(filter == 3);
        
        if (sandboxView != null) {
            sandboxView.setFilterType(filter);
        }
        updateMetadata();
    }
    
    private void resetAll() {
        switchMode(0);
        applyFilter(0);
        if (sandboxView != null) {
            if (sandboxView.isSwapped()) {
                sandboxView.swapImages();
            }
            while (sandboxView.getRotationAngle() != 0) {
                sandboxView.rotateImages();
            }
        }
        updateMetadata();
        Toast.makeText(this, "All settings restored to default", Toast.LENGTH_SHORT).show();
    }
    
    private void updateMetadata() {
        if (sandboxView == null) return;
        
        String filterName = "None";
        if (currentFilter == 1) filterName = "Grayscale";
        else if (currentFilter == 2) filterName = "Sepia";
        else if (currentFilter == 3) filterName = "Inverted";

        String rotationText = sandboxView.getRotationAngle() + "°";

        if (!sandboxView.isSwapped()) {
            if (txtInfoA != null) txtInfoA.setText("Left/Full (Slot A): asset_6a9efd3f547c3.png | Filter: " + filterName + " | Rotation: " + rotationText);
            if (txtInfoB != null) txtInfoB.setText("Right/Overlay (Slot B): asset_6a9efd696a0df.png | Filter: " + filterName + " | Rotation: " + rotationText);
        } else {
            if (txtInfoA != null) txtInfoA.setText("Left/Full (Slot A): asset_6a9efd696a0df.png | Filter: " + filterName + " | Rotation: " + rotationText);
            if (txtInfoB != null) txtInfoB.setText("Right/Overlay (Slot B): asset_6a9efd3f547c3.png | Filter: " + filterName + " | Rotation: " + rotationText);
        }
    }
}