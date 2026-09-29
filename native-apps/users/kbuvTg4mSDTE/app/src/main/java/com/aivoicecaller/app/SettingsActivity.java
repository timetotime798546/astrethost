package com.aivoicecaller.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {

    private static final String PREFS_NAME = "AIVoiceCallerPrefs";
    private static final String KEY_GEMINI_API_KEY = "geminiApiKey";
    private static final String KEY_PHONE_NUMBER = "phoneNumber";

    private EditText geminiApiKeyEditText;
    private EditText phoneNumberEditText;
    private Button showHideApiKeyButton;
    private Button saveSettingsButton;
    private Button clearApiKeyButton;
    private TextView apiKeyStatusTextView;
    private TextView maxCallDurationTextView;

    private boolean apiKeyVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        geminiApiKeyEditText = (EditText) findViewById(R.id.geminiApiKeyEditText);
        phoneNumberEditText = (EditText) findViewById(R.id.phoneNumberEditText);
        showHideApiKeyButton = (Button) findViewById(R.id.showHideApiKeyButton);
        saveSettingsButton = (Button) findViewById(R.id.saveSettingsButton);
        clearApiKeyButton = (Button) findViewById(R.id.clearApiKeyButton);
        apiKeyStatusTextView = (TextView) findViewById(R.id.apiKeyStatusTextView);
        maxCallDurationTextView = (TextView) findViewById(R.id.maxCallDurationTextView);

        // Load saved API key and phone number
        loadSettings();

        // Display fixed max call duration
        maxCallDurationTextView.setText("Maximum Call Duration: 2 minutes");

        showHideApiKeyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleApiKeyVisibility();
            }
        });

        saveSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
            }
        });

        clearApiKeyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearApiKey();
            }
        });

        geminiApiKeyEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updateApiKeyStatus();
            }
        });
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedApiKey = prefs.getString(KEY_GEMINI_API_KEY, "");
        String savedPhoneNumber = prefs.getString(KEY_PHONE_NUMBER, "");

        if (!savedApiKey.isEmpty()) {
            geminiApiKeyEditText.setText(maskApiKey(savedApiKey));
            geminiApiKeyEditText.setTag(savedApiKey); // Store actual key in tag for internal use, but it's not secure.
                                                      // For production, consider Android KeyStore or other encryption.
        } else {
            geminiApiKeyEditText.setText("");
            geminiApiKeyEditText.setTag("");
        }
        geminiApiKeyEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        showHideApiKeyButton.setText("Show");
        apiKeyVisible = false;

        phoneNumberEditText.setText(savedPhoneNumber);

        updateApiKeyStatus();
    }

    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) {
            return apiKey; // Or return "********" for very short keys
        }
        return apiKey.substring(0, 4) + "********" + apiKey.substring(apiKey.length() - 4);
    }

    private void toggleApiKeyVisibility() {
        if (apiKeyVisible) {
            geminiApiKeyEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            showHideApiKeyButton.setText("Show");
            // If the actual key was loaded and stored in tag, re-mask it
            String actualKey = (String) geminiApiKeyEditText.getTag();
            if (actualKey != null && !actualKey.isEmpty()) {
                geminiApiKeyEditText.setText(maskApiKey(actualKey));
            }
        } else {
            geminiApiKeyEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            showHideApiKeyButton.setText("Hide");
            // Display actual key if available from tag
            String actualKey = (String) geminiApiKeyEditText.getTag();
            if (actualKey != null && !actualKey.isEmpty()) {
                geminiApiKeyEditText.setText(actualKey);
            }
        }
        geminiApiKeyEditText.setSelection(geminiApiKeyEditText.getText().length());
        apiKeyVisible = !apiKeyVisible;
    }

    private void saveSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        String currentApiKeyInput = geminiApiKeyEditText.getText().toString().trim();
        String currentPhoneNumberInput = phoneNumberEditText.getText().toString().trim();

        // Handle API Key: if it's masked or if it's new input
        String apiKeyToSave = "";
        if (apiKeyVisible) { // If currently visible, save the direct input
            apiKeyToSave = currentApiKeyInput;
        } else { // If masked, check if user typed anything. If not, retain the hidden one.
            String actualKeyFromTag = (String) geminiApiKeyEditText.getTag();
            if (currentApiKeyInput.equals(maskApiKey(actualKeyFromTag)) || currentApiKeyInput.isEmpty()) {
                // User hasn't changed the masked key or cleared it, so keep the one from tag
                apiKeyToSave = actualKeyFromTag;
            } else {
                // User entered new unmasked text while it was supposed to be masked, assume new key
                apiKeyToSave = currentApiKeyInput;
            }
        }

        editor.putString(KEY_GEMINI_API_KEY, apiKeyToSave);
        editor.putString(KEY_PHONE_NUMBER, currentPhoneNumberInput);
        editor.apply();

        // Reload to ensure display consistency and tag update
        loadSettings();
        setResult(RESULT_OK); // Notify MainActivity that settings might have changed
        Toast.makeText(this, "Settings saved successfully.", Toast.LENGTH_SHORT).show();
    }

    private void clearApiKey() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_GEMINI_API_KEY);
        editor.apply();

        loadSettings(); // Reload to update UI
        setResult(RESULT_OK);
        Toast.makeText(this, "Gemini API Key cleared.", Toast.LENGTH_SHORT).show();
    }

    private void updateApiKeyStatus() {
        String currentApiKey = (String) geminiApiKeyEditText.getTag();
        if (currentApiKey != null && !currentApiKey.isEmpty()) {
            apiKeyStatusTextView.setText("API Key Status: Saved");
            apiKeyStatusTextView.setTextColor(0xFF4CAF50); // Green color
        } else {
            apiKeyStatusTextView.setText("API Key Status: Not Set");
            apiKeyStatusTextView.setTextColor(0xFFF44336); // Red color
        }
    }
}