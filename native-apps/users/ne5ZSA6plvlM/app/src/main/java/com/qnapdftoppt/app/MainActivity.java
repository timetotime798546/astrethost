package com.qnapdftoppt.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends Activity {

    private Button btnSelectPdf, btnCreateManual, btnLoadSample, btnLogout;
    private TextView tvHeaderTitle, tvEmptyHistory;
    private LinearLayout historyContainer;
    private BackendApi backendApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        backendApi = new BackendApi(this);

        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        btnLogout = findViewById(R.id.btnLogout);
        btnSelectPdf = findViewById(R.id.btnSelectPdf);
        btnCreateManual = findViewById(R.id.btnCreateManual);
        btnLoadSample = findViewById(R.id.btnLoadSample);
        historyContainer = findViewById(R.id.historyContainer);
        tvEmptyHistory = findViewById(R.id.tvEmptyHistory);

        backendApi.fetchAdminConfig(new BackendApi.ConfigCallback() {
            @Override
            public void onConfigLoaded(String appTitle, String watermark) {
                if (tvHeaderTitle != null) tvHeaderTitle.setText(appTitle);
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backendApi.logout();
                Intent intent = new Intent(MainActivity.this, AuthActivity.class);
                startActivity(intent);
                finish();
            }
        });

        btnSelectPdf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, PdfPickerActivity.class);
                startActivity(intent);
            }
        });

        btnCreateManual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, QnAEditorActivity.class);
                startActivity(intent);
            }
        });

        btnLoadSample.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadSampleQnAData();
            }
        });

        loadHistory();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHistory();
    }

    private void loadSampleQnAData() {
        ArrayList<QnAItem> sampleList = new ArrayList<>();
        sampleList.add(new QnAItem(1, "What is Photosynthesis?", "Photosynthesis is the process used by plants to convert light energy into chemical energy stored in glucose. Formula: 6CO₂ + 6H₂O + Light → C₆H₁₂O₆ + 6O₂"));
        sampleList.add(new QnAItem(2, "State Newton's Second Law of Motion.", "Newton's Second Law states that force equals mass times acceleration (F = m * a). The acceleration of an object is directly proportional to the net force."));
        sampleList.add(new QnAItem(3, "Define Data Structures in Computer Science.", "A data structure is a specialized format for organizing, processing, retrieving, and storing data efficiently. Examples include Arrays, Linked Lists, Trees, and Graphs."));
        sampleList.add(new QnAItem(4, "What is the capital city of France?", "Paris is the capital and most populous city of France, located along the Seine River in northern France."));

        Intent intent = new Intent(MainActivity.this, SlidePreviewActivity.class);
        intent.putExtra("qna_list", sampleList);
        intent.putExtra("title", "General Science & Quiz Presentation");
        startActivity(intent);
    }

    private void loadHistory() {
        backendApi.fetchConversionHistory(new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(String responseText, JSONObject jsonResponse) {
                historyContainer.removeAllViews();
                if (jsonResponse == null) return;

                JSONArray records = jsonResponse.optJSONArray("records");
                if (records == null || records.length() == 0) {
                    tvEmptyHistory.setVisibility(View.VISIBLE);
                    return;
                }

                tvEmptyHistory.setVisibility(View.GONE);
                LayoutInflater inflater = LayoutInflater.from(MainActivity.this);

                for (int i = 0; i < records.length(); i++) {
                    JSONObject recObj = records.optJSONObject(i);
                    if (recObj == null) continue;
                    JSONObject data = recObj.optJSONObject("data");
                    if (data == null) continue;

                    String title = data.optString("title", "Question Presentation");
                    int qCount = data.optInt("question_count", 0);

                    // Offline Java logic calculation
                    int totalSlides = qCount + 1;

                    View itemView = inflater.inflate(R.layout.item_history, historyContainer, false);
                    TextView tvTitle = itemView.findViewById(R.id.tvHistoryTitle);
                    TextView tvDetails = itemView.findViewById(R.id.tvHistoryDetails);

                    tvTitle.setText(title);
                    tvDetails.setText(qCount + " Questions • Total " + totalSlides + " Slides");

                    historyContainer.addView(itemView);
                }
            }

            @Override
            public void onError(String errorMessage) {
                // Silently handle offline/empty history
            }
        });
    }
}