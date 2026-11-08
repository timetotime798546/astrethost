package com.youtubebackgroundplayer.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final String YOUTUBE_MOBILE_URL = "https://m.youtube.com";
    private static final int REQUEST_CODE_NOTIFICATIONS = 101;

    private WebView webView;
    private ProgressBar progressBar;
    private LinearLayout errorLayout;
    private TextView btnBack;
    private TextView btnForward;
    private TextView btnHome;
    private TextView btnRefresh;
    private TextView btnRetry;
    private String lastTitle = "YouTube";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupNavigationControls();
        checkNotificationPermissions();
        setupWebView();
        startBackgroundService("YouTube Background Player");

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            loadUrl(YOUTUBE_MOBILE_URL);
        }
    }

    private void initViews() {
        webView = findViewById(R.id.web_view);
        progressBar = findViewById(R.id.progress_bar);
        errorLayout = findViewById(R.id.error_layout);
        btnBack = findViewById(R.id.btn_back);
        btnForward = findViewById(R.id.btn_forward);
        btnHome = findViewById(R.id.btn_home);
        btnRefresh = findViewById(R.id.btn_refresh);
        btnRetry = findViewById(R.id.btn_retry);
    }

    private void setupNavigationControls() {
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    Toast.makeText(MainActivity.this, "No back history", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (webView.canGoForward()) {
                    webView.goForward();
                } else {
                    Toast.makeText(MainActivity.this, "No forward history", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideError();
                loadUrl(YOUTUBE_MOBILE_URL);
            }
        });

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideError();
                webView.reload();
            }
        });

        btnRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideError();
                webView.reload();
            }
        });
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(false);

        // Mobile Chrome User Agent configuration for web playback
        String defaultUA = settings.getUserAgentString();
        if (defaultUA != null) {
            settings.setUserAgentString(defaultUA.replace("Android", "Android 10").replace("Mobile", "Mobile Crosswalk"));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        }
        CookieManager.getInstance().setAcceptCookie(true);

        webView.setWebViewClient(new CustomWebViewClient());
        webView.setWebChromeClient(new CustomWebChromeClient());
    }

    private void checkNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_CODE_NOTIFICATIONS);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadUrl(String url) {
        webView.loadUrl(url);
    }

    private void startBackgroundService(String title) {
        Intent serviceIntent = new Intent(this, BackgroundAudioService.class);
        serviceIntent.setAction(BackgroundAudioService.ACTION_START);
        serviceIntent.putExtra(BackgroundAudioService.EXTRA_TITLE, title);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void updateServiceTitle(String title) {
        Intent serviceIntent = new Intent(this, BackgroundAudioService.class);
        serviceIntent.setAction(BackgroundAudioService.ACTION_UPDATE_TITLE);
        serviceIntent.putExtra(BackgroundAudioService.EXTRA_TITLE, title);
        startService(serviceIntent);
    }

    private void showError() {
        webView.setVisibility(View.GONE);
        errorLayout.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
    }

    private void hideError() {
        errorLayout.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
    }

    private class CustomWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                String url = request.getUrl().toString();
                if (url.contains("youtube.com") || url.contains("youtu.be")) {
                    return false;
                }
            }
            return false;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            hideError();
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(10);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            progressBar.setVisibility(View.GONE);
            String title = view.getTitle();
            if (title != null && !title.isEmpty() && !title.startsWith("http")) {
                lastTitle = title;
                updateServiceTitle(lastTitle);
            }
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (request.isForMainFrame()) {
                    showError();
                }
            }
        }
    }

    private class CustomWebChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            progressBar.setProgress(newProgress);
            if (newProgress == 100) {
                progressBar.setVisibility(View.GONE);
            } else {
                if (progressBar.getVisibility() == View.GONE) {
                    progressBar.setVisibility(View.VISIBLE);
                }
            }
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            if (title != null && !title.isEmpty() && !title.startsWith("http")) {
                lastTitle = title;
                updateServiceTitle(lastTitle);
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        webView.restoreState(savedInstanceState);
    }
}