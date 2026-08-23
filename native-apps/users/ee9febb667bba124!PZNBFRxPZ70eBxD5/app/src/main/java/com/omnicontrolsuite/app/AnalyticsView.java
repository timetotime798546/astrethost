package com.omnicontrolsuite.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.util.Random;

public class AnalyticsView extends View {
    private Paint gridPaint;
    private Paint linePaint;
    private Paint dotPaint;
    private Paint textPaint;
    private int[] dataPoints;
    private static final int MAX_POINTS = 8;

    public AnalyticsView(Context context) {
        super(context);
        init();
    }

    public AnalyticsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AnalyticsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        gridPaint = new Paint();
        gridPaint.setColor(Color.parseColor("#E0E0E0"));
        gridPaint.setStrokeWidth(2f);
        gridPaint.setStyle(Paint.Style.STROKE);

        linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#3F51B5"));
        linePaint.setStrokeWidth(5f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setAntiAlias(true);

        dotPaint = new Paint();
        dotPaint.setColor(Color.parseColor("#009688"));
        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(Color.parseColor("#757575"));
        textPaint.setTextSize(24f);
        textPaint.setAntiAlias(true);

        generateRandomData();
    }

    public void generateRandomData() {
        dataPoints = new int[MAX_POINTS];
        Random r = new Random();
        for (int i = 0; i < MAX_POINTS; i++) { 
            dataPoints[i] = r.nextInt(100);
        }
        invalidate();
    }

    public void clearData() {
        dataPoints = new int[MAX_POINTS];
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int paddingLeft = 50;
        int paddingTop = 30;
        int paddingRight = 30;
        int paddingBottom = 40;

        int graphWidth = width - paddingLeft - paddingRight;
        int graphHeight = height - paddingTop - paddingBottom;

        // Draw axes outline grid background
        canvas.drawRect(paddingLeft, paddingTop, width - paddingRight, height - paddingBottom, gridPaint);

        // Draw parallel horizontal grid lines
        int gridLines = 4;
        for (int i = 1; i < gridLines; i++) {
            float y = paddingTop + (graphHeight / (float) gridLines) * i;
            canvas.drawLine(paddingLeft, y, width - paddingRight, y, gridPaint);
        }

        // Plot line graph
        if (dataPoints != null && dataPoints.length > 0) {
            float segmentWidth = graphWidth / (float) (MAX_POINTS - 1);

            for (int i = 0; i < MAX_POINTS - 1; i++) {
                float startX = paddingLeft + (i * segmentWidth);
                float startY = (height - paddingBottom) - (graphHeight * (dataPoints[i] / 100f));

                float stopX = paddingLeft + ((i + 1) * segmentWidth);
                float stopY = (height - paddingBottom) - (graphHeight * (dataPoints[i + 1] / 100f));

                canvas.drawLine(startX, startY, stopX, stopY, linePaint);
                canvas.drawCircle(startX, startY, 8f, dotPaint);
                if (i == MAX_POINTS - 2) {
                    canvas.drawCircle(stopX, stopY, 8f, dotPaint);
                }
            }
        }

        // Draw bottom metric axis labels
        canvas.drawText("T1", paddingLeft, height - 10, textPaint);
        canvas.drawText("T4", paddingLeft + graphWidth / 2, height - 10, textPaint);
        canvas.drawText("T8", width - paddingRight - 30, height - 10, textPaint);
    }
}
