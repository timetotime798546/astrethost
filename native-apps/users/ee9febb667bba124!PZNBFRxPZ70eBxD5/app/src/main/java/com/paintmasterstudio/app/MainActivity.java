package com.paintmasterstudio.app;

import android.app.Activity;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.FileOutputStream;

public class MainActivity extends Activity {

    private DrawingView drawingView;
    private FrameLayout canvasContainer;
    private TextView txtBrushSize;
    private SeekBar seekBrushSize;

    private Button btnUndo, btnRedo, btnClear, btnSave;
    private Button btnFreehand, btnLine, btnRect, btnCircle, btnEraser, btnBoardBg;

    private int currentBgIndex = 0;
    private final int[] bgColors = new int[]{
        Color.WHITE,
        Color.parseColor("#F5F5DC"), 
        Color.parseColor("#D3D3D3"), 
        Color.parseColor("#222222")  
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        canvasContainer = (FrameLayout) findViewById(R.id.canvas_container);
        drawingView = new DrawingView(this);
        canvasContainer.addView(drawingView);

        txtBrushSize = (TextView) findViewById(R.id.txt_brush_size);
        seekBrushSize = (SeekBar) findViewById(R.id.seek_brush_size);

        btnUndo = (Button) findViewById(R.id.btn_undo);
        btnRedo = (Button) findViewById(R.id.btn_redo);
        btnClear = (Button) findViewById(R.id.btn_clear);
        btnSave = (Button) findViewById(R.id.btn_save);

        btnFreehand = (Button) findViewById(R.id.btn_tool_freehand);
        btnLine = (Button) findViewById(R.id.btn_tool_line);
        btnRect = (Button) findViewById(R.id.btn_tool_rect);
        btnCircle = (Button) findViewById(R.id.btn_tool_circle);
        btnEraser = (Button) findViewById(R.id.btn_tool_eraser);
        btnBoardBg = (Button) findViewById(R.id.btn_board_bg);

        setupColorPalette();
        setupBrushSizeBar();
        setupDrawingActions();
        setupToolButtons();
        
        highlightActiveTool(btnFreehand);
    }

    private void setupColorPalette() {
        int[] colorIds = new int[]{
            R.id.color_black, R.id.color_red, R.id.color_green,
            R.id.color_blue, R.id.color_yellow, R.id.color_orange,
            R.id.color_purple, R.id.color_cyan, R.id.color_pink, R.id.color_brown
        };

        View.OnClickListener colorClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String hexColor = (String) v.getTag();
                if (hexColor != null) {
                    int color = Color.parseColor(hexColor);
                    drawingView.setBrushColor(color);
                    drawingView.setEraserActive(false);
                    
                    highlightActiveTool(btnFreehand); 
                    Toast.makeText(MainActivity.this, "Color Switched!", Toast.LENGTH_SHORT).show();
                }
            } 
        };

        for (int id : colorIds) {
            findViewById(id).setOnClickListener(colorClickListener);
        }
    }

    private void setupBrushSizeBar() {
        seekBrushSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int val = progress < 1 ? 1 : progress;
                txtBrushSize.setText("Size: " + val + "px");
                drawingView.setStrokeWidth(val);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void setupDrawingActions() {
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
                drawingView.clearCanvas();
                Toast.makeText(MainActivity.this, "Canvas Cleared", Toast.LENGTH_SHORT).show();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveDrawingToGallery();
            }
        });
    }

    private void setupToolButtons() {
        btnFreehand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.setShapeType("FREEHAND");
                drawingView.setEraserActive(false);
                highlightActiveTool(btnFreehand);
            }
        });

        btnLine.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.setShapeType("LINE");
                drawingView.setEraserActive(false);
                highlightActiveTool(btnLine);
            }
        });

        btnRect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.setShapeType("RECT");
                drawingView.setEraserActive(false);
                highlightActiveTool(btnRect);
            }
        });

        btnCircle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.setShapeType("CIRCLE");
                drawingView.setEraserActive(false);
                highlightActiveTool(btnCircle);
            }
        });

        btnEraser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.setEraserActive(true);
                highlightActiveTool(btnEraser);
            }
        });

        btnBoardBg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentBgIndex = (currentBgIndex + 1) % bgColors.length;
                int chosenBgColor = bgColors[currentBgIndex];
                drawingView.setBgColor(chosenBgColor);
                canvasContainer.setBackgroundColor(chosenBgColor);
                Toast.makeText(MainActivity.this, "BG Swapped!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void highlightActiveTool(Button activeBtn) {
        Button[] tools = new Button[]{btnFreehand, btnLine, btnRect, btnCircle, btnEraser};
        for (Button b : tools) {
            b.setBackgroundColor(Color.parseColor("#3E3E3E"));
            b.setTextColor(Color.parseColor("#BBBBBB"));
        }
        activeBtn.setBackgroundColor(Color.parseColor("#00E676"));
        activeBtn.setTextColor(Color.parseColor("#000000"));
    }

    private void saveDrawingToGallery() {
        try {
            String title = "Drawing_" + System.currentTimeMillis();
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, title + ".png");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PaintStudio");
                values.put(MediaStore.Images.Media.IS_PENDING, 1);
            }

            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "w");
                if (pfd != null) {
                    FileOutputStream fos = new FileOutputStream(pfd.getFileDescriptor());
                    Bitmap bitmap = drawingView.getCanvasBitmap();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    fos.close();
                    pfd.close();
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear();
                    values.put(MediaStore.Images.Media.IS_PENDING, 0);
                    getContentResolver().update(uri, values, null, null);
                }
                Toast.makeText(this, "Artwork Saved to Gallery!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Error creating file registry entry", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
