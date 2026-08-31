package com.ludoclassic.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends Activity {
    public static final int STATE_NEED_ROLL = 1;
    public static final int STATE_NEED_MOVE = 2;
    public static final int STATE_MOVING = 3;
    public static final int STATE_GAME_OVER = 4;

    private int currentTurn = 0; // 0: RED, 1: GREEN, 2: YELLOW, 3: BLUE
    private int gameState = STATE_NEED_ROLL;
    private int diceValue = 1;
    private final int[][] tokenPositions = new int[4][4]; // [player][token]
    private final boolean[] activePlayers = new boolean[4];
    private int playerCount = 4;
    private boolean isRolling = false;
    private final List<Integer> eligibleTokens = new ArrayList<Integer>();

    private LudoBoardView boardView;
    private DiceView diceView;
    private TextView statusText;
    private Button btnRoll;
    private Button btnMode2P;
    private Button btnMode4P;
    private Button btnReset;

    private TextView txtRedScore;
    private TextView txtGreenScore;
    private TextView txtYellowScore;
    private TextView txtBlueScore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        boardView = (LudoBoardView) findViewById(R.id.ludoBoard);
        diceView = (DiceView) findViewById(R.id.diceView);
        statusText = (TextView) findViewById(R.id.statusText);
        btnRoll = (Button) findViewById(R.id.btnRoll);
        btnMode2P = (Button) findViewById(R.id.btnMode2P);
        btnMode4P = (Button) findViewById(R.id.btnMode4P);
        btnReset = (Button) findViewById(R.id.btnReset);

        txtRedScore = (TextView) findViewById(R.id.txtRedScore);
        txtGreenScore = (TextView) findViewById(R.id.txtGreenScore);
        txtYellowScore = (TextView) findViewById(R.id.txtYellowScore);
        txtBlueScore = (TextView) findViewById(R.id.txtBlueScore);

        btnRoll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rollDice();
            }
        });

        diceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rollDice();
            }
        });

        btnMode2P.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initGame(2);
            }
        });

        btnMode4P.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initGame(4);
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initGame(playerCount);
            }
        });

        boardView.setCellClickListener(new LudoBoardView.CellClickListener() {
            @Override
            public void onCellClicked(int col, int row) {
                handleBoardClick(col, row);
            }
        });

        initGame(4);
    }

    private void initGame(int mode) {
        playerCount = mode;
        if (playerCount == 2) {
            activePlayers[0] = true;  // RED
            activePlayers[1] = false; // GREEN
            activePlayers[2] = true;  // YELLOW
            activePlayers[3] = false; // BLUE
            btnMode2P.setBackgroundColor(Color.parseColor("#37474F"));
            btnMode2P.setTextColor(Color.WHITE);
            btnMode4P.setBackgroundColor(Color.parseColor("#CFD8DC"));
            btnMode4P.setTextColor(Color.BLACK);
        } else {
            activePlayers[0] = true;  // RED
            activePlayers[1] = true;  // GREEN
            activePlayers[2] = true;  // YELLOW
            activePlayers[3] = true;  // BLUE
            btnMode4P.setBackgroundColor(Color.parseColor("#37474F"));
            btnMode4P.setTextColor(Color.WHITE);
            btnMode2P.setBackgroundColor(Color.parseColor("#CFD8DC"));
            btnMode2P.setTextColor(Color.BLACK);
        }

        currentTurn = 0;
        gameState = STATE_NEED_ROLL;
        diceValue = 1;
        eligibleTokens.clear();

        for (int p = 0; p < 4; p++) {
            for (int t = 0; t < 4; t++) {
                tokenPositions[p][t] = -1; // inside Yard
            }
        }
        updateUI();
    }

    private void rollDice() {
        if (gameState != STATE_NEED_ROLL || isRolling) return;
        isRolling = true;
        btnRoll.setEnabled(false);
        diceView.setRolling(true);

        final Random random = new Random();
        final Handler handler = new Handler(Looper.getMainLooper());
        final int[] count = {0};

        final Runnable rollRunnable = new Runnable() {
            @Override
            public void run() {
                if (count[0] < 8) {
                    diceValue = random.nextInt(6) + 1;
                    diceView.setValue(diceValue);
                    count[0]++;
                    handler.postDelayed(this, 60);
                } else {
                    diceValue = random.nextInt(6) + 1;
                    diceView.setValue(diceValue);
                    diceView.setRolling(false);
                    isRolling = false;
                    btnRoll.setEnabled(true);
                    onDiceRollComplete();
                }
            }
        };
        handler.post(rollRunnable);
    }

    private void onDiceRollComplete() {
        eligibleTokens.clear();
        for (int t = 0; t < 4; t++) {
            int pos = tokenPositions[currentTurn][t];
            if (pos == -1) {
                if (diceValue == 6) {
                    eligibleTokens.add(t);
                }
            } else if (pos >= 0 && pos < 56) {
                if (pos + diceValue <= 56) {
                    eligibleTokens.add(t);
                }
            }
        }

        if (eligibleTokens.isEmpty()) {
            statusText.setText(getPlayerName(currentTurn) + " rolled " + diceValue + ". No moves available!");
            statusText.setBackgroundColor(getPlayerSoftColor(currentTurn));
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    nextTurn();
                }
            }, 1500);
        } else {
            gameState = STATE_NEED_MOVE;
            statusText.setText(getPlayerName(currentTurn) + " rolled " + diceValue + "! Tap a token to move.");
            statusText.setBackgroundColor(getPlayerSoftColor(currentTurn));
            boardView.setGameState(tokenPositions, currentTurn, gameState, eligibleTokens);
        }
    }

    private void handleBoardClick(int col, int row) {
        if (gameState != STATE_NEED_MOVE) return;

        int clickedToken = -1;
        for (int t : eligibleTokens) {
            int[] coords = boardView.getTokenCoords(currentTurn, t);
            if (coords[0] == col && coords[1] == row) {
                clickedToken = t;
                break;
            }
        }

        if (clickedToken != -1) {
            int currentPos = tokenPositions[currentTurn][clickedToken];
            int targetPos = (currentPos == -1) ? 0 : currentPos + diceValue;
            animateMove(currentTurn, clickedToken, targetPos);
        }
    }

    private void animateMove(final int player, final int token, final int targetPos) {
        gameState = STATE_MOVING;
        boardView.setGameState(tokenPositions, currentTurn, gameState, eligibleTokens);
        final Handler handler = new Handler(Looper.getMainLooper());
        
        handler.post(new Runnable() {
            @Override
            public void run() {
                int currentPos = tokenPositions[player][token];
                if (currentPos < targetPos) {
                    if (currentPos == -1) {
                        tokenPositions[player][token] = 0;
                    } else {
                        tokenPositions[player][token] = currentPos + 1;
                    }
                    boardView.invalidate();
                    boardView.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
                    handler.postDelayed(this, 180);
                } else {
                    onMoveComplete(player, token);
                }
            }
        });
    }

    private void onMoveComplete(int player, int token) {
        int finalPos = tokenPositions[player][token];
        boolean extraTurn = (diceValue == 6);

        if (finalPos >= 0 && finalPos <= 50) {
            int boardIdx = (boardView.startIndices[player] + finalPos) % 52;
            if (!boardView.isSafeBoardIndex(boardIdx)) {
                for (int op = 0; op < 4; op++) {
                    if (op == player || !activePlayers[op]) continue;
                    for (int ot = 0; ot < 4; ot++) {
                        int opos = tokenPositions[op][ot];
                        if (opos >= 0 && opos <= 50) {
                            int oppBoardIdx = (boardView.startIndices[op] + opos) % 52;
                            if (oppBoardIdx == boardIdx) {
                                tokenPositions[op][ot] = -1;
                                extraTurn = true;
                                Toast.makeText(this, getPlayerName(player) + " captured " + getPlayerName(op) + "'s token!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
            }
        }

        if (finalPos == 56) {
            extraTurn = true;
            Toast.makeText(this, getPlayerName(player) + " got a token Home!", Toast.LENGTH_SHORT).show();
        }

        checkWinner();
        if (gameState == STATE_GAME_OVER) return;

        if (extraTurn) {
            gameState = STATE_NEED_ROLL;
            eligibleTokens.clear();
            updateUI();
        } else {
            nextTurn();
        }
    }

    private void nextTurn() {
        int attempts = 0;
        do {
            currentTurn = (currentTurn + 1) % 4;
            attempts++;
        } while (!activePlayers[currentTurn] && attempts < 4);

        gameState = STATE_NEED_ROLL;
        eligibleTokens.clear();
        updateUI();
    }

    private void checkWinner() {
        for (int p = 0; p < 4; p++) {
            if (!activePlayers[p]) continue;
            boolean won = true;
            for (int t = 0; t < 4; t++) {
                if (tokenPositions[p][t] != 56) {
                    won = false;
                    break;
                }
            }
            if (won) {
                gameState = STATE_GAME_OVER;
                statusText.setText(getPlayerName(p) + " WINS THE GAME!!!");
                statusText.setBackgroundColor(Color.parseColor("#4CAF50"));
                Toast.makeText(this, getPlayerName(p) + " is the Champion!", Toast.LENGTH_LONG).show();
                break;
            }
        }
    }

    private void updateUI() {
        if (gameState == STATE_NEED_ROLL) {
            statusText.setText(getPlayerName(currentTurn) + "'s Turn: Tap ROLL DICE");
            statusText.setBackgroundColor(getPlayerSoftColor(currentTurn));
        }

        txtRedScore.setText(getHomeCount(0) + "/4 Home");
        txtGreenScore.setText(getHomeCount(1) + "/4 Home");
        txtYellowScore.setText(getHomeCount(2) + "/4 Home");
        txtBlueScore.setText(getHomeCount(3) + "/4 Home");

        findViewById(R.id.statRed).setBackgroundColor(currentTurn == 0 ? Color.parseColor("#FFCDD2") : Color.parseColor("#FFEBEE"));
        findViewById(R.id.statGreen).setBackgroundColor(currentTurn == 1 ? Color.parseColor("#C8E6C9") : Color.parseColor("#E8F5E9"));
        findViewById(R.id.statYellow).setBackgroundColor(currentTurn == 2 ? Color.parseColor("#FFF9C4") : Color.parseColor("#FFFDE7"));
        findViewById(R.id.statBlue).setBackgroundColor(currentTurn == 3 ? Color.parseColor("#BBDEFB") : Color.parseColor("#E3F2FD"));

        findViewById(R.id.statGreen).setVisibility(activePlayers[1] ? View.VISIBLE : View.GONE);
        findViewById(R.id.statBlue).setVisibility(activePlayers[3] ? View.VISIBLE : View.GONE);

        boardView.setGameState(tokenPositions, currentTurn, gameState, eligibleTokens);
        diceView.setValue(diceValue);
    }

    private int getHomeCount(int player) {
        int count = 0;
        for (int t = 0; t < 4; t++) {
            if (tokenPositions[player][t] == 56) {
                count++;
            }
        }
        return count;
    }

    private String getPlayerName(int p) {
        switch (p) {
            case 0: return "RED";
            case 1: return "GREEN";
            case 2: return "YELLOW";
            case 3: return "BLUE";
            default: return "";
        }
    }

    private int getPlayerSoftColor(int p) {
        switch (p) {
            case 0: return Color.parseColor("#EF5350");
            case 1: return Color.parseColor("#66BB6A");
            case 2: return Color.parseColor("#FDD835");
            case 3: return Color.parseColor("#42A5F5");
            default: return Color.GRAY;
        }
    }
}

class LudoBoardView extends View {
    public final int[] startIndices = {0, 13, 26, 39};
    private int[][] tokenPositions = new int[4][4];
    private int currentTurn = 0;
    private int gameState = 1;
    private List<Integer> eligibleTokens = new ArrayList<Integer>();
    private CellClickListener cellClickListener;
    private float cellSize;

    private final int[][] trackCoords = {
        {1, 6}, {2, 6}, {3, 6}, {4, 6}, {5, 6},
        {6, 5}, {6, 4}, {6, 3}, {6, 2}, {6, 1}, {6, 0},
        {7, 0},
        {8, 0}, {8, 1}, {8, 2}, {8, 3}, {8, 4}, {8, 5},
        {9, 6}, {10, 6}, {11, 6}, {12, 6}, {13, 6}, {14, 6},
        {14, 7},
        {14, 8}, {13, 8}, {12, 8}, {11, 8}, {10, 8}, {9, 8},
        {8, 9}, {8, 10}, {8, 11}, {8, 12}, {8, 13}, {8, 14},
        {7, 14},
        {6, 14}, {6, 13}, {6, 12}, {6, 11}, {6, 10}, {6, 9},
        {5, 8}, {4, 8}, {3, 8}, {2, 8}, {1, 8}, {0, 8},
        {0, 7},
        {0, 6}
    };

    public interface CellClickListener {
        void onCellClicked(int col, int row);
    }

    public LudoBoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setGameState(int[][] positions, int turn, int state, List<Integer> eligible) {
        for (int p = 0; p < 4; p++) {
            System.arraycopy(positions[p], 0, this.tokenPositions[p], 0, 4);
        }
        this.currentTurn = turn;
        this.gameState = state;
        this.eligibleTokens = eligible;
        invalidate();
    }

    public void setCellClickListener(CellClickListener listener) {
        this.cellClickListener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int size = Math.min(getMeasuredWidth(), getMeasuredHeight());
        if (size <= 0) {
            size = getResources().getDisplayMetrics().widthPixels;
        }
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        cellSize = (float) getWidth() / 15f;
        drawBoardGrid(canvas);
        drawYards(canvas);
        drawCenterTriangles(canvas);
        drawStars(canvas);
        drawTokens(canvas);
    }

    private void drawBoardGrid(Canvas canvas) {
        Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setColor(Color.parseColor("#BDBDBD"));
        strokePaint.setStrokeWidth(1.5f);

        for (int r = 0; r < 15; r++) {
            for (int c = 0; c < 15; c++) {
                if (isYardArea(c, r) || isCenterArea(c, r)) continue;

                float left = c * cellSize;
                float top = r * cellSize;
                
                fillPaint.setColor(Color.WHITE);
                if (r == 7 && c >= 1 && c <= 5) fillPaint.setColor(Color.parseColor("#EF5350")); // RED Home
                else if (c == 1 && r == 6) fillPaint.setColor(Color.parseColor("#EF5350")); // RED start
                else if (c == 7 && r >= 1 && r <= 5) fillPaint.setColor(Color.parseColor("#66BB6A")); // GREEN Home
                else if (c == 8 && r == 1) fillPaint.setColor(Color.parseColor("#66BB6A")); // GREEN start
                else if (r == 7 && c >= 9 && c <= 13) fillPaint.setColor(Color.parseColor("#FDD835")); // YELLOW Home
                else if (c == 13 && r == 8) fillPaint.setColor(Color.parseColor("#FDD835")); // YELLOW start
                else if (c == 7 && r >= 9 && r <= 13) fillPaint.setColor(Color.parseColor("#42A5F5")); // BLUE Home
                else if (c == 6 && r == 13) fillPaint.setColor(Color.parseColor("#42A5F5")); // BLUE start

                canvas.drawRect(left, top, left + cellSize, top + cellSize, fillPaint);
                canvas.drawRect(left, top, left + cellSize, top + cellSize, strokePaint);
            }
        }
    }

    private void drawYards(Canvas canvas) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        
        drawYardBase(canvas, 0, 0, "#FFEBEE", "#EF5350", paint); // Top-Left RED
        drawYardBase(canvas, 9, 0, "#E8F5E9", "#66BB6A", paint); // Top-Right GREEN
        drawYardBase(canvas, 9, 9, "#FFFDE7", "#FDD835", paint); // Bottom-Right YELLOW
        drawYardBase(canvas, 0, 9, "#E3F2FD", "#42A5F5", paint); // Bottom-Left BLUE
    }

    private void drawYardBase(Canvas canvas, int col, int row, String bgColor, String strokeColor, Paint paint) {
        float left = col * cellSize;
        float top = row * cellSize;
        float right = left + 6 * cellSize;
        float bottom = top + 6 * cellSize;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor(bgColor));
        canvas.drawRect(left, top, right, bottom, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.parseColor(strokeColor));
        paint.setStrokeWidth(5f);
        canvas.drawRect(left, top, right, bottom, paint);

        // Draw inner box
        float offset = cellSize;
        paint.setStrokeWidth(3f);
        canvas.drawRect(left + offset, top + offset, right - offset, bottom - offset, paint);

        // Draw token spots
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(left + 2f * cellSize, top + 2f * cellSize, cellSize * 0.4f, paint);
        canvas.drawCircle(left + 4f * cellSize, top + 2f * cellSize, cellSize * 0.4f, paint);
        canvas.drawCircle(left + 2f * cellSize, top + 4f * cellSize, cellSize * 0.4f, paint);
        canvas.drawCircle(left + 4f * cellSize, top + 4f * cellSize, cellSize * 0.4f, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.parseColor(strokeColor));
        canvas.drawCircle(left + 2f * cellSize, top + 2f * cellSize, cellSize * 0.4f, paint);
        canvas.drawCircle(left + 4f * cellSize, top + 2f * cellSize, cellSize * 0.4f, paint);
        canvas.drawCircle(left + 2f * cellSize, top + 4f * cellSize, cellSize * 0.4f, paint);
        canvas.drawCircle(left + 4f * cellSize, top + 4f * cellSize, cellSize * 0.4f, paint);
    }

    private void drawCenterTriangles(Canvas canvas) {
        float cx = 7.5f * cellSize;
        float cy = 7.5f * cellSize;
        float x1 = 6f * cellSize;
        float y1 = 6f * cellSize;
        float x2 = 9f * cellSize;
        float y2 = 9f * cellSize;

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);

        // RED (Left)
        paint.setColor(Color.parseColor("#EF5350"));
        Path path = new Path();
        path.moveTo(x1, y1);
        path.lineTo(cx, cy);
        path.lineTo(x1, x2);
        path.close();
        canvas.drawPath(path, paint);

        // GREEN (Top)
        paint.setColor(Color.parseColor("#66BB6A"));
        path.reset();
        path.moveTo(x1, y1);
        path.lineTo(x2, y1);
        path.lineTo(cx, cy);
        path.close();
        canvas.drawPath(path, paint);

        // YELLOW (Right)
        paint.setColor(Color.parseColor("#FDD835"));
        path.reset();
        path.moveTo(x2, y1);
        path.lineTo(x2, x2);
        path.lineTo(cx, cy);
        path.close();
        canvas.drawPath(path, paint);

        // BLUE (Bottom)
        paint.setColor(Color.parseColor("#42A5F5"));
        path.reset();
        path.moveTo(x1, x2);
        path.lineTo(x2, x2);
        path.lineTo(cx, cy);
        path.close();
        canvas.drawPath(path, paint);

        // Draw central outline borders
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(Color.parseColor("#757575"));
        borderPaint.setStrokeWidth(3f);
        canvas.drawLine(x1, y1, x2, x2, borderPaint);
        canvas.drawLine(x1, x2, x2, y1, borderPaint);
        canvas.drawRect(x1, y1, x2, x2, borderPaint);
    }

    private void drawStars(Canvas canvas) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.parseColor("#757575"));
        paint.setStyle(Paint.Style.FILL);

        int[][] stars = {
            {1, 6}, {2, 8}, {8, 1}, {6, 2}, {13, 8}, {12, 6}, {6, 13}, {8, 12}
        };
        for (int[] star : stars) {
            float cx = star[0] * cellSize + cellSize / 2f;
            float cy = star[1] * cellSize + cellSize / 2f;
            drawStar(canvas, cx, cy, cellSize * 0.28f, paint);
        }
    }

    private void drawStar(Canvas canvas, float cx, float cy, float radius, Paint paint) {
        Path path = new Path();
        double angle = Math.PI / 5;
        for (int i = 0; i < 10; i++) {
            float r = (i % 2 == 0) ? radius : radius * 0.4f;
            float currAngle = (float) (i * angle - Math.PI / 2);
            float x = (float) (cx + Math.cos(currAngle) * r);
            float y = (float) (cy + Math.sin(currAngle) * r);
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        path.close();
        canvas.drawPath(path, paint);
    }

    private void drawTokens(Canvas canvas) {
        boolean[][] tokenDrawn = new boolean[4][4];

        for (int p = 0; p < 4; p++) {
            for (int t = 0; t < 4; t++) {
                if (tokenDrawn[p][t]) continue;
                int pos = tokenPositions[p][t];

                if (pos == -1) {
                    int[] coords = getTokenCoords(p, t);
                    boolean eligible = isTokenEligible(p, t);
                    drawSingleToken(canvas, coords[0], coords[1], p, t, eligible);
                    tokenDrawn[p][t] = true;
                    continue;
                }

                if (pos == 56) {
                    drawHomeToken(canvas, p, t);
                    tokenDrawn[p][t] = true;
                    continue;
                }

                int[] coords = getTokenCoords(p, t);
                int col = coords[0];
                int row = coords[1];

                List<int[]> cluster = new ArrayList<int[]>();
                cluster.add(new int[]{p, t});

                for (int op = 0; op < 4; op++) {
                    for (int ot = 0; ot < 4; ot++) {
                        if (op == p && ot == t) continue;
                        if (tokenDrawn[op][ot]) continue;
                        int opos = tokenPositions[op][ot];
                        if (opos >= 0 && opos < 56) {
                            int[] ocoords = getTokenCoords(op, ot);
                            if (ocoards[0] == col && ocoords[1] == row) {
                                cluster.add(new int[]{op, ot});
                            }
                        }
                    }
                }

                drawTokenCluster(canvas, col, row, cluster);
                for (int[] info : cluster) {
                    tokenDrawn[info[0]][info[1]] = true;
                }
            }
        }
    }

    private void drawSingleToken(Canvas canvas, int col, int row, int player, int token, boolean eligible) {
        float cx = col * cellSize + cellSize / 2f;
        float cy = row * cellSize + cellSize / 2f;
        float radius = cellSize * 0.35f;
        drawTokenAt(canvas, cx, cy, radius, player, token, eligible);
    }

    private void drawTokenAt(Canvas canvas, float cx, float cy, float radius, int player, int token, boolean eligible) {
        Paint pPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int pColor = getPlayerColor(player);

        if (eligible) {
            Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            glowPaint.setColor(Color.WHITE);
            glowPaint.setStyle(Paint.Style.STROKE);
            glowPaint.setStrokeWidth(dpToPx(4));
            canvas.drawCircle(cx, cy, radius + dpToPx(3), glowPaint);

            glowPaint.setColor(Color.YELLOW);
            glowPaint.setStrokeWidth(dpToPx(2));
            canvas.drawCircle(cx, cy, radius + dpToPx(3), glowPaint);
        }

        pPaint.setColor(Color.BLACK);
        pPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy, radius, pPaint);

        pPaint.setColor(pColor);
        canvas.drawCircle(cx, cy, radius - dpToPx(1.5f), pPaint);

        pPaint.setColor(Color.WHITE);
        canvas.drawCircle(cx - radius * 0.25f, cy - radius * 0.25f, radius * 0.2f, pPaint);

        pPaint.setColor(Color.WHITE);
        pPaint.setTextSize(radius * 0.9f);
        pPaint.setTextAlign(Paint.Align.CENTER);
        pPaint.setFakeBoldText(true);
        canvas.drawText(String.valueOf(token + 1), cx, cy + radius * 0.32f, pPaint);
    }

    private void drawTokenCluster(Canvas canvas, int col, int row, List<int[]> cluster) {
        float cx = col * cellSize + cellSize / 2f;
        float cy = row * cellSize + cellSize / 2f;
        int n = cluster.size();

        if (n == 1) {
            int p = cluster.get(0)[0];
            int t = cluster.get(0)[1];
            boolean eligible = isTokenEligible(p, t);
            drawSingleToken(canvas, col, row, p, t, eligible);
        } else if (n == 2) {
            float r = cellSize * 0.25f;
            for (int i = 0; i < 2; i++) {
                int p = cluster.get(i)[0];
                int t = cluster.get(i)[1];
                boolean eligible = isTokenEligible(p, t);
                float dx = (i == 0) ? -cellSize * 0.18f : cellSize * 0.18f;
                drawTokenAt(canvas, cx + dx, cy, r, p, t, eligible);
            }
        } else if (n == 3) {
            float r = cellSize * 0.22f;
            float[][] offsets = {
                {-cellSize * 0.18f, -cellSize * 0.15f},
                {cellSize * 0.18f, -cellSize * 0.15f},
                {0, cellSize * 0.18f}
            };
            for (int i = 0; i < 3; i++) {
                int p = cluster.get(i)[0];
                int t = cluster.get(i)[1];
                boolean eligible = isTokenEligible(p, t);
                drawTokenAt(canvas, cx + offsets[i][0], cy + offsets[i][1], r, p, t, eligible);
            }
        } else {
            float r = cellSize * 0.2f;
            float[][] offsets = {
                {-cellSize * 0.2f, -cellSize * 0.2f},
                {cellSize * 0.2f, -cellSize * 0.2f},
                {-cellSize * 0.2f, cellSize * 0.2f},
                {cellSize * 0.2f, cellSize * 0.2f}
            };
            for (int i = 0; i < Math.min(n, 4); i++) {
                int p = cluster.get(i)[0];
                int t = cluster.get(i)[1];
                boolean eligible = isTokenEligible(p, t);
                drawTokenAt(canvas, cx + offsets[i][0], cy + offsets[i][1], r, p, t, eligible);
            }
        }
    }

    private void drawHomeToken(Canvas canvas, int player, int token) {
        float rx = 0, ry = 0;
        float r = cellSize * 0.18f;
        switch (player) {
            case 0: // RED
                if (token == 0) { rx = 6.3f * cellSize; ry = 6.8f * cellSize; }
                else if (token == 1) { rx = 6.3f * cellSize; ry = 8.2f * cellSize; }
                else if (token == 2) { rx = 6.8f * cellSize; ry = 7.2f * cellSize; }
                else { rx = 6.8f * cellSize; ry = 7.8f * cellSize; }
                break;
            case 1: // GREEN
                if (token == 0) { rx = 6.8f * cellSize; ry = 6.3f * cellSize; }
                else if (token == 1) { rx = 8.2f * cellSize; ry = 6.3f * cellSize; }
                else if (token == 2) { rx = 7.2f * cellSize; ry = 6.8f * cellSize; }
                else { rx = 7.8f * cellSize; ry = 6.8f * cellSize; }
                break;
            case 2: // YELLOW
                if (token == 0) { rx = 8.7f * cellSize; ry = 6.8f * cellSize; }
                else if (token == 1) { rx = 8.7f * cellSize; ry = 8.2f * cellSize; }
                else if (token == 2) { rx = 8.2f * cellSize; ry = 7.2f * cellSize; }
                else { rx = 8.2f * cellSize; ry = 7.8f * cellSize; }
                break;
            case 3: // BLUE
                if (token == 0) { rx = 6.8f * cellSize; ry = 8.7f * cellSize; }
                else if (token == 1) { rx = 8.2f * cellSize; ry = 8.7f * cellSize; }
                else if (token == 2) { rx = 7.2f * cellSize; ry = 8.2f * cellSize; }
                else { rx = 7.8f * cellSize; ry = 8.2f * cellSize; }
                break;
        }
        drawTokenAt(canvas, rx, ry, r, player, token, false); 
    }

    public int[] getTokenCoords(int player, int token) {
        int pos = tokenPositions[player][token];
        if (pos == -1) {
            switch (player) {
                case 0: // RED
                    if (token == 0) return new int[]{2, 2};
                    if (token == 1) return new int[]{3, 2};
                    if (token == 2) return new int[]{2, 3};
                    if (token == 3) return new int[]{3, 3};
                    break;
                case 1: // GREEN
                    if (token == 0) return new int[]{11, 2};
                    if (token == 1) return new int[]{12, 2};
                    if (token == 2) return new int[]{11, 3};
                    if (token == 3) return new int[]{12, 3};
                    break;
                case 2: // YELLOW
                    if (token == 0) return new int[]{11, 11};
                    if (token == 1) return new int[]{12, 11};
                    if (token == 2) return new int[]{11, 12};
                    if (token == 3) return new int[]{12, 12};
                    break;
                case 3: // BLUE
                    if (token == 0) return new int[]{2, 11};
                    if (token == 1) return new int[]{3, 11};
                    if (token == 2) return new int[]{2, 12};
                    if (token == 3) return new int[]{3, 12};
                    break;
            }
        } else if (pos >= 0 && pos <= 50) {
            int boardIdx = (startIndices[player] + pos) % 52;
            return trackCoords[boardIdx];
        } else if (pos >= 51 && pos <= 55) {
            int idx = pos - 51;
            switch (player) {
                case 0: return new int[]{1 + idx, 7}; // RED
                case 1: return new int[]{7, 1 + idx}; // GREEN
                case 2: return new int[]{13 - idx, 7}; // YELLOW
                case 3: return new int[]{7, 13 - idx}; // BLUE
            }
        } else if (pos == 56) {
            switch (player) {
                case 0: return new int[]{6, 7};
                case 1: return new int[]{7, 6};
                case 2: return new int[]{8, 7};
                case 3: return new int[]{7, 8};
            }
        }
        return new int[]{7, 7};
    }

    private boolean isTokenEligible(int player, int token) {
        if (eligibleTokens == null || player != currentTurn || gameState != 2) return false;
        return eligibleTokens.contains(token);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (cellClickListener != null) {
                int col = (int) (event.getX() / cellSize);
                int row = (int) (event.getY() / cellSize);
                if (col >= 0 && col < 15 && row >= 0 && row < 15) {
                    cellClickListener.onCellClicked(col, row);
                }
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    private boolean isYardArea(int col, int row) {
        return (col < 6 && row < 6) || (col >= 9 && row < 6) || (col >= 9 && row >= 9) || (col < 6 && row >= 9);
    }

    private boolean isCenterArea(int col, int row) {
        return col >= 6 && col <= 8 && row >= 6 && row <= 8;
    }

    public boolean isSafeBoardIndex(int idx) {
        return idx == 0 || idx == 8 || idx == 13 || idx == 21 || idx == 26 || idx == 34 || idx == 39 || idx == 47;
    }

    private int getPlayerColor(int player) {
        switch (player) {
            case 0: return Color.parseColor("#E53935");
            case 1: return Color.parseColor("#4CAF50");
            case 2: return Color.parseColor("#FBC02D");
            case 3: return Color.parseColor("#1E88E5");
            default: return Color.GRAY;
        }
    }

    private int dpToPx(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}

class DiceView extends View {
    private int value = 1;
    private boolean rolling = false;
    private Paint bgPaint;
    private Paint dotPaint;

    public DiceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setStyle(Paint.Style.FILL);
        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(Color.parseColor("#37474F"));
        dotPaint.setStyle(Paint.Style.FILL);
    }

    public void setValue(int val) {
        this.value = val;
        invalidate();
    }

    public void setRolling(boolean rolling) {
        this.rolling = rolling;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = dpToPx(70);
        setMeasuredDimension(size, size);
    }

    private int dpToPx(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float radius = dpToPx(10);

        RectF rect = new RectF(4, 4, w - 4, h - 4);
        bgPaint.setColor(Color.WHITE);
        canvas.drawRoundRect(rect, radius, radius, bgPaint);

        Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setColor(Color.parseColor("#BDBDBD"));
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dpToPx(2));
        canvas.drawRoundRect(rect, radius, radius, strokePaint);

        if (rolling) {
            Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setColor(Color.GRAY);
            textPaint.setTextSize(dpToPx(18));
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("...", w / 2, h / 2 + dpToPx(5), textPaint);
            return;
        }

        float r = dpToPx(5);
        float cx = w / 2;
        float cy = h / 2;
        float left = w * 0.25f;
        float right = w * 0.75f;
        float top = h * 0.25f;
        float bottom = h * 0.75f;

        switch (value) {
            case 1:
                canvas.drawCircle(cx, cy, r, dotPaint);
                break;
            case 2:
                canvas.drawCircle(left, top, r, dotPaint);
                canvas.drawCircle(right, bottom, r, dotPaint);
                break;
            case 3:
                canvas.drawCircle(left, top, r, dotPaint);
                canvas.drawCircle(cx, cy, r, dotPaint);
                canvas.drawCircle(right, bottom, r, dotPaint);
                break;
            case 4:
                canvas.drawCircle(left, top, r, dotPaint);
                canvas.drawCircle(right, top, r, dotPaint);
                canvas.drawCircle(left, bottom, r, dotPaint);
                canvas.drawCircle(right, bottom, r, dotPaint);
                break;
            case 5:
                canvas.drawCircle(left, top, r, dotPaint);
                canvas.drawCircle(right, top, r, dotPaint);
                canvas.drawCircle(cx, cy, r, dotPaint);
                canvas.drawCircle(left, bottom, r, dotPaint);
                canvas.drawCircle(right, bottom, r, dotPaint);
                break;
            case 6:
                canvas.drawCircle(left, top, r, dotPaint);
                canvas.drawCircle(right, top, r, dotPaint);
                canvas.drawCircle(left, cy, r, dotPaint);
                canvas.drawCircle(right, cy, r, dotPaint);
                canvas.drawCircle(left, bottom, r, dotPaint);
                canvas.drawCircle(right, bottom, r, dotPaint);
                break;
        }
    }
}