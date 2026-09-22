package com.luxelight.app;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity {

    private CameraManager cameraManager;
    private String cameraId;
    private boolean isFlashOn = false;

    // UI elements
    private Button btnPower;
    private ToggleButton toggleStrobe;
    private ToggleButton toggleSos;
    private SeekBar seekStrobe;
    private TextView txtStrobeFreq;
    private TextView txtBrightness;
    private SeekBar seekBrightness;
    private LinearLayout panelColorPreview;
    private Button btnFullscreenBeam;
    private View viewDiscoVisualizer;
    private TextView txtPartySpeed;
    private SeekBar seekPartySpeed;
    private CheckBox checkSyncFlash;
    private Button btnPartyStart;

    private LinearLayout tabTorch;
    private LinearLayout tabScreen;
    private LinearLayout tabParty;

    private TextView txtTabTorch;
    private TextView txtTabScreen;
    private TextView txtTabParty;

    private View viewTorchContent;
    private View viewScreenLightContent;
    private View viewPartyContent;
    private RelativeLayout layoutFullscreenOverlay;

    // Execution Helpers
    private Handler handler = new Handler();
    
    // Strobe Variables
    private boolean isStrobeRunning = false;
    private int strobeDelayMs = 100; // Corresponding to 5Hz default

    // SOS Variables
    private boolean isSosRunning = false;
    private int sosIndex = 0;
    // Morse code pattern mapping sequence (1 = ON, 0 = OFF)
    private final int[] sosPattern = {
        1, 0, 1, 0, 1, 0,                      // S (...): ON, OFF, ON, OFF, ON, OFF
        0, 0,                                  // Interval
        1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 0,    // O (---): ON (3 periods), OFF (1 period)
        0, 0,                                  // Interval
        1, 0, 1, 0, 1, 0,                      // S (...): ON, OFF, ON, OFF, ON, OFF
        0, 0, 0, 0                             // Cycle delay
    };

    // Screen color configurations
    private int selectedColor = Color.WHITE;
    private float originalBrightness = -1f;

    // Disco parameters
    private boolean isPartyRunning = false;
    private int partyDelayMs = 250;
    private int partyIndex = 0;
    private final int[] partyColors = {
        Color.parseColor("#FF1744"), // Vibrant Red
        Color.parseColor("#FFD600"), // Radiant Yellow
        Color.parseColor("#00E676"), // Emerald Green
        Color.parseColor("#00B0FF"), // Neon Cyan
        Color.parseColor("#D500F9"), // Electric Magenta
        Color.parseColor("#3D5AFE"), // Hyper Blue
        Color.parseColor("#FF9100")  // Safety Orange
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Fetch original window parameters
        originalBrightness = getWindow().getAttributes().screenBrightness;

        initViews();
        setupTabs();
        setupListeners();
        initCamera();

        // Check runtime permission if on Marshmallow+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 101);
            }
        }
    }

    private void initViews() {
        btnPower = (Button) findViewById(R.id.btn_power);
        toggleStrobe = (ToggleButton) findViewById(R.id.toggle_strobe);
        toggleSos = (ToggleButton) findViewById(R.id.toggle_sos);
        seekStrobe = (SeekBar) findViewById(R.id.seek_strobe);
        txtStrobeFreq = (TextView) findViewById(R.id.txt_strobe_freq);
        txtBrightness = (TextView) findViewById(R.id.txt_brightness);
        seekBrightness = (SeekBar) findViewById(R.id.seek_brightness);
        panelColorPreview = (LinearLayout) findViewById(R.id.panel_color_preview);
        btnFullscreenBeam = (Button) findViewById(R.id.btn_fullscreen_beam);
        viewDiscoVisualizer = findViewById(R.id.view_disco_visualizer);
        txtPartySpeed = (TextView) findViewById(R.id.txt_party_speed);
        seekPartySpeed = (SeekBar) findViewById(R.id.seek_party_speed);
        checkSyncFlash = (CheckBox) findViewById(R.id.check_sync_flash);
        btnPartyStart = (Button) findViewById(R.id.btn_party_start);

        tabTorch = (LinearLayout) findViewById(R.id.tab_torch);
        tabScreen = (LinearLayout) findViewById(R.id.tab_screen);
        tabParty = (LinearLayout) findViewById(R.id.tab_party);

        txtTabTorch = (TextView) findViewById(R.id.txt_tab_torch);
        txtTabScreen = (TextView) findViewById(R.id.txt_tab_screen);
        txtTabParty = (TextView) findViewById(R.id.txt_tab_party);

        viewTorchContent = findViewById(R.id.view_torch);
        viewScreenLightContent = findViewById(R.id.view_screen_light);
        viewPartyContent = findViewById(R.id.view_party);
        layoutFullscreenOverlay = (RelativeLayout) findViewById(R.id.layout_fullscreen_overlay);
    }

    private void setupTabs() {
        tabTorch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(1);
            }
        });

        tabScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(2);
            }
        });

        tabParty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(3);
            }
        });
    }

    private void switchTab(int tabId) {
        stopStrobe();
        stopSos();
        stopParty();

        tabTorch.setBackgroundColor(Color.parseColor("#1E1E1E"));
        tabScreen.setBackgroundColor(Color.parseColor("#1E1E1E"));
        tabParty.setBackgroundColor(Color.parseColor("#1E1E1E"));

        txtTabTorch.setTextColor(Color.parseColor("#8A8A8F"));
        txtTabScreen.setTextColor(Color.parseColor("#8A8A8F"));
        txtTabParty.setTextColor(Color.parseColor("#8A8A8F"));

        viewTorchContent.setVisibility(View.GONE);
        viewScreenLightContent.setVisibility(View.GONE);
        viewPartyContent.setVisibility(View.GONE);

        if (tabId == 1) {
            tabTorch.setBackgroundColor(Color.parseColor("#2E2E2E"));
            txtTabTorch.setTextColor(Color.parseColor("#FFD700"));
            viewTorchContent.setVisibility(View.VISIBLE);
        } else if (tabId == 2) {
            tabScreen.setBackgroundColor(Color.parseColor("#2E2E2E"));
            txtTabScreen.setTextColor(Color.parseColor("#00BFFF"));
            viewScreenLightContent.setVisibility(View.VISIBLE);
        } else if (tabId == 3) {
            tabParty.setBackgroundColor(Color.parseColor("#2E2E2E"));
            txtTabParty.setTextColor(Color.parseColor("#FF4D4D"));
            viewPartyContent.setVisibility(View.VISIBLE);
        }
    }

    private void setupListeners() {
        btnPower.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isFlashOn) {
                    setFlashlight(false);
                    btnPower.setText("OFF");
                    btnPower.setBackgroundResource(R.drawable.btn_round_off);
                } else {
                    stopStrobe();
                    stopSos();
                    setFlashlight(true);
                    btnPower.setText("ON");
                    btnPower.setBackgroundResource(R.drawable.btn_round_on);
                }
            }
        });

        seekStrobe.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int hz = progress + 1; // From 1Hz to 20Hz
                txtStrobeFreq.setText("Frequency: " + hz + " Hz");
                strobeDelayMs = 1000 / (hz * 2);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        toggleStrobe.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    stopSos();
                    if (isFlashOn) {
                        setFlashlight(false);
                        btnPower.setText("OFF");
                        btnPower.setBackgroundResource(R.drawable.btn_round_off);
                    }
                    isStrobeRunning = true;
                    handler.post(strobeRunnable);
                } else {
                    stopStrobe();
                    setFlashlight(false);
                }
            }
        });

        toggleSos.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    stopStrobe();
                    if (isFlashOn) {
                        setFlashlight(false);
                        btnPower.setText("OFF");
                        btnPower.setBackgroundResource(R.drawable.btn_round_off);
                    }
                    isSosRunning = true;
                    sosIndex = 0;
                    handler.post(sosRunnable);
                } else {
                    stopSos();
                    setFlashlight(false);
                }
            }
        });

        // Color selection triggers
        findViewById(R.id.btn_color_white).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { selectScreenColor(Color.WHITE); }
        });
        findViewById(R.id.btn_color_yellow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { selectScreenColor(Color.parseColor("#FFEB3B")); }
        });
        findViewById(R.id.btn_color_orange).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { selectScreenColor(Color.parseColor("#FF9800")); }
        });
        findViewById(R.id.btn_color_red).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { selectScreenColor(Color.parseColor("#E53935")); }
        });
        findViewById(R.id.btn_color_green).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { selectScreenColor(Color.parseColor("#4CAF50")); }
        });
        findViewById(R.id.btn_color_cyan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { selectScreenColor(Color.parseColor("#00BCD4")); }
        });
        findViewById(R.id.btn_color_blue).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { selectScreenColor(Color.parseColor("#2196F3")); }
        });
        findViewById(R.id.btn_color_purple).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { selectScreenColor(Color.parseColor("#9C27B0")); }
        });

        seekBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float val = progress / 100f;
                if (val < 0.05f) val = 0.05f; // Maintain minimum screen glow
                txtBrightness.setText("Screen Brightness: " + progress + "%");

                WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
                layoutParams.screenBrightness = val;
                getWindow().setAttributes(layoutParams);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnFullscreenBeam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutFullscreenOverlay.setVisibility(View.VISIBLE);
                layoutFullscreenOverlay.setBackgroundColor(selectedColor);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    layoutFullscreenOverlay.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    );
                }
            }
        });

        layoutFullscreenOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutFullscreenOverlay.setVisibility(View.GONE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    layoutFullscreenOverlay.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
                }
            }
        });

        seekPartySpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                String desc = "Standard";
                if (progress == 0) {
                    partyDelayMs = 600;
                    desc = "Very Slow";
                } else if (progress == 1) {
                    partyDelayMs = 400;
                    desc = "Slow";
                } else if (progress == 2) {
                    partyDelayMs = 250;
                    desc = "Standard";
                } else if (progress == 3) {
                    partyDelayMs = 120;
                    desc = "Fast";
                } else if (progress == 4) {
                    partyDelayMs = 70;
                    desc = "Turbo";
                }
                txtPartySpeed.setText("Disco Pulse Speed: " + desc);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnPartyStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPartyRunning) {
                    stopParty();
                } else {
                    startParty();
                }
            }
        });
    }

    private void selectScreenColor(int color) {
        selectedColor = color;
        panelColorPreview.setBackgroundColor(color);
        if (layoutFullscreenOverlay.getVisibility() == View.VISIBLE) {
            layoutFullscreenOverlay.setBackgroundColor(color);
        }
    }

    private void initCamera() {
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] list = cameraManager.getCameraIdList();
            for (String id : list) {
                CameraCharacteristics chars = cameraManager.getCameraCharacteristics(id);
                Boolean hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if (hasFlash != null && hasFlash) {
                    cameraId = id;
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Hardware camera flash missing or unavailable.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setFlashlight(boolean turnOn) {
        if (cameraId == null) return;
        try {
            cameraManager.setTorchMode(cameraId, turnOn);
            isFlashOn = turnOn;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Runnable Tasks
    private Runnable strobeRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isStrobeRunning) return;
            setFlashlight(!isFlashOn);
            handler.postDelayed(this, strobeDelayMs);
        }
    };

    private Runnable sosRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isSosRunning) return;
            boolean level = (sosPattern[sosIndex] == 1);
            setFlashlight(level);
            sosIndex = (sosIndex + 1) % sosPattern.length;
            handler.postDelayed(this, 200);
        }
    };

    private Runnable partyRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isPartyRunning) return;
            int col = partyColors[partyIndex];
            viewDiscoVisualizer.setBackgroundColor(col);

            if (checkSyncFlash.isChecked()) {
                setFlashlight(!isFlashOn);
            }

            partyIndex = (partyIndex + 1) % partyColors.length;
            handler.postDelayed(this, partyDelayMs);
        }
    };

    private void stopStrobe() {
        isStrobeRunning = false;
        handler.removeCallbacks(strobeRunnable);
        toggleStrobe.setChecked(false);
    }

    private void stopSos() {
        isSosRunning = false;
        handler.removeCallbacks(sosRunnable);
        toggleSos.setChecked(false);
    }

    private void startParty() {
        stopStrobe();
        stopSos();
        isPartyRunning = true;
        partyIndex = 0;
        btnPartyStart.setText("STOP PARTY");
        btnPartyStart.setBackgroundColor(Color.parseColor("#E53935"));
        handler.post(partyRunnable);
    }

    private void stopParty() {
        isPartyRunning = false;
        handler.removeCallbacks(partyRunnable);
        btnPartyStart.setText("START PARTY");
        btnPartyStart.setBackgroundColor(Color.parseColor("#4CAF50"));
        setFlashlight(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopStrobe();
        stopSos();
        stopParty();
        setFlashlight(false);
        
        btnPower.setText("OFF");
        btnPower.setBackgroundResource(R.drawable.btn_round_off);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (originalBrightness >= 0) {
            WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
            layoutParams.screenBrightness = originalBrightness;
            getWindow().setAttributes(layoutParams);
        }
    }
}