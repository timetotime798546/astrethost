package com.zenhabit.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class CustomChartView extends View {
    private Paint paintBar;
    private Paint paintAxis;
    private Paint paintText;
    private List<Integer> dataPoints;

    public CustomChartView(Context context) {
        super(context);
        init();
    }

    public CustomChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paintBar = new Paint();
        paintBar.setColor(Color.parseColor("#00796B"));
        paintBar.setStyle(Paint.Style.FILL);

        paintAxis = new Paint();
        paintAxis.setColor(Color.DKGRAY);
        paintAxis.setStrokeWidth(5f);

        paintText = new Paint();
        paintText.setColor(Color.BLACK);
        paintText.setTextSize(32f);
        paintText.setAntiAlias(true);

        dataPoints = new ArrayList<>();
        // Seed standard initial items
        dataPoints.add(3);
        dataPoints.add(4);
        dataPoints.add(4);
        dataPoints.add(5);
        dataPoints.add(3);
    }

    public void setData(List<Integer> points) {
        this.dataPoints = points;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        int padding = 65;

        // Construct Axis representation lines
        canvas.drawLine(padding, padding, padding, height - padding, paintAxis);
        canvas.drawLine(padding, height - padding, width - padding, height - padding, paintAxis);

        if (dataPoints == null || dataPoints.isEmpty()) {
            canvas.drawText("No entry logs available.", width / 2f - 150, height / 2f, paintText);
            return;
        }

        int count = dataPoints.size();
        float colWidth = (width - 2 * padding) / (float) count;
        float maxVal = 5f;

        for (int i = 0; i < count; i++) {
            float val = dataPoints.get(i);
            float left = padding + (i * colWidth) + 18;
            float right = padding + ((i + 1) * colWidth) - 18;
            float barHeight = (val / maxVal) * (height - 2 * padding - 40);
            float top = height - padding - barHeight;

            canvas.drawRect(left, top, right, height - padding, paintBar);

            // Print scores and date references
            canvas.drawText(String.valueOf((int)val), left + (colWidth / 3.5f), top - 10, paintText);
            canvas.drawText("Log " + (i + 1), left + (colWidth / 12), height - padding + 40, paintText);
        }
    }
}