package com.threedshapestudio.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends Activity {

    private Shape3DView shapeView;
    private TextView lblScale;
    private TextView lblRotationSpeed;
    private TextView lblLightX;
    private TextView lblLightY;

    // Track active selected geometry and material preset pill elements
    private Button[] shapeButtons = new Button[5];
    private Button[] colorButtons = new Button[5];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        shapeView = (Shape3DView) findViewById(R.id.shape_3d_view);
        lblScale = (TextView) findViewById(R.id.lbl_scale);
        lblRotationSpeed = (TextView) findViewById(R.id.lbl_rotation_speed);
        lblLightX = (TextView) findViewById(R.id.lbl_light_x);
        lblLightY = (TextView) findViewById(R.id.lbl_light_y);

        // Bind Shape Buttons
        shapeButtons[0] = (Button) findViewById(R.id.btn_shape_cube);
        shapeButtons[1] = (Button) findViewById(R.id.btn_shape_tetra);
        shapeButtons[2] = (Button) findViewById(R.id.btn_shape_octa);
        shapeButtons[3] = (Button) findViewById(R.id.btn_shape_prism);
        shapeButtons[4] = (Button) findViewById(R.id.btn_shape_star);

        for (int i = 0; i < shapeButtons.length; i++) {
            final int index = i;
            shapeButtons[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectShapeButton(index);
                    shapeView.setShapeIndex(index);
                }
            });
        }
        selectShapeButton(0); // Set Cube active by default

        // Bind Color Scheme Buttons
        colorButtons[0] = (Button) findViewById(R.id.btn_color_cyan);
        colorButtons[1] = (Button) findViewById(R.id.btn_color_purple);
        colorButtons[2] = (Button) findViewById(R.id.btn_color_emerald);
        colorButtons[3] = (Button) findViewById(R.id.btn_color_orange);
        colorButtons[4] = (Button) findViewById(R.id.btn_color_gold);

        for (int i = 0; i < colorButtons.length; i++) {
            final int index = i;
            colorButtons[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectColorButton(index);
                    shapeView.setColorScheme(index);
                }
            });
        }
        selectColorButton(0); // Set Neon Cyan active by default

        // Control Panel Tuning: Scale SeekBar
        SeekBar seekScale = (SeekBar) findViewById(R.id.seek_scale);
        seekScale.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float val = progress / 100.0f;
                if (val < 0.1f) val = 0.1f;
                lblScale.setText("Scale Multiplier: " + (int)(val * 100) + "%");
                shapeView.setScaleValue(val);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Control Panel Tuning: Speed SeekBar
        SeekBar seekSpeed = (SeekBar) findViewById(R.id.seek_rotation_speed);
        seekSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float speedScalar = progress / 1000.0f;
                lblRotationSpeed.setText("Auto Spin Speed: " + progress + "%");
                shapeView.setAutoRotationSpeed(speedScalar);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Control Panel Tuning: Lighting Yaw/Pitch Seekbars
        SeekBar seekLightX = (SeekBar) findViewById(R.id.seek_light_x);
        SeekBar seekLightY = (SeekBar) findViewById(R.id.seek_light_y);

        SeekBar.OnSeekBarChangeListener lightListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int px = seekLightX.getProgress();
                int py = seekLightY.getProgress();
                lblLightX.setText("Light Orientation Pitch: " + px + "°");
                lblLightY.setText("Light Orientation Yaw: " + py + "°");
                shapeView.setLightAngles(px, py);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        };
        seekLightX.setOnSeekBarChangeListener(lightListener);
        seekLightY.setOnSeekBarChangeListener(lightListener);

        // Control Panel Toggles (Checkboxes/Switches)
        CheckBox chkShading = (CheckBox) findViewById(R.id.chk_shading);
        chkShading.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                shapeView.setShadingMode(isChecked);
            }
        });

        CheckBox chkWireframe = (CheckBox) findViewById(R.id.chk_wireframe);
        chkWireframe.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                shapeView.setWireframeMode(isChecked);
            }
        });

        CheckBox chkVertices = (CheckBox) findViewById(R.id.chk_vertices);
        chkVertices.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                shapeView.setVerticesVisible(isChecked);
            }
        });

        CheckBox chkAutoX = (CheckBox) findViewById(R.id.chk_auto_x);
        chkAutoX.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                shapeView.setAutoRotateX(isChecked);
            }
        });

        CheckBox chkAutoY = (CheckBox) findViewById(R.id.chk_auto_y);
        chkAutoY.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                shapeView.setAutoRotateY(isChecked);
            }
        });

        // Quick Reset Button
        Button btnReset = (Button) findViewById(R.id.btn_reset);
        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shapeView.resetView();
                seekScale.setProgress(100);
                seekSpeed.setProgress(30);
                seekLightX.setProgress(45);
                seekLightY.setProgress(45);
                lblScale.setText("Scale Multiplier: 100%");
                lblRotationSpeed.setText("Auto Spin Speed: 30%");
                lblLightX.setText("Light Orientation Pitch: 45°");
                lblLightY.setText("Light Orientation Yaw: 45°");
            }
        });
    }

    private void selectShapeButton(int index) {
        for (int i = 0; i < shapeButtons.length; i++) {
            if (shapeButtons[i] != null) {
                shapeButtons[i].setSelected(i == index);
            }
        }
    }

    private void selectColorButton(int index) {
        for (int i = 0; i < colorButtons.length; i++) {
            if (colorButtons[i] != null) {
                colorButtons[i].setSelected(i == index);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        shapeView.startAnimating();
    }

    @Override
    protected void onPause() {
        super.onPause();
        shapeView.stopAnimating();
    }
}