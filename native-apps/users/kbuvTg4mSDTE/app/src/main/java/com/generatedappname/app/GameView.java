package com.generatedappname.app;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GameView extends GLSurfaceView {
    private GameRenderer renderer;

    public GameView(Context context) {
        super(context);
        setEGLContextClientVersion(2);
        renderer = new GameRenderer(context);
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX();
        float y = e.getY();
        float width = getWidth();
        float height = getHeight();
        
        // Map touch to steering
        // Left half: steer left, Right half: steer right
        if (x < width / 2.0f) {
            renderer.setSteering(-1.0f);
        } else {
            renderer.setSteering(1.0f);
        }
        
        // Accelerate if touching bottom half
        if (y > height / 2.0f) {
            renderer.setAccelerating(true);
        } else {
            renderer.setAccelerating(false);
        }
        
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        super.onPause();
    }
}

