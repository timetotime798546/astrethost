package com.photoeditorpro.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {

    private PhotoEditView editView;
    private Button btnImport, btnSave;
    private Button tabBtnFilters, tabBtnAdjustments, tabBtnPaint;
    private LinearLayout tabFiltersLayout, tabAdjustmentsLayout, tabPaintLayout;
    
    // Seekbars
    private SeekBar seekBrightness, seekContrast, seekBrushSize;
    private TextView txtBrightnessVal, txtContrastVal, txtBrushSizeVal;
    
    // Draw Brush State
    private Button btnToggleDraw, btnUndo, btnRedo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind Views
        editView = findViewById(R.id.photo_edit_view);
        btnImport = findViewById(R.id.btn_import);
        btnSave = findViewById(R.id.btn_save);

        tabBtnFilters = findViewById(R.id.tab_btn_filters);
        tabBtnAdjustments = findViewById(R.id.tab_btn_adjustments);
        tabBtnPaint = findViewById(R.id.tab_btn_paint);

        tabFiltersLayout = findViewById(R.id.tab_filters);
        tabAdjustmentsLayout = findViewById(R.id.tab_adjustments);
        tabPaintLayout = findViewById(R.id.tab_paint);

        seekBrightness = findViewById(R.id.seek_brightness);
        seekContrast = findViewById(R.id.seek_contrast);
        seekBrushSize = findViewById(R.id.seek_brush_size);

        txtBrightnessVal = findViewById(R.id.txt_brightness_val);
        txtContrastVal = findViewById(R.id.txt_contrast_val);
        txtBrushSizeVal = findViewById(R.id.txt_brush_size_val);

        btnToggleDraw = findViewById(R.id.btn_toggle_draw);
        btnUndo = findViewById(R.id.btn_undo);
        btnRedo = findViewById(R.id.btn_redo);

        // Setup top actions
        btnImport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImportOptionsDialog();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveOrShareImage();
            }
        });

        // Setup bottom navigation tabs switching
        tabBtnFilters.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(0);
            }
        });

        tabBtnAdjustments.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(1);
            }
        });

        tabBtnPaint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(2);
            }
        });

        // Setup Seekbars
        seekBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // progress: 0 to 200, default is 100
                float brightnessVal = (progress - 100) * 1.5f;
                txtBrightnessVal.setText(String.valueOf((int) brightnessVal));
                editView.setBrightness(brightnessVal);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekContrast.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // progress: 0 to 150, default is 50
                float contrastVal = 0.2f + (progress / 50.0f) * 0.8f;
                txtContrastVal.setText(String.format("%.1f", contrastVal));
                editView.setContrast(contrastVal);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekBrushSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float size = progress + 5f;
                txtBrushSizeVal.setText(String.valueOf((int) size));
                editView.setBrushSize(size);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Setup paint controls
        btnToggleDraw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean active = !editView.isDrawingMode();
                editView.setDrawingMode(active);
                if (active) {
                    btnToggleDraw.setText("Disable Paint");
                    btnToggleDraw.setBackgroundColor(0xFF00ADB5);
                    showToast("Paint Mode Enabled! Draw with finger.");
                } else {
                    btnToggleDraw.setText("Enable Paint");
                    btnToggleDraw.setBackgroundColor(0xFF2D2D2D);
                    showToast("Paint Mode Disabled.");
                }
            }
        });

        btnUndo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editView.undo();
            }
        });

        btnRedo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editView.redo();
            }
        });

        // Populate Presets & Colors dynamically
        setupPresets();
        setupColorPalette();

        // Load Default Procedural Image on Start
        Bitmap defaultBmp = createSampleBitmap(0);
        editView.setOriginalBitmap(defaultBmp);
    }

    private void switchTab(int tabIndex) {
        tabFiltersLayout.setVisibility(tabIndex == 0 ? View.VISIBLE : View.GONE);
        tabAdjustmentsLayout.setVisibility(tabIndex == 1 ? View.VISIBLE : View.GONE);
        tabPaintLayout.setVisibility(tabIndex == 2 ? View.VISIBLE : View.GONE);

        tabBtnFilters.setBackgroundColor(tabIndex == 0 ? 0xFF1E1E1E : 0xFF121212);
        tabBtnFilters.setTextColor(tabIndex == 0 ? 0xFF00ADB5 : 0xFF888888);

        tabBtnAdjustments.setBackgroundColor(tabIndex == 1 ? 0xFF1E1E1E : 0xFF121212);
        tabBtnAdjustments.setTextColor(tabIndex == 1 ? 0xFF00ADB5 : 0xFF888888);

        tabBtnPaint.setBackgroundColor(tabIndex == 2 ? 0xFF1E1E1E : 0xFF121212);
        tabBtnPaint.setTextColor(tabIndex == 2 ? 0xFF00ADB5 : 0xFF888888);

        // Turn drawing off if switching from paint to avoid accidental drawings
        if (tabIndex != 2) {
            editView.setDrawingMode(false);
            btnToggleDraw.setText("Enable Paint");
            btnToggleDraw.setBackgroundColor(0xFF2D2D2D);
        }
    }

    private void showImportOptionsDialog() {
        final CharSequence[] options = {"Choose Photo", "Load Sample: Sunset", "Load Sample: Forest", "Load Sample: Retro", "Reset Canvas"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Source");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (item == 0) {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    startActivityForResult(intent, 100);
                } else if (item == 1) {
                    editView.setOriginalBitmap(createSampleBitmap(0));
                    showToast("Sunset Sunset sample loaded");
                } else if (item == 2) {
                    editView.setOriginalBitmap(createSampleBitmap(1));
                    showToast("Emerald Forest sample loaded");
                } else if (item == 3) {
                    editView.setOriginalBitmap(createSampleBitmap(2));
                    showToast("Retro Abstract sample loaded");
                } else if (item == 4) {
                    editView.resetAll();
                    seekBrightness.setProgress(100);
                    seekContrast.setProgress(50);
                    showToast("Canvas restored to initial state.");
                }
            }
        });
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                InputStream imageStream = getContentResolver().openInputStream(imageUri);
                Bitmap selected = android.graphics.BitmapFactory.decodeStream(imageStream);
                if (selected != null) {
                    Bitmap scaled = scaleBitmapDown(selected, 1200);
                    editView.setOriginalBitmap(scaled);
                    seekBrightness.setProgress(100);
                    seekContrast.setProgress(50);
                    showToast("Custom photo loaded!");
                } else {
                    showToast("Error reading selected image");
                }
            } catch (Exception e) {
                showToast("Error loading file: " + e.getLocalizedMessage());
            }
        }
    }

    private Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (Math.max(width, height) <= maxDimension) {
            return bitmap;
        }
        float ratio = (float) width / (float) height;
        int newWidth = maxDimension;
        int newHeight = maxDimension;
        if (width > height) {
            newHeight = (int) (maxDimension / ratio);
        } else {
            newWidth = (int) (maxDimension * ratio);
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    private void saveOrShareImage() {
        final Bitmap editedBmp = editView.exportEditedBitmap();
        if (editedBmp == null) {
            showToast("No active canvas to save.");
            return;
        }

        final CharSequence[] options = {"Save to Device Gallery", "Share image file"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Export Options");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (item == 0) {
                    saveToGallery(editedBmp);
                } else {
                    shareBitmap(editedBmp);
                }
            }
        });
        builder.show();
    }

    private void saveToGallery(Bitmap bitmap) {
        String filename = "Edited_" + System.currentTimeMillis() + ".png";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PhotoEditorPro");
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
        }

        ContentResolver resolver = getContentResolver();
        Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        if (uri != null) {
            try {
                OutputStream out = resolver.openOutputStream(uri);
                if (out != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                    out.close();
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear();
                    values.put(MediaStore.Images.Media.IS_PENDING, 0);
                    resolver.update(uri, values, null, null);
                }
                showToast("Saved to Pictures/PhotoEditorPro!");
            } catch (Exception e) {
                showToast("Save failure: " + e.getLocalizedMessage());
            }
        } else {
            showToast("Failed to create file row in MediaStore.");
        }
    }

    private void shareBitmap(Bitmap bitmap) {
        try {
            java.io.File cachePath = new java.io.File(getCacheDir(), "images");
            cachePath.mkdirs();
            java.io.File file = new java.io.File(cachePath, "edited_share_photo.png");
            java.io.FileOutputStream stream = new java.io.FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/png");
            Uri contentUri = Uri.fromFile(file);
            intent.putExtra(Intent.EXTRA_STREAM, contentUri);
            startActivity(Intent.createChooser(intent, "Share Image via:"));
        } catch (Exception e) {
            showToast("Share error: " + e.getLocalizedMessage());
        }
    }

    private void setupPresets() {
        LinearLayout container = findViewById(R.id.filter_buttons_container);
        container.removeAllViews();

        container.addView(createFilterButton("Original", null));
        container.addView(createFilterButton("Grayscale", new float[]{
            0.299f, 0.587f, 0.114f, 0, 0,
            0.299f, 0.587f, 0.114f, 0, 0,
            0.299f, 0.587f, 0.114f, 0, 0,
            0,      0,      0,      1, 0
        }));
        container.addView(createFilterButton("Sepia Tone", new float[]{
            0.393f, 0.769f, 0.189f, 0, 0,
            0.349f, 0.686f, 0.168f, 0, 0,
            0.272f, 0.534f, 0.131f, 0, 0,
            0,      0,      0,      1, 0
        }));
        container.addView(createFilterButton("Negative", new float[]{
            -1.0f,  0,      0,      0, 255,
             0,    -1.0f,   0,      0, 255,
             0,     0,     -1.0f,   0, 255,
             0,     0,      0,      1,   0
        }));
        container.addView(createFilterButton("Warm Glow", new float[]{
            1.2f,   0,      0,      0, 10,
            0,      1.0f,   0,      0, 0,
            0,      0,      0.8f,   0, -10,
            0,      0,      0,      1, 0
        }));
        container.addView(createFilterButton("Cool Aqua", new float[]{
            0.8f,   0,      0,      0, -10,
            0,      1.0f,   0,      0, 0,
            0,      0,      1.2f,   0, 10,
            0,      0,      0,      1, 0
        }));
        container.addView(createFilterButton("Lomo Vintage", new float[]{
            0.9f,   0.5f,   0.1f,   0, 0,
            0.3f,   0.8f,   0.1f,   0, 0,
            0.2f,   0.3f,   0.5f,   0, 0,
            0,      0,      0,      1, 0
        }));
    }

    private Button createFilterButton(final String name, final float[] matrix) {
        Button btn = new Button(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(12, 12, 12, 12);
        btn.setLayoutParams(params);
        btn.setText(name);
        btn.setTextColor(0xFFFFFFFF);
        btn.setTextSize(12);
        btn.setAllCaps(false);

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(0xFF2D2D2D);
        gd.setCornerRadius(15);
        btn.setBackground(gd);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editView.setColorFilter(matrix);
                showToast("Preset Applied: " + name);
            }
        });
        return btn;
    }

    private void setupColorPalette() {
        LinearLayout container = findViewById(R.id.brush_colors_container);
        container.removeAllViews();

        int[] colors = {
            0xFFFF1744, // Bright Red
            0xFFFF9100, // Vibrant Orange
            0xFFFFEA00, // High-vis Yellow
            0xFF00E676, // Neon Green
            0xFF00E5FF, // Vivid Cyan
            0xFF2979FF, // Pure Blue
            0xFFD500F9, // Electric Purple
            0xFFF50057, // Deep Pink
            0xFFFFFFFF, // Pure White
            0xFF000000  // Pure Black
        };

        int sizePx = (int) (40 * getResources().getDisplayMetrics().density);

        for (int color : colors) {
            container.addView(createColorButton(color, sizePx));
        }
    }

    private View createColorButton(final int color, int sizePx) {
        View view = new View(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(sizePx, sizePx);
        params.setMargins(16, 8, 16, 8);
        view.setLayoutParams(params);

        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(color);
        gd.setStroke(4, 0xFFFFFFFF);
        view.setBackground(gd);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editView.setBrushColor(color);
                showToast("Brush color selected!");
            }
        });
        return view;
    }

    private Bitmap createSampleBitmap(int type) {
        Bitmap bitmap = Bitmap.createBitmap(800, 800, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        if (type == 0) { // Sunset
            LinearGradient gradient = new LinearGradient(
                0, 0, 0, 800,
                new int[]{0xFFFF5F6D, 0xFFFFC371},
                null, Shader.TileMode.CLAMP);
            paint.setShader(gradient);
            canvas.drawRect(0, 0, 800, 800, paint);

            paint.setShader(null);
            paint.setColor(0xEEFFFFFF);
            canvas.drawCircle(400, 500, 150, paint);

            paint.setColor(0xFF2C3E50);
            Path path = new Path();
            path.moveTo(0, 800);
            path.lineTo(200, 500);
            path.lineTo(400, 650);
            path.lineTo(650, 450);
            path.lineTo(800, 800);
            path.close();
            canvas.drawPath(path, paint);
        } else if (type == 1) { // Forest Landscape
            LinearGradient gradient = new LinearGradient(
                0, 0, 0, 800,
                new int[]{0xFF4CA1AF, 0xFF2C3E50},
                null, Shader.TileMode.CLAMP);
            paint.setShader(gradient);
            canvas.drawRect(0, 0, 800, 800, paint);

            paint.setShader(null);
            paint.setColor(0xFF1B4D3E);
            for (int i = 0; i < 5; i++) {
                int x = 100 + i * 150;
                Path tree = new Path();
                tree.moveTo(x, 800);
                tree.lineTo(x - 60, 600);
                tree.lineTo(x - 20, 600);
                tree.lineTo(x - 50, 450);
                tree.lineTo(x, 300);
                tree.lineTo(x + 50, 450);
                tree.lineTo(x + 20, 600);
                tree.lineTo(x + 60, 600);
                tree.close();
                canvas.drawPath(tree, paint);
            }
        } else { // Abstract Retro Pattern
            LinearGradient gradient = new LinearGradient(
                0, 0, 800, 800,
                new int[]{0xFF8A2387, 0xFFE94057, 0xFFF27121},
                null, Shader.TileMode.CLAMP);
            paint.setShader(gradient);
            canvas.drawRect(0, 0, 800, 800, paint);

            paint.setShader(null);
            paint.setColor(0x33FFFFFF);
            canvas.drawCircle(300, 300, 200, paint);
            canvas.drawRect(400, 100, 700, 400, paint);
        }
        return bitmap;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}