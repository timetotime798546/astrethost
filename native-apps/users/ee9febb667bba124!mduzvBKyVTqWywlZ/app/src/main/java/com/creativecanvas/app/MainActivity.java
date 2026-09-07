package com.creativecanvas.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;

public class MainActivity extends Activity {

    private static final String TAG = "CreativeCanvas";

    // App Screens Containers
    private View layoutHome;
    private View layoutStudio;
    private View layoutPuzzle;
    private View layoutAbout;

    // Home Screen Views
    private View cardImage1;
    private View cardImage2;
    private Button btnModeStudio;
    private Button btnModePuzzle;
    private Button btnModeAbout;

    // Studio Screen Views
    private Button btnStudioBack;
    private ImageView imgStudioPreview;
    private TextView txtOverlayPreview;
    private EditText edtOverlayText;
    private Button btnFilterOriginal;
    private Button btnFilterGray;
    private Button btnFilterSepia;
    private Button btnFilterInvert;
    private Button btnFilterWarm;
    private View colorWhite, colorYellow, colorRed, colorBlue, colorGreen;
    private Button btnSizeDecrease, btnSizeIncrease;
    private TextView txtFontSizeVal;
    private Button btnResetCanvas;

    // Puzzle Screen Views
    private Button btnPuzzleBack;
    private TextView txtPuzzleMoves;
    private TextView txtPuzzleStatus;
    private GridLayout puzzleGridLayout;
    private Button btnPuzzleShuffle;
    private Button btnPuzzleSolve;

    // Selection States
    private int selectedImageResId = R.drawable.asset_6a9f18319c82d; // Default asset 1
    private int currentTextSizeSp = 22;
    private int currentTextColor = Color.WHITE;

    // Sliding Puzzle Game States
    private int[] boardState = new int[9]; // Map representing 3x3 positions. 0-8. 8 is empty/blank tile.
    private Bitmap[] croppedTiles = new Bitmap[9];
    private ImageView[] tileImageViews = new ImageView[9];
    private int movesCount = 0;
    private boolean isGameStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupNavigation();
        setupStudioInteractivity();
        setupPuzzleGame();
    }

    private void initializeViews() {
        // Screens
        layoutHome = findViewById(R.id.screen_home);
        layoutStudio = findViewById(R.id.screen_studio);
        layoutPuzzle = findViewById(R.id.screen_puzzle);
        layoutAbout = findViewById(R.id.screen_about);

        // Home View Bindings
        cardImage1 = findViewById(R.id.card_image_1);
        cardImage2 = findViewById(R.id.card_image_2);
        btnModeStudio = findViewById(R.id.btn_mode_studio);
        btnModePuzzle = findViewById(R.id.btn_mode_puzzle);
        btnModeAbout = findViewById(R.id.btn_mode_about);

        // Studio View Bindings
        btnStudioBack = findViewById(R.id.btn_studio_back);
        imgStudioPreview = findViewById(R.id.img_studio_preview);
        txtOverlayPreview = findViewById(R.id.txt_overlay_preview);
        edtOverlayText = findViewById(R.id.edt_overlay_text);
        btnFilterOriginal = findViewById(R.id.btn_filter_original);
        btnFilterGray = findViewById(R.id.btn_filter_gray);
        btnFilterSepia = findViewById(R.id.btn_filter_sepia);
        btnFilterInvert = findViewById(R.id.btn_filter_invert);
        btnFilterWarm = findViewById(R.id.btn_filter_warm);

        colorWhite = findViewById(R.id.color_palette_white);
        colorYellow = findViewById(R.id.color_palette_yellow);
        colorRed = findViewById(R.id.color_palette_red);
        colorBlue = findViewById(R.id.color_palette_blue);
        colorGreen = findViewById(R.id.color_palette_green);

        btnSizeDecrease = findViewById(R.id.btn_size_decrease);
        btnSizeIncrease = findViewById(R.id.btn_size_increase);
        txtFontSizeVal = findViewById(R.id.txt_font_size_val);
        btnResetCanvas = findViewById(R.id.btn_reset_canvas);

        // Puzzle View Bindings
        btnPuzzleBack = findViewById(R.id.btn_puzzle_back);
        txtPuzzleMoves = findViewById(R.id.txt_puzzle_moves);
        txtPuzzleStatus = findViewById(R.id.txt_puzzle_status);
        puzzleGridLayout = findViewById(R.id.puzzle_grid_layout);
        btnPuzzleShuffle = findViewById(R.id.btn_puzzle_shuffle);
        btnPuzzleSolve = findViewById(R.id.btn_puzzle_solve_cheat);
    }

    private void setupNavigation() {
        // Selection cards state visuals
        cardImage1.setBackgroundColor(Color.parseColor("#EBF5FB")); // Highlighted by default
        cardImage2.setBackgroundColor(Color.parseColor("#FFFFFF"));

        cardImage1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedImageResId = R.drawable.asset_6a9f18319c82d;
                cardImage1.setBackgroundColor(Color.parseColor("#EBF5FB"));
                cardImage2.setBackgroundColor(Color.parseColor("#FFFFFF"));
                Toast.makeText(MainActivity.this, "Canvas One Selected!", Toast.LENGTH_SHORT).show();
            }
        });

        cardImage2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedImageResId = R.drawable.asset_6a9f1839db93c;
                cardImage2.setBackgroundColor(Color.parseColor("#EBF5FB"));
                cardImage1.setBackgroundColor(Color.parseColor("#FFFFFF"));
                Toast.makeText(MainActivity.this, "Canvas Two Selected!", Toast.LENGTH_SHORT).show();
            }
        });

        btnModeStudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToScreen(layoutStudio);
                loadStudioCanvas();
            }
        });

        btnModePuzzle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToScreen(layoutPuzzle);
                initAndLoadPuzzleBoard();
            }
        });

        btnModeAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToScreen(layoutAbout);
            }
        });

        btnStudioBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToScreen(layoutHome);
            }
        });

        btnPuzzleBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToScreen(layoutHome);
            }
        });

        btnAboutBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToScreen(layoutHome);
            }
        });
    }

    private void switchToScreen(View screen) {
        layoutHome.setVisibility(View.GONE);
        layoutStudio.setVisibility(View.GONE);
        layoutPuzzle.setVisibility(View.GONE);
        layoutAbout.setVisibility(View.GONE);

        screen.setVisibility(View.VISIBLE);
    }

    // --- Studio Canvas Screen Logic ---
    private void loadStudioCanvas() {
        imgStudioPreview.setImageResource(selectedImageResId);
        imgStudioPreview.clearColorFilter();
        txtOverlayPreview.setVisibility(View.GONE);
        edtOverlayText.setText("");
        currentTextColor = Color.WHITE;
        currentTextSizeSp = 22;
        txtFontSizeVal.setText(currentTextSizeSp + "sp");
        txtOverlayPreview.setTextSize(currentTextSizeSp);
        txtOverlayPreview.setTextColor(currentTextColor);
    }

    private void setupStudioInteractivity() {
        // Text watcher
        edtOverlayText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    txtOverlayPreview.setVisibility(View.VISIBLE);
                    txtOverlayPreview.setText(s.toString());
                } else {
                    txtOverlayPreview.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Color palettes
        colorWhite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentTextColor = Color.WHITE;
                txtOverlayPreview.setTextColor(currentTextColor);
            }
        });
        colorYellow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentTextColor = Color.YELLOW;
                txtOverlayPreview.setTextColor(currentTextColor);
            }
        });
        colorRed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentTextColor = Color.RED;
                txtOverlayPreview.setTextColor(currentTextColor);
            }
        });
        colorBlue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentTextColor = Color.BLUE;
                txtOverlayPreview.setTextColor(currentTextColor);
            }
        });
        colorGreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentTextColor = Color.GREEN;
                txtOverlayPreview.setTextColor(currentTextColor);
            }
        });

        // Font Size Adjusters
        btnSizeDecrease.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentTextSizeSp > 12) {
                    currentTextSizeSp -= 2;
                    txtFontSizeVal.setText(currentTextSizeSp + "sp");
                    txtOverlayPreview.setTextSize(currentTextSizeSp);
                }
            }
        });

        btnSizeIncrease.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentTextSizeSp < 48) {
                    currentTextSizeSp += 2;
                    txtFontSizeVal.setText(currentTextSizeSp + "sp");
                    txtOverlayPreview.setTextSize(currentTextSizeSp);
                }
            }
        });

        // Image Filter Setters (Strict ColorMatrix & Java 8 compatibility)
        btnFilterOriginal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imgStudioPreview.clearColorFilter();
            }
        });

        btnFilterGray.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ColorMatrix matrix = new ColorMatrix();
                matrix.setSaturation(0);
                imgStudioPreview.setColorFilter(new ColorMatrixColorFilter(matrix));
            }
        });

        btnFilterSepia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ColorMatrix matrix = new ColorMatrix();
                matrix.setScale(1f, 0.95f, 0.82f, 1.0f);
                imgStudioPreview.setColorFilter(new ColorMatrixColorFilter(matrix));
            }
        });

        btnFilterInvert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                float[] invertMatrix = {
                        -1.0f,  0.0f,  0.0f, 0.0f, 255.0f,
                        0.0f, -1.0f,  0.0f, 0.0f, 255.0f,
                        0.0f,  0.0f, -1.0f, 0.0f, 255.0f,
                        0.0f,  0.0f,  0.0f, 1.0f,   0.0f
                };
                imgStudioPreview.setColorFilter(new ColorMatrixColorFilter(new ColorMatrix(invertMatrix)));
            }
        });

        btnFilterWarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ColorMatrix matrix = new ColorMatrix();
                matrix.setSaturation(1.6f);
                imgStudioPreview.setColorFilter(new ColorMatrixColorFilter(matrix));
            }
        });

        btnResetCanvas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadStudioCanvas();
            }
        });
    }

    // --- Dynamic 3x3 Slide Puzzle Game Logic ---
    private void initAndLoadPuzzleBoard() {
        movesCount = 0;
        isGameStarted = false;
        txtPuzzleMoves.setText("Moves: " + movesCount);
        txtPuzzleStatus.setText("Status: Ready to play");

        // Clean layout
        puzzleGridLayout.removeAllViews();

        try {
            // Load base image resources dynamically
            Bitmap srcBitmap = BitmapFactory.decodeResource(getResources(), selectedImageResId);
            if (srcBitmap == null) {
                throw new Exception("Bitmap resolution failed");
            }

            // Standardize resolution of the board slice target
            int targetWidthHeight = 450;
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(srcBitmap, targetWidthHeight, targetWidthHeight, true);

            // Slice into 3x3 grids (150x150 pixels per chunk)
            int tileSize = 150;
            int count = 0;
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    croppedTiles[count] = Bitmap.createBitmap(scaledBitmap, c * tileSize, r * tileSize, tileSize, tileSize);
                    boardState[count] = count; // Initialize in solved condition
                    count++;
                }
            }

            // Build board visual layouts
            renderBoardViews();

        } catch (Exception e) {
            Log.e(TAG, "Error loading puzzle board graphics", e);
            Toast.makeText(this, "Failed to load graphical asset. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void renderBoardViews() {
        puzzleGridLayout.removeAllViews();
        int itemSize = getResources().getDisplayMetrics().densityDpi > 320 ? 104 : 80;

        for (int i = 0; i < 9; i++) {
            final int pos = i;
            int tileIndex = boardState[i];

            ImageView imageView = new ImageView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            params.setMargins(2, 2, 2, 2);
            imageView.setLayoutParams(params);
            imageView.setScaleType(ImageView.ScaleType.FIT_XY);

            // If it is the blank piece (value 8)
            if (tileIndex == 8) {
                imageView.setBackgroundColor(Color.parseColor("#2C3E50"));
                imageView.setImageDrawable(null);
            } else {
                imageView.setImageBitmap(croppedTiles[tileIndex]);
            }

            // Click listener for moving slices
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handleTileClick(pos);
                }
            });

            tileImageViews[i] = imageView;

            // Generate Layout details for inside GridLayout grid cells
            GridLayout.Spec rowSpec = GridLayout.spec(i / 3, 1f);
            GridLayout.Spec colSpec = GridLayout.spec(i % 3, 1f);
            GridLayout.LayoutParams gridParams = new GridLayout.LayoutParams(rowSpec, colSpec);
            gridParams.width = 0;
            gridParams.height = 0;
            puzzleGridLayout.addView(imageView, gridParams);
        }
    }

    private void handleTileClick(int clickedPos) {
        if (!isGameStarted) {
            Toast.makeText(this, "Click 'Shuffle & Start' to play!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Search for position of the empty blank cell (index 8)
        int blankPos = -1;
        for (int i = 0; i < 9; i++) {
            if (boardState[i] == 8) {
                blankPos = i;
                break;
            }
        }

        if (isAdjacent(clickedPos, blankPos)) {
            // Swap values in state array
            int temp = boardState[clickedPos];
            boardState[clickedPos] = boardState[blankPos];
            boardState[blankPos] = temp;

            movesCount++;
            txtPuzzleMoves.setText("Moves: " + movesCount);

            renderBoardViews();
            checkWinCondition();
        }
    }

    private boolean isAdjacent(int pos1, int pos2) {
        int r1 = pos1 / 3;
        int c1 = pos1 % 3;
        int r2 = pos2 / 3;
        int c2 = pos2 % 3;

        return (Math.abs(r1 - r2) + Math.abs(c1 - c2)) == 1;
    }

    private void setupPuzzleGame() {
        btnPuzzleShuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shuffleBoard();
                movesCount = 0;
                isGameStarted = true;
                txtPuzzleMoves.setText("Moves: 0");
                txtPuzzleStatus.setText("Status: In Progress");
                renderBoardViews();
                Toast.makeText(MainActivity.this, "Board Shuffled! Good luck!", Toast.LENGTH_SHORT).show();
            }
        });

        btnPuzzleSolve.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Instantly solves or resets board
                for (int i = 0; i < 9; i++) {
                    boardState[i] = i;
                }
                isGameStarted = false;
                movesCount = 0;
                txtPuzzleMoves.setText("Moves: 0");
                txtPuzzleStatus.setText("Status: Solved (Reset)");
                renderBoardViews();
            }
        });
    }

    private void shuffleBoard() {
        Random random = new Random();
        // Simulates valid random movements from the solved state to guarantee solvability!
        int blankPos = 8;
        for (int step = 0; step < 120; step++) {
            int[] validMoves = new int[4];
            int possibleCount = 0;

            for (int i = 0; i < 9; i++) {
                if (isAdjacent(i, blankPos)) {
                    validMoves[possibleCount] = i;
                    possibleCount++;
                }
            }

            int targetMove = validMoves[random.nextInt(possibleCount)];
            // Perform swap
            int temp = boardState[targetMove];
            boardState[targetMove] = boardState[blankPos];
            boardState[blankPos] = temp;

            blankPos = targetMove;
        }
    }

    private void checkWinCondition() {
        boolean isSolved = true;
        for (int i = 0; i < 9; i++) {
            if (boardState[i] != i) {
                isSolved = false;
                break;
            }
        }

        if (isSolved) {
            isGameStarted = false;
            txtPuzzleStatus.setText("Status: Solved! 🎉");

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Victory Accomplished!");
            builder.setMessage("Amazing job! You successfully reconstructed the gorgeous asset in " + movesCount + " moves.");
            builder.setPositiveButton("Awesome", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.create().show();
        }
    }
}