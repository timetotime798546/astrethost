package com.threeduilab.app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

public class ThreeDCardView extends FrameLayout {

    private android.graphics.Camera camera = new android.graphics.Camera();
    private Matrix matrix = new Matrix();
    
    private float rotationX = 0f;
    private float rotationY = 0f;
    private float targetRotationX = 0f;
    private float targetRotationY = 0f;
    
    private Paint glarePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float touchX = -1000f;
    private float touchY = -1000f;
    private boolean isPressed = false;
    private float maxTiltAngle = 25f; 
    private float perspectiveValue = 15f; 
    private ValueAnimator resetAnimatorX;
    private ValueAnimator resetAnimatorY;
    private int glareColor = Color.argb(80, 255, 255, 255);
    
    private boolean autoTiltEnabled = false;
    private float autoTime = 0f;

    public ThreeDCardView(Context context) {
        super(context);
        init();
    }

    public ThreeDCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ThreeDCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        setClickable(true);
        setFocusable(true);
    }

    public void setAutoTilt(boolean enabled) {
        this.autoTiltEnabled = enabled;
        if (enabled) {
            postInvalidateOnAnimation();
        } else {
            resetToCenter();
        }
    }

    public void setMaxTiltAngle(float angle) {
        this.maxTiltAngle = angle;
        invalidate();
    }

    public void setPerspectiveValue(float val) {
        this.perspectiveValue = val;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (autoTiltEnabled) return super.onTouchEvent(event);

        float x = event.getX();
        float y = event.getY();
        float width = getWidth();
        float height = getHeight();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                cancelResetAnimations();
                isPressed = true;
                // fall through
            case MotionEvent.ACTION_MOVE:
                touchX = x;
                touchY = y;
                
                float relativeX = (x / width) - 0.5f;
                float relativeY = (y / height) - 0.5f;

                if (relativeX < -0.5f) relativeX = -0.5f;
                if (relativeX > 0.5f) relativeX = 0.5f;
                if (relativeY < -0.5f) relativeY = -0.5f;
                if (relativeY > 0.5f) relativeY = 0.5f;

                targetRotationX = -relativeY * maxTiltAngle * 2.0f;
                targetRotationY = relativeX * maxTiltAngle * 2.0f;

                rotationX += (targetRotationX - rotationX) * 0.25f;
                rotationY += (targetRotationY - rotationY) * 0.25f;

                invalidate();
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isPressed = false;
                resetToCenter();
                break;
        }
        return true;
    }

    private void resetToCenter() {
        cancelResetAnimations();

        resetAnimatorX = ValueAnimator.ofFloat(rotationX, 0f);
        resetAnimatorX.setDuration(400);
        resetAnimatorX.setInterpolator(new DecelerateInterpolator());
        resetAnimatorX.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                rotationX = (float) animation.getAnimatedValue();
                invalidate();
            }
        });

        resetAnimatorY = ValueAnimator.ofFloat(rotationY, 0f);
        resetAnimatorY.setDuration(400);
        resetAnimatorY.setInterpolator(new DecelerateInterpolator());
        resetAnimatorY.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                rotationY = (float) animation.getAnimatedValue();
                invalidate();
            }
        });

        resetAnimatorX.start();
        resetAnimatorY.start();
        touchX = -1000f;
        touchY = -1000f;
    }

    private void cancelResetAnimations() {
        if (resetAnimatorX != null && resetAnimatorX.isRunning()) {
            resetAnimatorX.cancel();
        }
        if (resetAnimatorY != null && resetAnimatorY.isRunning()) {
            resetAnimatorY.cancel();
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (autoTiltEnabled) {
            autoTime += 0.03f;
            rotationX = (float) Math.sin(autoTime) * (maxTiltAngle * 0.7f);
            rotationY = (float) Math.cos(autoTime * 0.8f) * (maxTiltAngle * 0.7f);
            touchX = getWidth() / 2.0f + (float) Math.cos(autoTime * 0.8f) * (getWidth() * 0.35f);
            touchY = getHeight() / 2.0f + (float) Math.sin(autoTime) * (getHeight() * 0.35f);
        }

        canvas.save();
        camera.save();
        
        // standard default distance is -8. Scale with customizable perspective.
        camera.setLocation(0, 0, -perspectiveValue);

        camera.rotateX(rotationX);
        camera.rotateY(rotationY);
        camera.getMatrix(matrix);
        camera.restore();

        float centerX = getWidth() / 2.0f;
        float centerY = getHeight() / 2.0f;

        matrix.preTranslate(-centerX, -centerY);
        matrix.postTranslate(centerX, centerY);

        canvas.concat(matrix);

        super.dispatchDraw(canvas);

        if (isPressed || autoTiltEnabled) {
            float glX = touchX;
            float glY = touchY;
            if (glX > -500f) {
                RadialGradient gradient = new RadialGradient(
                        glX, glY,
                        Math.max(getWidth(), getHeight()) * 0.8f,
                        new int[]{glareColor, Color.TRANSPARENT},
                        new float[]{0.0f, 1.0f},
                        Shader.TileMode.CLAMP
                );
                glarePaint.setShader(gradient);
                glarePaint.setStyle(Paint.Style.FILL);
                canvas.drawRect(0, 0, getWidth(), getHeight(), glarePaint);
            }
        }

        canvas.restore();

        if (autoTiltEnabled) {
            postInvalidateOnAnimation();
        }
    }
}