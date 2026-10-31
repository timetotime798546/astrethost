package com.cyberrunner.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

public class LeaderboardActivity extends Activity {

    private ScoreManager scoreManager;
    private LinearLayout scoresContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        scoreManager = new ScoreManager(this);
        scoresContainer = (LinearLayout) findViewById(R.id.ll_scores_container);

        Button btnBack = (Button) findViewById(R.id.btn_back);
        Button btnClear = (Button) findViewById(R.id.btn_clear_scores);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scoreManager.clearScores();
                renderLeaderboard();
            }
        });

        renderLeaderboard();
    }

    private void renderLeaderboard() {
        scoresContainer.removeAllViews();
        List<ScoreManager.ScoreEntry> scores = scoreManager.getLeaderboard();

        if (scores.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("NO CYBER RECORDS SAVED YET\nSURVIVE THE GRID TO CLAIM A RECORD");
            emptyText.setTextColor(Color.parseColor("#8E8EB2"));
            emptyText.setTextSize(14f);
            emptyText.setGravity(Gravity.CENTER);
            emptyText.setPadding(0, 60, 0, 0);
            scoresContainer.addView(emptyText);
            return;
        }

        for (int i = 0; i < scores.size(); i++) {
            ScoreManager.ScoreEntry entry = scores.get(i);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(10, 16, 10, 16);
            if (i % 2 == 0) {
                row.setBackgroundColor(Color.parseColor("#121124"));
            } else {
                row.setBackgroundColor(Color.parseColor("#0C0B18"));
            }

            // Rank #
            TextView tvRank = new TextView(this);
            tvRank.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(50), LinearLayout.LayoutParams.WRAP_CONTENT));
            tvRank.setText("#" + (i + 1));
            tvRank.setGravity(Gravity.CENTER);
            tvRank.setTextColor(i == 0 ? Color.parseColor("#FFE600") : Color.parseColor("#00F3FF"));
            tvRank.setTextSize(14f);
            row.addView(tvRank);

            // Score
            TextView tvScore = new TextView(this);
            LinearLayout.LayoutParams scoreParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            tvScore.setLayoutParams(scoreParams);
            tvScore.setText(String.valueOf(entry.score));
            tvScore.setTextColor(Color.WHITE);
            tvScore.setTextSize(14f);
            row.addView(tvScore);

            // Coins
            TextView tvCoins = new TextView(this);
            tvCoins.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(60), LinearLayout.LayoutParams.WRAP_CONTENT));
            tvCoins.setText(String.valueOf(entry.coins));
            tvCoins.setGravity(Gravity.CENTER);
            tvCoins.setTextColor(Color.parseColor("#FFE600"));
            tvCoins.setTextSize(14f);
            row.addView(tvCoins);

            // Date
            TextView tvDate = new TextView(this);
            tvDate.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(100), LinearLayout.LayoutParams.WRAP_CONTENT));
            tvDate.setText(entry.date);
            tvDate.setGravity(Gravity.END);
            tvDate.setTextColor(Color.parseColor("#8E8EB2"));
            tvDate.setTextSize(11f);
            row.addView(tvDate);

            scoresContainer.addView(row);
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }
}