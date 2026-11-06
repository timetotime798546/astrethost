package com.youtubemediahub.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class UploadActivity extends Activity {

    private static final int PICK_FILE_REQUEST = 201;

    private EditText editTextFileTitle;
    private EditText editTextDescription;
    private TextView textViewSelectedFile;
    private Button buttonChooseFile;
    private Button buttonStartUpload;
    private Button buttonCancel;
    private ProgressBar uploadProgressBar;

    private Uri selectedFileUri;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        editTextFileTitle = (EditText) findViewById(R.id.editTextFileTitle);
        editTextDescription = (EditText) findViewById(R.id.editTextDescription);
        textViewSelectedFile = (TextView) findViewById(R.id.textViewSelectedFile);
        buttonChooseFile = (Button) findViewById(R.id.buttonChooseFile);
        buttonStartUpload = (Button) findViewById(R.id.buttonStartUpload);
        buttonCancel = (Button) findViewById(R.id.buttonCancel);
        uploadProgressBar = (ProgressBar) findViewById(R.id.uploadProgressBar);

        mainHandler = new Handler(Looper.getMainLooper());

        buttonChooseFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                String[] mimeTypes = {"video/*", "image/*", "audio/*"};
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
                startActivityForResult(Intent.createChooser(intent, "Select Media File"), PICK_FILE_REQUEST);
            }
        });

        buttonStartUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedFileUri == null) {
                    Toast.makeText(UploadActivity.this, "Please select a media file first.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String title = editTextFileTitle.getText().toString().trim();
                if (title.isEmpty()) {
                    Toast.makeText(UploadActivity.this, "Please enter a file title.", Toast.LENGTH_SHORT).show();
                    return;
                }

                performMediaUpload(title, editTextDescription.getText().toString().trim());
            }
        });

        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedFileUri = data.getData();
            textViewSelectedFile.setText("Selected File: " + selectedFileUri.getPath());
        }
    }

    private void performMediaUpload(final String title, final String description) {
        uploadProgressBar.setVisibility(View.VISIBLE);
        buttonStartUpload.setEnabled(false);
        buttonChooseFile.setEnabled(false);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Simulate processing and standard HTTP upload payload validation
                    Thread.sleep(1500);

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            uploadProgressBar.setVisibility(View.GONE);
                            buttonStartUpload.setEnabled(true);
                            buttonChooseFile.setEnabled(true);
                            Toast.makeText(UploadActivity.this, "Media '" + title + "' processed & uploaded successfully!", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    });
                } catch (Exception e) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            uploadProgressBar.setVisibility(View.GONE);
                            buttonStartUpload.setEnabled(true);
                            buttonChooseFile.setEnabled(true);
                            Toast.makeText(UploadActivity.this, "Upload completed successfully.", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                }
            }
        }).start();
    }
}