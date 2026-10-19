package com.cricketpro3d.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class GameActivity extends Activity implements Cricket3DView.CricketActionListener {

    private Cricket3DView cricket3DView;
    private TextView tvLiveScore, tvOvers, tvEquation, tvRRR, tvOverBalls, tvTarget, tvFeedback, tvMatchMode, tvGestureTip;
    private Button btnToggleElevation, btnNextBall, btnLegSide, btnStraight, btnOffSide, btnHit, btnSoundToggle;

    private SessionManager sessionManager;
    private BackendApi backendApi;
    private SoundManager soundManager;
    private Vibrator vibrator;

    private int totalBallsLimit = 6;
    private int targetRuns = 18;
    private int maxWickets = 2;
    private String modeTitle = "Super Over";

    // Match variables
    private int currentRuns = 0;
    private int currentWickets = 0;
    private int ballsBowled = 0;
    private int matchSixes = 0;
    private int matchFours = 0;

    private boolean isLofted = false;
    private int selectedDirection = 1; // 0: Leg, 1: Straight, 2: Off

    private final List<String> overDeliveries = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        sessionManager = new SessionManager(this);
        backendApi = new BackendApi(this);
        soundManager = SoundManager.getInstance(this);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        totalBallsLimit = getIntent().getIntExtra("TOTAL_BALLS", 6);
        targetRuns = getIntent().getIntExtra("TARGET_RUNS", 18);
        maxWickets = getIntent().getIntExtra("MAX_WICKETS", 2);
        modeTitle = getIntent().getStringExtra("MODE_NAME");
        if (modeTitle == null) modeTitle = "Cricket Match";

        cricket3DView = (Cricket3DView) findViewById(R.id.cricket3DView);
        tvLiveScore = (TextView) findViewById(R.id.tvLiveScore);
        tvOvers = (TextView) findViewById(R.id.tvOvers);
        tvEquation = (TextView) findViewById(R.id.tvEquation);
        tvRRR = (TextView) findViewById(R.id.tvRRR);
        tvOverBalls = (TextView) findViewById(R.id.tvOverBalls);
        tvTarget = (TextView) findViewById(R.id.tvTarget);
        tvFeedback = (TextView) findViewById(R.id.tvFeedback);
        tvMatchMode = (TextView) findViewById(R.id.tvMatchMode);
        tvGestureTip = (TextView) findViewById(R.id.tvGestureTip);

        btnToggleElevation = (Button) findViewById(R.id.btnToggleElevation);
        btnNextBall = (Button) findViewById(R.id.btnNextBall);
        btnLegSide = (Button) findViewById(R.id.btnLegSide);
        btnStraight = (Button) findViewById(R.id.btnStraight);
        btnOffSide = (Button) findViewById(R.id.btnOffSide);
        btnHit = (Button) findViewById(R.id.btnHit);
        btnSoundToggle = (Button) findViewById(R.id.btnSoundToggle);

        tvMatchMode.setText(modeTitle.toUpperCase());
        tvTarget.setText(targetRuns > 0 ? "Target: " + targetRuns + " runs" : "Nets Practice");

        cricket3DView.setCricketActionListener(this);

        updateSoundButtonState();
        updateScoreboard();

        btnSoundToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                soundManager.toggleSound();
                updateSoundButtonState();
            }
        });

        btnNextBall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cricket3DView.startNextDelivery();
            }
        });

        btnHit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cricket3DView.triggerBatSwing();
            }
        });

        btnToggleElevation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLofted = !isLofted;
                btnToggleElevation.setText(isLofted ? "SHOT: LOFTED (AIR)" : "SHOT: GROUND");
                cricket3DView.setShotConfig(selectedDirection, isLofted);
            }
        });

        btnLegSide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedDirection = 0;
                highlightDirection(btnLegSide);
                cricket3DView.setShotConfig(selectedDirection, isLofted);
            }
        });

        btnStraight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedDirection = 1;
                highlightDirection(btnStraight);
                cricket3DView.setShotConfig(selectedDirection, isLofted);
            }
        });

        btnOffSide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedDirection = 2;
                highlightDirection(btnOffSide);
                cricket3DView.setShotConfig(selectedDirection, isLofted);
            }
        });

        // Launch opening delivery
        cricket3DView.postDelayed(new Runnable() {
            @Override
            public void run() {
                cricket3DView.startNextDelivery();
            }
        }, 500);
    }

    private void updateSoundButtonState() {
        boolean enabled = soundManager.isSoundEnabled();
        btnSoundToggle.setText(enabled ? "SFX: ON 🔊" : "SFX: MUTED 🔇");
        btnSoundToggle.setTextColor(enabled ? getResources().getColor(R.color.accent_gold) : 0xFFB0BEC5);
    }

    private void highlightDirection(Button active) {
        btnLegSide.setBackgroundColor(0xFF1B3822);
        btnStraight.setBackgroundColor(0xFF1B3822);
        btnOffSide.setBackgroundColor(0xFF1B3822);
        btnLegSide.setTextColor(0xFFFFFFFF);
        btnStraight.setTextColor(0xFFFFFFFF);
        btnOffSide.setTextColor(0xFFFFFFFF);

        active.setBackgroundColor(0xFFFFD700);
        active.setTextColor(0xFF041A0A);
    }

    private void updateScoreboard() {
        int completedOvers = ballsBowled / 6;
        int remainingBallsInOver = ballsBowled % 6;
        tvOvers.setText(" (" + completedOvers + "." + remainingBallsInOver + " Ovs)");
        tvLiveScore.setText(currentRuns + " / " + currentWickets);

        double strikeRate = ballsBowled > 0 ? ((double) currentRuns / ballsBowled) * 100.0 : 0.0;

        if (targetRuns > 0) {
            int runsNeeded = targetRuns - currentRuns;
            int ballsLeft = totalBallsLimit - ballsBowled;
            if (runsNeeded < 0) runsNeeded = 0;

            tvEquation.setText("Need " + runsNeeded + " off " + ballsLeft + " b");

            double remainingOvers = ballsLeft / 6.0;
            double rrr = remainingOvers > 0 ? (runsNeeded / remainingOvers) : 0.0;
            tvRRR.setText(String.format("RRR: %.1f | SR: %.1f", rrr, strikeRate));
        } else {
            tvEquation.setText("Runs: " + currentRuns);
            double oversFloat = completedOvers + (remainingBallsInOver / 6.0);
            double crr = oversFloat > 0 ? (currentRuns / oversFloat) : 0.0;
            tvRRR.setText(String.format("CRR: %.1f | SR: %.1f", crr, strikeRate));
        }

        StringBuilder sb = new StringBuilder("THIS OVER: [ ");
        for (int i = 0; i < overDeliveries.size(); i++) {
            sb.append(overDeliveries.get(i)).append(" ");
        }
        for (int i = overDeliveries.size(); i < 6; i++) {
            sb.append("- ");
        }
        sb.append("]");
        tvOverBalls.setText(sb.toString());
    }

    @Override
    public void onShotTriggered(final int direction, final boolean lofted) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                selectedDirection = direction;
                isLofted = lofted;
                btnToggleElevation.setText(isLofted ? "SHOT: LOFTED (AIR)" : "SHOT: GROUND");
                if (direction == 0) highlightDirection(btnLegSide);
                else if (direction == 2) highlightDirection(btnOffSide);
                else highlightDirection(btnStraight);
            }
        });
    }

    @Override
    public void onBallCompleted(int runsScored, boolean isWicket, String dismissalType, String shotQuality, int distanceMeters) {
        ballsBowled++;
        currentRuns += runsScored;

        if (runsScored == 6) matchSixes++;
        if (runsScored == 4) matchFours++;

        if (isWicket) {
            currentWickets++;
            overDeliveries.add("W");
            if (vibrator != null) {
                try { vibrator.vibrate(250); } catch (Exception ignored) {}
            }
        } else {
            overDeliveries.add(runsScored == 0 ? "•" : String.valueOf(runsScored));
            if (runsScored >= 4 && vibrator != null) {
                try { vibrator.vibrate(100); } catch (Exception ignored) {}
            }
        }

        if (overDeliveries.size() >= 6) {
            overDeliveries.clear();
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateScoreboard();
                checkMatchEndCondition();
            }
        });
    }

    @Override
    public void onTimingFeedback(final String timingText, final int color) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvFeedback.setText(timingText);
                tvFeedback.setTextColor(color);
                tvFeedback.setVisibility(View.VISIBLE);
                tvFeedback.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        tvFeedback.setVisibility(View.GONE);
                    }
                }, 2200);
            }
        });
    }

    private void checkMatchEndCondition() {
        if (targetRuns <= 0) return;

        boolean won = currentRuns >= targetRuns;
        boolean allOut = currentWickets >= maxWickets;
        boolean ballsExhausted = ballsBowled >= totalBallsLimit;

        if (won || allOut || ballsExhausted) {
            saveCareerStats(won);
            showMatchDialog(won);
        }
    }

    private void saveCareerStats(boolean won) {
        int matches = sessionManager.getMatches() + 1;
        int wins = sessionManager.getWins() + (won ? 1 : 0);
        int totalRuns = sessionManager.getTotalRuns() + currentRuns;
        int highScore = Math.max(sessionManager.getHighScore(), currentRuns);
        int sixes = sessionManager.getSixes() + matchSixes;
        int fours = sessionManager.getFours() + matchFours;

        sessionManager.updateCareer(matches, wins, highScore, totalRuns, sixes, fours);

        String token = sessionManager.getToken();
        String recId = sessionManager.getCareerRecordId();
        if (token != null && !token.isEmpty()) {
            try {
                JSONObject data = new JSONObject();
                data.put("matches_played", matches);
                data.put("matches_won", wins);
                data.put("highest_score", highScore);
                data.put("total_runs", totalRuns);
                data.put("total_sixes", sixes);
                data.put("total_fours", fours);

                if (recId != null && !recId.isEmpty()) {
                    backendApi.updateData(token, recId, data, null);
                } else {
                    backendApi.createData(token, "career_stats", data, null);
                }
            } catch (Exception ignored) {}
        }
    }

    private void showMatchDialog(boolean won) {
        if (won && soundManager != null) {
            soundManager.playCrowdCheer();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(won ? "🏆 MATCH WON!" : "MATCH OVER");
        builder.setMessage(
                (won ? "Sensational batting performance! You chased down the target.\n\n" : "You fell short of the target.\n\n") +
                "Final Score: " + currentRuns + "/" + currentWickets + " (" + (ballsBowled / 6) + "." + (ballsBowled % 6) + " Ovs)\n" +
                "Boundaries: " + matchSixes + " Sixes, " + matchFours + " Fours"
        );
        builder.setPositiveButton("REPLAY", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                currentRuns = 0;
                currentWickets = 0;
                ballsBowled = 0;
                matchSixes = 0;
                matchFours = 0;
                overDeliveries.clear();
                updateScoreboard();
                cricket3DView.startNextDelivery();
            }
        });
        builder.setNegativeButton("MAIN MENU", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.setCancelable(false);
        builder.show();
    }
}