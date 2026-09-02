package com.smartnotes.app;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;

public class ThreeDLinearLayout extends LinearLayout {
    private float maxRotation = 10f;

    public ThreeDLinearLayout(Context context) {
        super(context);
        init();
    }

    public ThreeDLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ThreeDLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setCameraDistance(getResources().getDisplayMetrics().density * 1500);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        handle3DTilt(ev);
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        handle3DTilt(event);
        return super.onTouchEvent(event);
    }

    private void handle3DTilt(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        float width = getWidth();
        float height = getHeight();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                if (width > 0 && height > 0) {
                    float rx = -((y - height / 2f) / (height / 2f)) * maxRotation;
                    float ry = ((x - width / 2f) / (width / 2f)) * maxRotation;
                    
                    if (rx > maxRotation) rx = maxRotation;
                    if (rx < -maxRotation) rx = -maxRotation;
                    if (ry > maxRotation) ry = maxRotation;
                    if (ry < -maxRotation) ry = -maxRotation;

                    setRotationX(rx);
                    setRotationY(ry);
                    setScaleX(0.97f);
                    setScaleY(0.97f);
                    setTranslationZ(12f);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                animate().rotationX(0).rotationY(0).scaleX(1.0f).scaleY(1.0f).translationZ(0).setDuration(150).start();
                break;
        }
    }
}