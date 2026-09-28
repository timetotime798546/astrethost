package com.studentdatasaver.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.ImageButton;

public class FloatingActionButton extends ImageButton {

    public FloatingActionButton(Context context) {
        super(context);
        init();
    }

    public FloatingActionButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FloatingActionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        ShapeDrawable background = new ShapeDrawable(new OvalShape());
        background.getPaint().setColor(Color.parseColor("#4285F4")); // Google Blue

        // Set dimensions (dp to pixels)
        float density = getResources().getDisplayMetrics().density;
        int size = (int) (56 * density); // 56dp for standard FAB
        int padding = (int) (16 * density); // Padding for the icon

        setMinimumWidth(size);
        setMinimumHeight(size);
        setMaxWidth(size);
        setMaxHeight(size);
        setPadding(padding, padding, padding, padding);

        setBackgroundDrawable(background); // Deprecated but works for older API levels
        setElevation(6 * density); // Shadow
        setClickable(true);
        setFocusable(true);
        setScaleType(ScaleType.FIT_CENTER);
        setGravity(Gravity.CENTER);
    }
}