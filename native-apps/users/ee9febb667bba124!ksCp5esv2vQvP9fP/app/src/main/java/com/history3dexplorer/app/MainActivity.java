package com.history3dexplorer.app;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private RelativeLayout splashLayout;
    private ProgressBar splashProgress;
    private TextView txtSplashMessage;

    private History3DCarouselView carouselView;
    private TextView txtEraTitle;
    private TextView txtEraYear;
    private TextView txtEraDescription;
    private Button btnSoundToggle;
    private Button btnTriviaChallenge;
    private Button btnQuickFact;
    private Button btnInfo;

    private List<HistoryEra> eras = new ArrayList<>();
    private HistoryEra currentEra;

    @Override
    protected void onCreate(Bundle savedInstanceState) { 
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        splashLayout = (RelativeLayout) findViewById(R.id.splash_layout);
        splashProgress = (ProgressBar) findViewById(R.id.splash_progress);
        txtSplashMessage = (TextView) findViewById(R.id.txt_splash_loading_message);

        carouselView = (History3DCarouselView) findViewById(R.id.carousel_view);
        txtEraTitle = (TextView) findViewById(R.id.txt_era_title);
        txtEraYear = (TextView) findViewById(R.id.txt_era_year);
        txtEraDescription = (TextView) findViewById(R.id.txt_era_description);
        btnSoundToggle = (Button) findViewById(R.id.btn_sound_toggle);
        btnTriviaChallenge = (Button) findViewById(R.id.btn_trivia_challenge);
        btnQuickFact = (Button) findViewById(R.id.btn_quick_fact);
        btnInfo = (Button) findViewById(R.id.btn_info);

        setupErasData();
        setupSoundToggle();
        setupInfoButton();
        setupInteractiveButtons();

        startAppLoader();
    }

    private void startAppLoader() {
        SoundSynthesizer.playSound(1);

        final Handler handler = new Handler();
        final int totalDuration = 2500;
        final int interval = 50;
        final int steps = totalDuration / interval;

        Runnable progressRunnable = new Runnable() {
            int currentStep = 0;
            @Override
            public void run() {
                if (currentStep <= steps) {
                    int progress = (int) (((float) currentStep / steps) * 100);
                    splashProgress.setProgress(progress);

                    if (progress < 25) {
                        txtSplashMessage.setText("Unlocking time-travel portals...");
                    } else if (progress < 50) {
                        txtSplashMessage.setText("Aligning 3D temporal matrices...");
                    } else if (progress < 75) {
                        txtSplashMessage.setText("Restoring historic tapestries...");
                    } else {
                        txtSplashMessage.setText("Calibrating hardware synthesizers...");
                    }

                    currentStep++;
                    handler.postDelayed(this, interval);
                } else {
                    AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
                    fadeOut.setDuration(500);
                    fadeOut.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {}

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            splashLayout.setVisibility(View.GONE);
                            initializeCarousel();
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {}
                    });
                    splashLayout.startAnimation(fadeOut);
                } 
            }
        };
        handler.post(progressRunnable);
    }

    private void setupErasData() {
        eras.add(new HistoryEra(
                "Ancient Egypt", "3100 BC",
                "An empire along the Nile famous for Pharaohs, towering pyramids, Hieroglyphics, and deep mystical mythology that survived millennia.",
                "Pyramids were built by highly paid contract laborers who took great pride in their architectural craftsmanship, rather than slaves.",
                0xFF5D4037,
                "Who built the magnificent Egyptian pyramids?",
                new String[]{"Enslaved prisoners", "Paid contract laborers", "Visiting extraterrestrials"},
                1,
                "Archaeological excavations show workers had structured, comfortable camps, paid wages, and were fed premium rations."
        ));

        eras.add(new HistoryEra(
                "Classical Greece", "500 BC",
                "Birthplace of modern democracy, philosophy, mathematics, and dramatic arts. Thinkers like Socrates shaped modern logic.",
                "The word 'school' comes from the ancient Greek word 'schole', which originally meant 'free time' or 'leisure'.",
                0xFF0D47A1,
                "What did the Greek origin word 'schole' (school) mean?",
                new String[]{"Mandatory labor", "Unending trials", "Leisure or free time"},
                2,
                "To the ancient Greeks, structured learning was a pursuit reserved for free leisure hours when basic labor was done."
        ));

        eras.add(new HistoryEra(
                "Roman Empire", "27 BC",
                "A colossal civilization renowned for engineering marvels (arched aqueducts, concrete) and military dominance across the Mediterranean.",
                "Romans valued urine highly and used it as mouthwash! The high ammonia concentration was highly effective at whitening teeth.",
                0xFF880E4F,
                "What did ancient Romans utilize as a premium teeth whitening wash?",
                new String[]{"Crushed charcoal", "Ammonia-rich urine", "Chalky limestone water"},
                1,
                "Urine was so highly sought after for oral hygiene and laundering that emperors levied a dedicated tax on its collection."
        ));

        eras.add(new HistoryEra(
                "Medieval Era", "476 AD",
                "A feudal epoch across Europe characterized by giant stone castles, chivalric knights, heraldry, and deep monastic religious devotion.",
                "Standard battle armor weighed up to 50 pounds, but was so well distributed that a knight could easily run and mount horses.",
                0xFF37474F,
                "Approximately how heavy was standard medieval battle armor?",
                new String[]{"Around 15 pounds", "Around 50 pounds", "Around 125 pounds"},
                1,
                "Despite looking incredibly stiff, the articulated plates allowed complete mobility and high combat agility."
        ));

        eras.add(new HistoryEra(
                "Renaissance", "1300 AD",
                "A brilliant cultural and intellectual rebirth. Scientific observation, naturalistic art, and philosophical reason flourished in Europe.",
                "The legendary Leonardo da Vinci could write backwards with one hand and sketch realistic forms with the other simultaneously.",
                0xFF1B5E20,
                "What unique motor capability did Leonardo da Vinci possess?",
                new String[]{"Writing backwards and drawing simultaneously", "Writing fluidly with either foot", "Blindfolded drafting"},
                0,
                "Being naturally left-handed, Da Vinci utilized mirroring scripts from right to left to prevent smearing ink."
        ));

        eras.add(new HistoryEra(
                "Industrial Era", "1760 AD",
                "The global transition to mechanization, steam engines, massive factories, and fast railroads, completely restructuring humanity.",
                "The first commercial steam locomotive designed by Richard Trevithick chugged along at a top speed of just 5 miles per hour.",
                0xFFE65100,
                "What was the top speed capability of the earliest steam locomotive?",
                new String[]{"5 Miles per hour", "25 Miles per hour", "55 Miles per hour"},
                0,
                "The locomotive was incredibly slow, but it proved that steam power could move massive tonnages on iron rails."
        ));

        eras.add(new HistoryEra(
                "Space Age", "1957 AD",
                "Humanity ventures beyond Earth's bounds, launching cosmic probes, landing on the Moon, and sending rovers to explore distant planets.",
                "The Apollo 11 navigation system carried exponentially less computational power than today's standard electric toothbrushes.",
                0xFF311B92,
                "How powerful was the historic Apollo 11 guidance computer?",
                new String[]{"Slower than a digital toothbrush", "Equal to a modern high-end laptop", "Faster than an Apple iPhone"},
                0,
                "The guidance computer operated at roughly 1 MHz, whereas low-cost microcontrollers in home gadgets run much faster."
        ));
    }

    private void setupSoundToggle() {
        btnSoundToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SoundSynthesizer.isSoundEnabled = !SoundSynthesizer.isSoundEnabled;
                if (SoundSynthesizer.isSoundEnabled) {
                    btnSoundToggle.setText("🔊");
                    SoundSynthesizer.playSound(3);
                } else {
                    btnSoundToggle.setText("🔇");
                }
            }
        });
    }

    private void setupInfoButton() {
        btnInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SoundSynthesizer.playSound(3);
                final Dialog dialog = new Dialog(MainActivity.this, android.R.style.Theme_Material_Dialog_NoActionBar);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

                LinearLayout root = new LinearLayout(MainActivity.this);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setBackgroundColor(0xFF1E1E1E);
                root.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));
                root.setGravity(Gravity.CENTER_HORIZONTAL);

                TextView title = new TextView(MainActivity.this);
                title.setText("3D Graphics & Sound Info");
                title.setTextColor(0xFFFFD700);
                title.setTextSize(20);
                title.setPadding(0, 0, 0, dpToPx(12));
                root.addView(title);

                TextView infoText = new TextView(MainActivity.this);
                infoText.setText("This application features a custom 3D carousel engine developed using pure Android Canvas.\n\n" + 
                        "By applying real-time spatial matrix projection and camera perspective calculations, we project cards onto Z-depth planes. " + 
                        "We render accurate depths by employing a Painter's Algorithm layer sorting mechanism.\n\n" + 
                        "Additionally, all sound cues are synthesized programmatically at runtime using Android's native PCM AudioTrack APIs, bypassing bulky media files.");
                infoText.setTextColor(0xFFDDDDDD);
                infoText.setTextSize(14);
                infoText.setLineSpacing(1.2f, 1.0f);
                infoText.setPadding(0, 0, 0, dpToPx(20));
                root.addView(infoText);

                Button okBtn = new Button(MainActivity.this);
                okBtn.setText("Got it");
                okBtn.setTextColor(0xFF111111);
                okBtn.setBackgroundColor(0xFFFFD700);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dpToPx(110), LinearLayout.LayoutParams.WRAP_CONTENT);
                okBtn.setLayoutParams(params);
                okBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
                root.addView(okBtn);

                dialog.setContentView(root);
                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                }
                dialog.show();
            }
        });
    }

    private void setupInteractiveButtons() {
        btnTriviaChallenge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentEra != null) {
                    showTriviaDialog(currentEra);
                }
            }
        });

        btnQuickFact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentEra != null) {
                    showDidYouKnowDialog(currentEra);
                }
            }
        });
    }

    private void initializeCarousel() {
        carouselView.setEras(eras);
        carouselView.setOnEraChangedListener(new History3DCarouselView.OnEraChangedListener() {
            @Override
            public void onEraChanged(int index, HistoryEra era) {
                updateSelectedEra(era);
            }

            @Override
            public void onEraSelected(int index, HistoryEra era) {
                SoundSynthesizer.playSound(3);
                showDidYouKnowDialog(era);
            }
        });
        updateSelectedEra(eras.get(0));
    }

    private void updateSelectedEra(HistoryEra era) {
        currentEra = era;
        txtEraTitle.setText(era.title);
        txtEraYear.setText(era.year);
        txtEraDescription.setText(era.description);
    }

    private void showTriviaDialog(final HistoryEra era) {
        SoundSynthesizer.playSound(3);
        final Dialog dialog = new Dialog(this, android.R.style.Theme_Material_Dialog_NoActionBar);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF1E1E1E);
        root.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        
        TextView title = new TextView(this);
        title.setText("💡 " + era.title + " Trivia");
        title.setTextColor(0xFFFFD700);
        title.setTextSize(20);
        title.setPadding(0, 0, 0, dpToPx(16));
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView questionText = new TextView(this);
        questionText.setText(era.triviaQuestion);
        questionText.setTextColor(0xFFFFFFFF);
        questionText.setTextSize(16);
        questionText.setPadding(0, 0, 0, dpToPx(20));
        questionText.setGravity(Gravity.CENTER);
        root.addView(questionText);

        final Button[] optionButtons = new Button[era.triviaOptions.length];
        for (int i = 0; i < era.triviaOptions.length; i++) {
            final int index = i;
            final Button btn = new Button(this);
            btn.setText(era.triviaOptions[i]);
            btn.setTextColor(0xFFFFFFFF);
            btn.setBackgroundColor(0xFF2E2E2E);
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, dpToPx(10));
            btn.setLayoutParams(params);
            
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    for (Button b : optionButtons) {
                        b.setEnabled(false);
                    }
                    
                    if (index == era.correctAnswerIndex) {
                        btn.setBackgroundColor(0xFF4CAF50);
                        btn.setTextColor(0xFFFFFFFF);
                        SoundSynthesizer.playSound(4);
                    } else {
                        btn.setBackgroundColor(0xFFF44336);
                        optionButtons[era.correctAnswerIndex].setBackgroundColor(0xFF4CAF50);
                        optionButtons[era.correctAnswerIndex].setTextColor(0xFFFFFFFF);
                        SoundSynthesizer.playSound(5);
                    }
                    
                    TextView explanation = new TextView(MainActivity.this);
                    explanation.setText("\nExplanation:\n" + era.triviaExplanation);
                    explanation.setTextColor(0xFFCCCCCC);
                    explanation.setTextSize(14);
                    explanation.setPadding(0, dpToPx(10), 0, 0);
                    explanation.setGravity(Gravity.LEFT);
                    
                    ((LinearLayout) v.getParent()).addView(explanation);
                }
            });
            optionButtons[i] = btn;
            root.addView(btn);
        }

        Button closeBtn = new Button(this);
        closeBtn.setText("Close");
        closeBtn.setTextColor(0xFF111111);
        closeBtn.setBackgroundColor(0xFFFFD700);
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
                dpToPx(120),
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        closeParams.setMargins(0, dpToPx(20), 0, 0);
        closeBtn.setLayoutParams(closeParams);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        root.addView(closeBtn);

        dialog.setContentView(root);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();
    }

    private void showDidYouKnowDialog(HistoryEra era) {
        SoundSynthesizer.playSound(3);
        final Dialog dialog = new Dialog(this, android.R.style.Theme_Material_Dialog_NoActionBar);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF1E1E1E);
        root.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("🔍 Did You Know? — " + era.title);
        title.setTextColor(0xFF00BFFF);
        title.setTextSize(20);
        title.setPadding(0, 0, 0, dpToPx(16));
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView factText = new TextView(this);
        factText.setText(era.funFact);
        factText.setTextColor(0xFFFFFFFF);
        factText.setTextSize(16);
        factText.setPadding(0, 0, 0, dpToPx(20));
        factText.setGravity(Gravity.CENTER);
        root.addView(factText);

        Button closeBtn = new Button(this);
        closeBtn.setText("Awesome!");
        closeBtn.setTextColor(0xFFFFFFFF);
        closeBtn.setBackgroundColor(0xFF333333);
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
                dpToPx(120),
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        closeBtn.setLayoutParams(closeParams);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        root.addView(closeBtn);

        dialog.setContentView(root);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}