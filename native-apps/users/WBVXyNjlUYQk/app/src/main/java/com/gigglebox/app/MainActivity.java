package com.gigglebox.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;

public class MainActivity extends Activity {

    private TextView jokeTextView;
    private TextView excuseTextView;
    private TextView giggleCounterView;
    private Button jokeButton;
    private Button excuseButton;
    private Button copyButton;
    
    private Button catWork;
    private Button catSchool;
    private Button catGym;

    private Button soundBtnLaser;
    private Button soundBtnBounce;
    private Button soundBtnRaspberry;
    private Button prankCrackBtn;

    private int giggleCount = 0;
    private String currentExcuseCategory = "Work";
    private final Random random = new Random();

    // Funny Data Reservoirs
    private final String[] dadJokes = {
        "Why don't scientists trust atoms? Because they make up everything!",
        "What do you call a fake noodle? An impasta!",
        "How does a penguin build its house? Igloos it together!",
        "Why did the scarecrow win an award? Because he was outstanding in his field!",
        "Why don't skeletons fight each other? They don't have the guts.",
        "What do you call a belt made out of watches? A waist of time.",
        "How do you organize a space party? You planet.",
        "Why did the bicycle fall over? Because it was two-tired!",
        "I'm reading a book on gravity. I just can't put it down!",
        "What do you call a sleeping bull? A bulldozer.",
        "Did you hear about the guy who invented the knock-knock joke? He won the 'no-bell' prize!"
    };

    private final String[] excusesWork = {
        "I was on my way but my garage door opener was possessed and wouldn't open.",
        "My dog scheduled an emergency meeting with the neighborhood council.",
        "I stepped into a portal in my kitchen and lost 40 minutes.",
        "A rogue Roomba trapped me in my own bathroom.",
        "My coffee maker was holding my morning brew hostage until I negotiated terms."
    };

    private final String[] excusesSchool = {
        "My cat fell asleep on my laptop and she looked so peaceful I couldn't wake her.",
        "The wind was blowing exactly 4 mph too fast for safe walking velocities.",
        "I calculated that if I left home, the solar system alignment would trigger a gravity anomaly.",
        "My homework accidentally joined a minimalist art movement and cannot be disturbed.",
        "I forgot how to gravity for about an hour and was floating near the ceiling."
    };

    private final String[] excusesGym = {
        "I ran out of motivational soundtracks and was forced to contemplate life instead.",
        "My workout clothes are currently experiencing an existential crisis in the dryer.",
        "I did 500 reps of intense blinking. That is enough physical strain for a Tuesday.",
        "The dumbbell winked at me suspiciously and I did not feel safe lifting it.",
        "My left shoe has decided to start its own political career and refuses to collaborate."
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind Views
        jokeTextView = (TextView) findViewById(R.id.jokeText);
        excuseTextView = (TextView) findViewById(R.id.excuseText);
        giggleCounterView = (TextView) findViewById(R.id.giggleCounter);
        jokeButton = (Button) findViewById(R.id.jokeButton);
        excuseButton = (Button) findViewById(R.id.excuseButton);
        copyButton = (Button) findViewById(R.id.copyButton);

        catWork = (Button) findViewById(R.id.catWork);
        catSchool = (Button) findViewById(R.id.catSchool);
        catGym = (Button) findViewById(R.id.catGym);

        soundBtnLaser = (Button) findViewById(R.id.soundBtnLaser);
        soundBtnBounce = (Button) findViewById(R.id.soundBtnBounce);
        soundBtnRaspberry = (Button) findViewById(R.id.soundBtnRaspberry);
        prankCrackBtn = (Button) findViewById(R.id.prankCrackBtn);

        // Category Highlight logic
        updateCategorySelection();

        // Register Action Listeners
        jokeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateDadJoke();
            }
        });

        excuseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateExcuse();
            }
        });

        copyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyExcuseToClipboard();
            }
        });

        catWork.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentExcuseCategory = "Work";
                updateCategorySelection();
            }
        });

        catSchool.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentExcuseCategory = "School";
                updateCategorySelection();
            }
        });

        catGym.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentExcuseCategory = "Gym";
                updateCategorySelection();
            }
        });

        // Cartoon sound events
        soundBtnLaser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playCartoonSound(1);
            }
        });

        soundBtnBounce.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playCartoonSound(2);
            }
        });

        soundBtnRaspberry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playCartoonSound(3);
            }
        });

        // Prank trigger
        prankCrackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCrackedScreenPrank();
            }
        });
    }

    private void generateDadJoke() {
        int index = random.nextInt(dadJokes.length);
        jokeTextView.setText(dadJokes[index]);
        giggleCount++;
        giggleCounterView.setText("Total giggles: " + giggleCount);
        
        // Soft tactile confirmation
        triggerVibration(40);
    }

    private void generateExcuse() {
        String excuse = "";
        if ("Work".equals(currentExcuseCategory)) {
            excuse = excusesWork[random.nextInt(excusesWork.length)];
        } else if ("School".equals(currentExcuseCategory)) {
            excuse = excusesSchool[random.nextInt(excusesSchool.length)];
        } else {
            excuse = excusesGym[random.nextInt(excusesGym.length)];
        }
        excuseTextView.setText(excuse);
        triggerVibration(40);
    }

    private void copyExcuseToClipboard() {
        String excuseText = excuseTextView.getText().toString();
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Funny Excuse", excuseText);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Excuse copied to clipboard! Go deliver it!", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateCategorySelection() {
        // Reset colors
        catWork.setBackgroundResource(R.drawable.button_bg);
        catSchool.setBackgroundResource(R.drawable.button_bg);
        catGym.setBackgroundResource(R.drawable.button_bg);

        // Highlight selected
        if ("Work".equals(currentExcuseCategory)) {
            catWork.setBackgroundResource(R.drawable.button_bg_purple);
        } else if ("School".equals(currentExcuseCategory)) {
            catSchool.setBackgroundResource(R.drawable.button_bg_purple);
        } else if ("Gym".equals(currentExcuseCategory)) {
            catGym.setBackgroundResource(R.drawable.button_bg_purple);
        }
    }

    // Synthesize Cartoon Retro soundscapes programmatically
    private void playCartoonSound(final int type) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int sampleRate = 8000;
                int durationMs = 350;
                int numSamples = durationMs * sampleRate / 1000;
                double[] sample = new double[numSamples];
                byte[] generatedSnd = new byte[2 * numSamples];

                for (int i = 0; i < numSamples; ++i) {
                    double t = (double) i / sampleRate;
                    double frequency = 440;

                    if (type == 1) { // Laser Beam pitch drop
                        frequency = 1100 - (t * 2600);
                        if (frequency < 120) frequency = 120;
                        sample[i] = Math.sin(2 * Math.PI * frequency * t);
                    } else if (type == 2) { // Cartoon Bounce (swept frequency up & down)
                        frequency = 200 + Math.sin(t * Math.PI * 4) * 150;
                        sample[i] = Math.sin(2 * Math.PI * frequency * t);
                    } else { // Raspberry/Fart noise (low noisy square wave)
                        frequency = 75 + (Math.random() * 35);
                        double rawWave = Math.sin(2 * Math.PI * frequency * t);
                        // Convert to rough square wave
                        sample[i] = rawWave > 0 ? 0.7 : -0.7;
                    }
                }

                int idx = 0;
                for (double dVal : sample) {
                    short val = (short) (dVal * 32767);
                    generatedSnd[idx++] = (byte) (val & 0x00ff);
                    generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
                }

                AudioTrack audioTrack = null;
                try {
                    audioTrack = new AudioTrack(
                            AudioManager.STREAM_MUSIC,
                            sampleRate,
                            AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            generatedSnd.length,
                            AudioTrack.MODE_STATIC
                    );
                    audioTrack.write(generatedSnd, 0, generatedSnd.length);
                    audioTrack.play();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (audioTrack != null) {
                        // Let sound complete playing then release
                        try {
                            Thread.sleep(durationMs + 50);
                            audioTrack.release();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    private void triggerVibration(long ms) {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(ms);
        }
    }

    // Interactive custom full-screen prank overlay dialog
    private void showCrackedScreenPrank() {
        final Dialog prankDialog = new Dialog(this, android.R.style.Theme_NoTitleBar_Fullscreen);
        
        // Custom interactive broken-glass Canvas drawer view
        PrankView prankView = new PrankView(this, new Runnable() {
            private int taps = 0;
            @Override
            public void run() {
                taps++;
                if (taps >= 5) {
                    prankDialog.dismiss();
                    Toast.makeText(MainActivity.this, "Prank deactivated!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Tap " + (5 - taps) + " more times to escape!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        prankDialog.setContentView(prankView);
        prankDialog.setCancelable(false);

        // Play crack effect (vibration and high-pitch break click)
        triggerVibration(300);
        playPrankSoundCrack();

        prankDialog.show();
    }

    private void playPrankSoundCrack() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int sampleRate = 8000;
                int durationMs = 120;
                int numSamples = durationMs * sampleRate / 1000;
                byte[] generatedSnd = new byte[2 * numSamples];

                for (int i = 0; i < numSamples; ++i) {
                    // White noise mixed with high frequency decay
                    double t = (double) i / sampleRate;
                    double freq = 3000 * Math.exp(-20 * t);
                    double signal = Math.sin(2 * Math.PI * freq * t) * (Math.random() - 0.5);
                    short val = (short) (signal * 32767);
                    generatedSnd[i*2] = (byte) (val & 0x00ff);
                    generatedSnd[i*2+1] = (byte) ((val & 0xff00) >>> 8);
                }

                AudioTrack track = new AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        generatedSnd.length,
                        AudioTrack.MODE_STATIC
                );
                track.write(generatedSnd, 0, generatedSnd.length);
                track.play();
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {}
                track.release();
            }
        }).start();
    }

    // custom dynamic broken screen render layout
    private static class PrankView extends View {
        private final Paint crackPaint;
        private final Paint textPaint;
        private final Runnable escapeCallback;

        public PrankView(Context context, Runnable escapeCallback) {
            super(context);
            this.escapeCallback = escapeCallback;

            crackPaint = new Paint();
            crackPaint.setColor(Color.argb(220, 240, 240, 240));
            crackPaint.setStyle(Paint.Style.STROKE);
            crackPaint.setStrokeWidth(4f);
            crackPaint.setAntiAlias(true);

            textPaint = new Paint();
            textPaint.setColor(Color.argb(120, 255, 255, 255));
            textPaint.setTextSize(36f);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setAntiAlias(true);

            setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    PrankView.this.escapeCallback.run();
                }
            });
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            
            // Render very realistic pitch black / dark underlying fracture lines
            canvas.drawColor(Color.argb(80, 0, 0, 0));

            int w = getWidth();
            int h = getHeight();

            // Set up 3 central impact epicenters
            drawCrackPattern(canvas, w / 3, h / 4, 150);
            drawCrackPattern(canvas, w / 2, h / 2 + 100, 250);
            drawCrackPattern(canvas, w * 3 / 4, h / 3, 180);

            // Subtle reminder instructions watermarked on screen
            canvas.drawText("[ System Failure - Screen Cracked ]", w / 2, h - 100, textPaint);
        }

        private void drawCrackPattern(Canvas canvas, float cx, float cy, float maxRadius) {
            int lines = 12;
            for (int i = 0; i < lines; i++) {
                double angle = (2 * Math.PI / lines) * i + (Math.random() * 0.3);
                float currX = cx;
                float currY = cy;

                // Trace segmented fracture vectors
                int segments = 5;
                float dist = maxRadius / segments;
                for (int j = 0; j < segments; j++) {
                    float nextX = currX + (float) (Math.cos(angle) * dist) + (float) ((Math.random() - 0.5) * 40);
                    float nextY = currY + (float) (Math.sin(angle) * dist) + (float) ((Math.random() - 0.5) * 40);
                    
                    // Main vector
                    crackPaint.setStrokeWidth(5f / (j + 1));
                    canvas.drawLine(currX, currY, nextX, nextY, crackPaint);
                    
                    // Side fracture spider web branches
                    if (Math.random() > 0.4) {
                        float branchAngle = (float) (angle + (Math.random() - 0.5) * 1.2);
                        float branchX = nextX + (float) (Math.cos(branchAngle) * dist * 0.7);
                        float branchY = nextY + (float) (Math.sin(branchAngle) * dist * 0.7);
                        crackPaint.setStrokeWidth(1.5f);
                        canvas.drawLine(nextX, nextY, branchX, branchY, crackPaint);
                    }

                    currX = nextX;
                    currY = nextY;
                }
            }

            // Draw immediate circular impact point cracks
            crackPaint.setStyle(Paint.Style.STROKE);
            crackPaint.setStrokeWidth(3f);
            canvas.drawCircle(cx, cy, 25, crackPaint);
            canvas.drawCircle(cx, cy, 55, crackPaint);
        }
    }
}