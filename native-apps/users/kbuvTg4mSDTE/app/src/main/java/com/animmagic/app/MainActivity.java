package com.animmagic.app;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.BounceInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends Activity {

    // Tab buttons
    private Button tabPreset;
    private Button tabFlipbook;
    private Button tabParticle;

    // View panels
    private ScrollView panelPreset;
    private LinearLayout panelFlipbook;
    private LinearLayout panelParticle;

    // PRESET TWEEN VIEW Widgets
    private View animPreviewBox;
    private CheckBox cbRotate, cbScale, cbTranslate, cbFade;
    private RadioGroup rgInterpolators;
    private RadioButton rbLinear, rbBounce, rbAccelerate, rbOvershoot;
    private SeekBar sbDuration;
    private TextView tvDurationLabel;
    private Button btnTriggerAnim;

    // FLIPBOOK MODULE VIEW Widgets
    private FrameLayout flipbookCanvasContainer;
    private FlipbookCanvas flipbookCanvas;
    private TextView tvFrameStatus;
    private CheckBox cbOnionSkin;
    private Button btnAddFrame;
    private Button btnClearFrame;
    private Button btnResetFlipbook;
    private Button btnPlayFlipbook;
    private SeekBar sbFps;
    private TextView tvFpsLabel;

    // FLIPBOOK Vector Frame Architecture
    private final List<List<Stroke>> framesList = new ArrayList<List<Stroke>>();
    private List<Stroke> currentFrameStrokes = new ArrayList<Stroke>();
    private boolean isPlayingFlipbook = false;
    private int playFrameIndex = 0;
    private final Handler playbackHandler = new Handler();
    private Runnable playbackRunnable;
    private int fpsRate = 6;

    // PHYSICS PARTICLE MODULE Widgets
    private FrameLayout particleCanvasContainer;
    private ParticleCanvas particleCanvas;
    private SeekBar sbGravity;
    private TextView tvGravityLabel;
    private Button btnClearParticles;

    // Physics Particle variables
    private float gravityAcceleration = 0.5f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeTabs();
        initializePresetModule();
        initializeFlipbookModule();
        initializeParticleModule();

        // Load Default Theme/Skins to preview items dynamically
        applyDynamicBoxStyling();
    }

    private void applyDynamicBoxStyling() {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(24f);
        shape.setColor(0xFFFF5722); // Orange Accent
        shape.setStroke(6, 0xFFE64A19);
        animPreviewBox.setBackground(shape);
    }

    private void initializeTabs() {
        tabPreset = (Button) findViewById(R.id.tab_preset);
        tabFlipbook = (Button) findViewById(R.id.tab_flipbook);
        tabParticle = (Button) findViewById(R.id.tab_particle);

        panelPreset = (ScrollView) findViewById(R.id.panel_preset);
        panelFlipbook = (LinearLayout) findViewById(R.id.panel_flipbook);
        panelParticle = (LinearLayout) findViewById(R.id.panel_particle);

        tabPreset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(0);
            }
        });

        tabFlipbook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(1);
            }
        });

        tabParticle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(2);
            }
        });
    }

    private void switchTab(int index) {
        // Reset tab styles
        tabPreset.setTextColor(0xFF757575);
        tabPreset.setTypeface(null, android.graphics.Typeface.NORMAL);
        tabFlipbook.setTextColor(0xFF757575);
        tabFlipbook.setTypeface(null, android.graphics.Typeface.NORMAL);
        tabParticle.setTextColor(0xFF757575);
        tabParticle.setTypeface(null, android.graphics.Typeface.NORMAL);

        panelPreset.setVisibility(View.GONE);
        panelFlipbook.setVisibility(View.GONE);
        panelParticle.setVisibility(View.GONE);

        // Pause flipbook loop on switch
        if (isPlayingFlipbook) {
            stopFlipbookMovie();
        }

        // Pause Physics Engine loop on switch
        if (particleCanvas != null) {
            particleCanvas.setPhysicsEngineRunning(false);
        }

        if (index == 0) {
            tabPreset.setTextColor(0xFF6200EE);
            tabPreset.setTypeface(null, android.graphics.Typeface.BOLD);
            panelPreset.setVisibility(View.VISIBLE);
        } else if (index == 1) {
            tabFlipbook.setTextColor(0xFF6200EE);
            tabFlipbook.setTypeface(null, android.graphics.Typeface.BOLD);
            panelFlipbook.setVisibility(View.VISIBLE);
        } else if (index == 2) {
            tabParticle.setTextColor(0xFF6200EE);
            tabParticle.setTypeface(null, android.graphics.Typeface.BOLD);
            panelParticle.setVisibility(View.VISIBLE);
            if (particleCanvas != null) {
                particleCanvas.setPhysicsEngineRunning(true);
            }
        }
    }

    private void initializePresetModule() {
        animPreviewBox = findViewById(R.id.anim_preview_box);
        cbRotate = (CheckBox) findViewById(R.id.cb_rotate);
        cbScale = (CheckBox) findViewById(R.id.cb_scale);
        cbTranslate = (CheckBox) findViewById(R.id.cb_translate);
        cbFade = (CheckBox) findViewById(R.id.cb_fade);

        rgInterpolators = (RadioGroup) findViewById(R.id.rg_interpolators);
        rbLinear = (RadioButton) findViewById(R.id.rb_linear);
        rbBounce = (RadioButton) findViewById(R.id.rb_bounce);
        rbAccelerate = (RadioButton) findViewById(R.id.rb_accelerate);
        rbOvershoot = (RadioButton) findViewById(R.id.rb_overshoot);

        sbDuration = (SeekBar) findViewById(R.id.sb_duration);
        tvDurationLabel = (TextView) findViewById(R.id.tv_duration_label);
        btnTriggerAnim = (Button) findViewById(R.id.btn_trigger_anim);

        sbDuration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 100) {
                    progress = 100;
                    sbDuration.setProgress(progress);
                }
                tvDurationLabel.setText("Duration: " + progress + " ms");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnTriggerAnim.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runSelectedTweenPreset();
            }
        });
    }

    private void runSelectedTweenPreset() {
        long duration = sbDuration.getProgress();

        AnimationSet set = new AnimationSet(false);

        if (cbRotate.isChecked()) {
            RotateAnimation rot = new RotateAnimation(0f, 360f,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f);
            rot.setDuration(duration);
            set.addAnimation(rot);
        }

        if (cbScale.isChecked()) {
            ScaleAnimation scale = new ScaleAnimation(1.0f, 1.6f, 1.0f, 1.6f,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f);
            scale.setDuration(duration);
            scale.setRepeatCount(1);
            scale.setRepeatMode(Animation.REVERSE);
            set.addAnimation(scale);
        }

        if (cbTranslate.isChecked()) {
            TranslateAnimation trans = new TranslateAnimation(
                    Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f,
                    Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.25f);
            trans.setDuration(duration);
            trans.setRepeatCount(1);
            trans.setRepeatMode(Animation.REVERSE);
            set.addAnimation(trans);
        }

        if (cbFade.isChecked()) {
            AlphaAnimation fade = new AlphaAnimation(1.0f, 0.1f);
            fade.setDuration(duration);
            fade.setRepeatCount(1);
            fade.setRepeatMode(Animation.REVERSE);
            set.addAnimation(fade);
        }

        // Apply selected interpolators manually
        int selectedId = rgInterpolators.getCheckedRadioButtonId();
        if (selectedId == R.id.rb_linear) {
            set.setInterpolator(new LinearInterpolator());
        } else if (selectedId == R.id.rb_bounce) {
            set.setInterpolator(new BounceInterpolator());
        } else if (selectedId == R.id.rb_accelerate) {
            set.setInterpolator(new AccelerateInterpolator());
        } else if (selectedId == R.id.rb_overshoot) {
            set.setInterpolator(new OvershootInterpolator());
        }

        if (!cbRotate.isChecked() && !cbScale.isChecked() && !cbTranslate.isChecked() && !cbFade.isChecked()) {
            Toast.makeText(MainActivity.this, "Please check at least one animation action!", Toast.LENGTH_SHORT).show();
            return;
        }

        animPreviewBox.startAnimation(set);
    }

    private void initializeFlipbookModule() {
        flipbookCanvasContainer = (FrameLayout) findViewById(R.id.flipbook_canvas_container);
        tvFrameStatus = (TextView) findViewById(R.id.tv_frame_status);
        cbOnionSkin = (CheckBox) findViewById(R.id.cb_onion_skin);
        btnAddFrame = (Button) findViewById(R.id.btn_add_frame);
        btnClearFrame = (Button) findViewById(R.id.btn_clear_frame);
        btnResetFlipbook = (Button) findViewById(R.id.btn_reset_flipbook);
        btnPlayFlipbook = (Button) findViewById(R.id.btn_play_flipbook);
        sbFps = (SeekBar) findViewById(R.id.sb_fps);
        tvFpsLabel = (TextView) findViewById(R.id.tv_fps_label);

        // Dynamically insert high performance Flipbook canvas
        flipbookCanvas = new FlipbookCanvas(this);
        flipbookCanvasContainer.addView(flipbookCanvas);

        cbOnionSkin.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                flipbookCanvas.invalidate();
            }
        });

        btnAddFrame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveCurrentCanvasFrame();
            }
        });

        btnClearFrame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentFrameStrokes.clear();
                flipbookCanvas.invalidate();
                Toast.makeText(MainActivity.this, "Canvas Cleared", Toast.LENGTH_SHORT).show();
            }
        });

        btnResetFlipbook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetEntireFlipbook();
            }
        });

        btnPlayFlipbook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPlayingFlipbook) {
                    stopFlipbookMovie();
                } else {
                    startFlipbookMovie();
                }
            }
        });

        sbFps.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 1) {
                    progress = 1;
                    sbFps.setProgress(1);
                }
                fpsRate = progress;
                tvFpsLabel.setText("Playback FPS: " + fpsRate);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Initialize dynamic animation frames engine
        playbackRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isPlayingFlipbook || framesList.isEmpty()) {
                    stopFlipbookMovie();
                    return;
                }

                playFrameIndex++;
                if (playFrameIndex >= framesList.size()) {
                    playFrameIndex = 0;
                }

                flipbookCanvas.invalidate();
                long nextDelay = 1000 / fpsRate;
                playbackHandler.postDelayed(this, nextDelay);
            }
        };
    }

    private void saveCurrentCanvasFrame() {
        if (currentFrameStrokes.isEmpty()) {
            Toast.makeText(MainActivity.this, "Cannot capture an empty canvas!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Clone current strokes programmatically
        List<Stroke> frameCopy = new ArrayList<Stroke>();
        for (Stroke stroke : currentFrameStrokes) {
            frameCopy.add(new Stroke(new Path(stroke.path), stroke.color, stroke.strokeWidth));
        }

        framesList.add(frameCopy);
        currentFrameStrokes = new ArrayList<Stroke>(); // clear vector reference for next frame
        flipbookCanvas.invalidate();

        updateFrameCounterView();
        Toast.makeText(MainActivity.this, "Frame Captured & Stored!", Toast.LENGTH_SHORT).show();
    }

    private void updateFrameCounterView() {
        tvFrameStatus.setText("Frames Captured: " + framesList.size());
    }

    private void resetEntireFlipbook() {
        stopFlipbookMovie();
        framesList.clear();
        currentFrameStrokes.clear();
        playFrameIndex = 0;
        updateFrameCounterView();
        flipbookCanvas.invalidate();
        Toast.makeText(MainActivity.this, "Flipbook Reset Successful", Toast.LENGTH_SHORT).show();
    }

    private void startFlipbookMovie() {
        if (framesList.isEmpty() && currentFrameStrokes.isEmpty()) {
            Toast.makeText(MainActivity.this, "Draw frames first to build a movie!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Auto backup current drawing to frames list if present
        if (!currentFrameStrokes.isEmpty()) {
            saveCurrentCanvasFrame();
        }

        isPlayingFlipbook = true;
        playFrameIndex = 0;
        btnPlayFlipbook.setText("Stop Movie");
        btnPlayFlipbook.setBackgroundColor(0xFFCF6679);

        // Lock draw triggers on Play
        btnAddFrame.setEnabled(false);
        btnClearFrame.setEnabled(false);

        playbackHandler.post(playbackRunnable);
    }

    private void stopFlipbookMovie() {
        isPlayingFlipbook = false;
        btnPlayFlipbook.setText("Play Movie");
        btnPlayFlipbook.setBackgroundColor(0xFF6200EE);

        btnAddFrame.setEnabled(true);
        btnClearFrame.setEnabled(true);

        playbackHandler.removeCallbacks(playbackRunnable);
        flipbookCanvas.invalidate();
    }

    private void initializeParticleModule() {
        particleCanvasContainer = (FrameLayout) findViewById(R.id.particle_canvas_container);
        sbGravity = (SeekBar) findViewById(R.id.sb_gravity);
        tvGravityLabel = (TextView) findViewById(R.id.tv_gravity_label);
        btnClearParticles = (Button) findViewById(R.id.btn_clear_particles);

        // Dynamic insertion of our high-speed particle sandbox engine
        particleCanvas = new ParticleCanvas(this);
        particleCanvasContainer.addView(particleCanvas);

        sbGravity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                gravityAcceleration = progress / 10.0f;
                tvGravityLabel.setText("Gravity Acceleration: " + gravityAcceleration);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnClearParticles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (particleCanvas != null) {
                    particleCanvas.clearParticles();
                }
            }
        });
    }

    // Static Vector Stroke representation (Clean internal storage structure)
    static class Stroke {
        Path path;
        int color;
        float strokeWidth;

        public Stroke(Path path, int color, float strokeWidth) {
            this.path = path;
            this.color = color;
            this.strokeWidth = strokeWidth;
        }
    }

    // High performance Vector Drawing canvas component
    private class FlipbookCanvas extends View {
        private final Paint drawPaint = new Paint();
        private final Paint onionPaint = new Paint();
        private Path activeDrawingPath;
        private final int paintColor = 0xFF212121;
        private final float brushWidth = 10f;

        public FlipbookCanvas(Activity context) {
            super(context);
            setupDrawingEngine();
        }

        private void setupDrawingEngine() {
            drawPaint.setColor(paintColor);
            drawPaint.setAntiAlias(true);
            drawPaint.setStrokeWidth(brushWidth);
            drawPaint.setStyle(Paint.Style.STROKE);
            drawPaint.setStrokeJoin(Paint.Join.ROUND);
            drawPaint.setStrokeCap(Paint.Cap.ROUND);

            onionPaint.setColor(0xFF9E9E9E);
            onionPaint.setAlpha(60); // 25% opacity onion layer mapping
            onionPaint.setAntiAlias(true);
            onionPaint.setStrokeWidth(brushWidth);
            onionPaint.setStyle(Paint.Style.STROKE);
            onionPaint.setStrokeJoin(Paint.Join.ROUND);
            onionPaint.setStrokeCap(Paint.Cap.ROUND);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawColor(Color.WHITE);

            if (isPlayingFlipbook) {
                // Movie Playback State
                if (playFrameIndex < framesList.size()) {
                    List<Stroke> frame = framesList.get(playFrameIndex);
                    for (Stroke stroke : frame) {
                        drawPaint.setColor(stroke.color);
                        drawPaint.setStrokeWidth(stroke.strokeWidth);
                        canvas.drawPath(stroke.path, drawPaint);
                    }
                }
            } else {
                // Editing State Onion Skin drawing layer
                if (cbOnionSkin.isChecked() && !framesList.isEmpty()) {
                    List<Stroke> previousFrame = framesList.get(framesList.size() - 1);
                    for (Stroke stroke : previousFrame) {
                        canvas.drawPath(stroke.path, onionPaint);
                    }
                }

                // Render current active layout frames
                for (Stroke stroke : currentFrameStrokes) {
                    drawPaint.setColor(stroke.color);
                    drawPaint.setStrokeWidth(stroke.strokeWidth);
                    canvas.drawPath(stroke.path, drawPaint);
                }
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (isPlayingFlipbook) {
                return false; // ignore touch on active playback
            }

            float x = event.getX();
            float y = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    activeDrawingPath = new Path();
                    activeDrawingPath.moveTo(x, y);
                    currentFrameStrokes.add(new Stroke(activeDrawingPath, paintColor, brushWidth));
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (activeDrawingPath != null) {
                        activeDrawingPath.lineTo(x, y);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (activeDrawingPath != null) {
                        activeDrawingPath.lineTo(x, y);
                    }
                    activeDrawingPath = null;
                    break;
                default:
                    return false;
            }

            invalidate();
            return true;
        }
    }

    // Interactive custom Gravity-Particle Physics simulation engine
    private class ParticleCanvas extends View {
        private final List<Particle> particles = new ArrayList<Particle>();
        private final Paint particlePaint = new Paint();
        private final Random random = new Random();
        private final Handler loopHandler = new Handler();
        private Runnable engineLoop;
        private boolean isRunning = false;

        public ParticleCanvas(Activity context) {
            super(context);
            setupPaint();
            startEngineLoop();
        }

        private void setupPaint() {
            particlePaint.setAntiAlias(true);
            particlePaint.setStyle(Paint.Style.FILL);
        }

        public void setPhysicsEngineRunning(boolean run) {
            this.isRunning = run;
            if (run) {
                loopHandler.post(engineLoop);
            } else {
                loopHandler.removeCallbacks(engineLoop);
            }
        }

        private void startEngineLoop() {
            engineLoop = new Runnable() {
                @Override
                public void run() {
                    if (!isRunning) return;

                    updatePhysics();
                    invalidate();

                    // Lock update cycle to ~60fps limits
                    loopHandler.postDelayed(this, 16);
                }
            };
        }

        private void updatePhysics() {
            int width = getWidth();
            int height = getHeight();

            for (int i = particles.size() - 1; i >= 0; i--) {
                Particle p = particles.get(i);
                p.vy += gravityAcceleration; // Gravity constant
                p.x += p.vx;
                p.y += p.vy;

                // Collision detection with screen boundaries
                if (p.x < p.radius) {
                    p.x = p.radius;
                    p.vx = -p.vx * 0.75f; // Dampened bouncing elasticity
                } else if (p.x > width - p.radius) {
                    p.x = width - p.radius;
                    p.vx = -p.vx * 0.75f;
                }

                if (p.y > height - p.radius) {
                    p.y = height - p.radius;
                    p.vy = -p.vy * 0.65f; // Bounce bounce decay
                    p.vx *= 0.9f; // horizontal friction decay
                }

                p.age += 1;
                if (p.age >= p.maxAge) {
                    particles.remove(i);
                }
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            // Draw clean background grid
            canvas.drawColor(0xFF1E1E1E);

            // Draw active particles
            for (Particle p : particles) {
                particlePaint.setColor(p.color);
                particlePaint.setAlpha((int) ((1.0f - ((float) p.age / p.maxAge)) * 255));
                canvas.drawCircle(p.x, p.y, p.radius, particlePaint);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                // Generate colorful physical objects at touch point coordinates
                for (int i = 0; i < 5; i++) {
                    float vx = (random.nextFloat() * 14f) - 7f;
                    float vy = (random.nextFloat() * -12f) - 4f;
                    float radius = (random.nextFloat() * 15f) + 12f;
                    int maxAge = (random.nextInt(60)) + 70;

                    // Random Neon Palette Selector
                    int[] colors = {0xFF03DAC5, 0xFFFF0266, 0xFF00E5FF, 0xFFCCFF90, 0xFFFFEA00};
                    int color = colors[random.nextInt(colors.length)];

                    particles.add(new Particle(event.getX(), event.getY(), vx, vy, radius, color, maxAge));
                }
                invalidate();
                return true;
            }
            return super.onTouchEvent(event);
        }

        public void clearParticles() {
            particles.clear();
            invalidate();
        }
    }

    // Physical particle blueprint model
    static class Particle {
        float x, y;
        float vx, vy;
        float radius;
        int color;
        int age;
        int maxAge;

        public Particle(float x, float y, float vx, float vy, float radius, int color, int maxAge) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.radius = radius;
            this.color = color;
            this.age = 0;
            this.maxAge = maxAge;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopFlipbookMovie();
        if (particleCanvas != null) {
            particleCanvas.setPhysicsEngineRunning(false);
        }
    }
}