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
import android.graphics.BitmapFactory;
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
import android.webkit.WebResourceResponse;
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {

    private static final String YOUTUBE_HOME = "https://m.youtube.com";
    private static final int REQUEST_CODE_HISTORY = 1002;
    private static final int PERMISSION_REQUEST_NOTIFICATIONS = 1003;

    private WebView webView;
    private ProgressBar progressBar;
    private LinearLayout mainContentContainer;
    private LinearLayout errorContainer;
    private ImageButton btnBack;
    private ImageButton btnForward;
    private ImageButton btnRefresh;
    private ImageButton btnHome;
    private ImageButton btnHistory;
    private ImageButton btnExit;
    private Button btnRetry;

    // Paywall Overlay Views
    private ScrollView paywallOverlay;
    private TextView txtPaywallTitle;
    private TextView txtPaywallSub;
    private TextView txtDeviceId;
    private Button btnCopyDeviceId;
    private ImageView imgQrCode;
    private ProgressBar qrProgress;
    private TextView txtUpiId;
    private Button btnCopyUpi;
    private Button btnWhatsappProof;
    private Button btnCheckStatus;
    private ProgressBar checkStatusProgress;

    private HistoryDbHelper dbHelper;
    private TrialPaywallManager trialPaywallManager;
    private Intent serviceIntent;
    private BackgroundAudioService audioService;
    private boolean isServiceBound = false;
    private boolean isExiting = false;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    private Runnable trialMonitorRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isExiting) {
                if (trialPaywallManager != null && !trialPaywallManager.isAccessGranted(MainActivity.this)) {
                    stopPlaybackAndLock();
                } else {
                    mainHandler.postDelayed(this, 3000);
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

            if (!trialPaywallManager.isAccessGranted(MainActivity.this)) {
                stopPlaybackAndLock();
                return;
            }

            audioService.setMediaStateChangeListener(new BackgroundAudioService.MediaStateChangeListener() {
                @Override
                public void onPlayRequested() {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (!trialPaywallManager.isAccessGranted(MainActivity.this)) {
                                stopPlaybackAndLock();
                                return;
                            }
                            if (webView != null && !isExiting && paywallOverlay.getVisibility() != View.VISIBLE) {
                                webView.evaluateJavascript("var v = document.querySelector('video.html5-main-video') || document.querySelector('video'); if (v) v.play();", null);
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
                                webView.evaluateJavascript("var v = document.querySelector('video.html5-main-video') || document.querySelector('video'); if (v) v.pause();", null);
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
                    if (!trialPaywallManager.isAccessGranted(MainActivity.this)) {
                        stopPlaybackAndLock();
                        return;
                    }
                    if (audioService != null && !isExiting && paywallOverlay.getVisibility() != View.VISIBLE) {
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
        trialPaywallManager = new TrialPaywallManager();

        initViews();
        setupWebView();
        setupListeners();
        setupPaywallListeners();
        checkNotificationPermission();

        handleNativeAdVisibility();

        if (trialPaywallManager.isAccessGranted(this)) {
            startMediaService();
        }

        // Execute Paywall & Trial Validation Flow
        verifyAccessAndLoadApp();
    }

    private void startMediaService() {
        if (!isServiceBound && trialPaywallManager.isAccessGranted(this)) {
            serviceIntent = new Intent(this, BackgroundAudioService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private void verifyAccessAndLoadApp() {
        trialPaywallManager.checkAccessStatus(this, new TrialPaywallManager.StatusCallback() {
            @Override
            public void onStatusResult(int status, String message) {
                handleNativeAdVisibility();
                if (status == TrialPaywallManager.STATUS_EXPIRED || !trialPaywallManager.isAccessGranted(MainActivity.this)) {
                    stopPlaybackAndLock();
                } else {
                    hidePaywallLockOverlay();
                    startMediaService();
                    if (webView != null && (webView.getUrl() == null || webView.getUrl().equals("about:blank"))) {
                        webView.loadUrl(YOUTUBE_HOME);
                    }
                    mainHandler.removeCallbacks(trialMonitorRunnable);
                    mainHandler.postDelayed(trialMonitorRunnable, 3000);
                }
            }
        });
    }

    private void stopPlaybackAndLock() {
        mainHandler.removeCallbacks(trialMonitorRunnable);

        if (webView != null) {
            webView.evaluateJavascript("window.shouldKeepPlaying = false; var v = document.querySelectorAll('video, audio'); for(var i=0; i<v.length; i++){ v[i].pause(); v[i].src = ''; }", null);
            webView.stopLoading();
        }

        if (audioService != null) {
            audioService.stopForegroundAndAudio();
            audioService = null;
        }

        if (isServiceBound) {
            try {
                unbindService(serviceConnection);
            } catch (Exception e) {
                e.printStackTrace();
            }
            isServiceBound = false;
        }

        if (serviceIntent != null) {
            try {
                stopService(serviceIntent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        showPaywallLockOverlay();
    }

    private void showPaywallLockOverlay() {
        mainContentContainer.setVisibility(View.GONE);
        paywallOverlay.setVisibility(View.VISIBLE);

        String deviceId = trialPaywallManager.getDeviceId(this);
        String upiId = trialPaywallManager.getUpiId(this);
        String amount = trialPaywallManager.getPremiumAmount(this);

        txtDeviceId.setText(deviceId);
        txtPaywallSub.setText("Upgrade to Lifetime Premium access for " + amount);
        txtUpiId.setText("UPI ID: " + upiId);

        loadDynamicQrCode(upiId, amount);
    }

    private void hidePaywallLockOverlay() {
        paywallOverlay.setVisibility(View.GONE);
        mainContentContainer.setVisibility(View.VISIBLE);
    }

    private void loadDynamicQrCode(String upiId, String amount) {
        String data = "upi://pay?pa=" + upiId + "&pn=BGTube&am=" + amount;
        final String qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=" + Uri.encode(data);

        qrProgress.setVisibility(View.VISIBLE);
        imgQrCode.setImageBitmap(null);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(qrUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);
                    conn.connect();

                    InputStream is = conn.getInputStream();
                    final Bitmap bitmap = BitmapFactory.decodeStream(is);
                    is.close();

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            qrProgress.setVisibility(View.GONE);
                            if (bitmap != null) {
                                imgQrCode.setImageBitmap(bitmap);
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            qrProgress.setVisibility(View.GONE);
                        }
                    });
                }
            }
        }).start();
    }

    private void setupPaywallListeners() {
        btnCopyDeviceId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String deviceId = txtDeviceId.getText().toString();
                copyToClipboard("Device ID", deviceId);
                Toast.makeText(MainActivity.this, "Device ID Copied!", Toast.LENGTH_SHORT).show();
            }
        });

        btnCopyUpi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String upiId = trialPaywallManager.getUpiId(MainActivity.this);
                copyToClipboard("UPI ID", upiId);
                Toast.makeText(MainActivity.this, "UPI ID Copied!", Toast.LENGTH_SHORT).show();
            }
        });

        btnWhatsappProof.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String whatsappNum = trialPaywallManager.getWhatsappNumber(MainActivity.this);
                String deviceId = trialPaywallManager.getDeviceId(MainActivity.this);
                String amount = trialPaywallManager.getPremiumAmount(MainActivity.this);

                String msg = "Hi, I have paid " + amount + " for BG Tube Player. My Device ID is: " + deviceId;
                String waUrl = "https://wa.me/" + whatsappNum + "?text=" + Uri.encode(msg);

                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(waUrl));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "WhatsApp is not installed on this device.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnCheckStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkStatusProgress.setVisibility(View.VISIBLE);
                btnCheckStatus.setEnabled(false);

                trialPaywallManager.recheckAccessStatus(MainActivity.this, new TrialPaywallManager.StatusCallback() {
                    @Override
                    public void onStatusResult(int status, String message) {
                        checkStatusProgress.setVisibility(View.GONE);
                        btnCheckStatus.setEnabled(true);
                        handleNativeAdVisibility();

                        if (status == TrialPaywallManager.STATUS_GRANTED && trialPaywallManager.isAccessGranted(MainActivity.this)) {
                            Toast.makeText(MainActivity.this, "Lifetime Premium Activated!", Toast.LENGTH_LONG).show();
                            hidePaywallLockOverlay();
                            startMediaService();
                            if (webView != null && (webView.getUrl() == null || webView.getUrl().equals("about:blank"))) {
                                webView.loadUrl(YOUTUBE_HOME);
                            }
                            mainHandler.postDelayed(trialMonitorRunnable, 3000);
                        } else {
                            Toast.makeText(MainActivity.this, "Payment verification pending. Please send proof via WhatsApp.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
    }

    private void copyToClipboard(String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText(label, text);
            clipboard.setPrimaryClip(clip);
        }
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, PERMISSION_REQUEST_NOTIFICATIONS);
            }
        }
    }

    private void initViews() {
        mainContentContainer = (LinearLayout) findViewById(R.id.main_content_container);
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
        paywallOverlay = (ScrollView) findViewById(R.id.paywall_overlay);
        txtPaywallTitle = (TextView) findViewById(R.id.txt_paywall_title);
        txtPaywallSub = (TextView) findViewById(R.id.txt_paywall_sub);
        txtDeviceId = (TextView) findViewById(R.id.txt_device_id);
        btnCopyDeviceId = (Button) findViewById(R.id.btn_copy_device_id);
        imgQrCode = (ImageView) findViewById(R.id.img_qr_code);
        qrProgress = (ProgressBar) findViewById(R.id.qr_progress);
        txtUpiId = (TextView) findViewById(R.id.txt_upi_id);
        btnCopyUpi = (Button) findViewById(R.id.btn_copy_upi);
        btnWhatsappProof = (Button) findViewById(R.id.btn_whatsapp_proof);
        btnCheckStatus = (Button) findViewById(R.id.btn_check_status);
        checkStatusProgress = (ProgressBar) findViewById(R.id.check_status_progress);
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
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if (request != null && request.getUrl() != null) {
                        String urlStr = request.getUrl().toString();
                        if (shouldBlockAdUrl(urlStr)) {
                            return createEmptyWebResourceResponse();
                        }
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

            @SuppressWarnings("deprecation")
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                if (url != null && shouldBlockAdUrl(url)) {
                    return createEmptyWebResourceResponse();
                }
                return super.shouldInterceptRequest(view, url);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                if (!trialPaywallManager.isAccessGranted(MainActivity.this)) {
                    stopPlaybackAndLock();
                    return;
                }
                if (!isExiting && paywallOverlay.getVisibility() != View.VISIBLE) {
                    progressBar.setVisibility(View.VISIBLE);
                    errorContainer.setVisibility(View.GONE);
                    updateNavButtons();
                    // Inject ad blocking script at the earliest possible stage (onPageStarted)
                    injectAdBlockingJsIfNeeded();
                    injectBackgroundAudioJs();
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (!trialPaywallManager.isAccessGranted(MainActivity.this)) {
                    stopPlaybackAndLock();
                    return;
                }
                if (!isExiting && paywallOverlay.getVisibility() != View.VISIBLE) {
                    progressBar.setVisibility(View.GONE);
                    updateNavButtons();

                    injectAdBlockingJsIfNeeded();
                    injectBackgroundAudioJs();

                    String title = view.getTitle();
                    if (url != null && (url.contains("youtube.com") || url.contains("youtu.be"))) {
                        dbHelper.addHistory(title, url);
                    }
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if (request.isForMainFrame() && !isExiting) {
                        showErrorScreen();
                    }
                }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                if (!isExiting) {
                    showErrorScreen();
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (!isExiting && paywallOverlay.getVisibility() != View.VISIBLE) {
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

    private boolean shouldBlockAdUrl(String url) {
        if (trialPaywallManager == null || !trialPaywallManager.isPremium(this)) {
            return false;
        }
        if (url == null) return false;
        String lower = url.toLowerCase();
        return lower.contains("googleads.g.doubleclick.net")
                || lower.contains("googlesyndication.com")
                || lower.contains("pagead2.googlesyndication.com")
                || lower.contains("adservice.google.")
                || lower.contains("doubleclick.net")
                || lower.contains("/ptracking")
                || lower.contains("/pagead/")
                || lower.contains("/get_ad_tags")
                || lower.contains("/ad_break")
                || lower.contains("/api/stats/ads");
    }

    private WebResourceResponse createEmptyWebResourceResponse() {
        return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream(new byte[0]));
    }

    private void handleNativeAdVisibility() {
        if (trialPaywallManager != null && trialPaywallManager.isPremium(this)) {
            int[] adContainerIds = new int[] {
                getResources().getIdentifier("ad_view_container", "id", getPackageName()),
                getResources().getIdentifier("banner_ad_container", "id", getPackageName()),
                getResources().getIdentifier("ad_view", "id", getPackageName())
            };
            for (int id : adContainerIds) {
                if (id != 0) {
                    View v = findViewById(id);
                    if (v != null) {
                        v.setVisibility(View.GONE);
                    }
                }
            }
        }
    }

    private void injectAdBlockingJsIfNeeded() {
        if (webView == null || trialPaywallManager == null) return;
        if (trialPaywallManager.isPremium(this)) {
            String adBlockJs = "javascript:(function() {" +
                    "try {" +
                    "  var styleId = 'bg-tube-mweb-adblock-style';" +
                    "  if (!document.getElementById(styleId)) {" +
                    "    var style = document.createElement('style');" +
                    "    style.id = styleId;" +
                    "    style.type = 'text/css';" +
                    "    style.innerHTML = '.ytp-ad-player-overlay-instream-info--clean-player-mweb, .ytp-ad-badge--clean-player, .ytp-ad-duration-remaining--clean-player, .ytp-ad-pod-index--mweb, [aria-label*=\"Sponsored\"], [aria-label=\"Sponsored\"], .video-ads, .ytp-ad-module, .ytp-ad-overlay-container, .ytp-ad-image-overlay, .ytp-ad-text, ytd-promoted-sparkles-web-renderer, ytd-display-ad-renderer, ytd-statement-banner-renderer, ytd-in-feed-ad-layout-renderer, ytd-banner-promo-renderer, ins.adsbygoogle, iframe[src*=\"googlesyndication\"], iframe[src*=\"doubleclick.net\"], #player-ads, .ad-container, .ad-div, #masthead-ad, ytd-promoted-video-renderer, .ytd-mealbar-promo-renderer, ytm-promoted-sparkles-web-renderer, ytm-single-option-survey-renderer, .ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-ad-preview-container, .ytp-ad-text-social, ytm-companion-ad-renderer, tp-yt-paper-dialog { display: none !important; visibility: hidden !important; height: 0px !important; width: 0px !important; opacity: 0 !important; pointer-events: none !important; }';" +
                    "    (document.head || document.documentElement).appendChild(style);" +
                    "  }" +
                    "  function executeMwebBypassLoop() {" +
                    "    var mwebSelectors = [" +
                    "      '.ytp-ad-player-overlay-instream-info--clean-player-mweb'," +
                    "      '.ytp-ad-badge--clean-player'," +
                    "      '.ytp-ad-duration-remaining--clean-player'," +
                    "      '.ytp-ad-pod-index--mweb'," +
                    "      '[aria-label*=\"Sponsored\"]'," +
                    "      '[aria-label=\"Sponsored\"]'," +
                    "      '.ad-showing'," +
                    "      '.ad-interrupting'," +
                    "      '.ytp-ad-player-overlay'" +
                    "    ];" +
                    "    var activeAdOverlay = null;" +
                    "    for (var i = 0; i < mwebSelectors.length; i++) {" +
                    "      var elem = document.querySelector(mwebSelectors[i]);" +
                    "      if (elem) { activeAdOverlay = elem; break; }" +
                    "    }" +
                    "    var video = document.querySelector('video.html5-main-video') || document.querySelector('video');" +
                    "    if (activeAdOverlay) {" +
                    "      if (video) {" +
                    "        video.playbackRate = 16.0;" +
                    "        video.muted = true;" +
                    "        if (isFinite(video.duration) && video.duration > 0) { video.currentTime = video.duration; }" +
                    "      }" +
                    "      var skipBtn = document.querySelector('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, button[class*=\"ytp-ad-skip\"], .ytp-ad-skip-button-slot, .ytp-ad-overlay-close-button, #dismiss-button');" +
                    "      if (skipBtn) { try { skipBtn.click(); } catch(e) {} }" +
                    "      try { activeAdOverlay.style.display = 'none'; } catch(e) {}" +
                    "      window._mwebAdActive = true;" +
                    "    } else if (window._mwebAdActive) {" +
                    "      if (video) {" +
                    "        video.playbackRate = 1.0;" +
                    "        video.muted = false;" +
                    "      }" +
                    "      window._mwebAdActive = false;" +
                    "    }" +
                    "  }" +
                    "  executeMwebBypassLoop();" +
                    "  if (!window.mwebBypassInterval) {" +
                    "    window.mwebBypassInterval = setInterval(executeMwebBypassLoop, 200);" +
                    "  }" +
                    "  if (!window.mwebMutationObserver) {" +
                    "    window.mwebMutationObserver = new MutationObserver(function(mutations) { executeMwebBypassLoop(); });" +
                    "    window.mwebMutationObserver.observe(document.body || document.documentElement, { childList: true, subtree: true, attributes: true });" +
                    "  }" +
                    "} catch(e) {}" +
                    "})();";

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                webView.evaluateJavascript(adBlockJs.replace("javascript:", ""), null);
            } else {
                webView.loadUrl(adBlockJs);
            }
        }
    }

    private void injectBackgroundAudioJs() {
        if (webView == null || isExiting || paywallOverlay.getVisibility() == View.VISIBLE || !trialPaywallManager.isAccessGranted(this)) return;

        boolean isPremium = trialPaywallManager.isPremium(this);
        String isPremiumStr = isPremium ? "true" : "false";

        String js = "javascript:(function() {" +
                "try {" +
                "  var isPremiumUser = " + isPremiumStr + ";" +
                "  function processAdsAndBypass() {" +
                "    if (!isPremiumUser) return;" +
                "    try {" +
                "      var styleId = 'bg-tube-mweb-adblock-style';" +
                "      if (!document.getElementById(styleId)) {" +
                "        var style = document.createElement('style');" +
                "        style.id = styleId;" +
                "        style.type = 'text/css';" +
                "        style.innerHTML = '.ytp-ad-player-overlay-instream-info--clean-player-mweb, .ytp-ad-badge--clean-player, .ytp-ad-duration-remaining--clean-player, .ytp-ad-pod-index--mweb, [aria-label*=\"Sponsored\"], [aria-label=\"Sponsored\"], .video-ads, .ytp-ad-module, .ytp-ad-overlay-container, .ytp-ad-image-overlay, .ytp-ad-text, ytd-promoted-sparkles-web-renderer, ytd-display-ad-renderer, ytd-statement-banner-renderer, ytd-in-feed-ad-layout-renderer, ytd-banner-promo-renderer, ins.adsbygoogle, iframe[src*=\"googlesyndication\"], iframe[src*=\"doubleclick.net\"], #player-ads, .ad-container, .ad-div, #masthead-ad, ytd-promoted-video-renderer, .ytd-mealbar-promo-renderer, ytm-promoted-sparkles-web-renderer, ytm-single-option-survey-renderer, .ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-ad-preview-container, .ytp-ad-text-social, ytm-companion-ad-renderer, tp-yt-paper-dialog { display: none !important; visibility: hidden !important; height: 0px !important; width: 0px !important; opacity: 0 !important; pointer-events: none !important; }';" +
                "        (document.head || document.documentElement).appendChild(style);" +
                "      }" +
                "      var mwebSelectors = [" +
                "        '.ytp-ad-player-overlay-instream-info--clean-player-mweb'," +
                "        '.ytp-ad-badge--clean-player'," +
                "        '.ytp-ad-duration-remaining--clean-player'," +
                "        '.ytp-ad-pod-index--mweb'," +
                "        '[aria-label*=\"Sponsored\"]'," +
                "        '[aria-label=\"Sponsored\"]'," +
                "        '.ad-showing'," +
                "        '.ad-interrupting'," +
                "        '.ytp-ad-player-overlay'" +
                "      ];" +
                "      var activeAdOverlay = null;" +
                "      for (var k = 0; k < mwebSelectors.length; k++) {" +
                "        var target = document.querySelector(mwebSelectors[k]);" +
                "        if (target) { activeAdOverlay = target; break; }" +
                "      }" +
                "      var vAd = document.querySelector('video.html5-main-video') || document.querySelector('video');" +
                "      if (activeAdOverlay) {" +
                "        if (vAd) {" +
                "          vAd.muted = true;" +
                "          vAd.playbackRate = 16.0;" +
                "          if (isFinite(vAd.duration) && vAd.duration > 0) { vAd.currentTime = vAd.duration; }" +
                "        }" +
                "        var skipBtn = document.querySelector('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, button[class*=\"ytp-ad-skip\"], .ytp-ad-skip-button-slot, .ytp-ad-overlay-close-button, #dismiss-button');" +
                "        if (skipBtn) { try { skipBtn.click(); } catch(e) {} }" +
                "        try { activeAdOverlay.style.display = 'none'; } catch(e) {}" +
                "        window._mwebAdActive = true;" +
                "      } else if (window._mwebAdActive) {" +
                "        if (vAd) {" +
                "          vAd.playbackRate = 1.0;" +
                "          vAd.muted = false;" +
                "        }" +
                "        window._mwebAdActive = false;" +
                "      }" +
                "    } catch(e) {}" +
                "  }" +
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
                "  function attachBridgeAndBypass() {" +
                "    processAdsAndBypass();" +
                "    var v = document.querySelector('video.html5-main-video') || document.querySelector('video');" +
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
                "  attachBridgeAndBypass();" +
                "  if (!window.bgAudioInterval) { window.bgAudioInterval = setInterval(attachBridgeAndBypass, 200); }" +
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
        mainHandler.removeCallbacks(trialMonitorRunnable);

        if (webView != null) {
            webView.evaluateJavascript("window.shouldKeepPlaying = false; var v = document.querySelectorAll('video, audio'); for(var i=0; i<v.length; i++){ v[i].pause(); v[i].src = ''; }", null);
            webView.stopLoading();
            webView.loadUrl("about:blank");
        }

        if (audioService != null) {
            audioService.stopForegroundAndAudio();
            audioService = null;
        }

        if (isServiceBound) {
            try {
                unbindService(serviceConnection);
            } catch (Exception e) {
                e.printStackTrace();
            }
            isServiceBound = false;
        }

        if (serviceIntent != null) {
            try {
                stopService(serviceIntent);
            } catch (Exception e) {
                e.printStackTrace();
            }
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
        if (isExiting || paywallOverlay.getVisibility() == View.VISIBLE || !trialPaywallManager.isAccessGranted(this)) return;
        injectAdBlockingJsIfNeeded();
        injectBackgroundAudioJs();
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (webView != null && !isExiting && paywallOverlay.getVisibility() != View.VISIBLE && trialPaywallManager.isAccessGranted(MainActivity.this)) {
                    webView.evaluateJavascript(
                            "(function(){ window.shouldKeepPlaying = true; var v = document.querySelector('video.html5-main-video') || document.querySelector('video'); if (v && v.paused) { v.play(); } })();",
                            null
                    );
                }
            }
        }, 300);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handleNativeAdVisibility();
        if (!trialPaywallManager.isAccessGranted(this)) {
            stopPlaybackAndLock();
        } else {
            mainHandler.removeCallbacks(trialMonitorRunnable);
            mainHandler.postDelayed(trialMonitorRunnable, 1000);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!isExiting && paywallOverlay.getVisibility() != View.VISIBLE) {
            if (!trialPaywallManager.isAccessGranted(this)) {
                stopPlaybackAndLock();
            } else {
                keepAudioPlayingInBackground();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isExiting && paywallOverlay.getVisibility() != View.VISIBLE) {
            if (!trialPaywallManager.isAccessGranted(this)) {
                stopPlaybackAndLock();
            } else {
                keepAudioPlayingInBackground();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (paywallOverlay != null && paywallOverlay.getVisibility() == View.VISIBLE) {
            Toast.makeText(this, "Please upgrade to premium access to continue.", Toast.LENGTH_SHORT).show();
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
                if (!trialPaywallManager.isAccessGranted(this)) {
                    stopPlaybackAndLock();
                } else {
                    webView.loadUrl(selectedUrl);
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        mainHandler.removeCallbacks(trialMonitorRunnable);
        if (isServiceBound) {
            try {
                unbindService(serviceConnection);
            } catch (Exception e) {
                e.printStackTrace();
            }
            isServiceBound = false;
        }
        super.onDestroy();
    }
}