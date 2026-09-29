package com.qwenvoice.app;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {

    private static final String TAG = "QwenVoice";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int MAX_CONVERSATION_HISTORY = 6; // Keep last 3 user + 3 assistant messages

    // UI Elements
    private TextView statusText;
    private ImageView microphoneIcon;
    private Button startButton;
    private Button stopButton;
    private Button settingsButton;
    private TextView connectionStatus;

    // Speech Recognition
    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;
    private boolean isListening = false;
    private boolean autoListenEnabled = true;

    // Text to Speech
    private TextToSpeech textToSpeech;
    private boolean isSpeaking = false;
    private String selectedVoiceLanguageTag;
    private float speechRate = 1.0f;
    private float speechPitch = 1.0f;

    // Qwen AI Communication
    private QwenApiClient qwenApiClient;
    private String qwenServerUrl = "http://10.0.2.2:4444";
    private List<Map<String, String>> conversationHistory;
    private boolean isThinking = false;
    private int maxTokens = 200;

    // State management
    private enum AppState {
        IDLE, LISTENING, THINKING, SPEAKING
    }
    private AppState currentAppState = AppState.IDLE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind UI elements
        statusText = (TextView) findViewById(R.id.statusText);
        microphoneIcon = (ImageView) findViewById(R.id.microphoneIcon);
        startButton = (Button) findViewById(R.id.startButton);
        stopButton = (Button) findViewById(R.id.stopButton);
        settingsButton = (Button) findViewById(R.id.settingsButton);
        connectionStatus = (TextView) findViewById(R.id.connectionStatus);

        conversationHistory = new ArrayList<Map<String, String>>();
        qwenApiClient = new QwenApiClient();

        // Load settings and update UI
        loadSettings();
        applySettings();
        updateConnectionStatus(false); // Initial status unknown

        // Initialize TextToSpeech
        textToSpeech = new TextToSpeech(this, this);

        // Setup SpeechRecognizer
        setupSpeechRecognizer();

        // Set up button listeners
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentAppState == AppState.IDLE) {
                    startConversation();
                } else {
                    Toast.makeText(MainActivity.this, "Already active. Tap STOP first.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopConversation();
            }
        });

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });

        // Request audio permission if not granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
            } else {
                // Permission already granted, can start connection test and auto-listen if enabled
                testQwenConnection();
                if (autoListenEnabled) {
                    startConversation();
                }
            }
        } else {
            // Permissions granted at install time for older Android versions
            testQwenConnection();
            if (autoListenEnabled) {
                startConversation();
            }
        }

        updateUI(AppState.IDLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload settings in case they were changed in SettingsActivity
        loadSettings();
        applySettings();
        if (currentAppState == AppState.IDLE) { // Only test if not already in an active state
            testQwenConnection();
        }
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences("QwenVoiceSettings", Context.MODE_PRIVATE);
        qwenServerUrl = prefs.getString("qwenServerUrl", "http://10.0.2.2:4444");
        autoListenEnabled = prefs.getBoolean("autoListenEnabled", true);
        selectedVoiceLanguageTag = prefs.getString("selectedVoiceLanguageTag", null);
        speechRate = prefs.getFloat("speechRate", 1.0f);
        speechPitch = prefs.getFloat("speechPitch", 1.0f);
        maxTokens = prefs.getInt("maxTokens", 200);

        // Clear conversation history if requested by settings, or on new app session
        boolean clearOnStart = prefs.getBoolean("clearConversationOnNextStart", false);
        if (clearOnStart) {
            conversationHistory.clear();
            prefs.edit().putBoolean("clearConversationOnNextStart", false).apply();
            Toast.makeText(this, "Conversation history cleared.", Toast.LENGTH_SHORT).show();
        }
    }

    private void applySettings() {
        if (textToSpeech != null) {
            textToSpeech.setSpeechRate(speechRate);
            textToSpeech.setPitch(speechPitch);
            if (selectedVoiceLanguageTag != null) {
                try {
                    Locale locale = Locale.forLanguageTag(selectedVoiceLanguageTag);
                    int result = textToSpeech.setLanguage(locale);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e(TAG, "Selected language not supported: " + selectedVoiceLanguageTag);
                    }
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Invalid language tag: " + selectedVoiceLanguageTag, e);
                }
            }
        }
        // Qwen server URL is used by QwenApiClient
    }

    private void testQwenConnection() {
        qwenApiClient.testConnection(qwenServerUrl, new QwenApiClient.QwenApiResponseListener() {
            @Override
            public void onResponse(String response) {
                // Not used for connection test
            }

            @Override
            public void onError(String error) {
                updateConnectionStatus(false);
                Log.e(TAG, "Qwen connection test error: " + error);
            }

            @Override
            public void onConnectionTestResult(final boolean success) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateConnectionStatus(success);
                    }
                });
            }
        });
    }

    private void updateConnectionStatus(boolean connected) {
        if (connected) {
            connectionStatus.setText("Qwen Connected");
            connectionStatus.setTextColor(0xFF00C853); // Green color
        } else {
            connectionStatus.setText("Qwen Offline");
            connectionStatus.setTextColor(0xFFFF1744); // Red color
        }
    }


    private void setupSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getPackageName());
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false); // Get final results only
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    Log.d(TAG, "onReadyForSpeech");
                    updateUI(AppState.LISTENING);
                }

                @Override
                public void onBeginningOfSpeech() {
                    Log.d(TAG, "onBeginningOfSpeech");
                }

                @Override
                public void onRmsChanged(float rmsdB) {
                    // Visual feedback could be implemented here
                }

                @Override
                public void onBufferReceived(byte[] buffer) {
                    // Not typically used for continuous speech
                }

                @Override
                public void onEndOfSpeech() {
                    Log.d(TAG, "onEndOfSpeech");
                    // Recognition is ongoing, waiting for results
                    // Do not change UI state until results are processed
                }

                @Override
                public void onError(int error) {
                    isListening = false;
                    String errorMessage = getErrorText(error);
                    Log.e(TAG, "SpeechRecognizer error: " + errorMessage);
                    if (currentAppState != AppState.IDLE) { // If not explicitly stopped by user
                        Toast.makeText(MainActivity.this, "Speech error: " + errorMessage + ". Please try again.", Toast.LENGTH_SHORT).show();
                        if (autoListenEnabled) {
                            // Automatically try listening again if it's not a severe error
                            if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                                startListening(); // Retry listening
                            } else {
                                updateUI(AppState.IDLE); // Go back to idle for other errors
                            }
                        } else {
                            updateUI(AppState.IDLE);
                        }
                    }
                }

                @Override
                public void onResults(Bundle results) {
                    isListening = false;
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && matches.size() > 0) {
                        String recognizedText = matches.get(0);
                        Log.d(TAG, "onResults: " + recognizedText);
                        if (!recognizedText.trim().isEmpty()) {
                            sendToQwen(recognizedText);
                        } else {
                            Log.d(TAG, "Empty speech recognized, restarting listener.");
                            if (autoListenEnabled && currentAppState != AppState.IDLE) {
                                startListening(); // Immediately start listening again for empty input
                            } else {
                                updateUI(AppState.IDLE);
                            }
                        }
                    } else {
                        Log.d(TAG, "No speech results, restarting listener.");
                        if (autoListenEnabled && currentAppState != AppState.IDLE) {
                            startListening(); // Immediately start listening again for no results
                        } else {
                            updateUI(AppState.IDLE);
                        }
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                    // Not used
                }

                @Override
                public void onEvent(int eventType, Bundle params) {
                    // Not used
                }
            });
        } else {
            Toast.makeText(this, "Speech recognition not available on this device.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Speech recognition not available.");
            startButton.setEnabled(false);
        }
    }

    private void updateUI(final AppState newState) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                currentAppState = newState;
                switch (newState) {
                    case IDLE:
                        statusText.setText("Tap START to begin");
                        startButton.setEnabled(true);
                        stopButton.setEnabled(false);
                        microphoneIcon.setAlpha(0.5f); // Dim mic
                        break;
                    case LISTENING:
                        statusText.setText("Listening...");
                        startButton.setEnabled(false);
                        stopButton.setEnabled(true);
                        microphoneIcon.setAlpha(1.0f); // Bright mic
                        break;
                    case THINKING:
                        statusText.setText("Thinking...");
                        startButton.setEnabled(false);
                        stopButton.setEnabled(true);
                        microphoneIcon.setAlpha(0.7f); // Slightly dim
                        break;
                    case SPEAKING:
                        statusText.setText("Speaking...");
                        startButton.setEnabled(false);
                        stopButton.setEnabled(true);
                        microphoneIcon.setAlpha(0.8f); // Slightly dim
                        break;
                }
            }
        });
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
                message = "No speech match";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "Recognizer busy";
                break;
            case SpeechRecognizer.ERROR_SERVER:
                message = "Server error";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "No speech input";
                break;
            default:
                message = "Unknown speech error";
                break;
        }
        return message;
    }

    private void startConversation() {
        if (currentAppState == AppState.IDLE || currentAppState == AppState.LISTENING) {
            startListening();
        } else {
            Log.w(TAG, "Attempted to start conversation while in active state: " + currentAppState);
        }
    }

    private void stopConversation() {
        if (isListening && speechRecognizer != null) {
            speechRecognizer.stopListening();
            isListening = false;
        }
        if (isSpeaking && textToSpeech != null) {
            textToSpeech.stop();
            isSpeaking = false;
        }
        isThinking = false;
        updateUI(AppState.IDLE);
        Log.d(TAG, "Conversation stopped.");
    }

    private void startListening() {
        if (!isListening && !isSpeaking && !isThinking) {
            if (speechRecognizer != null) {
                speechRecognizer.startListening(speechRecognizerIntent);
                isListening = true;
            } else {
                Toast.makeText(this, "SpeechRecognizer not initialized.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.d(TAG, "Cannot start listening. Already listening, speaking, or thinking. State: " + currentAppState);
        }
    }

    private void stopListening() {
        if (isListening && speechRecognizer != null) {
            speechRecognizer.stopListening();
            isListening = false;
            updateUI(AppState.IDLE); // Transition to idle or next appropriate state
        }
    }

    private void sendToQwen(final String userSpeech) {
        if (isThinking) {
            Log.w(TAG, "Already thinking, ignoring new request.");
            return;
        }
        isThinking = true;
        updateUI(AppState.THINKING);
        stopListening(); // Ensure microphone is off while sending/receiving

        // Add user message to history
        Map<String, String> userMessage = new HashMap<String, String>();
        userMessage.put("role", "user");
        userMessage.put("content", userSpeech);
        conversationHistory.add(userMessage);
        trimConversationHistory();

        qwenApiClient.sendMessage(qwenServerUrl, conversationHistory, maxTokens, new QwenApiClient.QwenApiResponseListener() {
            @Override
            public void onResponse(String response) {
                isThinking = false;
                Log.d(TAG, "Qwen response: " + response);
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    JSONArray choices = jsonResponse.optJSONArray("choices");
                    if (choices != null && choices.length() > 0) {
                        JSONObject firstChoice = choices.optJSONObject(0);
                        if (firstChoice != null) {
                            JSONObject message = firstChoice.optJSONObject("message");
                            if (message != null) {
                                String assistantResponse = message.optString("content");
                                if (assistantResponse != null && !assistantResponse.trim().isEmpty()) {
                                    Log.d(TAG, "Assistant: " + assistantResponse);
                                    // Add assistant message to history
                                    Map<String, String> assistantMessage = new HashMap<String, String>();
                                    assistantMessage.put("role", "assistant");
                                    assistantMessage.put("content", assistantResponse);
                                    conversationHistory.add(assistantMessage);
                                    trimConversationHistory();
                                    speak(assistantResponse);
                                } else {
                                    Log.w(TAG, "Qwen response content is empty.");
                                    Toast.makeText(MainActivity.this, "Qwen provided an empty response.", Toast.LENGTH_SHORT).show();
                                    if (autoListenEnabled) {
                                        startListening();
                                    } else {
                                        updateUI(AppState.IDLE);
                                    }
                                }
                            } else {
                                Log.e(TAG, "Qwen response 'message' object is null.");
                                Toast.makeText(MainActivity.this, "Error parsing Qwen response.", Toast.LENGTH_SHORT).show();
                                if (autoListenEnabled) {
                                    startListening();
                                } else {
                                    updateUI(AppState.IDLE);
                                }
                            }
                        } else {
                            Log.e(TAG, "Qwen response 'choices[0]' is null.");
                            Toast.makeText(MainActivity.this, "Error parsing Qwen response.", Toast.LENGTH_SHORT).show();
                            if (autoListenEnabled) {
                                startListening();
                            } else {
                                updateUI(AppState.IDLE);
                            }
                        }
                    } else {
                        Log.e(TAG, "Qwen response 'choices' array is empty or null.");
                        Toast.makeText(MainActivity.this, "Error parsing Qwen response.", Toast.LENGTH_SHORT).show();
                        if (autoListenEnabled) {
                            startListening();
                        } else {
                            updateUI(AppState.IDLE);
                        }
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Failed to parse Qwen JSON response: " + e.getMessage(), e);
                    Toast.makeText(MainActivity.this, "Error parsing Qwen response.", Toast.LENGTH_SHORT).show();
                    if (autoListenEnabled) {
                        startListening();
                    } else {
                        updateUI(AppState.IDLE);
                    }
                }
            }

            @Override
            public void onError(String error) {
                isThinking = false;
                Log.e(TAG, "Qwen AI error: " + error);
                Toast.makeText(MainActivity.this, "Qwen server is offline or error: " + error, Toast.LENGTH_LONG).show();
                // Ensure connection status is updated
                testQwenConnection();
                if (autoListenEnabled) {
                    startListening(); // Try listening again despite Qwen error
                } else {
                    updateUI(AppState.IDLE);
                }
            }

            @Override
            public void onConnectionTestResult(boolean success) {
                // Not used in sendMessage context
            }
        });
    }

    private void trimConversationHistory() {
        while (conversationHistory.size() > MAX_CONVERSATION_HISTORY) {
            conversationHistory.remove(0); // Remove oldest message
        }
    }

    private void speak(String text) {
        if (textToSpeech != null && !isSpeaking) {
            isSpeaking = true;
            updateUI(AppState.SPEAKING);
            stopListening(); // Ensure speech recognizer is off

            Bundle params = new Bundle();
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "qwen_utterance_id");

            // Use TextToSpeech.speak with params for utterance ID
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params, "qwen_utterance_id");
        } else {
            Log.w(TAG, "TextToSpeech not ready or already speaking.");
            Toast.makeText(this, "Voice output unavailable.", Toast.LENGTH_SHORT).show();
            if (autoListenEnabled && currentAppState != AppState.IDLE) {
                startListening();
            } else {
                updateUI(AppState.IDLE);
            }
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            applySettings(); // Apply language, rate, pitch after TTS is initialized

            textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    Log.d(TAG, "TTS onStart: " + utteranceId);
                }

                @Override
                public void onDone(String utteranceId) {
                    Log.d(TAG, "TTS onDone: " + utteranceId);
                    isSpeaking = false;
                    // Automatically restart listening if auto-listen is enabled
                    if (autoListenEnabled && currentAppState != AppState.IDLE) {
                        startListening();
                    } else {
                        updateUI(AppState.IDLE);
                    }
                }

                @Override
                public void onError(String utteranceId) {
                    Log.e(TAG, "TTS onError: " + utteranceId);
                    isSpeaking = false;
                    Toast.makeText(MainActivity.this, "Voice output error.", Toast.LENGTH_SHORT).show();
                    if (autoListenEnabled && currentAppState != AppState.IDLE) {
                        startListening();
                    } else {
                        updateUI(AppState.IDLE);
                    }
                }
                // Deprecated methods for older APIs, required for Java 8 compatibility
                @Override
                public void onError(String utteranceId, int errorCode) {
                    Log.e(TAG, "TTS onError: " + utteranceId + ", code: " + errorCode);
                    isSpeaking = false;
                    Toast.makeText(MainActivity.this, "Voice output error.", Toast.LENGTH_SHORT).show();
                    if (autoListenEnabled && currentAppState != AppState.IDLE) {
                        startListening();
                    } else {
                        updateUI(AppState.IDLE);
                    }
                }
            });
            Log.d(TAG, "TextToSpeech initialized successfully.");
        } else {
            Log.e(TAG, "TextToSpeech initialization failed with status: " + status);
            Toast.makeText(this, "Text-to-speech engine not available.", Toast.LENGTH_LONG).show();
            speak("Text to speech engine not available.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            boolean audioPermissionGranted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (audioPermissionGranted) {
                Log.d(TAG, "RECORD_AUDIO permission granted.");
                testQwenConnection(); // Test connection after permission
                if (autoListenEnabled) {
                    startConversation(); // Start conversation if auto-listen is enabled
                }
            } else {
                Log.w(TAG, "RECORD_AUDIO permission denied.");
                Toast.makeText(this, "Microphone permission is required for voice interaction.", Toast.LENGTH_LONG).show();
                updateUI(AppState.IDLE);
            }
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
        Log.d(TAG, "MainActivity destroyed.");
    }
}