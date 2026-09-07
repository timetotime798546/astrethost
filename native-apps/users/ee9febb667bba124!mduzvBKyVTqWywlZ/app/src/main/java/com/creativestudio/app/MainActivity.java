package com.creativestudio.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity {

    // Tab view hooks
    private Button tabCanvas;
    private Button tabGame;
    private View viewCanvasContainer;
    private View viewGameContainer;

    // Interactive Canvas Component references
    private FrameLayout artCanvas;
    private View canvasBackgroundView;
    private ImageView canvasStickerOne;
    private ImageView canvasStickerTwo;
    private TextView canvasOverlayText;
    private TextView statusText;

    // Manipulation state trackers
    private View selectedElement = null; 
    private int currentBgIndex = 0;
    private final int[] bgColors = {
            Color.parseColor("#D1C4E9"), // Lavender
            Color.parseColor("#B2EBF2"), // Cyan
            Color.parseColor("#C8E6C9"), // Light Green
            Color.parseColor("#FFCCBC"), // Light Orange
            Color.parseColor("#F5F5F5"), // Light Gray
            Color.parseColor("#263238")  // Dark Slate
    };

    // Card Game Mechanics Tracker
    private ImageView[] cardViews;
    private int[] cardValues; // Stores code identifiers for layouts
    private boolean[] matchedCards;
    private int firstClickedIndex = -1;
    private int secondClickedIndex = -1;
    private boolean isCheckingMatch = false;
    private int movesCounter = 0;
    private int scoreCounter = 0;
    private int bestRecord = Integer.MAX_VALUE;

    // Constants representing patterns mapped to cards
    private static final int TYPE_ASSET_ONE = 1;
    private static final int TYPE_ASSET_TWO = 2;
    private static final int TYPE_HEXAGON = 3;
    private static final int TYPE_STAR = 4;
    private static final int TYPE_DIAMOND = 5;
    private static final int TYPE_OCTAGON = 6;

    // Drag-And-Drop position metrics
    private float dX = 0f;
    private float dY = 0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Core tabs setup
        tabCanvas = (Button) findViewById(R.id.tab_canvas);
        tabGame = (Button) findViewById(R.id.tab_game);
        viewCanvasContainer = findViewById(R.id.view_canvas_container);
        viewGameContainer = findViewById(R.id.view_game_container);

        // Canvas workspace elements
        artCanvas = (FrameLayout) findViewById(R.id.art_canvas);
        canvasBackgroundView = findViewById(R.id.canvas_background_view);
        canvasStickerOne = (ImageView) findViewById(R.id.canvas_sticker_one);
        canvasStickerTwo = (ImageView) findViewById(R.id.canvas_sticker_two);
        canvasOverlayText = (TextView) findViewById(R.id.canvas_overlay_text);
        statusText = (TextView) findViewById(R.id.status_text);

        // Canvas UI buttons setup
        Button btnSelectOne = (Button) findViewById(R.id.btn_select_one);
        Button btnSelectTwo = (Button) findViewById(R.id.btn_select_two);
        Button btnSelectText = (Button) findViewById(R.id.btn_select_text);
        Button btnScaleUp = (Button) findViewById(R.id.btn_scale_up);
        Button btnScaleDown = (Button) findViewById(R.id.btn_scale_down);
        Button btnRotateClockwise = (Button) findViewById(R.id.btn_rotate_clockwise);
        Button btnChangeBg = (Button) findViewById(R.id.btn_change_bg);
        Button btnEditCaption = (Button) findViewById(R.id.btn_edit_caption);
        Button btnResetWorkspace = (Button) findViewById(R.id.btn_reset_workspace);

        Button btnAlphaLow = (Button) findViewById(R.id.btn_alpha_low);
        Button btnAlphaMid = (Button) findViewById(R.id.btn_alpha_mid);
        Button btnAlphaFull = (Button) findViewById(R.id.btn_alpha_full);

        // Setup Game elements hooks
        setupGameCardHooks();
        loadHighScore();

        // Canvas Selection defaults
        selectElement(canvasStickerOne, "Sticker One (Blue)");

        // Touch gesture implementations
        setupDragAndDropListener(canvasStickerOne);
        setupDragAndDropListener(canvasStickerTwo);
        setupDragAndDropListener(canvasOverlayText);

        // Navigation Tab Listeners
        tabCanvas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(true);
            }
        });

        tabGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(false);
            }
        });

        // Controller logic binding
        btnSelectOne.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectElement(canvasStickerOne, "Sticker One (Blue)");
            }
        });

        btnSelectTwo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectElement(canvasStickerTwo, "Sticker Two (Green)");
            }
        });

        btnSelectText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectElement(canvasOverlayText, "Custom Caption Text");
            }
        });

        btnScaleUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adjustScale(0.15f);
            }
        });

        btnScaleDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adjustScale(-0.15f);
            }
        });

        btnRotateClockwise.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedElement != null) {
                    float currentRotation = selectedElement.getRotation();
                    selectedElement.setRotation(currentRotation + 15.0f);
                }
            }
        });

        btnChangeBg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentBgIndex = (currentBgIndex + 1) % bgColors.length;
                canvasBackgroundView.setBackgroundColor(bgColors[currentBgIndex]);
                Toast.makeText(MainActivity.this, "Canvas theme updated!", Toast.LENGTH_SHORT).show();
            }
        });

        btnEditCaption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openEditCaptionDialog();
            }
        });

        btnResetWorkspace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetWorkspaceToDefaults();
            }
        });

        // Opacity Controller binds
        btnAlphaLow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setAlphaValue(0.3f);
            }
        });

        btnAlphaMid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setAlphaValue(0.7f);
            }
        });

        btnAlphaFull.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setAlphaValue(1.0f);
            }
        });

        // Restart Match Engine action hook
        Button btnRestartGame = (Button) findViewById(R.id.btn_restart_game);
        btnRestartGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startNewGameEngine();
            }
        });

        // Initial setup for the puzzle grid on start
        startNewGameEngine();
    }

    private void switchTab(boolean showCanvas) {
        if (showCanvas) {
            viewCanvasContainer.setVisibility(View.VISIBLE);
            viewGameContainer.setVisibility(View.GONE);
            tabCanvas.setTextColor(Color.parseColor("#FFFFFF"));
            tabGame.setTextColor(Color.parseColor("#B0BEC5"));
        } else {
            viewCanvasContainer.setVisibility(View.GONE);
            viewGameContainer.setVisibility(View.VISIBLE);
            tabCanvas.setTextColor(Color.parseColor("#B0BEC5"));
            tabGame.setTextColor(Color.parseColor("#FFFFFF"));
        }
    }

    // Canvas Selection highlights
    private void selectElement(View view, String name) {
        selectedElement = view;
        statusText.setText("Selected: " + name);

        // Visual outline markers
        canvasStickerOne.setBackgroundColor(Color.TRANSPARENT);
        canvasStickerTwo.setBackgroundColor(Color.TRANSPARENT);
        canvasOverlayText.setBackgroundColor(Color.TRANSPARENT);

        view.setBackgroundColor(Color.parseColor("#448AFFFD")); // Subtle cyan highlighting
    }

    // Drag-and-drop mechanics implementation using translation parameters
    private void setupDragAndDropListener(final View view) {
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Record selection
                        if (v == canvasStickerOne) {
                            selectElement(canvasStickerOne, "Sticker One (Blue)");
                        } else if (v == canvasStickerTwo) {
                            selectElement(canvasStickerTwo, "Sticker Two (Green)");
                        } else if (v == canvasOverlayText) {
                            selectElement(canvasOverlayText, "Custom Caption Text");
                        }

                        dX = v.getX() - event.getRawX();
                        dY = v.getY() - event.getRawY();
                        break;

                    case MotionEvent.ACTION_MOVE:
                        float targetX = event.getRawX() + dX;
                        float targetY = event.getRawY() + dY;

                        // Bounds checking to keep objects within frame boundaries
                        int maxRight = artCanvas.getWidth() - v.getWidth();
                        int maxBottom = artCanvas.getHeight() - v.getHeight();

                        if (targetX < 0) targetX = 0;
                        if (targetX > maxRight) targetX = maxRight;
                        if (targetY < 0) targetY = 0;
                        if (targetY > maxBottom) targetY = maxBottom;

                        v.animate()
                                .x(targetX)
                                .y(targetY)
                                .setDuration(0)
                                .start();
                        break;
                }
                return true;
            }
        });
    }

    // Scale helpers
    private void adjustScale(float delta) {
        if (selectedElement != null) {
            float nextScaleX = selectedElement.getScaleX() + delta;
            float nextScaleY = selectedElement.getScaleY() + delta;

            // Restrict size manipulation boundaries
            if (nextScaleX >= 0.4f && nextScaleX <= 3.0f) {
                selectedElement.setScaleX(nextScaleX);
                selectedElement.setScaleY(nextScaleY);
            }
        }
    }

    // Alpha configuration
    private void setAlphaValue(float value) {
        if (selectedElement != null) {
            selectedElement.setAlpha(value);
        }
    }

    // Caption Editing Handler
    private void openEditCaptionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Modify Overlay Text");

        final EditText inputField = new EditText(this);
        inputField.setText(canvasOverlayText.getText().toString());
        inputField.setSelection(inputField.getText().length());
        builder.setView(inputField);

        builder.setPositiveButton("Update", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String dynamicText = inputField.getText().toString().trim();
                if (!dynamicText.isEmpty()) {
                    canvasOverlayText.setText(dynamicText);
                }
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // Reset Canvas State
    private void resetWorkspaceToDefaults() {
        canvasStickerOne.setScaleX(1.0f);
        canvasStickerOne.setScaleY(1.0f);
        canvasStickerOne.setRotation(0.0f);
        canvasStickerOne.setAlpha(1.0f);

        canvasStickerTwo.setScaleX(1.0f);
        canvasStickerTwo.setScaleY(1.0f);
        canvasStickerTwo.setRotation(0.0f);
        canvasStickerTwo.setAlpha(1.0f);

        canvasOverlayText.setScaleX(1.0f);
        canvasOverlayText.setScaleY(1.0f);
        canvasOverlayText.setRotation(0.0f);
        canvasOverlayText.setAlpha(1.0f);
        canvasOverlayText.setText("TAP ME TO MOVE!");

        canvasBackgroundView.setBackgroundColor(bgColors[0]);
        currentBgIndex = 0;

        // Reset relative translation coordinates
        canvasStickerOne.animate().x(50).y(50).setDuration(150).start();
        canvasStickerTwo.animate().x(200).y(100).setDuration(150).start();
        canvasOverlayText.animate().x(100).y(200).setDuration(150).start();

        selectElement(canvasStickerOne, "Sticker One (Blue)");
        Toast.makeText(this, "Canvas values reset!", Toast.LENGTH_SHORT).show();
    }

    // ============================================
    // MEMORY MATCH PUZZLE LOGIC
    // ============================================

    private void setupGameCardHooks() {
        cardViews = new ImageView[12];
        cardViews[0] = (ImageView) findViewById(R.id.card_0);
        cardViews[1] = (ImageView) findViewById(R.id.card_1);
        cardViews[2] = (ImageView) findViewById(R.id.card_2);
        cardViews[3] = (ImageView) findViewById(R.id.card_3);
        cardViews[4] = (ImageView) findViewById(R.id.card_4);
        cardViews[5] = (ImageView) findViewById(R.id.card_5);
        cardViews[6] = (ImageView) findViewById(R.id.card_6);
        cardViews[7] = (ImageView) findViewById(R.id.card_7);
        cardViews[8] = (ImageView) findViewById(R.id.card_8);
        cardViews[9] = (ImageView) findViewById(R.id.card_9);
        cardViews[10] = (ImageView) findViewById(R.id.card_10);
        cardViews[11] = (ImageView) findViewById(R.id.card_11);

        cardValues = new int[12];
        matchedCards = new boolean[12];

        // Bind clicks programmatically
        for (int i = 0; i < cardViews.length; i++) {
            final int index = i;
            cardViews[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onCardSelected(index);
                }
            });
        }
    }

    private void startNewGameEngine() {
        movesCounter = 0;
        scoreCounter = 0;
        firstClickedIndex = -1;
        secondClickedIndex = -1;
        isCheckingMatch = false;

        TextView movesView = (TextView) findViewById(R.id.game_text_moves);
        TextView scoreView = (TextView) findViewById(R.id.game_text_score);
        movesView.setText("0");
        scoreView.setText("0/6");

        // Prepare matching structures
        List<Integer> resourcePool = new ArrayList<>();
        // Add dual matching pairs
        resourcePool.add(TYPE_ASSET_ONE);
        resourcePool.add(TYPE_ASSET_ONE);
        resourcePool.add(TYPE_ASSET_TWO);
        resourcePool.add(TYPE_ASSET_TWO);
        resourcePool.add(TYPE_HEXAGON);
        resourcePool.add(TYPE_HEXAGON);
        resourcePool.add(TYPE_STAR);
        resourcePool.add(TYPE_STAR);
        resourcePool.add(TYPE_DIAMOND);
        resourcePool.add(TYPE_DIAMOND);
        resourcePool.add(TYPE_OCTAGON);
        resourcePool.add(TYPE_OCTAGON);

        Collections.shuffle(resourcePool);

        for (int i = 0; i < 12; i++) {
            cardValues[i] = resourcePool.get(i);
            matchedCards[i] = false;
            cardViews[i].setBackgroundResource(R.drawable.card_bg);
            cardViews[i].setImageDrawable(null);
        }

        Toast.makeText(this, "Puzzle Shuffled! Match the assets.", Toast.LENGTH_SHORT).show();
    }

    private void onCardSelected(int clickedIndex) {
        if (isCheckingMatch || matchedCards[clickedIndex]) {
            return; // Ignore clicking matched items or during match animation periods
        }

        if (clickedIndex == firstClickedIndex) {
            return; // Clicked same tile
        }

        // Show selected tile face
        revealCardContent(clickedIndex);

        if (firstClickedIndex == -1) {
            // Picked initial tile
            firstClickedIndex = clickedIndex;
        } else {
            // Picked second tile, execute comparison sequence
            secondClickedIndex = clickedIndex;
            incrementMoves();
            isCheckingMatch = true;

            if (cardValues[firstClickedIndex] == cardValues[secondClickedIndex]) {
                // Success Scenario
                matchedCards[firstClickedIndex] = true;
                matchedCards[secondClickedIndex] = true;
                scoreCounter++;

                TextView scoreView = (TextView) findViewById(R.id.game_text_score);
                scoreView.setText(scoreCounter + "/6");

                firstClickedIndex = -1;
                secondClickedIndex = -1;
                isCheckingMatch = false;

                checkWinScenario();
            } else {
                // Failure Scenario - Flip back after interactive delay
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        resetCardsFace(firstClickedIndex, secondClickedIndex);
                        firstClickedIndex = -1;
                        secondClickedIndex = -1;
                        isCheckingMatch = false;
                    }
                }, 900);
            }
        }
    }

    private void revealCardContent(int index) {
        cardViews[index].setBackgroundResource(R.drawable.card_selected);
        switch (cardValues[index]) {
            case TYPE_ASSET_ONE:
                cardViews[index].setImageResource(R.drawable.asset_one);
                break;
            case TYPE_ASSET_TWO:
                cardViews[index].setImageResource(R.drawable.asset_two);
                break;
            case TYPE_HEXAGON:
                cardViews[index].setImageResource(android.R.drawable.ic_menu_compass);
                break;
            case TYPE_STAR:
                cardViews[index].setImageResource(android.R.drawable.btn_star_big_on);
                break;
            case TYPE_DIAMOND:
                cardViews[index].setImageResource(android.R.drawable.ic_menu_gallery);
                break;
            case TYPE_OCTAGON:
                cardViews[index].setImageResource(android.R.drawable.ic_menu_manage);
                break;
        }
    }

    private void resetCardsFace(int index1, int index2) {
        cardViews[index1].setBackgroundResource(R.drawable.card_bg);
        cardViews[index1].setImageDrawable(null);
        cardViews[index2].setBackgroundResource(R.drawable.card_bg);
        cardViews[index2].setImageDrawable(null);
    }

    private void incrementMoves() {
        movesCounter++;
        TextView movesView = (TextView) findViewById(R.id.game_text_moves);
        movesView.setText(String.valueOf(movesCounter));
    }

    private void checkWinScenario() {
        if (scoreCounter == 6) {
            // All matches solved
            Toast.makeText(this, "Victory! Solved in " + movesCounter + " moves!", Toast.LENGTH_LONG).show();

            if (movesCounter < bestRecord) {
                bestRecord = movesCounter;
                saveHighScore(bestRecord);
                TextView bestView = (TextView) findViewById(R.id.game_text_best);
                bestView.setText(String.valueOf(bestRecord));
                Toast.makeText(this, "New Best Record!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadHighScore() {
        SharedPreferences prefs = getSharedPreferences("puzzle_prefs", MODE_PRIVATE);
        bestRecord = prefs.getInt("best_moves", Integer.MAX_VALUE);
        TextView bestView = (TextView) findViewById(R.id.game_text_best);
        if (bestRecord == Integer.MAX_VALUE) {
            bestView.setText("--");
        } else {
            bestView.setText(String.valueOf(bestRecord));
        }
    }

    private void saveHighScore(int score) {
        SharedPreferences.Editor editor = getSharedPreferences("puzzle_prefs", MODE_PRIVATE).edit();
        editor.putInt("best_moves", score);
        editor.apply();
    }
}