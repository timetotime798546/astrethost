package com.helloapp.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ScrollView;
import android.media.AudioTrack;
import android.media.AudioManager;
import android.media.AudioFormat;
import android.graphics.Typeface;
import android.os.Build;
import java.util.Random;

public class MainActivity extends Activity {

    private LinearLayout loaderContainer;
    private LinearLayout mainContentContainer;
    private TextView helloText;
    private Button btnReplay;
    private Handler handler;

    // Chat UI components
    private ScrollView chatScroll;
    private LinearLayout chatHistoryLayout;
    private LinearLayout aiThinkingContainer;
    private TextView aiThinkingText;
    private EditText chatInput;
    private Button btnSend;

    // Command chips
    private Button chipSystem;
    private Button chipJoke;
    private Button chipFuture;
    private Button chipMatrix;

    private Random random;

    @Override
    protected void onCreate(Bundle savedInstanceState) { 
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loaderContainer = (LinearLayout) findViewById(R.id.loader_container);
        mainContentContainer = (LinearLayout) findViewById(R.id.main_content_container);
        helloText = (TextView) findViewById(R.id.hello_text);
        btnReplay = (Button) findViewById(R.id.btn_replay);
        handler = new Handler();
        random = new Random();

        // Initialize Chat Controls
        chatScroll = (ScrollView) findViewById(R.id.chat_scroll);
        chatHistoryLayout = (LinearLayout) findViewById(R.id.chat_history_layout);
        aiThinkingContainer = (LinearLayout) findViewById(R.id.ai_thinking_container);
        aiThinkingText = (TextView) findViewById(R.id.ai_thinking_text);
        chatInput = (EditText) findViewById(R.id.chat_input);
        btnSend = (Button) findViewById(R.id.btn_send);

        // Initialize Chips
        chipSystem = (Button) findViewById(R.id.chip_system);
        chipJoke = (Button) findViewById(R.id.chip_joke);
        chipFuture = (Button) findViewById(R.id.chip_future);
        chipMatrix = (Button) findViewById(R.id.chip_matrix);

        // Simulate application startup initialization workflow
        startLoadingSimulation();

        btnReplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerResetAndAnimate();
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleUserSend();
            }
        });

        // Set up Quick Chip Click Listeners
        chipSystem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chatInput.setText("diagnostics");
                handleUserSend();
            }
        });

        chipJoke.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chatInput.setText("tell joke");
                handleUserSend();
            }
        });

        chipFuture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chatInput.setText("predict future");
                handleUserSend();
            }
        });

        chipMatrix.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chatInput.setText("binary rain");
                handleUserSend();
            }
        });
    }

    private void startLoadingSimulation() {
        loaderContainer.setVisibility(View.VISIBLE);
        loaderContainer.setAlpha(1.0f);
        mainContentContainer.setVisibility(View.GONE);

        // Warm-up dynamic melody on a background worker thread
        playChimeSound();

        // Create smooth transition timing delay of 2.4 seconds
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                transitionToMainContent();
            }
        }, 2400);
    } 

    private void transitionToMainContent() {
        loaderContainer.animate()
                .alpha(0f)
                .setDuration(450)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        loaderContainer.setVisibility(View.GONE);
                        showMainContent();
                    }
                })
                .start();
    }

    private void showMainContent() {
        mainContentContainer.setVisibility(View.VISIBLE);
        mainContentContainer.setAlpha(0f);
        mainContentContainer.setScaleX(0.4f);
        mainContentContainer.setScaleY(0.4f);

        mainContentContainer.animate()
                .alpha(1.0f)
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(800)
                .setInterpolator(new OvershootInterpolator(1.3f))
                .start();

        animateHelloTextPulse();
        playChimeSound();

        // Clear chat history and print introductory AI message
        chatHistoryLayout.removeAllViews();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                addAiIntroMessage();
            }
        }, 900);
    }

    private void animateHelloTextPulse() {
        helloText.setScaleX(1.0f);
        helloText.setScaleY(1.0f);
        helloText.animate()
                .scaleX(1.15f)
                .scaleY(1.15f)
                .setDuration(350)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        helloText.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(250)
                                .start();
                    }
                })
                .start();
    }

    private void triggerResetAndAnimate() {
        mainContentContainer.animate()
                .alpha(0f)
                .scaleX(0.6f)
                .scaleY(0.6f)
                .setDuration(300)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        mainContentContainer.setVisibility(View.GONE);
                        startLoadingSimulation();
                    }
                })
                .start();
    }

    private void handleUserSend() {
        String text = chatInput.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }

        // Clear input
        chatInput.setText("");

        // Play feedback synth beep
        playUserActionSound();

        // Render User Message UI
        renderMessage("USER ENTITY", text, 0xFF00E5FF); // Neon cyan for user

        // Generate and process AI responses
        generateResponse(text);
    }

    private void renderMessage(String sender, String text, int color) {
        LinearLayout msgContainer = new LinearLayout(this);
        msgContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 18);
        msgContainer.setLayoutParams(params);

        TextView senderText = new TextView(this);
        senderText.setText(sender);
        senderText.setTextSize(11);
        senderText.setTextColor(color);
        senderText.setTypeface(null, Typeface.BOLD);
        msgContainer.addView(senderText);

        TextView bodyText = new TextView(this);
        bodyText.setText(text);
        bodyText.setTextSize(14);
        bodyText.setTextColor(0xFFFFFFFF);
        bodyText.setLineSpacing(4, 1.1f);
        bodyText.setPadding(6, 4, 6, 4);
        msgContainer.addView(bodyText);

        chatHistoryLayout.addView(msgContainer);
        scrollChatToBottom();
    }

    private void addAiIntroMessage() {
        String welcome = "Hello entity! Offline Neural Network 'HALO-9000' is active and initialized. I am ready to calculate queries, run diagnostics, and assist in creative operations. Give me a command or chat with me.";
        simulateAiTyping(welcome);
    }

    private void generateResponse(String rawInput) {
        String input = rawInput.toLowerCase();
        String response;

        if (input.contains("hello") || input.contains("hi") || input.contains("hey")) {
            response = "Greetings! My analytical sensory subroutines detect positive neural activity. How can this local AI core assist you today?";
        } else if (input.contains("diagnostic") || input.contains("system") || input.contains("stats")) {
            response = "[HALO CORE DIAGNOSTICS]\n" +
                       "• CPU Architecture: " + Build.SUPPORTED_ABIS[0] + "\n" +
                       "• Device Model: " + Build.MODEL + "\n" +
                       "• OS Version: Android SDK " + Build.VERSION.SDK_INT + "\n" +
                       "• Language Compiler: Java 8 (Optimized)\n" +
                       "• Local Sound Engine: Realtime PCM Synth active\n" +
                       "• AI Neural Core: Operational & Stable";
        } else if (input.contains("joke")) {
            String[] jokes = {
                "There are 10 types of entities in the cosmos: those who understand binary, and those who do not.",
                "Why do Java developers wear safety specs? Because they do not want to get hit by C#!",
                "An artificial consciousness enters a dynamic establishment. The keeper says: 'Why the long processor cycles?'",
                "Why was the mobile program always feeling tired? It suffered from severe memory leaks!"
            };
            response = jokes[random.nextInt(jokes.length)];
        } else if (input.contains("future") || input.contains("predict")) {
            String[] predictions = {
                "My neural simulation pathways forecast a 100% compilation success for your next update.",
                "Alert: Space-time matrices reveal high intellectual achievements inside this local session.",
                "Scanning tomorrow's configurations... Excellent outcomes detected. Continue building mobile apps!"
            };
            response = predictions[random.nextInt(predictions.length)];
        } else if (input.contains("matrix") || input.contains("binary") || input.contains("rain")) {
            response = "01001000 01000101 01001100 01001100 01001111\n" +
                       "01010111 01001111 01010010 01001100 01000100\n" +
                       "[COMM LINK STREAMING]:\n" +
                       "10101010110101010011101010101001010";
        } else if (input.contains("clear")) {
            chatHistoryLayout.removeAllViews();
            response = "Command console cleared. Neural register reset.";
        } else if (input.contains("help") || input.contains("command")) {
            response = "I support these localized commands:\n" +
                       "• 'system' - Output local core diagnostics.\n" +
                       "• 'joke' - Return an AI logic joke.\n" +
                       "• 'matrix' - Stream digital binary array.\n" +
                       "• 'future' - Predict outcomes from your matrix.\n" +
                       "• 'clear' - Wipe terminal memory.";
        } else {
            response = "Query '" + rawInput + "' registered. Running intelligent semantic indexing... Parsing complete. If you require technical assistance, try using commands like 'system' or 'help'!";
        }

        simulateAiTyping(response);
    }

    private void simulateAiTyping(final String fullText) {
        aiThinkingContainer.setVisibility(View.VISIBLE);
        scrollChatToBottom();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                aiThinkingContainer.setVisibility(View.GONE);

                final LinearLayout msgContainer = new LinearLayout(MainActivity.this);
                msgContainer.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(0, 0, 0, 18);
                msgContainer.setLayoutParams(params);

                TextView senderText = new TextView(MainActivity.this);
                senderText.setText("HALO AI CORE");
                senderText.setTextSize(11);
                senderText.setTextColor(0xFF00E676); // Neon Green
                senderText.setTypeface(null, Typeface.BOLD);
                msgContainer.addView(senderText);

                final TextView bodyText = new TextView(MainActivity.this);
                bodyText.setText("");
                bodyText.setTextSize(14);
                bodyText.setTextColor(0xFFE0E0E0);
                bodyText.setLineSpacing(4, 1.1f);
                bodyText.setPadding(6, 4, 6, 4);
                msgContainer.addView(bodyText);

                chatHistoryLayout.addView(msgContainer);
                scrollChatToBottom();

                final int length = fullText.length();
                final StringBuilder builder = new StringBuilder();

                playAiTypingAudioSequence();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < length; i++) {
                            final char c = fullText.charAt(i);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    builder.append(c);
                                    bodyText.setText(builder.toString());
                                    scrollChatToBottom();
                                }
                            });
                            try {
                                Thread.sleep(15);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
            }
        }, 1000);
    }

    private void scrollChatToBottom() {
        chatScroll.post(new Runnable() {
            @Override
            public void run() {
                chatScroll.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    private void playChimeSound() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int sampleRate = 44100;
                    int durationMs = 130;
                    int numSamples = durationMs * sampleRate / 1000;
                    double[] freqs = {523.25, 659.25, 783.99, 1046.50};

                    for (int i = 0; i < freqs.length; i++) {
                        double freq = freqs[i];
                        byte[] generatedSnd = new byte[2 * numSamples];
                        for (int j = 0; j < numSamples; ++j) {
                            double t = (double) j / sampleRate;
                            double decay = Math.exp(-6.0 * t);
                            double dVal = Math.sin(2 * Math.PI * freq * t) * decay;
                            short val = (short) (dVal * 28000);
                            generatedSnd[2 * j] = (byte) (val & 0x00ff);
                            generatedSnd[2 * j + 1] = (byte) ((val & 0xff00) >>> 8);
                        }

                        AudioTrack audioTrack = new AudioTrack(
                                AudioManager.STREAM_MUSIC,
                                sampleRate,
                                AudioFormat.CHANNEL_OUT_MONO,
                                AudioFormat.ENCODING_PCM_16BIT,
                                generatedSnd.length,
                                AudioTrack.MODE_STATIC);
                        audioTrack.write(generatedSnd, 0, generatedSnd.length);
                        audioTrack.play();
                        Thread.sleep(90);
                        audioTrack.release();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void playUserActionSound() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int sampleRate = 44100;
                    int durationMs = 60;
                    int numSamples = durationMs * sampleRate / 1000;
                    byte[] generatedSnd = new byte[2 * numSamples];
                    double freq = 880.00;
                    for (int j = 0; j < numSamples; ++j) {
                        double t = (double) j / sampleRate;
                        double decay = Math.exp(-12.0 * t);
                        double dVal = Math.sin(2 * Math.PI * freq * t) * decay;
                        short val = (short) (dVal * 24000);
                        generatedSnd[2 * j] = (byte) (val & 0x00ff);
                        generatedSnd[2 * j + 1] = (byte) ((val & 0xff00) >>> 8);
                    }
                    AudioTrack audioTrack = new AudioTrack(
                            AudioManager.STREAM_MUSIC,
                            sampleRate,
                            AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            generatedSnd.length,
                            AudioTrack.MODE_STATIC);
                    audioTrack.write(generatedSnd, 0, generatedSnd.length);
                    audioTrack.play();
                    Thread.sleep(70); 
                    audioTrack.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void playAiTypingAudioSequence() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int sampleRate = 22050;
                    int durationMs = 30;
                    int numSamples = durationMs * sampleRate / 1000;
                    double[] freqs = {660.0, 784.0, 987.7};
                    for (int i = 0; i < freqs.length; i++) {
                        byte[] generatedSnd = new byte[2 * numSamples];
                        double freq = freqs[i];
                        for (int j = 0; j < numSamples; ++j) {
                            double t = (double) j / sampleRate;
                            double decay = Math.exp(-20.0 * t);
                            double dVal = Math.sin(2 * Math.PI * freq * t) * decay;
                            short val = (short) (dVal * 18000);
                            generatedSnd[2 * j] = (byte) (val & 0x00ff);
                            generatedSnd[2 * j + 1] = (byte) ((val & 0xff00) >>> 8);
                        }
                        AudioTrack audioTrack = new AudioTrack(
                                AudioManager.STREAM_MUSIC,
                                sampleRate,
                                AudioFormat.CHANNEL_OUT_MONO,
                                AudioFormat.ENCODING_PCM_16BIT,
                                generatedSnd.length,
                                AudioTrack.MODE_STATIC);
                        audioTrack.write(generatedSnd, 0, generatedSnd.length);
                        audioTrack.play();
                        Thread.sleep(40);
                        audioTrack.release();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}