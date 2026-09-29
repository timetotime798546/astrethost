package com.qwenvoice.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SettingsActivity extends Activity implements TextToSpeech.OnInitListener {

    private static final String TAG = "QwenVoiceSettings";

    private EditText qwenServerUrlEditText;
    private Button testConnectionButton;
    private TextView connectionTestResult;
    private Spinner voiceSpinner;
    private SeekBar speechRateSeekBar;
    private TextView speechRateValue;
    private SeekBar voicePitchSeekBar;
    private TextView voicePitchValue;
    private Switch autoListenSwitch;
    private EditText maxTokensEditText;
    private Button clearConversationButton;
    private Button saveSettingsButton;

    private SharedPreferences prefs;
    private TextToSpeech tempTts; // Temporary TTS for voice list
    private List<Map<String, String>> availableVoices; // Map for voice display name and locale tag
    private ArrayAdapter<String> voiceAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences("QwenVoiceSettings", Context.MODE_PRIVATE);

        // Bind UI elements
        qwenServerUrlEditText = (EditText) findViewById(R.id.qwenServerUrlEditText);
        testConnectionButton = (Button) findViewById(R.id.testConnectionButton);
        connectionTestResult = (TextView) findViewById(R.id.connectionTestResult);
        voiceSpinner = (Spinner) findViewById(R.id.voiceSpinner);
        speechRateSeekBar = (SeekBar) findViewById(R.id.speechRateSeekBar);
        speechRateValue = (TextView) findViewById(R.id.speechRateValue);
        voicePitchSeekBar = (SeekBar) findViewById(R.id.voicePitchSeekBar);
        voicePitchValue = (TextView) findViewById(R.id.voicePitchValue);
        autoListenSwitch = (Switch) findViewById(R.id.autoListenSwitch);
        maxTokensEditText = (EditText) findViewById(R.id.maxTokensEditText);
        clearConversationButton = (Button) findViewById(R.id.clearConversationButton);
        saveSettingsButton = (Button) findViewById(R.id.saveSettingsButton);

        // Initialize temporary TTS for voice list population
        tempTts = new TextToSpeech(this, this);

        // Load and display current settings
        loadSettings();
        setupListeners();
    }

    private void loadSettings() {
        qwenServerUrlEditText.setText(prefs.getString("qwenServerUrl", "http://10.0.2.2:4444"));
        speechRateSeekBar.setProgress((int) (prefs.getFloat("speechRate", 1.0f) * 100));
        voicePitchSeekBar.setProgress((int) (prefs.getFloat("voicePitch", 1.0f) * 100));
        autoListenSwitch.setChecked(prefs.getBoolean("autoListenEnabled", true));
        maxTokensEditText.setText(String.valueOf(prefs.getInt("maxTokens", 200)));

        updateSpeechRateValue(speechRateSeekBar.getProgress());
        updateVoicePitchValue(voicePitchSeekBar.getProgress());
    }

    private void setupListeners() {
        testConnectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testQwenConnection();
            }
        });

        speechRateSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateSpeechRateValue(progress);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        voicePitchSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateVoicePitchValue(progress);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        clearConversationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Set a flag to clear conversation history on the next MainActivity start
                prefs.edit().putBoolean("clearConversationOnNextStart", true).apply();
                Toast.makeText(SettingsActivity.this, "Conversation history will be cleared on next app start.", Toast.LENGTH_LONG).show();
            }
        });

        saveSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
            }
        });
    }

    private void updateSpeechRateValue(int progress) {
        float rate = (float) progress / 100.0f;
        speechRateValue.setText(String.format(Locale.getDefault(), "%.1fx", rate));
    }

    private void updateVoicePitchValue(int progress) {
        float pitch = (float) progress / 100.0f;
        voicePitchValue.setText(String.format(Locale.getDefault(), "%.1fx", pitch));
    }

    private void testQwenConnection() {
        final String serverUrl = qwenServerUrlEditText.getText().toString();
        connectionTestResult.setText("Testing connection...");
        connectionTestResult.setTextColor(0xFF000000); // Black

        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                HttpURLConnection urlConnection = null;
                try {
                    URL url = new URL(serverUrl);
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.setConnectTimeout(3000);
                    urlConnection.setReadTimeout(5000);
                    urlConnection.connect();
                    int responseCode = urlConnection.getResponseCode();
                    return responseCode >= 200 && responseCode < 400;
                } catch (Exception e) {
                    Log.e(TAG, "Connection test error: " + e.getMessage());
                    return false;
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                }
            }

            @Override
            protected void onPostExecute(final Boolean success) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (success) {
                            connectionTestResult.setText("Qwen Connected");
                            connectionTestResult.setTextColor(0xFF00C853); // Green
                        } else {
                            connectionTestResult.setText("Unable to connect to Qwen");
                            connectionTestResult.setTextColor(0xFFFF1744); // Red
                        }
                    }
                });
            }
        }.execute();
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("qwenServerUrl", qwenServerUrlEditText.getText().toString());
        editor.putFloat("speechRate", (float) speechRateSeekBar.getProgress() / 100.0f);
        editor.putFloat("voicePitch", (float) voicePitchSeekBar.getProgress() / 100.0f);
        editor.putBoolean("autoListenEnabled", autoListenSwitch.isChecked());

        String maxTokensStr = maxTokensEditText.getText().toString();
        if (!maxTokensStr.isEmpty()) {
            try {
                editor.putInt("maxTokens", Integer.parseInt(maxTokensStr));
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid Max Tokens value. Using default.", Toast.LENGTH_SHORT).show();
                editor.putInt("maxTokens", 200); // Reset to default
            }
        } else {
            editor.putInt("maxTokens", 200); // Set to default if empty
        }


        // Save selected voice locale tag
        if (voiceSpinner.getSelectedItemPosition() >= 0 && availableVoices != null) {
            String selectedVoiceLocaleTag = availableVoices.get(voiceSpinner.getSelectedItemPosition()).get("localeTag");
            editor.putString("selectedVoiceLanguageTag", selectedVoiceLocaleTag);
        } else {
            editor.remove("selectedVoiceLanguageTag"); // Clear if no voice selected
        }

        editor.apply();
        Toast.makeText(this, "Settings saved!", Toast.LENGTH_SHORT).show();
        finish(); // Go back to MainActivity
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            populateVoiceSpinner();
        } else {
            Log.e(TAG, "TTS initialization failed for voice list.");
            Toast.makeText(this, "Text-to-speech engine not available for voice selection.", Toast.LENGTH_LONG).show();
            voiceSpinner.setEnabled(false);
        }
    }

    private void populateVoiceSpinner() {
        if (tempTts == null) {
            Log.e(TAG, "tempTts is null, cannot populate voice spinner.");
            return;
        }

        Set<TextToSpeech.Voice> voices = tempTts.getVoices();
        availableVoices = new ArrayList<Map<String, String>>();
        List<String> voiceNames = new ArrayList<String>();
        String savedLocaleTag = prefs.getString("selectedVoiceLanguageTag", null);
        int selectedPosition = 0;

        for (TextToSpeech.Voice voice : voices) {
            // Only add voices that speak a valid language and are not network-only if offline
            if (voice.getFeatures() != null && voice.getLocale() != null && !voice.getLocale().getDisplayName().isEmpty()) {
                Map<String, String> voiceMap = new HashMap<String, String>();
                String displayName = voice.getName() + " (" + voice.getLocale().getDisplayName() + ")";
                String localeTag = voice.getLocale().toLanguageTag();

                voiceMap.put("displayName", displayName);
                voiceMap.put("localeTag", localeTag);
                availableVoices.add(voiceMap);
            }
        }

        // Sort voices alphabetically by display name
        Collections.sort(availableVoices, new Comparator<Map<String, String>>() {
            @Override
            public int compare(Map<String, String> m1, Map<String, String> m2) {
                return m1.get("displayName").compareTo(m2.get("displayName"));
            }
        });

        for (int i = 0; i < availableVoices.size(); i++) {
            Map<String, String> voiceMap = availableVoices.get(i);
            voiceNames.add(voiceMap.get("displayName"));
            if (savedLocaleTag != null && savedLocaleTag.equals(voiceMap.get("localeTag"))) {
                selectedPosition = i;
            }
        }

        voiceAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, voiceNames);
        voiceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        voiceSpinner.setAdapter(voiceAdapter);
        voiceSpinner.setSelection(selectedPosition);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tempTts != null) {
            tempTts.stop();
            tempTts.shutdown();
        }
    }
}