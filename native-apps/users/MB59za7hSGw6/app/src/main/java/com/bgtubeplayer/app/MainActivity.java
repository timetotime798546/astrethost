package com.bgtubeplayer.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bgtubeplayer.app.R;

import java.net.URLEncoder;

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
    private ImageButton btnExit;
    private Button btnRetry;

    // Paywall Lock Overlay Views
    private ScrollView paywallOverlay;
    private TextView txtPaywallSubtext;
    private TextView txtDeviceId;
    private Button btnCopyDeviceId;
    private TextView txtUpiId;
    private Button btnCopyUpi;
    private ImageView imgQrCode;
    private Button btnSendWhatsapp;
    private Button btnCheckStatus;

    private PaywallManager paywallManager;
    private PaywallManager.PaywallConfig currentConfig;
    private boolean isLocked = true; // Default locked until explicitly verified paid

    private HistoryDbHelper dbHelper;
    private Intent serviceIntent;
    private BackgroundAudioService audioService;
    private boolean isServiceBound = false;
    private boolean isExiting = false;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BackgroundAudioService.LocalBinder binder = (BackgroundAudioService.LocalBinder) service;
            audioService = binder.getService();
            isServiceBound = true;

            if (isLocked) {
                audioService.pauseAndClearAudio();
            }

            audioService.setMediaStateChangeListener(new BackgroundAudioService.MediaStateChangeListener() {
                @Override
                public void onPlayRequested() {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (isLocked) {
                                stopAllAudioAndMediaPlayback();
                                return;
                            }
                            if (webView != null && !isExiting) {
                                webView.evaluateJavascript("var v = document.querySelector('video'); if (v) v.play();", null);
                            }
                        }
                    });
                }

                @Override
                public void onPauseRequested() {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (webView != null && !isExiting) {
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
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (isLocked) {
                        stopAllAudioAndMediaPlayback();
                        return;
                    }
                    if (audioService != null && !isExiting) {
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
        paywallManager = new PaywallManager(this);

        initViews();
        setupWebView();
        setupListeners();
        setupPaywallListeners();
        checkNotificationPermission();

        serviceIntent = new Intent(this, BackgroundAudioService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        updateCheckStatusButton();
        checkPaywallStatus(false);
    }

    private void checkPaywallStatus(final boolean manualCheck) {
        if (manualCheck) {
            if (!paywallManager.canPerformManualCheck()) {
                Toast.makeText(this, "Manual check limit reached (max 3 per hour).", Toast.LENGTH_LONG).show();
                updateCheckStatusButton();
                return;
            }
            paywallManager.recordManualCheck();
            updateCheckStatusButton();
            Toast.makeText(this, "Checking access status...", Toast.LENGTH_SHORT).show();
        }

        paywallManager.checkAccess(manualCheck, new PaywallManager.StatusCallback() {
            @Override
            public void onStatusResult(boolean isPremium, boolean isExpired, PaywallManager.PaywallConfig config) {
                currentConfig = config;
                updateCheckStatusButton();

                // Strict Rules: Only grant access when status === 'paid' (isPremium == true)
                if (isPremium) {
                    isLocked = false;
                    paywallOverlay.setVisibility(View.GONE);
                    if (manualCheck) {
                        Toast.makeText(MainActivity.this, "Lifetime Premium Activated!", Toast.LENGTH_LONG).show();
                    }
                    if (webView.getUrl() == null || webView.getUrl().isEmpty() || "about:blank".equals(webView.getUrl())) {
                        webView.loadUrl(YOUTUBE_HOME);
                    }
                } else {
                    // Strict Rule 2 & 3: Force lock screen active for status 'trial' or 'expired' or any non-paid state
                    isLocked = true;
                    stopAllAudioAndMediaPlayback();
                    showPaywallOverlay(config);
                    if (manualCheck) {
                        Toast.makeText(MainActivity.this, "Access not approved yet. Your payment status is still pending/trial.", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }

    private void stopAllAudioAndMediaPlayback() {
        if (webView != null) {
            webView.evaluateJavascript(
                    "window.shouldKeepPlaying = false; " +
                    "if (window.bgAudioInterval) { clearInterval(window.bgAudioInterval); window.bgAudioInterval = null; } " +
                    "var elems = document.querySelectorAll('video, audio'); " +
                    "for (var i = 0; i < elems.length; i++) { " +
                    "    try { elems[i].pause(); elems[i].currentTime = 0; } catch(e) {} " +
                    "}",
                    null
            );
        }

        if (audioService != null) {
            audioService.pauseAndClearAudio();
        }
    }

    private void updateCheckStatusButton() {
        if (btnCheckStatus == null) return;
        int checksLeft = paywallManager.getManualChecksLeft();
        if (checksLeft <= 0) {
            btnCheckStatus.setEnabled(false);
            btnCheckStatus.setAlpha(0.5f);
            btnCheckStatus.setText("Limit Reached (3 checks/hr)");
        } else {
            btnCheckStatus.setEnabled(true);
            btnCheckStatus.setAlpha(1.0f);
            btnCheckStatus.setText(getString(R.string.check_access_status) + " (" + checksLeft + "/3 left)");
        }
    }

    private void showPaywallOverlay(PaywallManager.PaywallConfig config) {
        paywallOverlay.setVisibility(View.VISIBLE);

        stopAllAudioAndMediaPlayback();

        if (config.premiumAmount != null && !config.premiumAmount.isEmpty()) {
            txtPaywallSubtext.setText("Upgrade to Lifetime Premium access for " + config.premiumAmount);
        } else {
            txtPaywallSubtext.setText("Upgrade to Lifetime Premium access to continue.");
        }

        txtDeviceId.setText(config.deviceId);
        txtUpiId.setText("UPI ID: " + (config.upiId.isEmpty() ? "N/A" : config.upiId));

        if (!config.upiId.isEmpty()) {
            PaywallManager.fetchQrCodeBitmap(config.upiId, config.premiumAmount, new PaywallManager.ImageCallback() {
                @Override
                public void onImageLoaded(Bitmap bitmap) {
                    if (bitmap != null) {
                        imgQrCode.setImageBitmap(bitmap);
                    }
                }
            });
        }
    }

    private void setupPaywallListeners() {
        btnCopyDeviceId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Device ID", txtDeviceId.getText().toString());
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(MainActivity.this, "Device ID Copied!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnCopyUpi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String upi = currentConfig != null ? currentConfig.upiId : "";
                if (!upi.isEmpty()) {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("UPI ID", upi);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(MainActivity.this, "UPI ID Copied!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        btnSendWhatsapp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentConfig == null) return;
                try {
                    String number = currentConfig.whatsappNumber;
                    String message = "Hi, I have paid " + currentConfig.premiumAmount + " for BG Tube Player. My Device ID is: " + currentConfig.deviceId;
                    String url = "https://wa.me/" + number + "?text=" + URLEncoder.encode(message, "UTF-8");
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Unable to open WhatsApp", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnCheckStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPaywallStatus(true);
            }
        });
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
        btnExit = (ImageButton) findViewById(R.id.btn_exit);
        btnRetry = (Button) findViewById(R.id.btn_retry);

        // Paywall Views
        paywallOverlay = (ScrollView) findViewById(R.id.paywall_lock_overlay);
        txtPaywallSubtext = (TextView) findViewById(R.id.txt_paywall_subtext);
        txtDeviceId = (TextView) findViewById(R.id.txt_device_id);
        btnCopyDeviceId = (Button) findViewById(R.id.btn_copy_device_id);
        txtUpiId = (TextView) findViewById(R.id.txt_upi_id);
        btnCopyUpi = (Button) findViewById(R.id.btn_copy_upi);
        imgQrCode = (ImageView) findViewById(R.id.img_qr_code);
        btnSendWhatsapp = (Button) findViewById(R.id.btn_send_whatsapp);
        btnCheckStatus = (Button) findViewById(R.id.btn_check_status);
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
                if (!isExiting && !isLocked) {
                    progressBar.setVisibility(View.VISIBLE);
                    errorContainer.setVisibility(View.GONE);
                    updateNavButtons();
                    injectBackgroundAudioJs();
                } else if (isLocked) {
                    stopAllAudioAndMediaPlayback();
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (!isExiting && !isLocked) {
                    progressBar.setVisibility(View.GONE);
                    updateNavButtons();

                    injectBackgroundAudioJs();

                    String title = view.getTitle();
                    if (url != null && (url.contains("youtube.com") || url.contains("youtu.be"))) {
                        dbHelper.addHistory(title, url);
                    }
                } else if (isLocked) {
                    stopAllAudioAndMediaPlayback();
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if (request.isForMainFrame() && !isExiting && !isLocked) {
                        showErrorScreen();
                    }
                }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                if (!isExiting && !isLocked) {
                    showErrorScreen();
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (!isExiting && !isLocked) {
                    if (newProgress < 100) {
                        progressBar.setVisibility(View.VISIBLE);
                        progressBar.setProgress(newProgress);
                    } else {
                        progressBar.setVisibility(View.GONE);
                    }
                }
            }
        });
    }

    private void injectBackgroundAudioJs() {
        if (webView == null || isExiting || isLocked) {
            stopAllAudioAndMediaPlayback();
            return;
        }
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
                "  function attachBridge() {" +
                "    var v = document.querySelector('video');" +
                "    if (v) {" +
                "      if (!v.dataset.hasBridge) {" +
                "        v.dataset.hasBridge = 'true';" +
                "        v.addEventListener('play', function() { if(window.AndroidMediaBridge) window.AndroidMediaBridge.onVideoStateChanged(true, document.title); });" +
                "        v.addEventListener('pause', function() { if(window.AndroidMediaBridge) window.AndroidMediaBridge.onVideoStateChanged(false, document.title); });" +
                "        v.addEventListener('ended', function() { if(window.AndroidMediaBridge) window.AndroidMediaBridge.onVideoStateChanged(false, document.title); });" +
                "        if (!v.paused && window.AndroidMediaBridge) { window.AndroidMediaBridge.onVideoStateChanged(true, document.title); }" +
                "      }" +
                "      if (v.paused && window.shouldKeepPlaying) {" +
                "        v.play();" +
                "      }" +
                "    }" +
                "  }" +
                "  attachBridge();" +
                "  if (!window.bgAudioInterval) { window.bgAudioInterval = setInterval(attachBridge, 800); }" +
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

        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopAppAndAudio();
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

    private void stopAppAndAudio() {
        isExiting = true;

        stopAllAudioAndMediaPlayback();

        if (webView != null) {
            webView.stopLoading();
            webView.loadUrl("about:blank");
        }

        if (audioService != null) {
            audioService.stopForegroundAndAudio();
        }

        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }

        if (serviceIntent != null) {
            stopService(serviceIntent);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask();
        } else {
            finish();
        }
        System.exit(0);
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

    private void keepAudioPlayingInBackground() {
        if (isExiting || isLocked) {
            stopAllAudioAndMediaPlayback();
            return;
        }
        injectBackgroundAudioJs();
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (webView != null && !isExiting && !isLocked) {
                    webView.evaluateJavascript(
                            "(function(){ window.shouldKeepPlaying = true; var v = document.querySelector('video'); if (v && v.paused) { v.play(); } })();",
                            null
                    );
                }
            }
        }, 300);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!isExiting && !isLocked) {
            keepAudioPlayingInBackground();
        } else if (isLocked) {
            stopAllAudioAndMediaPlayback();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isExiting && !isLocked) {
            keepAudioPlayingInBackground();
        } else if (isLocked) {
            stopAllAudioAndMediaPlayback();
        }
    }

    @Override
    public void onBackPressed() {
        if (isLocked) {
            stopAllAudioAndMediaPlayback();
            moveTaskToBack(true);
            return;
        }
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
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
        if (isLocked) {
            stopAllAudioAndMediaPlayback();
        }
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
        super.onDestroy();
    }
}