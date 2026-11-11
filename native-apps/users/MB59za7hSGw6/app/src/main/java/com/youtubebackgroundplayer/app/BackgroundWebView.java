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
        // Crucial fix: Force native Android WebKit engine to keep audio track rendering active
        // when the activity is backgrounded, minimized, or screen locked.
        super.onWindowVisibilityChanged(View.VISIBLE);
    }
}