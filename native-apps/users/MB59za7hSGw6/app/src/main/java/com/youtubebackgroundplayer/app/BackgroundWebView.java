package com.youtubebackgroundplayer.app;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.WebView;

public class BackgroundWebView extends WebView {

    public BackgroundWebView(Context context) {
        super(context);
    }

    public BackgroundWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BackgroundWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        // Prevent WebKit core engine from pausing HTML5 video/audio when activity is minimized or screen turns off
        super.onWindowVisibilityChanged(View.VISIBLE);
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, View.VISIBLE);
    }

    @Override
    public void dispatchWindowVisibilityChanged(int visibility) {
        super.dispatchWindowVisibilityChanged(View.VISIBLE);
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(true);
    }

    @Override
    public void onPause() {
        // Override standard WebKit onPause to keep timers and audio playback running continuously
    }
}