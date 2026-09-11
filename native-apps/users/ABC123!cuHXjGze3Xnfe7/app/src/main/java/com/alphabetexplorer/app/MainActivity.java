package com.alphabetexplorer.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity {

    private final String[] letters = {
        "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", 
        "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"
    };

    private final String[] words = {
        "Apple", "Banana", "Cat", "Dog", "Elephant", "Frog", "Giraffe", "Horse", "Iguana", 
        "Jellyfish", "Kangaroo", "Lion", "Monkey", "Nest", "Owl", "Penguin", "Queen", 
        "Rabbit", "Sun", "Tiger", "Umbrella", "Violin", "Whale", "Xylophone", "Yak", "Zebra"
    };

    // Premium vibrant child-friendly gradient color pairs
    private final String[][] gradientPairs = {
        {"#FF6B6B", "#FF8E53"}, // Warm Sunset
        {"#FF8E53", "#FFD200"}, // Tangerine
        {"#FFD200", "#FFB302"}, // Gold
        {"#4CD137", "#44BD32"}, // Green
        {"#00CEC9", "#00A8FF"}, // Cyan Blue
        {"#9C27B0", "#E91E63"}, // Magenta Purple
        {"#E91E63", "#FF7675"}, // Rose
        {"#00A8FF", "#3742FA"}, // Blue Sky
        {"#6C5CE7", "#A29BFE"}, // Lavender
        {"#FDA7DF", "#D980FA"}, // Candy Pink
        {"#FFEAA7", "#FDCB6E"}, // Pastel Yellow
        {"#55EFC4", "#81ECEC"}, // Mint
        {"#81ECEC", "#00CEC9"}, // Aqua
        {"#74B9FF", "#0984E3"}, // Sky Blue
        {"#FAB1A0", "#FF7675"}, // Peach Coral
        {"#DF80AC", "#E91E63"}, // Plum
        {"#FF7675", "#D63031"}  // Berry
    };

    private TextToSpeech tts;
    private boolean isTtsInitialized = false;

    private int currentLearnIndex = 0;
    private int currentTraceIndex = 0;
    private int quizTargetIndex = 0;
    private int score = 0;

    private DrawingView drawingView;
    private Interactive3DView interactive3DView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initTTS();
        setupNavigation();
        setupLearnGridView();
        setupQuizListeners();
        setupTraceMode();
        setupDetailViewListeners();
        beautifyUI();

        // Instantiate and embed our ultra-realistic custom Interactive 3D View inside detail popup
        FrameLayout cubeHolder = findViewById(R.id.container_3d_cube);
        interactive3DView = new Interactive3DView(this);
        cubeHolder.addView(interactive3DView);

        // Start mode 1 (Learn View) instantly
        switchMode(1);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                speak("Welcome to Alphabet Explorer 3D! Rotate our physical toy blocks!");
            }
        }, 850);
    }

    private void initTTS() {
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(Locale.US);
                    if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                        isTtsInitialized = true;
                    }
                }
            }
        });
    }

    private void speak(String text) {
        if (isTtsInitialized && tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    // Dynamic Helper to generate Tactile 3D Drawables with pressed translation effect
    private Drawable create3DDrawable(int mainColor, int shadowColor, float cornerRadiusDp, int depthDp) {
        float scale = getResources().getDisplayMetrics().density;
        int depthPx = (int) (depthDp * scale + 0.5f);
        float radiusPx = cornerRadiusDp * scale;

        // Normal face layer
        GradientDrawable topNormal = new GradientDrawable();
        topNormal.setColor(mainColor);
        topNormal.setCornerRadius(radiusPx);

        // Darker shadow block layer
        GradientDrawable bottomNormal = new GradientDrawable();
        bottomNormal.setColor(shadowColor);
        bottomNormal.setCornerRadius(radiusPx);

        LayerDrawable normalLayers = new LayerDrawable(new Drawable[] { bottomNormal, topNormal });
        normalLayers.setLayerInset(0, 0, depthPx, 0, 0); // Shadow sits at the bottom base
        normalLayers.setLayerInset(1, 0, 0, 0, depthPx); // Normal face shifts up by depthPx

        // Pressed/Focused face layer
        GradientDrawable topPressed = new GradientDrawable();
        topPressed.setColor(mainColor);
        topPressed.setCornerRadius(radiusPx);

        GradientDrawable bottomPressed = new GradientDrawable();
        bottomPressed.setColor(shadowColor);
        bottomPressed.setCornerRadius(radiusPx);

        LayerDrawable pressedLayers = new LayerDrawable(new Drawable[] { bottomPressed, topPressed });
        // Simulates compressing physical mechanics down closer to base shadow
        int pressOffset = (int) (1.5f * scale); 
        pressedLayers.setLayerInset(0, 0, depthPx, 0, 0);
        pressedLayers.setLayerInset(1, 0, depthPx - pressOffset, 0, pressOffset);

        StateListDrawable stateDrawable = new StateListDrawable();
        stateDrawable.addState(new int[] { android.R.attr.state_pressed }, pressedLayers);
        stateDrawable.addState(new int[] { android.R.attr.state_focused }, pressedLayers);
        stateDrawable.addState(new int[] {}, normalLayers);

        return stateDrawable;
    }

    // Dynamic Helper to generate Linear Gradient 3D tactile buttons
    private Drawable create3DGradientDrawable(int[] gradientColors, int shadowColor, float cornerRadiusDp, int depthDp) {
        float scale = getResources().getDisplayMetrics().density;
        int depthPx = (int) (depthDp * scale + 0.5f);
        float radiusPx = cornerRadiusDp * scale;

        // Face Layer
        GradientDrawable topNormal = new GradientDrawable(GradientDrawable.Orientation.TL_BR, gradientColors);
        topNormal.setCornerRadius(radiusPx);

        // Shadow block
        GradientDrawable bottomNormal = new GradientDrawable();
        bottomNormal.setColor(shadowColor);
        bottomNormal.setCornerRadius(radiusPx);

        LayerDrawable normalLayers = new LayerDrawable(new Drawable[] { bottomNormal, topNormal });
        normalLayers.setLayerInset(0, 0, depthPx, 0, 0);
        normalLayers.setLayerInset(1, 0, 0, 0, depthPx);

        // Pressed state
        GradientDrawable topPressed = new GradientDrawable(GradientDrawable.Orientation.TL_BR, gradientColors);
        topPressed.setCornerRadius(radiusPx);

        GradientDrawable bottomPressed = new GradientDrawable();
        bottomPressed.setColor(shadowColor);
        bottomPressed.setCornerRadius(radiusPx);

        LayerDrawable pressedLayers = new LayerDrawable(new Drawable[] { bottomPressed, topPressed });
        int pressOffset = (int) (1.5f * scale); 
        pressedLayers.setLayerInset(0, 0, depthPx, 0, 0);
        pressedLayers.setLayerInset(1, 0, depthPx - pressOffset, 0, pressOffset);

        StateListDrawable stateDrawable = new StateListDrawable();
        stateDrawable.addState(new int[] { android.R.attr.state_pressed }, pressedLayers);
        stateDrawable.addState(new int[] { android.R.attr.state_focused }, pressedLayers);
        stateDrawable.addState(new int[] {}, normalLayers);

        return stateDrawable;
    }

    // Dynamic Helper to generate flat 3D Card platforms (Static)
    private Drawable create3DCardDrawable(int faceColor, int shadowColor, float cornerRadiusDp, int depthDp) {
        float scale = getResources().getDisplayMetrics().density;
        int depthPx = (int) (depthDp * scale + 0.5f);
        float radiusPx = cornerRadiusDp * scale;

        GradientDrawable top = new GradientDrawable();
        top.setColor(faceColor);
        top.setCornerRadius(radiusPx);

        GradientDrawable bottom = new GradientDrawable();
        bottom.setColor(shadowColor);
        bottom.setCornerRadius(radiusPx);

        LayerDrawable layers = new LayerDrawable(new Drawable[] { bottom, top });
        layers.setLayerInset(0, 0, depthPx, 0, 0);
        layers.setLayerInset(1, 0, 0, 0, depthPx);
        return layers;
    }

    // Dynamic color shifting tool for perfect matching shaded depths
    private int calculateShadowColor(String hexColor) {
        int color = Color.parseColor(hexColor);
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.68f; // Deepen brightness by 32% to build realistic skeuomorphic shadow depth
        return Color.HSVToColor(hsv);
    }

    private void setupNavigation() {
        final Button navBtnLearn = findViewById(R.id.nav_btn_learn);
        final Button navBtnQuiz = findViewById(R.id.nav_btn_quiz);
        final Button navBtnTrace = findViewById(R.id.nav_btn_trace);

        navBtnLearn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchMode(1);
            }
        });

        navBtnQuiz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchMode(2);
            }
        });

        navBtnTrace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchMode(3);
            }
        });
    }

    private void updateNavigationUI(int activeMode) {
        Button navBtnLearn = findViewById(R.id.nav_btn_learn);
        Button navBtnQuiz = findViewById(R.id.nav_btn_quiz);
        Button navBtnTrace = findViewById(R.id.nav_btn_trace);

        // Apply distinct active pressed-down or elevated highlights matching user selection
        if (activeMode == 1) {
            navBtnLearn.setBackground(create3DDrawable(Color.parseColor("#6C5CE7"), calculateShadowColor("#6C5CE7"), 12, 4));
            navBtnLearn.setTextColor(Color.WHITE);
            
            navBtnQuiz.setBackground(create3DDrawable(Color.parseColor("#EBF0F6"), calculateShadowColor("#CBD5E0"), 12, 3));
            navBtnQuiz.setTextColor(Color.parseColor("#4A5568"));
            
            navBtnTrace.setBackground(create3DDrawable(Color.parseColor("#EBF0F6"), calculateShadowColor("#CBD5E0"), 12, 3));
            navBtnTrace.setTextColor(Color.parseColor("#4A5568"));
        } else if (activeMode == 2) {
            navBtnLearn.setBackground(create3DDrawable(Color.parseColor("#EBF0F6"), calculateShadowColor("#CBD5E0"), 12, 3));
            navBtnLearn.setTextColor(Color.parseColor("#4A5568"));
            
            navBtnQuiz.setBackground(create3DDrawable(Color.parseColor("#FF6B6B"), calculateShadowColor("#FF6B6B"), 12, 4));
            navBtnQuiz.setTextColor(Color.WHITE);
            
            navBtnTrace.setBackground(create3DDrawable(Color.parseColor("#EBF0F6"), calculateShadowColor("#CBD5E0"), 12, 3));
            navBtnTrace.setTextColor(Color.parseColor("#4A5568"));
        } else if (activeMode == 3) {
            navBtnLearn.setBackground(create3DDrawable(Color.parseColor("#EBF0F6"), calculateShadowColor("#CBD5E0"), 12, 3));
            navBtnLearn.setTextColor(Color.parseColor("#4A5568"));
            
            navBtnQuiz.setBackground(create3DDrawable(Color.parseColor("#EBF0F6"), calculateShadowColor("#CBD5E0"), 12, 3));
            navBtnQuiz.setTextColor(Color.parseColor("#4A5568"));
            
            navBtnTrace.setBackground(create3DDrawable(Color.parseColor("#00B894"), calculateShadowColor("#00B894"), 12, 4));
            navBtnTrace.setTextColor(Color.WHITE);
        }
    }

    private void switchMode(int mode) {
        View containerLearn = findViewById(R.id.container_learn);
        View containerQuiz = findViewById(R.id.container_quiz);
        View containerTrace = findViewById(R.id.container_trace);
        TextView toolbarScore = findViewById(R.id.toolbar_score);

        findViewById(R.id.detail_overlay).setVisibility(View.GONE);

        containerLearn.setVisibility(View.GONE);
        containerQuiz.setVisibility(View.GONE);
        containerTrace.setVisibility(View.GONE);
        toolbarScore.setVisibility(View.GONE);

        updateNavigationUI(mode);

        if (mode == 1) {
            containerLearn.setVisibility(View.VISIBLE);
            speak("Let's play and learn letters in 3D!");
        } else if (mode == 2) {
            containerQuiz.setVisibility(View.VISIBLE);
            toolbarScore.setVisibility(View.VISIBLE);
            generateQuizQuestion();
        } else if (mode == 3) {
            containerTrace.setVisibility(View.VISIBLE);
            updateTraceLetter();
        }
    }

    private void setupLearnGridView() {
        GridView gridView = findViewById(R.id.alphabet_grid);
        gridView.setAdapter(new AlphabetAdapter(this));
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showDetail(position);
            }
        });
    }

    private void showDetail(int index) {
        currentLearnIndex = index;
        String letter = letters[index];
        String word = words[index];

        TextView letterPair = findViewById(R.id.detail_letter_pair);
        TextView wordPhrase = findViewById(R.id.detail_word_phrase);

        letterPair.setText(letter + letter.toLowerCase());
        wordPhrase.setText(letter + " is for " + word);

        // Update the custom 3D interactive block with the chosen letter and color theme
        int pairIndex = index % gradientPairs.length;
        String[] colors = gradientPairs[pairIndex];
        int mainColor = Color.parseColor(colors[0]);
        if (interactive3DView != null) {
            interactive3DView.setBlockData(letter, mainColor);
        }

        View detailOverlay = findViewById(R.id.detail_overlay);
        detailOverlay.setVisibility(View.VISIBLE);

        speakLetterDetail(index);
    }

    private void speakLetterDetail(int index) {
        String letter = letters[index];
        String word = words[index];
        speak(letter + ". " + letter + " is for " + word + ".");
    }

    private void setupDetailViewListeners() {
        findViewById(R.id.btn_close_detail).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.detail_overlay).setVisibility(View.GONE);
            }
        });

        findViewById(R.id.btn_speak_detail).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speakLetterDetail(currentLearnIndex);
            }
        });

        findViewById(R.id.btn_detail_prev).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLearnIndex > 0) {
                    showDetail(currentLearnIndex - 1);
                } else {
                    Toast.makeText(MainActivity.this, "This is the first letter!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.btn_detail_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLearnIndex < 25) {
                    showDetail(currentLearnIndex + 1);
                } else {
                    Toast.makeText(MainActivity.this, "This is the last letter!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void generateQuizQuestion() {
        Random rand = new Random();
        quizTargetIndex = rand.nextInt(26);
        String targetLetter = letters[quizTargetIndex];

        TextView targetDisplay = findViewById(R.id.quiz_target_letter);
        targetDisplay.setText(targetLetter);

        List<Integer> choices = new ArrayList<Integer>();
        choices.add(quizTargetIndex);

        while (choices.size() < 4) {
            int r = rand.nextInt(26);
            if (!choices.contains(r)) {
                choices.add(r);
            }
        }

        Collections.shuffle(choices);

        Button btn1 = findViewById(R.id.btn_option_1);
        Button btn2 = findViewById(R.id.btn_option_2);
        Button btn3 = findViewById(R.id.btn_option_3);
        Button btn4 = findViewById(R.id.btn_option_4);

        final Button[] buttons = {btn1, btn2, btn3, btn4};
        for (int i = 0; i < 4; i++) {
            final int index = choices.get(i);
            buttons[i].setText(letters[index]);
            buttons[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    evaluateAnswer(index);
                }
            });

            // Dynamically build skeuomorphic gradient 3D Buttons for choices
            int pairIndex = index % gradientPairs.length;
            String[] colors = gradientPairs[pairIndex];
            int mainColor1 = Color.parseColor(colors[0]);
            int mainColor2 = Color.parseColor(colors[1]);
            int shadowColor = calculateShadowColor(colors[1]);

            buttons[i].setBackground(create3DGradientDrawable(new int[]{mainColor1, mainColor2}, shadowColor, 18, 5));
        }

        speakQuizQuestion();
    }

    private void speakQuizQuestion() {
        speak("Which button shows the letter " + letters[quizTargetIndex] + "?");
    }

    private void evaluateAnswer(int selectedIndex) {
        if (selectedIndex == quizTargetIndex) {
            score += 10;
            speak("Terrific! That is absolutely correct!");
            Toast.makeText(this, "Correct! +10 Points", Toast.LENGTH_SHORT).show();

            TextView toolbarScore = findViewById(R.id.toolbar_score);
            toolbarScore.setText("Score: " + score);

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    generateQuizQuestion();
                }
            }, 1200);
        } else {
            speak("That is letter " + letters[selectedIndex] + ". Give it another try!");
            Toast.makeText(this, "Not quite! Try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupQuizListeners() {
        findViewById(R.id.btn_quiz_listen).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speakQuizQuestion();
            }
        });
    }

    private void setupTraceMode() {
        FrameLayout holder = findViewById(R.id.trace_canvas_holder);
        drawingView = new DrawingView(this);
        holder.addView(drawingView);

        findViewById(R.id.btn_trace_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.clearCanvas();
            }
        });

        findViewById(R.id.btn_trace_prev).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentTraceIndex > 0) {
                    currentTraceIndex--;
                    updateTraceLetter();
                } else {
                    Toast.makeText(MainActivity.this, "This is the first letter!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.btn_trace_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentTraceIndex < 25) {
                    currentTraceIndex++;
                    updateTraceLetter();
                } else {
                    Toast.makeText(MainActivity.this, "This is the last letter!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateTraceLetter() {
        String targetChar = letters[currentTraceIndex];
        drawingView.setLetter(targetChar);

        TextView traceInstruction = findViewById(R.id.trace_instruction);
        traceInstruction.setText("Trace the Letter " + targetChar);
        speak("Let's draw " + targetChar);
    }

    private void beautifyUI() {
        // App background gradient
        View rootLayout = findViewById(R.id.root_layout);
        if (rootLayout != null) {
            GradientDrawable rootBg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] { Color.parseColor("#F4F7FB"), Color.parseColor("#DBE3EC") }
            );
            rootLayout.setBackground(rootBg);
        }

        // Toolbar setup
        View toolbar = findViewById(R.id.toolbar_container);
        GradientDrawable toolbarBg = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[] { Color.parseColor("#5A4FCF"), Color.parseColor("#7B2CBF") }
        );
        toolbarBg.setShape(GradientDrawable.RECTANGLE);
        float density = getResources().getDisplayMetrics().density;
        float cornerRadius = 18f * density;
        toolbarBg.setCornerRadii(new float[]{0, 0, 0, 0, cornerRadius, cornerRadius, cornerRadius, cornerRadius});
        toolbar.setBackground(toolbarBg);

        // Score Badge
        TextView toolbarScore = findViewById(R.id.toolbar_score);
        toolbarScore.setBackground(create3DCardDrawable(Color.parseColor("#FFD200"), calculateShadowColor("#FFD200"), 12, 3));
        toolbarScore.setTextColor(Color.parseColor("#1A202C"));
        toolbarScore.setPadding((int)(16*density), (int)(8*density), (int)(16*density), (int)(8*density));

        // Quiz Question card in 3D
        View quizCard = findViewById(R.id.quiz_card);
        quizCard.setBackground(create3DCardDrawable(Color.WHITE, Color.parseColor("#CBD5E0"), 24, 6));

        // Listen Button in Quiz
        Button btnListen = findViewById(R.id.btn_quiz_listen);
        btnListen.setBackground(create3DDrawable(Color.parseColor("#6C5CE7"), calculateShadowColor("#6C5CE7"), 16, 4));

        // Popup Container
        View cardContainer = findViewById(R.id.detail_card_container);
        cardContainer.setBackground(create3DCardDrawable(Color.WHITE, Color.parseColor("#CBD5E0"), 26, 8));

        // Speak Phonics trigger inside overlay
        Button btnSpeak = findViewById(R.id.btn_speak_detail);
        btnSpeak.setBackground(create3DDrawable(Color.parseColor("#00B894"), calculateShadowColor("#00B894"), 16, 4));

        // Modal Sheet control buttons
        Button btnPrev = findViewById(R.id.btn_detail_prev);
        btnPrev.setBackground(create3DDrawable(Color.parseColor("#718096"), calculateShadowColor("#4A5568"), 14, 4));

        Button btnNext = findViewById(R.id.btn_detail_next);
        btnNext.setBackground(create3DDrawable(Color.parseColor("#6C5CE7"), calculateShadowColor("#6C5CE7"), 14, 4));

        // Drawing Panel design
        View canvasHolder = findViewById(R.id.trace_canvas_holder);
        canvasHolder.setBackground(create3DCardDrawable(Color.WHITE, Color.parseColor("#DFE6E9"), 20, 6));

        // Tracing control triggers
        Button tracePrev = findViewById(R.id.btn_trace_prev);
        tracePrev.setBackground(create3DDrawable(Color.parseColor("#6C5CE7"), calculateShadowColor("#6C5CE7"), 14, 4));

        Button traceNext = findViewById(R.id.btn_trace_next);
        traceNext.setBackground(create3DDrawable(Color.parseColor("#6C5CE7"), calculateShadowColor("#6C5CE7"), 14, 4));

        Button traceClear = findViewById(R.id.btn_trace_clear);
        traceClear.setBackground(create3DDrawable(Color.parseColor("#FF5252"), calculateShadowColor("#D32F2F"), 14, 4));
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    // Grid adapter displaying fully interactive 3D letter cards
    private class AlphabetAdapter extends BaseAdapter {
        private Context context;

        public AlphabetAdapter(Context context) {
            this.context = context;
        }

        @Override
        public int getCount() {
            return 26;
        }

        @Override
        public Object getItem(int position) {
            return letters[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView textView;
            if (convertView == null) {
                textView = new TextView(context);
                float density = context.getResources().getDisplayMetrics().density;
                int heightPx = (int) (106 * density);
                textView.setLayoutParams(new GridView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, heightPx
                ));
                textView.setGravity(Gravity.CENTER);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 38);
                textView.setTextColor(Color.WHITE);
                textView.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            } else {
                textView = (TextView) convertView;
            }

            String letter = letters[position];
            textView.setText(letter);

            // Fetch matched color sets to form 3D dimensional gradients
            int pairIndex = position % gradientPairs.length;
            String[] colors = gradientPairs[pairIndex];
            int mainColor1 = Color.parseColor(colors[0]);
            int mainColor2 = Color.parseColor(colors[1]);
            int shadowColor = calculateShadowColor(colors[1]);

            textView.setBackground(create3DGradientDrawable(new int[]{mainColor1, mainColor2}, shadowColor, 20, 5));

            return textView;
        }
    }

    // Canvas rendering engine equipped with skeuomorphic trace stencils
    public static class DrawingView extends View {
        private Paint paint;
        private Paint bgPaint;
        private Path path;
        private String letter = "A";

        public DrawingView(Context context) {
            super(context);
            init();
        }

        private void init() {
            paint = new Paint();
            paint.setColor(Color.parseColor("#6C5CE7"));
            paint.setAntiAlias(true);
            paint.setStrokeWidth(20f);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeCap(Paint.Cap.ROUND);

            bgPaint = new Paint();
            bgPaint.setColor(Color.parseColor("#EBF0F6"));
            bgPaint.setAntiAlias(true);
            bgPaint.setTextAlign(Paint.Align.CENTER);
            bgPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            bgPaint.setStyle(Paint.Style.FILL);

            path = new Path();
        }

        public void setLetter(String letter) {
            this.letter = letter;
            clearCanvas();
        }

        public void clearCanvas() {
            path.reset();
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            float optimalSize = getHeight() * 0.7f;
            bgPaint.setTextSize(optimalSize);

            float xPos = getWidth() / 2f;
            float yPos = (getHeight() / 2f) - ((bgPaint.descent() + bgPaint.ascent()) / 2f);
            canvas.drawText(letter, xPos, yPos, bgPaint);

            canvas.drawPath(path, paint);
        }

        @Override
        public boolean performClick() {
            return super.performClick();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX();
            float y = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    path.moveTo(x, y);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    path.lineTo(x, y);
                    break;
                case MotionEvent.ACTION_UP:
                    performClick();
                    break;
                default:
                    return false;
            }
            invalidate();
            return true;
        }
    }

    // A gorgeous, high-performance custom interactive 3D Cube View using Android Camera transforms & Physics
    public static class Interactive3DView extends View {
        private android.graphics.Camera camera;
        private android.graphics.Matrix matrix;
        private Paint blockPaint;
        private Paint borderPaint;
        private Paint textPaint;
        private Paint shadowPaint;
        
        private float rotateX = -15f;
        private float rotateY = 25f;
        private float lastTouchX;
        private float lastTouchY;
        private String currentLetter = "A";
        private int blockColor = Color.parseColor("#FF6B6B");
        
        private float size;
        private float velocityX = 0f;
        private float velocityY = 0f;
        private boolean isDragging = false;
        private Runnable inertiaRunnable;
        private Handler physicsHandler = new Handler();

        public Interactive3DView(Context context) {
            super(context);
            init();
        }

        private void init() {
            camera = new android.graphics.Camera();
            matrix = new android.graphics.Matrix();
            
            blockPaint = new Paint();
            blockPaint.setAntiAlias(true);
            blockPaint.setStyle(Paint.Style.FILL);
            
            borderPaint = new Paint();
            borderPaint.setAntiAlias(true);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(4f);
            
            textPaint = new Paint();
            textPaint.setAntiAlias(true);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            
            shadowPaint = new Paint();
            shadowPaint.setAntiAlias(true);
            shadowPaint.setColor(Color.parseColor("#331A202C"));
            shadowPaint.setStyle(Paint.Style.FILL);
            
            float scale = getContext().getResources().getDisplayMetrics().density;
            size = 110f * scale; // Ideal layout dimension for target popups
            textPaint.setTextSize(52f * scale);
        }

        public void setBlockData(String letter, int color) {
            this.currentLetter = letter;
            this.blockColor = color;
            this.rotateX = -15f;
            this.rotateY = 25f;
            this.velocityX = 7f; // Kickoff spin
            this.velocityY = 2f;
            startInertia();
            invalidate();
        }

        private float getRotatedZ(float x, float y, float z) {
            double radX = Math.toRadians(rotateX);
            double radY = Math.toRadians(rotateY);
            
            // Apply rotation around Y axis
            double x1 = x * Math.cos(radY) - z * Math.sin(radY);
            double z1 = x * Math.sin(radY) + z * Math.cos(radY);
            
            // Apply rotation around X axis
            double y2 = y * Math.cos(radX) - z1 * Math.sin(radX);
            double z2 = y * Math.sin(radX) + z1 * Math.cos(radX);
            
            return (float) z2;
        }

        private int adjustShading(int color, float factor) {
            float[] hsv = new float[3];
            Color.colorToHSV(color, hsv);
            hsv[2] = Math.max(0f, Math.min(1f, hsv[2] * factor));
            return Color.HSVToColor(hsv);
        }

        class Face implements Comparable<Face> {
            float rotX, rotY;
            float transX, transY, transZ;
            String text;
            int bgColor;
            float depth;

            Face(float rotX, float rotY, float transX, float transY, float transZ, String text, int bgColor, float rawX, float rawY, float rawZ) {
                this.rotX = rotX;
                this.rotY = rotY;
                this.transX = transX;
                this.transY = transY;
                this.transZ = transZ;
                this.text = text;
                this.bgColor = bgColor;
                this.depth = getRotatedZ(rawX, rawY, rawZ);
            }

            @Override
            public int compareTo(Face other) {
                // Painter's algorithm: Sort descending so furthest Z is drawn first
                return Float.compare(other.depth, this.depth);
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            
            int w = getWidth();
            int h = getHeight();
            float centerX = w / 2f;
            float centerY = h / 2f;
            
            // Floor Perspective shadow
            float shadowRadiusX = size * 0.72f + (rotateX * 0.1f);
            float shadowRadiusY = size * 0.22f;
            canvas.drawOval(centerX - shadowRadiusX, centerY + size * 0.82f, centerX + shadowRadiusX, centerY + size * 0.94f, shadowPaint);
            
            float s = size;
            float hs = s / 2f;
            
            List<Face> faces = new ArrayList<>();
            // Define all 6 faces of physical cube
            faces.add(new Face(0, 0, 0, 0, -hs, currentLetter, blockColor, 0, 0, -hs));
            faces.add(new Face(0, 180, 0, 0, -hs, currentLetter.toLowerCase(), blockColor, 0, 0, hs));
            faces.add(new Face(0, -90, 0, 0, -hs, currentLetter, blockColor, -hs, 0, 0));
            faces.add(new Face(0, 90, 0, 0, -hs, currentLetter, blockColor, hs, 0, 0));
            faces.add(new Face(-90, 0, 0, 0, -hs, "★", blockColor, 0, -hs, 0));
            faces.add(new Face(90, 0, 0, 0, -hs, "❤", blockColor, 0, hs, 0));
            
            Collections.sort(faces);
            
            for (Face face : faces) {
                canvas.save();
                camera.save();
                
                camera.setLocation(0, 0, -10);
                camera.rotateX(rotateX);
                camera.rotateY(rotateY);
                
                camera.rotateX(face.rotX);
                camera.rotateY(face.rotY);
                camera.translate(face.transX, face.transY, face.transZ);
                
                camera.getMatrix(matrix);
                camera.restore();
                
                matrix.preTranslate(-centerX, -centerY);
                matrix.postTranslate(centerX, centerY);
                
                canvas.concat(matrix);
                
                // Real-time shading factor simulating diffuse point-light reflection
                float shadeFactor = (face.depth + hs) / (2f * hs);
                int finalColor = adjustShading(face.bgColor, 0.7f + shadeFactor * 0.4f);
                
                blockPaint.setColor(finalColor);
                canvas.drawRoundRect(centerX - hs, centerY - hs, centerX + hs, centerY + hs, 24f, 24f, blockPaint);
                
                borderPaint.setColor(adjustShading(finalColor, 0.8f));
                canvas.drawRoundRect(centerX - hs + 6, centerY - hs + 6, centerX + hs - 6, centerY + hs - 6, 18f, 18f, borderPaint);
                
                canvas.drawText(face.text, centerX, centerY - ((textPaint.descent() + textPaint.ascent()) / 2f), textPaint);
                
                canvas.restore();
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX();
            float y = event.getY();
            
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isDragging = true;
                    velocityX = 0f;
                    velocityY = 0f;
                    lastTouchX = x;
                    lastTouchY = y;
                    physicsHandler.removeCallbacks(inertiaRunnable);
                    return true;
                    
                case MotionEvent.ACTION_MOVE:
                    float dx = x - lastTouchX;
                    float dy = y - lastTouchY;
                    
                    rotateY += dx * 0.6f;
                    rotateX -= dy * 0.6f;
                    
                    rotateX = Math.max(-90f, Math.min(90f, rotateX));
                    
                    velocityX = dx * 0.6f;
                    velocityY = dy * 0.6f;
                    
                    lastTouchX = x;
                    lastTouchY = y;
                    invalidate();
                    return true;
                    
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    isDragging = false;
                    startInertia();
                    performClick();
                    return true;
            }
            return super.onTouchEvent(event);
        }

        @Override
        public boolean performClick() {
            return super.performClick();
        }

        private void startInertia() {
            physicsHandler.removeCallbacks(inertiaRunnable);
            inertiaRunnable = new Runnable() {
                @Override
                public void run() {
                    if (isDragging) return;
                    
                    rotateY += velocityX;
                    rotateX -= velocityY;
                    
                    rotateX = Math.max(-90f, Math.min(90f, rotateX));
                    
                    velocityX *= 0.95f;
                    velocityY *= 0.95f;
                    
                    invalidate();
                    
                    if (Math.abs(velocityX) > 0.05f || Math.abs(velocityY) > 0.05f) {
                        physicsHandler.postDelayed(this, 16);
                    }
                }
            };
            physicsHandler.post(inertiaRunnable);
        }
    }
}