package com.aabinstaller.app;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends Activity {

    private static final int REQ_CHOOSE_FILE = 2001;

    private TextView tvSelectedFileName;
    private TextView tvSelectedFileSize;
    private Button btnSelectAab;
    private Button btnClearFile;

    private LinearLayout cardBundleInfo;
    private TextView tvStatDex;
    private TextView tvStatAssets;
    private TextView tvStatManifest;
    private TextView tvStatAbis;

    private ProgressBar progressConversion;
    private TextView tvProgressStatus;
    private Button btnConvertAab;

    private LinearLayout layoutApkActions;
    private Button btnInstallApk;
    private Button btnShareApk;

    private TextView tvConsoleOutput;
    private TextView tvClearLog;

    private Uri selectedAabUri;
    private File generatedApkFile;
    private AabConverter aabConverter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        aabConverter = new AabConverter(this);
        initViews();
        handleIntentData(getIntent());
    }

    private void initViews() {
        tvSelectedFileName = (TextView) findViewById(R.id.tv_selected_file_name);
        tvSelectedFileSize = (TextView) findViewById(R.id.tv_selected_file_size);
        btnSelectAab = (Button) findViewById(R.id.btn_select_aab);
        btnClearFile = (Button) findViewById(R.id.btn_clear_file);

        cardBundleInfo = (LinearLayout) findViewById(R.id.card_bundle_info);
        tvStatDex = (TextView) findViewById(R.id.tv_stat_dex);
        tvStatAssets = (TextView) findViewById(R.id.tv_stat_assets);
        tvStatManifest = (TextView) findViewById(R.id.tv_stat_manifest);
        tvStatAbis = (TextView) findViewById(R.id.tv_stat_abis);

        progressConversion = (ProgressBar) findViewById(R.id.progress_conversion);
        tvProgressStatus = (TextView) findViewById(R.id.tv_progress_status);
        btnConvertAab = (Button) findViewById(R.id.btn_convert_aab);

        layoutApkActions = (LinearLayout) findViewById(R.id.layout_apk_actions);
        btnInstallApk = (Button) findViewById(R.id.btn_install_apk);
        btnShareApk = (Button) findViewById(R.id.btn_share_apk);

        tvConsoleOutput = (TextView) findViewById(R.id.tv_console_output);
        tvConsoleOutput.setMovementMethod(new ScrollingMovementMethod());
        tvClearLog = (TextView) findViewById(R.id.tv_clear_log);

        btnSelectAab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFilePicker();
            }
        });

        btnClearFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetSelection();
            }
        });

        btnConvertAab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startConversion();
            }
        });

        btnInstallApk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initiateInstall();
            }
        });

        btnShareApk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareConvertedApk();
            }
        });

        tvClearLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvConsoleOutput.setText("");
            }
        });
    }

    private void handleIntentData(Intent intent) {
        if (intent != null && intent.getData() != null) {
            Uri dataUri = intent.getData();
            onFileSelected(dataUri);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntentData(intent);
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "Select .aab or .apks bundle"), REQ_CHOOSE_FILE);
        } catch (Exception e) {
            Toast.makeText(this, "No file manager found on device.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CHOOSE_FILE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                onFileSelected(uri);
            }
        } else if (requestCode == PackageInstallManager.REQ_MANAGE_UNKNOWN_APPS) {
            if (PackageInstallManager.checkInstallPermission(this)) {
                logMessage("Install permission granted.");
                if (generatedApkFile != null && generatedApkFile.exists()) {
                    PackageInstallManager.installApk(this, generatedApkFile);
                }
            } else {
                logMessage("Install permission denied by user.");
                Toast.makeText(this, "Permission to install unknown apps is required.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void onFileSelected(Uri uri) {
        this.selectedAabUri = uri;
        String fileName = queryFileName(uri);
        long fileSize = queryFileSize(uri);

        tvSelectedFileName.setText(fileName);
        tvSelectedFileSize.setText("Size: " + formatSize(fileSize));
        btnConvertAab.setEnabled(true);
        layoutApkActions.setVisibility(View.GONE);
        cardBundleInfo.setVisibility(View.GONE);

        logMessage("Selected bundle: " + fileName + " (" + formatSize(fileSize) + ")");
    }

    private void startConversion() {
        if (selectedAabUri == null) {
            Toast.makeText(this, "Please select an .aab file first.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnConvertAab.setEnabled(false);
        progressConversion.setVisibility(View.VISIBLE);
        progressConversion.setProgress(5);
        tvProgressStatus.setText("Starting conversion process...");
        layoutApkActions.setVisibility(View.GONE);

        aabConverter.inspectAndConvert(selectedAabUri, new AabConverter.ConversionListener() {
            @Override
            public void onProgress(int percentage, String message) {
                progressConversion.setProgress(percentage);
                tvProgressStatus.setText(message);
                logMessage("[" + percentage + "%] " + message);
            }

            @Override
            public void onSuccess(File outputFile, AabBundleInfo info) {
                generatedApkFile = outputFile;
                btnConvertAab.setEnabled(true);
                progressConversion.setVisibility(View.GONE);
                tvProgressStatus.setText("APK Ready for installation!");
                layoutApkActions.setVisibility(View.VISIBLE);

                // Populate bundle info card
                cardBundleInfo.setVisibility(View.VISIBLE);
                tvStatDex.setText(String.valueOf(info.getDexFileCount()));
                tvStatAssets.setText(String.valueOf(info.getAssetFileCount()));
                tvStatManifest.setText(info.isHasManifest() ? "Detected" : "Repackaged");

                if (info.getSupportedAbis().isEmpty()) {
                    tvStatAbis.setText("Native ABIs: Universal (All Architectures)");
                } else {
                    StringBuilder sb = new StringBuilder("Native ABIs: ");
                    for (int i = 0; i < info.getSupportedAbis().size(); i++) {
                        sb.append(info.getSupportedAbis().get(i));
                        if (i < info.getSupportedAbis().size() - 1) {
                            sb.append(", ");
                        }
                    }
                    tvStatAbis.setText(sb.toString());
                }

                logMessage(">> Successfully generated: " + outputFile.getName());
                Toast.makeText(MainActivity.this, "AAB converted to APK successfully!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                btnConvertAab.setEnabled(true);
                progressConversion.setVisibility(View.GONE);
                tvProgressStatus.setText("Conversion failed");
                logMessage("ERROR: " + error);
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void initiateInstall() {
        if (generatedApkFile == null || !generatedApkFile.exists()) {
            Toast.makeText(this, "No converted APK found. Convert an .aab file first.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!PackageInstallManager.checkInstallPermission(this)) {
            logMessage("Requesting INSTALL_PACKAGES permission...");
            PackageInstallManager.requestInstallPermission(this);
            return;
        }

        logMessage("Launching installer for: " + generatedApkFile.getName());
        PackageInstallManager.installApk(this, generatedApkFile);
    }

    private void shareConvertedApk() {
        if (generatedApkFile == null || !generatedApkFile.exists()) {
            Toast.makeText(this, "No converted APK available to share.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Uri contentUri = Uri.parse("content://" + ApkFileProvider.AUTHORITY + "/" + generatedApkFile.getName());
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/vnd.android.package-archive");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share Converted APK"));
        } catch (Exception e) {
            Toast.makeText(this, "Failed to share: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void resetSelection() {
        selectedAabUri = null;
        generatedApkFile = null;
        tvSelectedFileName.setText("No file selected yet (.aab or .apks)");
        tvSelectedFileSize.setText("Size: --");
        btnConvertAab.setEnabled(false);
        progressConversion.setVisibility(View.GONE);
        tvProgressStatus.setText("Ready");
        layoutApkActions.setVisibility(View.GONE);
        cardBundleInfo.setVisibility(View.GONE);
        logMessage("Cleared selection.");
    }

    private void logMessage(String text) {
        tvConsoleOutput.append(text + "\n");
    }

    private String queryFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (idx != -1) {
                            result = cursor.getString(idx);
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result != null ? result : "bundle.aab";
    }

    private long queryFileSize(Uri uri) {
        long size = 0L;
        if ("content".equals(uri.getScheme())) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        int idx = cursor.getColumnIndex(OpenableColumns.SIZE);
                        if (idx != -1) {
                            size = cursor.getLong(idx);
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
        }
        return size;
    }

    private String formatSize(long bytes) {
        if (bytes <= 0) return "0 B";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }
}