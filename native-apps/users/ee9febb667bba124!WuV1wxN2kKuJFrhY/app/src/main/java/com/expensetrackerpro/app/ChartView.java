package com.expensetrackerpro.app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import java.util.ArrayList;
import java.util.List;

public class ChartView extends View {
    public static class ChartData {
        public String label;
        public double value;
        public int color;

        public ChartData(String label, double value, int color) {
            this.label = label;
            this.value = value;
            this.color = color;
        }
    }

    private List<ChartData> dataList = new ArrayList<>();
    private Paint paint;
    private RectF rectF;
    private float animationProgress = 0f;

    public ChartView(Context context) {
        super(context);
        init();
    }

    public ChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        rectF = new RectF();
    }

    public void setData(List<ChartData> data) {
        this.dataList = data;
        
        // Premium dynamic entrance animator for pie slices (fully compatible with SDK 21)
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(900);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                animationProgress = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (dataList == null || dataList.isEmpty()) {
            paint.setColor(Color.parseColor("#64748B"));
            paint.setTextSize(36);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setShadowLayer(0, 0, 0, 0);
            canvas.drawText("No expenses registered yet", getWidth() / 2, getHeight() / 2, paint);
            return;
        }

        double total = 0;
        for (int i = 0; i < dataList.size(); i++) {
            total += dataList.get(i).value;
        }

        if (total == 0) return;

        int size = Math.min(getWidth(), getHeight()) - 180;
        if (size <= 0) return;

        int left = (getWidth() - size) / 2;
        int top = 40;
        rectF.set(left, top, left + size, top + size);

        // Elevated 3D shadow depth effect
        paint.setShadowLayer(14, 0, 8, Color.parseColor("#260F172A"));

        float startAngle = -90f; // Standard clean top orientation starting angle
        for (int i = 0; i < dataList.size(); i++) {
            ChartData data = dataList.get(i);
            float sweepAngle = (float) ((data.value / total) * 360.0) * animationProgress;
            
            // Draw Pie Segment
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(data.color);
            canvas.drawArc(rectF, startAngle, sweepAngle, true, paint);

            // Draw Premium white outer boundary division strokes
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.WHITE);
            paint.setStrokeWidth(5);
            canvas.drawArc(rectF, startAngle, sweepAngle, true, paint);

            startAngle += (float) ((data.value / total) * 360.0);
        }

        // Disable shadow layer to ensure perfectly clean donut chart overlay details
        paint.setShadowLayer(0, 0, 0, 0);

        // Draw center white circular cutout representing modern visual analytics design
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(getWidth() / 2, top + size / 2, size / 3, paint);

        // Draw Dynamic Legends layout
        paint.setTextSize(24);
        paint.setStrokeWidth(0);

        float legendY = top + size + 45;
        float legendX = 40;
        for (int i = 0; i < dataList.size(); i++) {
            ChartData data = dataList.get(i);
            
            paint.setColor(data.color);
            canvas.drawCircle(legendX + 12, legendY - 8, 10, paint);

            paint.setColor(Color.parseColor("#334155"));
            paint.setTextAlign(Paint.Align.LEFT);
            String labelDetail = data.label + " (" + String.format("%.1f%%", (data.value / total) * 100) + ")";
            canvas.drawText(labelDetail, legendX + 28, legendY, paint);

            legendX += 250;
            if (legendX > getWidth() - 220) {
                legendX = 40;
                legendY += 38;
            }
        }
    }
}