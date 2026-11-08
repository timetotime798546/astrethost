package com.qnapdftoppt.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

public class SlidePreviewActivity extends Activity {

    private TextView tvSlideCounter, tvSlideHeader, tvQuestionContent, tvAnswerContent;
    private LinearLayout answerContainer;
    private Button btnPrevSlide, btnNextSlide, btnToggleAnswer, btnExportPptx;

    private ArrayList<QnAItem> qnaList;
    private String presentationTitle = "Q&A Presentation";
    private String adminWatermark = "Created with Q&A PDF to PPT Maker";

    private int currentIndex = 0; // 0 = Title Slide, 1..N = Q&A Slides
    private boolean isAnswerVisible = true;
    private BackendApi backendApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slide_preview);

        backendApi = new BackendApi(this);

        Intent intent = getIntent();
        if (intent != null) {
            qnaList = (ArrayList<QnAItem>) intent.getSerializableExtra("qna_list");
            if (intent.hasExtra("title")) {
                presentationTitle = intent.getStringExtra("title");
            }
        }

        if (qnaList == null || qnaList.isEmpty()) {
            qnaList = new ArrayList<>();
            qnaList.add(new QnAItem(1, "Sample Question", "Sample Answer content for presentation preview."));
        }

        tvSlideCounter = findViewById(R.id.tvSlideCounter);
        tvSlideHeader = findViewById(R.id.tvSlideHeader);
        tvQuestionContent = findViewById(R.id.tvQuestionContent);
        tvAnswerContent = findViewById(R.id.tvAnswerContent);
        answerContainer = findViewById(R.id.answerContainer);
        btnPrevSlide = findViewById(R.id.btnPrevSlide);
        btnNextSlide = findViewById(R.id.btnNextSlide);
        btnToggleAnswer = findViewById(R.id.btnToggleAnswer);
        btnExportPptx = findViewById(R.id.btnExportPptx);

        // Fetch dynamic admin watermark
        backendApi.fetchAdminConfig(new BackendApi.ConfigCallback() {
            @Override
            public void onConfigLoaded(String appTitle, String watermark) {
                adminWatermark = watermark;
            }
        });

        renderCurrentSlide();

        btnPrevSlide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentIndex > 0) {
                    currentIndex--;
                    renderCurrentSlide();
                }
            }
        });

        btnNextSlide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int totalSlides = qnaList.size() + 1;
                if (currentIndex < totalSlides - 1) {
                    currentIndex++;
                    renderCurrentSlide();
                }
            }
        });

        btnToggleAnswer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isAnswerVisible = !isAnswerVisible;
                answerContainer.setVisibility(isAnswerVisible ? View.VISIBLE : View.GONE);
                btnToggleAnswer.setText(isAnswerVisible ? "Hide Answer" : "Show Answer");
            }
        });

        btnExportPptx.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateAndSavePptx();
            }
        });
    }

    private void renderCurrentSlide() {
        int totalSlides = qnaList.size() + 1; // Slide 0 = Title Slide
        tvSlideCounter.setText("Slide " + (currentIndex + 1) + " of " + totalSlides);

        btnPrevSlide.setEnabled(currentIndex > 0);
        btnNextSlide.setEnabled(currentIndex < totalSlides - 1);

        if (currentIndex == 0) {
            // Title Slide
            tvSlideHeader.setText("PRESENTATION TITLE SLIDE");
            tvQuestionContent.setText(presentationTitle);
            answerContainer.setVisibility(View.VISIBLE);
            tvAnswerContent.setText("Total Questions: " + qnaList.size() + "\nGenerated PowerPoint Presentation Deck");
            btnToggleAnswer.setVisibility(View.GONE);
        } else {
            // Q&A Slide
            btnToggleAnswer.setVisibility(View.VISIBLE);
            QnAItem item = qnaList.get(currentIndex - 1);
            tvSlideHeader.setText("QUESTION #" + currentIndex);
            tvQuestionContent.setText(item.getQuestion());
            tvAnswerContent.setText(item.getAnswer());
            answerContainer.setVisibility(isAnswerVisible ? View.VISIBLE : View.GONE);
        }
    }

    private void generateAndSavePptx() {
        try {
            final File pptFile = PptxGenerator.createPresentation(this, presentationTitle, qnaList, adminWatermark);

            // Save record in API database
            backendApi.saveConversionRecord(presentationTitle, qnaList.size(), pptFile.getName(), new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(String responseText, JSONObject jsonResponse) {
                    showExportSuccessDialog(pptFile);
                }

                @Override
                public void onError(String errorMessage) {
                    // Local export still succeeded even if network history sync failed
                    showExportSuccessDialog(pptFile);
                }
            });

        } catch (Exception e) {
            Toast.makeText(this, "PPT Export Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showExportSuccessDialog(final File pptFile) {
        new AlertDialog.Builder(this)
                .setTitle("🎉 PPTX Created Successfully!")
                .setMessage("Your PowerPoint file has been generated:\n\n" + pptFile.getName() + "\n\nLocation: " + pptFile.getAbsolutePath())
                .setPositiveButton("Share / Open PPT", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        openPptFile(pptFile);
                    }
                })
                .setNegativeButton("Back to Home", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(SlidePreviewActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                    }
                })
                .show();
    }

    private void openPptFile(File file) {
        try {
            Uri fileUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/vnd.openxmlformats-officedocument.presentationml.presentation");
            intent.putExtra(Intent.EXTRA_STREAM, fileUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Open / Share PowerPoint Presentation"));
        } catch (Exception e) {
            Toast.makeText(this, "File created at: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        }
    }
}