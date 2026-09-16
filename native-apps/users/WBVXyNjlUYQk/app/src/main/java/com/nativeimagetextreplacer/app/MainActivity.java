package com.nativeimagetextreplacer.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;

public class MainActivity extends Activity implements EditorView.EditorListener {

    private static final int PICK_IMAGE_REQUEST = 101;
    private static final int PICK_OVERLAY_REQUEST = 102;

    private EditorView editorView;
    
    // Tab switching containers
    private Button btnModeText;
    private Button btnModeBrush;
    private Button btnModeOverlay;
    
    private View panelTextMode;
    private View panelBrushMode;
    private View panelOverlayMode;
    
    // Text Mode Panels
    private View panelNoSelection;
    private View panelSelection;
    private EditText etReplacement;
    private SeekBar sbTextSize;
    private Button btnBold;
    private Button btnAlignLeft;
    private Button btnAlignCenter;
    private Button btnAlignRight;
    private LinearLayout chipsContainer;

    // Brush Mode Panels
    private SeekBar sbBrushSize;
    private TextView tvBrushSizeLabel;

    private final int[] PRESET_COLORS = {
        0xFF000000, // Black
        0xFFFFFFFF, // White
        0xFFD32F2F, // Red
        0xFF1976D2, // Blue
        0xFF388E3C, // Green
        0xFFFBC02D, // Yellow
        0xFF7B1FA2, // Purple
        0xFFE64A19, // Orange
        0xFF757575  // Gray
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editorView = findViewById(R.id.editor_view);
        editorView.setListener(this);

        // Core edit mode switchers
        btnModeText = findViewById(R.id.btn_mode_text);
        btnModeBrush = findViewById(R.id.btn_mode_brush);
        btnModeOverlay = findViewById(R.id.btn_mode_overlay);
        
        panelTextMode = findViewById(R.id.container_text_mode);
        panelBrushMode = findViewById(R.id.panel_brush_mode);
        panelOverlayMode = findViewById(R.id.panel_overlay_mode);

        // Text Mode Views
        panelNoSelection = findViewById(R.id.panel_no_selection);
        panelSelection = findViewById(R.id.panel_selection);
        etReplacement = findViewById(R.id.et_replacement);
        sbTextSize = findViewById(R.id.sb_text_size);
        btnBold = findViewById(R.id.btn_bold);
        btnAlignLeft = findViewById(R.id.btn_align_left);
        btnAlignCenter = findViewById(R.id.btn_align_center);
        btnAlignRight = findViewById(R.id.btn_align_right);
        chipsContainer = findViewById(R.id.detected_chips_container);

        // Brush Mode Views
        sbBrushSize = findViewById(R.id.sb_brush_size);
        tvBrushSizeLabel = findViewById(R.id.tv_brush_size_label);

        setupTopActions();
        setupModeSwitchers();
        setupStylingListeners();
        setupColorPalettes();
        setupHindiEnglishQuickChips();
        setupBrushModeListeners();
        setupOverlayModeListeners();
        
        updateChips();
    }

    private void setupTopActions() {
        findViewById(R.id.btn_load).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, PICK_IMAGE_REQUEST);
            }
        });

        findViewById(R.id.btn_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveImageToGallery();
            }
        });

        findViewById(R.id.btn_add_box).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editorView.addCustomBlock();
                updateChips();
            }
        });
    }

    private void setupModeSwitchers() {
        btnModeText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setEditorMode(EditorView.EDIT_MODE_TEXT);
            }
        });
        
        btnModeBrush.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setEditorMode(EditorView.EDIT_MODE_BRUSH);
            }
        });
        
        btnModeOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setEditorMode(EditorView.EDIT_MODE_OVERLAY);
            }
        });
    }

    private void setEditorMode(int mode) {
        editorView.setActiveEditMode(mode);
        
        btnModeText.setBackgroundColor(mode == EditorView.EDIT_MODE_TEXT ? 0xFF0288D1 : 0xFFCFD8DC);
        btnModeText.setTextColor(mode == EditorView.EDIT_MODE_TEXT ? 0xFFFFFFFF : 0xFF37474F);
        
        btnModeBrush.setBackgroundColor(mode == EditorView.EDIT_MODE_BRUSH ? 0xFF0288D1 : 0xFFCFD8DC);
        btnModeBrush.setTextColor(mode == EditorView.EDIT_MODE_BRUSH ? 0xFFFFFFFF : 0xFF37474F);
        
        btnModeOverlay.setBackgroundColor(mode == EditorView.EDIT_MODE_OVERLAY ? 0xFF0288D1 : 0xFFCFD8DC);
        btnModeOverlay.setTextColor(mode == EditorView.EDIT_MODE_OVERLAY ? 0xFFFFFFFF : 0xFF37474F);
        
        panelTextMode.setVisibility(mode == EditorView.EDIT_MODE_TEXT ? View.VISIBLE : View.GONE);
        panelBrushMode.setVisibility(mode == EditorView.EDIT_MODE_BRUSH ? View.VISIBLE : View.GONE);
        panelOverlayMode.setVisibility(mode == EditorView.EDIT_MODE_OVERLAY ? View.VISIBLE : View.GONE);
    }

    private void setupBrushModeListeners() {
        sbBrushSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float size = Math.max(5.0f, (float) progress);
                editorView.setBrushSize(size);
                tvBrushSizeLabel.setText("Brush Size: " + (int) size);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        findViewById(R.id.btn_brush_undo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editorView.undoBrush();
            }
        });

        findViewById(R.id.btn_brush_redo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editorView.redoBrush();
            }
        });

        findViewById(R.id.btn_brush_reset).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editorView.resetBrush();
            }
        });
    }

    private void setupOverlayModeListeners() {
        findViewById(R.id.btn_add_overlay).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, PICK_OVERLAY_REQUEST);
            }
        });

        findViewById(R.id.btn_delete_overlay).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editorView.deleteSelectedOverlay();
            }
        });
    }

    private void setupHindiEnglishQuickChips() {
        findViewById(R.id.chip_hi_1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { etReplacement.setText("नमस्ते"); }
        });
        findViewById(R.id.chip_hi_2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { etReplacement.setText("धन्यवाद"); }
        });
        findViewById(R.id.chip_hi_3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { etReplacement.setText("स्वागत है"); }
        });
        findViewById(R.id.chip_en_1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { etReplacement.setText("REPLACED"); }
        });
        findViewById(R.id.chip_en_2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { etReplacement.setText("NEW TEXT"); }
        });
    }

    private void setupStylingListeners() {
        findViewById(R.id.btn_replace_apply).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextBlock selected = editorView.getSelectedBlock();
                if (selected != null) {
                    selected.replacementText = etReplacement.getText().toString();
                    selected.isReplaced = true;
                    editorView.invalidate();
                    updateChips();
                }
            }
        });

        findViewById(R.id.btn_reset_block).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextBlock selected = editorView.getSelectedBlock();
                if (selected != null) {
                    selected.isReplaced = false;
                    selected.replacementText = "";
                    etReplacement.setText("");
                    editorView.invalidate();
                    updateChips();
                }
            }
        });

        findViewById(R.id.btn_delete_block).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editorView.deleteSelectedBlock();
                updateChips();
            }
        });

        sbTextSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    TextBlock selected = editorView.getSelectedBlock();
                    if (selected != null) {
                        selected.textSize = Math.max(10.0f, (float) progress);
                        editorView.invalidate();
                    }
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnBold.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextBlock selected = editorView.getSelectedBlock();
                if (selected != null) {
                    selected.isBold = !selected.isBold;
                    btnBold.setBackgroundColor(selected.isBold ? 0xFFCCCCCC : 0xFFEEEEEE);
                    editorView.invalidate();
                }
            }
        });

        btnAlignLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextBlock selected = editorView.getSelectedBlock();
                if (selected != null) {
                    selected.alignment = "LEFT";
                    updateAlignmentUI("LEFT");
                    editorView.invalidate();
                }
            }
        });

        btnAlignCenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextBlock selected = editorView.getSelectedBlock();
                if (selected != null) {
                    selected.alignment = "CENTER";
                    updateAlignmentUI("CENTER");
                    editorView.invalidate();
                }
            }
        });

        btnAlignRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextBlock selected = editorView.getSelectedBlock();
                if (selected != null) {
                    selected.alignment = "RIGHT";
                    updateAlignmentUI("RIGHT");
                    editorView.invalidate();
                }
            }
        });
    }

    private void updateAlignmentUI(String alignment) {
        btnAlignLeft.setBackgroundColor("LEFT".equals(alignment) ? 0xFFFFFFFF : 0xFFEEEEEE);
        btnAlignCenter.setBackgroundColor("CENTER".equals(alignment) ? 0xFFFFFFFF : 0xFFEEEEEE);
        btnAlignRight.setBackgroundColor("RIGHT".equals(alignment) ? 0xFFFFFFFF : 0xFFEEEEEE);
    }

    private void setupColorPalettes() {
        LinearLayout textColorContainer = findViewById(R.id.container_text_colors);
        LinearLayout bgColorContainer = findViewById(R.id.container_bg_colors);
        
        textColorContainer.removeAllViews();
        bgColorContainer.removeAllViews();
        
        int size = (int) (24 * getResources().getDisplayMetrics().density);
        int margin = (int) (6 * getResources().getDisplayMetrics().density);
        
        for (int i = 0; i < PRESET_COLORS.length; i++) {
            final int color = PRESET_COLORS[i];
            
            View tBtn = new View(this);
            LinearLayout.LayoutParams lpT = new LinearLayout.LayoutParams(size, size);
            lpT.setMargins(margin, margin, margin, margin);
            tBtn.setLayoutParams(lpT);
            
            android.graphics.drawable.GradientDrawable gdT = new android.graphics.drawable.GradientDrawable();
            gdT.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            gdT.setColor(color);
            gdT.setStroke(2, 0xFFBDBDBD);
            tBtn.setBackground(gdT);
            
            tBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TextBlock selected = editorView.getSelectedBlock();
                    if (selected != null) {
                        selected.textColor = color;
                        editorView.invalidate();
                    }
                }
            });
            textColorContainer.addView(tBtn);
            
            View bBtn = new View(this);
            LinearLayout.LayoutParams lpB = new LinearLayout.LayoutParams(size, size);
            lpB.setMargins(margin, margin, margin, margin);
            bBtn.setLayoutParams(lpB);
            
            android.graphics.drawable.GradientDrawable gdB = new android.graphics.drawable.GradientDrawable();
            gdB.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            gdB.setColor(color);
            gdB.setStroke(2, 0xFFBDBDBD);
            bBtn.setBackground(gdB);
            
            bBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TextBlock selected = editorView.getSelectedBlock();
                    if (selected != null) {
                        selected.backgroundColor = color;
                        editorView.invalidate();
                    }
                }
            });
            bgColorContainer.addView(bBtn);
        }
    }

    private void updateChips() {
        chipsContainer.removeAllViews();
        ArrayList<TextBlock> blocks = editorView.getTextBlocks();
        int density = (int) getResources().getDisplayMetrics().density;
        
        for (int i = 0; i < blocks.size(); i++) {
            final TextBlock b = blocks.get(i);
            Button btn = new Button(this);
            
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                (int) (32 * getResources().getDisplayMetrics().density)
            );
            lp.setMargins((int)(4*density), 0, (int)(4*density), 0);
            btn.setLayoutParams(lp);
            
            String text = b.isReplaced ? b.replacementText : b.originalText;
            if (text == null || text.trim().isEmpty()) {
                text = b.originalText;
            }
            if (text.length() > 10) {
                text = text.substring(0, 8) + "..";
            }
            btn.setText(text);
            btn.setTextSize(10.0f);
            btn.setTransformationMethod(null);
            btn.setPadding((int)(8*density), 0, (int)(8*density), 0);
            
            if (b == editorView.getSelectedBlock()) {
                btn.setBackgroundColor(0xFF007AFF);
                btn.setTextColor(0xFFFFFFFF);
            } else if (b.isReplaced) {
                btn.setBackgroundColor(0xFFE8F5E9);
                btn.setTextColor(0xFF2E7D32);
            } else {
                btn.setBackgroundColor(0xFFEEEEEE);
                btn.setTextColor(0xFF212121);
            }
            
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setEditorMode(EditorView.EDIT_MODE_TEXT);
                    editorView.setSelectedBlock(b);
                    updateChips();
                }
            });
            
            chipsContainer.addView(btn);
        }
    }

    @Override
    public void onBlockSelected(TextBlock block) {
        panelNoSelection.setVisibility(View.GONE);
        panelSelection.setVisibility(View.VISIBLE);
        
        if (block != null) {
            etReplacement.setText(block.isReplaced ? block.replacementText : "");
            sbTextSize.setProgress((int) block.textSize);
            btnBold.setBackgroundColor(block.isBold ? 0xFFCCCCCC : 0xFFEEEEEE);
            updateAlignmentUI(block.alignment);
        }
        updateChips();
    }

    @Override
    public void onBlockDeselected() {
        panelNoSelection.setVisibility(View.VISIBLE);
        panelSelection.setVisibility(View.GONE);
        updateChips();
    }

    @Override
    public void onBlockStyleUpdated(TextBlock block) {
        sbTextSize.setProgress((int) block.textSize);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            if (requestCode == PICK_IMAGE_REQUEST) {
                editorView.setImageUri(data.getData());
                setEditorMode(EditorView.EDIT_MODE_TEXT);
                updateChips();
            } else if (requestCode == PICK_OVERLAY_REQUEST) {
                try {
                    java.io.InputStream is = getContentResolver().openInputStream(data.getData());
                    Bitmap overBmp = BitmapFactory.decodeStream(is);
                    if (is != null) is.close();
                    
                    if (overBmp != null) {
                        setEditorMode(EditorView.EDIT_MODE_OVERLAY);
                        editorView.startDragDropNewOverlay(overBmp);
                    } else {
                        Toast.makeText(this, "Could not load overlay bitmap", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void saveImageToGallery() {
        Bitmap finalBmp = editorView.generateFinalBitmap();
        if (finalBmp == null) {
            Toast.makeText(this, "No image currently loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        android.content.ContentValues values = new android.content.ContentValues();
        values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "TextReplacer_" + System.currentTimeMillis() + ".png");
        values.put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES);
        
        android.net.Uri uri = getContentResolver().insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri != null) {
            try {
                java.io.OutputStream out = getContentResolver().openOutputStream(uri);
                if (out != null) {
                    finalBmp.compress(Bitmap.CompressFormat.PNG, 100, out);
                    out.close();
                    Toast.makeText(this, "Success! Replaced image saved to Pictures folder", Toast.LENGTH_LONG).show();
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        try {
            java.io.File dir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
            java.io.File file = new java.io.File(dir, "replaced_" + System.currentTimeMillis() + ".png");
            java.io.FileOutputStream out = new java.io.FileOutputStream(file);
            finalBmp.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.close();
            Toast.makeText(this, "Saved to App Folder: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}