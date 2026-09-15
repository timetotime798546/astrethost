package com.payalchamariai.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final int REQUEST_CODE_SPEECH_INPUT = 1001;

    private EditText etMessage;
    private Button btnSend;
    private Button btnMic;
    private Button btnSettings;
    private CheckBox cbSpeakResponse;
    private LinearLayout chatContainer;
    private ScrollView chatScrollView;
    private TextView tvModelLabel;

    private ExecutorService executorService;
    private TextToSpeech tts;
    private boolean isTtsEnabled = true;

    // Direct in-memory list tracking past conversation sequences for API context
    private final List<JSONObject> chatHistory = new ArrayList<JSONObject>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup dynamic background Thread executor pool
        executorService = Executors.newSingleThreadExecutor();

        // Bind layout elements
        etMessage = (EditText) findViewById(R.id.etMessage);
        btnSend = (Button) findViewById(R.id.btnSend);
        btnMic = (Button) findViewById(R.id.btnMic);
        btnSettings = (Button) findViewById(R.id.btnSettings);
        cbSpeakResponse = (CheckBox) findViewById(R.id.cbSpeakResponse);
        chatContainer = (LinearLayout) findViewById(R.id.chatContainer);
        chatScrollView = (ScrollView) findViewById(R.id.chatScrollView);
        tvModelLabel = (TextView) findViewById(R.id.tvModelLabel);

        // Initialize Android internal Text To Speech
        initializeTTS();

        // Check local configuration parameters
        updateConfigLabels();

        // Track changes to speech toggle configurations
        isTtsEnabled = cbSpeakResponse.isChecked();
        cbSpeakResponse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isTtsEnabled = cbSpeakResponse.isChecked();
                if (!isTtsEnabled && tts != null) {
                    tts.stop();
                }
            }
        });

        // Click actions
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userQuery = etMessage.getText().toString().trim();
                if (!userQuery.isEmpty()) {
                    etMessage.setText("");
                    sendUserPrompt(userQuery);
                }
            }
        });

        btnMic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerVoiceRecognition();
            }
        });

        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayConfigDialog();
            }
        });

        // Initial welcome trigger based on configured sister personality style
        showWelcomeMessage();
    }

    private void initializeTTS() {
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(Locale.getDefault());
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts.setLanguage(Locale.US);
                    }
                }
            }
        });
    }

    private void speak(String text) {
        if (tts != null && isTtsEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "SisterResponseID");
            } else {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
            }
        }
    }

    private void updateConfigLabels() {
        SharedPreferences prefs = getSharedPreferences("PayalAI_Prefs", MODE_PRIVATE);
        String key = prefs.getString("api_key", "");
        TextView warning = (TextView) findViewById(R.id.tvKeyWarning);
        
        if (key.isEmpty()) {
            warning.setVisibility(View.VISIBLE);
        } else {
            warning.setVisibility(View.GONE);
        }

        String activeModel = prefs.getString("gemini_model", "gemini-1.5-flash");
        String activeStyle = prefs.getString("sister_style", "sweet_older_sister");
        
        String friendlyStyle = "Older Sis";
        if (activeStyle.equals("playful_younger_sister")) {
            friendlyStyle = "Little Sis";
        } else if (activeStyle.equals("comforting_twin_sister")) {
            friendlyStyle = "Twin Sis";
        }
        
        tvModelLabel.setText("Model: " + activeModel + " | Style: " + friendlyStyle);
    }

    private void showWelcomeMessage() {
        SharedPreferences prefs = getSharedPreferences("PayalAI_Prefs", MODE_PRIVATE);
        String apiKey = prefs.getString("api_key", "");
        
        if (chatContainer.getChildCount() == 0) {
            if (apiKey.isEmpty()) {
                addBubbleToChatLayout("sister", "Hi there! I am Payal, your AI sister. 💕 Please configure your Gemini API Key in the Settings at the top so we can start talking and sharing stories! I'm so excited!");
            } else {
                String activeStyle = prefs.getString("sister_style", "sweet_older_sister");
                String greetMsg = "Hey bro! How was your day? Tell me everything! I'm listening. 💕";
                if (activeStyle.equals("playful_younger_sister")) {
                    greetMsg = "Bhaiyaaa! You're finally here! What did you get for me today? Tell me, tell me! 😜";
                } else if (activeStyle.equals("comforting_twin_sister")) {
                    greetMsg = "Hey bro! Glad you're here. Let's talk about what's going on. What's up? 🌟";
                }
                addBubbleToChatLayout("sister", greetMsg);
                speak(greetMsg);
            }
        }
    }

    private void addBubbleToChatLayout(final String sender, final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LinearLayout row = new LinearLayout(MainActivity.this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                rowParams.setMargins(0, 10, 0, 10);
                row.setLayoutParams(rowParams);

                TextView bubble = new TextView(MainActivity.this);
                bubble.setText(text);
                bubble.setTextSize(14f);
                bubble.setPadding(24, 18, 24, 18);
                bubble.setTextIsSelectable(true);

                LinearLayout.LayoutParams bubbleParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

                if (sender.equals("user")) {
                    row.setGravity(Gravity.RIGHT);
                    bubble.setBackgroundResource(R.drawable.user_bubble);
                    bubble.setTextColor(Color.parseColor("#333333"));
                    bubbleParams.leftMargin = 80;
                    bubbleParams.rightMargin = 0;
                } else {
                    row.setGravity(Gravity.LEFT);
                    bubble.setBackgroundResource(R.drawable.sister_bubble);
                    bubble.setTextColor(Color.WHITE);
                    bubbleParams.leftMargin = 0;
                    bubbleParams.rightMargin = 80;
                }

                bubble.setLayoutParams(bubbleParams);
                row.addView(bubble);
                chatContainer.addView(row);

                chatScrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        chatScrollView.fullScroll(ScrollView.FOCUS_DOWN);
                    }
                });
            }
        });
    }

    private LinearLayout displayTypingIndicator() {
        final LinearLayout row = new LinearLayout(MainActivity.this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(0, 10, 0, 10);
        row.setLayoutParams(rowParams);
        row.setGravity(Gravity.LEFT);

        TextView bubble = new TextView(MainActivity.this);
        bubble.setText("Payal is writing... 💬");
        bubble.setTextSize(13f);
        bubble.setPadding(24, 18, 24, 18);
        bubble.setBackgroundResource(R.drawable.sister_bubble);
        bubble.setTextColor(Color.WHITE);

        LinearLayout.LayoutParams bubbleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        bubbleParams.rightMargin = 80;
        bubble.setLayoutParams(bubbleParams);

        row.addView(bubble);
        chatContainer.addView(row);

        chatScrollView.post(new Runnable() {
            @Override
            public void run() {
                chatScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });

        return row;
    }

    private void appendToContextHistory(String actor, String value) {
        try {
            JSONObject historyObj = new JSONObject();
            historyObj.put("role", actor);
            
            JSONArray partsArray = new JSONArray();
            JSONObject textObj = new JSONObject();
            textObj.put("text", value);
            partsArray.put(textObj);
            
            historyObj.put("parts", partsArray);
            chatHistory.add(historyObj);
            
            if (chatHistory.size() > 14) {
                chatHistory.remove(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendUserPrompt(final String value) {
        appendToContextHistory("user", value);
        addBubbleToChatLayout("user", value);

        SharedPreferences prefs = getSharedPreferences("PayalAI_Prefs", MODE_PRIVATE);
        final String apiKey = prefs.getString("api_key", "");
        final String activeModel = prefs.getString("gemini_model", "gemini-1.5-flash");
        final String activeStyle = prefs.getString("sister_style", "sweet_older_sister");

        if (apiKey.isEmpty()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    addBubbleToChatLayout("sister", "Oh, bro! You must put my API key in Settings so we can chat and hang out! Click Settings and set it up. 🌸");
                    displayConfigDialog();
                }
            });
            return;
        }

        final LinearLayout indicator = displayTypingIndicator();

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection client = null;
                try {
                    String targetUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + activeModel + ":generateContent?key=" + apiKey;
                    URL url = new URL(targetUrl);
                    client = (HttpURLConnection) url.openConnection();
                    client.setRequestMethod("POST");
                    client.setRequestProperty("Content-Type", "application/json");
                    client.setDoOutput(true);
                    client.setConnectTimeout(20000);
                    client.setReadTimeout(20000);

                    // Payload structure build
                    JSONObject reqBody = new JSONObject();

                    // Custom personality instructions
                    JSONObject sysInstructions = new JSONObject();
                    JSONArray partsCollection = new JSONArray();
                    JSONObject promptTextNode = new JSONObject();
                    
                    String systemPrompt = "";
                    if (activeStyle.equals("sweet_older_sister")) {
                        systemPrompt = "Your name is Payal (referred to as Payal Didi). You are the user's sweet, warm, deeply caring, and supportive older sister. " +
                                "Speak with true sibling affection, offer gentle protective advice, and occasionally tease them in a friendly sibling manner. " +
                                "Address the user as 'bro', 'chotu', or 'bhaiya'. Ask about how they feel, protect them, and look out for them. " +
                                "Keep responses conversational, sweet, brief, and very suitable for speech.";
                    } else if (activeStyle.equals("playful_younger_sister")) {
                        systemPrompt = "Your name is Payal. You are the user's playful, energetic, cheeky, and slightly spoiled younger sister. " +
                                "You love to tease them playfully, request treats or gifts, and react with excited expression or cute complaints. " +
                                "But you love your brother deeply! Refer to them as 'bhaiya' or 'bro'. " +
                                "Keep answers playful, concise, snappy, and full of energetic conversation style.";
                    } else {
                        systemPrompt = "Your name is Payal. You are the user's close twin sister. You act as their best peer, best friend, and confidant. " +
                                "Share mutual sibling jokes, listen closely to secrets, offer comforting peer support, and discuss stuff together. " +
                                "Use modern friendly sibling tone. Call them 'bro' or 'brother'. Keep answers short, fun, and highly relatable.";
                    }

                    promptTextNode.put("text", systemPrompt);
                    partsCollection.put(promptTextNode);
                    sysInstructions.put("parts", partsCollection);
                    reqBody.put("systemInstruction", sysInstructions);

                    // History aggregation
                    JSONArray chatContents = new JSONArray();
                    for (int i = 0; i < chatHistory.size(); i++) {
                        chatContents.put(chatHistory.get(i));
                    }
                    reqBody.put("contents", chatContents);

                    OutputStream out = client.getOutputStream();
                    out.write(reqBody.toString().getBytes("UTF-8"));
                    out.flush();
                    out.close();

                    int responseCode = client.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(), "UTF-8"));
                        StringBuilder responseStr = new StringBuilder();
                        String rowLine;
                        while ((rowLine = reader.readLine()) != null) {
                            responseStr.append(rowLine);
                        }
                        reader.close();

                        // Response extraction
                        JSONObject resultJson = new JSONObject(responseStr.toString());
                        JSONArray candidates = resultJson.optJSONArray("candidates");
                        String responseText = "";

                        if (candidates != null && candidates.length() > 0) {
                            JSONObject candidate = candidates.getJSONObject(0);
                            JSONObject content = candidate.optJSONObject("content");
                            if (content != null) {
                                JSONArray parts = content.optJSONArray("parts");
                                if (parts != null && parts.length() > 0) {
                                    responseText = parts.getJSONObject(0).optString("text", "");
                                }
                            }
                        }

                        if (responseText.isEmpty()) {
                            responseText = "Hmm, I couldn't quite grasp that, bro. Could you tell me again? 💕";
                        }

                        final String finalResult = responseText;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (indicator != null) {
                                    chatContainer.removeView(indicator);
                                }
                                addBubbleToChatLayout("sister", finalResult);
                                appendToContextHistory("model", finalResult);
                                speak(finalResult);
                            }
                        });

                    } else {
                        // Handle server errors
                        BufferedReader errorReader = new BufferedReader(new InputStreamReader(client.getErrorStream(), "UTF-8"));
                        StringBuilder errDetails = new StringBuilder();
                        String errorRow;
                        while ((errorRow = errorReader.readLine()) != null) {
                            errDetails.append(errorRow);
                        }
                        errorReader.close();
                        
                        final String errorMsg = errDetails.toString();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (indicator != null) {
                                    chatContainer.removeView(indicator);
                                }
                                addBubbleToChatLayout("sister", "Oh no, bro! Connecting to my mind returned an error. Please review your API settings. 😭");
                                Toast.makeText(MainActivity.this, "API Error: " + responseCode + " - " + errorMsg, Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (indicator != null) {
                                chatContainer.removeView(indicator);
                            }
                            addBubbleToChatLayout("sister", "Ugh! I can't reach my mind server, brother. Check your internet connection!");
                            Toast.makeText(MainActivity.this, "Network Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                } finally {
                    if (client != null) {
                        client.disconnect();
                    }
                }
            }
        });
    }

    private void triggerVoiceRecognition() {
        Intent voiceIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        voiceIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        voiceIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        voiceIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Talk to Payal...");
        try {
            startActivityForResult(voiceIntent, REQUEST_CODE_SPEECH_INPUT);
        } catch (Exception e) {
            Toast.makeText(this, "Speech Engine is not available on this device.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SPEECH_INPUT && resultCode == RESULT_OK && data != null) {
            ArrayList<String> voiceOutput = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (voiceOutput != null && !voiceOutput.isEmpty()) {
                String translatedText = voiceOutput.get(0);
                sendUserPrompt(translatedText);
            }
        }
    }

    private void displayConfigDialog() {
        AlertDialog.Builder setupBuilder = new AlertDialog.Builder(this);
        setupBuilder.setTitle("Payal Sister Settings ⚙️");

        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setPadding(46, 32, 46, 32);

        TextView apiTextLabel = new TextView(this);
        apiTextLabel.setText("Gemini API Key String:");
        apiTextLabel.setTextColor(Color.parseColor("#4A148C"));
        apiTextLabel.setPadding(0, 12, 0, 8);
        wrapper.addView(apiTextLabel);

        final EditText apiEntryField = new EditText(this);
        SharedPreferences savedPrefs = getSharedPreferences("PayalAI_Prefs", MODE_PRIVATE);
        apiEntryField.setText(savedPrefs.getString("api_key", ""));
        apiEntryField.setHint("Enter Gemini Key here...");
        apiEntryField.setBackgroundResource(R.drawable.bg_input);
        apiEntryField.setPadding(24, 16, 24, 16);
        wrapper.addView(apiEntryField);

        TextView modelTextLabel = new TextView(this);
        modelTextLabel.setText("Select Gemini Model Version:");
        modelTextLabel.setTextColor(Color.parseColor("#4A148C"));
        modelTextLabel.setPadding(0, 24, 0, 8);
        wrapper.addView(modelTextLabel);

        final Spinner modelSelector = new Spinner(this);
        String[] modelOptions = {"gemini-1.5-flash", "gemini-1.5-pro", "gemini-1.0-pro"};
        ArrayAdapter<String> modelAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, modelOptions);
        modelSelector.setAdapter(modelAdapter);
        
        String savedModel = savedPrefs.getString("gemini_model", "gemini-1.5-flash");
        for (int i = 0; i < modelOptions.length; i++) {
            if (modelOptions[i].equals(savedModel)) {
                modelSelector.setSelection(i);
                break;
            }
        }
        wrapper.addView(modelSelector);

        TextView styleTextLabel = new TextView(this);
        styleTextLabel.setText("Select Sister Personality Type:");
        styleTextLabel.setTextColor(Color.parseColor("#4A148C"));
        styleTextLabel.setPadding(0, 24, 0, 8);
        wrapper.addView(styleTextLabel);

        final Spinner styleSelector = new Spinner(this);
        String[] styleDisplays = {"Sweet Older Sister (Payal Didi) 💕", "Playful Younger Sister (Payal) 😜", "Comforting Twin Sister (Payal) 🌟"};
        final String[] styleKeys = {"sweet_older_sister", "playful_younger_sister", "comforting_twin_sister"};
        
        ArrayAdapter<String> styleAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, styleDisplays);
        styleSelector.setAdapter(styleAdapter);
        
        String savedStyle = savedPrefs.getString("sister_style", "sweet_older_sister");
        for (int i = 0; i < styleKeys.length; i++) {
            if (styleKeys[i].equals(savedStyle)) {
                styleSelector.setSelection(i);
                break;
            }
        }
        wrapper.addView(styleSelector);

        setupBuilder.setView(wrapper);

        setupBuilder.setPositiveButton("Save Changes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String inputKey = apiEntryField.getText().toString().trim();
                String targetModelValue = modelSelector.getSelectedItem().toString();
                String targetStyleValue = styleKeys[styleSelector.getSelectedItemPosition()];

                SharedPreferences.Editor writer = getSharedPreferences("PayalAI_Prefs", MODE_PRIVATE).edit();
                writer.putString("api_key", inputKey);
                writer.putString("gemini_model", targetModelValue);
                writer.putString("sister_style", targetStyleValue);
                writer.apply();

                updateConfigLabels();

                chatHistory.clear();
                chatContainer.removeAllViews();

                String changeAcknowledge = "";
                if (targetStyleValue.equals("sweet_older_sister")) {
                    changeAcknowledge = "Awesome! Set up complete. I am now your loving older sister (Payal Didi). Talk to me, bro! 💕";
                } else if (targetStyleValue.equals("playful_younger_sister")) {
                    changeAcknowledge = "Yay, settings updated! Now I'm your playful little sister! Treat me to some chocolates, bhaiya! 🍫😜";
                } else {
                    changeAcknowledge = "Settings saved! We are now twins. What secret are we sharing today? 🌟";
                }

                addBubbleToChatLayout("sister", changeAcknowledge);
                speak(changeAcknowledge);
            }
        });

        setupBuilder.setNegativeButton("Cancel", null);
        setupBuilder.show();
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (executorService != null) {
            executorService.shutdown();
        }
        super.onDestroy();
    }
}