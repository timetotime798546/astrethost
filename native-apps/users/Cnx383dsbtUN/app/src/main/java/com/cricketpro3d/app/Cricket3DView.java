package com.cricketpro3d.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;

import java.util.Random;

public class Cricket3DView extends View {

    public interface CricketActionListener {
        void onBallCompleted(int runsScored, boolean isWicket, String dismissalType, String shotQuality, int distanceMeters);
        void onTimingFeedback(String timingText, int color);
        void onShotTriggered(int direction, boolean lofted);
    }

    private CricketActionListener listener;
    private SoundManager soundManager;

    // Rendering Paints
    private final Paint skyPaint = new Paint();
    private final Paint crowdPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stadiumRoofsPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint turfPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint turfRingsPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint boundaryRopePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pitchPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pitchCreasePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pitchLengthZonePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stumpPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ballPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ballHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ballSeamPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ballShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ballTrailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint batsmanPadsPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint batsmanJerseyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint batsmanHelmetPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint batWillowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint batGripPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bowlerJerseyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint floodlightTowerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint floodlightGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint swipeTrailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint swipeGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Ball 3D Coordinates:
    // X (-0.5 to 0.5 across pitch), Y (height above ground), Z (1.0 at bowler, 0.0 at batting crease)
    private float ballX = 0f;
    private float ballY = 1.35f;
    private float ballZ = 1.0f;

    // Motion parameters
    private float ballSpeedZ = 0.019f;
    private float ballSpeedY = -0.024f;
    private float ballSpeedX = 0.001f;
    private float pitchSpotZ = 0.28f;
    private float pitchSpotX = 0.02f;
    private boolean hasBounced = false;

    // Delivery attributes
    private String deliveryType = "Fast Seamer";

    // Batsman Setup & Direction Controls
    // Direction: 0 = Leg Side (< LEG), 1 = Straight, 2 = Off Side (OFF >)
    private int shotDirection = 1;
    private boolean isLofted = false;

    // Bat Swing Animation
    private boolean isSwinging = false;
    private float batAngleDeg = 0f;
    private float batSweepX = 0f;
    private long swingStartTimestamp = 0;
    private static final long SWING_DURATION_MS = 380;

    // Gesture & Touch Screen Swipe Fields
    private float touchDownX = 0f;
    private float touchDownY = 0f;
    private long touchDownTime = 0;
    private float currentTouchX = 0f;
    private float currentTouchY = 0f;
    private boolean isTrackingSwipe = false;
    private VelocityTracker velocityTracker;
    private float gesturePowerMultiplier = 1.0f;

    // Visual Swipe Trail Effect
    private float swipeStartX = -1f;
    private float swipeStartY = -1f;
    private float swipeEndX = -1f;
    private float swipeEndY = -1f;
    private float swipeAlpha = 0f;

    // Machine States
    public static final int STATE_IDLE = 0;
    public static final int STATE_BOWLER_RUNUP = 1;
    public static final int STATE_BALL_IN_AIR = 2;
    public static final int STATE_SHOT_HIT = 3;
    public static final int STATE_WICKET_FALLEN = 4;
    private int currentState = STATE_IDLE;

    // Hit Physics & Projectile Flight
    private float hitBallX = 0f;
    private float hitBallY = 0f;
    private float hitBallZ = 0f;
    private float hitVelX = 0f;
    private float hitVelY = 0f;
    private float hitVelZ = 0f;
    private float bounceRestitution = 0.55f;
    private int computedDistanceMeters = 0;

    // Bowled Stumps Animation
    private float bailOffY1 = 0f;
    private float bailOffY2 = 0f;
    private float bailVelY = 0f;
    private float bailSpin = 0f;

    // Bowler Run-up
    private float bowlerRunProgress = 0f;

    // Motion history for realistic ball trail
    private static final int TRAIL_POINTS = 5;
    private final float[] trailX = new float[TRAIL_POINTS];
    private final float[] trailY = new float[TRAIL_POINTS];
    private final float[] trailZ = new float[TRAIL_POINTS];
    private int trailHead = 0;

    private final Random random = new Random();
    private final Handler physicsLoopHandler = new Handler(Looper.getMainLooper());
    private final Runnable physicsLoopRunnable = new Runnable() {
        @Override
        public void run() {
            updatePhysics();
            invalidate();
            physicsLoopHandler.postDelayed(this, 16);
        }
    };

    public Cricket3DView(Context context) {
        super(context);
        init(context);
    }

    public Cricket3DView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        soundManager = SoundManager.getInstance(context);

        // Turf & Pitch Colors
        turfPaint.setColor(Color.parseColor("#154F20"));
        turfRingsPaint.setColor(Color.argb(30, 255, 255, 255));
        turfRingsPaint.setStrokeWidth(5f);

        pitchPaint.setColor(Color.parseColor("#E4D4B5"));
        pitchCreasePaint.setColor(Color.WHITE);
        pitchCreasePaint.setStrokeWidth(3.5f);

        pitchLengthZonePaint.setColor(Color.argb(35, 255, 235, 59));
        pitchLengthZonePaint.setStyle(Paint.Style.STROKE);
        pitchLengthZonePaint.setStrokeWidth(2f);
        pitchLengthZonePaint.setPathEffect(new DashPathEffect(new float[]{8, 6}, 0));

        boundaryRopePaint.setColor(Color.WHITE);
        boundaryRopePaint.setStrokeWidth(4.5f);

        // Stumps & Bails
        stumpPaint.setColor(Color.parseColor("#FDD835"));
        bailPaint.setColor(Color.parseColor("#FFF59D"));

        // Leather Ball Colors
        ballPaint.setColor(Color.parseColor("#C62828"));
        ballHighlightPaint.setColor(Color.argb(160, 255, 255, 255));
        ballSeamPaint.setColor(Color.argb(220, 255, 255, 255));
        ballSeamPaint.setStrokeWidth(2.5f);
        ballShadowPaint.setColor(Color.argb(120, 10, 20, 10));

        ballTrailPaint.setColor(Color.argb(70, 255, 255, 255));
        ballTrailPaint.setStrokeWidth(4f);

        // Batsman & Bowler Kits
        batsmanPadsPaint.setColor(Color.parseColor("#ECEFF1"));
        batsmanJerseyPaint.setColor(Color.parseColor("#0288D1"));
        batsmanHelmetPaint.setColor(Color.parseColor("#01579B"));

        batWillowPaint.setColor(Color.parseColor("#8D6E63"));
        batGripPaint.setColor(Color.parseColor("#D32F2F"));
        batGripPaint.setStrokeWidth(3.5f);

        bowlerJerseyPaint.setColor(Color.parseColor("#D32F2F"));

        floodlightTowerPaint.setColor(Color.parseColor("#37474F"));
        floodlightGlowPaint.setColor(Color.argb(160, 255, 255, 210));

        // Touch Swipe Trajectory Paints
        swipeTrailPaint.setColor(Color.parseColor("#FFD700"));
        swipeTrailPaint.setStrokeWidth(8f);
        swipeTrailPaint.setStrokeCap(Paint.Cap.ROUND);

        swipeGlowPaint.setColor(Color.argb(120, 255, 215, 0));
        swipeGlowPaint.setStrokeWidth(18f);
        swipeGlowPaint.setStrokeCap(Paint.Cap.ROUND);

        for (int i = 0; i < TRAIL_POINTS; i++) {
            trailZ[i] = -1f;
        }

        physicsLoopHandler.post(physicsLoopRunnable);
    }

    public void setCricketActionListener(CricketActionListener listener) {
        this.listener = listener;
    }

    public void setShotConfig(int direction, boolean lofted) {
        this.shotDirection = direction;
        this.isLofted = lofted;
    }

    public int getShotDirection() {
        return shotDirection;
    }

    public boolean isLofted() {
        return isLofted;
    }

    public int getCurrentState() {
        return currentState;
    }

    /**
     * Touch Screen Gesture Listener:
     * Full omni-directional swipe detection that dynamically controls:
     * 1. Swipe Direction: Up (Lofted Six), Left (Leg Side Flick/Sweep), Right (Off Side Cover Drive/Cut)
     * 2. Swipe Velocity: Fast swipes increase shot power and boundary clearance
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker.addMovement(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchDownX = event.getX();
                touchDownY = event.getY();
                touchDownTime = System.currentTimeMillis();
                currentTouchX = touchDownX;
                currentTouchY = touchDownY;
                isTrackingSwipe = true;
                return true;

            case MotionEvent.ACTION_MOVE:
                currentTouchX = event.getX();
                currentTouchY = event.getY();
                return true;

            case MotionEvent.ACTION_UP:
                if (!isTrackingSwipe) return true;
                isTrackingSwipe = false;

                float deltaX = event.getX() - touchDownX;
                float deltaY = event.getY() - touchDownY;
                long duration = Math.max(1, System.currentTimeMillis() - touchDownTime);

                velocityTracker.computeCurrentVelocity(1000);
                float vx = velocityTracker.getXVelocity();
                float vy = velocityTracker.getYVelocity();
                float totalVelocity = (float) Math.hypot(vx, vy);

                // Set visual swipe trail
                swipeStartX = touchDownX;
                swipeStartY = touchDownY;
                swipeEndX = event.getX();
                swipeEndY = event.getY();
                swipeAlpha = 1.0f;

                // Threshold: minimum distance to register a deliberate swipe gesture
                float minDistance = 45f;
                float swipeDistance = (float) Math.hypot(deltaX, deltaY);

                if (swipeDistance >= minDistance) {
                    processSwipeGesture(deltaX, deltaY, totalVelocity);
                } else {
                    // Tap gesture defaults to currently selected HUD directional button
                    gesturePowerMultiplier = 1.0f;
                    triggerBatSwing();
                }

                if (velocityTracker != null) {
                    velocityTracker.recycle();
                    velocityTracker = null;
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                isTrackingSwipe = false;
                if (velocityTracker != null) {
                    velocityTracker.recycle();
                    velocityTracker = null;
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    /**
     * Maps dynamic swipe angle and velocity into shot parameters
     */
    private void processSwipeGesture(float deltaX, float deltaY, float velocity) {
        // Velocity multiplier: 1.0x (standard) to 1.45x (maximum power flick)
        gesturePowerMultiplier = Math.min(1.45f, Math.max(0.95f, velocity / 1400f));

        // Evaluate swipe angle (in degrees: 0 to 360)
        double angleRad = Math.atan2(deltaY, deltaX);
        double angleDeg = Math.toDegrees(angleRad); // -180 to 180

        // Invert Y coordinate standard: upwards swipe has negative deltaY
        // -135 to -45: Pure Upward Swipe (Lofted Straight Drive / Six)
        // -180 to -135 or -45 to 0: Diagonal Upward Swipes
        // Horizontal Swipes: Left (Leg) vs Right (Off)
        int resolvedDirection = 1;
        boolean resolvedLofted = false;

        if (deltaY < -60f) {
            // Distinct upward swipe -> Lofted Shot!
            resolvedLofted = true;
            if (deltaX < -50f) {
                resolvedDirection = 0; // Slog sweep to leg side
            } else if (deltaX > 50f) {
                resolvedDirection = 2; // Inside-out loft over cover
            } else {
                resolvedDirection = 1; // Straight Six over bowler's head
            }
        } else {
            // Ground level drives, glances, and cuts
            resolvedLofted = false;
            if (deltaX < -40f) {
                resolvedDirection = 0; // Leg glance / pull along ground
            } else if (deltaX > 40f) {
                resolvedDirection = 2; // Cover drive / square cut along ground
            } else {
                resolvedDirection = 1; // Straight punch down the ground
            }
        }

        this.shotDirection = resolvedDirection;
        this.isLofted = resolvedLofted;

        if (listener != null) {
            listener.onShotTriggered(shotDirection, isLofted);
        }

        triggerBatSwing();
    }

    /**
     * Start bowling delivery with realistic pace, swing and lengths
     */
    public void startNextDelivery() {
        if (currentState == STATE_BOWLER_RUNUP || currentState == STATE_BALL_IN_AIR) return;

        ballZ = 1.0f;
        ballX = 0f;
        ballY = 1.45f;
        hasBounced = false;
        isSwinging = false;
        batAngleDeg = 0f;
        batSweepX = 0f;
        bowlerRunProgress = 0f;
        bailOffY1 = 0f;
        bailOffY2 = 0f;
        bailVelY = 0f;
        bailSpin = 0f;

        for (int i = 0; i < TRAIL_POINTS; i++) {
            trailZ[i] = -1f;
        }

        // 4 Delivery variations with predictable, batter-friendly physics
        int roll = random.nextInt(4);
        if (roll == 0) {
            deliveryType = "Full Length";
            pitchSpotZ = 0.22f;
            pitchSpotX = (random.nextFloat() - 0.5f) * 0.08f;
            ballSpeedZ = 0.019f;
            ballSpeedY = -0.022f;
            ballSpeedX = (random.nextFloat() - 0.5f) * 0.002f;
        } else if (roll == 1) {
            deliveryType = "Outswinger";
            pitchSpotZ = 0.32f;
            pitchSpotX = 0.08f;
            ballSpeedZ = 0.018f;
            ballSpeedY = -0.020f;
            ballSpeedX = 0.0025f;
        } else if (roll == 2) {
            deliveryType = "Inswinging Yorker";
            pitchSpotZ = 0.14f;
            pitchSpotX = -0.05f;
            ballSpeedZ = 0.020f;
            ballSpeedY = -0.024f;
            ballSpeedX = -0.002f;
        } else {
            deliveryType = "Spin Off-Break";
            pitchSpotZ = 0.35f;
            pitchSpotX = -0.10f;
            ballSpeedZ = 0.016f;
            ballSpeedY = -0.018f;
            ballSpeedX = -0.003f;
        }

        currentState = STATE_BOWLER_RUNUP;
    }

    /**
     * Expanded batting timing and collision logic
     */
    public void triggerBatSwing() {
        if (isSwinging) return;
        isSwinging = true;
        swingStartTimestamp = System.currentTimeMillis();

        // Audio trigger: bat swing whoosh
        if (soundManager != null) {
            soundManager.playBatWhoosh();
        }

        if (currentState == STATE_BALL_IN_AIR) {
            float contactZ = ballZ;

            // Expanded timing contact window
            if (contactZ > 0.46f) {
                // Ball is still way down the bowler's end
                if (listener != null) {
                    listener.onTimingFeedback("TOO EARLY! Wait for the ball ⏱️", Color.parseColor("#FFA000"));
                }
            } else if (contactZ < -0.05f) {
                // Ball has already completely passed the crease
                if (listener != null) {
                    listener.onTimingFeedback("TOO LATE! Beaten by pace ⚡", Color.parseColor("#E53935"));
                }
            } else {
                // Legitimate contact made!
                executeBatContact(contactZ);
            }
        }
    }

    /**
     * Executes the collision and maps direction & power accurately
     */
    private void executeBatContact(float contactZ) {
        currentState = STATE_SHOT_HIT;
        hitBallX = ballX;
        hitBallY = Math.max(0.2f, ballY);
        hitBallZ = ballZ;

        // Sweet spot is right at the batting crease (0.07f to 0.18f)
        float sweetSpotCenter = 0.12f;
        float timingDelta = Math.abs(contactZ - sweetSpotCenter);

        // Timing thresholds
        boolean isPerfect = timingDelta <= 0.055f;
        boolean isGood = timingDelta > 0.055f && timingDelta <= 0.16f;
        boolean isPlayableEarly = contactZ > 0.16f;
        boolean isPlayableLate = contactZ < 0.065f;

        // Lateral trajectory angle based on user swipe (< LEG, STRAIGHT, OFF >)
        float lateralBase = 0f;
        String shotName = "Straight";
        if (shotDirection == 0) {
            // LEG SIDE: pulls, flicks, on-drives to the left
            lateralBase = -0.048f - (random.nextFloat() * 0.015f);
            shotName = isLofted ? "Towering Slog Sweep" : "Crisp Mid-Wicket Flick";
        } else if (shotDirection == 2) {
            // OFF SIDE: cover drives, square cuts, slashes to the right
            lateralBase = 0.048f + (random.nextFloat() * 0.015f);
            shotName = isLofted ? "Inside-Out Loft" : "Glorious Cover Drive";
        } else {
            // STRAIGHT: down the ground
            lateralBase = (random.nextFloat() - 0.5f) * 0.012f;
            shotName = isLofted ? "Magnificent Straight Six" : "Bullet Straight Drive";
        }

        if (isLofted) {
            if (isPerfect) {
                // Maximum power 6 over the fence
                hitVelY = (0.068f + (random.nextFloat() * 0.008f)) * gesturePowerMultiplier;
                hitVelZ = (0.046f + (random.nextFloat() * 0.008f)) * gesturePowerMultiplier;
                hitVelX = lateralBase * 1.35f;
                bounceRestitution = 0.40f;
                computedDistanceMeters = (int) ((88 + random.nextInt(26)) * Math.min(1.3f, gesturePowerMultiplier));

                if (soundManager != null) {
                    soundManager.playBatHit(true);
                    soundManager.playCrowdCheer();
                }

                if (listener != null) {
                    listener.onTimingFeedback("🚀 PERFECT TIMING! " + shotName + " (SIX!)", Color.parseColor("#00E676"));
                    listener.onBallCompleted(6, false, "", "PERFECT", computedDistanceMeters);
                }
            } else if (isGood) {
                // High one-bounce or clearing boundary for 4 or 6
                boolean reachesSix = (random.nextBoolean() || gesturePowerMultiplier > 1.15f);
                hitVelY = 0.054f * gesturePowerMultiplier;
                hitVelZ = 0.038f * gesturePowerMultiplier;
                hitVelX = lateralBase * 1.15f;
                bounceRestitution = 0.45f;
                int runs = reachesSix ? 6 : 4;
                computedDistanceMeters = (int) ((reachesSix ? 78 + random.nextInt(8) : 66 + random.nextInt(8)) * Math.min(1.2f, gesturePowerMultiplier));

                if (soundManager != null) {
                    soundManager.playBatHit(reachesSix);
                    soundManager.playCrowdCheer();
                }

                if (listener != null) {
                    listener.onTimingFeedback("🏏 " + (runs == 6 ? "CLEARS THE ROPE! (SIX)" : "OVER THE INFIELD! (FOUR)"), Color.parseColor("#00E676"));
                    listener.onBallCompleted(runs, false, "", "GOOD", computedDistanceMeters);
                }
            } else if (isPlayableEarly) {
                // Early loft: aerial push over the 30-yard circle for 2 or 3 runs
                hitVelY = 0.042f;
                hitVelZ = 0.024f;
                hitVelX = lateralBase * 0.85f;
                bounceRestitution = 0.48f;
                computedDistanceMeters = 42 + random.nextInt(12);

                if (soundManager != null) soundManager.playBatHit(false);

                if (listener != null) {
                    listener.onTimingFeedback("EARLY CONTACT: Lands safely in the deep! (2 Runs)", Color.parseColor("#FFD54F"));
                    listener.onBallCompleted(2, false, "", "EARLY", computedDistanceMeters);
                }
            } else {
                // Playable late slice through third man or fine leg for 1 or 2 runs
                hitVelY = 0.038f;
                hitVelZ = 0.022f;
                hitVelX = lateralBase * 0.75f + (shotDirection == 0 ? 0.02f : -0.02f);
                bounceRestitution = 0.48f;
                computedDistanceMeters = 38 + random.nextInt(10);

                if (soundManager != null) soundManager.playBatHit(false);

                if (listener != null) {
                    listener.onTimingFeedback("LATE DEFLECTION: Guides it fine! (1 Run)", Color.parseColor("#81D4FA"));
                    listener.onBallCompleted(1, false, "", "LATE", computedDistanceMeters);
                }
            }
        } else {
            // Ground shot
            if (isPerfect) {
                // Blistering boundary along the turf
                hitVelY = 0.008f;
                hitVelZ = 0.052f * gesturePowerMultiplier;
                hitVelX = lateralBase * 1.30f;
                bounceRestitution = 0.30f;
                computedDistanceMeters = 72 + random.nextInt(12);

                if (soundManager != null) {
                    soundManager.playBatHit(false);
                    soundManager.playCrowdCheer();
                }

                if (listener != null) {
                    listener.onTimingFeedback("⚡ SWEET TIMING! " + shotName + " (FOUR!)", Color.parseColor("#00E676"));
                    listener.onBallCompleted(4, false, "", "PERFECT", computedDistanceMeters);
                }
            } else if (isGood) {
                // Well placed along the carpet for 2 runs
                hitVelY = 0.006f;
                hitVelZ = 0.036f * gesturePowerMultiplier;
                hitVelX = lateralBase * 1.05f;
                bounceRestitution = 0.35f;
                computedDistanceMeters = 48 + random.nextInt(14);

                if (soundManager != null) soundManager.playBatHit(false);

                if (listener != null) {
                    listener.onTimingFeedback("WELL PLACED GAP! (2 Runs)", Color.parseColor("#FFD54F"));
                    listener.onBallCompleted(2, false, "", "GOOD", computedDistanceMeters);
                }
            } else if (isPlayableEarly) {
                // Solid front foot punch for a single
                hitVelY = 0.004f;
                hitVelZ = 0.022f;
                hitVelX = lateralBase * 0.70f;
                bounceRestitution = 0.35f;
                computedDistanceMeters = 28 + random.nextInt(10);

                if (soundManager != null) soundManager.playBatHit(false);

                if (listener != null) {
                    listener.onTimingFeedback("EARLY PUNCH: Pushed into the gap (1 Run)", Color.parseColor("#81D4FA"));
                    listener.onBallCompleted(1, false, "", "EARLY", computedDistanceMeters);
                }
            } else {
                // Late dab / glance
                hitVelY = 0.003f;
                hitVelZ = 0.018f;
                hitVelX = lateralBase * 0.50f;
                bounceRestitution = 0.35f;
                computedDistanceMeters = 20 + random.nextInt(8);

                if (soundManager != null) soundManager.playBatHit(false);

                if (listener != null) {
                    listener.onTimingFeedback("LATE GLANCE: Quick single taken! (1 Run)", Color.parseColor("#81D4FA"));
                    listener.onBallCompleted(1, false, "", "LATE", computedDistanceMeters);
                }
            }
        }
    }

    private void updatePhysics() {
        // Fade swipe trail over time
        if (swipeAlpha > 0f) {
            swipeAlpha -= 0.045f;
            if (swipeAlpha < 0f) swipeAlpha = 0f;
        }

        if (currentState == STATE_BOWLER_RUNUP) {
            bowlerRunProgress += 0.042f;
            if (bowlerRunProgress >= 1.0f) {
                currentState = STATE_BALL_IN_AIR;
            }
        } else if (currentState == STATE_BALL_IN_AIR) {
            trailX[trailHead] = ballX;
            trailY[trailHead] = ballY;
            trailZ[trailHead] = ballZ;
            trailHead = (trailHead + 1) % TRAIL_POINTS;

            ballZ -= ballSpeedZ;

            if (ballZ <= pitchSpotZ && !hasBounced) {
                hasBounced = true;
                ballSpeedY = 0.038f;
                if (deliveryType.contains("Outswinger")) {
                    ballSpeedX += 0.003f;
                } else if (deliveryType.contains("Inswinging")) {
                    ballSpeedX -= 0.003f;
                } else if (deliveryType.contains("Spin")) {
                    ballSpeedX += 0.005f;
                }
            } else if (!hasBounced) {
                ballSpeedY -= 0.0018f;
            } else {
                ballSpeedY -= 0.0022f;
            }

            ballY += ballSpeedY;
            ballX += ballSpeedX;

            if (ballZ <= -0.02f) {
                boolean isStumpsHit = Math.abs(ballX) <= 0.075f && ballY >= 0.0f && ballY <= 0.72f;
                if (isStumpsHit) {
                    currentState = STATE_WICKET_FALLEN;
                    bailVelY = 0.075f;
                    bailSpin = 12f;

                    if (soundManager != null) {
                        soundManager.playWicket();
                    }

                    if (listener != null) {
                        listener.onTimingFeedback("BOWLED HIM! 💥 Stumps Shattered!", Color.parseColor("#E53935"));
                        listener.onBallCompleted(0, true, "Bowled", "MISS", 0);
                    }
                } else {
                    currentState = STATE_IDLE;

                    if (soundManager != null) {
                        soundManager.playMissBall();
                    }

                    if (listener != null) {
                        listener.onTimingFeedback("DOT BALL: Safely left through to keeper", Color.parseColor("#B0BEC5"));
                        listener.onBallCompleted(0, false, "", "BEATEN", 0);
                    }
                }
            }
        } else if (currentState == STATE_SHOT_HIT) {
            hitBallX += hitVelX;
            hitBallY += hitVelY;
            hitBallZ += hitVelZ;
            hitVelY -= 0.0030f;

            if (hitBallY <= 0.0f) {
                hitBallY = 0.0f;
                hitVelY = -hitVelY * bounceRestitution;
                hitVelX *= 0.88f;
                hitVelZ *= 0.88f;
            }

            if (hitBallZ > 1.35f || (hitBallY == 0.0f && Math.abs(hitVelY) < 0.001f && Math.abs(hitVelZ) < 0.002f)) {
                currentState = STATE_IDLE;
            }
        } else if (currentState == STATE_WICKET_FALLEN) {
            bailOffY1 += bailVelY;
            bailOffY2 += bailVelY * 0.85f;
            bailVelY -= 0.004f;
            bailSpin += 14f;
            if (bailOffY1 <= 0.0f) {
                bailOffY1 = 0f;
                bailOffY2 = 0f;
                currentState = STATE_IDLE;
            }
        }

        if (isSwinging) {
            long elapsed = System.currentTimeMillis() - swingStartTimestamp;
            if (elapsed < SWING_DURATION_MS) {
                float norm = (float) elapsed / SWING_DURATION_MS;
                batAngleDeg = (float) Math.sin(norm * Math.PI) * 85f;
                if (shotDirection == 0) {
                    batSweepX = -norm * 28f;
                } else if (shotDirection == 2) {
                    batSweepX = norm * 28f;
                } else {
                    batSweepX = 0f;
                }
            } else {
                isSwinging = false;
                batAngleDeg = 0f;
                batSweepX = 0f;
            }
        }
    }

    private float projectX(float x3d, float z3d, float screenWidth) {
        float perspectiveFactor = 1.0f + (z3d * 2.35f);
        return (screenWidth / 2f) + (x3d * screenWidth * 0.95f / perspectiveFactor);
    }

    private float projectY(float y3d, float z3d, float screenHeight) {
        float horizonY = screenHeight * 0.40f;
        float groundSpread = screenHeight * 0.52f;
        float depthScale = (1.0f - (z3d * 0.68f));
        float groundY = horizonY + (groundSpread * depthScale);
        float heightScale = 160f / (1.0f + (z3d * 2.1f));
        return groundY - (y3d * heightScale);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        float horizonY = h * 0.40f;

        // 1. Stadium Night Sky Gradient
        LinearGradient skyGrad = new LinearGradient(0, 0, 0, horizonY,
                Color.parseColor("#040C14"), Color.parseColor("#0F2838"), Shader.TileMode.CLAMP);
        skyPaint.setShader(skyGrad);
        canvas.drawRect(0, 0, w, horizonY, skyPaint);

        // 2. Stadium Stands & Spectators
        crowdPaint.setColor(Color.parseColor("#1B3026"));
        canvas.drawRect(0, horizonY - 55f, w, horizonY, crowdPaint);

        stadiumRoofsPaint.setColor(Color.parseColor("#264234"));
        canvas.drawRect(0, horizonY - 65f, w, horizonY - 55f, stadiumRoofsPaint);

        // Stadium Floodlight Towers
        for (int i = 0; i < 4; i++) {
            float towerX = (w * 0.12f) + (i * w * 0.25f);
            canvas.drawRect(towerX - 4f, horizonY - 80f, towerX + 4f, horizonY - 10f, floodlightTowerPaint);
            canvas.drawCircle(towerX, horizonY - 80f, 12f, floodlightGlowPaint);
        }

        // 3. Outfield Turf Grass
        canvas.drawRect(0, horizonY, w, h, turfPaint);

        for (int i = 1; i <= 5; i++) {
            float ringZ = i * 0.22f;
            float ringY = projectY(0f, ringZ, h);
            canvas.drawLine(0, ringY, w, ringY, turfRingsPaint);
        }

        // Boundary Rope
        float ropeFarLeft = projectX(-0.65f, 1.15f, w);
        float ropeFarRight = projectX(0.65f, 1.15f, w);
        float ropeFarY = projectY(0f, 1.15f, h);
        canvas.drawLine(ropeFarLeft, ropeFarY, ropeFarRight, ropeFarY, boundaryRopePaint);

        // 4. 3D Cricket Pitch
        Path pitchPath = new Path();
        float pFarL = projectX(-0.16f, 1.0f, w);
        float pFarR = projectX(0.16f, 1.0f, w);
        float pFarY = projectY(0f, 1.0f, h);

        float pNearL = projectX(-0.36f, 0.0f, w);
        float pNearR = projectX(0.36f, 0.0f, w);
        float pNearY = projectY(0f, 0.0f, h);

        pitchPath.moveTo(pFarL, pFarY);
        pitchPath.lineTo(pFarR, pFarY);
        pitchPath.lineTo(pNearR, pNearY);
        pitchPath.lineTo(pNearL, pNearY);
        pitchPath.close();
        canvas.drawPath(pitchPath, pitchPaint);

        // Pitch Good Length Zone Marker
        float glFarL = projectX(-0.14f, 0.38f, w);
        float glFarR = projectX(0.14f, 0.38f, w);
        float glFarY = projectY(0f, 0.38f, h);
        float glNearL = projectX(-0.18f, 0.22f, w);
        float glNearR = projectX(0.18f, 0.22f, w);
        float glNearY = projectY(0f, 0.22f, h);

        Path glZone = new Path();
        glZone.moveTo(glFarL, glFarY);
        glZone.lineTo(glFarR, glFarY);
        glZone.lineTo(glNearR, glNearY);
        glZone.lineTo(glNearL, glNearY);
        glZone.close();
        canvas.drawPath(glZone, pitchLengthZonePaint);

        // Crease Lines
        float bCreaseL = projectX(-0.15f, 0.95f, w);
        float bCreaseR = projectX(0.15f, 0.95f, w);
        float bCreaseY = projectY(0f, 0.95f, h);
        canvas.drawLine(bCreaseL, bCreaseY, bCreaseR, bCreaseY, pitchCreasePaint);

        float batCreaseL = projectX(-0.34f, 0.06f, w);
        float batCreaseR = projectX(0.34f, 0.06f, w);
        float batCreaseY = projectY(0f, 0.06f, h);
        canvas.drawLine(batCreaseL, batCreaseY, batCreaseR, batCreaseY, pitchCreasePaint);

        // 5. Far Stumps
        stumpPaint.setStrokeWidth(2.8f);
        for (int i = -1; i <= 1; i++) {
            float sx = projectX(i * 0.02f, 1.0f, w);
            float syBase = projectY(0f, 1.0f, h);
            float syTop = projectY(0.48f, 1.0f, h);
            canvas.drawLine(sx, syBase, sx, syTop, stumpPaint);
        }

        // 6. Bowler Animation
        if (currentState == STATE_BOWLER_RUNUP || currentState == STATE_BALL_IN_AIR) {
            float bRunX = projectX(-0.05f + (bowlerRunProgress * 0.05f), 1.0f, w);
            float bRunY = projectY(0.48f, 1.0f, h);
            canvas.drawCircle(bRunX, bRunY - 26f, 8f, bowlerJerseyPaint);
            canvas.drawLine(bRunX, bRunY - 18f, bRunX, bRunY, bowlerJerseyPaint);
        }

        // 7. Near Stumps
        stumpPaint.setStrokeWidth(5.5f);
        float nStumpBaseY = projectY(0f, 0.0f, h);
        float nStumpTopY = projectY(0.65f, 0.0f, h);
        for (int i = -1; i <= 1; i++) {
            float sx = projectX(i * 0.038f, 0.0f, w);
            canvas.drawLine(sx, nStumpBaseY, sx, nStumpTopY, stumpPaint);
        }

        // Bails
        float bailLeftX = projectX(-0.05f, 0.0f, w);
        float bailRightX = projectX(0.05f, 0.0f, w);
        float bailCurrY = nStumpTopY - (bailOffY1 * 130f);

        canvas.save();
        if (currentState == STATE_WICKET_FALLEN) {
            canvas.rotate(bailSpin, (bailLeftX + bailRightX) / 2f, bailCurrY);
        }
        bailPaint.setStrokeWidth(4.5f);
        canvas.drawLine(bailLeftX, bailCurrY, bailRightX, bailCurrY, bailPaint);
        canvas.restore();

        // 8. Batsman Model
        float batsmanBaseX = projectX(0.14f, 0.06f, w) + batSweepX;
        float batsmanBaseY = projectY(0f, 0.06f, h);

        canvas.drawOval(new RectF(batsmanBaseX - 30f, batsmanBaseY - 10f, batsmanBaseX + 30f, batsmanBaseY + 10f), ballShadowPaint);
        canvas.drawRoundRect(new RectF(batsmanBaseX - 18f, batsmanBaseY - 52f, batsmanBaseX - 5f, batsmanBaseY), 5f, 5f, batsmanPadsPaint);
        canvas.drawRoundRect(new RectF(batsmanBaseX + 2f, batsmanBaseY - 52f, batsmanBaseX + 15f, batsmanBaseY), 5f, 5f, batsmanPadsPaint);
        canvas.drawRoundRect(new RectF(batsmanBaseX - 20f, batsmanBaseY - 110f, batsmanBaseX + 20f, batsmanBaseY - 48f), 9f, 9f, batsmanJerseyPaint);
        canvas.drawCircle(batsmanBaseX, batsmanBaseY - 120f, 14f, batsmanHelmetPaint);

        // Bat
        canvas.save();
        canvas.rotate(batAngleDeg, batsmanBaseX + 10f, batsmanBaseY - 55f);
        canvas.drawRoundRect(new RectF(batsmanBaseX + 8f, batsmanBaseY - 60f, batsmanBaseX + 19f, batsmanBaseY + 16f), 4f, 4f, batWillowPaint);
        canvas.drawLine(batsmanBaseX + 13.5f, batsmanBaseY - 80f, batsmanBaseX + 13.5f, batsmanBaseY - 60f, batGripPaint);
        canvas.restore();

        // 9. Ball Motion Trail
        if (currentState == STATE_BALL_IN_AIR) {
            for (int i = 0; i < TRAIL_POINTS; i++) {
                if (trailZ[i] >= 0f) {
                    float tX = projectX(trailX[i], trailZ[i], w);
                    float tY = projectY(trailY[i], trailZ[i], h);
                    float tRad = 3f + (1.0f - trailZ[i]) * 10f;
                    canvas.drawCircle(tX, tY, tRad * 0.45f, ballTrailPaint);
                }
            }
        }

        // 10. Ball 3D Rendering
        if (currentState == STATE_BALL_IN_AIR) {
            float sBallX = projectX(ballX, ballZ, w);
            float sBallY = projectY(ballY, ballZ, h);
            float sShadowY = projectY(0f, ballZ, h);

            float radius = 7f + ((1.0f - ballZ) * 19f);
            float shadowRadius = radius * 1.15f;

            canvas.drawOval(new RectF(sBallX - shadowRadius, sShadowY - (shadowRadius * 0.35f),
                    sBallX + shadowRadius, sShadowY + (shadowRadius * 0.35f)), ballShadowPaint);
            canvas.drawCircle(sBallX, sBallY, radius, ballPaint);
            canvas.drawCircle(sBallX - (radius * 0.35f), sBallY - (radius * 0.35f), radius * 0.35f, ballHighlightPaint);
            canvas.drawLine(sBallX - (radius * 0.7f), sBallY, sBallX + (radius * 0.7f), sBallY, ballSeamPaint);
        } else if (currentState == STATE_SHOT_HIT) {
            float sHitX = projectX(hitBallX, hitBallZ, w);
            float sHitY = projectY(hitBallY, hitBallZ, h);
            float sShadowY = projectY(0f, hitBallZ, h);

            float hitRadius = Math.max(5f, 22f - (hitBallZ * 10f));
            canvas.drawOval(new RectF(sHitX - hitRadius, sShadowY - (hitRadius * 0.30f),
                    sHitX + hitRadius, sShadowY + (hitRadius * 0.30f)), ballShadowPaint);
            canvas.drawCircle(sHitX, sHitY, hitRadius, ballPaint);
        }

        // 11. Visual Swipe Trail Glow
        if (swipeAlpha > 0f && swipeStartX >= 0f) {
            int glowAlpha = (int) (swipeAlpha * 120);
            int coreAlpha = (int) (swipeAlpha * 255);
            swipeGlowPaint.setColor(Color.argb(glowAlpha, 255, 215, 0));
            swipeTrailPaint.setColor(Color.argb(coreAlpha, 255, 255, 255));

            canvas.drawLine(swipeStartX, swipeStartY, swipeEndX, swipeEndY, swipeGlowPaint);
            canvas.drawLine(swipeStartX, swipeStartY, swipeEndX, swipeEndY, swipeTrailPaint);
        }
    }
}