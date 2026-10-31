package com.cyberrunner.app;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class CyberGLSurfaceView extends GLSurfaceView {

    private CyberRenderer renderer;

    public CyberGLSurfaceView(Context context) {
        super(context);
        init();
    }

    public CyberGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setEGLContextClientVersion(2);
        renderer = new CyberRenderer();
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    public CyberRenderer getCyberRenderer() {
        return renderer;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (renderer != null) {
                queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        renderer.triggerJump();
                    }
                });
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    public void triggerStartOrRestart() {
        if (renderer != null) {
            queueEvent(new Runnable() {
                @Override
                public void run() {
                    renderer.triggerStartOrRestart();
                }
            });
        }
    }
}