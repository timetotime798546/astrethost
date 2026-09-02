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
        bgPaint.setColor(0x00000000); // Fully transparent so parent layout gradient shines through!

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
        highlightPaint.setStrokeWidth(3.5f);
        highlightPaint.setStyle(Paint.Style.STROKE);

        buttons = new ArrayList<ThreeDButton>();
        
        // buttonType maps:
        // 1: Clear/Backspace (Hot dynamic rose gradient)
        // 2: Parentheses (Sleek slate-indigo gradient)
        // 3: Numbers (Luminous glossy silver-gray metal gradient)
        // 4: Operators (Hot synthwave neon orange-red gradient)
        // 5: Equal (Cyan-blue hyper space gradient)
        
        buttons.add(new ThreeDButton("C", 0, 0, 1));
        buttons.add(new ThreeDButton("←", 1, 0, 1));
        buttons.add(new ThreeDButton("(", 2, 0, 2));
        buttons.add(new ThreeDButton(")", 3, 0, 2));
        
        buttons.add(new ThreeDButton("7", 0, 1, 3));
        buttons.add(new ThreeDButton("8", 1, 1, 3));
        buttons.add(new ThreeDButton("9", 2, 1, 3));
        buttons.add(new ThreeDButton("/", 3, 1, 4));
        
        buttons.add(new ThreeDButton("4", 0, 2, 3));
        buttons.add(new ThreeDButton("5", 1, 2, 3));
        buttons.add(new ThreeDButton("6", 2, 2, 3));
        buttons.add(new ThreeDButton("*", 3, 2, 4));
        
        buttons.add(new ThreeDButton("1", 0, 3, 3));
        buttons.add(new ThreeDButton("2", 1, 3, 3));
        buttons.add(new ThreeDButton("3", 2, 3, 3));
        buttons.add(new ThreeDButton("-", 3, 3, 4));
        
        buttons.add(new ThreeDButton("0", 0, 4, 3));
        buttons.add(new ThreeDButton(".", 1, 4, 3));
        buttons.add(new ThreeDButton("=", 2, 4, 5));
        buttons.add(new ThreeDButton("+", 3, 4, 4));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
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

        shadowPaint.setColor(0x66000000);
        canvas.drawRect(left + 10, top + 10, right + 10, bottom + 10, shadowPaint);
    }

    private void drawButtonWalls(Canvas canvas, ThreeDButton btn) {
        float cellWidth = (getWidth() - padding * 2) / 4f;
        float cellHeight = (getHeight() - padding * 2) / 5f;
        
        float left = padding + btn.gridCol * cellWidth + cellGap;
        float top = padding + btn.gridRow * cellHeight + cellGap;
        float right = left + cellWidth - cellGap * 2;
        float bottom = top + cellHeight - cellGap * 2;

        float dx = -btn.currentZ * 0.42f;
        float dy = -btn.currentZ * 0.42f;

        float LT_x = left + dx;
        float LT_y = top + dy;
        float RT_x = right + dx;
        float RT_y = top + dy;
        float RB_x = right + dx;
        float RB_y = bottom + dy;
        float LB_x = left + dx;
        float LB_y = bottom + dy;

        // Right Wall with LinearGradient representation
        Path rightWall = new Path();
        rightWall.moveTo(right, top);
        rightWall.lineTo(right, bottom);
        rightWall.lineTo(RB_x, RB_y);
        rightWall.lineTo(RT_x, RT_y);
        rightWall.close();
        
        android.graphics.LinearGradient wallRightGrad = new android.graphics.LinearGradient(
            right, top, RB_x, RB_y,
            btn.wallColorRightStart, btn.wallColorRightEnd,
            android.graphics.Shader.TileMode.CLAMP
        );
        wallPaint.setShader(wallRightGrad);
        canvas.drawPath(rightWall, wallPaint);

        // Bottom Wall with LinearGradient representation
        Path bottomWall = new Path();
        bottomWall.moveTo(left, bottom);
        bottomWall.lineTo(right, bottom);
        bottomWall.lineTo(RB_x, RB_y);
        bottomWall.lineTo(LB_x, LB_y);
        bottomWall.close();
        
        android.graphics.LinearGradient wallBottomGrad = new android.graphics.LinearGradient(
            left, bottom, LB_x, LB_y,
            btn.wallColorBottomStart, btn.wallColorBottomEnd,
            android.graphics.Shader.TileMode.CLAMP
        );
        wallPaint.setShader(wallBottomGrad);
        canvas.drawPath(bottomWall, wallPaint);
        
        wallPaint.setShader(null); // clear
    }

    private void drawButtonTop(Canvas canvas, ThreeDButton btn) {
        float cellWidth = (getWidth() - padding * 2) / 4f;
        float cellHeight = (getHeight() - padding * 2) / 5f;
        
        float left = padding + btn.gridCol * cellWidth + cellGap;
        float top = padding + btn.gridRow * cellHeight + cellGap;
        float right = left + cellWidth - cellGap * 2;
        float bottom = top + cellHeight - cellGap * 2;

        float dx = -btn.currentZ * 0.42f;
        float dy = -btn.currentZ * 0.42f;

        float LT_x = left + dx;
        float LT_y = top + dy;
        float RT_x = right + dx;
        float RT_y = top + dy;
        float RB_x = right + dx;
        float RB_y = bottom + dy;
        float LB_x = left + dx;
        float LB_y = bottom + dy;

        // Top surface with linear gradients for breathtaking specular lighting
        android.graphics.LinearGradient topGrad = new android.graphics.LinearGradient(
            LT_x, LT_y, LT_x, RB_y,
            btn.colorStart, btn.colorEnd,
            android.graphics.Shader.TileMode.CLAMP
        );
        topPaint.setShader(topGrad);
        canvas.drawRect(LT_x, LT_y, RB_x, RB_y, topPaint);
        topPaint.setShader(null);

        // Highlighting borders to construct real 3D depth bevels
        highlightPaint.setColor(0x66FFFFFF);
        canvas.drawLine(LT_x, LT_y, RT_x, RT_y, highlightPaint);
        canvas.drawLine(LT_x, LT_y, LB_x, LB_y, highlightPaint);

        highlightPaint.setColor(0x40000000); 
        canvas.drawLine(LB_x, LB_y, RB_x, RB_y, highlightPaint);
        canvas.drawLine(RT_x, RT_y, RB_x, RB_y, highlightPaint);

        // Center Label Text
        float textX = (LT_x + RB_x) / 2f;
        float textY = ((LT_y + RB_y) / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f);
        
        textPaint.setColor(btn.textColor);
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
                        btn.targetZ = 3f;
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

    static class ThreeDButton {
        String label;
        float gridCol;
        float gridRow;
        
        int colorStart;
        int colorEnd;
        int wallColorRightStart;
        int wallColorRightEnd;
        int wallColorBottomStart;
        int wallColorBottomEnd;
        int textColor;
        
        float currentZ = 20f;
        float targetZ = 20f;
        boolean isPressed = false;

        ThreeDButton(String label, float col, float row, int type) {
            this.label = label;
            this.gridCol = col;
            this.gridRow = row;
            
            if (type == 1) {
                // Neon Hot Rose Gradient
                this.colorStart = 0xFFFF416C;
                this.colorEnd = 0xFFFF4B2B;
                this.wallColorRightStart = 0xFFD82850;
                this.wallColorRightEnd = 0xFFB31B3C;
                this.wallColorBottomStart = 0xFFB31B3C;
                this.wallColorBottomEnd = 0xFF800E23;
                this.textColor = 0xFFFFFFFF;
            } else if (type == 2) {
                // Luminous Slate Indigo
                this.colorStart = 0xFF5F6E8F;
                this.colorEnd = 0xFF3D465C;
                this.wallColorRightStart = 0xFF31384A;
                this.wallColorRightEnd = 0xFF242938;
                this.wallColorBottomStart = 0xFF242938;
                this.wallColorBottomEnd = 0xFF151821;
                this.textColor = 0xFFFFFFFF;
            } else if (type == 3) {
                // Platinum Silver Metallic
                this.colorStart = 0xFFFFFFFF;
                this.colorEnd = 0xFFCCD5E4;
                this.wallColorRightStart = 0xFFAFBACF;
                this.wallColorRightEnd = 0xFF8B98B0;
                this.wallColorBottomStart = 0xFF8B98B0;
                this.wallColorBottomEnd = 0xFF657187;
                this.textColor = 0xFF15181F;
            } else if (type == 4) {
                // Hot Retro Cyber Orange-Red Gradient
                this.colorStart = 0xFFFF9F0A;
                this.colorEnd = 0xFFFF375F;
                this.wallColorRightStart = 0xFFDB2D4E;
                this.wallColorRightEnd = 0xFFB01D38;
                this.wallColorBottomStart = 0xFFB01D38;
                this.wallColorBottomEnd = 0xFF7D0E20;
                this.textColor = 0xFFFFFFFF;
            } else {
                // Electric Glowing Space Cyan Gradient
                this.colorStart = 0xFF00FFCC;
                this.colorEnd = 0xFF00A3FF;
                this.wallColorRightStart = 0xFF0086CE;
                this.wallColorRightEnd = 0xFF0067A3;
                this.wallColorBottomStart = 0xFF0067A3;
                this.wallColorBottomEnd = 0xFF004570;
                this.textColor = 0xFF15181F;
            }
        }

        void update() {
            if (currentZ != targetZ) {
                currentZ += (targetZ - currentZ) * 0.38f;
                if (Math.abs(currentZ - targetZ) < 0.2f) {
                    currentZ = targetZ;
                }
            }
        }
    }
}