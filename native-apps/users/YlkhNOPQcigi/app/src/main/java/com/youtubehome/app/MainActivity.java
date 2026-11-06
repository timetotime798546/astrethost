package com.youtubehome.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final String YOUTUBE_HOME_URL = "https://www.youtube.com/";
    private static final String YOUTUBE_TRENDING_URL = "https://www.youtube.com/feed/trending";
    private static final String YOUTUBE_UPLOAD_URL = "https://www.youtube.com/upload";

    private static final int REQUEST_FILE_CHOOSER = 1001;
    private static final int PERMISSION_STORAGE_CODE = 2002;

    private WebView webView;
    private ProgressBar progressBar;
    private FrameLayout customViewContainer;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private View customView;

    private Button btnUpload;
    private Button btnDownloads;
    private Button btnDesktopToggle;

    private Button btnNavBack;
    private Button btnNavHome;
    private Button btnNavTrending;
    private Button btnNavRefresh;
    private Button btnNavForward;

    private ValueCallback<Uri[]> uploadMessage;
    private boolean isDesktopMode = false;
    private String pendingDownloadUrl;
    private String pendingUserAgent;
    private String pendingContentDisposition;
    private String pendingMimeType;

    private final List<DownloadItem> downloadHistoryList = new ArrayList<DownloadItem>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupWebView();
        setupListeners();

        webView.loadUrl(YOUTUBE_HOME_URL);
    }

    private void initViews() {
        webView = (WebView) findViewById(R.id.webView);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        customViewContainer = (FrameLayout) findViewById(R.id.customViewContainer);

        btnUpload = (Button) findViewById(R.id.btnUpload);
        btnDownloads = (Button) findViewById(R.id.btnDownloads);
        btnDesktopToggle = (Button) findViewById(R.id.btnDesktopToggle);

        btnNavBack = (Button) findViewById(R.id.btnNavBack);
        btnNavHome = (Button) findViewById(R.id.btnNavHome);
        btnNavTrending = (Button) findViewById(R.id.btnNavTrending);
        btnNavRefresh = (Button) findViewById(R.id.btnNavRefresh);
        btnNavForward = (Button) findViewById(R.id.btnNavForward);
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
                updateNavigationButtons();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                    view.loadUrl(url);
                    return true;
                }
                return false;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                progressBar.setProgress(newProgress);
            }

            // File Upload Support for YouTube Studio / Upload
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (uploadMessage != null) {
                    uploadMessage.onReceiveValue(null);
                    uploadMessage = null;
                }
                uploadMessage = filePathCallback;

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                String[] mimetypes = {"video/*", "image/*", "audio/*"};
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);

                try {
                    startActivityForResult(Intent.createChooser(intent, "Select File to Upload"), REQUEST_FILE_CHOOSER);
                } catch (ActivityNotFoundException e) {
                    uploadMessage = null;
                    Toast.makeText(MainActivity.this, "Cannot open file picker", Toast.LENGTH_LONG).show();
                    return false;
                }
                return true;
            }

            // Fullscreen Video Playback Handling
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                customView = view;
                customViewCallback = callback;
                webView.setVisibility(View.GONE);
                customViewContainer.addView(view);
                customViewContainer.setVisibility(View.VISIBLE);
            }

            @Override
            public void onHideCustomView() {
                if (customView == null) return;
                webView.setVisibility(View.VISIBLE);
                customViewContainer.setVisibility(View.GONE);
                customViewContainer.removeView(customView);
                customView = null;
                if (customViewCallback != null) {
                    customViewCallback.onCustomViewHidden();
                }
            }
        });

        // Download Listener Support for YouTube Video / File Downloads
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                pendingDownloadUrl = url;
                pendingUserAgent = userAgent;
                pendingContentDisposition = contentDisposition;
                pendingMimeType = mimetype;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_STORAGE_CODE);
                        return;
                    }
                }
                executeDownload();
            }
        });
    }

    private void setupListeners() {
        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.loadUrl(YOUTUBE_UPLOAD_URL);
            }
        });

        btnDownloads.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDownloadsDialog();
            }
        });

        btnDesktopToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleDesktopMode();
            }
        });

        btnNavBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (webView.canGoBack()) {
                    webView.goBack();
                }
            }
        });

        btnNavHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.loadUrl(YOUTUBE_HOME_URL);
            }
        });

        btnNavTrending.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.loadUrl(YOUTUBE_TRENDING_URL);
            }
        });

        btnNavRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.reload();
            }
        });

        btnNavForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (webView.canGoForward()) {
                    webView.goForward();
                }
            }
        });
    }

    private void toggleDesktopMode() {
        WebSettings settings = webView.getSettings();
        if (!isDesktopMode) {
            String desktopUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36";
            settings.setUserAgentString(desktopUserAgent);
            btnDesktopToggle.setText("Mobile");
            isDesktopMode = true;
            Toast.makeText(this, "Desktop mode enabled", Toast.LENGTH_SHORT).show();
        } else {
            settings.setUserAgentString(null);
            btnDesktopToggle.setText("Desktop");
            isDesktopMode = false;
            Toast.makeText(this, "Mobile mode enabled", Toast.LENGTH_SHORT).show();
        }
        webView.reload();
    }

    private void executeDownload() {
        if (pendingDownloadUrl == null) return;

        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(pendingDownloadUrl));
            request.setMimeType(pendingMimeType);

            String cookies = CookieManager.getInstance().getCookie(pendingDownloadUrl);
            if (cookies != null) {
                request.addRequestHeader("cookie", cookies);
            }
            if (pendingUserAgent != null) {
                request.addRequestHeader("User-Agent", pendingUserAgent);
            }

            request.setDescription("Downloading file from YouTube Home...");

            String fileName = URLUtil.guessFileName(pendingDownloadUrl, pendingContentDisposition, pendingMimeType);
            request.setTitle(fileName);

            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            if (dm != null) {
                dm.enqueue(request);
                Toast.makeText(MainActivity.this, "Downloading " + fileName, Toast.LENGTH_LONG).show();

                String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
                downloadHistoryList.add(0, new DownloadItem(fileName, pendingDownloadUrl, dateStr, "Started"));
            }
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            pendingDownloadUrl = null;
        }
    }

    private void showDownloadsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_downloads, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();

        ListView lvDownloads = (ListView) dialogView.findViewById(R.id.lvDownloads);
        TextView tvEmptyDownloads = (TextView) dialogView.findViewById(R.id.tvEmptyDownloads);
        Button btnCloseDialog = (Button) dialogView.findViewById(R.id.btnCloseDialog);

        if (downloadHistoryList.isEmpty()) {
            tvEmptyDownloads.setVisibility(View.VISIBLE);
            lvDownloads.setVisibility(View.GONE);
        } else {
            tvEmptyDownloads.setVisibility(View.GONE);
            lvDownloads.setVisibility(View.VISIBLE);
            DownloadAdapter adapter = new DownloadAdapter(this, downloadHistoryList);
            lvDownloads.setAdapter(adapter);
        }

        btnCloseDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void updateNavigationButtons() {
        btnNavBack.setEnabled(webView.canGoBack());
        btnNavForward.setEnabled(webView.canGoForward());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_FILE_CHOOSER) {
            if (uploadMessage == null) return;
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null) {
                String dataString = data.getDataString();
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    results = new Uri[count];
                    for (int i = 0; i < count; i++) {
                        results[i] = data.getClipData().getItemAt(i).getUri();
                    }
                } else if (dataString != null) {
                    results = new Uri[]{Uri.parse(dataString)};
                }
            }
            uploadMessage.onReceiveValue(results);
            uploadMessage = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_STORAGE_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                executeDownload();
            } else {
                Toast.makeText(this, "Storage permission required for downloads", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (customView != null) {
            WebChromeClient webChromeClient = webView.getWebChromeClient();
            if (webChromeClient != null) {
                webChromeClient.onHideCustomView();
            }
            return;
        }
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            new AlertDialog.Builder(this)
                .setTitle("Exit YouTube Home?")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setNegativeButton("No", null)
                .show();
        }
    }
}