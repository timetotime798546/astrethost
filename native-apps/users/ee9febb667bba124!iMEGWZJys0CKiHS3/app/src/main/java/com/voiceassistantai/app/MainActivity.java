package com.voiceassistantai.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final int PERMISSION_RECORD_AUDIO = 101;
    private static final String PREFS_NAME = "GeminiVoicePrefs";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_ROLE = "role";
    private static final String KEY_MODEL = "model_key";
    private static final String KEY_CUSTOM_MODEL = "custom_model_key";
    private static final String KEY_USE_CUSTOM = "use_custom_bool";

    private SharedPreferences sharedPrefs;
    private ScrollView chatScrollView;
    private LinearLayout chatContainer;
    private TextView tvStatus;
    private WaveformView waveformView;
    private ImageButton btnMic;
    private ImageView btnSettings;

    private SpeechRecognizer speechRecognizer;
    private Intent speechIntent;
    private TextToSpeech textToSpeech;

    private boolean isListening = false;
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Keep screen on for smooth real-time assistant flow
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        chatScrollView = findViewById(R.id.chat_scroll_view);
        chatContainer = findViewById(R.id.chat_container);
        tvStatus = findViewById(R.id.tv_status);
        waveformView = findViewById(R.id.waveform_view);
        btnMic = findViewById(R.id.btn_mic);
        btnSettings = findViewById(R.id.btn_settings);

        initializeSpeechRecognizer();
        initializeTextToSpeech();

        btnMic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleMicButtonClick();
            }
        });

        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettingsDialog();
            }
        });

        // Welcome instruction bubble
        addBubbleMessage("Hello! I am your real-time voice assistant. Tap the microphone icon and start speaking.", false);
        
        // Auto-check API key status on start
        if (getApiKey().isEmpty()) {
            tvStatus.setText("Setup required: Enter your Gemini API key in settings!");
            showSettingsDialog();
        }
    }

    private void initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            speechIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    isListening = true;
                    tvStatus.setText("Listening... speak now");
                    waveformView.setVisibility(View.VISIBLE);
                    waveformView.setAmplitude(1.0f);
                    btnMic.setBackgroundResource(R.drawable.bg_mic_listening);
                    btnMic.setImageResource(R.drawable.ic_stop);
                }

                @Override
                public void onBeginningOfSpeech() {
                    tvStatus.setText("Hearing speech...");
                }

                @Override
                public void onRmsChanged(float rmsdB) {
                    waveformView.setAmplitude(rmsdB);
                }

                @Override
                public void onBufferReceived(byte[] buffer) {}

                @Override
                public void onEndOfSpeech() {
                    tvStatus.setText("Processing sound...");
                }

                @Override
                public void onError(int error) {
                    stopVoiceListeningUI();
                    String message;
                    switch (error) {
                        case SpeechRecognizer.ERROR_AUDIO:
                            message = "Audio recording error.";
                            break;
                        case SpeechRecognizer.ERROR_CLIENT:
                            message = "Client side speech process error.";
                            break;
                        case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                            message = "Microphone permission required.";
                            break;
                        case SpeechRecognizer.ERROR_NETWORK:
                        case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                            message = "Network transmission error.";
                            break;
                        case SpeechRecognizer.ERROR_NO_MATCH:
                            message = "No speech match caught. Try again.";
                            break;
                        case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                            message = "Speech engine is busy.";
                            break;
                        case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                            message = "No input detected. Speaking timeout.";
                            break;
                        default:
                            message = "Speech recognition failed.";
                            break;
                    }
                    tvStatus.setText(message);
                }

                @Override
                public void onResults(Bundle results) {
                    stopVoiceListeningUI();
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String queryText = matches.get(0);
                        addBubbleMessage(queryText, true);
                        queryGeminiAPI(queryText);
                    } else {
                        tvStatus.setText("Could not catch queries, try again.");
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                    ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        tvStatus.setText(matches.get(0));
                    }
                }

                @Override
                public void onEvent(int eventType, Bundle params) {}
            });
        } else {
            Toast.makeText(this, "Speech Recognition is not available on this device.", Toast.LENGTH_LONG).show();
        }
    }

    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech.setLanguage(Locale.US);
                }
            }
        });
    }

    private void handleMicButtonClick() {
        // Cancel TTS playback if speaking
        if (textToSpeech != null && textToSpeech.isSpeaking()) {
            textToSpeech.stop();
            tvStatus.setText("Playback stopped.");
            return;
        }

        if (isListening) {
            if (speechRecognizer != null) {
                speechRecognizer.stopListening();
            }
            stopVoiceListeningUI();
        } else {
            checkAndStartRecognition();
        }
    }

    private void checkAndStartRecognition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_RECORD_AUDIO);
                return;
            }
        }
        startListening();
    }

    private void startListening() {
        if (speechRecognizer != null) {
            try {
                speechRecognizer.startListening(speechIntent);
            } catch (Exception e) {
                tvStatus.setText("Error starting speech recorder.");
            }
        }
    }

    private void stopVoiceListeningUI() {
        isListening = false;
        waveformView.setVisibility(View.INVISIBLE);
        btnMic.setBackgroundResource(R.drawable.bg_mic_idle);
        btnMic.setImageResource(R.drawable.ic_mic);
    }

    private void speakResponse(String phrase) {
        if (textToSpeech != null) {
            tvStatus.setText("Speaking response...");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                textToSpeech.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, "ResponseID");
            } else {
                textToSpeech.speak(phrase, TextToSpeech.QUEUE_FLUSH, null);
            }
        }
    }

    private void queryGeminiAPI(final String userText) {
        final String apiKey = getApiKey();
        if (apiKey.isEmpty()) {
            addBubbleMessage("Setup required: Please open settings from the upper right, insert a valid Gemini API Key, and try again!", false);
            tvStatus.setText("Missing Gemini API Key.");
            showSettingsDialog();
            return;
        }

        tvStatus.setText("Thinking...");
        final String activeModel = getSelectedModel();
        final String systemInstruction = sharedPrefs.getString(KEY_ROLE, "You are a helpful, extremely brief conversational voice assistant. Reply with concise, friendly responses suitable for read aloud.");

        networkExecutor.execute(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                BufferedReader reader = null;
                try {
                    String apiEndpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + activeModel + ":generateContent?key=" + apiKey;
                    URL url = new URL(apiEndpoint);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setDoOutput(true);
                    connection.setConnectTimeout(15000);
                    connection.setReadTimeout(15000);

                    // Prepare body json with system instructions
                    JSONObject body = new JSONObject();

                    // System instructions structure
                    JSONObject systemInstructionObj = new JSONObject();
                    JSONArray systemPartsArray = new JSONArray();
                    JSONObject systemTextObj = new JSONObject();
                    systemTextObj.put("text", systemInstruction);
                    systemPartsArray.put(systemTextObj);
                    systemInstructionObj.put("parts", systemPartsArray);
                    body.put("systemInstruction", systemInstructionObj);

                    // Content text structures
                    JSONArray contentsArray = new JSONArray();
                    JSONObject userContentObj = new JSONObject();
                    JSONArray userPartsArray = new JSONArray();
                    JSONObject userTextObj = new JSONObject();
                    userTextObj.put("text", userText);
                    userPartsArray.put(userTextObj);
                    userContentObj.put("parts", userPartsArray);
                    contentsArray.put(userContentObj);
                    body.put("contents", contentsArray);

                    // Write network payloads
                    OutputStream os = connection.getOutputStream();
                    os.write(body.toString().getBytes("UTF-8"));
                    os.close();

                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder builder = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            builder.append(line);
                        }

                        final String resultText = parseGeminiResponse(builder.toString());
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                addBubbleMessage(resultText, false);
                                speakResponse(resultText);
                            }
                        });

                    } else {
                        // Capture error responses from API
                        reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                        StringBuilder builder = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            builder.append(line);
                        }
                        final String rawError = builder.toString();
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                tvStatus.setText("Gemini API returned error code: " + responseCode);
                                addBubbleMessage("API Connection Error: " + rawError, false);
                            }
                        });
                    }

                } catch (final Exception e) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            tvStatus.setText("Unable to reach server.");
                            addBubbleMessage("Connection error occurred: " + e.getMessage(), false);
                        }
                    });
                } finally {
                    try {
                        if (reader != null) reader.close();
                        if (connection != null) connection.disconnect();
                    } catch (Exception ignored) {}
                }
            }
        });
    }

    private String parseGeminiResponse(String rawJson) {
        try {
            JSONObject root = new JSONObject(rawJson);
            JSONArray candidates = root.getJSONArray("candidates");
            if (candidates.length() > 0) {
                JSONObject candidate = candidates.getJSONObject(0);
                JSONObject content = candidate.getJSONObject("content");
                JSONArray parts = content.getJSONArray("parts");
                if (parts.length() > 0) {
                    return parts.getJSONObject(0).getString("text");
                }
            }
        } catch (Exception e) {
            return "Parser error while evaluating reply content: " + e.getMessage();
        }
        return "No viable responses were resolved from target host.";
    }

    private void addBubbleMessage(String text, boolean isUser) {
        LayoutInflater inflater = getLayoutInflater();
        View bubble = inflater.inflate(
                isUser ? R.layout.chat_item_user : R.layout.chat_item_assistant,
                chatContainer,
                false
        );
        TextView messageText = bubble.findViewById(R.id.chat_message_text);
        messageText.setText(text);
        chatContainer.addView(bubble);

        // Auto scroll helper
        chatScrollView.post(new Runnable() {
            @Override
            public void run() {
                chatScrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_settings, null);
        builder.setView(dialogView);

        final EditText etApiKey = dialogView.findViewById(R.id.et_api_key);
        final EditText etRole = dialogView.findViewById(R.id.et_role);
        final EditText etCustomModel = dialogView.findViewById(R.id.et_custom_model);
        final RadioGroup rgModels = dialogView.findViewById(R.id.rg_models);
        final LinearLayout layoutCustomModel = dialogView.findViewById(R.id.layout_custom_model);
        
        final RadioButton rb25Flash = dialogView.findViewById(R.id.rb_model_25_flash);
        final RadioButton rb15Flash = dialogView.findViewById(R.id.rb_model_15_flash);
        final RadioButton rb15Pro = dialogView.findViewById(R.id.rb_model_15_pro);
        final RadioButton rbCustom = dialogView.findViewById(R.id.rb_model_custom);
        
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel_settings);
        Button btnSave = dialogView.findViewById(R.id.btn_save_settings);

        // Preload values
        etApiKey.setText(getApiKey());
        etRole.setText(sharedPrefs.getString(KEY_ROLE, "You are a helpful, extremely brief conversational voice assistant. Reply with concise, friendly responses suitable for read aloud."));
        etCustomModel.setText(sharedPrefs.getString(KEY_CUSTOM_MODEL, "gemini-2.5-pro-preview"));

        String activeModel = sharedPrefs.getString(KEY_MODEL, "gemini-2.5-flash");
        boolean useCustom = sharedPrefs.getBoolean(KEY_USE_CUSTOM, false);

        if (useCustom) {
            rbCustom.setChecked(true);
            layoutCustomModel.setVisibility(View.VISIBLE);
        } else {
            if ("gemini-1.5-flash".equals(activeModel)) {
                rb15Flash.setChecked(true);
            } else if ("gemini-1.5-pro".equals(activeModel)) {
                rb15Pro.setChecked(true);
            } else {
                rb25Flash.setChecked(true);
            }
        }

        rgModels.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rb_model_custom) {
                    layoutCustomModel.setVisibility(View.VISIBLE);
                } else {
                    layoutCustomModel.setVisibility(View.GONE);
                }
            }
        });

        final AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putString(KEY_API_KEY, etApiKey.getText().toString().trim());
                editor.putString(KEY_ROLE, etRole.getText().toString().trim());

                if (rbCustom.isChecked()) {
                    editor.putBoolean(KEY_USE_CUSTOM, true);
                    editor.putString(KEY_CUSTOM_MODEL, etCustomModel.getText().toString().trim());
                } else {
                    editor.putBoolean(KEY_USE_CUSTOM, false);
                    if (rb15Flash.isChecked()) {
                        editor.putString(KEY_MODEL, "gemini-1.5-flash");
                    } else if (rb15Pro.isChecked()) {
                        editor.putString(KEY_MODEL, "gemini-1.5-pro");
                    } else {
                        editor.putString(KEY_MODEL, "gemini-2.5-flash");
                    }
                }
                editor.apply();

                Toast.makeText(MainActivity.this, "Preferences updated!", Toast.LENGTH_SHORT).show();
                tvStatus.setText("Ready. Tap mic to talk!");
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private String getApiKey() {
        return sharedPrefs.getString(KEY_API_KEY, "");
    }

    private String getSelectedModel() {
        boolean useCustom = sharedPrefs.getBoolean(KEY_USE_CUSTOM, false);
        if (useCustom) {
            return sharedPrefs.getString(KEY_CUSTOM_MODEL, "gemini-2.5-flash");
        }
        return sharedPrefs.getString(KEY_MODEL, "gemini-2.5-flash");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startListening();
            } else {
                Toast.makeText(this, "Permission denied. Audio capabilities require the voice recorder permission.", Toast.LENGTH_LONG).show();
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
        networkExecutor.shutdown();
    }
}