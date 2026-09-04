package com.piccraftphotoeditor.app;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final int REQ_CODE_GALLERY = 1001;

    // View References
    private FrameLayout mPhotoFrame;
    private LinearLayout mLayoutEmptyState;
    private Button mBtnImportEmpty;
    
    private Button mTabLoad, mTabFilters, mTabAdjust, mTabDraw, mTabText, mTabTransform;
    private View mPanelFilters, mPanelAdjust, mPanelDraw, mPanelText, mPanelTransform;
    
    // Sliders
    private SeekBar mAdjustSeekBar;
    private TextView mTvSliderLabel;
    private Button mBtnBright, mBtnContrast, mBtnSaturate;
    
    // Draw Sub panel components
    private SeekBar mBrushSizeSeekBar;
    private View mColorRed, mColorGreen, mColorBlue, mColorYellow, mColorWhite;
    
    // Text overlay input components
    private EditText mEtOverlayText;
    private Button mBtnAddTextApply;
    
    // Save, Reset Controls
    private Button mBtnSave, mBtnReset;
    
    // Canvas Workspace View
    private PhotoEditorView mEditorView;

    // Bitmap Containers
    private Bitmap mOriginalBitmap;
    private Bitmap mBaseWorkingBitmap; // stores filtered/rotated base matrix image

    // Edit states tracking
    private float mBrightness = 1.0f; // 1.0 = Default
    private float mContrast = 1.0f;   // 1.0 = Default
    private float mSaturation = 1.0f; // 1.0 = Default
    private int mActiveAdjustMode = 1; // 1 = Brightness, 2 = Contrast, 3 = Saturation

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupTabs();
        setupAdjustSliders();
        setupDrawingControls();
        setupTextOverlayControls();
        setupTransformControls();
        setupTopBarActions();

        // Initially hide workspaces
        toggleImageState(false);
    }

    private void initViews() {
        mPhotoFrame = (FrameLayout) findViewById(R.id.photo_frame);
        mLayoutEmptyState = (LinearLayout) findViewById(R.id.layout_empty_state);
        mBtnImportEmpty = (Button) findViewById(R.id.btn_import_empty);

        // Sub panels
        mPanelFilters = findViewById(R.id.panel_filters);
        mPanelAdjust = findViewById(R.id.panel_adjust);
        mPanelDraw = findViewById(R.id.panel_draw);
        mPanelText = findViewById(R.id.panel_text);
        mPanelTransform = findViewById(R.id.panel_transform);

        // Primary dock tabs
        mTabLoad = (Button) findViewById(R.id.tab_load);
        mTabFilters = (Button) findViewById(R.id.tab_filters);
        mTabAdjust = (Button) findViewById(R.id.tab_adjust);
        mTabDraw = (Button) findViewById(R.id.tab_draw);
        mTabText = (Button) findViewById(R.id.tab_text);
        mTabTransform = (Button) findViewById(R.id.tab_transform);

        // Top controls
        mBtnSave = (Button) findViewById(R.id.btn_save);
        mBtnReset = (Button) findViewById(R.id.btn_reset);

        // Sub widgets
        mAdjustSeekBar = (SeekBar) findViewById(R.id.adjustment_seekbar);
        mTvSliderLabel = (TextView) findViewById(R.id.tv_slider_label);
        mBtnBright = (Button) findViewById(R.id.btn_adjust_brightness);
        mBtnContrast = (Button) findViewById(R.id.btn_adjust_contrast);
        mBtnSaturate = (Button) findViewById(R.id.btn_adjust_saturation);

        mBrushSizeSeekBar = (SeekBar) findViewById(R.id.brush_size_seekbar);
        mColorRed = findViewById(R.id.color_red);
        mColorGreen = findViewById(R.id.color_green);
        mColorBlue = findViewById(R.id.color_blue);
        mColorYellow = findViewById(R.id.color_yellow);
        mColorWhite = findViewById(R.id.color_white);

        mEtOverlayText = (EditText) findViewById(R.id.et_overlay_text);
        mBtnAddTextApply = (Button) findViewById(R.id.btn_add_text_apply);

        // Load handler on empty space
        mBtnImportEmpty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImageGallery();
            }
        });
    }

    private void toggleImageState(boolean hasImage) {
        if (hasImage) {
            mLayoutEmptyState.setVisibility(View.GONE);
            mPhotoFrame.setVisibility(View.VISIBLE);
            mBtnSave.setEnabled(true);
            mBtnReset.setEnabled(true);
        } else {
            mLayoutEmptyState.setVisibility(View.VISIBLE);
            mPhotoFrame.setVisibility(View.GONE);
            mBtnSave.setEnabled(false);
            mBtnReset.setEnabled(false);
        }
    }

    private void setupTabs() {
        mTabLoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImageGallery();
            }
        });

        mTabFilters.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchActivePanel(mPanelFilters, mTabFilters);
                if (mEditorView != null) {
                    mEditorView.setMode(PhotoEditorView.MODE_VIEW);
                }
            }
        });

        mTabAdjust.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchActivePanel(mPanelAdjust, mTabAdjust);
                if (mEditorView != null) {
                    mEditorView.setMode(PhotoEditorView.MODE_VIEW);
                }
            }
        });

        mTabDraw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchActivePanel(mPanelDraw, mTabDraw);
                if (mEditorView != null) {
                    mEditorView.setMode(PhotoEditorView.MODE_DRAW);
                    Toast.makeText(MainActivity.this, "Draw mode active! Touch & draw over the image.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mTabText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchActivePanel(mPanelText, mTabText);
                if (mEditorView != null) {
                    mEditorView.setMode(PhotoEditorView.MODE_TEXT);
                    Toast.makeText(MainActivity.this, "Text mode active! Type and click Apply.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mTabTransform.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchActivePanel(mPanelTransform, mTabTransform);
                if (mEditorView != null) {
                    mEditorView.setMode(PhotoEditorView.MODE_VIEW);
                }
            }
        });
    }

    private void switchActivePanel(View targetPanel, Button activeTab) {
        mPanelFilters.setVisibility(View.GONE);
        mPanelAdjust.setVisibility(View.GONE);
        mPanelDraw.setVisibility(View.GONE);
        mPanelText.setVisibility(View.GONE);
        mPanelTransform.setVisibility(View.GONE);

        targetPanel.setVisibility(View.VISIBLE);

        // Reset tab styling
        mTabFilters.setTextColor(Color.parseColor("#CCCCCC"));
        mTabAdjust.setTextColor(Color.parseColor("#CCCCCC"));
        mTabDraw.setTextColor(Color.parseColor("#CCCCCC"));
        mTabText.setTextColor(Color.parseColor("#CCCCCC"));
        mTabTransform.setTextColor(Color.parseColor("#CCCCCC"));

        activeTab.setTextColor(Color.parseColor("#00E676"));
    }

    private void setupAdjustSliders() {
        mAdjustSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mEditorView == null || mBaseWorkingBitmap == null) return;
                
                float factor = progress / 100.0f; // Scale slider around neutral point (100 -> 1.0f)
                if (mActiveAdjustMode == 1) {
                    mBrightness = factor;
                } else if (mActiveAdjustMode == 2) {
                    mContrast = factor;
                } else if (mActiveAdjustMode == 3) {
                    mSaturation = factor;
                }
                updateAdjustmentsColorMatrix();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        mBtnBright.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActiveAdjustMode = 1;
                mTvSliderLabel.setText("Brightness");
                mAdjustSeekBar.setProgress((int) (mBrightness * 100));
                mBtnBright.setBackgroundColor(Color.parseColor("#00796B"));
                mBtnContrast.setBackgroundColor(Color.parseColor("#333333"));
                mBtnSaturate.setBackgroundColor(Color.parseColor("#333333"));
            }
        });

        mBtnContrast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActiveAdjustMode = 2;
                mTvSliderLabel.setText("Contrast");
                mAdjustSeekBar.setProgress((int) (mContrast * 100));
                mBtnBright.setBackgroundColor(Color.parseColor("#333333"));
                mBtnContrast.setBackgroundColor(Color.parseColor("#00796B"));
                mBtnSaturate.setBackgroundColor(Color.parseColor("#333333"));
            }
        });

        mBtnSaturate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActiveAdjustMode = 3;
                mTvSliderLabel.setText("Saturation");
                mAdjustSeekBar.setProgress((int) (mSaturation * 100));
                mBtnBright.setBackgroundColor(Color.parseColor("#333333"));
                mBtnContrast.setBackgroundColor(Color.parseColor("#333333"));
                mBtnSaturate.setBackgroundColor(Color.parseColor("#00796B"));
            }
        });

        // Filters configuration handlers
        findViewById(R.id.filter_none).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetWorkingBitmapFilter();
            }
        });

        findViewById(R.id.filter_grayscale).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyColorFilterPreset(getGrayscaleMatrix());
            }
        });

        findViewById(R.id.filter_sepia).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyColorFilterPreset(getSepiaMatrix());
            }
        });

        findViewById(R.id.filter_invert).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyColorFilterPreset(getInvertMatrix());
            }
        });

        findViewById(R.id.filter_warm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyColorFilterPreset(getWarmMatrix());
            }
        });

        findViewById(R.id.filter_cool).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyColorFilterPreset(getCoolMatrix());
            }
        });
    }

    private void setupDrawingControls() {
        mBrushSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mEditorView != null) {
                    mEditorView.setBrushThickness(progress > 0 ? progress : 1);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        mColorRed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditorView.setBrushColor(Color.RED);
                deselectColors();
                mColorRed.setScaleX(1.2f); mColorRed.setScaleY(1.2f);
            }
        });

        mColorGreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditorView.setBrushColor(Color.GREEN);
                deselectColors();
                mColorGreen.setScaleX(1.2f); mColorGreen.setScaleY(1.2f);
            }
        });

        mColorBlue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditorView.setBrushColor(Color.BLUE);
                deselectColors();
                mColorBlue.setScaleX(1.2f); mColorBlue.setScaleY(1.2f);
            }
        });

        mColorYellow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditorView.setBrushColor(Color.YELLOW);
                deselectColors();
                mColorYellow.setScaleX(1.2f); mColorYellow.setScaleY(1.2f);
            }
        });

        mColorWhite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditorView.setBrushColor(Color.WHITE);
                deselectColors();
                mColorWhite.setScaleX(1.2f); mColorWhite.setScaleY(1.2f);
            }
        });

        // Initialize white selected
        mColorWhite.setScaleX(1.2f);
        mColorWhite.setScaleY(1.2f);
    }

    private void deselectColors() {
        mColorRed.setScaleX(1.0f); mColorRed.setScaleY(1.0f);
        mColorGreen.setScaleX(1.0f); mColorGreen.setScaleY(1.0f);
        mColorBlue.setScaleX(1.0f); mColorBlue.setScaleY(1.0f);
        mColorYellow.setScaleX(1.0f); mColorYellow.setScaleY(1.0f);
        mColorWhite.setScaleX(1.0f); mColorWhite.setScaleY(1.0f);
    }

    private void setupTextOverlayControls() {
        mBtnAddTextApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String val = mEtOverlayText.getText().toString().trim();
                if (val.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please write text first", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (mEditorView != null) {
                    mEditorView.setOverlayText(val);
                    mEtOverlayText.setText("");
                    Toast.makeText(MainActivity.this, "Text applied! Drag text on image to change position.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupTransformControls() {
        findViewById(R.id.btn_rotate_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyTransformation(rotateBitmap(mBaseWorkingBitmap, -90));
            }
        });

        findViewById(R.id.btn_rotate_right).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyTransformation(rotateBitmap(mBaseWorkingBitmap, 90));
            }
        });

        findViewById(R.id.btn_flip_h).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyTransformation(flipBitmap(mBaseWorkingBitmap, true, false));
            }
        });

        findViewById(R.id.btn_flip_v).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyTransformation(flipBitmap(mBaseWorkingBitmap, false, true));
            }
        });
    }

    private void setupTopBarActions() {
        mBtnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOriginalBitmap != null) {
                    mBaseWorkingBitmap = mOriginalBitmap.copy(Bitmap.Config.ARGB_8888, true);
                    mBrightness = 1.0f;
                    mContrast = 1.0f;
                    mSaturation = 1.0f;
                    mAdjustSeekBar.setProgress(100);
                    
                    mEditorView.setWorkingBitmap(mBaseWorkingBitmap);
                    mEditorView.clearDoodlesAndText();
                    Toast.makeText(MainActivity.this, "Image and edits completely reset!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mBtnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveFinalRenderedBitmap();
            }
        });
    }

    private void openImageGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Image To Edit"), REQ_CODE_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == REQ_CODE_GALLERY && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                // Read and scale bitmap down safely to protect heap memory
                InputStream imageStream = getContentResolver().openInputStream(imageUri);
                Bitmap tempRaw = BitmapFactory.decodeStream(imageStream);
                if (tempRaw == null) {
                    Toast.makeText(this, "Failed to parse loaded file.", Toast.LENGTH_LONG).show();
                    return;
                }

                mOriginalBitmap = scaleDownToMaxDimensions(tempRaw, 1280);
                mBaseWorkingBitmap = mOriginalBitmap.copy(Bitmap.Config.ARGB_8888, true);

                // Initialize Canvas Workspace
                mPhotoFrame.removeAllViews();
                mEditorView = new PhotoEditorView(this);
                mEditorView.setWorkingBitmap(mBaseWorkingBitmap);
                mPhotoFrame.addView(mEditorView, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

                // Switch states
                toggleImageState(true);
                // Switch focus to primary filters screen
                switchActivePanel(mPanelFilters, mTabFilters);

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error importing file: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    // Bitmap scaling optimizer
    private Bitmap scaleDownToMaxDimensions(Bitmap realImg, float maxDimension) {
        float width = realImg.getWidth();
        float height = realImg.getHeight();
        if (width <= maxDimension && height <= maxDimension) {
            return realImg;
        }
        float ratio = Math.min(maxDimension / width, maxDimension / height);
        int finalW = Math.round(ratio * width);
        int finalH = Math.round(ratio * height);
        return Bitmap.createScaledBitmap(realImg, finalW, finalH, true);
    }

    // Pixel matrix adjustment transformations
    private void updateAdjustmentsColorMatrix() {
        if (mEditorView == null) return;
        
        ColorMatrix overallMatrix = new ColorMatrix();

        // 1. Brightness
        // Simple scale offset: we can manipulate translation of RGB colors using scaling factor
        ColorMatrix brightMatrix = new ColorMatrix();
        float bOffset = (mBrightness - 1.0f) * 255.0f;
        brightMatrix.set(new float[] {
                1, 0, 0, 0, bOffset,
                0, 1, 0, 0, bOffset,
                0, 0, 1, 0, bOffset,
                0, 0, 0, 1, 0
        });
        overallMatrix.postConcat(brightMatrix);

        // 2. Contrast
        ColorMatrix contrastMatrix = new ColorMatrix();
        float scale = mContrast;
        float translate = (-.5f * scale + .5f) * 255f;
        contrastMatrix.set(new float[] {
                scale, 0, 0, 0, translate,
                0, scale, 0, 0, translate,
                0, 0, scale, 0, translate,
                0, 0, 0, 1, 0
        });
        overallMatrix.postConcat(contrastMatrix);

        // 3. Saturation
        ColorMatrix satMatrix = new ColorMatrix();
        satMatrix.setSaturation(mSaturation);
        overallMatrix.postConcat(satMatrix);

        mEditorView.setImageMatrixFilter(overallMatrix);
    }

    private void applyColorFilterPreset(ColorMatrix filterMatrix) {
        if (mBaseWorkingBitmap == null || mEditorView == null) return;
        
        // Create an adjusted copy of base working bitmap with preset applied
        Bitmap filteredResult = Bitmap.createBitmap(mBaseWorkingBitmap.getWidth(), mBaseWorkingBitmap.getHeight(), mBaseWorkingBitmap.getConfig());
        Canvas canvas = new Canvas(filteredResult);
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(filterMatrix));
        canvas.drawBitmap(mBaseWorkingBitmap, 0, 0, paint);

        mBaseWorkingBitmap = filteredResult;
        mEditorView.setWorkingBitmap(mBaseWorkingBitmap);
        updateAdjustmentsColorMatrix(); // Reapply basic sliders on top of preset
    }

    private void resetWorkingBitmapFilter() {
        if (mOriginalBitmap == null || mEditorView == null) return;
        mBaseWorkingBitmap = mOriginalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        mEditorView.setWorkingBitmap(mBaseWorkingBitmap);
        updateAdjustmentsColorMatrix();
    }

    private void applyTransformation(Bitmap transformed) {
        if (transformed == null) return;
        mBaseWorkingBitmap = transformed;
        mEditorView.setWorkingBitmap(mBaseWorkingBitmap);
    }

    // Color matrices presets generator
    private ColorMatrix getGrayscaleMatrix() {
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0);
        return matrix;
    }

    private ColorMatrix getSepiaMatrix() {
        ColorMatrix matrix = new ColorMatrix();
        matrix.set(new float[] {
                0.393f, 0.769f, 0.189f, 0, 0,
                0.349f, 0.686f, 0.168f, 0, 0,
                0.272f, 0.534f, 0.131f, 0, 0,
                0,      0,      0,      1, 0
        });
        return matrix;
    }

    private ColorMatrix getInvertMatrix() {
        return new ColorMatrix(new float[] {
                -1.0f,  0.0f,  0.0f, 1.0f, 255.0f,
                 0.0f, -1.0f,  0.0f, 1.0f, 255.0f,
                 0.0f,  0.0f, -1.0f, 1.0f, 255.0f,
                 0.0f,  0.0f,  0.0f, 1.0f,   0.0f
        });
    }

    private ColorMatrix getWarmMatrix() {
        ColorMatrix matrix = new ColorMatrix();
        matrix.set(new float[] {
                1.2f, 0, 0, 0, 0,
                0, 1.0f, 0, 0, 0,
                0, 0, 0.8f, 0, 0,
                0, 0, 0, 1, 0
        });
        return matrix;
    }

    private ColorMatrix getCoolMatrix() {
        ColorMatrix matrix = new ColorMatrix();
        matrix.set(new float[] {
                0.8f, 0, 0, 0, 0,
                0, 1.0f, 0, 0, 0,
                0, 0, 1.2f, 0, 0,
                0, 0, 0, 1, 0
        });
        return matrix;
    }

    // Geometry transforms helper methods
    private Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    private Bitmap flipBitmap(Bitmap source, boolean horizontal, boolean vertical) {
        Matrix matrix = new Matrix();
        matrix.preScale(horizontal ? -1.0f : 1.0f, vertical ? -1.0f : 1.0f);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    // Render final edited bitmap and save in Public Directory using MediaStore API
    private void saveFinalRenderedBitmap() {
        if (mEditorView == null || mBaseWorkingBitmap == null) {
            Toast.makeText(this, "No image to save!", Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap finalComposition = mEditorView.generateCompositeBitmap();
        if (finalComposition == null) {
            Toast.makeText(this, "Synthesis failed", Toast.LENGTH_SHORT).show();
            return;
        }

        OutputStream fos = null;
        String filename = "PicCraft_" + System.currentTimeMillis() + ".jpg";

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PicCraftEditor");

                Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                if (imageUri != null) {
                    fos = getContentResolver().openOutputStream(imageUri);
                }
            } else {
                java.io.File imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                java.io.File customFolder = new java.io.File(imagesDir, "PicCraftEditor");
                if (!customFolder.exists()) {
                    customFolder.mkdirs();
                }
                java.io.File imageFile = new java.io.File(customFolder, filename);
                fos = new java.io.FileOutputStream(imageFile);
            }

            if (fos != null) {
                finalComposition.compress(Bitmap.CompressFormat.JPEG, 95, fos);
                fos.flush();
                fos.close();
                Toast.makeText(this, "Photo exported to Gallery successfully!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Error initializing output streams", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Saving failed: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Inner Canvas drawing custom component
    private static class PhotoEditorView extends View {

        public static final int MODE_VIEW = 0;
        public static final int MODE_DRAW = 1;
        public static final int MODE_TEXT = 2;

        private int mCurrentMode = MODE_VIEW;

        private Bitmap mBitmap;
        private ColorMatrix mFilterMatrix;
        private Paint mImagePaint;

        // Path drawing variables
        private Path mActiveDrawingPath;
        private Paint mActiveDrawingPaint;
        private final List<Path> mPathList = new ArrayList<>();
        private final List<Paint> mPaintList = new ArrayList<>();

        private int mBrushColor = Color.WHITE;
        private int mBrushThickness = 10;

        // Text rendering properties
        private String mOverlayText = "";
        private float mTextX = 100f;
        private float mTextY = 100f;
        private Paint mTextPaint;
        private float mTouchOffsetX, mTouchOffsetY;
        private boolean mIsDraggingText = false;

        public PhotoEditorView(Context context) {
            super(context);
            init();
        }

        private void init() {
            mImagePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            mActiveDrawingPath = new Path();

            setupTextPaint();
            setupActivePaint();
        }

        private void setupActivePaint() {
            mActiveDrawingPaint = new Paint();
            mActiveDrawingPaint.setColor(mBrushColor);
            mActiveDrawingPaint.setStyle(Paint.Style.STROKE);
            mActiveDrawingPaint.setStrokeCap(Paint.Cap.ROUND);
            mActiveDrawingPaint.setStrokeJoin(Paint.Join.ROUND);
            mActiveDrawingPaint.setStrokeWidth(mBrushThickness);
            mActiveDrawingPaint.setAntiAlias(true);
        }

        private void setupTextPaint() {
            mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mTextPaint.setColor(Color.WHITE);
            mTextPaint.setTextSize(60f);
            mTextPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            mTextPaint.setShadowLayer(8f, 0f, 0f, Color.BLACK);
        }

        public void setWorkingBitmap(Bitmap bmp) {
            mBitmap = bmp;
            invalidate();
        }

        public void setImageMatrixFilter(ColorMatrix matrix) {
            mFilterMatrix = matrix;
            if (mFilterMatrix != null) {
                mImagePaint.setColorFilter(new ColorMatrixColorFilter(mFilterMatrix));
            } else {
                mImagePaint.setColorFilter(null);
            }
            invalidate();
        }

        public void setMode(int mode) {
            mCurrentMode = mode;
        }

        public void setBrushColor(int color) {
            mBrushColor = color;
            setupActivePaint();
        }

        public void setBrushThickness(int size) {
            mBrushThickness = size;
            setupActivePaint();
        }

        public void setOverlayText(String text) {
            mOverlayText = text;
            mTextX = getWidth() / 2f;
            mTextY = getHeight() / 2f;
            invalidate();
        }

        public void clearDoodlesAndText() {
            mPathList.clear();
            mPaintList.clear();
            mOverlayText = "";
            mActiveDrawingPath.reset();
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (mBitmap == null) return;

            // Compute ideal scale-to-fit transformation inside local workspace area
            float viewWidth = getWidth();
            float viewHeight = getHeight();
            float bitmapWidth = mBitmap.getWidth();
            float bitmapHeight = mBitmap.getHeight();

            float scale = Math.min(viewWidth / bitmapWidth, viewHeight / bitmapHeight);
            float dx = (viewWidth - bitmapWidth * scale) / 2f;
            float dy = (viewHeight - bitmapHeight * scale) / 2f;

            // Build structural dynamic mapping matrices
            canvas.save();
            canvas.translate(dx, dy);
            canvas.scale(scale, scale);

            // Draw base image with filters applied
            canvas.drawBitmap(mBitmap, 0, 0, mImagePaint);

            // Draw historic completed freehand drawing paths
            for (int i = 0; i < mPathList.size(); i++) {
                canvas.drawPath(mPathList.get(i), mPaintList.get(i));
            }

            // Draw active user drawing path
            if (mCurrentMode == MODE_DRAW) {
                canvas.drawPath(mActiveDrawingPath, mActiveDrawingPaint);
            }

            // Draw text overlays if configured
            if (mOverlayText != null && !mOverlayText.isEmpty()) {
                // Adjust text coordinates to map relative coordinate system back into base workspace canvas space
                float localTxtX = (mTextX - dx) / scale;
                float localTxtY = (mTextY - dy) / scale;
                canvas.drawText(mOverlayText, localTxtX, localTxtY, mTextPaint);
            }

            canvas.restore();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (mBitmap == null) return false;

            float x = event.getX();
            float y = event.getY();

            if (mCurrentMode == MODE_DRAW) {
                // Custom drawing coordinates capture
                float viewWidth = getWidth();
                float viewHeight = getHeight();
                float scale = Math.min(viewWidth / mBitmap.getWidth(), viewHeight / mBitmap.getHeight());
                float dx = (viewWidth - mBitmap.getWidth() * scale) / 2f;
                float dy = (viewHeight - mBitmap.getHeight() * scale) / 2f;

                // Adjust drawn path relative coordinates to map exactly back into original bitmap sizing
                float relX = (x - dx) / scale;
                float relY = (y - dy) / scale;

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mActiveDrawingPath.moveTo(relX, relY);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        mActiveDrawingPath.lineTo(relX, relY);
                        break;
                    case MotionEvent.ACTION_UP:
                        mActiveDrawingPath.lineTo(relX, relY);
                        mPathList.add(mActiveDrawingPath);
                        mPaintList.add(mActiveDrawingPaint);

                        // Reset internal tracking active path
                        mActiveDrawingPath = new Path();
                        setupActivePaint();
                        break;
                }
                invalidate();
                return true;
            } else if (mCurrentMode == MODE_TEXT && mOverlayText != null && !mOverlayText.isEmpty()) {
                // Draggable text coordinates configuration
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Simple bounding box checks to see if user tapped around text coordinates
                        Rect bounds = new Rect();
                        mTextPaint.getTextBounds(mOverlayText, 0, mOverlayText.length(), bounds);
                        float txtW = bounds.width();
                        float txtH = bounds.height();

                        if (Math.abs(x - mTextX) < txtW + 50 && Math.abs(y - mTextY) < txtH + 50) {
                            mIsDraggingText = true;
                            mTouchOffsetX = x - mTextX;
                            mTouchOffsetY = y - mTextY;
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (mIsDraggingText) {
                            mTextX = x - mTouchOffsetX;
                            mTextY = y - mTouchOffsetY;
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        mIsDraggingText = false;
                        break;
                }
                invalidate();
                return true;
            }

            return super.onTouchEvent(event);
        }

        // Generates full flattened result combining filter changes, overlays and graphics into saved bitmap export
        public Bitmap generateCompositeBitmap() {
            if (mBitmap == null) return null;

            // Match full scale composition to target dimensions
            Bitmap output = Bitmap.createBitmap(mBitmap.getWidth(), mBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(output);

            // 1. Draw filtered original bitmap
            canvas.drawBitmap(mBitmap, 0, 0, mImagePaint);

            // 2. Draw all captured vector freehand brush strokes
            for (int i = 0; i < mPathList.size(); i++) {
                canvas.drawPath(mPathList.get(i), mPaintList.get(i));
            }

            // 3. Render overlaid text relative vectors
            if (mOverlayText != null && !mOverlayText.isEmpty()) {
                float viewWidth = getWidth();
                float viewHeight = getHeight();
                float scale = Math.min(viewWidth / mBitmap.getWidth(), viewHeight / mBitmap.getHeight());
                float dx = (viewWidth - mBitmap.getWidth() * scale) / 2f;
                float dy = (viewHeight - mBitmap.getHeight() * scale) / 2f;

                float localTxtX = (mTextX - dx) / scale;
                float localTxtY = (mTextY - dy) / scale;

                canvas.drawText(mOverlayText, localTxtX, localTxtY, mTextPaint);
            }

            return output;
        }
    }
}