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

    private final String[] cardColors = {
        "#E57373", "#F06292", "#BA68C8", "#9575CD", "#7986CB", "#64B5F6", 
        "#4FC3F7", "#4DD0E1", "#4DB6AC", "#81C784", "#AED581", "#D4E157", 
        "#FFD54F", "#FFB74D", "#FF8A65", "#A1887F", "#90A4AE", "#78909C"
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
        
        navBtnLearn.setBackgroundColor(Color.parseColor("#E0E0E0"));
        navBtnLearn.setTextColor(Color.parseColor("#757575"));
        navBtnQuiz.setBackgroundColor(Color.parseColor("#E0E0E0"));
        navBtnQuiz.setTextColor(Color.parseColor("#757575"));
        navBtnTrace.setBackgroundColor(Color.parseColor("#E0E0E0"));
        navBtnTrace.setTextColor(Color.parseColor("#757575"));
        
        toolbarScore.setVisibility(View.GONE);

        if (mode == 1) {
            containerLearn.setVisibility(View.VISIBLE);
            navBtnLearn.setBackgroundColor(Color.parseColor("#C5CAE9"));
            navBtnLearn.setTextColor(Color.parseColor("#3F51B5"));
            speak("Let's explore our letters!");
        } else if (mode == 2) {
            containerQuiz.setVisibility(View.VISIBLE);
            navBtnQuiz.setBackgroundColor(Color.parseColor("#C5CAE9"));
            navBtnQuiz.setTextColor(Color.parseColor("#3F51B5"));
            toolbarScore.setVisibility(View.VISIBLE);
            generateQuizQuestion();
        } else if (mode == 3) {
            containerTrace.setVisibility(View.VISIBLE);
            navBtnTrace.setBackgroundColor(Color.parseColor("#C5CAE9"));
            navBtnTrace.setTextColor(Color.parseColor("#3F51B5"));
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
            
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setCornerRadius(16f);
            shape.setColor(Color.parseColor(cardColors[index % cardColors.length]));
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

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    // Grid adapter for rendering beautiful rounded letters cards
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
            
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setCornerRadius(18f);
            shape.setColor(Color.parseColor(cardColors[position % cardColors.length]));
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
            paint.setColor(Color.parseColor("#3F51B5"));
            paint.setAntiAlias(true);
            paint.setStrokeWidth(16f);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeCap(Paint.Cap.ROUND);

            bgPaint = new Paint();
            bgPaint.setColor(Color.parseColor("#E0E0E0"));
            bgPaint.setAntiAlias(true);
            bgPaint.setTextSize(340f);
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