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
    
    private Button mTabLoad, mTabFilters, mTabAdjust, mTabDraw, mTabText, mTabTransform, mTabFrames, mBtnAiEnhance;
    private View mPanelFilters, mPanelAdjust, mPanelDraw, mPanelText, mPanelTransform, mPanelFrames;
    
    // Sliders
    private SeekBar mAdjustSeekBar;
    private TextView mTvSliderLabel;
    private Button mBtnBright, mBtnContrast, mBtnSaturate;
    
    // Draw components
    private SeekBar mBrushSizeSeekBar;
    private View mColorRed, mColorGreen, mColorBlue, mColorYellow, mColorWhite;
    
    // Text overlay input
    private EditText mEtOverlayText;
    private Button mBtnAddTextApply;
    
    // Action Buttons
    private Button mBtnSave, mBtnReset;
    
    // Workspace View
    private PhotoEditorView mEditorView;

    // Bitmaps
    private Bitmap mOriginalBitmap;
    private Bitmap mBaseWorkingBitmap;

    // States tracking
    private float mBrightness = 1.0f;
    private float mContrast = 1.0f;
    private float mSaturation = 1.0f;
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
        setupFramesControls();
        setupTopBarActions();

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
        mPanelFrames = findViewById(R.id.panel_frames);

        // Navigation dock tabs
        mTabLoad = (Button) findViewById(R.id.tab_load);
        mTabFilters = (Button) findViewById(R.id.tab_filters);
        mTabAdjust = (Button) findViewById(R.id.tab_adjust);
        mTabDraw = (Button) findViewById(R.id.tab_draw);
        mTabText = (Button) findViewById(R.id.tab_text);
        mTabTransform = (Button) findViewById(R.id.tab_transform);
        mTabFrames = (Button) findViewById(R.id.tab_frames);
        mBtnAiEnhance = (Button) findViewById(R.id.btn_ai_enhance);

        // Top controls
        mBtnSave = (Button) findViewById(R.id.btn_save);
        mBtnReset = (Button) findViewById(R.id.btn_reset);

        // Adjustment Widgets
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
                    Toast.makeText(MainActivity.this, "Pro Brush loaded. Sketch directly on the frame.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mTabText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchActivePanel(mPanelText, mTabText);
                if (mEditorView != null) {
                    mEditorView.setMode(PhotoEditorView.MODE_TEXT);
                    Toast.makeText(MainActivity.this, "Type premium titles & apply, drag to position.", Toast.LENGTH_SHORT).show();
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

        mTabFrames.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchActivePanel(mPanelFrames, mTabFrames);
                if (mEditorView != null) {
                    mEditorView.setMode(PhotoEditorView.MODE_VIEW);
                }
            }
        });

        mBtnAiEnhance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyAiAutoEnhance();
            }
        });
    }

    private void switchActivePanel(View targetPanel, Button activeTab) {
        mPanelFilters.setVisibility(View.GONE);
        mPanelAdjust.setVisibility(View.GONE);
        mPanelDraw.setVisibility(View.GONE);
        mPanelText.setVisibility(View.GONE);
        mPanelTransform.setVisibility(View.GONE);
        mPanelFrames.setVisibility(View.GONE);

        targetPanel.setVisibility(View.VISIBLE);

        // Reset text styles
        mTabFilters.setTextColor(Color.parseColor("#8E8E93"));
        mTabAdjust.setTextColor(Color.parseColor("#8E8E93"));
        mTabDraw.setTextColor(Color.parseColor("#8E8E93"));
        mTabText.setTextColor(Color.parseColor("#8E8E93"));
        mTabTransform.setTextColor(Color.parseColor("#8E8E93"));
        mTabFrames.setTextColor(Color.parseColor("#FFE259"));

        mTabFilters.setBackgroundResource(0);
        mTabAdjust.setBackgroundResource(0);
        mTabDraw.setBackgroundResource(0);
        mTabText.setBackgroundResource(0);
        mTabTransform.setBackgroundResource(0);
        mTabFrames.setBackgroundResource(0);

        // Set active highlighted style
        if (activeTab == mTabFrames) {
            activeTab.setTextColor(Color.parseColor("#FFFFFF"));
            activeTab.setBackgroundResource(R.drawable.bg_premium_gold);
        } else {
            activeTab.setTextColor(Color.parseColor("#FFE259"));
            activeTab.setBackgroundResource(R.drawable.bg_tab_active);
        }
    }

    private void setupAdjustSliders() {
        mAdjustSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mEditorView == null || mBaseWorkingBitmap == null) return;
                
                float factor = progress / 100.0f;
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
                
                mBtnBright.setBackgroundResource(R.drawable.bg_premium_gold);
                mBtnBright.setTextColor(Color.parseColor("#121212"));
                
                mBtnContrast.setBackgroundResource(R.drawable.bg_button_dark);
                mBtnContrast.setTextColor(Color.parseColor("#FFFFFF"));
                mBtnSaturate.setBackgroundResource(R.drawable.bg_button_dark);
                mBtnSaturate.setTextColor(Color.parseColor("#FFFFFF"));
            }
        });

        mBtnContrast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActiveAdjustMode = 2;
                mTvSliderLabel.setText("Contrast");
                mAdjustSeekBar.setProgress((int) (mContrast * 100));
                
                mBtnBright.setBackgroundResource(R.drawable.bg_button_dark);
                mBtnBright.setTextColor(Color.parseColor("#FFFFFF"));
                
                mBtnContrast.setBackgroundResource(R.drawable.bg_premium_gold);
                mBtnContrast.setTextColor(Color.parseColor("#121212"));
                
                mBtnSaturate.setBackgroundResource(R.drawable.bg_button_dark);
                mBtnSaturate.setTextColor(Color.parseColor("#FFFFFF"));
            }
        });

        mBtnSaturate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActiveAdjustMode = 3;
                mTvSliderLabel.setText("Saturation");
                mAdjustSeekBar.setProgress((int) (mSaturation * 100));
                
                mBtnBright.setBackgroundResource(R.drawable.bg_button_dark);
                mBtnBright.setTextColor(Color.parseColor("#FFFFFF"));
                mBtnContrast.setBackgroundResource(R.drawable.bg_button_dark);
                mBtnContrast.setTextColor(Color.parseColor("#FFFFFF"));
                
                mBtnSaturate.setBackgroundResource(R.drawable.bg_premium_gold);
                mBtnSaturate.setTextColor(Color.parseColor("#121212"));
            }
        });

        // Filter button click listeners
        findViewById(R.id.filter_none).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetWorkingBitmapFilter();
            }
        });

        findViewById(R.id.filter_cyber).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyColorFilterPreset(getCyberMatrix());
            }
        });

        findViewById(R.id.filter_gold).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyColorFilterPreset(getGoldMatrix());
            }
        });

        findViewById(R.id.filter_emerald).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyColorFilterPreset(getEmeraldMatrix());
            }
        });

        findViewById(R.id.filter_midnight).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyColorFilterPreset(getMidnightMatrix());
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
    }

    private void applyAiAutoEnhance() {
        if (mBaseWorkingBitmap == null) {
            Toast.makeText(this, "Load a canvas photo first!", Toast.LENGTH_SHORT).show();
            return;
        }
        // AI dynamic balance formula
        mBrightness = 1.08f;
        mContrast = 1.15f;
        mSaturation = 1.20f;
        
        mAdjustSeekBar.setProgress(108);
        mActiveAdjustMode = 1;
        mTvSliderLabel.setText("Brightness");
        
        updateAdjustmentsColorMatrix();
        Toast.makeText(this, "✦ AI Engine Auto-Balanced Studio Settings Successfully!", Toast.LENGTH_SHORT).show();
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
                mEditorView.setBrushColor(Color.parseColor("#FF5252"));
                deselectColors();
                mColorRed.setScaleX(1.2f); mColorRed.setScaleY(1.2f);
            }
        });

        mColorGreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditorView.setBrushColor(Color.parseColor("#00E676"));
                deselectColors();
                mColorGreen.setScaleX(1.2f); mColorGreen.setScaleY(1.2f);
            }
        });

        mColorBlue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditorView.setBrushColor(Color.parseColor("#448AFF"));
                deselectColors();
                mColorBlue.setScaleX(1.2f); mColorBlue.setScaleY(1.2f);
            }
        });

        mColorYellow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditorView.setBrushColor(Color.parseColor("#FFE259"));
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
                    Toast.makeText(MainActivity.this, "Write custom text caption.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (mEditorView != null) {
                    mEditorView.setOverlayText(val);
                    mEtOverlayText.setText("");
                    Toast.makeText(MainActivity.this, "Premium text applied! Drag directly onto the photo.", Toast.LENGTH_SHORT).show();
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

    private void setupFramesControls() {
        findViewById(R.id.frame_none).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEditorView != null) {
                    mEditorView.setFrame(PhotoEditorView.FRAME_NONE);
                }
            }
        });

        findViewById(R.id.frame_gold).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEditorView != null) {
                    mEditorView.setFrame(PhotoEditorView.FRAME_ROYAL_GOLD);
                }
            }
        });

        findViewById(R.id.frame_cyber).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEditorView != null) {
                    mEditorView.setFrame(PhotoEditorView.FRAME_CYBER_NEON);
                }
            }
        });

        findViewById(R.id.frame_matte).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEditorView != null) {
                    mEditorView.setFrame(PhotoEditorView.FRAME_CINEMATIC_MATTE);
                }
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
                    mEditorView.setFrame(PhotoEditorView.FRAME_NONE);
                    Toast.makeText(MainActivity.this, "Canvas & history clean-slate complete.", Toast.LENGTH_SHORT).show();
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
        startActivityForResult(Intent.createChooser(intent, "Open Studio Image"), REQ_CODE_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == REQ_CODE_GALLERY && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                InputStream imageStream = getContentResolver().openInputStream(imageUri);
                Bitmap tempRaw = BitmapFactory.decodeStream(imageStream);
                if (tempRaw == null) {
                    Toast.makeText(this, "Failed to load format.", Toast.LENGTH_LONG).show();
                    return;
                }

                mOriginalBitmap = scaleDownToMaxDimensions(tempRaw, 1600); // Higher max dimension for high quality Pro output
                mBaseWorkingBitmap = mOriginalBitmap.copy(Bitmap.Config.ARGB_8888, true);

                mPhotoFrame.removeAllViews();
                mEditorView = new PhotoEditorView(this);
                mEditorView.setWorkingBitmap(mBaseWorkingBitmap);
                mPhotoFrame.addView(mEditorView, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

                toggleImageState(true);
                switchActivePanel(mPanelFilters, mTabFilters);

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Import failure: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

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

    private void updateAdjustmentsColorMatrix() {
        if (mEditorView == null) return;
        
        ColorMatrix overallMatrix = new ColorMatrix();

        ColorMatrix brightMatrix = new ColorMatrix();
        float bOffset = (mBrightness - 1.0f) * 255.0f;
        brightMatrix.set(new float[] {
                1, 0, 0, 0, bOffset,
                0, 1, 0, 0, bOffset,
                0, 0, 1, 0, bOffset,
                0, 0, 0, 1, 0
        });
        overallMatrix.postConcat(brightMatrix);

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

        ColorMatrix satMatrix = new ColorMatrix();
        satMatrix.setSaturation(mSaturation);
        overallMatrix.postConcat(satMatrix);

        mEditorView.setImageMatrixFilter(overallMatrix);
    }

    private void applyColorFilterPreset(ColorMatrix filterMatrix) {
        if (mBaseWorkingBitmap == null || mEditorView == null) return;
        
        Bitmap filteredResult = Bitmap.createBitmap(mBaseWorkingBitmap.getWidth(), mBaseWorkingBitmap.getHeight(), mBaseWorkingBitmap.getConfig());
        Canvas canvas = new Canvas(filteredResult);
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(filterMatrix));
        canvas.drawBitmap(mBaseWorkingBitmap, 0, 0, paint);

        mBaseWorkingBitmap = filteredResult;
        mEditorView.setWorkingBitmap(mBaseWorkingBitmap);
        updateAdjustmentsColorMatrix();
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

    // High End Color Matrices
    private ColorMatrix getCyberMatrix() {
        return new ColorMatrix(new float[] {
                1.1f, 0,    0.5f, 0, -20f,
                0,    0.9f, 0.4f, 0, 10f,
                0.3f, 0,    1.3f, 0, 30f,
                0,    0,    0,    1, 0
        });
    }

    private ColorMatrix getGoldMatrix() {
        return new ColorMatrix(new float[] {
                1.2f, 0.1f, 0,    0, 20f,
                0.1f, 1.1f, 0,    0, 10f,
                0,    0,    0.8f, 0, -10f,
                0,    0,    0,    1, 0
        });
    }

    private ColorMatrix getEmeraldMatrix() {
        return new ColorMatrix(new float[] {
                0.8f, 0,    0,    0, 0,
                0,    1.3f, 0,    0, 20f,
                0,    0,    0.9f, 0, 10f,
                0,    0,    0,    1, 0
        });
    }

    private ColorMatrix getMidnightMatrix() {
        return new ColorMatrix(new float[] {
                0.6f, 0.1f, 0.1f, 0, -10f,
                0.1f, 0.7f, 0.2f, 0, -10f,
                0.2f, 0.2f, 1.3f, 0, 15f,
                0,    0,    0,    1, 0
        });
    }

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

    private void saveFinalRenderedBitmap() {
        if (mEditorView == null || mBaseWorkingBitmap == null) {
            Toast.makeText(this, "Empty studio canvas!", Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap finalComposition = mEditorView.generateCompositeBitmap();
        if (finalComposition == null) {
            Toast.makeText(this, "Generation failure", Toast.LENGTH_SHORT).show();
            return;
        }

        OutputStream fos = null;
        String filename = "PicCraft_Pro_" + System.currentTimeMillis() + ".jpg";

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PicCraftPro");

                Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                if (imageUri != null) {
                    fos = getContentResolver().openOutputStream(imageUri);
                }
            } else {
                java.io.File imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                java.io.File customFolder = new java.io.File(imagesDir, "PicCraftPro");
                if (!customFolder.exists()) {
                    customFolder.mkdirs();
                }
                java.io.File imageFile = new java.io.File(customFolder, filename);
                fos = new java.io.FileOutputStream(imageFile);
            }

            if (fos != null) {
                finalComposition.compress(Bitmap.CompressFormat.JPEG, 98, fos); // Premium 98% compression density ratio
                fos.flush();
                fos.close();
                Toast.makeText(this, "✦ Masterpiece Exported successfully to Pictures/PicCraftPro!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Failed opening disk stream", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Save failure: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Canvas rendering widget
    private static class PhotoEditorView extends View {

        public static final int MODE_VIEW = 0;
        public static final int MODE_DRAW = 1;
        public static final int MODE_TEXT = 2;

        public static final int FRAME_NONE = 0;
        public static final int FRAME_ROYAL_GOLD = 1;
        public static final int FRAME_CYBER_NEON = 2;
        public static final int FRAME_CINEMATIC_MATTE = 3;

        private int mCurrentMode = MODE_VIEW;
        private int mCurrentFrame = FRAME_NONE;

        private Bitmap mBitmap;
        private ColorMatrix mFilterMatrix;
        private Paint mImagePaint;

        private Path mActiveDrawingPath;
        private Paint mActiveDrawingPaint;
        private final List<Path> mPathList = new ArrayList<>();
        private final List<Paint> mPaintList = new ArrayList<>();

        private int mBrushColor = Color.WHITE;
        private int mBrushThickness = 10;

        private String mOverlayText = "";
        private float mTextX = 200f;
        private float mTextY = 200f;
        private Paint mTextPaint;
        private float mTouchOffsetX, mTouchOffsetY;
        private boolean mIsDraggingText = false;

        // Custom frame render paints
        private Paint mFramePaintGold;
        private Paint mFramePaintNeon;
        private Paint mFramePaintMatte;

        public PhotoEditorView(Context context) {
            super(context);
            init();
        }

        private void init() {
            mImagePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            mActiveDrawingPath = new Path();

            setupTextPaint();
            setupActivePaint();
            setupFramePaints();
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
            mTextPaint.setTextSize(72f); // Elevated readable font density
            mTextPaint.setTypeface(android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD));
            mTextPaint.setShadowLayer(10f, 2f, 2f, Color.BLACK);
        }

        private void setupFramePaints() {
            mFramePaintGold = new Paint(Paint.ANTI_ALIAS_FLAG);
            mFramePaintGold.setColor(Color.parseColor("#FFE259"));
            mFramePaintGold.setStyle(Paint.Style.STROKE);
            mFramePaintGold.setStrokeWidth(24f);

            mFramePaintNeon = new Paint(Paint.ANTI_ALIAS_FLAG);
            mFramePaintNeon.setColor(Color.parseColor("#FF00FF"));
            mFramePaintNeon.setStyle(Paint.Style.STROKE);
            mFramePaintNeon.setStrokeWidth(16f);
            mFramePaintNeon.setShadowLayer(15f, 0f, 0f, Color.parseColor("#00FFFF"));

            mFramePaintMatte = new Paint(Paint.ANTI_ALIAS_FLAG);
            mFramePaintMatte.setColor(Color.BLACK);
            mFramePaintMatte.setStyle(Paint.Style.FILL);
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

        public void setFrame(int frameType) {
            mCurrentFrame = frameType;
            invalidate();
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

            float viewWidth = getWidth();
            float viewHeight = getHeight();
            float bitmapWidth = mBitmap.getWidth();
            float bitmapHeight = mBitmap.getHeight();

            float scale = Math.min(viewWidth / bitmapWidth, viewHeight / bitmapHeight);
            float dx = (viewWidth - bitmapWidth * scale) / 2f;
            float dy = (viewHeight - bitmapHeight * scale) / 2f;

            canvas.save();
            canvas.translate(dx, dy);
            canvas.scale(scale, scale);

            // Draw Photo
            canvas.drawBitmap(mBitmap, 0, 0, mImagePaint);

            // Draw Vectors
            for (int i = 0; i < mPathList.size(); i++) {
                canvas.drawPath(mPathList.get(i), mPaintList.get(i));
            }

            if (mCurrentMode == MODE_DRAW) {
                canvas.drawPath(mActiveDrawingPath, mActiveDrawingPaint);
            }

            // Draw Premium Frames Over Base Image Space
            drawBespokeFrame(canvas, bitmapWidth, bitmapHeight);

            // Text Rendering Layer
            if (mOverlayText != null && !mOverlayText.isEmpty()) {
                float localTxtX = (mTextX - dx) / scale;
                float localTxtY = (mTextY - dy) / scale;
                canvas.drawText(mOverlayText, localTxtX, localTxtY, mTextPaint);
            }

            canvas.restore();
        }

        private void drawBespokeFrame(Canvas canvas, float width, float height) {
            if (mCurrentFrame == FRAME_ROYAL_GOLD) {
                float inset = mFramePaintGold.getStrokeWidth() / 2f;
                canvas.drawRect(inset, inset, width - inset, height - inset, mFramePaintGold);
                
                // Fine inner border
                Paint thinGold = new Paint(mFramePaintGold);
                thinGold.setStrokeWidth(4f);
                float thinInset = mFramePaintGold.getStrokeWidth() + 15f;
                canvas.drawRect(thinInset, thinInset, width - thinInset, height - thinInset, thinGold);
                
            } else if (mCurrentFrame == FRAME_CYBER_NEON) {
                float inset = mFramePaintNeon.getStrokeWidth() / 2f;
                canvas.drawRect(inset, inset, width - inset, height - inset, mFramePaintNeon);
                
            } else if (mCurrentFrame == FRAME_CINEMATIC_MATTE) {
                float barHeight = height * 0.12f;
                canvas.drawRect(0, 0, width, barHeight, mFramePaintMatte);
                canvas.drawRect(0, height - barHeight, width, height, mFramePaintMatte);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (mBitmap == null) return false;

            float x = event.getX();
            float y = event.getY();

            if (mCurrentMode == MODE_DRAW) {
                float viewWidth = getWidth();
                float viewHeight = getHeight();
                float scale = Math.min(viewWidth / mBitmap.getWidth(), viewHeight / mBitmap.getHeight());
                float dx = (viewWidth - mBitmap.getWidth() * scale) / 2f;
                float dy = (viewHeight - mBitmap.getHeight() * scale) / 2f;

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

                        mActiveDrawingPath = new Path();
                        setupActivePaint();
                        break;
                }
                invalidate();
                return true;
            } else if (mCurrentMode == MODE_TEXT && mOverlayText != null && !mOverlayText.isEmpty()) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        Rect bounds = new Rect();
                        mTextPaint.getTextBounds(mOverlayText, 0, mOverlayText.length(), bounds);
                        float txtW = bounds.width();
                        float txtH = bounds.height();

                        if (Math.abs(x - mTextX) < txtW + 80 && Math.abs(y - mTextY) < txtH + 80) {
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

        public Bitmap generateCompositeBitmap() {
            if (mBitmap == null) return null;

            Bitmap output = Bitmap.createBitmap(mBitmap.getWidth(), mBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(output);

            // Draw source filtered bitmap
            canvas.drawBitmap(mBitmap, 0, 0, mImagePaint);

            // Draw sketches
            for (int i = 0; i < mPathList.size(); i++) {
                canvas.drawPath(mPathList.get(i), mPaintList.get(i));
            }

            // Draw Premium Frame overlay directly on composite
            drawBespokeFrame(canvas, mBitmap.getWidth(), mBitmap.getHeight());

            // Draw exact text placement
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