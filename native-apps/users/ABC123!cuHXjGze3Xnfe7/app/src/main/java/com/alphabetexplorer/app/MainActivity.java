package com.alphabetexplorer.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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

    // Premium vibrant child-friendly gradient pairs (Start Color, End Color)
    private final String[][] gradientPairs = {
        {"#FF6B6B", "#FF8E53"}, // Warm Sunset Red-Orange
        {"#FF8E53", "#FFD200"}, // Juicy Tangerine
        {"#FFD200", "#FFB302"}, // Honey Gold
        {"#4CD137", "#44BD32"}, // Fresh Grass Green
        {"#00CEC9", "#00A8FF"}, // Bright Cyan Blue
        {"#9C27B0", "#E91E63"}, // Vibrant Purple-Pink
        {"#E91E63", "#FF7675"}, // Coral Rose
        {"#00A8FF", "#3742FA"}, // Clear Sky to Deep Blue
        {"#6C5CE7", "#A29BFE"}, // Elegant Lavender Purple
        {"#FDA7DF", "#D980FA"}, // Sweet Cotton Candy
        {"#FFEAA7", "#FDCB6E"}, // Pastel Banana Yellow
        {"#55EFC4", "#81ECEC"}, // Minty Turquoise
        {"#81ECEC", "#00CEC9"}, // Electric Aqua
        {"#74B9FF", "#0984E3"}, // Deep Sky Blue
        {"#FAB1A0", "#FF7675"}, // Soft Peach-Coral
        {"#DF80AC", "#E91E63"}, // Sugar Plum Pink
        {"#FF7675", "#D63031"}  // Crimson Berry
    };

    private TextToSpeech tts;
    private boolean isTtsInitialized = false;

    private int currentLearnIndex = 0;
    private int currentTraceIndex = 0;
    private int quizTargetIndex = 0;
    private int score = 0;

    private DrawingView drawingView;

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

        // Load mode 1 immediately to build gorgeous custom graphics
        switchMode(1);

        // Welcome Greeting
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                speak("Welcome to Alphabet Explorer! Let's learn letters from A to Z.");
            }
        }, 800);
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

    private void setButtonState(Button button, boolean active, String activeBgColor, String activeTextColor) {
        if (active) {
            GradientDrawable activePill = new GradientDrawable();
            activePill.setShape(GradientDrawable.RECTANGLE);
            float radius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics());
            activePill.setCornerRadius(radius);
            activePill.setColor(Color.parseColor(activeBgColor));
            button.setBackground(activePill);
            button.setTextColor(Color.parseColor(activeTextColor));
        } else {
            GradientDrawable inactivePill = new GradientDrawable();
            inactivePill.setShape(GradientDrawable.RECTANGLE);
            float radius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics());
            inactivePill.setCornerRadius(radius);
            inactivePill.setColor(Color.parseColor("#F1F2F6"));
            button.setBackground(inactivePill);
            button.setTextColor(Color.parseColor("#8F9BB3"));
        }
    }

    private void switchMode(int mode) {
        View containerLearn = findViewById(R.id.container_learn);
        View containerQuiz = findViewById(R.id.container_quiz);
        View containerTrace = findViewById(R.id.container_trace);
        
        Button navBtnLearn = findViewById(R.id.nav_btn_learn);
        Button navBtnQuiz = findViewById(R.id.nav_btn_quiz);
        Button navBtnTrace = findViewById(R.id.nav_btn_trace);
        
        TextView toolbarScore = findViewById(R.id.toolbar_score);
        
        findViewById(R.id.detail_overlay).setVisibility(View.GONE);

        containerLearn.setVisibility(View.GONE);
        containerQuiz.setVisibility(View.GONE);
        containerTrace.setVisibility(View.GONE);
        
        setButtonState(navBtnLearn, false, null, null);
        setButtonState(navBtnQuiz, false, null, null);
        setButtonState(navBtnTrace, false, null, null);
        
        toolbarScore.setVisibility(View.GONE);

        if (mode == 1) {
            containerLearn.setVisibility(View.VISIBLE);
            setButtonState(navBtnLearn, true, "#6C5CE7", "#FFFFFF");
            speak("Let's explore our letters!");
        } else if (mode == 2) {
            containerQuiz.setVisibility(View.VISIBLE);
            setButtonState(navBtnQuiz, true, "#FF6B6B", "#FFFFFF");
            toolbarScore.setVisibility(View.VISIBLE);
            generateQuizQuestion();
        } else if (mode == 3) {
            containerTrace.setVisibility(View.VISIBLE);
            setButtonState(navBtnTrace, true, "#00B894", "#FFFFFF");
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
            
            // Build magnificent gradient shapes for quiz buttons
            int pairIndex = index % gradientPairs.length;
            String[] colors = gradientPairs[pairIndex];
            
            GradientDrawable shape = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[] { Color.parseColor(colors[0]), Color.parseColor(colors[1]) }
            );
            shape.setShape(GradientDrawable.RECTANGLE);
            float r = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());
            shape.setCornerRadius(r);
            shape.setStroke(4, Color.parseColor("#FFFFFF"));
            buttons[i].setBackground(shape);
        }
        
        speakQuizQuestion();
    }

    private void speakQuizQuestion() {
        speak("Find the letter " + letters[quizTargetIndex] + "!");
    }

    private void evaluateAnswer(int selectedIndex) {
        if (selectedIndex == quizTargetIndex) {
            score += 10;
            speak("Correct! Outstanding!");
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
            speak("No, that is " + letters[selectedIndex] + ". Try again!");
            Toast.makeText(this, "Oops! Try again.", Toast.LENGTH_SHORT).show();
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
        traceInstruction.setText("Trace the letter " + targetChar);
        speak("Let's trace " + targetChar);
    }

    private void beautifyUI() {
        // App-wide Root Ambient Background Gradient
        View rootLayout = findViewById(R.id.root_layout);
        if (rootLayout != null) {
            GradientDrawable rootBg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] { Color.parseColor("#F6F8FB"), Color.parseColor("#E4ECF4") }
            );
            rootLayout.setBackground(rootBg);
        }

        // Toolbar Wave rounded gradient design
        View toolbar = findViewById(R.id.toolbar_container);
        GradientDrawable toolbarBg = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[] { Color.parseColor("#6C5CE7"), Color.parseColor("#8E2DE2") }
        );
        toolbarBg.setShape(GradientDrawable.RECTANGLE);
        float tbRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());
        toolbarBg.setCornerRadii(new float[]{0, 0, 0, 0, tbRadius, tbRadius, tbRadius, tbRadius});
        toolbar.setBackground(toolbarBg);

        // Score Badge
        TextView toolbarScore = findViewById(R.id.toolbar_score);
        GradientDrawable scoreBg = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[] { Color.parseColor("#FFD200"), Color.parseColor("#FFA800") }
        );
        scoreBg.setShape(GradientDrawable.RECTANGLE);
        float scRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        scoreBg.setCornerRadius(scRadius);
        toolbarScore.setBackground(scoreBg);
        toolbarScore.setTextColor(Color.parseColor("#2D3436"));
        toolbarScore.setPadding(32, 12, 32, 12);

        // Listen Button Gradient
        View btnListen = findViewById(R.id.btn_quiz_listen);
        GradientDrawable listenShape = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[] { Color.parseColor("#FF8E53"), Color.parseColor("#FF6B6B") }
        );
        listenShape.setShape(GradientDrawable.RECTANGLE);
        float lr = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics());
        listenShape.setCornerRadius(lr);
        btnListen.setBackground(listenShape);

        // Detail Popup Container
        View cardContainer = findViewById(R.id.detail_card_container);
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setShape(GradientDrawable.RECTANGLE);
        float cardRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 28, getResources().getDisplayMetrics());
        cardBg.setCornerRadius(cardRadius);
        cardBg.setColor(Color.WHITE);
        cardContainer.setBackground(cardBg);

        View btnClose = findViewById(R.id.btn_close_detail);
        GradientDrawable roundClose = new GradientDrawable();
        roundClose.setShape(GradientDrawable.OVAL);
        roundClose.setColor(Color.parseColor("#F1F2F6"));
        btnClose.setBackground(roundClose);

        // Speak Phonics Button Gradient
        View btnSpeak = findViewById(R.id.btn_speak_detail);
        GradientDrawable speakShape = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[] { Color.parseColor("#00B894"), Color.parseColor("#00CEC9") }
        );
        speakShape.setShape(GradientDrawable.RECTANGLE);
        float sr = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics());
        speakShape.setCornerRadius(sr);
        btnSpeak.setBackground(speakShape);

        View btnPrev = findViewById(R.id.btn_detail_prev);
        GradientDrawable prevShape = new GradientDrawable();
        prevShape.setShape(GradientDrawable.RECTANGLE);
        float pr = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 18, getResources().getDisplayMetrics());
        prevShape.setCornerRadius(pr);
        prevShape.setColor(Color.parseColor("#F1F2F6"));
        btnPrev.setBackground(prevShape);

        // Detail Next Button Gradient
        View btnNext = findViewById(R.id.btn_detail_next);
        GradientDrawable nextShape = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[] { Color.parseColor("#6C5CE7"), Color.parseColor("#8E2DE2") }
        );
        nextShape.setShape(GradientDrawable.RECTANGLE);
        float nr = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 18, getResources().getDisplayMetrics());
        nextShape.setCornerRadius(nr);
        btnNext.setBackground(nextShape);

        // Tracing Frame Holder design
        View canvasHolder = findViewById(R.id.trace_canvas_holder);
        GradientDrawable canvasBg = new GradientDrawable();
        canvasBg.setShape(GradientDrawable.RECTANGLE);
        float cr = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics());
        canvasBg.setCornerRadius(cr);
        canvasBg.setColor(Color.WHITE);
        canvasBg.setStroke(6, Color.parseColor("#DFE6E9"));
        canvasHolder.setBackground(canvasBg);

        // Tracing control buttons with custom gradients
        View tracePrev = findViewById(R.id.btn_trace_prev);
        GradientDrawable tPrevShape = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[] { Color.parseColor("#6C5CE7"), Color.parseColor("#8E2DE2") }
        );
        tPrevShape.setShape(GradientDrawable.RECTANGLE);
        float tpr = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        tPrevShape.setCornerRadius(tpr);
        tracePrev.setBackground(tPrevShape);

        View traceNext = findViewById(R.id.btn_trace_next);
        GradientDrawable tNextShape = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[] { Color.parseColor("#6C5CE7"), Color.parseColor("#8E2DE2") }
        );
        tNextShape.setShape(GradientDrawable.RECTANGLE);
        float tnr = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        tNextShape.setCornerRadius(tnr);
        traceNext.setBackground(tNextShape);

        View traceClear = findViewById(R.id.btn_trace_clear);
        GradientDrawable tClearShape = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[] { Color.parseColor("#FF7675"), Color.parseColor("#D63031") }
        );
        tClearShape.setShape(GradientDrawable.RECTANGLE);
        float tcr = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        tClearShape.setCornerRadius(tcr);
        traceClear.setBackground(tClearShape);
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    // Grid adapter for rendering beautiful rounded letters cards with rich linear gradients
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
                int heightPx = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 100, context.getResources().getDisplayMetrics()
                );
                textView.setLayoutParams(new GridView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, heightPx
                ));
                textView.setGravity(Gravity.CENTER);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 36);
                textView.setTextColor(Color.WHITE);
                textView.setTypeface(null, Typeface.BOLD);
            } else {
                textView = (TextView) convertView;
            }

            String letter = letters[position];
            textView.setText(letter);
            
            // Build dual-color gradient backgrounds dynamically based on position
            int pairIndex = position % gradientPairs.length;
            String[] colors = gradientPairs[pairIndex];
            
            GradientDrawable shape = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[] { Color.parseColor(colors[0]), Color.parseColor(colors[1]) }
            );
            shape.setShape(GradientDrawable.RECTANGLE);
            float r = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 22, context.getResources().getDisplayMetrics());
            shape.setCornerRadius(r);
            shape.setStroke(4, Color.parseColor("#FFFFFF"));
            textView.setBackground(shape);

            return textView;
        }
    }

    // Canvas drawing View for tracing alphabet letters
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
            paint.setStrokeWidth(18f);
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
            
            // Scaled stencil calculation to support all densities
            float optimalSize = getHeight() * 0.65f;
            bgPaint.setTextSize(optimalSize);
            
            float xPos = getWidth() / 2f;
            float yPos = (getHeight() / 2f) - ((bgPaint.descent() + bgPaint.ascent()) / 2f);
            canvas.drawText(letter, xPos, yPos, bgPaint);

            canvas.drawPath(path, paint);
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
                    break;
                default:
                    return false;
            }
            invalidate();
            return true;
        }
    }
}