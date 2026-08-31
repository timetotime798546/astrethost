package com.omnitoolspremium.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

public class PaintActivity extends Activity {

    private PremiumDrawView drawView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paint);

        FrameLayout container = (FrameLayout) findViewById(R.id.container_canvas);
        drawView = new PremiumDrawView(this);
        container.addView(drawView);

        Button btnBlack = (Button) findViewById(R.id.btn_color_black);
        Button btnRed = (Button) findViewById(R.id.btn_color_red);
        Button btnBlue = (Button) findViewById(R.id.btn_color_blue);
        Button btnGreen = (Button) findViewById(R.id.btn_color_green);
        Button btnClear = (Button) findViewById(R.id.btn_clear_canvas);

        btnBlack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawView.setBrushColor(0xFF000000);
            }
        });

        btnRed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawView.setBrushColor(0xFFE74C3C);
            }
        });

        btnBlue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawView.setBrushColor(0xFF3498DB);
            }
        });

        btnGreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawView.setBrushColor(0xFF2ECC71);
            }
        });

        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawView.clearDrawing();
            }
        });
    }

    public static class PremiumDrawView extends View {
        private Paint paint;
        private Path path;

        public PremiumDrawView(Context context) {
            super(context);
            paint = new Paint();
            path = new Path();

            paint.setAntiAlias(true);
            paint.setStrokeWidth(12f);
            paint.setColor(0xFF000000);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeCap(Paint.Cap.ROUND);
        }

        public void setBrushColor(int color) {
            paint.setColor(color);
        }

        public void clearDrawing() {
            path.reset();
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawPath(path, paint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX();
            float y = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    path.moveTo(x, y);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    path.lineTo(x, y);
                    break;
                case MotionEvent.ACTION_UP:
                    break;
                default:
                    return false;
            }
            invalidate();
            return true;
        }
    }
}