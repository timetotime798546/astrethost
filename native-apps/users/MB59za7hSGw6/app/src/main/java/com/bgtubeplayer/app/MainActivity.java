package com.bgtubeplayer.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
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
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class MainActivity extends Activity {

    private static final String YOUTUBE_HOME = "https://www.youtube.com";
    private static final String PREFS_NAME = "bg_tube_prefs";
    private static final int REQUEST_CODE_HISTORY = 1002;
    private static final int PERMISSION_REQUEST_NOTIFICATIONS = 1003;

    private WebView webView;
    private ProgressBar progressBar;
    private LinearLayout errorContainer;
    private ImageButton btnUser;
    private ImageButton btnRefresh;
    private ImageButton btnHome;
    private ImageButton btnHistory;
    private ImageButton btnExit;
    private Button btnRetry;

    // Profile Overlay components
    private FrameLayout overlayUser;
    private TextView txtTrialStatus;
    private TextView txtPlanStatus;
    private TextView txtDeviceId;
    private Button btnCopyDeviceId;
    private Button btnRefreshStatus;
    private Button btnCloseOverlay;
    private LinearLayout layoutPayInfo;
    private TextView txtPayDetails;
    private Button btnWhatsApp;

    // Full Screen Lock Overlay components
    private FrameLayout overlayLock;
    private TextView txtLockDeviceId;
    private Button btnLockCopyId;
    private TextView txtLockPayDetails;
    private Button btnLockWhatsApp;
    private Button btnLockRefresh;

    private HistoryDbHelper dbHelper;
    private Intent serviceIntent;
    private BackgroundAudioService audioService;
    private boolean isServiceBound = false;
    private boolean isExiting = false;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private String currentDeviceId = "";

    private boolean hasAutoRefreshedOnExpiry = false;

    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isExiting) {
                updateTrialAndLockState();
                timerHandler.postDelayed(this, 1000);
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
                            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                            boolean isPremium = prefs.getBoolean("is_premium", false);
                            long expiresAt = prefs.getLong("expires_at", 0);
                            long nowSec = System.currentTimeMillis() / 1000;

                            if (!isPremium && expiresAt <= nowSec && expiresAt > 0) {
                                // Locked - don't allow playback when trial expired
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
                    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                    boolean isPremium = prefs.getBoolean("is_premium", false);
                    long expiresAt = prefs.getLong("expires_at", 0);
                    long nowSec = System.currentTimeMillis() / 1000;

                    if (!isPremium && expiresAt <= nowSec && expiresAt > 0) {
                        // Pause video immediately if trial expired
                        if (webView != null) {
                            webView.evaluateJavascript("var v = document.querySelector('video'); if (v) v.pause();", null);
                        }
                        if (audioService != null) {
                            audioService.updatePlaybackState(PlaybackState.STATE_PAUSED);
                        }
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

        initViews();
        setupWebView();
        setupListeners();
        setupDeviceInfo();
        checkNotificationPermission();

        serviceIntent = new Intent(this, BackgroundAudioService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        // SMART NETWORK FETCH TRIGGER #1:
        // Check SharedPreferences on app startup. If is_premium is true (or status is 'paid'),
        // DO NOT make any network call on launch, on overlay open, or at any time.
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isPremium = prefs.getBoolean("is_premium", false);
        if (!isPremium) {
            fetchStatusFromWorker(false);
        }

        updateTrialAndLockState();
        timerHandler.post(timerRunnable);

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

        btnUser = (ImageButton) findViewById(R.id.btn_user);
        btnRefresh = (ImageButton) findViewById(R.id.btn_refresh);
        btnHome = (ImageButton) findViewById(R.id.btn_home);
        btnHistory = (ImageButton) findViewById(R.id.btn_history);
        btnExit = (ImageButton) findViewById(R.id.btn_exit);
        btnRetry = (Button) findViewById(R.id.btn_retry);

        // User Overlay
        overlayUser = (FrameLayout) findViewById(R.id.overlay_user);
        txtTrialStatus = (TextView) findViewById(R.id.txt_trial_status);
        txtPlanStatus = (TextView) findViewById(R.id.txt_plan_status);
        txtDeviceId = (TextView) findViewById(R.id.txt_device_id);
        btnCopyDeviceId = (Button) findViewById(R.id.btn_copy_device_id);
        btnRefreshStatus = (Button) findViewById(R.id.btn_refresh_status);
        btnCloseOverlay = (Button) findViewById(R.id.btn_close_overlay);
        layoutPayInfo = (LinearLayout) findViewById(R.id.layout_pay_info);
        txtPayDetails = (TextView) findViewById(R.id.txt_pay_details);
        btnWhatsApp = (Button) findViewById(R.id.btn_whatsapp);

        // Lock Overlay
        overlayLock = (FrameLayout) findViewById(R.id.overlay_lock);
        txtLockDeviceId = (TextView) findViewById(R.id.txt_lock_device_id);
        btnLockCopyId = (Button) findViewById(R.id.btn_lock_copy_id);
        txtLockPayDetails = (TextView) findViewById(R.id.txt_lock_pay_details);
        btnLockWhatsApp = (Button) findViewById(R.id.btn_lock_whatsapp);
        btnLockRefresh = (Button) findViewById(R.id.btn_lock_refresh);
    }

    private void setupDeviceInfo() {
        currentDeviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        if (currentDeviceId == null || currentDeviceId.isEmpty()) {
            currentDeviceId = "UNKNOWN_DEVICE_ID";
        }
        txtDeviceId.setText(currentDeviceId);
        txtLockDeviceId.setText(currentDeviceId);
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
                if (!isExiting) {
                    progressBar.setVisibility(View.VISIBLE);
                    errorContainer.setVisibility(View.GONE);
                    injectBackgroundAudioJs();
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (!isExiting) {
                    progressBar.setVisibility(View.GONE);
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
                if (!isExiting) {
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
        if (webView == null || isExiting) return;
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

    private String getAppId() {
        try {
            InputStream is = getAssets().open("app-meta.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String jsonStr = new String(buffer, "UTF-8");
            JSONObject json = new JSONObject(jsonStr);
            return json.optString("app_id", "app_6af6c9ceb8503");
        } catch (Exception e) {
            return "app_6af6c9ceb8503";
        }
    }

    private void fetchStatusFromWorker(final boolean isUserInitiated) {
        SharedPreferences prefsCheck = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean currentIsPremium = prefsCheck.getBoolean("is_premium", false);

        // PERMANENT PAID STATUS BYPASS: If is_premium is true and not manually triggered by user, skip network call
        if (currentIsPremium && !isUserInitiated) {
            return;
        }

        if (isUserInitiated) {
            Toast.makeText(this, "Refreshing status with server...", Toast.LENGTH_SHORT).show();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    String appId = getAppId();
                    String urlStr = "https://trumpledroid-apps-admin-47at498aq98hrg9633fugqd3.cloudbeta28624.workers.dev/get-app-config?app_id="
                            + URLEncoder.encode(appId, "UTF-8")
                            + "&device_id=" + URLEncoder.encode(currentDeviceId, "UTF-8");
                    URL url = new URL(urlStr);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);

                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200) {
                        InputStream is = conn.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                        reader.close();

                        JSONObject json = new JSONObject(sb.toString());

                        boolean isPremium = json.optBoolean("is_premium", false);
                        if (!isPremium && json.has("status")) {
                            String st = json.optString("status", "");
                            if ("paid".equalsIgnoreCase(st) || "premium".equalsIgnoreCase(st)) {
                                isPremium = true;
                            }
                        }

                        boolean isTrial = json.optBoolean("is_trial", true);
                        long expiresAt = json.optLong("expires_at", 0);
                        String whatsapp = json.optString("whatsapp_number", "");
                        String upi = json.optString("upi_id", "");
                        String amount = json.optString("premium_amount", "");

                        // LOCAL CACHING & RESPONSE SYNC: Save all fields to SharedPreferences
                        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean("is_premium", isPremium);
                        editor.putBoolean("is_trial", isTrial);
                        editor.putLong("expires_at", expiresAt);
                        editor.putString("whatsapp_number", whatsapp);
                        editor.putString("upi_id", upi);
                        editor.putString("premium_amount", amount);
                        editor.putLong("last_sync_time", System.currentTimeMillis());
                        editor.apply();

                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                updateTrialAndLockState();
                                if (isUserInitiated) {
                                    Toast.makeText(MainActivity.this, "Status refreshed successfully", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    } else if (isUserInitiated) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Unable to reach server. Displaying cached status.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (isUserInitiated) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Network connection issue. Displaying cached status.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
            }
        }).start();
    }

    private void updateTrialAndLockState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isPremium = prefs.getBoolean("is_premium", false);
        boolean isTrial = prefs.getBoolean("is_trial", true);
        long expiresAt = prefs.getLong("expires_at", 0);
        final String whatsapp = prefs.getString("whatsapp_number", "");
        String upi = prefs.getString("upi_id", "");
        String amount = prefs.getString("premium_amount", "");

        // Display UPI and Payment Info if available
        StringBuilder payText = new StringBuilder();
        if (!upi.isEmpty()) {
            payText.append("UPI ID: ").append(upi);
        }
        if (!amount.isEmpty()) {
            if (payText.length() > 0) payText.append(" | ");
            payText.append("Fee: ₹").append(amount);
        }

        if (payText.length() > 0) {
            txtPayDetails.setText(payText.toString());
            txtLockPayDetails.setText(payText.toString());
            layoutPayInfo.setVisibility(View.VISIBLE);
            txtLockPayDetails.setVisibility(View.VISIBLE);
        } else {
            layoutPayInfo.setVisibility(View.GONE);
            txtLockPayDetails.setVisibility(View.GONE);
        }

        if (!whatsapp.isEmpty()) {
            btnWhatsApp.setVisibility(View.VISIBLE);
            btnLockWhatsApp.setVisibility(View.VISIBLE);
            View.OnClickListener waClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openWhatsApp(whatsapp);
                }
            };
            btnWhatsApp.setOnClickListener(waClickListener);
            btnLockWhatsApp.setOnClickListener(waClickListener);
        } else {
            btnWhatsApp.setVisibility(View.GONE);
            btnLockWhatsApp.setVisibility(View.GONE);
        }

        // 1. PERMANENT PAID STATUS BYPASS:
        // Displays "Plan Status: Lifetime Plan (Paid)" in green inside profile overlay
        // Permanently bypasses trial timers, background playback locks, and overlay warnings.
        if (isPremium) {
            txtPlanStatus.setText("Plan Status: Lifetime Plan (Paid)");
            txtPlanStatus.setTextColor(Color.parseColor("#4CAF50"));
            txtTrialStatus.setText("Lifetime Access Activated");
            txtTrialStatus.setTextColor(Color.parseColor("#4CAF50"));

            overlayLock.setVisibility(View.GONE);
            return;
        }

        // 2. DYNAMIC CLIENT-SIDE COUNTDOWN & LOCK HANDLING:
        long nowSec = System.currentTimeMillis() / 1000;

        if (expiresAt > nowSec) {
            long remainingSec = expiresAt - nowSec;
            String timeFormatted = formatDuration(remainingSec);

            txtPlanStatus.setText("Free Trial Plan");
            txtPlanStatus.setTextColor(Color.parseColor("#FFFFFF"));
            txtTrialStatus.setText("Active (" + timeFormatted + " remaining)");
            txtTrialStatus.setTextColor(Color.parseColor("#4CAF50"));

            overlayLock.setVisibility(View.GONE);
            hasAutoRefreshedOnExpiry = false;
        } else {
            txtPlanStatus.setText("Free Trial Plan (Expired)");
            txtPlanStatus.setTextColor(Color.parseColor("#FF5252"));
            txtTrialStatus.setText("Trial Status: Free Trial Expired");
            txtTrialStatus.setTextColor(Color.parseColor("#FF5252"));

            // Trial expired: trigger full-screen lock overlay and stop/pause background media playback
            overlayLock.setVisibility(View.VISIBLE);

            if (webView != null) {
                webView.evaluateJavascript("var v = document.querySelector('video'); if (v) v.pause();", null);
            }
            if (audioService != null) {
                audioService.updatePlaybackState(PlaybackState.STATE_PAUSED);
            }

            // SMART NETWORK FETCH TRIGGER #2:
            // When local trial countdown reaches zero, re-verify with Worker API once if admin updated D1 status to 'paid'
            if (!hasAutoRefreshedOnExpiry && expiresAt > 0) {
                hasAutoRefreshedOnExpiry = true;
                fetchStatusFromWorker(false);
            }
        }
    }

    private String formatDuration(long seconds) {
        if (seconds <= 0) return "0 Secs";
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long mins = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (days > 0) {
            return days + "d " + hours + "h " + mins + "m";
        } else if (hours > 0) {
            return hours + "h " + mins + "m " + secs + "s";
        } else if (mins > 0) {
            return mins + " Mins " + secs + " Secs";
        } else {
            return secs + " Secs";
        }
    }

    private void openWhatsApp(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            Toast.makeText(this, "No WhatsApp contact available", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String cleanPhone = phone.replaceAll("[^0-9]", "");
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://api.whatsapp.com/send?phone=" + cleanPhone + "&text=" + Uri.encode("Hello, I would like to activate Lifetime Premium for BG Tube Player. My Device ID is: " + currentDeviceId)));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Could not launch WhatsApp app", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupListeners() {
        // LOCAL CACHING & RESPONSE SYNC (NO OVERLAY API POLL):
        // Always load and display profile overlay instantly using cached local SharedPreferences data
        btnUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateTrialAndLockState();
                if (overlayUser != null) {
                    overlayUser.setVisibility(View.VISIBLE);
                }
            }
        });

        View.OnClickListener copyListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Device ID", currentDeviceId);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(MainActivity.this, "Device ID copied to clipboard", Toast.LENGTH_SHORT).show();
                }
            }
        };

        btnCopyDeviceId.setOnClickListener(copyListener);
        btnLockCopyId.setOnClickListener(copyListener);

        // SMART NETWORK FETCH TRIGGER #3:
        // Triggered when user explicitly taps "Refresh Status" button in profile overlay
        View.OnClickListener refreshStatusListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchStatusFromWorker(true);
            }
        };

        btnRefreshStatus.setOnClickListener(refreshStatusListener);
        btnLockRefresh.setOnClickListener(refreshStatusListener);

        btnCloseOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (overlayUser != null) {
                    overlayUser.setVisibility(View.GONE);
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
        timerHandler.removeCallbacks(timerRunnable);

        if (webView != null) {
            webView.evaluateJavascript("window.shouldKeepPlaying = false; var v = document.querySelectorAll('video, audio'); for(var i=0; i<v.length; i++){ v[i].pause(); v[i].src = ''; }", null);
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

    private void showErrorScreen() {
        progressBar.setVisibility(View.GONE);
        errorContainer.setVisibility(View.VISIBLE);
    }

    private void keepAudioPlayingInBackground() {
        if (isExiting) return;

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isPremium = prefs.getBoolean("is_premium", false);
        long expiresAt = prefs.getLong("expires_at", 0);
        long nowSec = System.currentTimeMillis() / 1000;

        if (!isPremium && expiresAt <= nowSec && expiresAt > 0) {
            return; // Trial expired - lock background playback
        }

        injectBackgroundAudioJs();
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (webView != null && !isExiting) {
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
        if (!isExiting) {
            keepAudioPlayingInBackground();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isExiting) {
            keepAudioPlayingInBackground();
        }
    }

    @Override
    public void onBackPressed() {
        if (overlayUser != null && overlayUser.getVisibility() == View.VISIBLE) {
            overlayUser.setVisibility(View.GONE);
            return;
        }

        if (overlayLock != null && overlayLock.getVisibility() == View.VISIBLE) {
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
        timerHandler.removeCallbacks(timerRunnable);
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
        super.onDestroy();
    }
}