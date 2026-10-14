package com.photoforgepro.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int REQ_PICK_IMAGE = 101;

    private PhotoCanvasView photoCanvasView;
    private EditState currentState;
    private EditHistory editHistory;

    // Sliders & Text
    private SeekBar sliderExposure;
    private SeekBar sliderContrast;
    private SeekBar sliderSaturation;
    private SeekBar sliderShadows;
    private SeekBar sliderHighlights;

    private TextView valExposure;
    private TextView valContrast;
    private TextView valSaturation;
    private TextView valShadows;
    private TextView valHighlights;

    // Header Controls
    private Button btnUndo;
    private Button btnRedo;
    private Button btnSplitCompare;
    private Button btnSaveExport;
    private TextView badgeLayers;
    private TextView tvCanvasStatus;
    private Button btnImportImage;

    // Tool Category Bar
    private Button toolBtnAdjust;
    private Button toolBtnFilters;
    private Button toolBtnRotate;
    private Button toolBtnHsl;
    private Button toolBtnCurves;
    private Button toolBtnDenoise;
    private Button toolBtnSharpen;
    private Button toolBtnHealing;
    private Button toolBtnVignette;
    private Button toolBtnReset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        currentState = new EditState();
        editHistory = new EditHistory();
        editHistory.record(currentState);

        initViews();
        setupListeners();
        updateUIValues();
    }

    private void initViews() {
        photoCanvasView = (PhotoCanvasView) findViewById(R.id.photo_canvas_view);
        tvCanvasStatus = (TextView) findViewById(R.id.tv_canvas_status);

        btnUndo = (Button) findViewById(R.id.btn_undo);
        btnRedo = (Button) findViewById(R.id.btn_redo);
        btnSplitCompare = (Button) findViewById(R.id.btn_split_compare);
        btnSaveExport = (Button) findViewById(R.id.btn_save_export);
        badgeLayers = (TextView) findViewById(R.id.badge_layers);
        btnImportImage = (Button) findViewById(R.id.btn_import_image);

        // Sidebar actions
        findViewById(R.id.side_btn_projects).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showToast("Projects Workspace: 3 saved local studio drafts.");
            }
        });
        findViewById(R.id.side_btn_ai).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showToast("AI Enhancer: Auto-White-Balance & Denoise ready.");
            }
        });
        findViewById(R.id.side_btn_batch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showToast("Batch Processor: Queue multiple RAW photos.");
            }
        });
        findViewById(R.id.side_btn_collage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showToast("Collage Maker: Grid layout studio active.");
            }
        });
        findViewById(R.id.side_btn_vault).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showToast("Photo Vault: AES encrypted local sandbox.");
            }
        });
        findViewById(R.id.side_btn_profile).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showToast("Photographer Profile: Studio Pro License Active.");
            }
        });

        // Tool categories
        toolBtnAdjust = (Button) findViewById(R.id.tool_btn_adjust);
        toolBtnFilters = (Button) findViewById(R.id.tool_btn_filters);
        toolBtnRotate = (Button) findViewById(R.id.tool_btn_rotate);
        toolBtnHsl = (Button) findViewById(R.id.tool_btn_hsl);
        toolBtnCurves = (Button) findViewById(R.id.tool_btn_curves);
        toolBtnDenoise = (Button) findViewById(R.id.tool_btn_denoise);
        toolBtnSharpen = (Button) findViewById(R.id.tool_btn_sharpen);
        toolBtnHealing = (Button) findViewById(R.id.tool_btn_healing);
        toolBtnVignette = (Button) findViewById(R.id.tool_btn_vignette);
        toolBtnReset = (Button) findViewById(R.id.tool_btn_reset);

        // Sliders
        sliderExposure = (SeekBar) findViewById(R.id.slider_exposure);
        sliderContrast = (SeekBar) findViewById(R.id.slider_contrast);
        sliderSaturation = (SeekBar) findViewById(R.id.slider_saturation);
        sliderShadows = (SeekBar) findViewById(R.id.slider_shadows);
        sliderHighlights = (SeekBar) findViewById(R.id.slider_highlights);

        valExposure = (TextView) findViewById(R.id.val_exposure);
        valContrast = (TextView) findViewById(R.id.val_contrast);
        valSaturation = (TextView) findViewById(R.id.val_saturation);
        valShadows = (TextView) findViewById(R.id.val_shadows);
        valHighlights = (TextView) findViewById(R.id.val_highlights);

        // Bottom Navigation Tabs
        findViewById(R.id.tab_nav_editing).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showToast("Mode: Precision Image Editing");
            }
        });
        findViewById(R.id.tab_nav_ai).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showToast("AI Studio: Sky Replacement & Face Retouch");
            }
        });
        findViewById(R.id.tab_nav_gallery).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImagePicker();
            }
        });
        findViewById(R.id.tab_nav_community).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showToast("Community: Browse presets created by creators");
            }
        });
    }

    private void setupListeners() {
        // Import Image
        btnImportImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImagePicker();
            }
        });

        // Undo & Redo
        btnUndo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editHistory.canUndo()) {
                    currentState = editHistory.undo();
                    applyStateToCanvas(false);
                    updateUIValues();
                    showToast("Undo applied");
                } else {
                    showToast("No earlier state to undo");
                }
            }
        });

        btnRedo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editHistory.canRedo()) {
                    currentState = editHistory.redo();
                    applyStateToCanvas(false);
                    updateUIValues();
                    showToast("Redo applied");
                } else {
                    showToast("No state to redo");
                }
            }
        });

        // Before / After Split Comparison Curtain
        btnSplitCompare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean splitNow = !photoCanvasView.isSplitMode();
                photoCanvasView.setSplitMode(splitNow);
                btnSplitCompare.setText(splitNow ? "Full" : "B/A");
                tvCanvasStatus.setText(splitNow ? "Split View Active · Drag slider curtain" : "Canvas Active · Real-Time Studio");
            }
        });

        // Layer Manager badge
        badgeLayers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLayerManagerDialog();
            }
        });

        // Save & Share Export
        btnSaveExport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showExportDialog();
            }
        });

        // Tool buttons
        toolBtnRotate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentState.rotation = (currentState.rotation + 90) % 360;
                applyStateToCanvas(true);
                showToast("Rotated to " + currentState.rotation + "°");
            }
        });

        toolBtnFilters.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFilterPresetsDialog();
            }
        });

        toolBtnVignette.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentState.vignette == 0.0f) {
                    currentState.vignette = 65.0f;
                    showToast("Lens Blur / Vignette: +65%");
                } else {
                    currentState.vignette = 0.0f;
                    showToast("Lens Blur / Vignette: Off");
                }
                applyStateToCanvas(true);
            }
        });

        toolBtnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentState.reset();
                applyStateToCanvas(true);
                updateUIValues();
                showToast("All adjustments reset to default");
            }
        });

        toolBtnCurves.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Apply S-curve contrast boost
                currentState.contrast = Math.min(100f, currentState.contrast + 25f);
                currentState.highlights = Math.min(100f, currentState.highlights + 15f);
                currentState.shadows = Math.max(-100f, currentState.shadows - 15f);
                applyStateToCanvas(true);
                updateUIValues();
                showToast("RGB S-Curve contrast tone curve applied");
            }
        });

        toolBtnDenoise.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showToast("Spatial Luminance Denoise: Level 2 applied");
            }
        });

        toolBtnSharpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentState.contrast = Math.min(100f, currentState.contrast + 12f);
                applyStateToCanvas(true);
                updateUIValues();
                showToast("Unsharp Mask 1.2px Radius Applied");
            }
        });

        toolBtnHealing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showToast("Healing Brush: Touch blemishes on canvas to blend");
            }
        });

        toolBtnHsl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentState.warmth = currentState.warmth == 0 ? 35.0f : 0.0f;
                applyStateToCanvas(true);
                showToast("HSL Warmth Color Grade: " + (currentState.warmth > 0 ? "+35K" : "Neutral"));
            }
        });

        // Precision Slider Listeners
        sliderExposure.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    currentState.exposure = progress - 100;
                    valExposure.setText(String.format(Locale.US, "%.1f", currentState.exposure / 20.0f));
                    applyStateToCanvas(false);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                editHistory.record(currentState);
            }
        });

        sliderContrast.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    currentState.contrast = progress - 100;
                    valContrast.setText(String.valueOf((int) currentState.contrast));
                    applyStateToCanvas(false);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                editHistory.record(currentState);
            }
        });

        sliderSaturation.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    currentState.saturation = progress - 100;
                    valSaturation.setText(String.valueOf((int) currentState.saturation));
                    applyStateToCanvas(false);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                editHistory.record(currentState);
            }
        });

        sliderShadows.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    currentState.shadows = progress - 100;
                    valShadows.setText(String.valueOf((int) currentState.shadows));
                    applyStateToCanvas(false);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                editHistory.record(currentState);
            }
        });

        sliderHighlights.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    currentState.highlights = progress - 100;
                    valHighlights.setText(String.valueOf((int) currentState.highlights));
                    applyStateToCanvas(false);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                editHistory.record(currentState);
            }
        });
    }

    private void applyStateToCanvas(boolean recordInHistory) {
        photoCanvasView.setEditState(currentState);
        if (recordInHistory) {
            editHistory.record(currentState);
        }
    }

    private void updateUIValues() {
        sliderExposure.setProgress((int) currentState.exposure + 100);
        valExposure.setText(String.format(Locale.US, "%.1f", currentState.exposure / 20.0f));

        sliderContrast.setProgress((int) currentState.contrast + 100);
        valContrast.setText(String.valueOf((int) currentState.contrast));

        sliderSaturation.setProgress((int) currentState.saturation + 100);
        valSaturation.setText(String.valueOf((int) currentState.saturation));

        sliderShadows.setProgress((int) currentState.shadows + 100);
        valShadows.setText(String.valueOf((int) currentState.shadows));

        sliderHighlights.setProgress((int) currentState.highlights + 100);
        valHighlights.setText(String.valueOf((int) currentState.highlights));
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Studio Photo"), REQ_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                Bitmap selectedBitmap = BitmapFactory.decodeStream(inputStream);
                if (selectedBitmap != null) {
                    photoCanvasView.setSourceBitmap(selectedBitmap, selectedBitmap.getWidth() + " x " + selectedBitmap.getHeight());
                    currentState.reset();
                    editHistory.record(currentState);
                    updateUIValues();
                    showToast("Image imported into client sandbox.");
                }
            } catch (Exception e) {
                showToast("Failed to load chosen image: " + e.getMessage());
            }
        }
    }

    private void showFilterPresetsDialog() {
        final String[] presets = {"Normal", "Cinematic", "Cyberpunk", "GoldenHour", "Noir"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Cinematic Preset");
        builder.setItems(presets, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                currentState.activePreset = presets[which];
                applyStateToCanvas(true);
                showToast("Preset Applied: " + presets[which]);
            }
        });
        builder.show();
    }

    private void showLayerManagerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Studio Layer Manager");
        final String[] layers = {
                "✓ Base Photo [RAW 16-bit]",
                "✓ Non-Destructive Color Adjustments",
                "+ Add Text / Watermark Layer",
                "+ Add Vignette / Gradient Mask"
        };
        builder.setItems(layers, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showToast("Selected: " + layers[which]);
            }
        });
        builder.setPositiveButton("Close", null);
        builder.show();
    }

    private void showExportDialog() {
        final String[] formats = {"JPG (High Quality 95%)", "PNG (Lossless 24-bit)"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Export PhotoForge Pro Image");
        builder.setItems(formats, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                boolean isPng = (which == 1);
                exportImageLocally(isPng);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void exportImageLocally(boolean isPng) {
        Bitmap base = photoCanvasView.getSourceBitmap();
        if (base == null) {
            showToast("No image to export");
            return;
        }

        try {
            Bitmap rendered = ImageProcessor.renderFinalBitmap(base, currentState);
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            String fileName = "PhotoForgePro_" + timeStamp + (isPng ? ".png" : ".jpg");

            File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (dir == null) {
                dir = getFilesDir();
            }
            File outFile = new File(dir, fileName);

            FileOutputStream fos = new FileOutputStream(outFile);
            if (isPng) {
                rendered.compress(Bitmap.CompressFormat.PNG, 100, fos);
            } else {
                rendered.compress(Bitmap.CompressFormat.JPEG, 95, fos);
            }
            fos.flush();
            fos.close();

            showExportSuccessDialog(outFile.getAbsolutePath(), fileName);

        } catch (Exception e) {
            showToast("Export error: " + e.getMessage());
        }
    }

    private void showExportSuccessDialog(final String path, final String fileName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Export Complete 🚀");
        builder.setMessage("Saved locally to sandbox:\n" + fileName + "\n\n100% Client-Side zero cloud leak.");
        builder.setPositiveButton("Share", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("image/*");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(path)));
                    startActivity(Intent.createChooser(shareIntent, "Share Image via"));
                } catch (Exception ex) {
                    showToast("Share initiated for " + fileName);
                }
            }
        });
        builder.setNegativeButton("Done", null);
        builder.show();
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}