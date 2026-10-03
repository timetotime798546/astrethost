package com.animationsandbox.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

public class InterpolatorGraphView extends View {
    private Interpolator mInterpolator = new LinearInterpolator();
    private String mName = "Linear";
    private float mProgress = -1f;

    private Paint mGridPaint;
    private Paint mCurvePaint;
    private Paint mMarkerPaint;
    private Paint mTextPaint;
    private Path mPath;

    public InterpolatorGraphView(Context context) {
        super(context);
        init();
    }

    public InterpolatorGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mGridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mGridPaint.setColor(Color.parseColor("#E0E0E0"));
        mGridPaint.setStyle(Paint.Style.STROKE);
        mGridPaint.setStrokeWidth(2f);

        mCurvePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCurvePaint.setColor(Color.parseColor("#2980B9"));
        mCurvePaint.setStyle(Paint.Style.STROKE);
        mCurvePaint.setStrokeWidth(6f);

        mMarkerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mMarkerPaint.setColor(Color.RED);
        mMarkerPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mMarkerPaint.setStrokeWidth(4f);

        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(Color.DKGRAY);
        mTextPaint.setTextSize(32f);

        mPath = new Path();
    }

    public void setInterpolator(Interpolator interpolator, String name) {
        mInterpolator = interpolator;
        mName = name;
        mProgress = -1f;
        invalidate();
    }

    public void setProgress(float progress) {
        mProgress = progress;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        float paddingX = 60f;
        float paddingY = 80f;
        float graphWidth = width - 2 * paddingX;
        float graphHeight = height - 2 * paddingY;

        // Draw bounding box
        canvas.drawRect(paddingX, paddingY, paddingX + graphWidth, paddingY + graphHeight, mGridPaint);

        // Draw reference grid guidelines (values 0.0, 0.5, 1.0)
        canvas.drawLine(paddingX, paddingY + graphHeight, paddingX + graphWidth, paddingY + graphHeight, mGridPaint); // Floor (0.0)
        canvas.drawLine(paddingX, paddingY + graphHeight / 2, paddingX + graphWidth, paddingY + graphHeight / 2, mGridPaint); // Mid (0.5)
        canvas.drawLine(paddingX, paddingY, paddingX + graphWidth, paddingY, mGridPaint); // Top (1.0)

        // Draw dynamic text indicators
        mTextPaint.setTextSize(26f);
        canvas.drawText("1.0", 12f, paddingY + 10f, mTextPaint);
        canvas.drawText("0.0", 12f, paddingY + graphHeight + 10f, mTextPaint);
        canvas.drawText("t=0", paddingX, paddingY + graphHeight + 35f, mTextPaint);
        canvas.drawText("t=1", paddingX + graphWidth - 45f, paddingY + graphHeight + 35f, mTextPaint);

        // Draw title
        mTextPaint.setTextSize(32f);
        mTextPaint.setFakeBoldText(true);
        canvas.drawText(mName + " Curve", paddingX, paddingY - 24f, mTextPaint);

        if (mInterpolator == null) return;

        // Trace interpolation path mathematically
        mPath.reset();
        int steps = 100;
        for (int i = 0; i <= steps; i++) {
            float t = (float) i / steps;
            float val = mInterpolator.getInterpolation(t);

            float x = paddingX + t * graphWidth;
            float y = (paddingY + graphHeight) - val * graphHeight;

            if (i == 0) {
                mPath.moveTo(x, y);
            } else {
                mPath.lineTo(x, y);
            }
        }
        canvas.drawPath(mPath, mCurvePaint);

        // Draw interactive progress sweep line
        if (mProgress >= 0f && mProgress <= 1.0f) {
            float markerX = paddingX + mProgress * graphWidth;
            float interpVal = mInterpolator.getInterpolation(mProgress);
            float markerY = (paddingY + graphHeight) - interpVal * graphHeight;

            // Draw full vertical reference line
            mMarkerPaint.setAlpha(80);
            canvas.drawLine(markerX, paddingY, markerX, paddingY + graphHeight, mMarkerPaint);

            // Draw coordinate bubble marker
            mMarkerPaint.setAlpha(255);
            canvas.drawCircle(markerX, markerY, 12f, mMarkerPaint);
        }
    }
}