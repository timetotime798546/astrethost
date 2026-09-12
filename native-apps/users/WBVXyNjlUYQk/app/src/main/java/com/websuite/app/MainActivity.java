package com.websuite.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final String TARGET_URL = "https://trimpledroid.kesug.com/web-builder/web-generated/WBVXyNjlUYQk/my-website/";

    private WebView webView;
    private ProgressBar progressBar;
    private LinearLayout offlineLayout;
    private TextView btnBack;
    private TextView btnForward;
    private TextView btnRefresh;
    private TextView btnHome;
    private Button btnRetry;
    private TextView appTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = (WebView) findViewById(R.id.webview);
        progressBar = (ProgressBar) findViewById(R.id.web_progress_bar);
        offlineLayout = (LinearLayout) findViewById(R.id.offline_layout);
        btnBack = (TextView) findViewById(R.id.btn_back);
        btnForward = (TextView) findViewById(R.id.btn_forward);
        btnRefresh = (TextView) findViewById(R.id.btn_refresh);
        btnHome = (TextView) findViewById(R.id.btn_home);
        btnRetry = (Button) findViewById(R.id.btn_retry);
        appTitle = (TextView) findViewById(R.id.app_title);

        setupWebView();
        setupNavigation();
        loadWebsite();
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(0);
                updateNavigationState();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
                updateNavigationState();
                
                // Set the page title on the toolbar if available
                String title = view.getTitle();
                if (title != null && !title.isEmpty()) {
                    appTitle.setText(title);
                } else {
                    appTitle.setText("WebSuite");
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                // If it is a main frame error, show the offline warning
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    if (request.isForMainFrame()) {
                        showOfflineView();
                    }
                } else {
                    showOfflineView();
                }
            }

            @SuppressWarnings("deprecation")
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                // Compatibility layer for older Android versions
                showOfflineView();
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                progressBar.setProgress(newProgress);
                if (newProgress == 100) {
                    progressBar.setVisibility(View.GONE);
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void setupNavigation() {
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
                if (isNetworkAvailable()) {
                    hideOfflineView();
                    webView.reload();
                } else {
                    Toast.makeText(MainActivity.this, "No network connection.", Toast.LENGTH_SHORT).show();
                    showOfflineView();
                }
            }
        });

        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isNetworkAvailable()) {
                    hideOfflineView();
                    webView.loadUrl(TARGET_URL);
                } else {
                    showOfflineView();
                }
            }
        });

        btnRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadWebsite();
            }
        });
    }

    private void loadWebsite() {
        if (isNetworkAvailable()) {
            hideOfflineView();
            webView.loadUrl(TARGET_URL);
        } else {
            showOfflineView();
        }
    }

    private void showOfflineView() {
        webView.setVisibility(View.GONE);
        offlineLayout.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
    }

    private void hideOfflineView() {
        webView.setVisibility(View.VISIBLE);
        offlineLayout.setVisibility(View.GONE);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    private void updateNavigationState() {
        if (webView.canGoBack()) {
            btnBack.setAlpha(1.0f);
            btnBack.setEnabled(true);
        } else {
            btnBack.setAlpha(0.4f);
            btnBack.setEnabled(false);
        }

        if (webView.canGoForward()) {
            btnForward.setAlpha(1.0f);
            btnForward.setEnabled(true);
        } else {
            btnForward.setAlpha(0.4f);
            btnForward.setEnabled(false);
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.getVisibility() == View.VISIBLE && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}