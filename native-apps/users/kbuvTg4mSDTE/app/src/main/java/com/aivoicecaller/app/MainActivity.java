package com.aivoicecaller.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final String TAG = "AI_Voice_Caller";
    private static final String PREFS_NAME = "AIVoiceCallerPrefs";
    private static final String KEY_PHONE_NUMBER = "phoneNumber";
    private static final String KEY_GEMINI_API_KEY = "geminiApiKey";

    private static final int REQUEST_CALL_PHONE_PERMISSION = 1;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 2;
    private static final int REQUEST_SETTINGS = 3;

    private EditText phoneNumberEditText;
    private Button startCallButton;
    private Button endCallButton;
    private Button settingsButton;
    private TextView callStatusTextView;
    private TextView countdownTimerTextView;

    private CountDownTimer callTimer;
    private long timeLeftInMillis = 120000; // 2 minutes (120 seconds)
    private boolean timerRunning = false;
    private boolean aiConversationActive = false;

    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    private String savedGeminiApiKey;
    private String savedPhoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        phoneNumberEditText = (EditText) findViewById(R.id.phoneNumberEditText);
        startCallButton = (Button) findViewById(R.id.startCallButton);
        endCallButton = (Button) findViewById(R.id.endCallButton);
        settingsButton = (Button) findViewById(R.id.settingsButton);
        callStatusTextView = (TextView) findViewById(R.id.callStatusTextView);
        countdownTimerTextView = (TextView) findViewById(R.id.countdownTimerTextView);

        loadSavedData();
        updatePhoneNumberDisplay();
        updateCallStatus("Ready");
        updateCountdownText();

        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = textToSpeech.setLanguage(Locale.ENGLISH); // Default language
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e(TAG, "Language not supported by TextToSpeech");
                    }
                } else {
                    Log.e(TAG, "TextToSpeech Initialization failed");
                }
            }
        });

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new SpeechRecognitionListener());

        startCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initiateAICall();
            }
        });

        endCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endAICall();
            }
        });

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivityForResult(settingsIntent, REQUEST_SETTINGS);
            }
        });
    }

    private void loadSavedData() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        savedPhoneNumber = prefs.getString(KEY_PHONE_NUMBER, "");
        savedGeminiApiKey = prefs.getString(KEY_GEMINI_API_KEY, "");
    }

    private void updatePhoneNumberDisplay() {
        phoneNumberEditText.setText(savedPhoneNumber);
    }

    private void updateCallStatus(String status) {
        callStatusTextView.setText("Status: " + status);
    }

    private void updateCountdownText() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        String timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        countdownTimerTextView.setText("Max Duration: " + timeFormatted);
    }

    private void initiateAICall() {
        savedPhoneNumber = phoneNumberEditText.getText().toString().trim();

        if (savedPhoneNumber.isEmpty() || !PhoneNumberUtils.isGlobalPhoneNumber(savedPhoneNumber)) {
            Toast.makeText(this, "Please enter a valid phone number.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (savedGeminiApiKey.isEmpty()) {
            Toast.makeText(this, "Please save your Gemini API Key in Settings first.", Toast.LENGTH_LONG).show();
            return;
        }

        // Show safety and telephony limitation notice
        new AlertDialog.Builder(this)
                .setTitle("AI Call Notice")
                .setMessage("This call uses an AI assistant. Make sure the person you are calling is aware that they are speaking with an AI.\n\n" +
                        "IMPORTANT: This app initiates a standard phone call. The AI conversation will occur using your device's microphone and speaker, separate from the actual call's audio stream. For direct AI integration into a cellular call's audio, specialized Telecom APIs or VoIP services are required, which are beyond the scope of a standard third-party application.")
                .setPositiveButton("Proceed", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        requestPermissionsAndStartCall();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void requestPermissionsAndStartCall() {
        if (checkSelfPermission(android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.CALL_PHONE}, REQUEST_CALL_PHONE_PERMISSION);
        } else if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
        } else {
            startPhoneCallAndAIConversation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CALL_PHONE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestPermissionsAndStartCall(); // Try to get RECORD_AUDIO next or start call
            } else {
                Toast.makeText(this, "CALL_PHONE permission denied. Cannot make calls.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startPhoneCallAndAIConversation();
            } else {
                Toast.makeText(this, "RECORD_AUDIO permission denied. AI conversation will not work.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startPhoneCallAndAIConversation() {
        updateCallStatus("Calling...");
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + savedPhoneNumber));
        try {
            startActivity(callIntent);
            startCallTimer();
            aiConversationActive = true;
            updateCallStatus("AI Connected (Simulated)");
            speakAIResponse("Hello, this is an AI assistant. How can I help you today?");
            // Optionally start listening for user input immediately
            startSpeechRecognition();
        } catch (SecurityException e) {
            Log.e(TAG, "Permission not granted to make call: " + e.getMessage());
            Toast.makeText(this, "Permission to make phone calls was denied.", Toast.LENGTH_LONG).show();
            updateCallStatus("Error");
            endAICall();
        } catch (Exception e) {
            Log.e(TAG, "Error starting call: " + e.getMessage());
            Toast.makeText(this, "Failed to start call. Ensure the number is valid.", Toast.LENGTH_LONG).show();
            updateCallStatus("Error");
            endAICall();
        }
    }

    private void startCallTimer() {
        callTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateCountdownText();
            }

            @Override
            public void onFinish() {
                timeLeftInMillis = 0;
                updateCountdownText();
                Toast.makeText(MainActivity.this, "Call duration reached its maximum. Ending call.", Toast.LENGTH_LONG).show();
                endAICall();
            }
        }.start();
        timerRunning = true;
    }

    private void endAICall() {
        if (timerRunning) {
            callTimer.cancel();
            timerRunning = false;
        }
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
            speechRecognizer.destroy();
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new SpeechRecognitionListener());
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
        }

        aiConversationActive = false;
        timeLeftInMillis = 120000; // Reset to 2 minutes
        updateCountdownText();
        updateCallStatus("Call Ended");
        Toast.makeText(this, "Call Ended.", Toast.LENGTH_SHORT).show();
    }

    private void startSpeechRecognition() {
        if (!aiConversationActive || checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Cannot start speech recognition: AI conversation not active or RECORD_AUDIO permission not granted.");
            return;
        }

        Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US"); // Default to English, can be dynamic
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());

        speechRecognizer.startListening(recognizerIntent);
        updateCallStatus("Listening...");
    }

    private void speakAIResponse(String text) {
        if (textToSpeech != null && !text.isEmpty()) {
            updateCallStatus("AI Speaking...");
            // Set language based on content or user preference if multi-lingual support is desired
            // For English, Hindi, Hinglish, you'd need to detect or set based on context.
            // For simplicity, sticking to English or default.
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
            textToSpeech.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                }

                @Override
                public void onDone(String utteranceId) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (aiConversationActive) {
                                updateCallStatus("AI Connected (Simulated)");
                                startSpeechRecognition(); // Listen again after AI speaks
                            }
                        }
                    });
                }

                @Override
                public void onError(String utteranceId) {
                    Log.e(TAG, "Error in TTS speaking");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateCallStatus("Error in AI Speech");
                            if (aiConversationActive) {
                                startSpeechRecognition(); // Try listening again despite error
                            }
                        }
                    });
                }
            });
        }
    }

    private void sendTextToGemini(final String userText) {
        if (userText.isEmpty()) {
            return;
        }
        updateCallStatus("Thinking...");
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String aiResponse = GeminiApiClient.getGeminiResponse(savedGeminiApiKey, userText);
                    final String finalAiResponse = aiResponse;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (finalAiResponse != null && !finalAiResponse.isEmpty()) {
                                speakAIResponse(finalAiResponse);
                            } else {
                                speakAIResponse("I am having trouble understanding. Can you please repeat?");
                            }
                        }
                    });
                } catch (final Exception e) {
                    Log.e(TAG, "Error communicating with Gemini API: " + e.getMessage());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateCallStatus("Error: " + e.getMessage());
                            speakAIResponse("I'm sorry, I'm having trouble connecting to the AI. Please try again later.");
                            if (aiConversationActive) {
                                startSpeechRecognition(); // Try listening again
                            }
                        }
                    });
                }
            }
        });
    }

    private class SpeechRecognitionListener implements RecognitionListener {
        @Override
        public void onReadyForSpeech(Bundle params) {
            Log.d(TAG, "onReadyForSpeech");
            // updateCallStatus("Listening...");
        }

        @Override
        public void onBeginningOfSpeech() {
            Log.d(TAG, "onBeginningOfSpeech");
        }

        @Override
        public void onRmsChanged(float rmsdB) {
            // Log.d(TAG, "onRmsChanged: " + rmsdB);
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
            // Log.d(TAG, "onBufferReceived");
        }

        @Override
        public void onEndOfSpeech() {
            Log.d(TAG, "onEndOfSpeech");
            updateCallStatus("Processing input...");
        }

        @Override
        public void onError(int error) {
            String errorMessage = getErrorText(error);
            Log.e(TAG, "Speech recognition error: " + errorMessage);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (aiConversationActive) {
                        updateCallStatus("Speech Error: " + errorMessage);
                        // Only try to restart listening if it's not a permanent error or call isn't ending
                        if (error != SpeechRecognizer.ERROR_CLIENT && error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                            startSpeechRecognition();
                        } else if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                            speakAIResponse("I didn't catch that. Could you please repeat?");
                        }
                    }
                }
            });
        }

        @Override
        public void onResults(Bundle results) {
            Log.d(TAG, "onResults");
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                final String userSpokenText = matches.get(0);
                Log.d(TAG, "User spoke: " + userSpokenText);
                sendTextToGemini(userSpokenText);
            } else {
                Log.d(TAG, "No speech results found.");
                if (aiConversationActive) {
                    speakAIResponse("I didn't quite hear you. Can you say that again?");
                    // startSpeechRecognition(); // This will be called after AI speaks
                }
            }
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            // Log.d(TAG, "onPartialResults");
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
            // Log.d(TAG, "onEvent");
        }
    }

    private String getErrorText(int errorCode) {
        String message;
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "Audio recording error";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                message = "Client side error";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Insufficient permissions";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                message = "Network error";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "Network timeout";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "No match";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "Recognition service busy";
                break;
            case SpeechRecognizer.ERROR_SERVER:
                message = "Error from server";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "No speech input";
                break;
            default:
                message = "Unknown speech recognition error";
                break;
        }
        return message;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SETTINGS && resultCode == RESULT_OK) {
            loadSavedData(); // Reload API key and phone number from SharedPreferences
            updatePhoneNumberDisplay(); // Update if phone number was changed/saved in settings
            Toast.makeText(this, "Settings updated.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        if (executorService != null) {
            executorService.shutdownNow();
        }
        if (callTimer != null) {
            callTimer.cancel();
        }
    }
}