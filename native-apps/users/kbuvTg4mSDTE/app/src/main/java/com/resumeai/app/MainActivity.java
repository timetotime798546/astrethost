package com.resumeai.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private EditText etJobAndSkills;
    private Button btnGenerate;
    private LinearLayout layoutResultContainer;
    private TextView tabResume;
    private TextView tabCoverLetter;
    private TextView tvGeneratedContent;
    private TextView tvDocumentStatus;
    private Button btnCopy;
    private Button btnShare;

    private TextView chipPreset1;
    private TextView chipPreset2;
    private TextView chipPreset3;
    private TextView chipPreset4;

    private ResumeGeneratorEngine.CareerPackage currentPackage = null;
    private boolean isResumeTabActive = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupListeners();
    }

    private void initViews() {
        etJobAndSkills = (EditText) findViewById(R.id.etJobAndSkills);
        btnGenerate = (Button) findViewById(R.id.btnGenerate);
        layoutResultContainer = (LinearLayout) findViewById(R.id.layoutResultContainer);
        tabResume = (TextView) findViewById(R.id.tabResume);
        tabCoverLetter = (TextView) findViewById(R.id.tabCoverLetter);
        tvGeneratedContent = (TextView) findViewById(R.id.tvGeneratedContent);
        tvDocumentStatus = (TextView) findViewById(R.id.tvDocumentStatus);
        btnCopy = (Button) findViewById(R.id.btnCopy);
        btnShare = (Button) findViewById(R.id.btnShare);

        chipPreset1 = (TextView) findViewById(R.id.chipPreset1);
        chipPreset2 = (TextView) findViewById(R.id.chipPreset2);
        chipPreset3 = (TextView) findViewById(R.id.chipPreset3);
        chipPreset4 = (TextView) findViewById(R.id.chipPreset4);
    }

    private void setupListeners() {
        chipPreset1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etJobAndSkills.setText("Senior Android Engineer, Java, Kotlin, Coroutines, MVVM, Room, Retrofit, Jetpack");
                triggerGeneration();
            }
        });

        chipPreset2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etJobAndSkills.setText("Technical Product Manager, User Research, Agile Scrum, Roadmapping, SQL, A/B Testing, OKRs");
                triggerGeneration();
            }
        });

        chipPreset3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etJobAndSkills.setText("Senior Data Analyst, Python, SQL, Tableau, Predictive Modeling, ETL Pipelines, PowerBI");
                triggerGeneration();
            }
        });

        chipPreset4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etJobAndSkills.setText("Senior UI/UX Designer, Figma, Design Systems, User Journeys, Wireframing, Usability Testing");
                triggerGeneration();
            }
        });

        btnGenerate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerGeneration();
            }
        });

        tabResume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isResumeTabActive = true;
                refreshDisplay();
            }
        });

        tabCoverLetter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isResumeTabActive = false;
                refreshDisplay();
            }
        });

        btnCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyCurrentDocument();
            }
        });

        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareCurrentDocument();
            }
        });
    }

    private void triggerGeneration() {
        String input = etJobAndSkills.getText().toString().trim();
        if (input.isEmpty()) {
            Toast.makeText(this, "Please enter a target role or select a preset.", Toast.LENGTH_SHORT).show();
            return;
        }

        currentPackage = ResumeGeneratorEngine.generate(input);
        layoutResultContainer.setVisibility(View.VISIBLE);
        isResumeTabActive = true;
        refreshDisplay();

        Toast.makeText(this, "Resume and Cover Letter generated!", Toast.LENGTH_SHORT).show();
    }

    private void refreshDisplay() {
        if (currentPackage == null) {
            return;
        }

        if (isResumeTabActive) {
            tabResume.setBackgroundResource(R.drawable.bg_tab_selected);
            tabResume.setTextColor(getResources().getColor(R.color.text_primary));

            tabCoverLetter.setBackgroundResource(R.drawable.bg_tab_unselected);
            tabCoverLetter.setTextColor(getResources().getColor(R.color.text_secondary));

            tvGeneratedContent.setText(currentPackage.getResumeText());
            tvDocumentStatus.setText("Resume Generated • Ready to Copy / Export");
        } else {
            tabCoverLetter.setBackgroundResource(R.drawable.bg_tab_selected);
            tabCoverLetter.setTextColor(getResources().getColor(R.color.text_primary));

            tabResume.setBackgroundResource(R.drawable.bg_tab_unselected);
            tabResume.setTextColor(getResources().getColor(R.color.text_secondary));

            tvGeneratedContent.setText(currentPackage.getCoverLetterText());
            tvDocumentStatus.setText("Cover Letter Generated • Ready to Copy / Export");
        }
    }

    private void copyCurrentDocument() {
        if (currentPackage == null) {
            return;
        }

        String contentToCopy = isResumeTabActive ? currentPackage.getResumeText() : currentPackage.getCoverLetterText();
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("AI Generated Career Document", contentToCopy);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            String label = isResumeTabActive ? "Resume" : "Cover Letter";
            Toast.makeText(this, label + " copied to clipboard!", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareCurrentDocument() {
        if (currentPackage == null) {
            return;
        }

        String content = isResumeTabActive ? currentPackage.getResumeText() : currentPackage.getCoverLetterText();
        String subject = isResumeTabActive ? "Professional Resume" : "Professional Cover Letter";

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, content);
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        sendIntent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(sendIntent, "Share Document via");
        startActivity(shareIntent);
    }
}