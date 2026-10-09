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

    // Dice dot views mapped clockwise
    private View dotTl, dotTc, dotTr;
    private View dotMl, dotMc, dotMr;
    private View dotBl, dotBc, dotBr;

    // Setup configuration
    private Button btnPlayers2, btnPlayers3, btnPlayers4;
    private Button btnReset;

    // Game variables
    private int playerCount = 4;
    private int currentTurn = 0; // 0: Red, 1: Green, 2: Yellow, 3: Blue
    private int diceValue = 1;
    private boolean diceRolled = false;
    private boolean isRolling = false;

    // Token positions: [player][tokenIndex]
    // 0 represents yard, 1..51 path, 52..56 home column, 57 home center (finished)
    private int[][] tokenPositions = new int[4][4];
    private boolean[] playerActive = new boolean[4];
    private boolean[] hasWon = new boolean[4];

    private final Random random = new Random();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private static final int COLOR_RED = 0xFFE53935;
    private static final int COLOR_GREEN = 0xFF43A047;
    private static final int COLOR_YELLOW = 0xFFFFB300;
    private static final int COLOR_BLUE = 0xFF1E88E5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI Setup
        ludoBoardView = (LudoBoardView) findViewById(R.id.ludo_board);
        viewTurnColor = findViewById(R.id.view_turn_color);
        tvTurnIndicator = (TextView) findViewById(R.id.tv_turn_indicator);
        tvRollHint = (TextView) findViewById(R.id.tv_roll_hint);
        btnRollDice = findViewById(R.id.btn_roll_dice);
        btnReset = (Button) findViewById(R.id.btn_reset);

        btnPlayers2 = (Button) findViewById(R.id.btn_players_2);
        btnPlayers3 = (Button) findViewById(R.id.btn_players_3);
        btnPlayers4 = (Button) findViewById(R.id.btn_players_4);

        // Dot views mappings
        dotTl = findViewById(R.id.dot_tl);
        dotTc = findViewById(R.id.dot_tc);
        dotTr = findViewById(R.id.dot_tr);
        dotMl = findViewById(R.id.dot_ml);
        dotMc = findViewById(R.id.dot_mc);
        dotMr = findViewById(R.id.dot_mr);
        dotBl = findViewById(R.id.dot_bl);
        dotBc = findViewById(R.id.dot_bc);
        dotBr = findViewById(R.id.dot_br);

        // Listeners
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
                Toast.makeText(MainActivity.this, "New Match Started!", Toast.LENGTH_SHORT).show();
            }
        });

        btnPlayers2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initNewMatch(2);
                updateSetupButtons();
            }
        });

        btnPlayers3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initNewMatch(3);
                updateSetupButtons();
            }
        });

        btnPlayers4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initNewMatch(4);
                updateSetupButtons();
            }
        });

        // Load state
        if (!loadGameState()) {
            initNewMatch(4); // Default 4 players
        }
        updateSetupButtons();
    }

    private void updateSetupButtons() {
        btnPlayers2.setBackgroundTintList(android.content.res.ColorStateList.valueOf(playerCount == 2 ? 0xFF37474F : 0xFF78909C));
        btnPlayers3.setBackgroundTintList(android.content.res.ColorStateList.valueOf(playerCount == 3 ? 0xFF37474F : 0xFF78909C));
        btnPlayers4.setBackgroundTintList(android.content.res.ColorStateList.valueOf(playerCount == 4 ? 0xFF37474F : 0xFF78909C));
    }

    private void initNewMatch(int playersCount) {
        this.playerCount = playersCount;

        // Reset positions
        for (int p = 0; p < 4; p++) {
            for (int t = 0; t < 4; t++) {
                tokenPositions[p][t] = 0; // Return to Yard
            }
            hasWon[p] = false;
        }

        // Configure player active flags
        if (playersCount == 2) {
            // Standard opposite colors: Red (0) and Yellow (2)
            playerActive[0] = true;
            playerActive[1] = false;
            playerActive[2] = true;
            playerActive[3] = false;
            currentTurn = 0; // Red starts
        } else if (playersCount == 3) {
            playerActive[0] = true;
            playerActive[1] = true;
            playerActive[2] = true;
            playerActive[3] = false;
            currentTurn = 0; // Red starts
        } else {
            playerActive[0] = true;
            playerActive[1] = true;
            playerActive[2] = true;
            playerActive[3] = true;
            currentTurn = 0; // Red starts
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
        tvRollHint.setText("Rolling...");
        
        // Disable dice physical click while rolling
        btnRollDice.setClickable(false);

        // Perform programmatic rolling animation
        final int rollsCount = 10;
        final long rollInterval = 60; // ms

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

                // Final roll result
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

    private void onDiceRollFinished() {
        // Check if there is at least one valid move for the current player
        if (!hasAnyValidMoves(currentTurn, diceValue)) {
            String colorName = getPlayerColorName(currentTurn);
            Toast.makeText(this, "No valid moves for " + colorName + "! Pass turn.", Toast.LENGTH_LONG).show();
            tvRollHint.setText(colorName + " rolled " + diceValue + "! No moves.");

            // Automatic delayed turn skip to give player visual feedback
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    passTurn();
                }
            }, 1800);
        } else {
            tvRollHint.setText("Select a token to move!");
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
        
        // Find next active player who hasn't won yet
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
    public void onTokenSelected(int playerIndex, int tokenIndex) {
        if (!diceRolled || isRolling || playerIndex != currentTurn) return;

        int currentPos = tokenPositions[playerIndex][tokenIndex];
        int nextPos;

        if (currentPos == 0) {
            if (diceValue == 6) {
                nextPos = 1; // Release token to path index 1
            } else {
                return; // Yard tokens only move on rolling 6
            }
        } else {
            nextPos = currentPos + diceValue;
        }

        if (nextPos > 57) return; // Beyond path limit

        // Execute token position change
        tokenPositions[playerIndex][tokenIndex] = nextPos;

        boolean extraRoll = false;
        String colorName = getPlayerColorName(playerIndex);

        // 1. Check if token finished (reached 57)
        if (nextPos == 57) {
            Toast.makeText(this, colorName + " token got home! Extra roll!", Toast.LENGTH_SHORT).show();
            extraRoll = true;
            
            // Check if player has completely finished match
            if (checkPlayerWin(playerIndex)) {
                hasWon[playerIndex] = true;
                Toast.makeText(this, "★ " + colorName + " player completed the match! ★", Toast.LENGTH_LONG).show();
            }
        } else {
            // 2. Check for Capture of opponent token
            LudoBoardView.Cell currentCell = LudoBoardView.playerPaths[playerIndex][nextPos];
            if (!isSafeCell(currentCell.col, currentCell.row)) {
                // Look for opponent tokens sharing the exact same coordinate
                for (int opponent = 0; opponent < 4; opponent++) {
                    if (opponent == playerIndex || !playerActive[opponent]) continue;
                    for (int t = 0; t < 4; t++) {
                        int oPos = tokenPositions[opponent][t];
                        if (oPos > 0) {
                            LudoBoardView.Cell oCell = LudoBoardView.playerPaths[opponent][oPos];
                            if (oCell.col == currentCell.col && oCell.row == currentCell.row) {
                                // Captured! Return captured token to yard position 0
                                tokenPositions[opponent][t] = 0;
                                extraRoll = true;
                                Toast.makeText(this, colorName + " captured " + getPlayerColorName(opponent) + "! Extra roll!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
            }
        }

        // 3. Extra roll for getting a 6
        if (diceValue == 6) {
            extraRoll = true;
            Toast.makeText(this, "Rolled 6! Extra roll!", Toast.LENGTH_SHORT).show();
        }

        // Complete turn phase
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
        // Match ends if everyone or all but one active player have finished
        return activeWon >= activeCount - 1;
    }

    private void updateUI() {
        int activeColor = getPlayerColorValue(currentTurn);
        
        // Update turn indicator color banner
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(activeColor);
        viewTurnColor.setBackground(shape);

        String activeName = getPlayerColorName(currentTurn);
        if (diceRolled) {
            tvTurnIndicator.setText(activeName + " rolled " + diceValue + "! Select token.");
        } else {
            tvTurnIndicator.setText(activeName + "'s Turn! Roll the dice.");
        }

        ludoBoardView.updateState(tokenPositions, playerActive, hasWon, currentTurn, diceValue, diceRolled);
    }

    private void drawDiceValue(int value) {
        // Toggle visibility to construct standard physical dots
        dotTl.setVisibility((value == 4 || value == 5 || value == 6 || value == 2 || value == 3) ? View.VISIBLE : View.INVISIBLE);
        dotTc.setVisibility(View.INVISIBLE); // Never used in standard dice faces
        dotTr.setVisibility((value == 4 || value == 5 || value == 6) ? View.VISIBLE : View.INVISIBLE);
        dotMl.setVisibility((value == 6) ? View.VISIBLE : View.INVISIBLE);
        dotMc.setVisibility((value == 1 || value == 3 || value == 5) ? View.VISIBLE : View.INVISIBLE);
        dotMr.setVisibility((value == 6) ? View.VISIBLE : View.INVISIBLE);
        dotBl.setVisibility((value == 4 || value == 5 || value == 6) ? View.VISIBLE : View.INVISIBLE);
        dotBc.setVisibility(View.INVISIBLE);
        dotBr.setVisibility((value == 4 || value == 5 || value == 6 || value == 2 || value == 3) ? View.VISIBLE : View.INVISIBLE);
        
        // Fix standard visual offset anomalies for face 2 and 3:
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
        // Starts
        if (col == 1 && row == 6) return true;
        if (col == 8 && row == 1) return true;
        if (col == 13 && row == 8) return true;
        if (col == 6 && row == 13) return true;

        // Stars
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