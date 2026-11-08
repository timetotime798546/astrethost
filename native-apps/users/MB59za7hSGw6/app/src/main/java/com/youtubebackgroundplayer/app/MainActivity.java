package com.youtubebackgroundplayer.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

public class MainActivity extends Activity {

    private static final String YOUTUBE_HOME = "https://www.youtube.com";
    private static final int REQUEST_CODE_HISTORY = 1002;
    private static final int PERMISSION_REQUEST_NOTIFICATIONS = 1003;

    private WebView webView;
    private ProgressBar progressBar;
    private LinearLayout errorContainer;
    private ImageButton btnBack;
    private ImageButton btnForward;
    private ImageButton btnRefresh;
    private ImageButton btnHome;
    private ImageButton btnHistory;
    private Button btnRetry;

    private HistoryDbHelper dbHelper;
    private Intent serviceIntent;
    private BackgroundAudioService audioService;
    private boolean isServiceBound = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BackgroundAudioService.LocalBinder binder = (BackgroundAudioService.LocalBinder) service;
            audioService = binder.getService();
            isServiceBound = true;

            audioService.setMediaStateChangeListener(new BackgroundAudioService.MediaStateChangeListener() {
                @Override
                public void onPlayRequested() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (webView != null) {
                                webView.evaluateJavascript("var v = document.querySelector('video'); if (v) v.play();", null);
                            }
                        }
                    });
                }

                @Override
                public void onPauseRequested() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (webView != null) {
                                webView.evaluateJavascript("var v = document.querySelector('video'); if (v) v.pause();", null);
                            }
                        }
                    });
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            audioService = null;
            isServiceBound = false;
        }
    };

    public class WebAppInterface {
        @JavascriptInterface
        public void onVideoStateChanged(final boolean isPlaying, final String title) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (audioService != null) {
                        audioService.updateMediaMetadata(title);
                        int state = isPlaying ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED;
                        audioService.updatePlaybackState(state);
                    }
                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new HistoryDbHelper(this);

        initViews();
        setupWebView();
        setupListeners();
        checkNotificationPermission();

        // Start & Bind Foreground Service for media focus and notification support
        serviceIntent = new Intent(this, BackgroundAudioService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        webView.loadUrl(YOUTUBE_HOME);
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, PERMISSION_REQUEST_NOTIFICATIONS);
            }
        }
    }

    private void initViews() {
        webView = (WebView) findViewById(R.id.web_view);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        errorContainer = (LinearLayout) findViewById(R.id.error_container);

        btnBack = (ImageButton) findViewById(R.id.btn_back);
        btnForward = (ImageButton) findViewById(R.id.btn_forward);
        btnRefresh = (ImageButton) findViewById(R.id.btn_refresh);
        btnHome = (ImageButton) findViewById(R.id.btn_home);
        btnHistory = (ImageButton) findViewById(R.id.btn_history);
        btnRetry = (Button) findViewById(R.id.btn_retry);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        // Mobile user agent for optimal mobile layout while retaining background play
        settings.setUserAgentString("Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36");

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        }

        webView.addJavascriptInterface(new WebAppInterface(), "AndroidMediaBridge");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
                errorContainer.setVisibility(View.GONE);
                updateNavButtons();
                injectBackgroundAudioJs();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
                updateNavButtons();

                injectBackgroundAudioJs();

                // Save URL and page title to local SQLite history
                String title = view.getTitle();
                if (url != null && (url.contains("youtube.com") || url.contains("youtu.be"))) {
                    dbHelper.addHistory(title, url);
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if (request.isForMainFrame()) {
                        showErrorScreen();
                    }
                }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                showErrorScreen();
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
    }

    /**
     * Overrides Page Visibility API inside HTML5 player and registers HTML5 video event listeners.
     */
    private void injectBackgroundAudioJs() {
        if (webView == null) return;
        String js = "javascript:(function() {" +
                "try {" +
                "  Object.defineProperty(document, 'hidden', {get: function() { return false; }, configurable: true});" +
                "  Object.defineProperty(document, 'visibilityState', {get: function() { return 'visible'; }, configurable: true});" +
                "  document.addEventListener('visibilitychange', function(e) { e.stopImmediatePropagation(); }, true);" +
                "  window.addEventListener('pagehide', function(e) { e.stopImmediatePropagation(); }, true);" +
                "  window.addEventListener('blur', function(e) { e.stopImmediatePropagation(); }, true);" +
                "  function attachBridge() {" +
                "    var v = document.querySelector('video');" +
                "    if (v && !v.dataset.hasBridge) {" +
                "      v.dataset.hasBridge = 'true';" +
                "      v.addEventListener('play', function() { if(window.AndroidMediaBridge) window.AndroidMediaBridge.onVideoStateChanged(true, document.title); });" +
                "      v.addEventListener('pause', function() { if(window.AndroidMediaBridge) window.AndroidMediaBridge.onVideoStateChanged(false, document.title); });" +
                "      v.addEventListener('ended', function() { if(window.AndroidMediaBridge) window.AndroidMediaBridge.onVideoStateChanged(false, document.title); });" +
                "      if (!v.paused && window.AndroidMediaBridge) { window.AndroidMediaBridge.onVideoStateChanged(true, document.title); }" +
                "    }" +
                "  }" +
                "  attachBridge();" +
                "  if (!window.mediaBridgeInterval) { window.mediaBridgeInterval = setInterval(attachBridge, 1000); }" +
                "} catch(e) {}" +
                "})();";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript(js.replace("javascript:", ""), null);
        } else {
            webView.loadUrl(js);
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (webView.canGoBack()) {
                    webView.goBack();
                }
            }
        });

        btnForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (webView.canGoForward()) {
                    webView.goForward();
                }
            }
        });

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.reload();
            }
        });

        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.loadUrl(YOUTUBE_HOME);
            }
        });

        btnHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
                startActivityForResult(intent, REQUEST_CODE_HISTORY);
            }
        });

        btnRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                errorContainer.setVisibility(View.GONE);
                webView.reload();
            }
        });
    }

    private void updateNavButtons() {
        btnBack.setEnabled(webView.canGoBack());
        btnBack.setAlpha(webView.canGoBack() ? 1.0f : 0.4f);

        btnForward.setEnabled(webView.canGoForward());
        btnForward.setAlpha(webView.canGoForward() ? 1.0f : 0.4f);
    }

    private void showErrorScreen() {
        progressBar.setVisibility(View.GONE);
        errorContainer.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Do NOT pause WebView DOM timers or call webView.onPause() to sustain background playback
        injectBackgroundAudioJs();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Ensure Page Visibility parameters remain overridden when screen locks or activity stops
        injectBackgroundAudioJs();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_HISTORY && resultCode == RESULT_OK && data != null) {
            String selectedUrl = data.getStringExtra("selected_url");
            if (selectedUrl != null && !selectedUrl.trim().isEmpty()) {
                webView.loadUrl(selectedUrl);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
        if (serviceIntent != null) {
            stopService(serviceIntent);
        }
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}