package com.smartbusinessmanager.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class CustomChartView extends View {
    private float[] salesData = new float[0];
    private float[] expensesData = new float[0];
    private String[] labels = new String[0];
    private Paint paintSales;
    private Paint paintExpenses;
    private Paint paintText;
    private Paint paintGrid;

    public CustomChartView(Context context) {
        super(context);
        init();
    }

    public CustomChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paintSales = new Paint();
        paintSales.setColor(0xFF2E7D32); // Deep Green
        paintSales.setStyle(Paint.Style.FILL);
        paintSales.setAntiAlias(true);

        paintExpenses = new Paint();
        paintExpenses.setColor(0xFFC62828); // Vibrant Red
        paintExpenses.setStyle(Paint.Style.FILL);
        paintExpenses.setAntiAlias(true);

        paintText = new Paint();
        paintText.setColor(Color.DKGRAY);
        paintText.setTextSize(22f);
        paintText.setAntiAlias(true);
        paintText.setTextAlign(Paint.Align.CENTER);

        paintGrid = new Paint();
        paintGrid.setColor(Color.LTGRAY);
        paintGrid.setStrokeWidth(2f);
        paintGrid.setStyle(Paint.Style.STROKE);
    }

    public void updateData(float[] sales, float[] expenses, String[] lbls) {
        this.salesData = sales;
        this.expensesData = expenses;
        this.labels = lbls;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();

        if (w == 0 || h == 0 || labels.length == 0) {
            canvas.drawText("No chart metrics loaded yet", w / 2.0f, h / 2.0f, paintText);
            return;
        }

        float pLeft = 80f;
        float pRight = 40f;
        float pTop = 30f;
        float pBottom = 50f;

        float chartW = w - pLeft - pRight;
        float chartH = h - pTop - pBottom;

        // Draw basic layout frame
        canvas.drawLine(pLeft, pTop, pLeft, pTop + chartH, paintGrid);
        canvas.drawLine(pLeft, pTop + chartH, pLeft + chartW, pTop + chartH, paintGrid);

        // Find scale limit
        float maxLimit = 100f;
        for (int i = 0; i < salesData.length; i++) {
            if (salesData[i] > maxLimit) maxLimit = salesData[i];
        }
        for (int i = 0; i < expensesData.length; i++) {
            if (expensesData[i] > maxLimit) maxLimit = expensesData[i];
        }
        maxLimit = maxLimit * 1.2f; // Add ceiling headroom

        int totalSlots = labels.length;
        float slotW = chartW / totalSlots;
        float maxBarW = slotW * 0.35f;

        for (int i = 0; i < totalSlots; i++) {
            float slotMiddleX = pLeft + (i * slotW) + (slotW / 2.0f);

            // Render Sales Bar
            float saleVal = i < salesData.length ? salesData[i] : 0f;
            float saleBarH = (saleVal / maxLimit) * chartH;
            float sL = slotMiddleX - maxBarW - 4;
            float sT = pTop + chartH - saleBarH;
            float sR = slotMiddleX - 4;
            float sB = pTop + chartH;
            canvas.drawRect(new RectF(sL, sT, sR, sB), paintSales);

            // Render Expenses Bar
            float expVal = i < expensesData.length ? expensesData[i] : 0f;
            float expBarH = (expVal / maxLimit) * chartH;
            float eL = slotMiddleX + 4;
            float eT = pTop + chartH - expBarH;
            float eR = slotMiddleX + maxBarW + 4;
            float eB = pTop + chartH;
            canvas.drawRect(new RectF(eL, eT, eR, eB), paintExpenses);

            // Label text rendering
            canvas.drawText(labels[i], slotMiddleX, pTop + chartH + 34f, paintText);

            // Show simple values values on top of bars
            if (saleVal > 0) {
                paintText.setTextSize(16f);
                canvas.drawText(String.valueOf((int)saleVal), slotMiddleX - maxBarW/2 - 4, sT - 6, paintText);
            }
            if (expVal > 0) {
                paintText.setTextSize(16f);
                canvas.drawText(String.valueOf((int)expVal), slotMiddleX + maxBarW/2 + 4, eT - 6, paintText);
            }
            paintText.setTextSize(22f); // Restore
        }
    }
}