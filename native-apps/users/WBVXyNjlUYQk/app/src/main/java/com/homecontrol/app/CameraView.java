package com.homecontrol.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CameraView extends View {
    private Paint paint;
    private Paint textPaint;
    private float scanLineY = 0;
    private boolean scanDown = true;
    private boolean recLightOn = true;
    private long lastTimeUpdate = 0;

    public CameraView(Context context) {
        super(context);
        init();
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.GREEN);
        textPaint.setTextSize(36f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw dark feed background
        canvas.drawColor(Color.BLACK);

        int width = getWidth();
        int height = getHeight();

        // Draw camera crosshairs
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(2f);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(width * 0.2f, height * 0.2f, width * 0.8f, height * 0.8f, paint);
        
        // Horizontal center line segments
        canvas.drawLine(width * 0.45f, height / 2f, width * 0.55f, height / 2f, paint);
        // Vertical center line segments
        canvas.drawLine(width / 2f, height * 0.45f, width / 2f, height * 0.55f, paint);

        // Draw a simulated live scan line
        paint.setColor(Color.GREEN);
        paint.setAlpha(120);
        paint.setStrokeWidth(4f);
        canvas.drawLine(0, scanLineY, width, scanLineY, paint);

        // Update scan line position
        if (scanDown) {
            scanLineY += 5;
            if (scanLineY >= height) {
                scanDown = false;
            }
        } else {
            scanLineY -= 5;
            if (scanLineY <= 0) {
                scanDown = true;
            }
        }

        // Draw dynamic live time
        long now = System.currentTimeMillis();
        if (now - lastTimeUpdate > 1000) {
            recLightOn = !recLightOn;
            lastTimeUpdate = now;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        String liveTime = sdf.format(new Date(now));
        canvas.drawText("LIVE: " + liveTime, 30f, 60f, textPaint);

        // Draw REC blinking light
        if (recLightOn) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.RED);
            paint.setAlpha(255);
            canvas.drawCircle(width - 80f, 48f, 15f, paint);
            
            textPaint.setColor(Color.RED);
            canvas.drawText("REC", width - 150f, 60f, textPaint);
            textPaint.setColor(Color.GREEN);
        }

        // Keep redrawing for the animation effect
        postInvalidateDelayed(33); // approx 30 fps
    }
}