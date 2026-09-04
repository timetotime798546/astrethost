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
        invalidate(); // request layout redraw
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (dataList == null || dataList.isEmpty()) {
            paint.setColor(Color.parseColor("#7F8C8D"));
            paint.setTextSize(36);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("No charts generated (Expenses Empty)", getWidth() / 2, getHeight() / 2, paint);
            return;
        }

        double total = 0;
        for (int i = 0; i < dataList.size(); i++) {
            total += dataList.get(i).value;
        }

        if (total == 0) return;

        int size = Math.min(getWidth(), getHeight()) - 160;
        if (size <= 0) return;

        int left = (getWidth() - size) / 2;
        int top = 30;
        rectF.set(left, top, left + size, top + size);

        float startAngle = 0;
        for (int i = 0; i < dataList.size(); i++) {
            ChartData data = dataList.get(i);
            float sweepAngle = (float) ((data.value / total) * 360.0);
            
            // Draw Pie Piece
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(data.color);
            canvas.drawArc(rectF, startAngle, sweepAngle, true, paint);

            // Draw Clean Pie Divider Border
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.WHITE);
            paint.setStrokeWidth(5);
            canvas.drawArc(rectF, startAngle, sweepAngle, true, paint);

            startAngle += sweepAngle;
        }

        // Draw inner white circle representing a modern donut design
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(getWidth() / 2, top + size / 2, size / 3, paint);

        // Draw dynamic legend layout lines manually beneath the canvas plot
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(26);
        paint.setStrokeWidth(0);

        float legendY = top + size + 45;
        float legendX = 40;
        for (int i = 0; i < dataList.size(); i++) {
            ChartData data = dataList.get(i);
            
            // Draw visual representation dot
            paint.setColor(data.color);
            canvas.drawCircle(legendX + 15, legendY - 10, 12, paint);

            // Draw category details label texts
            paint.setColor(Color.parseColor("#2C3E50"));
            paint.setTextAlign(Paint.Align.LEFT);
            String labelDetail = data.label + " (" + String.format("%.1f%%", (data.value / total) * 100) + ")";
            canvas.drawText(labelDetail, legendX + 35, legendY, paint);

            legendX += 280;
            if (legendX > getWidth() - 250) {
                legendX = 40;
                legendY += 45;
            }
        }
    }
}