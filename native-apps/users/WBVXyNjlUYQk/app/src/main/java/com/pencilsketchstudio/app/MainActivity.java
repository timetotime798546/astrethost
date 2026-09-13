package com.pencilsketchstudio.app;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.content.ContentValues;
import android.provider.MediaStore;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends Activity {

    private DrawingView drawingView;
    private TextView tvSizeLabel;
    private TextView tvOpacityLabel;
    private TextView tvCurrentTemplateLabel;
    
    // Tools UI Buttons
    private Button btnHB, btn2B, btn6B, btnColor, btnEraser;
    
    // Current Active Attributes
    private String selectedTool = "2B";
    private int selectedColor = Color.parseColor("#1C1C1C");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawingView = (DrawingView) findViewById(R.id.drawing_view);
        tvSizeLabel = (TextView) findViewById(R.id.tv_size_label);
        tvOpacityLabel = (TextView) findViewById(R.id.tv_opacity_label);
        tvCurrentTemplateLabel = (TextView) findViewById(R.id.tv_current_template);

        // Header controls
        Button btnUndo = (Button) findViewById(R.id.btn_undo);
        Button btnRedo = (Button) findViewById(R.id.btn_redo);
        Button btnClear = (Button) findViewById(R.id.btn_clear);
        Button btnSave = (Button) findViewById(R.id.btn_save);
        Button btnPaper = (Button) findViewById(R.id.btn_paper);
        Button btnTemplateNext = (Button) findViewById(R.id.btn_template_next);

        // Selection Tools
        btnHB = (Button) findViewById(R.id.btn_tool_hb);
        btn2B = (Button) findViewById(R.id.btn_tool_2b);
        btn6B = (Button) findViewById(R.id.btn_tool_6b);
        btnColor = (Button) findViewById(R.id.btn_tool_color);
        btnEraser = (Button) findViewById(R.id.btn_tool_eraser);

        // Header Action implementations
        btnUndo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.undo();
            }
        });

        btnRedo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.redo();
            }
        });

        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.clear();
                Toast.makeText(MainActivity.this, "Sketch Cleared", Toast.LENGTH_SHORT).show();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveDrawing();
            }
        });

        btnPaper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean state = !drawingView.isPaperTextureEnabled();
                drawingView.setPaperTextureEnabled(state);
                Toast.makeText(MainActivity.this, "Paper Texture: " + (state ? "Enabled" : "Disabled"), Toast.LENGTH_SHORT).show();
            }
        });

        btnTemplateNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentGuide = drawingView.getGuideOutline();
                int nextGuide = (currentGuide + 1) % 4; // Cycles through indices 0 to 3
                drawingView.setGuideOutline(nextGuide);
                
                String labelName = "None";
                if (nextGuide == 1) labelName = "Human Portrait";
                else if (nextGuide == 2) labelName = "Botanical Flower";
                else if (nextGuide == 3) labelName = "Mountain Landscape";
                
                tvCurrentTemplateLabel.setText("Guide: " + labelName);
                Toast.makeText(MainActivity.this, "Draft Outline: " + labelName, Toast.LENGTH_SHORT).show();
            }
        });

        // Initialize lead thickness SeekBar
        SeekBar sbSize = (SeekBar) findViewById(R.id.sb_size);
        sbSize.setMax(100);
        sbSize.setProgress(12);
        drawingView.setStrokeWidth(12);
        tvSizeLabel.setText("Lead Size: 12px");
        sbSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int finalSize = Math.max(2, progress);
                drawingView.setStrokeWidth(finalSize);
                tvSizeLabel.setText("Lead Size: " + finalSize + "px");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Initialize lead hardness SeekBar
        SeekBar sbOpacity = (SeekBar) findViewById(R.id.sb_opacity);
        sbOpacity.setMax(255);
        sbOpacity.setProgress(200);
        drawingView.setOpacity(200);
        tvOpacityLabel.setText("Lead Opacity: 78%");
        sbOpacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int finalOpacity = Math.max(10, progress);
                drawingView.setOpacity(finalOpacity);
                int pctVal = (int) ((finalOpacity / 255.0) * 100);
                tvOpacityLabel.setText("Lead Opacity: " + pctVal + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Tool Mode selection clicks
        btnHB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTool("HB");
            }
        });
        btn2B.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTool("2B");
            }
        });
        btn6B.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTool("6B");
            }
        });
        btnColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTool("COLOR");
            }
        });
        btnEraser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTool("ERASER");
            }
        });

        setupColorPalette();
        updateToolSelectionHighlight();
    }

    private void selectTool(String tool) {
        selectedTool = tool;
        drawingView.setTool(tool);
        updateToolSelectionHighlight();
        Toast.makeText(this, "Tool changed: " + getToolFullName(tool), Toast.LENGTH_SHORT).show();
    }

    private String getToolFullName(String tool) {
        if ("HB".equals(tool)) return "HB Graphite Lead";
        if ("2B".equals(tool)) return "2B Sketch Lead";
        if ("6B".equals(tool)) return "6B Dark Charcoal";
        if ("COLOR".equals(tool)) return "Color Lead Pencil";
        if ("ERASER".equals(tool)) return "Sketch Rubber/Eraser";
        return tool;
    }

    private void updateToolSelectionHighlight() {
        // Reset colors
        String inactiveBg = "#E0E0E0";
        btnHB.setBackgroundColor(Color.parseColor(inactiveBg));
        btn2B.setBackgroundColor(Color.parseColor(inactiveBg));
        btn6B.setBackgroundColor(Color.parseColor(inactiveBg));
        btnColor.setBackgroundColor(Color.parseColor(inactiveBg));
        btnEraser.setBackgroundColor(Color.parseColor(inactiveBg));

        btnHB.setTextColor(Color.BLACK);
        btn2B.setTextColor(Color.BLACK);
        btn6B.setTextColor(Color.BLACK);
        btnColor.setTextColor(Color.BLACK);
        btnEraser.setTextColor(Color.BLACK);

        Button highlight = null;
        if ("HB".equals(selectedTool)) highlight = btnHB;
        else if ("2B".equals(selectedTool)) highlight = btn2B;
        else if ("6B".equals(selectedTool)) highlight = btn6B;
        else if ("COLOR".equals(selectedTool)) highlight = btnColor;
        else if ("ERASER".equals(selectedTool)) highlight = btnEraser;

        if (highlight != null) {
            highlight.setBackgroundColor(Color.parseColor("#37474F"));
            highlight.setTextColor(Color.WHITE);
        }
    }

    private void setupColorPalette() {
        LinearLayout drawer = (LinearLayout) findViewById(R.id.colors_layout_container);
        drawer.removeAllViews();

        final String[] hexPencils = {
            "#1C1C1C", // Charcoal Dark
            "#4E4E4E", // Lead Grey
            "#7A8B99", // Slate Blue Grey
            "#D32F2F", // Carmine Red
            "#1976D2", // Cobalt Blue
            "#388E3C", // Emerald Green
            "#FBC02D", // Ochre Yellow
            "#E64A19", // Amber Orange
            "#7B1FA2", // Violet
            "#8D6E63", // Sienna Brown
            "#D81B60"  // Wax Pink
        };

        for (int i = 0; i < hexPencils.length; i++) {
            final String hex = hexPencils[i];
            final int codeColor = Color.parseColor(hex);

            View leadDot = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(70, 70);
            lp.setMargins(14, 8, 14, 8);
            leadDot.setLayoutParams(lp);

            // Shape the color pencil tip
            GradientDrawable circleShape = new GradientDrawable();
            circleShape.setShape(GradientDrawable.OVAL);
            circleShape.setColor(codeColor);
            circleShape.setStroke(4, Color.parseColor("#ECEFF1"));
            leadDot.setBackground(circleShape);

            leadDot.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedColor = codeColor;
                    drawingView.setPaintColor(codeColor);
                    
                    // Direct switch to coloring tool if eraser was active
                    if ("ERASER".equals(selectedTool)) {
                        selectTool("COLOR");
                    }
                    Toast.makeText(MainActivity.this, "Lead color updated", Toast.LENGTH_SHORT).show();
                }
            });

            drawer.addView(leadDot);
        }
    }

    private void saveDrawing() {
        Bitmap outputBmp = drawingView.getCanvasBitmap();
        if (outputBmp == null) {
            Toast.makeText(this, "Empty Canvas", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String name = "Sketch_" + UUID.randomUUID().toString().substring(0, 8) + ".png";
            ContentValues entry = new ContentValues();
            entry.put(MediaStore.Images.Media.DISPLAY_NAME, name);
            entry.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            entry.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PencilSketchStudio");

            android.net.Uri outputUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, entry);
            if (outputUri != null) {
                OutputStream os = getContentResolver().openOutputStream(outputUri);
                if (os != null) {
                    outputBmp.compress(Bitmap.CompressFormat.PNG, 100, os);
                    os.close();
                    Toast.makeText(this, "Sketch saved to Pictures/PencilSketchStudio!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Failed saving stream", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Failed creating media file context", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving sketch: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}