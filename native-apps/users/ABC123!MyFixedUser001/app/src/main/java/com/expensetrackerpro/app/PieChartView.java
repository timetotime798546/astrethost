package com.expensetrackerpro.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class PieChartView extends View {

    public static class Slice {
        public String label;
        public float value;
        public int color;

        public Slice(String label, float value, int color) {
            this.label = label;
            this.value = value;
            this.color = color;
        }
    }

    private List<Slice> slices = new ArrayList<>();
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RectF rectF = new RectF();
    private Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public PieChartView(Context context) {
        super(context);
        init();
    }

    public PieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint.setStyle(Paint.Style.FILL);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(36f);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setSlices(List<Slice> slices) {
        this.slices = slices;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();
        float radius = Math.min(width, height) * 0.38f;
        rectF.set(width / 2 - radius, height / 2 - radius, width / 2 + radius, height / 2 + radius);

        if (slices == null || slices.isEmpty()) {
            // Empty graphic states representation
            paint.setColor(Color.LTGRAY);
            canvas.drawArc(rectF, 0, 360, true, paint);

            // Punch hole in middle
            paint.setColor(Color.WHITE);
            canvas.drawCircle(width / 2, height / 2, radius * 0.62f, paint);

            textPaint.setColor(Color.GRAY);
            textPaint.setTextSize(32f);
            canvas.drawText("No Data", width / 2, height / 2 + 10, textPaint);
            return;
        }

        float total = 0;
        for (int i = 0; i < slices.size(); i++) {
            total += slices.get(i).value;
        }

        if (total == 0) {
            paint.setColor(Color.LTGRAY);
            canvas.drawArc(rectF, 0, 360, true, paint);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(width / 2, height / 2, radius * 0.62f, paint);
            textPaint.setColor(Color.GRAY);
            textPaint.setTextSize(32f);
            canvas.drawText("No Expenses", width / 2, height / 2 + 10, textPaint);
            return;
        }

        float startAngle = -90f;
        for (int i = 0; i < slices.size(); i++) {
            Slice slice = slices.get(i);
            if (slice.value <= 0) continue;
            float sweepAngle = (slice.value / total) * 360f;
            paint.setColor(slice.color);
            canvas.drawArc(rectF, startAngle, sweepAngle, true, paint);
            startAngle += sweepAngle;
        }

        // Draw middle donut spacer cutout
        paint.setColor(Color.WHITE);
        canvas.drawCircle(width / 2, height / 2, radius * 0.62f, paint);

        // Center statistics overlay
        textPaint.setColor(Color.GRAY);
        textPaint.setTextSize(28f);
        canvas.drawText("Expenses", width / 2, height / 2 - 14, textPaint);
        
        textPaint.setTextSize(36f);
        textPaint.setColor(Color.BLACK);
        textPaint.setFakeBoldText(true);
        canvas.drawText(String.format("$%.2f", total), width / 2, height / 2 + 24, textPaint);
        textPaint.setFakeBoldText(false);
    }
}