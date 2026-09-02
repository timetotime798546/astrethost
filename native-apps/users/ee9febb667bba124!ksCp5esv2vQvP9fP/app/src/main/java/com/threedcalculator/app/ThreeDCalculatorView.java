package com.threedcalculator.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class ThreeDCalculatorView extends View {

    public interface OnButtonClickListener {
        void onButtonClick(String label);
    }

    private OnButtonClickListener clickListener;
    private List<ThreeDButton> buttons;
    
    private Paint bgPaint;
    private Paint shadowPaint;
    private Paint wallPaint;
    private Paint topPaint;
    private Paint textPaint;
    private Paint highlightPaint;
    
    private float padding = 20f;
    private float cellGap = 16f;
    
    public ThreeDCalculatorView(Context context) {
        super(context);
        init();
    }

    public ThreeDCalculatorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setOnButtonClickListener(OnButtonClickListener listener) {
        this.clickListener = listener;
    }

    private void init() {
        bgPaint = new Paint();
        bgPaint.setColor(0xFF1E222B);

        shadowPaint = new Paint();
        shadowPaint.setAntiAlias(true);

        wallPaint = new Paint();
        wallPaint.setAntiAlias(true);
        wallPaint.setStyle(Paint.Style.FILL);

        topPaint = new Paint();
        topPaint.setAntiAlias(true);
        topPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(54f);
        textPaint.setFakeBoldText(true);

        highlightPaint = new Paint();
        highlightPaint.setAntiAlias(true);
        highlightPaint.setStrokeWidth(3f);
        highlightPaint.setStyle(Paint.Style.STROKE);

        buttons = new ArrayList<ThreeDButton>();
        
        buttons.add(new ThreeDButton("C", 0, 0, 0xFFFF3B30));
        buttons.add(new ThreeDButton("←", 1, 0, 0xFFFF3B30));
        buttons.add(new ThreeDButton("(", 2, 0, 0xFF4E5564));
        buttons.add(new ThreeDButton(")", 3, 0, 0xFF4E5564));
        
        buttons.add(new ThreeDButton("7", 0, 1, 0xFFEBEBEB));
        buttons.add(new ThreeDButton("8", 1, 1, 0xFFEBEBEB));
        buttons.add(new ThreeDButton("9", 2, 1, 0xFFEBEBEB));
        buttons.add(new ThreeDButton("/", 3, 1, 0xFFFF9500));
        
        buttons.add(new ThreeDButton("4", 0, 2, 0xFFEBEBEB));
        buttons.add(new ThreeDButton("5", 1, 2, 0xFFEBEBEB));
        buttons.add(new ThreeDButton("6", 2, 2, 0xFFEBEBEB));
        buttons.add(new ThreeDButton("*", 3, 2, 0xFFFF9500));
        
        buttons.add(new ThreeDButton("1", 0, 3, 0xFFEBEBEB));
        buttons.add(new ThreeDButton("2", 1, 3, 0xFFEBEBEB));
        buttons.add(new ThreeDButton("3", 2, 3, 0xFFEBEBEB));
        buttons.add(new ThreeDButton("-", 3, 3, 0xFFFF9500));
        
        buttons.add(new ThreeDButton("0", 0, 4, 0xFFEBEBEB));
        buttons.add(new ThreeDButton(".", 1, 4, 0xFFEBEBEB));
        buttons.add(new ThreeDButton("=", 2, 4, 0xFFFFCC00));
        buttons.add(new ThreeDButton("+", 3, 4, 0xFFFF9500));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        canvas.drawRect(0, 0, getWidth(), getHeight(), bgPaint);
        boolean needsInvalidate = false;
        
        for (int i = 0; i < buttons.size(); i++) {
            ThreeDButton btn = buttons.get(i);
            btn.update();
            if (btn.currentZ != btn.targetZ) {
                needsInvalidate = true;
            }
            drawButtonShadow(canvas, btn);
        }
        
        for (int i = 0; i < buttons.size(); i++) {
            drawButtonWalls(canvas, buttons.get(i));
        }
        
        for (int i = 0; i < buttons.size(); i++) {
            drawButtonTop(canvas, buttons.get(i));
        }
        
        if (needsInvalidate) {
            postInvalidateOnAnimation();
        }
    }

    private void drawButtonShadow(Canvas canvas, ThreeDButton btn) {
        float cellWidth = (getWidth() - padding * 2) / 4f;
        float cellHeight = (getHeight() - padding * 2) / 5f;
        
        float left = padding + btn.gridCol * cellWidth + cellGap;
        float top = padding + btn.gridRow * cellHeight + cellGap;
        float right = left + cellWidth - cellGap * 2;
        float bottom = top + cellHeight - cellGap * 2;

        shadowPaint.setColor(0x55000000);
        canvas.drawRect(left + 8, top + 8, right + 8, bottom + 8, shadowPaint);
    }

    private void drawButtonWalls(Canvas canvas, ThreeDButton btn) {
        float cellWidth = (getWidth() - padding * 2) / 4f;
        float cellHeight = (getHeight() - padding * 2) / 5f;
        
        float left = padding + btn.gridCol * cellWidth + cellGap;
        float top = padding + btn.gridRow * cellHeight + cellGap;
        float right = left + cellWidth - cellGap * 2;
        float bottom = top + cellHeight - cellGap * 2;

        float dx = -btn.currentZ * 0.4f;
        float dy = -btn.currentZ * 0.4f;

        float LT_x = left + dx;
        float LT_y = top + dy;
        float RT_x = right + dx;
        float RT_y = top + dy;
        float RB_x = right + dx;
        float RB_y = bottom + dy;
        float LB_x = left + dx;
        float LB_y = bottom + dy;

        Path rightWall = new Path();
        rightWall.moveTo(right, top);
        rightWall.lineTo(right, bottom);
        rightWall.lineTo(RB_x, RB_y);
        rightWall.lineTo(RT_x, RT_y);
        rightWall.close();
        wallPaint.setColor(btn.wallColorRight);
        canvas.drawPath(rightWall, wallPaint);

        Path bottomWall = new Path();
        bottomWall.moveTo(left, bottom);
        bottomWall.lineTo(right, bottom);
        bottomWall.lineTo(RB_x, RB_y);
        bottomWall.lineTo(LB_x, LB_y);
        bottomWall.close();
        wallPaint.setColor(btn.wallColorBottom);
        canvas.drawPath(bottomWall, wallPaint);
    }

    private void drawButtonTop(Canvas canvas, ThreeDButton btn) {
        float cellWidth = (getWidth() - padding * 2) / 4f;
        float cellHeight = (getHeight() - padding * 2) / 5f;
        
        float left = padding + btn.gridCol * cellWidth + cellGap;
        float top = padding + btn.gridRow * cellHeight + cellGap;
        float right = left + cellWidth - cellGap * 2;
        float bottom = top + cellHeight - cellGap * 2;

        float dx = -btn.currentZ * 0.4f;
        float dy = -btn.currentZ * 0.4f;

        float LT_x = left + dx;
        float LT_y = top + dy;
        float RT_x = right + dx;
        float RT_y = top + dy;
        float RB_x = right + dx;
        float RB_y = bottom + dy;
        float LB_x = left + dx;
        float LB_y = bottom + dy;

        topPaint.setColor(btn.normalColor);
        canvas.drawRect(LT_x, LT_y, RB_x, RB_y, topPaint);

        highlightPaint.setColor(0x44FFFFFF);
        canvas.drawLine(LT_x, LT_y, RT_x, RT_y, highlightPaint);
        canvas.drawLine(LT_x, LT_y, LB_x, LB_y, highlightPaint);

        highlightPaint.setColor(0x22000000);
        canvas.drawLine(LB_x, LB_y, RB_x, RB_y, highlightPaint);
        canvas.drawLine(RT_x, RT_y, RB_x, RB_y, highlightPaint);

        float textX = (LT_x + RB_x) / 2f;
        float textY = ((LT_y + RB_y) / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f);
        
        if (btn.normalColor == 0xFFEBEBEB) {
            textPaint.setColor(0xFF1E222B);
        } else {
            textPaint.setColor(0xFFFFFFFF);
        }
        
        canvas.drawText(btn.label, textX, textY, textPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        float x = event.getX();
        float y = event.getY();

        float cellWidth = (getWidth() - padding * 2) / 4f;
        float cellHeight = (getHeight() - padding * 2) / 5f;

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                for (int i = 0; i < buttons.size(); i++) {
                    ThreeDButton btn = buttons.get(i);
                    float left = padding + btn.gridCol * cellWidth + cellGap;
                    float top = padding + btn.gridRow * cellHeight + cellGap;
                    float right = left + cellWidth - cellGap * 2;
                    float bottom = top + cellHeight - cellGap * 2;

                    if (x >= left && x <= right && y >= top && y <= bottom) {
                        btn.targetZ = 4f;
                        btn.isPressed = true;
                        
                        if (btn.label.equals("=")) {
                            SoundSynth.playEqualClick();
                        } else if (btn.label.equals("C") || btn.label.equals("←")) {
                            SoundSynth.playOperatorClick();
                        } else if ("+-*/()".contains(btn.label)) {
                            SoundSynth.playOperatorClick();
                        } else {
                            SoundSynth.playClick();
                        }

                        if (clickListener != null) {
                            clickListener.onButtonClick(btn.label);
                        }
                        invalidate();
                        break;
                    }
                }
                return true;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_POINTER_UP: {
                for (int i = 0; i < buttons.size(); i++) {
                    ThreeDButton btn = buttons.get(i);
                    if (btn.isPressed) {
                        btn.targetZ = 20f;
                        btn.isPressed = false;
                    }
                }
                invalidate();
                return true; 
            }
        }
        return super.onTouchEvent(event);
    }

    private static int adjustColorBrightness(int color, float factor) {
        int r = (int) (Color.red(color) * factor);
        int g = (int) (Color.green(color) * factor);
        int b = (int) (Color.blue(color) * factor);
        return Color.rgb(Math.min(255, r), Math.min(255, g), Math.min(255, b));
    }

    static class ThreeDButton {
        String label;
        float gridCol;
        float gridRow;
        int normalColor;
        int wallColorRight;
        int wallColorBottom;
        
        float currentZ = 20f;
        float targetZ = 20f;
        boolean isPressed = false;

        ThreeDButton(String label, float col, float row, int color) {
            this.label = label;
            this.gridCol = col;
            this.gridRow = row;
            this.normalColor = color;
            this.wallColorRight = adjustColorBrightness(color, 0.7f);
            this.wallColorBottom = adjustColorBrightness(color, 0.45f);
        }

        void update() {
            if (currentZ != targetZ) {
                currentZ += (targetZ - currentZ) * 0.35f;
                if (Math.abs(currentZ - targetZ) < 0.2f) {
                    currentZ = targetZ;
                }
            }
        }
    }
}