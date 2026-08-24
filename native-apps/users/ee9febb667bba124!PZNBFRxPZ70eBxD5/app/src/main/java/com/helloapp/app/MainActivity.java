package com.helloapp.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.media.AudioTrack;
import android.media.AudioManager;
import android.media.AudioFormat;

public class MainActivity extends Activity {

    private LinearLayout loaderContainer;
    private LinearLayout mainContentContainer;
    private TextView helloText;
    private Button btnReplay;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) { 
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loaderContainer = (LinearLayout) findViewById(R.id.loader_container);
        mainContentContainer = (LinearLayout) findViewById(R.id.main_content_container);
        helloText = (TextView) findViewById(R.id.hello_text);
        btnReplay = (Button) findViewById(R.id.btn_replay);
        handler = new Handler();

        // Simulate application startup initialization workflow
        startLoadingSimulation();

        btnReplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerResetAndAnimate();
            }
        });
    }

    private void startLoadingSimulation() {
        // Reset screen state values to start loaders safely
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
        // Smoothly fade out current loading container widget
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

        // Perform gorgeous spring transition expansion with bounce kinetics
        mainContentContainer.animate()
                .alpha(1.0f)
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(800)
                .setInterpolator(new OvershootInterpolator(1.3f))
                .start();

        // Double scale pulse sequence animation
        animateHelloTextPulse();

        // Sound synthesized chime chord melody playing asynchronously
        playChimeSound();
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
        // Fade away current content container beautifully
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

    private void playChimeSound() {
        // Execute synth synthesis process inside background thread to keep execution responsive
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int sampleRate = 44100;
                    int durationMs = 130;
                    int numSamples = durationMs * sampleRate / 1000;
                    double[] freqs = {523.25, 659.25, 783.99, 1046.50}; // Classic musical progression chord sequence

                    for (int i = 0; i < freqs.length; i++) {
                        double freq = freqs[i];
                        byte[] generatedSnd = new byte[2 * numSamples];
                        for (int j = 0; j < numSamples; ++j) {
                            double t = (double) j / sampleRate;
                            // Envelope dynamic volume reduction to prevent clipping artifacts
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
}