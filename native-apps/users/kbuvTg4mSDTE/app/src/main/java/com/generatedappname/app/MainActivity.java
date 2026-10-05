package com.generatedappname.app;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity {

    private GLCubeRenderer renderer;
    private GLSurfaceView glSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Hide status bar and navigation bar for immersive full screen experience
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Create the GLSurfaceView
        glSurfaceView = new GLSurfaceView(this);
        
        // CRITICAL: Request an OpenGL ES 2.0 compatible context. 
        // Failing to declare this before setting the renderer defaults to GLES 1.x context,
        // causing all GLES20 functions to trigger instantaneous native crashes.
        glSurfaceView.setEGLContextClientVersion(2);
        
        // Create the renderer
        renderer = new GLCubeRenderer();
        
        // Set the renderer
        glSurfaceView.setRenderer(renderer);
        
        // Render continuously for smooth animations and touch responses
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        
        setContentView(glSurfaceView);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (renderer != null) {
            renderer.handleTouch(event);
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (glSurfaceView != null) {
            glSurfaceView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (glSurfaceView != null) {
            glSurfaceView.onPause();
        }
    }
}