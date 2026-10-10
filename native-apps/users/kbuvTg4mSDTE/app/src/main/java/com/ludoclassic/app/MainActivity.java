package com.ludoclassic.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;

public class MainActivity extends Activity implements LudoBoardView.OnTokenSelectedListener {

    private LudoBoardView ludoBoardView;
    private View viewTurnColor;
    private TextView tvTurnIndicator;
    private TextView tvRollHint;
    private View btnRollDice;

    private View dotTl, dotTc, dotTr;
    private View dotMl, dotMc, dotMr;
    private View dotBl, dotBc, dotBr;

    private Button btnMenu;
    private Button btnReset;

    private int playerCount = 4;
    private int currentTurn = 0; 
    private int diceValue = 1;
    private boolean diceRolled = false;
    private boolean isRolling = false;

    private int[][] tokenPositions = new int[4][4];
    private boolean[] playerActive = new boolean[4];
    private boolean[] hasWon = new boolean[4];

    private final Random random = new Random();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private static final int COLOR_RED = 0xFFEF4444;
    private static final int COLOR_GREEN = 0xFF10B981;
    private static final int COLOR_YELLOW = 0xFFF59E0B;
    private static final int COLOR_BLUE = 0xFF3B82F6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ludoBoardView = (LudoBoardView) findViewById(R.id.ludo_board);
        viewTurnColor = findViewById(R.id.view_turn_color);
        tvTurnIndicator = (TextView) findViewById(R.id.tv_turn_indicator);
        tvRollHint = (TextView) findViewById(R.id.tv_roll_hint);
        btnRollDice = findViewById(R.id.btn_roll_dice);
        btnReset = (Button) findViewById(R.id.btn_reset);
        btnMenu = (Button) findViewById(R.id.btn_menu);

        dotTl = findViewById(R.id.dot_tl);
        dotTc = findViewById(R.id.dot_tc);
        dotTr = findViewById(R.id.dot_tr);
        dotMl = findViewById(R.id.dot_ml);
        dotMc = findViewById(R.id.dot_mc);
        dotMr = findViewById(R.id.dot_mr);
        dotBl = findViewById(R.id.dot_bl);
        dotBc = findViewById(R.id.dot_bc);
        dotBr = findViewById(R.id.dot_br);

        ludoBoardView.setOnTokenSelectedListener(this);

        btnRollDice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!diceRolled && !isRolling) {
                    rollDice();
                }
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initNewMatch(playerCount);
                Toast.makeText(MainActivity.this, "Premium Match Restarted!", Toast.LENGTH_SHORT).show();
            }
        });

        btnMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); 
            }
        });

        int selectedPlayerCount = getIntent().getIntExtra("player_count", -1);
        if (selectedPlayerCount != -1) {
            initNewMatch(selectedPlayerCount);
        } else {
            if (!loadGameState()) {
                initNewMatch(4); 
            }
        }
    }

    private void initNewMatch(int playersCount) {
        this.playerCount = playersCount;

        for (int p = 0; p < 4; p++) {
            for (int t = 0; t < 4; t++) {
                tokenPositions[p][t] = 0; 
            }
            hasWon[p] = false;
        }

        if (playersCount == 2) {
            playerActive[0] = true;
            playerActive[1] = false;
            playerActive[2] = true;
            playerActive[3] = false;
            currentTurn = 0; 
        } else if (playersCount == 3) {
            playerActive[0] = true;
            playerActive[1] = true;
            playerActive[2] = true;
            playerActive[3] = false;
            currentTurn = 0; 
        } else {
            playerActive[0] = true;
            playerActive[1] = true;
            playerActive[2] = true;
            playerActive[3] = true;
            currentTurn = 0; 
        }

        diceValue = 1;
        diceRolled = false;
        isRolling = false;

        drawDiceValue(diceValue);
        updateUI();
        saveGameState();
    }

    private void rollDice() {
        isRolling = true;
        tvRollHint.setText("ROLLING...");
        
        btnRollDice.setClickable(false);

        playDice3DAnimation();

        SoundManager.playSound(1);

        final int rollsCount = 10;
        final long rollInterval = 65; 

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < rollsCount; i++) {
                    final int tempValue = random.nextInt(6) + 1;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            drawDiceValue(tempValue);
                        }
                    });
                    try {
                        Thread.sleep(rollInterval);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                final int finalValue = random.nextInt(6) + 1;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        diceValue = finalValue;
                        drawDiceValue(finalValue);
                        diceRolled = true;
                        isRolling = false;
                        btnRollDice.setClickable(true);
                        
                        onDiceRollFinished();
                    }
                });
            }
        }).start();
    }

    private void playDice3DAnimation() {
        RotateAnimation rotate = new RotateAnimation(
            0, 360,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        );
        rotate.setDuration(450);

        ScaleAnimation scale = new ScaleAnimation(
            1.0f, 1.25f, 1.0f, 1.25f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        );
        scale.setDuration(225);
        scale.setRepeatMode(Animation.REVERSE);
        scale.setRepeatCount(1);

        AnimationSet animSet = new AnimationSet(true);
        animSet.addAnimation(rotate);
        animSet.addAnimation(scale);
        btnRollDice.startAnimation(animSet);
    }

    private void onDiceRollFinished() {
        if (!hasAnyValidMoves(currentTurn, diceValue)) {
            String colorName = getPlayerColorName(currentTurn);
            Toast.makeText(this, "No valid moves for " + colorName + "! Turn skipped.", Toast.LENGTH_LONG).show();
            tvRollHint.setText(colorName + " rolled " + diceValue + " (No Moves)");

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    passTurn();
                }
            }, 1800);
        } else {
            tvRollHint.setText("SELECT TOKEN!");
            updateUI();
        }
    }

    private boolean hasAnyValidMoves(int player, int value) {
        for (int t = 0; t < 4; t++) {
            int pos = tokenPositions[player][t];
            if (pos == 0) {
                if (value == 6) return true;
            } else {
                if (pos + value <= 57) return true;
            }
        }
        return false;
    }

    private void passTurn() {
        diceRolled = false;
        tvRollHint.setText("Tap Dice to Roll");
        
        int nextTurn = currentTurn;
        for (int i = 0; i < 4; i++) {
            nextTurn = (nextTurn + 1) % 4;
            if (playerActive[nextTurn] && !hasWon[nextTurn]) {
                currentTurn = nextTurn;
                break;
            }
        }

        updateUI();
        saveGameState();
    }

    @Override
    public void onTokenSelected(final int playerIndex, final int tokenIndex) {
        if (!diceRolled || isRolling || playerIndex != currentTurn) return;

        int currentPos = tokenPositions[playerIndex][tokenIndex];
        int nextPos;

        if (currentPos == 0) {
            if (diceValue == 6) {
                nextPos = 1; 
            } else {
                return; 
            }
        } else {
            nextPos = currentPos + diceValue;
        }

        if (nextPos > 57) return; 

        diceRolled = false; 

        final int targetPosition = nextPos;
        moveTokenStepByStep(playerIndex, tokenIndex, targetPosition);
    }

    private void moveTokenStepByStep(final int playerIndex, final int tokenIndex, final int targetPos) {
        int currentPos = tokenPositions[playerIndex][tokenIndex];
        if (currentPos == targetPos) {
            onMovementFinished(playerIndex, tokenIndex);
            return;
        }

        final int nextStep;
        if (currentPos == 0) {
            nextStep = 1;
        } else {
            nextStep = currentPos + 1;
        }

        tokenPositions[playerIndex][tokenIndex] = nextStep;
        updateUI();

        SoundManager.playSound(2);

        ludoBoardView.animateTokenHop(playerIndex, tokenIndex, new Runnable() {
            @Override
            public void run() {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        moveTokenStepByStep(playerIndex, tokenIndex, targetPos);
                    }
                }, 90);
            }
        });
    }

    private void onMovementFinished(final int playerIndex, final int tokenIndex) {
        int nextPos = tokenPositions[playerIndex][tokenIndex];
        boolean extraRoll = false;
        String colorName = getPlayerColorName(playerIndex);

        if (nextPos == 57) {
            SoundManager.playSound(4); 
            Toast.makeText(this, "★ " + colorName + " token finished! Extra roll! ★", Toast.LENGTH_SHORT).show();
            extraRoll = true;
            
            if (checkPlayerWin(playerIndex)) {
                hasWon[playerIndex] = true;
                Toast.makeText(this, "★★ " + colorName + " VICTORY! ★★", Toast.LENGTH_LONG).show();
            }
        } else {
            LudoBoardView.Cell currentCell = LudoBoardView.playerPaths[playerIndex][nextPos];
            if (!isSafeCell(currentCell.col, currentCell.row)) {
                for (int opponent = 0; opponent < 4; opponent++) {
                    if (opponent == playerIndex || !playerActive[opponent]) continue;
                    for (int t = 0; t < 4; t++) {
                        int oPos = tokenPositions[opponent][t];
                        if (oPos > 0) {
                            LudoBoardView.Cell oCell = LudoBoardView.playerPaths[opponent][oPos];
                            if (oCell.col == currentCell.col && oCell.row == currentCell.row) {
                                tokenPositions[opponent][t] = 0;
                                extraRoll = true;
                                SoundManager.playSound(3); 
                                Toast.makeText(this, colorName + " CAPTURED " + getPlayerColorName(opponent) + "! EXTRA ROLL!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
            }
        }

        if (diceValue == 6) {
            extraRoll = true;
            Toast.makeText(this, "Rolled 6! Extra roll!", Toast.LENGTH_SHORT).show();
        }

        if (checkMatchOver()) {
            Toast.makeText(this, "Match Finished! Start a new one.", Toast.LENGTH_LONG).show();
            btnRollDice.setClickable(false);
            tvRollHint.setText("Match Finished!");
        } else {
            if (extraRoll && !hasWon[currentTurn]) {
                diceRolled = false;
                tvRollHint.setText("Roll again!");
                updateUI();
                saveGameState();
            } else {
                passTurn();
            }
        }
    }

    private boolean checkPlayerWin(int player) {
        for (int t = 0; t < 4; t++) {
            if (tokenPositions[player][t] != 57) {
                return false;
            }
        }
        return true;
    }

    private boolean checkMatchOver() {
        int activeCount = 0;
        int activeWon = 0;
        for (int p = 0; p < 4; p++) {
            if (playerActive[p]) {
                activeCount++;
                if (hasWon[p]) {
                    activeWon++;
                }
            }
        }
        return activeWon >= activeCount - 1;
    }

    private void updateUI() {
        int activeColor = getPlayerColorValue(currentTurn);
        
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(activeColor);
        viewTurnColor.setBackground(shape);

        String activeName = getPlayerColorName(currentTurn);
        if (diceRolled) {
            tvTurnIndicator.setText(activeName + " rolled " + diceValue + "! Click highlight token.");
        } else {
            tvTurnIndicator.setText(activeName + "'s Turn! Roll dice.");
        }

        ludoBoardView.updateState(tokenPositions, playerActive, hasWon, currentTurn, diceValue, diceRolled);
    }

    private void drawDiceValue(int value) {
        dotTl.setVisibility((value == 4 || value == 5 || value == 6 || value == 2 || value == 3) ? View.VISIBLE : View.INVISIBLE);
        dotTc.setVisibility(View.INVISIBLE); 
        dotTr.setVisibility((value == 4 || value == 5 || value == 6) ? View.VISIBLE : View.INVISIBLE);
        dotMl.setVisibility((value == 6) ? View.VISIBLE : View.INVISIBLE);
        dotMc.setVisibility((value == 1 || value == 3 || value == 5) ? View.VISIBLE : View.INVISIBLE);
        dotMr.setVisibility((value == 6) ? View.VISIBLE : View.INVISIBLE);
        dotBl.setVisibility((value == 4 || value == 5 || value == 6) ? View.VISIBLE : View.INVISIBLE);
        dotBc.setVisibility(View.INVISIBLE);
        dotBr.setVisibility((value == 4 || value == 5 || value == 6 || value == 2 || value == 3) ? View.VISIBLE : View.INVISIBLE);
        
        if (value == 2) {
            dotTl.setVisibility(View.VISIBLE);
            dotBr.setVisibility(View.VISIBLE);
            dotTr.setVisibility(View.INVISIBLE);
            dotBl.setVisibility(View.INVISIBLE);
        } else if (value == 3) {
            dotTl.setVisibility(View.VISIBLE);
            dotMc.setVisibility(View.VISIBLE);
            dotBr.setVisibility(View.VISIBLE);
        }
    }

    private int getPlayerColorValue(int player) {
        switch (player) {
            case 0: return COLOR_RED;
            case 1: return COLOR_GREEN;
            case 2: return COLOR_YELLOW;
            case 3: return COLOR_BLUE;
            default: return Color.BLACK;
        }
    }

    private String getPlayerColorName(int player) {
        switch (player) {
            case 0: return "Red";
            case 1: return "Green";
            case 2: return "Yellow";
            case 3: return "Blue";
            default: return "";
        }
    }

    private boolean isSafeCell(int col, int row) {
        if (col == 1 && row == 6) return true;
        if (col == 8 && row == 1) return true;
        if (col == 13 && row == 8) return true;
        if (col == 6 && row == 13) return true;

        if (col == 8 && row == 6) return true;
        if (col == 6 && row == 8) return true;
        if (col == 2 && row == 6) return true;
        if (col == 8 && row == 2) return true;
        if (col == 12 && row == 8) return true;
        if (col == 6 && row == 12) return true;

        return false;
    }

    private void saveGameState() {
        SharedPreferences pref = getSharedPreferences("ludo_classic_prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt("player_count", playerCount);
        editor.putInt("current_turn", currentTurn);
        editor.putInt("dice_value", diceValue);
        editor.putBoolean("dice_rolled", diceRolled);

        for (int p = 0; p < 4; p++) {
            editor.putBoolean("player_active_" + p, playerActive[p]);
            editor.putBoolean("player_won_" + p, hasWon[p]);
            for (int t = 0; t < 4; t++) {
                editor.putInt("token_p_" + p + "_t_" + t, tokenPositions[p][t]);
            }
        }
        editor.apply();
    }

    private boolean loadGameState() {
        SharedPreferences pref = getSharedPreferences("ludo_classic_prefs", Context.MODE_PRIVATE);
        if (!pref.contains("player_count")) return false;

        playerCount = pref.getInt("player_count", 4);
        currentTurn = pref.getInt("current_turn", 0);
        diceValue = pref.getInt("dice_value", 1);
        diceRolled = pref.getBoolean("dice_rolled", false);

        for (int p = 0; p < 4; p++) {
            playerActive[p] = pref.getBoolean("player_active_" + p, p == 0 || p == 2);
            hasWon[p] = pref.getBoolean("player_won_" + p, false);
            for (int t = 0; t < 4; t++) {
                tokenPositions[p][t] = pref.getInt("token_p_" + p + "_t_" + t, 0);
            }
        }

        drawDiceValue(diceValue);
        updateUI();
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveGameState();
    }
}