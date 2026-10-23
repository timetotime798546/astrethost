package com.perfectcirclechallenge.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

public class MainActivity extends Activity {

    private static final String PREFS_NAME = "perfect_circle_prefs";
    private static final String KEY_BEST_SCORE = "key_best_score";

    private CircleCanvasView circleCanvasView;
    private TextView tvBigScore;
    private TextView tvFeedbackBadge;
    private TextView tvBestScore;
    private TextView tvValRoundness;
    private TextView tvValConsistency;
    private TextView tvValClosure;
    private TextView tvInstruction;
    private Button btnAction;

    private SharedPreferences sharedPreferences;
    private float bestScore = 0.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        bestScore = sharedPreferences.getFloat(KEY_BEST_SCORE, 0.0f);

        initViews();
        setupCallbacks();
        updateBestScoreDisplay();
    }

    private void initViews() {
        circleCanvasView = (CircleCanvasView) findViewById(R.id.circle_canvas);
        tvBigScore = (TextView) findViewById(R.id.tv_big_score);
        tvFeedbackBadge = (TextView) findViewById(R.id.tv_feedback_badge);
        tvBestScore = (TextView) findViewById(R.id.tv_best_score);
        tvValRoundness = (TextView) findViewById(R.id.tv_val_roundness);
        tvValConsistency = (TextView) findViewById(R.id.tv_val_consistency);
        tvValClosure = (TextView) findViewById(R.id.tv_val_closure);
        tvInstruction = (TextView) findViewById(R.id.tv_instruction);
        btnAction = (Button) findViewById(R.id.btn_action);

        btnAction.setText(getString(R.string.reset_button));
    }

    private void setupCallbacks() {
        circleCanvasView.setDrawingCallback(new CircleCanvasView.DrawingCallback() {
            @Override
            public void onDrawingStarted() {
                tvInstruction.setText("Drawing in progress...");
                tvBigScore.setText("Drawing...");
                tvFeedbackBadge.setText("Active");
                btnAction.setText(getString(R.string.reset_button));
            }

            @Override
            public void onDrawingFinished(List<CircleCanvasView.Point> points) {
                // Perform accuracy evaluation locally in native Java
                ScoreCalculator.EvaluationResult result = ScoreCalculator.evaluateCircle(points);

                // Show ideal fitted reference circle
                circleCanvasView.showFittedCircle(result.centroidX, result.centroidY, result.idealRadius);

                // Update UI text and metrics
                tvBigScore.setText(String.format("%.1f%%", result.overallScore));
                tvFeedbackBadge.setText(result.feedbackBadge);
                tvValRoundness.setText(String.format("%.1f%%", result.roundnessScore));
                tvValConsistency.setText(String.format("%.1f%%", result.consistencyScore));
                tvValClosure.setText(String.format("%.1f%%", result.closureScore));
                tvInstruction.setText("Circle analyzed against ideal geometry");

                btnAction.setText(getString(R.string.try_again));

                // Check and persist new best score locally
                if (result.overallScore > bestScore) {
                    bestScore = (float) result.overallScore;
                    sharedPreferences.edit().putFloat(KEY_BEST_SCORE, bestScore).apply();
                    updateBestScoreDisplay();
                }
            }
        });

        btnAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetDrawingState();
            }
        });
    }

    private void resetDrawingState() {
        circleCanvasView.resetCanvas();
        tvBigScore.setText("Ready!");
        tvFeedbackBadge.setText("Start Drawing");
        tvValRoundness.setText("--");
        tvValConsistency.setText("--");
        tvValClosure.setText("--");
        tvInstruction.setText(getString(R.string.prompt_instruction));
        btnAction.setText(getString(R.string.reset_button));
    }

    private void updateBestScoreDisplay() {
        tvBestScore.setText(String.format("%.1f%%", bestScore));
    }
}