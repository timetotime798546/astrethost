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

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {

    private static final String YOUTUBE_HOME = "https://www.youtube.com";
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
    private boolean isCheckingStatus = false;
    private Bitmap cachedQrBitmap = null;
    private String cachedQrKey = "";
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    private Runnable trialMonitorRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isExiting && paywallOverlay != null && paywallOverlay.getVisibility() != View.VISIBLE) {
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

        if (trialPaywallManager.isAccessGranted(this)) {
            startMediaService();
        }

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
            webView.evaluateJavascript(
                    "window.shouldKeepPlaying = false; " +
                    "var elems = document.querySelectorAll('video, audio'); " +
                    "for(var i=0; i<elems.length; i++){ " +
                    "   elems[i].pause(); " +
                    "   elems[i].src = ''; " +
                    "   elems[i].load(); " +
                    "}", null
            );
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

        showPaywallLockOverlay();
    }

    private void showPaywallLockOverlay() {
        if (paywallOverlay.getVisibility() == View.VISIBLE && cachedQrBitmap != null) {
            return;
        }

        mainContentContainer.setVisibility(View.GONE);
        paywallOverlay.setVisibility(View.VISIBLE);

        String deviceId = trialPaywallManager.getDeviceId(this);
        String upiId = trialPaywallManager.getUpiId(this);
        String amount = trialPaywallManager.getPremiumAmount(this);

        txtDeviceId.setText(deviceId);
        txtPaywallSub.setText("Upgrade to Lifetime Premium access for " + amount);
        txtUpiId.setText("UPI ID: " + upiId);

        renderQrCode(upiId, amount);
    }

    private void hidePaywallLockOverlay() {
        paywallOverlay.setVisibility(View.GONE);
        mainContentContainer.setVisibility(View.VISIBLE);
    }

    private void renderQrCode(final String upiId, final String amount) {
        String key = upiId + "_" + amount;
        if (cachedQrBitmap != null && key.equals(cachedQrKey)) {
            imgQrCode.setImageBitmap(cachedQrBitmap);
            qrProgress.setVisibility(View.GONE);
            return;
        }

        qrProgress.setVisibility(View.VISIBLE);

        // Instantly generate local QR Code bitmap so it never hangs or reloads continuously
        Bitmap localBitmap = QrCodeGenerator.generateQrBitmap("upi://pay?pa=" + upiId + "&pn=BGTube&am=" + amount, 210, 210);
        if (localBitmap != null) {
            cachedQrBitmap = localBitmap;
            cachedQrKey = key;
            imgQrCode.setImageBitmap(localBitmap);
            qrProgress.setVisibility(View.GONE);
        }

        // Try downloading high-res network QR code asynchronously in background
        final String data = "upi://pay?pa=" + upiId + "&pn=BGTube&am=" + amount;
        final String qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=" + Uri.encode(data);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(qrUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(4000);
                    conn.setReadTimeout(4000);
                    conn.connect();

                    InputStream is = conn.getInputStream();
                    final Bitmap remoteBitmap = BitmapFactory.decodeStream(is);
                    is.close();

                    if (remoteBitmap != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                cachedQrBitmap = remoteBitmap;
                                cachedQrKey = upiId + "_" + amount;
                                imgQrCode.setImageBitmap(remoteBitmap);
                                qrProgress.setVisibility(View.GONE);
                            }
                        });
                    }
                } catch (Exception e) {
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
                String encodedMsg = Uri.encode(msg);

                Intent waIntent = new Intent(Intent.ACTION_VIEW);
                waIntent.setData(Uri.parse("https://api.whatsapp.com/send?phone=" + whatsappNum + "&text=" + encodedMsg));
                waIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                try {
                    startActivity(waIntent);
                } catch (Exception e) {
                    try {
                        Intent fallbackIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/" + whatsappNum + "?text=" + encodedMsg));
                        fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(fallbackIntent);
                    } catch (Exception ex) {
                        Toast.makeText(MainActivity.this, "WhatsApp is not installed on this device.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        btnCheckStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isCheckingStatus) return;
                isCheckingStatus = true;

                checkStatusProgress.setVisibility(View.VISIBLE);
                btnCheckStatus.setEnabled(false);
                btnCheckStatus.setText("Checking Status...");

                trialPaywallManager.recheckAccessStatus(MainActivity.this, new TrialPaywallManager.StatusCallback() {
                    @Override
                    public void onStatusResult(int status, String message) {
                        isCheckingStatus = false;
                        checkStatusProgress.setVisibility(View.GONE);
                        btnCheckStatus.setEnabled(true);
                        btnCheckStatus.setText(R.string.check_status);

                        if (status == TrialPaywallManager.STATUS_GRANTED && trialPaywallManager.isAccessGranted(MainActivity.this)) {
                            Toast.makeText(MainActivity.this, "Lifetime Premium Activated!", Toast.LENGTH_LONG).show();
                            hidePaywallLockOverlay();
                            startMediaService();
                            if (webView != null) {
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

    private void injectBackgroundAudioJs() {
        if (webView == null || isExiting || paywallOverlay.getVisibility() == View.VISIBLE || !trialPaywallManager.isAccessGranted(this)) return;
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
                if (!trialPaywallManager.isAccessGranted(MainActivity.this)) {
                    stopPlaybackAndLock();
                    return;
                }
                if (webView.canGoBack()) {
                    webView.goBack();
                }
            }
        });

        btnForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!trialPaywallManager.isAccessGranted(MainActivity.this)) {
                    stopPlaybackAndLock();
                    return;
                }
                if (webView.canGoForward()) {
                    webView.goForward();
                }
            }
        });

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!trialPaywallManager.isAccessGranted(MainActivity.this)) {
                    stopPlaybackAndLock();
                    return;
                }
                webView.reload();
            }
        });

        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!trialPaywallManager.isAccessGranted(MainActivity.this)) {
                    stopPlaybackAndLock();
                    return;
                }
                webView.loadUrl(YOUTUBE_HOME);
            }
        });

        btnHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!trialPaywallManager.isAccessGranted(MainActivity.this)) {
                    stopPlaybackAndLock();
                    return;
                }
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
                if (!trialPaywallManager.isAccessGranted(MainActivity.this)) {
                    stopPlaybackAndLock();
                    return;
                }
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
        injectBackgroundAudioJs();
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (webView != null && !isExiting && paywallOverlay.getVisibility() != View.VISIBLE && trialPaywallManager.isAccessGranted(MainActivity.this)) {
                    webView.evaluateJavascript(
                            "(function(){ window.shouldKeepPlaying = true; var v = document.querySelector('video'); if (v && v.paused) { v.play(); } })();",
                            null
                    );
                }
            }
        }, 300);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!trialPaywallManager.isAccessGranted(this)) {
            stopPlaybackAndLock();
        } else {
            mainHandler.removeCallbacks(trialMonitorRunnable);
            mainHandler.postDelayed(trialMonitorRunnable, 3000);
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