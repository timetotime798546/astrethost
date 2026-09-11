package com.threeddesignstudio.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {

    // 3D Math structural data models
    public static class Vertex {
        public float x, y, z;
        public Vertex(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public static class Face {
        public int[] vertexIndices;
        public float avgZ;
        public float[] normal = new float[3];

        public Face(int[] indices) {
            this.vertexIndices = indices;
        }
    }

    // Shapes presets definitions
    private enum ShapeType {
        CUBE, PYRAMID, PRISM, CYLINDER, SPHERE
    }

    private enum RenderMode {
        SOLID, WIREFRAME, POINTS
    }

    // Material system pre-configured colors
    private final int[] colorPalette = {
        Color.parseColor("#EF4444"), // Ruby Red
        Color.parseColor("#F97316"), // Vibrant Orange
        Color.parseColor("#F59E0B"), // Amber Yellow
        Color.parseColor("#10B981"), // Emerald Green
        Color.parseColor("#06B6D4"), // Sky Cyan
        Color.parseColor("#3B82F6"), // Royal Blue
        Color.parseColor("#8B5CF6"), // Electric Purple
        Color.parseColor("#EC4899"), // Bubblegum Pink
        Color.parseColor("#F8FAFC"), // Arctic White
        Color.parseColor("#64748B")  // Metallic Grey
    };

    private ShapeType currentShape = ShapeType.CUBE;
    private RenderMode currentRenderMode = RenderMode.SOLID;
    private int selectedMaterialColor = Color.parseColor("#3B82F6"); // Default Royal Blue
    private float strokeThickness = 4.0f;
    private float globalScale = 1.0f;
    private float cameraDistance = 4.0f;
    private float lightAngleDegrees = 45.0f;

    // Projection Canvas instance
    private ThreeDViewportView viewportView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inject dynamic custom projection canvas inside Viewport container FrameLayout
        FrameLayout container = findViewById(R.id.viewport_container);
        viewportView = new ThreeDViewportView(this);
        container.addView(viewportView);

        // Setup all modules
        setupTabs();
        setupShapePanel();
        setupColorPanel();
        setupTransformPanel();
        setupProjectPanel();

        // Setup Header and render mode overlays
        setupShadingModes();
        
        findViewById(R.id.btn_reset_view).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewportView.resetViewportRotation();
            }
        });

        updateHUD();
    }

    private void setupTabs() {
        final Button tabShapes = findViewById(R.id.tab_shapes);
        final Button tabColors = findViewById(R.id.tab_colors);
        final Button tabTransforms = findViewById(R.id.tab_transforms);
        final Button tabProjects = findViewById(R.id.tab_projects);

        final View panelShapes = findViewById(R.id.panel_shapes);
        final View panelColors = findViewById(R.id.panel_colors);
        final View panelTransforms = findViewById(R.id.panel_transforms);
        final View panelProjects = findViewById(R.id.panel_projects);

        final Button[] tabs = {tabShapes, tabColors, tabTransforms, tabProjects};
        final View[] panels = {panelShapes, panelColors, panelTransforms, panelProjects};

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View clickedTab) {
                for (int i = 0; i < tabs.length; i++) {
                    if (tabs[i] == clickedTab) {
                        tabs[i].setBackgroundColor(Color.parseColor("#F1F5F9"));
                        tabs[i].setTextColor(Color.parseColor("#0F172A"));
                        tabs[i].setTypeface(null, Typeface.BOLD);
                        panels[i].setVisibility(View.VISIBLE);
                    } else {
                        tabs[i].setBackgroundColor(Color.parseColor("#E2E8F0"));
                        tabs[i].setTextColor(Color.parseColor("#64748B"));
                        tabs[i].setTypeface(null, Typeface.NORMAL);
                        panels[i].setVisibility(View.GONE);
                    }
                }
            }
        };

        tabShapes.setOnClickListener(listener);
        tabColors.setOnClickListener(listener);
        tabTransforms.setOnClickListener(listener);
        tabProjects.setOnClickListener(listener);
    }

    private void setupShapePanel() {
        final Button btnCube = findViewById(R.id.btn_shape_cube);
        final Button btnPyramid = findViewById(R.id.btn_shape_pyramid);
        final Button btnPrism = findViewById(R.id.btn_shape_prism);
        final Button btnCylinder = findViewById(R.id.btn_shape_cylinder);
        final Button btnSphere = findViewById(R.id.btn_shape_sphere);

        final Button[] shapeButtons = {btnCube, btnPyramid, btnPrism, btnCylinder, btnSphere};
        final ShapeType[] types = {ShapeType.CUBE, ShapeType.PYRAMID, ShapeType.PRISM, ShapeType.CYLINDER, ShapeType.SPHERE};

        View.OnClickListener shapeClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View clickedBtn) {
                for (int i = 0; i < shapeButtons.length; i++) {
                    if (shapeButtons[i] == clickedBtn) {
                        shapeButtons[i].setBackgroundColor(Color.parseColor("#10B981"));
                        shapeButtons[i].setTextColor(Color.WHITE);
                        shapeButtons[i].setTypeface(null, Typeface.BOLD);
                        currentShape = types[i];
                    } else {
                        shapeButtons[i].setBackgroundColor(Color.parseColor("#E2E8F0"));
                        shapeButtons[i].setTextColor(Color.parseColor("#1E293B"));
                        shapeButtons[i].setTypeface(null, Typeface.NORMAL);
                    }
                }
                viewportView.rebuildShape();
                updateHUD();
            }
        };

        btnCube.setOnClickListener(shapeClickListener);
        btnPyramid.setOnClickListener(shapeClickListener);
        btnPrism.setOnClickListener(shapeClickListener);
        btnCylinder.setOnClickListener(shapeClickListener);
        btnSphere.setOnClickListener(shapeClickListener);
    }

    private void setupColorPanel() {
        LinearLayout palette = findViewById(R.id.palette_container);
        palette.removeAllViews();

        int circleSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, getResources().getDisplayMetrics());
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, getResources().getDisplayMetrics());

        for (int i = 0; i < colorPalette.length; i++) {
            final int color = colorPalette[i];
            final View circle = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(circleSize, circleSize);
            params.setMargins(margin, 0, margin, 0);
            circle.setLayoutParams(params);

            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL);
            shape.setColor(color);
            shape.setStroke(2, Color.parseColor("#CBD5E1"));
            circle.setBackground(shape);

            circle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedMaterialColor = color;
                    viewportView.invalidate();
                    Toast.makeText(MainActivity.this, "Material Color Applied!", Toast.LENGTH_SHORT).show();
                }
            });

            palette.addView(circle);
        }

        // Setup Stroke Seekbar
        final TextView strokeLabel = findViewById(R.id.txt_stroke_label);
        SeekBar seekStroke = findViewById(R.id.seek_stroke);
        seekStroke.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                strokeThickness = progress + 1.0f;
                strokeLabel.setText("Line Thickness: " + (int) strokeThickness + " px");
                viewportView.invalidate();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void setupTransformPanel() {
        final TextView scaleLabel = findViewById(R.id.txt_scale_label);
        SeekBar seekScale = findViewById(R.id.seek_scale);
        seekScale.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // scale ranges from 0.2 to 2.5
                globalScale = 0.2f + (progress / 100.0f) * 2.3f;
                scaleLabel.setText("Model Scale: " + String.format("%.1f", globalScale) + "x");
                viewportView.invalidate();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        final TextView lightLabel = findViewById(R.id.txt_light_label);
        SeekBar seekLight = findViewById(R.id.seek_light);
        seekLight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                lightAngleDegrees = progress;
                lightLabel.setText("Light Direction: " + (int) lightAngleDegrees + "°");
                viewportView.invalidate();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        final TextView cameraLabel = findViewById(R.id.txt_depth_label);
        SeekBar seekCamera = findViewById(R.id.seek_camera);
        seekCamera.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // distance ranges from 2.0 to 10.0
                cameraDistance = 2.0f + (progress / 100.0f) * 8.0f;
                cameraLabel.setText("Camera Distance: " + String.format("%.1f", cameraDistance));
                viewportView.invalidate();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void setupProjectPanel() {
        final EditText edtName = findViewById(R.id.edt_project_name);
        Button btnSave = findViewById(R.id.btn_save_project);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = edtName.getText().toString().trim();
                if (name.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter a design name!", Toast.LENGTH_SHORT).show();
                    return;
                }
                saveDesignProject(name);
            }
        });

        loadAndDisplayProjects();
    }

    private void saveDesignProject(String name) {
        SharedPreferences prefs = getSharedPreferences("3D_DESIGN_PROJECTS", MODE_PRIVATE);
        // Serialization: shape_type|color|scale|lightAngle|cameraDist|renderMode
        String data = currentShape.name() + "|" + 
                      selectedMaterialColor + "|" + 
                      globalScale + "|" + 
                      lightAngleDegrees + "|" + 
                      cameraDistance + "|" + 
                      currentRenderMode.name();

        prefs.edit().putString(name, data).apply();
        Toast.makeText(this, "Project Saved Successfully!", Toast.LENGTH_SHORT).show();
        loadAndDisplayProjects();
    }

    private void loadAndDisplayProjects() {
        LinearLayout container = findViewById(R.id.saved_projects_container);
        container.removeAllViews();

        SharedPreferences prefs = getSharedPreferences("3D_DESIGN_PROJECTS", MODE_PRIVATE);
        Map<String, ?> allEntries = prefs.getAll();

        if (allEntries.isEmpty()) {
            TextView txtNone = new TextView(this);
            txtNone.setText("No saved designs yet.");
            txtNone.setTextColor(Color.parseColor("#94A3B8"));
            txtNone.setTextSize(12sp);
            container.addView(txtNone);
            return;
        }

        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());

        for (final Map.Entry<String, ?> entry : allEntries.entrySet()) {
            final String key = entry.getKey();
            final String value = entry.getValue().toString();

            Button btnProj = new Button(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(margin, 0, margin, 0);
            btnProj.setLayoutParams(params);
            btnProj.setText(key);
            btnProj.setTextSize(11sp);
            btnProj.setTextColor(Color.parseColor("#1E293B"));
            btnProj.setPadding(padding, padding, padding, padding);
            btnProj.setTransformationMethod(null);
            
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setColor(Color.parseColor("#F1F5F9"));
            shape.setStroke(2, Color.parseColor("#CBD5E1"));
            shape.setCornerRadius(10f);
            btnProj.setBackground(shape);

            btnProj.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        String[] parts = value.split("\\|");
                        currentShape = ShapeType.valueOf(parts[0]);
                        selectedMaterialColor = Integer.parseInt(parts[1]);
                        globalScale = Float.parseFloat(parts[2]);
                        lightAngleDegrees = Float.parseFloat(parts[3]);
                        cameraDistance = Float.parseFloat(parts[4]);
                        currentRenderMode = RenderMode.valueOf(parts[5]);

                        // Update seekbars and configurations
                        viewportView.rebuildShape();
                        viewportView.invalidate();
                        updateHUD();
                        syncUiConfigurations();

                        Toast.makeText(MainActivity.this, "Loaded " + key + " Design!", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Failed loading project structure", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            // Long click deletes project
            btnProj.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    SharedPreferences p = getSharedPreferences("3D_DESIGN_PROJECTS", MODE_PRIVATE);
                    p.edit().remove(key).apply();
                    Toast.makeText(MainActivity.this, "Deleted design: " + key, Toast.LENGTH_SHORT).show();
                    loadAndDisplayProjects();
                    return true;
                }
            });

            container.addView(btnProj);
        }
    }

    private void syncUiConfigurations() {
        SeekBar seekScale = findViewById(R.id.seek_scale);
        int scaleProg = (int) (((globalScale - 0.2f) / 2.3f) * 100);
        seekScale.setProgress(scaleProg);

        SeekBar seekLight = findViewById(R.id.seek_light);
        seekLight.setProgress((int) lightAngleDegrees);

        SeekBar seekCamera = findViewById(R.id.seek_camera);
        int camProg = (int) (((cameraDistance - 2.0f) / 8.0f) * 100);
        seekCamera.setProgress(camProg);

        // Update shading buttons
        updateRenderModeButtons();
    }

    private void setupShadingModes() {
        final Button btnSolid = findViewById(R.id.btn_mode_solid);
        final Button btnWire = findViewById(R.id.btn_mode_wire);
        final Button btnPoints = findViewById(R.id.btn_mode_points);

        View.OnClickListener clicker = new View.OnClickListener() {
            @Override
            public void onClick(View clickedBtn) {
                if (clickedBtn == btnSolid) {
                    currentRenderMode = RenderMode.SOLID;
                } else if (clickedBtn == btnWire) {
                    currentRenderMode = RenderMode.WIREFRAME;
                } else if (clickedBtn == btnPoints) {
                    currentRenderMode = RenderMode.POINTS;
                }
                updateRenderModeButtons();
                viewportView.invalidate();
            }
        };

        btnSolid.setOnClickListener(clicker);
        btnWire.setOnClickListener(clicker);
        btnPoints.setOnClickListener(clicker);
    }

    private void updateRenderModeButtons() {
        Button btnSolid = findViewById(R.id.btn_mode_solid);
        Button btnWire = findViewById(R.id.btn_mode_wire);
        Button btnPoints = findViewById(R.id.btn_mode_points);

        btnSolid.setBackgroundColor(Color.parseColor(currentRenderMode == RenderMode.SOLID ? "#10B981" : "#334155"));
        btnSolid.setTextColor(currentRenderMode == RenderMode.SOLID ? Color.WHITE : Color.parseColor("#94A3B8"));

        btnWire.setBackgroundColor(Color.parseColor(currentRenderMode == RenderMode.WIREFRAME ? "#10B981" : "#334155"));
        btnWire.setTextColor(currentRenderMode == RenderMode.WIREFRAME ? Color.WHITE : Color.parseColor("#94A3B8"));

        btnPoints.setBackgroundColor(Color.parseColor(currentRenderMode == RenderMode.POINTS ? "#10B981" : "#334155"));
        btnPoints.setTextColor(currentRenderMode == RenderMode.POINTS ? Color.WHITE : Color.parseColor("#94A3B8"));
    }

    private void updateHUD() {
        TextView hudShape = findViewById(R.id.hud_shape);
        TextView hudStats = findViewById(R.id.hud_stats);

        hudShape.setText("Shape: " + currentShape.name());
        
        int verts = 0;
        int faces = 0;
        if (viewportView != null) {
            verts = viewportView.getVerticesCount();
            faces = viewportView.getFacesCount();
        }
        hudStats.setText("Vertices: " + verts + " | Faces: " + faces);
    }

    // Dynamic View viewport designed with custom 3D Projection math on standard canvas
    private class ThreeDViewportView extends View {

        private Vertex[] meshVertices;
        private Face[] meshFaces;

        private float rotationX = 45.0f; // degrees
        private float rotationY = 45.0f; // degrees
        private float rotationZ = 0.0f;  // degrees

        private float lastTouchX;
        private float lastTouchY;

        private Paint fillPaint;
        private Paint strokePaint;
        private Paint vertexPaint;

        public ThreeDViewportView(Context context) {
            super(context);
            initView();
        }

        private void initView() {
            fillPaint = new Paint();
            fillPaint.setAntiAlias(true);
            fillPaint.setStyle(Paint.Style.FILL);

            strokePaint = new Paint();
            strokePaint.setAntiAlias(true);
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setColor(Color.parseColor("#1E293B"));

            vertexPaint = new Paint();
            vertexPaint.setAntiAlias(true);
            vertexPaint.setStyle(Paint.Style.FILL);

            rebuildShape();
        }

        public void resetViewportRotation() {
            rotationX = 45.0f;
            rotationY = 45.0f;
            rotationZ = 0.0f;
            invalidate();
            updateHUDText();
        }

        public int getVerticesCount() {
            return meshVertices != null ? meshVertices.length : 0;
        }

        public int getFacesCount() {
            return meshFaces != null ? meshFaces.length : 0;
        }

        public void rebuildShape() {
            switch (currentShape) {
                case CUBE:
                    meshVertices = new Vertex[]{
                        new Vertex(-1f, -1f, -1f),
                        new Vertex(1f, -1f, -1f),
                        new Vertex(1f, 1f, -1f),
                        new Vertex(-1f, 1f, -1f),
                        new Vertex(-1f, -1f, 1f),
                        new Vertex(1f, -1f, 1f),
                        new Vertex(1f, 1f, 1f),
                        new Vertex(-1f, 1f, 1f)
                    };
                    meshFaces = new Face[]{
                        new Face(new int[]{0, 1, 2, 3}), // Front
                        new Face(new int[]{5, 4, 7, 6}), // Back
                        new Face(new int[]{4, 5, 1, 0}), // Bottom
                        new Face(new int[]{3, 2, 6, 7}), // Top
                        new Face(new int[]{4, 0, 3, 7}), // Left
                        new Face(new int[]{1, 5, 6, 2})  // Right
                    };
                    break;

                case PYRAMID:
                    meshVertices = new Vertex[]{
                        new Vertex(-1f, -1f, -1f),
                        new Vertex(1f, -1f, -1f),
                        new Vertex(1f, -1f, 1f),
                        new Vertex(-1f, -1f, 1f),
                        new Vertex(0f, 1.2f, 0f) // Apex top
                    };
                    meshFaces = new Face[]{
                        new Face(new int[]{3, 2, 1, 0}), // Base
                        new Face(new int[]{0, 1, 4}),    // Front face
                        new Face(new int[]{1, 2, 4}),    // Right face
                        new Face(new int[]{2, 3, 4}),    // Back face
                        new Face(new int[]{3, 0, 4})     // Left face
                    };
                    break;

                case PRISM: // Triangular Prism
                    meshVertices = new Vertex[]{
                        new Vertex(-0.8f, -1f, -1f),
                        new Vertex(0.8f, -1f, -1f),
                        new Vertex(0f, 0.8f, -1f), // Back Cap
                        new Vertex(-0.8f, -1f, 1f),
                        new Vertex(0.8f, -1f, 1f),
                        new Vertex(0f, 0.8f, 1f)   // Front Cap
                    };
                    meshFaces = new Face[]{
                        new Face(new int[]{0, 1, 2}),    // Back Cap
                        new Face(new int[]{3, 5, 4}),    // Front Cap
                        new Face(new int[]{0, 3, 4, 1}), // Bottom Quad
                        new Face(new int[]{0, 2, 5, 3}), // Left slant
                        new Face(new int[]{1, 4, 5, 2})  // Right slant
                    };
                    break;

                case CYLINDER:
                    int segments = 10;
                    meshVertices = new Vertex[segments * 2];
                    // Bottom circle (z = -1)
                    for (int i = 0; i < segments; i++) {
                        double rad = i * 2 * Math.PI / segments;
                        meshVertices[i] = new Vertex((float) Math.cos(rad) * 0.8f, (float) Math.sin(rad) * 0.8f, -1f);
                    }
                    // Top circle (z = 1)
                    for (int i = 0; i < segments; i++) {
                        double rad = i * 2 * Math.PI / segments;
                        meshVertices[segments + i] = new Vertex((float) Math.cos(rad) * 0.8f, (float) Math.sin(rad) * 0.8f, 1f);
                    }

                    List<Face> faceList = new ArrayList<>();
                    // Connecting side quads
                    for (int i = 0; i < segments; i++) {
                        int next = (i + 1) % segments;
                        faceList.add(new Face(new int[]{i, next, segments + next, segments + i}));
                    }
                    // Bottom cap indices
                    int[] bottomCap = new int[segments];
                    for (int i = 0; i < segments; i++) {
                        bottomCap[i] = segments - 1 - i;
                    }
                    faceList.add(new Face(bottomCap));
                    // Top cap indices
                    int[] topCap = new int[segments];
                    for (int i = 0; i < segments; i++) {
                        topCap[i] = segments + i;
                    }
                    faceList.add(new Face(topCap));

                    meshFaces = faceList.toArray(new Face[0]);
                    break;

                case SPHERE:
                    int rings = 6;
                    int sectors = 10;
                    List<Vertex> sphereVerts = new ArrayList<>();
                    for (int r = 0; r <= rings; r++) {
                        double phi = Math.PI * r / rings;
                        float y = (float) Math.cos(phi) * 1.1f;
                        float sinPhi = (float) Math.sin(phi) * 1.1f;
                        for (int s = 0; s < sectors; s++) {
                            double theta = 2 * Math.PI * s / sectors;
                            float x = sinPhi * (float) Math.cos(theta);
                            float z = sinPhi * (float) Math.sin(theta);
                            sphereVerts.add(new Vertex(x, y, z));
                        }
                    }
                    meshVertices = sphereVerts.toArray(new Vertex[0]);

                    List<Face> sphereFaces = new ArrayList<>();
                    for (int r = 0; r < rings; r++) {
                        for (int s = 0; s < sectors; s++) {
                            int nextS = (s + 1) % sectors;
                            int i00 = r * sectors + s;
                            int i01 = r * sectors + nextS;
                            int i10 = (r + 1) * sectors + s;
                            int i11 = (r + 1) * sectors + nextS;
                            sphereFaces.add(new Face(new int[]{i00, i01, i11, i10}));
                        }
                    }
                    meshFaces = sphereFaces.toArray(new Face[0]);
                    break;
            }
            invalidate();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX();
            float y = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastTouchX = x;
                    lastTouchY = y;
                    break;
                case MotionEvent.ACTION_MOVE:
                    float dx = x - lastTouchX;
                    float dy = y - lastTouchY;

                    // Update viewport viewing angles
                    rotationY += dx * 0.5f;
                    rotationX += dy * 0.5f;

                    lastTouchX = x;
                    lastTouchY = y;
                    invalidate();
                    updateHUDText();
                    break;
            }
            return true;
        }

        private void updateHUDText() {
            TextView hudRotation = findViewById(R.id.hud_rotation);
            if (hudRotation != null) {
                int displayX = ((int) rotationX) % 360;
                int displayY = ((int) rotationY) % 360;
                hudRotation.setText("Rot X: " + displayX + "° | Y: " + displayY + "°");
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (meshVertices == null || meshFaces == null) return;

            int width = getWidth();
            int height = getHeight();

            // Pre-calculate view coordinates projection
            float scaleFactor = Math.min(width, height) * 0.35f;

            // Generate temporary transformed rotated coordinates projection array
            Vertex[] rotated = new Vertex[meshVertices.length];
            float radX = (float) Math.toRadians(rotationX);
            float radY = (float) Math.toRadians(rotationY);
            float radZ = (float) Math.toRadians(rotationZ);

            float cosX = (float) Math.cos(radX), sinX = (float) Math.sin(radX);
            float cosY = (float) Math.cos(radY), sinY = (float) Math.sin(radY);
            float cosZ = (float) Math.cos(radZ), sinZ = (float) Math.sin(radZ);

            for (int i = 0; i < meshVertices.length; i++) {
                Vertex v = meshVertices[i];

                // 1. Independent dimension transformations scaling
                float x0 = v.x * globalScale;
                float y0 = v.y * globalScale;
                float z0 = v.z * globalScale;

                // 2. Rotate Pitch (Around X axis)
                float y1 = y0 * cosX - z0 * sinX;
                float z1 = y0 * sinX + z0 * cosX;

                // 3. Rotate Yaw (Around Y axis)
                float x2 = x0 * cosY + z1 * sinY;
                float z2 = -x0 * sinY + z1 * cosY;

                // 4. Rotate Roll (Around Z axis)
                float x3 = x2 * cosZ - y1 * sinZ;
                float y3 = x2 * sinZ + y1 * cosZ;

                rotated[i] = new Vertex(x3, y3, z2);
            }

            // Depth index caching for Painter's algorithm
            for (Face face : meshFaces) {
                float sumZ = 0.0f;
                for (int idx : face.vertexIndices) {
                    sumZ += rotated[idx].z;
                }
                face.avgZ = sumZ / face.vertexIndices.length;
            }

            // Sort polygons using Java 8 compatible anonymous inner class depth sorting
            Arrays.sort(meshFaces, new Comparator<Face>() {
                @Override
                public int compare(Face f1, Face f2) {
                    if (f1.avgZ < f2.avgZ) return -1;
                    if (f1.avgZ > f2.avgZ) return 1;
                    return 0;
                }
            });

            // Lighting direction components derived from light position seekbar
            float lightRad = (float) Math.toRadians(lightAngleDegrees);
            float lx = (float) Math.cos(lightRad);
            float ly = 1.0f; // Ambient vertical offset
            float lz = (float) Math.sin(lightRad);
            float lenL = (float) Math.sqrt(lx * lx + ly * ly + lz * lz);
            lx /= lenL; ly /= lenL; lz /= lenL;

            // Render polygons
            for (Face face : meshFaces) {
                // Screen coordinates mapping path builder
                Path path = new Path();
                boolean validFace = true;
                float firstPx = 0f, firstPy = 0f;

                for (int j = 0; j < face.vertexIndices.length; j++) {
                    int idx = face.vertexIndices[j];
                    Vertex r = rotated[idx];

                    // Standard perspective projection formula mapping with camera clipping safeguard
                    float divisor = cameraDistance - r.z;
                    if (divisor < 0.2f) divisor = 0.2f;

                    float px = (r.x / divisor) * scaleFactor + (width / 2f);
                    float py = (-r.y / divisor) * scaleFactor + (height / 2f);

                    if (j == 0) {
                        path.moveTo(px, py);
                        firstPx = px;
                        firstPy = py;
                    } else {
                        path.lineTo(px, py);
                    }
                }
                path.close();

                // Surface normal cross product vector calculation for shading intensity
                if (face.vertexIndices.length >= 3) {
                    Vertex v0 = rotated[face.vertexIndices[0]];
                    Vertex v1 = rotated[face.vertexIndices[1]];
                    Vertex v2 = rotated[face.vertexIndices[2]];

                    float ax = v1.x - v0.x;
                    float ay = v1.y - v0.y;
                    float az = v1.z - v0.z;

                    float bx = v2.x - v0.x;
                    float by = v2.y - v0.y;
                    float bz = v2.z - v0.z;

                    float nx = ay * bz - az * by;
                    float ny = az * bx - ax * bz;
                    float nz = ax * by - ay * bx;

                    float lenN = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                    if (lenN > 0.0001f) {
                        nx /= lenN; ny /= lenN; nz /= lenN;
                    }

                    // Dot product for diffuse lighting
                    float dotProduct = nx * lx + ny * ly + nz * lz;
                    float intensity = 0.35f + 0.65f * Math.max(0.0f, dotProduct);

                    // Material Color Shading
                    int rBase = Color.red(selectedMaterialColor);
                    int gBase = Color.green(selectedMaterialColor);
                    int bBase = Color.blue(selectedMaterialColor);

                    int rShaded = Math.min(255, (int) (rBase * intensity));
                    int gShaded = Math.min(255, (int) (gBase * intensity));
                    int bShaded = Math.min(255, (int) (bBase * intensity));

                    face.normal[0] = nx;
                    face.normal[1] = ny;
                    face.normal[2] = nz;

                    fillPaint.setColor(Color.rgb(rShaded, gShaded, bShaded));
                }

                // Render styles processing
                if (currentRenderMode == RenderMode.SOLID) {
                    canvas.drawPath(path, fillPaint);
                    
                    // Draw outer border outline for high contrast
                    strokePaint.setStrokeWidth(strokeThickness / 2.0f);
                    canvas.drawPath(path, strokePaint);
                } else if (currentRenderMode == RenderMode.WIREFRAME) {
                    strokePaint.setStrokeWidth(strokeThickness);
                    strokePaint.setColor(selectedMaterialColor);
                    canvas.drawPath(path, strokePaint);
                }
            }

            // Draw extra points for Vertices/Point Cloud mode
            if (currentRenderMode == RenderMode.POINTS) {
                vertexPaint.setColor(selectedMaterialColor);
                for (Vertex r : rotated) {
                    float divisor = cameraDistance - r.z;
                    if (divisor < 0.2f) divisor = 0.2f;

                    float px = (r.x / divisor) * scaleFactor + (width / 2f);
                    float py = (-r.y / divisor) * scaleFactor + (height / 2f);

                    // Glowing vertex points
                    canvas.drawCircle(px, py, strokeThickness + 4f, vertexPaint);
                }
            }
        }
    }
}