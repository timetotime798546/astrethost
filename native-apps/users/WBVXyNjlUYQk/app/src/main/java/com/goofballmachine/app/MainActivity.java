package com.goofballmachine.app;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Random;

public class MainActivity extends Activity {

    // --- Dynamic Content Databases (Silly excuse segments) ---
    private final String[] subjects = {
        "A hyperactive squirrel",
        "My neighbor's aggressive toaster",
        "The ghost of an Victorian child",
        "A mysterious delivery guy",
        "An army of tiny garden gnomes",
        "My financial advisor (who is a parrot)",
        "A self-driving lawnmower"
    };

    private final String[] verbs = {
        "accidentally locked me in",
        "hijacked and reconfigured",
        "swallowed the security key for",
        "started holding a philosophical debate with",
        "declared a formal trade embargo against",
        "sent a text threat to"
    };

    private final String[] objects = {
        "my left running shoe.",
        "the main water pipe in my attic.",
        "my sanity and car keys.",
        "my collection of vintage buttons.",
        "the local pizza delivery route.",
        "the concepts of space-time."
    };

    // --- Dad Jokes List ---
    private final String[] dadJokes = {
        "Why did the invisible man turn down the job offer? ... He couldn't see himself doing it!",
        "What do you call a factory that makes okay products? ... A satisfactory!",
        "Why do birds fly south for the winter? ... Because it's too far to walk!",
        "What did the zero say to the eight? ... Nice belt!",
        "I told my doctor that I broke my arm in two places. He told me to stop going to those places.",
        "Why did the picture go to jail? ... Because it was framed!",
        "How does a penguin build its house? ... Igloos it together!"
    };

    // --- Suspicious Compliments list ---
    private final String[] suspiciousCompliments = {
        "You look highly competent when you keep your eyes shut.",
        "You have a marvelous face for standard radio broadcasting.",
        "You're not the worst person I have met so far today.",
        "I love how you do absolutely nothing and still look tired.",
        "That outfit makes you look like you have successfully escaped from a circus.",
        "Your capacity to absorb completely useless trivia is unparalleled."
    };

    // --- Dramatic Status events list ---
    private final String[] dramaEvents = {
        "A drop of water fell in an ocean miles away!",
        "The office printer made a weird growling noise!",
        "Somebody put a spoon in the non-metal sink compartment!",
        "An email was sent without a formal greeting!",
        "A light bulb hummed at 61Hz instead of 60Hz!"
    };

    private Random random;
    private Vibrator vibrator;

    // Layout elements
    private ScrollView mainScrollView;
    private LinearLayout shakeContainer;
    private LinearLayout layoutPanicScreen;

    // View Components
    private TextView tvExcuseOutput;
    private TextView tvDadJoke;
    private TextView tvDramaText;
    private TextView tvSuspiciousCompliment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        random = new Random();
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Map Views
        mainScrollView = (ScrollView) findViewById(R.id.main_scroll_view);
        shakeContainer = (LinearLayout) findViewById(R.id.shake_container);
        layoutPanicScreen = (LinearLayout) findViewById(R.id.layout_panic_screen);

        tvExcuseOutput = (TextView) findViewById(R.id.tv_excuse_output);
        tvDadJoke = (TextView) findViewById(R.id.tv_dad_joke);
        tvDramaText = (TextView) findViewById(R.id.tv_drama_text);
        tvSuspiciousCompliment = (TextView) findViewById(R.id.tv_suspicious_compliment);

        Button btnSpinExcuse = (Button) findViewById(R.id.btn_spin_excuse);
        Button btnNextJoke = (Button) findViewById(R.id.btn_next_joke);
        Button btnDrama = (Button) findViewById(R.id.btn_drama);
        Button btnCompliment = (Button) findViewById(R.id.btn_compliment);
        Button btnPanicTrigger = (Button) findViewById(R.id.btn_panic_trigger);
        Button btnExitPanic = (Button) findViewById(R.id.btn_exit_panic);

        // Action 1: Spin Excuse (Traditional Anonymous Class listener)
        btnSpinExcuse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String sub = subjects[random.nextInt(subjects.length)];
                String verb = verbs[random.nextInt(verbs.length)];
                String obj = objects[random.nextInt(objects.length)];
                String fullExcuse = "\"" + sub + " " + verb + " " + obj + "\"";
                tvExcuseOutput.setText(fullExcuse);

                // Quick vibration tap to confirm generation
                if (vibrator != null && vibrator.hasVibrator()) {
                    vibrator.vibrate(30);
                }
            }
        });

        // Action 2: Next Dad Joke (Traditional Anonymous Class listener)
        btnNextJoke.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String joke = dadJokes[random.nextInt(dadJokes.length)];
                tvDadJoke.setText(joke);

                if (vibrator != null && vibrator.hasVibrator()) {
                    vibrator.vibrate(20);
                }
            }
        });

        // Action 3: Red Alert Drama Generator (Shakes screen and emits Tone sound)
        btnDrama.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Shake screen
                TranslateAnimation shakeAnim = new TranslateAnimation(-15, 15, 0, 0);
                shakeAnim.setDuration(80);
                shakeAnim.setRepeatCount(5);
                shakeAnim.setRepeatMode(TranslateAnimation.REVERSE);
                shakeContainer.startAnimation(shakeAnim);

                // Play drama tone sound programmatically using ToneGenerator
                try {
                    ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
                    toneGen.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 300);
                } catch (Exception ignored) {
                }

                // High intense vibration
                if (vibrator != null && vibrator.hasVibrator()) {
                    vibrator.vibrate(400);
                }

                // Update text
                String incident = dramaEvents[random.nextInt(dramaEvents.length)];
                tvDramaText.setText("🚨 EMERGENCY: " + incident);
            }
        });

        // Action 4: Suspicious Compliment Machine
        btnCompliment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String insult = suspiciousCompliments[random.nextInt(suspiciousCompliments.length)];
                tvSuspiciousCompliment.setText("\"" + insult + "\"");

                if (vibrator != null && vibrator.hasVibrator()) {
                    vibrator.vibrate(15);
                }
            }
        });

        // Action 5: Enter Boss Panic Mode (Swaps layouts instantly)
        btnPanicTrigger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainScrollView.setVisibility(View.GONE);
                layoutPanicScreen.setVisibility(View.VISIBLE);

                // Intense panic pattern vibration
                if (vibrator != null && vibrator.hasVibrator()) {
                    vibrator.vibrate(100);
                }
            }
        });

        // Action 6: Exit Boss Panic Mode
        btnExitPanic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutPanicScreen.setVisibility(View.GONE);
                mainScrollView.setVisibility(View.VISIBLE);

                if (vibrator != null && vibrator.hasVibrator()) {
                    vibrator.vibrate(50);
                }
            }
        });
    }
}