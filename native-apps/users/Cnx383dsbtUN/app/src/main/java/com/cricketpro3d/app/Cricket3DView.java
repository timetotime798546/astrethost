package com.cricketpro3d.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

import java.util.Random;

public class Cricket3DView extends View {

    public interface CricketActionListener {
        void onBallCompleted(int runsScored, boolean isWicket, String dismissalType, String shotQuality, int distanceMeters);
        void onTimingFeedback(String timingText, int color);
    }

    private CricketActionListener listener;

    // Paints
    private final Paint skyPaint = new Paint();
    private final Paint grassPaint = new Paint();
    private final Paint grassStripePaint = new Paint();
    private final Paint pitchPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint creasePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stumpPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ballPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ballShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint batsmanPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint batPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bowlerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint crowdPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint floodlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Ball 3D state: X (-1.0 to 1.0), Y (height above ground), Z (1.0 = bowler, 0.0 = batsman)
    private float ballX = 0f;
    private float ballY = 1.2f;
    private float ballZ = 1.0f;

    private float pitchSpotX = 0f; // lateral spot
    private float pitchSpotZ = 0.28f; // length of pitch where ball lands

    private float ballSpeedZ = 0.026f;
    private float ballSpeedY = -0.04f;
    private float ballSpeedX = 0.002f;
    private boolean hasBounced = false;

    // Batsman positioning & shot mechanics
    private float batsmanOffset = 0f; // -0.4 (leg) to +0.4 (off)
    private int shotDirection = 1; // 0: Left/Leg, 1: Straight, 2: Right/Off
    private boolean isLofted = false;

    // Bat Swing Animation
    private boolean isSwinging = false;
    private float batAngle = 0f; // -45 to 60 deg
    private long swingStartTime = 0;

    // State machine
    public static final int STATE_IDLE = 0;
    public static final int STATE_BOWLER_RUNUP = 1;
    public static final int STATE_BALL_IN_AIR = 2;
    public static final int STATE_SHOT_HIT = 3;
    public static final int STATE_WICKET_FALLEN = 4;
    private int currentState = STATE_IDLE;

    // Hit projectile
    private float hitBallX = 0f;
    private float hitBallY = 0f;
    private float hitBallZ = 0f;
    private float hitVelX = 0f;
    private float hitVelY = 0f;
    private float hitVelZ = 0f;
    private int shotDistanceMeters = 0;
    private int computedRuns = 0;

    // Bails flying physics on bowled
    private float bail1Y = 0f;
    private float bail2Y = 0f;
    private float bailVelY = 0f;

    // Bowler run-up progression
    private float bowlerRunUpProgress = 0f;
    private String deliveryType = "Fast Seamer";

    private final Random random = new Random();
    private final Handler loopHandler = new Handler(Looper.getMainLooper());
    private final Runnable loopRunnable = new Runnable() {
        @Override
        public void run() {
            updatePhysics();
            invalidate();
            loopHandler.postDelayed(this, 16);
        }
    };

    public Cricket3DView(Context context) {
        super(context);
        init();
    }

    public Cricket3DView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        pitchPaint.setColor(Color.parseColor("#E0D4BA"));
        creasePaint.setColor(Color.WHITE);
        creasePaint.setStrokeWidth(3f);

        stumpPaint.setColor(Color.parseColor("#FFE082"));
        ballPaint.setColor(Color.parseColor("#D32F2F")); // Red leather ball
        ballShadowPaint.setColor(Color.argb(90, 0, 0, 0));

        batsmanPaint.setColor(Color.parseColor("#1565C0")); // Blue jersey
        batPaint.setColor(Color.parseColor("#8D6E63")); // English willow bat
        bowlerPaint.setColor(Color.parseColor("#C62828")); // Red jersey
        floodlightPaint.setColor(Color.argb(180, 255, 255, 230));

        loopHandler.post(loopRunnable);
    }

    public void setCricketActionListener(CricketActionListener listener) {
        this.listener = listener;
    }

    public void setShotConfig(int direction, boolean lofted) {
        this.shotDirection = direction;
        this.isLofted = lofted;
    }

    public void startNextDelivery() {
        if (currentState == STATE_BOWLER_RUNUP || currentState == STATE_BALL_IN_AIR) return;

        // Reset variables
        ballZ = 1.0f;
        ballX = 0f;
        ballY = 1.6f;
        hasBounced = false;
        isSwinging = false;
        batAngle = 0f;
        bowlerRunUpProgress = 0f;
        bail1Y = 0f;
        bail2Y = 0f;
        bailVelY = 0f;

        // Dynamic bowling variety
        int deliveryRoll = random.nextInt(4);
        if (deliveryRoll == 0) {
            deliveryType = "Yorker";
            pitchSpotZ = 0.12f;
            pitchSpotX = (random.nextFloat() - 0.5f) * 0.15f;
            ballSpeedZ = 0.028f;
        } else if (deliveryRoll == 1) {
            deliveryType = "Good Length Outswinger";
            pitchSpotZ = 0.28f;
            pitchSpotX = 0.12f + (random.nextFloat() * 0.15f);
            ballSpeedZ = 0.025f;
        } else if (deliveryRoll == 2) {
            deliveryType = "Bouncer";
            pitchSpotZ = 0.45f;
            pitchSpotX = -0.08f + (random.nextFloat() * 0.16f);
            ballSpeedZ = 0.029f;
        } else {
            deliveryType = "Off-Spin Turn";
            pitchSpotZ = 0.26f;
            pitchSpotX = -0.18f;
            ballSpeedZ = 0.020f;
        }

        currentState = STATE_BOWLER_RUNUP;
    }

    public void triggerBatSwing() {
        if (isSwinging) return;
        isSwinging = true;
        swingStartTime = System.currentTimeMillis();

        if (currentState == STATE_BALL_IN_AIR) {
            // Assess timing
            // Ideal contact Z is between 0.05 and 0.12
            float currentZ = ballZ;

            if (currentZ > 0.28f) {
                // Too early
                if (listener != null) {
                    listener.onTimingFeedback("TOO EARLY! (Beaten by flight)", Color.parseColor("#FFA000"));
                }
            } else if (currentZ < 0.02f) {
                // Too late
                if (listener != null) {
                    listener.onTimingFeedback("TOO LATE! (Through the gate)", Color.parseColor("#E53935"));
                }
            } else {
                // Contact window
                calculateShotOutcome(currentZ);
            }
        }
    }

    private void calculateShotOutcome(float contactZ) {
        currentState = STATE_SHOT_HIT;
        hitBallX = ballX;
        hitBallY = ballY;
        hitBallZ = ballZ;

        // Timing precision factor: optimal is around 0.085
        float timingDiff = Math.abs(contactZ - 0.085f);
        boolean isPerfect = timingDiff < 0.035f;
        boolean isGood = timingDiff < 0.065f;

        // Lateral shot direction
        float dirAngle = 0f;
        if (shotDirection == 0) dirAngle = -0.04f; // Leg side
        else if (shotDirection == 2) dirAngle = 0.04f; // Off side
        else dirAngle = (random.nextFloat() - 0.5f) * 0.015f; // Straight

        if (isLofted) {
            if (isPerfect) {
                hitVelY = 0.07f;
                hitVelZ = 0.05f;
                hitVelX = dirAngle * 1.5f;
                computedRuns = 6;
                shotDistanceMeters = 85 + random.nextInt(25);
                if (listener != null) {
                    listener.onTimingFeedback("PERFECT TIMING! 🚀 SIX!", Color.parseColor("#00E676"));
                    listener.onBallCompleted(6, false, "", "PERFECT", shotDistanceMeters);
                }
            } else if (isGood) {
                hitVelY = 0.05f;
                hitVelZ = 0.035f;
                hitVelX = dirAngle;
                computedRuns = 4;
                shotDistanceMeters = 68 + random.nextInt(12);
                if (listener != null) {
                    listener.onTimingFeedback("GOOD SHOT! 🏏 FOUR!", Color.parseColor("#00E676"));
                    listener.onBallCompleted(4, false, "", "GOOD", shotDistanceMeters);
                }
            } else {
                // Mistimed loft -> Caught in deep!
                hitVelY = 0.08f;
                hitVelZ = 0.02f;
                hitVelX = dirAngle * 0.5f;
                computedRuns = 0;
                shotDistanceMeters = 45;
                if (listener != null) {
                    listener.onTimingFeedback("MISTIMED! CAUGHT OUT! 🔴", Color.parseColor("#E53935"));
                    listener.onBallCompleted(0, true, "Caught Out", "POOR", shotDistanceMeters);
                }
            }
        } else {
            // Ground drive
            if (isPerfect) {
                hitVelY = 0.005f;
                hitVelZ = 0.048f;
                hitVelX = dirAngle * 1.2f;
                computedRuns = 4;
                shotDistanceMeters = 72;
                if (listener != null) {
                    listener.onTimingFeedback("SWEET GROUND DRIVE! 4️⃣ FOUR!", Color.parseColor("#00E676"));
                    listener.onBallCompleted(4, false, "", "PERFECT", shotDistanceMeters);
                }
            } else if (isGood) {
                hitVelY = 0.003f;
                hitVelZ = 0.03f;
                hitVelX = dirAngle;
                computedRuns = random.nextBoolean() ? 2 : 1;
                shotDistanceMeters = 40;
                if (listener != null) {
                    listener.onTimingFeedback("WELL PLACED! " + computedRuns + " RUNS", Color.parseColor("#FFEB3B"));
                    listener.onBallCompleted(computedRuns, false, "", "GOOD", shotDistanceMeters);
                }
            } else {
                // Edged or direct to fielder
                hitVelY = 0.002f;
                hitVelZ = 0.015f;
                hitVelX = dirAngle * 0.4f;
                computedRuns = 0;
                shotDistanceMeters = 15;
                if (listener != null) {
                    listener.onTimingFeedback("DOT BALL - Fielded cleanly", Color.parseColor("#B0BEC5"));
                    listener.onBallCompleted(0, false, "", "LATE", shotDistanceMeters);
                }
            }
        }
    }

    private void updatePhysics() {
        if (currentState == STATE_BOWLER_RUNUP) {
            bowlerRunUpProgress += 0.035f;
            if (bowlerRunUpProgress >= 1.0f) {
                currentState = STATE_BALL_IN_AIR;
            }
        } else if (currentState == STATE_BALL_IN_AIR) {
            // Ball advancing towards batsman
            ballZ -= ballSpeedZ;

            // Seam/Spin curve
            if (ballZ < pitchSpotZ && !hasBounced) {
                hasBounced = true;
                ballSpeedY = 0.042f; // rebound upward
                if (deliveryType.contains("Turn")) {
                    ballSpeedX = 0.006f; // spin sharp turn
                }
            } else if (!hasBounced) {
                ballSpeedY -= 0.0032f; // gravity pull towards pitch
            } else {
                ballSpeedY -= 0.0022f; // post-bounce gravity
            }

            ballY += ballSpeedY;
            ballX += ballSpeedX;

            // Check if ball passes batsman into stumps
            if (ballZ <= 0.0f) {
                // Check if hit stumps (stump X between -0.06 and 0.06, Y between 0.0 and 0.7)
                if (Math.abs(ballX) < 0.08f && ballY > 0.0f && ballY < 0.75f) {
                    currentState = STATE_WICKET_FALLEN;
                    bailVelY = 0.08f;
                    if (listener != null) {
                        listener.onTimingFeedback("BOWLED HIM! 💥 WICKET!", Color.parseColor("#E53935"));
                        listener.onBallCompleted(0, true, "Bowled", "MISS", 0);
                    }
                } else {
                    currentState = STATE_IDLE;
                    if (listener != null) {
                        listener.onTimingFeedback("DOT BALL (Beaten outside off)", Color.parseColor("#B0BEC5"));
                        listener.onBallCompleted(0, false, "", "BEATEN", 0);
                    }
                }
            }
        } else if (currentState == STATE_SHOT_HIT) {
            // Ball flies outwards
            hitBallX += hitVelX;
            hitBallY += hitVelY;
            hitBallZ += hitVelZ;
            hitVelY -= 0.0035f; // gravity

            if (hitBallY < 0.0f) {
                hitBallY = 0.0f;
                hitVelY = -hitVelY * 0.4f; // ground bounce
            }

            if (hitBallZ > 1.4f || (hitBallY == 0.0f && Math.abs(hitVelY) < 0.002f)) {
                // Delivery sequence finished
                currentState = STATE_IDLE;
            }
        } else if (currentState == STATE_WICKET_FALLEN) {
            bail1Y += bailVelY;
            bail2Y += bailVelY * 0.8f;
            bailVelY -= 0.005f;
            if (bail1Y < 0.0f) {
                bail1Y = 0.0f;
                bail2Y = 0.0f;
                currentState = STATE_IDLE;
            }
        }

        // Animate bat swing
        if (isSwinging) {
            long elapsed = System.currentTimeMillis() - swingStartTime;
            if (elapsed < 300) {
                batAngle = (elapsed / 300f) * 90f - 30f;
            } else {
                isSwinging = false;
                batAngle = 0f;
            }
        }
    }

    // 3D Perspective Projection Utility: transforms 3D (X, Y, Z) to 2D screen coordinate
    private float projectX(float x3d, float z3d, float screenWidth) {
        float fovFactor = 1.0f + (z3d * 2.2f);
        return (screenWidth / 2f) + (x3d * screenWidth * 0.8f / fovFactor);
    }

    private float projectY(float y3d, float z3d, float screenHeight) {
        float horizon = screenHeight * 0.42f;
        float groundSpread = screenHeight * 0.54f;
        float depthScale = (1.0f - (z3d * 0.65f));
        return horizon + (groundSpread * depthScale) - (y3d * 140f / (1.0f + z3d * 2.0f));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        float horizonY = h * 0.42f;

        // 1. Draw Stadium Sky with Twilight Night-Match Gradient
        LinearGradient skyGrad = new LinearGradient(0, 0, 0, horizonY,
                Color.parseColor("#05111B"), Color.parseColor("#102A38"), Shader.TileMode.CLAMP);
        skyPaint.setShader(skyGrad);
        canvas.drawRect(0, 0, w, horizonY, skyPaint);

        // Stadium Stands & Crowd
        crowdPaint.setColor(Color.parseColor("#1B2F2A"));
        canvas.drawRect(0, horizonY - 45f, w, horizonY, crowdPaint);

        // Floodlights
        for (int i = 0; i < 4; i++) {
            float lightX = (w * 0.12f) + (i * w * 0.26f);
            canvas.drawCircle(lightX, horizonY - 40f, 10f, floodlightPaint);
            canvas.drawLine(lightX, horizonY - 40f, lightX, horizonY - 10f, floodlightPaint);
        }

        // 2. Draw Outfield Turf Grass
        grassPaint.setColor(Color.parseColor("#1E5F29"));
        canvas.drawRect(0, horizonY, w, h, grassPaint);

        // Cut Grass concentric mowing lines in perspective
        grassStripePaint.setColor(Color.argb(35, 255, 255, 255));
        grassStripePaint.setStrokeWidth(6f);
        for (int i = 0; i < 6; i++) {
            float stripeY = horizonY + (i * (h - horizonY) / 6f);
            canvas.drawLine(0, stripeY, w, stripeY, grassStripePaint);
        }

        // 3. Draw 3D Cricket Pitch (trapezoid in perspective)
        Path pitchPath = new Path();
        float pitchFarLeft = projectX(-0.16f, 1.0f, w);
        float pitchFarRight = projectX(0.16f, 1.0f, w);
        float pitchFarY = projectY(0f, 1.0f, h);

        float pitchNearLeft = projectX(-0.35f, 0.0f, w);
        float pitchNearRight = projectX(0.35f, 0.0f, w);
        float pitchNearY = projectY(0f, 0.0f, h);

        pitchPath.moveTo(pitchFarLeft, pitchFarY);
        pitchPath.lineTo(pitchFarRight, pitchFarY);
        pitchPath.lineTo(pitchNearRight, pitchNearY);
        pitchPath.lineTo(pitchNearLeft, pitchNearY);
        pitchPath.close();
        canvas.drawPath(pitchPath, pitchPaint);

        // 4. Bowling Crease & Popping Crease
        float creaseFarX1 = projectX(-0.14f, 0.95f, w);
        float creaseFarX2 = projectX(0.14f, 0.95f, w);
        float creaseFarY = projectY(0f, 0.95f, h);
        canvas.drawLine(creaseFarX1, creaseFarY, creaseFarX2, creaseFarY, creasePaint);

        float creaseNearX1 = projectX(-0.32f, 0.05f, w);
        float creaseNearX2 = projectX(0.32f, 0.05f, w);
        float creaseNearY = projectY(0f, 0.05f, h);
        canvas.drawLine(creaseNearX1, creaseNearY, creaseNearX2, creaseNearY, creasePaint);

        // 5. Draw Bowler Stumps at far end
        for (int i = -1; i <= 1; i++) {
            float sx = projectX(i * 0.02f, 1.0f, w);
            float syBase = projectY(0f, 1.0f, h);
            float syTop = projectY(0.45f, 1.0f, h);
            stumpPaint.setStrokeWidth(3f);
            canvas.drawLine(sx, syBase, sx, syTop, stumpPaint);
        }

        // 6. Draw Bowler at far end during runup
        if (currentState == STATE_BOWLER_RUNUP || currentState == STATE_BALL_IN_AIR) {
            float bowlerRunX = projectX(-0.06f + (bowlerRunUpProgress * 0.06f), 1.0f, w);
            float bowlerRunY = projectY(0.45f, 1.0f, h);
            // Bowler body
            canvas.drawCircle(bowlerRunX, bowlerRunY - 24f, 8f, bowlerPaint); // head
            canvas.drawLine(bowlerRunX, bowlerRunY - 16f, bowlerRunX, bowlerRunY, bowlerPaint); // body
        }

        // 7. Draw Batsman Stumps at near end
        stumpPaint.setStrokeWidth(5f);
        float stumpYBase = projectY(0f, 0.0f, h);
        float stumpYTop = projectY(0.6f, 0.0f, h);
        for (int i = -1; i <= 1; i++) {
            float sx = projectX(i * 0.035f, 0.0f, w);
            canvas.drawLine(sx, stumpYBase, sx, stumpYTop, stumpPaint);
        }

        // Stumps Bails
        float bailLeft = projectX(-0.045f, 0.0f, w);
        float bailRight = projectX(0.045f, 0.0f, w);
        float bailY = stumpYTop - (bail1Y * 120f);
        canvas.drawLine(bailLeft, bailY, bailRight, bailY, stumpPaint);

        // 8. Draw Batsman (near popping crease, facing the bowler)
        float batX = projectX(0.12f + batsmanOffset, 0.04f, w);
        float batY = projectY(0f, 0.04f, h);

        // Batsman Shadow
        canvas.drawOval(new RectF(batX - 25f, batY - 8f, batX + 25f, batY + 8f), ballShadowPaint);

        // Batsman legs & pads (White pads)
        Paint padPaint = new Paint();
        padPaint.setColor(Color.WHITE);
        canvas.drawRoundRect(new RectF(batX - 16f, batY - 45f, batX - 4f, batY), 4f, 4f, padPaint);
        canvas.drawRoundRect(new RectF(batX + 2f, batY - 45f, batX + 14f, batY), 4f, 4f, padPaint);

        // Batsman Torso (Team jersey)
        canvas.drawRoundRect(new RectF(batX - 18f, batY - 95f, batX + 18f, batY - 40f), 8f, 8f, batsmanPaint);

        // Helmet / Head
        Paint helmetPaint = new Paint();
        helmetPaint.setColor(Color.parseColor("#0D47A1"));
        canvas.drawCircle(batX, batY - 105f, 13f, helmetPaint);

        // English Willow Bat with swing angle
        canvas.save();
        canvas.rotate(batAngle, batX + 8f, batY - 50f);
        canvas.drawRoundRect(new RectF(batX + 6f, batY - 52f, batX + 16f, batY + 12f), 3f, 3f, batPaint);
        canvas.drawLine(batX + 11f, batY - 70f, batX + 11f, batY - 52f, creasePaint); // bat handle
        canvas.restore();

        // 9. Draw the Ball in 3D Space
        if (currentState == STATE_BALL_IN_AIR) {
            float screenBallX = projectX(ballX, ballZ, w);
            float screenBallY = projectY(ballY, ballZ, h);
            float screenShadowY = projectY(0f, ballZ, h);

            // Radius scales with 3D depth (closer = larger)
            float radius = 5f + ((1.0f - ballZ) * 16f);

            // Ground shadow
            canvas.drawOval(new RectF(screenBallX - radius, screenShadowY - (radius * 0.35f),
                    screenBallX + radius, screenShadowY + (radius * 0.35f)), ballShadowPaint);

            // Ball sphere
            canvas.drawCircle(screenBallX, screenBallY, radius, ballPaint);

            // White seam stripe on leather ball
            Paint seamPaint = new Paint();
            seamPaint.setColor(Color.WHITE);
            seamPaint.setStrokeWidth(2f);
            canvas.drawLine(screenBallX - (radius * 0.6f), screenBallY, screenBallX + (radius * 0.6f), screenBallY, seamPaint);
        } else if (currentState == STATE_SHOT_HIT) {
            float hitScrX = projectX(hitBallX, hitBallZ, w);
            float hitScrY = projectY(hitBallY, hitBallZ, h);
            float radius = Math.max(4f, 18f - (hitBallZ * 8f));
            canvas.drawCircle(hitScrX, hitScrY, radius, ballPaint);
        }
    }
}