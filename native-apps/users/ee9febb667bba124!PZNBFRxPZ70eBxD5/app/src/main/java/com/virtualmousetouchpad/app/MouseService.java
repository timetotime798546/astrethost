package com.virtualmousetouchpad.app;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MouseService extends AccessibilityService implements SharedPreferences.OnSharedPreferenceChangeListener {

    private WindowManager windowManager;
    private View touchpadView;
    private View bubbleView;
    private CursorView cursorView;

    private WindowManager.LayoutParams touchpadParams;
    private WindowManager.LayoutParams bubbleParams;
    private WindowManager.LayoutParams cursorParams;

    private boolean isTouchpadAdded = false;
    private boolean isBubbleAdded = false;
    private boolean isCursorAdded = false;

    private int screenWidth;
    private int screenHeight;

    private float cursorX;
    private float cursorY;
    private float sensitivity = 1.5f;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        getScreenSize();

        // Start Cursor in the middle
        cursorX = screenWidth / 2f;
        cursorY = screenHeight / 2f;

        createViews();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
        loadPreferences(prefs);
    }

    private void getScreenSize() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;
    }

    private void loadPreferences(SharedPreferences prefs) {
        sensitivity = prefs.getFloat("pref_sensitivity", 1.5f);
        updateTouchpadSize();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if ("pref_sensitivity".equals(key)) {
            sensitivity = prefs.getFloat("pref_sensitivity", 1.5f);
        } else if ("pref_touchpad_size".equals(key)) {
            updateTouchpadSize();
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void updateTouchpadSize() {
        if (touchpadView == null || touchpadParams == null) return;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int sizeDp = prefs.getInt("pref_touchpad_size", 220);
        int widthPx = dpToPx(sizeDp);
        int heightPx = dpToPx((int) (sizeDp * 0.75f)); // Keep 4:3 aspect ratio

        touchpadParams.width = widthPx;
        touchpadParams.height = heightPx;

        if (isTouchpadAdded) {
            try {
                windowManager.updateViewLayout(touchpadView, touchpadParams);
            } catch (Exception e) {
                // Safe check
            }
        }
    }

    private void createViews() {
        LayoutInflater inflater = LayoutInflater.from(this);

        // --- TOUCHPAD LAYOUT ---
        touchpadView = inflater.inflate(R.layout.layout_touchpad, null);

        int layoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        touchpadParams = new WindowManager.LayoutParams(
                dpToPx(220),
                dpToPx(165),
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        touchpadParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        touchpadParams.y = dpToPx(50); // initial offset from bottom

        // --- BUBBLE LAYOUT ---
        bubbleView = inflater.inflate(R.layout.layout_bubble, null);
        bubbleParams = new WindowManager.LayoutParams(
                dpToPx(56),
                dpToPx(56),
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        bubbleParams.gravity = Gravity.TOP | Gravity.LEFT;
        bubbleParams.x = screenWidth - dpToPx(70);
        bubbleParams.y = screenHeight / 2;

        // --- CURSOR LAYOUT ---
        cursorView = new CursorView(this);
        cursorParams = new WindowManager.LayoutParams(
                dpToPx(40),
                dpToPx(40),
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
        );
        cursorParams.gravity = Gravity.TOP | Gravity.LEFT;
        cursorParams.x = (int) cursorX;
        cursorParams.y = (int) cursorY;

        setupListeners();
    }

    private void setupListeners() {
        // Handle dragging/moving the Touchpad itself
        View headerBar = touchpadView.findViewById(R.id.header_bar);
        headerBar.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = touchpadParams.x;
                        initialY = touchpadParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        touchpadParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                        touchpadParams.y = initialY - (int) (event.getRawY() - initialTouchY); // Y is reversed in BOTTOM gravity
                        // Clamp touchpad positions safely
                        try {
                            windowManager.updateViewLayout(touchpadView, touchpadParams);
                        } catch (Exception e) {}
                        return true;
                }
                return false;
            }
        });

        // Touchpad Minimize button
        Button btnMinimize = (Button) touchpadView.findViewById(R.id.btn_minimize);
        btnMinimize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showBubble();
            }
        });

        // Touchpad Cursor Moving Area
        View touchPadArea = touchpadView.findViewById(R.id.touch_pad_area);
        touchPadArea.setOnTouchListener(new View.OnTouchListener() {
            private float lastX;
            private float lastY;
            private boolean isClick;
            private long startTime;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastX = event.getRawX();
                        lastY = event.getRawY();
                        isClick = true;
                        startTime = System.currentTimeMillis();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - lastX;
                        float dy = event.getRawY() - lastY;

                        if (Math.abs(dx) > 6 || Math.abs(dy) > 6) {
                            isClick = false;
                        }

                        // Apply movement delta with sensitivity
                        cursorX += dx * sensitivity;
                        cursorY += dy * sensitivity;

                        // Clamp within boundaries
                        if (cursorX < 0) cursorX = 0;
                        if (cursorX > screenWidth) cursorX = screenWidth;
                        if (cursorY < 0) cursorY = 0;
                        if (cursorY > screenHeight) cursorY = screenHeight;

                        cursorParams.x = (int) cursorX;
                        cursorParams.y = (int) cursorY;

                        if (isCursorAdded) {
                            try {
                                windowManager.updateViewLayout(cursorView, cursorParams);
                            } catch (Exception e) {}
                        }

                        lastX = event.getRawX();
                        lastY = event.getRawY();
                        break;
                    case MotionEvent.ACTION_UP:
                        long duration = System.currentTimeMillis() - startTime;
                        if (isClick && duration < 200) {
                            performClickAtCursor();
                        }
                        break;
                }
                return true;
            }
        });

        // Scroll Area logic
        View scrollArea = touchpadView.findViewById(R.id.scroll_area);
        scrollArea.setOnTouchListener(new View.OnTouchListener() {
            private float lastY;
            private long lastScrollTime = 0;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastY = event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float dy = event.getRawY() - lastY;
                        long currentTime = System.currentTimeMillis();
                        // Control scrolling speed limit
                        if (Math.abs(dy) > 25 && (currentTime - lastScrollTime > 180)) {
                            performScroll(dy > 0);
                            lastY = event.getRawY();
                            lastScrollTime = currentTime;
                        }
                        break;
                }
                return true;
            }
        });

        // Left Click Button
        Button btnLeftClick = (Button) touchpadView.findViewById(R.id.btn_left_click);
        btnLeftClick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performClickAtCursor();
            }
        });

        // Right Click (Simulate Android Back gesture/button)
        Button btnRightClick = (Button) touchpadView.findViewById(R.id.btn_right_click);
        btnRightClick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performGlobalAction(GLOBAL_ACTION_BACK);
            }
        });

        // Drag & Click listener for Bubble overlay
        bubbleView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            private boolean isClick;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = bubbleParams.x;
                        initialY = bubbleParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        isClick = true;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - initialTouchX;
                        float dy = event.getRawY() - initialTouchY;
                        if (Math.abs(dx) > 6 || Math.abs(dy) > 6) {
                            isClick = false;
                        }
                        bubbleParams.x = initialX + (int) dx;
                        bubbleParams.y = initialY + (int) dy;
                        try {
                            windowManager.updateViewLayout(bubbleView, bubbleParams);
                        } catch (Exception e) {}
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (isClick) {
                            showTouchpad();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void performClickAtCursor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            GestureDescription.Builder builder = new GestureDescription.Builder();
            Path path = new Path();
            path.moveTo(cursorX, cursorY);
            builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 80));
            dispatchGesture(builder.build(), null, null);
        } else {
            Toast.makeText(this, "Click simulation requires API 24+", Toast.LENGTH_SHORT).show();
        }
    }

    private void performScroll(boolean scrollDown) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            GestureDescription.Builder builder = new GestureDescription.Builder();
            Path path = new Path();
            // Start gesture scroll at raw center of screen to scroll active viewport
            float startX = screenWidth / 2f;
            float startY = screenHeight / 2f;
            float endY = scrollDown ? (startY - 250) : (startY + 250); // Swipe up to scroll down and vice-versa

            path.moveTo(startX, startY);
            path.lineTo(startX, endY);

            builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 200));
            dispatchGesture(builder.build(), null, null);
        }
    }

    private void showTouchpad() {
        if (isBubbleAdded) {
            try {
                windowManager.removeView(bubbleView);
            } catch (Exception e) {}
            isBubbleAdded = false;
        }
        if (!isTouchpadAdded) {
            try {
                windowManager.addView(touchpadView, touchpadParams);
            } catch (Exception e) {}
            isTouchpadAdded = true;
        }
        if (!isCursorAdded) {
            try {
                windowManager.addView(cursorView, cursorParams);
            } catch (Exception e) {}
            isCursorAdded = true;
        }
    }

    private void showBubble() {
        if (isTouchpadAdded) {
            try {
                windowManager.removeView(touchpadView);
            } catch (Exception e) {}
            isTouchpadAdded = false;
        }
        if (isCursorAdded) {
            try {
                windowManager.removeView(cursorView);
            } catch (Exception e) {}
            isCursorAdded = false;
        }
        if (!isBubbleAdded) {
            try {
                windowManager.addView(bubbleView, bubbleParams);
            } catch (Exception e) {}
            isBubbleAdded = true;
        }
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        showTouchpad();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isTouchpadAdded) {
            try { windowManager.removeView(touchpadView); } catch (Exception e) {}
        }
        if (isCursorAdded) {
            try { windowManager.removeView(cursorView); } catch (Exception e) {}
        }
        if (isBubbleAdded) {
            try { windowManager.removeView(bubbleView); } catch (Exception e) {}
        }
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}

    // Custom View to render a sleek, crisp cursor arrow pointing top-left
    private static class CursorView extends View {
        private Paint paint;
        private Paint borderPaint;
        private Path path;

        public CursorView(Context context) {
            super(context);
            init();
        }

        private void init() {
            paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.FILL);

            borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            borderPaint.setColor(Color.BLACK);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(3f);

            path = new Path();
            path.moveTo(0, 0);
            path.lineTo(0, 36);
            path.lineTo(10, 27);
            path.lineTo(21, 43);
            path.lineTo(26, 40);
            path.lineTo(15, 24);
            path.lineTo(26, 24);
            path.close();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawPath(path, paint);
            canvas.drawPath(path, borderPaint);
        }
    }
}