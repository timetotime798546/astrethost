package com.cashsnap.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class PieChartView extends View {

    public static class Slice {
        public String category;
        public double value;
        public int color;

        public Slice(String category, double value, int color) {
            this.category = category;
            this.value = value;
            this.color = color;
        }
    }

    private List<Slice> slices = new ArrayList<>();
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RectF rectF = new RectF();

    public PieChartView(Context context) {
        super(context);
    }

    public PieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PieChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setSlices(List<Slice> slices) {
        this.slices = slices;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        if (slices == null || slices.isEmpty()) {
            paint.setColor(0xFFCCCCCC);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(12);
            int centerX = width / 2;
            int centerY = height / 3;
            int radius = Math.min(width, height) / 4;
            canvas.drawCircle(centerX, centerY, radius, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(36);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("No Expense Registered", centerX, centerY + 10, paint);
            return;
        }

        double total = 0;
        for (int i = 0; i < slices.size(); i++) {
            total += slices.get(i).value;
        }

        if (total == 0) return;

        int minSize = Math.min(width, height);
        int pieRadius = minSize / 3;
        int centerX = width / 2;
        int centerY = height / 3;

        rectF.set(centerX - pieRadius, centerY - pieRadius, centerX + pieRadius, centerY + pieRadius);

        float currentAngle = 0;
        paint.setStyle(Paint.Style.FILL);

        for (int i = 0; i < slices.size(); i++) {
            Slice slice = slices.get(i);
            float sweepAngle = (float) ((slice.value / total) * 360.0);
            paint.setColor(slice.color);
            canvas.drawArc(rectF, currentAngle, sweepAngle, true, paint);
            currentAngle += sweepAngle;
        }

        // Draw central donut hole for elegant dashboard UI design
        paint.setColor(0xFFFFFFFF);
        canvas.drawCircle(centerX, centerY, pieRadius / 2, paint);

        // Draw interactive list legends below the visual chart representation
        int startX = 40;
        int startY = (centerY + pieRadius) + 60;
        paint.setTextSize(34);
        paint.setTextAlign(Paint.Align.LEFT);

        for (int i = 0; i < slices.size(); i++) {
            Slice slice = slices.get(i);
            paint.setColor(slice.color);
            canvas.drawRect(startX, startY - 24, startX + 24, startY, paint);

            paint.setColor(0xFF222222);
            double percentage = (slice.value / total) * 100;
            String label = String.format("%s: $%.2f (%.1f%%)", slice.category, slice.value, percentage);
            canvas.drawText(label, startX + 44, startY, paint);

            startY += 54;
        }
    }
}