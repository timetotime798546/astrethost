package com.youtubebackgroundplayer.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
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

    private BackgroundWebView webView;
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
    private PowerManager.WakeLock appWakeLock;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    private BroadcastReceiver screenStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                if (Intent.ACTION_SCREEN_OFF.equals(action) || Intent.ACTION_SCREEN_ON.equals(action)) {
                    enableBackgroundMode(true);
                    triggerJsKeepAlive();
                }
            }
        }
    };

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BackgroundAudioService.LocalBinder binder = (BackgroundAudioService.LocalBinder) service;
            audioService = binder.getService();
            isServiceBound = true;

            audioService.setMediaStateChangeListener(new BackgroundAudioService.MediaStateChangeListener() {
                @Override
                public void onPlayRequested() {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (webView != null) {
                                webView.evaluateJavascript("if(typeof window.forcePlayVideo === 'function') { window.forcePlayVideo(); } else { var v = document.querySelector('video'); if (v) v.play(); }", null);
                            }
                        }
                    });
                }

                @Override
                public void onPauseRequested() {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (webView != null) {
                                webView.evaluateJavascript("if(typeof window.forcePauseVideo === 'function') { window.forcePauseVideo(); } else { var v = document.querySelector('video'); if (v) v.pause(); }", null);
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
            mainHandler.post(new Runnable() {
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

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            appWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "YouTubeBackgroundPlayer:MainActivityLock");
            appWakeLock.setReferenceCounted(false);
        }

        initViews();
        setupWebView();
        setupListeners();
        checkNotificationPermission();
        registerScreenStateReceiver();

        serviceIntent = new Intent(this, BackgroundAudioService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        webView.loadUrl(YOUTUBE_HOME);
    }

    private void registerScreenStateReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(screenStateReceiver, filter);
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, PERMISSION_REQUEST_NOTIFICATIONS);
            }
        }
    }

    private void initViews() {
        webView = (BackgroundWebView) findViewById(R.id.web_view);
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

    private void injectBackgroundAudioJs() {
        if (webView == null) return;
        String js = "javascript:(function() {" +
                "try {" +
                "  Object.defineProperty(document, 'hidden', {get: function() { return false; }, configurable: true});" +
                "  Object.defineProperty(document, 'visibilityState', {get: function() { return 'visible'; }, configurable: true});" +
                "  Object.defineProperty(document, 'webkitVisibilityState', {get: function() { return 'visible'; }, configurable: true});" +
                "  Object.defineProperty(document, 'webkitHidden', {get: function() { return false; }, configurable: true});" +
                "  Object.defineProperty(document, 'hasFocus', {value: function() { return true; }, configurable: true});" +

                "  var origAddEventListener = EventTarget.prototype.addEventListener;" +
                "  EventTarget.prototype.addEventListener = function(type, listener, options) {" +
                "    if (type === 'visibilitychange' || type === 'webkitvisibilitychange' || type === 'pagehide' || type === 'blur' || type === 'freeze') {" +
                "      return;" +
                "    }" +
                "    return origAddEventListener.call(this, type, listener, options);" +
                "  };" +

                "  if (!window.origVideoPause) {" +
                "    window.origVideoPause = HTMLVideoElement.prototype.pause;" +
                "    HTMLVideoElement.prototype.pause = function() {" +
                "      if (window.isBackgroundAudioActive && !window.userRequestedPause) {" +
                "        return;" +
                "      }" +
                "      return window.origVideoPause.apply(this, arguments);" +
                "    };" +
                "  }" +

                "  if (window.AudioContext) {" +
                "    var origResume = AudioContext.prototype.resume;" +
                "    AudioContext.prototype.resume = function() {" +
                "      return origResume.apply(this, arguments);" +
                "    };" +
                "  }" +

                "  window.forcePlayVideo = function() {" +
                "    window.userRequestedPause = false;" +
                "    window.isBackgroundAudioActive = true;" +
                "    var v = document.querySelector('video');" +
                "    if (v) { v.play(); }" +
                "  };" +

                "  window.forcePauseVideo = function() {" +
                "    window.userRequestedPause = true;" +
                "    var v = document.querySelector('video');" +
                "    if (v && window.origVideoPause) { window.origVideoPause.call(v); }" +
                "  };" +

                "  window.enableBgMode = function(enable) {" +
                "    window.isBackgroundAudioActive = enable;" +
                "    if (enable) {" +
                "      window.userRequestedPause = false;" +
                "      var v = document.querySelector('video');" +
                "      if (v && v.paused) { v.play(); }" +
                "    }" +
                "  };" +

                "  function attachBridge() {" +
                "    var v = document.querySelector('video');" +
                "    if (v && !v.dataset.hasBridge) {" +
                "      v.dataset.hasBridge = 'true';" +
                "      v.addEventListener('play', function() {" +
                "        window.userRequestedPause = false;" +
                "        if (window.AndroidMediaBridge) { window.AndroidMediaBridge.onVideoStateChanged(true, document.title); }" +
                "      });" +
                "      v.addEventListener('pause', function() {" +
                "        if (window.AndroidMediaBridge) { window.AndroidMediaBridge.onVideoStateChanged(!v.paused, document.title); }" +
                "      });" +
                "      if (!v.paused && window.AndroidMediaBridge) { window.AndroidMediaBridge.onVideoStateChanged(true, document.title); }" +
                "    }" +
                "  }" +

                "  attachBridge();" +
                "  if (!window.bgAudioInterval) {" +
                "    window.bgAudioInterval = setInterval(function() {" +
                "      attachBridge();" +
                "      if (window.isBackgroundAudioActive) {" +
                "        var v = document.querySelector('video');" +
                "        if (v && v.paused && !window.userRequestedPause) { v.play(); }" +
                "      }" +
                "    }, 500);" +
                "  }" +

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

    private void enableBackgroundMode(boolean enable) {
        if (appWakeLock != null) {
            if (enable && !appWakeLock.isHeld()) {
                try {
                    appWakeLock.acquire(24 * 60 * 60 * 1000L);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if (webView != null) {
            injectBackgroundAudioJs();
            final String jsCmd = "if(typeof window.enableBgMode === 'function') { window.enableBgMode(" + enable + "); }";
            webView.evaluateJavascript(jsCmd, null);
        }
    }

    private void triggerJsKeepAlive() {
        if (webView != null) {
            webView.evaluateJavascript("var v = document.querySelector('video'); if (v && v.paused && !window.userRequestedPause) { v.play(); }", null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
            webView.resumeTimers();
        }
        enableBackgroundMode(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        enableBackgroundMode(true);
        triggerJsKeepAlive();
    }

    @Override
    protected void onStop() {
        super.onStop();
        enableBackgroundMode(true);
        triggerJsKeepAlive();
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            // Keep playback running seamlessly by pushing task to background
            moveTaskToBack(true);
        }
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
    protected void onDestroy() {
        try {
            unregisterReceiver(screenStateReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (appWakeLock != null && appWakeLock.isHeld()) {
            try {
                appWakeLock.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
        super.onDestroy();
    }
}