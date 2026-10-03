package com.animationsandbox.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;

public class MainActivity extends Activity {

    // Main control tab buttons
    private Button btnTabProperty;
    private Button btnTabInterpolator;
    private Button btnTabPhysics;
    private Button btnTabParticle;

    // View Panel panels mapping each sandbox features
    private ScrollView panelProperty;
    private LinearLayout panelInterpolator;
    private LinearLayout panelPhysics;
    private LinearLayout panelParticle;

    // Property animator testbed elements
    private View animatedTarget;
    private TextView lblDuration;
    private TextView lblRotation;
    private TextView lblScale;
    private TextView lblTranslation;
    private SeekBar seekDuration;
    private SeekBar seekRotation;
    private SeekBar seekScale;
    private SeekBar seekTranslation;
    private Spinner spinnerPropertyInterpolator;
    private Button btnPlayProperty;
    private Button btnResetProperty;

    // Custom Interpolator visualization tab elements
    private Spinner spinnerCurveInterpolator;
    private InterpolatorGraphView interpolatorGraphView;
    private View testAnimatedDot;
    private Button btnTriggerTestMotion;

    // Custom interactive Physics canvas sandbox
    private PhysicsSandboxView physicsSandboxView;
    private TextView lblPhysicsGravity;
    private TextView lblPhysicsBounciness;
    private SeekBar seekPhysicsGravity;
    private SeekBar seekPhysicsBounciness;
    private Button btnClearPhysics;

    // Custom dynamic system particle sandbox elements
    private ParticleExplosionView particleExplosionView;
    private TextView lblParticleSpeed;
    private TextView lblParticleCount;
    private TextView lblParticleLifespan;
    private SeekBar seekParticleSpeed;
    private SeekBar seekParticleCount;
    private SeekBar seekParticleLifespan;
    private Button btnClearParticles;

    private static final String[] INTERPOLATORS_NAME_PRESETS = {
        "Linear",
        "Accelerate",
        "Decelerate",
        "Accelerate Decelerate",
        "Bounce",
        "Overshoot",
        "Anticipate",
        "Anticipate Overshoot"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Map tab components
        btnTabProperty = (Button) findViewById(R.id.btn_tab_property);
        btnTabInterpolator = (Button) findViewById(R.id.btn_tab_interpolator);
        btnTabPhysics = (Button) findViewById(R.id.btn_tab_physics);
        btnTabParticle = (Button) findViewById(R.id.btn_tab_particle);

        panelProperty = (ScrollView) findViewById(R.id.panel_property);
        panelInterpolator = (LinearLayout) findViewById(R.id.panel_interpolator);
        panelPhysics = (LinearLayout) findViewById(R.id.panel_physics);
        panelParticle = (LinearLayout) findViewById(R.id.panel_particle);

        // Map custom property configurations elements
        animatedTarget = findViewById(R.id.animated_target);
        lblDuration = (TextView) findViewById(R.id.lbl_duration);
        lblRotation = (TextView) findViewById(R.id.lbl_rotation);
        lblScale = (TextView) findViewById(R.id.lbl_scale);
        lblTranslation = (TextView) findViewById(R.id.lbl_translation);
        seekDuration = (SeekBar) findViewById(R.id.seek_duration);
        seekRotation = (SeekBar) findViewById(R.id.seek_rotation);
        seekScale = (SeekBar) findViewById(R.id.seek_scale);
        seekTranslation = (SeekBar) findViewById(R.id.seek_translation);
        spinnerPropertyInterpolator = (Spinner) findViewById(R.id.spinner_property_interpolator);
        btnPlayProperty = (Button) findViewById(R.id.btn_play_property);
        btnResetProperty = (Button) findViewById(R.id.btn_reset_property);

        // Map interpolator items
        spinnerCurveInterpolator = (Spinner) findViewById(R.id.spinner_curve_interpolator);
        interpolatorGraphView = (InterpolatorGraphView) findViewById(R.id.interpolator_graph_view);
        testAnimatedDot = findViewById(R.id.test_animated_dot);
        btnTriggerTestMotion = (Button) findViewById(R.id.btn_trigger_test_motion);

        // Map simulator physics panel items
        physicsSandboxView = (PhysicsSandboxView) findViewById(R.id.physics_sandbox_view);
        lblPhysicsGravity = (TextView) findViewById(R.id.lbl_physics_gravity);
        lblPhysicsBounciness = (TextView) findViewById(R.id.lbl_physics_bounciness);
        seekPhysicsGravity = (SeekBar) findViewById(R.id.seek_physics_gravity);
        seekPhysicsBounciness = (SeekBar) findViewById(R.id.seek_physics_bounciness);
        btnClearPhysics = (Button) findViewById(R.id.btn_clear_physics);

        // Map particle sandbox options elements
        particleExplosionView = (ParticleExplosionView) findViewById(R.id.particle_explosion_view);
        lblParticleSpeed = (TextView) findViewById(R.id.lbl_particle_speed);
        lblParticleCount = (TextView) findViewById(R.id.lbl_particle_count);
        lblParticleLifespan = (TextView) findViewById(R.id.lbl_particle_lifespan);
        seekParticleSpeed = (SeekBar) findViewById(R.id.seek_particle_speed);
        seekParticleCount = (SeekBar) findViewById(R.id.seek_particle_count);
        seekParticleLifespan = (SeekBar) findViewById(R.id.seek_particle_lifespan);
        btnClearParticles = (Button) findViewById(R.id.btn_clear_particles);

        // Bind standard selector adapter presets
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, INTERPOLATORS_NAME_PRESETS);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPropertyInterpolator.setAdapter(spinnerAdapter);
        spinnerCurveInterpolator.setAdapter(spinnerAdapter);

        setupControllerListeners();
        selectTabSection(0);
    }

    private void setupControllerListeners() {
        // Tab routing event handlers
        btnTabProperty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTabSection(0);
            }
        });
        btnTabInterpolator.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTabSection(1);
            }
        });
        btnTabPhysics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTabSection(2);
            }
        });
        btnTabParticle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTabSection(3);
            }
        });

        // Property animators seek configuration listeners
        seekDuration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                lblDuration.setText("Duration: " + progress + " ms");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekRotation.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                lblRotation.setText("Rotation Angle: " + progress + "°");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekScale.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float value = progress / 100f;
                lblScale.setText("Scale Factor: " + String.format("%.2f", value) + "x");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekTranslation.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = progress - 200; // supports negative values offset
                lblTranslation.setText("X-Translation: " + value + " px");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnPlayProperty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playInteractivePropertyAnimation();
            }
        });

        btnResetProperty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetPropertyAnimatorTarget();
            }
        });

        // Curve selected spinner item handler
        spinnerCurveInterpolator.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Interpolator ip = fetchInterpolatorByIndex(position);
                interpolatorGraphView.setInterpolator(ip, INTERPOLATORS_NAME_PRESETS[position]);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnTriggerTestMotion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerCurveProgressTest();
            }
        });

        // Custom gravity simulation controls listeners
        seekPhysicsGravity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float g = progress / 10f;
                lblPhysicsGravity.setText("Gravity Strength: " + String.format("%.1f", g));
                physicsSandboxView.setGravity(g);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekPhysicsBounciness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float b = progress / 100f;
                lblPhysicsBounciness.setText("Bounciness Elasticity: " + String.format("%.2f", b));
                physicsSandboxView.setBounciness(b);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnClearPhysics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                physicsSandboxView.clearBalls();
            }
        });

        // Particle configuration adjustment controls listeners
        seekParticleSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float speed = progress / 10f;
                lblParticleSpeed.setText("Velocity Speed Scale: " + String.format("%.1f", speed) + "x");
                particleExplosionView.setParticleSpeed(speed);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekParticleCount.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                lblParticleCount.setText("Particle Burst Density: " + progress);
                particleExplosionView.setParticleCount(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekParticleLifespan.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float life = progress / 10f;
                lblParticleLifespan.setText("Lifespan Duration: " + String.format("%.1f", life) + "x");
                particleExplosionView.setParticleLifespan(life);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnClearParticles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                particleExplosionView.clearParticles();
            }
        });
    }

    private void selectTabSection(int index) {
        // Switch tab colors
        btnTabProperty.setBackgroundResource(index == 0 ? R.drawable.tab_button_active : R.drawable.tab_button_inactive);
        btnTabProperty.setTextColor(index == 0 ? Color.WHITE : Color.parseColor("#2C3E50"));

        btnTabInterpolator.setBackgroundResource(index == 1 ? R.drawable.tab_button_active : R.drawable.tab_button_inactive);
        btnTabInterpolator.setTextColor(index == 1 ? Color.WHITE : Color.parseColor("#2C3E50"));

        btnTabPhysics.setBackgroundResource(index == 2 ? R.drawable.tab_button_active : R.drawable.tab_button_inactive);
        btnTabPhysics.setTextColor(index == 2 ? Color.WHITE : Color.parseColor("#2C3E50"));

        btnTabParticle.setBackgroundResource(index == 3 ? R.drawable.tab_button_active : R.drawable.tab_button_inactive);
        btnTabParticle.setTextColor(index == 3 ? Color.WHITE : Color.parseColor("#2C3E50"));

        // Toggle layouts
        panelProperty.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        panelInterpolator.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        panelPhysics.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        panelParticle.setVisibility(index == 3 ? View.VISIBLE : View.GONE);
    }

    private void playInteractivePropertyAnimation() {
        long duration = seekDuration.getProgress();
        float rotationDegree = seekRotation.getProgress();
        float scaleFactor = seekScale.getProgress() / 100f;
        float transXDelta = seekTranslation.getProgress() - 200;

        Interpolator ip = fetchInterpolatorByIndex(spinnerPropertyInterpolator.getSelectedItemPosition());

        ObjectAnimator animRotation = ObjectAnimator.ofFloat(animatedTarget, "rotation", 0f, rotationDegree);
        ObjectAnimator animScaleX = ObjectAnimator.ofFloat(animatedTarget, "scaleX", 1.0f, scaleFactor);
        ObjectAnimator animScaleY = ObjectAnimator.ofFloat(animatedTarget, "scaleY", 1.0f, scaleFactor);
        ObjectAnimator animTransX = ObjectAnimator.ofFloat(animatedTarget, "translationX", 0f, transXDelta);

        AnimatorSet propertySet = new AnimatorSet();
        propertySet.playTogether(animRotation, animScaleX, animScaleY, animTransX);
        propertySet.setDuration(duration);
        propertySet.setInterpolator(ip);
        propertySet.start();
    }

    private void resetPropertyAnimatorTarget() {
        animatedTarget.animate().cancel();
        animatedTarget.setRotation(0f);
        animatedTarget.setScaleX(1.0f);
        animatedTarget.setScaleY(1.0f);
        animatedTarget.setTranslationX(0f);
    }

    private void triggerCurveProgressTest() {
        btnTriggerTestMotion.setEnabled(false);
        testAnimatedDot.setTranslationX(0f);

        int pos = spinnerCurveInterpolator.getSelectedItemPosition();
        Interpolator testInterp = fetchInterpolatorByIndex(pos);

        View frameParent = (View) testAnimatedDot.getParent();
        final float slideDistanceLimit = frameParent.getWidth() - testAnimatedDot.getWidth() - 12f;

        // Linear animator used specifically to update timeline graphing marker linearly
        ValueAnimator linearTimelineAnimator = ValueAnimator.ofFloat(0f, 1.0f);
        linearTimelineAnimator.setDuration(2500);
        linearTimelineAnimator.setInterpolator(new LinearInterpolator());
        linearTimelineAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float progressVal = (Float) animation.getAnimatedValue();
                interpolatorGraphView.setProgress(progressVal);
            }
        });

        // Object animator sweeping testing sphere across linear layout layout track
        ObjectAnimator sphereAnim = ObjectAnimator.ofFloat(testAnimatedDot, "translationX", 0f, slideDistanceLimit);
        sphereAnim.setDuration(2500);
        sphereAnim.setInterpolator(testInterp);

        AnimatorSet animatorCombo = new AnimatorSet();
        animatorCombo.playTogether(linearTimelineAnimator, sphereAnim);
        animatorCombo.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {}
            @Override
            public void onAnimationEnd(Animator animation) {
                btnTriggerTestMotion.setEnabled(true);
            }
            @Override
            public void onAnimationCancel(Animator animation) {
                btnTriggerTestMotion.setEnabled(true);
            }
            @Override
            public void onAnimationRepeat(Animator animation) {}
        });
        animatorCombo.start();
    }

    private Interpolator fetchInterpolatorByIndex(int index) {
        switch (index) {
            case 1: return new android.view.animation.AccelerateInterpolator();
            case 2: return new android.view.animation.DecelerateInterpolator();
            case 3: return new android.view.animation.AccelerateDecelerateInterpolator();
            case 4: return new android.view.animation.BounceInterpolator();
            case 5: return new android.view.animation.OvershootInterpolator();
            case 6: return new android.view.animation.AnticipateInterpolator();
            case 7: return new android.view.animation.AnticipateOvershootInterpolator();
            case 0:
            default:
                return new android.view.animation.LinearInterpolator();
        }
    }
}