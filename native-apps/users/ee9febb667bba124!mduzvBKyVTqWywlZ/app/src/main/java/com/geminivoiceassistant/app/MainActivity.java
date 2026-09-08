package com.geminivoiceassistant.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.text.Html;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int PERMISSION_REQUEST_AUDIO = 101;
    private static final String PREFS_NAME = "GeminiPrefs";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_MODEL = "model_name";
    private static final String DEFAULT_MODEL = "gemini-1.5-flash";

    private TextView txtAppTitle;
    private TextView txtStatus;
    private TextView txtLog;
    private ScrollView logScroll;
    private Button btnRecord;
    private Button btnSettings;

    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private boolean isListening = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtAppTitle = (TextView) findViewById(R.id.app_title);
        txtStatus = (TextView) findViewById(R.id.txt_status_indicator);
        txtLog = (TextView) findViewById(R.id.txt_conversation_history);
        logScroll = (ScrollView) findViewById(R.id.log_scroll);
        btnRecord = (Button) findViewById(R.id.btn_record_trigger);
        btnSettings = (Button) findViewById(R.id.btn_settings);

        initTextToSpeech();
        initSpeechRecognizer();

        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_AUDIO);
                } else {
                    toggleSpeechInput();
                }
            }
        });

        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettingsDialog();
            }
        });
    }

    private void initTextToSpeech() {
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech.setLanguage(Locale.US);
                } else {
                    appendSystemLog("System Alert: TTS initialization failed.");
                }
            }
        });
    }

    private void initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    updateStatus("Listening closely...");
                }

                @Override
                public void onBeginningOfSpeech() {
                    updateStatus("Recording your voice...");
                }

                @Override
                public void onRmsChanged(float rmsdB) {}

                @Override
                public void onBufferReceived(byte[] buffer) {}

                @Override
                public void onEndOfSpeech() {
                    updateStatus("Processing your command...");
                    isListening = false;
                }

                @Override
                public void onError(int error) {
                    isListening = false;
                    String errorMessage = "Speech error. Tap to retry.";
                    switch (error) {
                        case SpeechRecognizer.ERROR_AUDIO:
                            errorMessage = "Audio recording error.";
                            break;
                        case SpeechRecognizer.ERROR_CLIENT:
                            errorMessage = "Device client error.";
                            break;
                        case SpeechRecognizer.ERROR_NETWORK:
                            errorMessage = "Internet connection error.";
                            break;
                        case SpeechRecognizer.ERROR_NO_MATCH:
                            errorMessage = "Sorry, couldn't hear that clearly.";
                            break;
                        case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                            errorMessage = "Listening timed out.";
                            break;
                    }
                    updateStatus(errorMessage);
                    appendSystemLog("<i>System status: " + errorMessage + "</i>");
                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String userPrompt = matches.get(0);
                        handleVoiceCommand(userPrompt);
                    } else {
                        updateStatus("Ready to listen");
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {}

                @Override
                public void onEvent(int eventType, Bundle params) {}
            });
        } else {
            appendSystemLog("System Alert: Local Voice Recognition Engine is unavailable on this model.");
        }
    }

    private void toggleSpeechInput() {
        if (isListening) {
            speechRecognizer.stopListening();
            isListening = false;
            updateStatus("Ready to listen");
        } else {
            if (textToSpeech != null && textToSpeech.isSpeaking()) {
                textToSpeech.stop();
            }
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            try {
                speechRecognizer.startListening(intent);
                isListening = true;
                updateStatus("Connecting receiver...");
            } catch (Exception e) {
                appendSystemLog("Error starting microphone: " + e.getMessage());
            }
        }
    }

    private void handleVoiceCommand(String query) {
        appendUserMessage(query);

        if (checkAndExecuteOpenApp(query)) {
            return;
        }

        String apiKey = getSavedApiKey();
        if (apiKey.isEmpty()) {
            String textResponse = "Your Gemini API key is missing. Please configuration it in Settings first.";
            appendAssistantResponse(textResponse);
            speakOutput(textResponse);
            showSettingsDialog();
            return;
        }

        executeGeminiQuery(query, apiKey, getSavedModel());
    }

    private boolean checkAndExecuteOpenApp(String command) {
        String normalized = command.trim().toLowerCase();
        if (normalized.startsWith("open ") || normalized.startsWith("launch ")) {
            String targetApp = normalized.substring(normalized.indexOf(" ") + 1).trim();
            if (!targetApp.isEmpty()) {
                return findAndLaunchPackage(targetApp);
            }
        }
        return false;
    }

    private boolean findAndLaunchPackage(String appName) {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        ApplicationInfo matchedApp = null;
        String resolvedLabel = "";

        for (ApplicationInfo appInfo : packages) {
            if (pm.getLaunchIntentForPackage(appInfo.packageName) == null) {
                continue;
            }
            String label = pm.getApplicationLabel(appInfo).toString().toLowerCase();
            if (label.equalsIgnoreCase(appName)) {
                matchedApp = appInfo;
                resolvedLabel = pm.getApplicationLabel(appInfo).toString();
                break;
            } else if (label.contains(appName)) {
                matchedApp = appInfo;
                resolvedLabel = pm.getApplicationLabel(appInfo).toString();
            }
        }

        if (matchedApp != null) {
            Intent launchIntent = pm.getLaunchIntentForPackage(matchedApp.packageName);
            if (launchIntent != null) {
                try {
                    startActivity(launchIntent);
                    String feedback = "Opening " + resolvedLabel;
                    appendAssistantResponse(feedback);
                    speakOutput(feedback);
                    updateStatus("Ready to listen");
                    return true;
                } catch (Exception e) {
                    appendSystemLog("Failed to launch app: " + e.getMessage());
                }
            }
        }

        String failFeedback = "I couldn't locate any installed application named " + appName;
        appendAssistantResponse(failFeedback);
        speakOutput(failFeedback);
        updateStatus("Ready to listen");
        return true;
    }

    private void executeGeminiQuery(final String promptText, final String apiKey, final String modelName) {
        updateStatus("Thinking...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                String responseText;
                try {
                    URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + apiKey);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setDoOutput(true);

                    // Build raw native JSON Payload
                    JSONObject root = new JSONObject();
                    JSONArray contentsArray = new JSONArray();
                    JSONObject contentsObj = new JSONObject();
                    JSONArray partsArray = new JSONArray();
                    JSONObject partsObj = new JSONObject();

                    partsObj.put("text", promptText);
                    partsArray.put(partsObj);
                    contentsObj.put("parts", partsArray);
                    contentsArray.put(contentsObj);
                    root.put("contents", contentsArray);

                    String payload = root.toString();

                    OutputStream outputStream = connection.getOutputStream();
                    outputStream.write(payload.getBytes("UTF-8"));
                    outputStream.close();

                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder builder = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            builder.append(line);
                        }
                        reader.close();

                        // Parse response text from Gemini
                        JSONObject responseJson = new JSONObject(builder.toString());
                        JSONArray candidates = responseJson.getJSONArray("candidates");
                        JSONObject candidateObj = candidates.getJSONObject(0);
                        JSONObject contentObj = candidateObj.getJSONObject("content");
                        JSONArray parts = contentObj.getJSONArray("parts");
                        responseText = parts.getJSONObject(0).getString("text");
                    } else {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                        StringBuilder builder = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            builder.append(line);
                        }
                        reader.close();
                        responseText = "API Error code " + responseCode + ": " + parseErrorMessage(builder.toString());
                    }
                } catch (Exception e) {
                    responseText = "Connection failure: " + e.getMessage();
                }

                final String finalResult = responseText;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        appendAssistantResponse(finalResult);
                        speakOutput(finalResult);
                        updateStatus("Ready to listen");
                    }
                });
            }
        }).start();
    }

    private String parseErrorMessage(String errorJson) {
        try {
            JSONObject obj = new JSONObject(errorJson);
            if (obj.has("error")) {
                return obj.getJSONObject("error").getString("message");
            }
        } catch (Exception e) {
            // Unparseable, return fallback payload string
        }
        return errorJson;
    }

    private void speakOutput(String text) {
        if (textToSpeech != null) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    private void updateStatus(final String status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtStatus.setText(status);
            }
        });
    }

    private void appendUserMessage(String query) {
        String formatted = "<br/><font color='#1565C0'><b>You:</b></font> " + query;
        appendHtmlLog(formatted);
    }

    private void appendAssistantResponse(String response) {
        String formatted = "<br/><br/><font color='#43A047'><b>Gemini:</b></font> " + response.replace("\n", "<br/>");
        appendHtmlLog(formatted);
    }

    private void appendSystemLog(String systemMessage) {
        String formatted = "<br/><font color='#757575'><i>" + systemMessage + "</i></font>";
        appendHtmlLog(formatted);
    }

    private void appendHtmlLog(final String formattedText) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtLog.append(Html.fromHtml(formattedText));
                logScroll.post(new Runnable() {
                    @Override
                    public void run() {
                        logScroll.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        });
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Gemini Engine Settings");

        LinearLayout wrapperLayout = new LinearLayout(this);
        wrapperLayout.setOrientation(LinearLayout.VERTICAL);
        wrapperLayout.setPadding(40, 20, 40, 20);

        TextView labelKey = new TextView(this);
        labelKey.setText("Gemini API Key:");
        labelKey.setTextSize(14);
        wrapperLayout.addView(labelKey);

        final EditText inputKey = new EditText(this);
        inputKey.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        inputKey.setText(getSavedApiKey());
        inputKey.setHint("AIzaSy...");
        wrapperLayout.addView(inputKey);

        TextView labelModel = new TextView(this);
        labelModel.setText("Gemini Target Model:");
        labelModel.setTextSize(14);
        labelModel.setPadding(0, 30, 0, 0);
        wrapperLayout.addView(labelModel);

        final EditText inputModel = new EditText(this);
        inputModel.setInputType(InputType.TYPE_CLASS_TEXT);
        inputModel.setText(getSavedModel());
        inputModel.setHint("gemini-1.5-flash");
        wrapperLayout.addView(inputModel);

        builder.setView(wrapperLayout);

        builder.setPositiveButton("Save Settings", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String keyInput = inputKey.getText().toString().trim();
                String modelInput = inputModel.getText().toString().trim();
                if (modelInput.isEmpty()) {
                    modelInput = DEFAULT_MODEL;
                }
                saveConfigurations(keyInput, modelInput);
                Toast.makeText(MainActivity.this, "Settings Saved!", Toast.LENGTH_SHORT).show();
                appendSystemLog("Settings Updated: Model set to " + modelInput);
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private String getSavedApiKey() {
        SharedPreferences sharedPref = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return sharedPref.getString(KEY_API_KEY, "");
    }

    private String getSavedModel() {
        SharedPreferences sharedPref = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return sharedPref.getString(KEY_MODEL, DEFAULT_MODEL);
    }

    private void saveConfigurations(String apiKey, String modelName) {
        SharedPreferences sharedPref = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(KEY_API_KEY, apiKey);
        editor.putString(KEY_MODEL, modelName);
        editor.apply();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                toggleSpeechInput();
            } else {
                Toast.makeText(this, "Microphone access is required for voice operations", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}