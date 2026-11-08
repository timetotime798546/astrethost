package com.qnapdftoppt.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class QnAEditorActivity extends Activity {

    private EditText etPresentationTitle;
    private LinearLayout slidesContainer;
    private Button btnAddSlide, btnPreviewSlides;
    private ArrayList<QnAItem> qnaItemsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qna_editor);

        etPresentationTitle = findViewById(R.id.etPresentationTitle);
        slidesContainer = findViewById(R.id.slidesContainer);
        btnAddSlide = findViewById(R.id.btnAddSlide);
        btnPreviewSlides = findViewById(R.id.btnPreviewSlides);

        // Add 2 initial slide cards
        addNewSlideCard("What is the speed of light?", "The speed of light in a vacuum is approximately 299,792,458 meters per second (approx 3 x 10^8 m/s).");
        addNewSlideCard("Explain Gravity briefly.", "Gravity is a fundamental physical force that attracts two masses toward each other.");

        btnAddSlide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNewSlideCard("", "");
            }
        });

        btnPreviewSlides.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                collectAndPreview();
            }
        });
    }

    private void addNewSlideCard(String defaultQ, String defaultA) {
        final View cardView = LayoutInflater.from(this).inflate(R.layout.item_qna, slidesContainer, false);
        final TextView tvSlideNumber = cardView.findViewById(R.id.tvSlideNumber);
        final TextView tvRemoveSlide = cardView.findViewById(R.id.tvRemoveSlide);
        final EditText etQuestion = cardView.findViewById(R.id.etQuestion);
        final EditText etAnswer = cardView.findViewById(R.id.etAnswer);

        etQuestion.setText(defaultQ);
        etAnswer.setText(defaultA);

        int slideCount = slidesContainer.getChildCount() + 1;
        tvSlideNumber.setText("Slide #" + (slideCount + 1) + " (Q&A)");

        tvRemoveSlide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (slidesContainer.getChildCount() > 1) {
                    slidesContainer.removeView(cardView);
                    refreshSlideNumbers();
                } else {
                    Toast.makeText(QnAEditorActivity.this, "Presentation must contain at least 1 slide", Toast.LENGTH_SHORT).show();
                }
            }
        });

        slidesContainer.addView(cardView);
    }

    private void refreshSlideNumbers() {
        for (int i = 0; i < slidesContainer.getChildCount(); i++) {
            View card = slidesContainer.getChildAt(i);
            TextView tvNum = card.findViewById(R.id.tvSlideNumber);
            tvNum.setText("Slide #" + (i + 2) + " (Q&A)");
        }
    }

    private void collectAndPreview() {
        qnaItemsList.clear();
        String mainTitle = etPresentationTitle.getText().toString().trim();
        if (mainTitle.isEmpty()) mainTitle = "Question & Answer Presentation";

        for (int i = 0; i < slidesContainer.getChildCount(); i++) {
            View card = slidesContainer.getChildAt(i);
            EditText etQ = card.findViewById(R.id.etQuestion);
            EditText etA = card.findViewById(R.id.etAnswer);

            String q = etQ.getText().toString().trim();
            String a = etA.getText().toString().trim();

            if (!q.isEmpty()) {
                qnaItemsList.add(new QnAItem(i + 1, q, a.isEmpty() ? "No answer specified." : a));
            }
        }

        if (qnaItemsList.isEmpty()) {
            Toast.makeText(this, "Please enter at least one question", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(QnAEditorActivity.this, SlidePreviewActivity.class);
        intent.putExtra("qna_list", qnaItemsList);
        intent.putExtra("title", mainTitle);
        startActivity(intent);
    }
}