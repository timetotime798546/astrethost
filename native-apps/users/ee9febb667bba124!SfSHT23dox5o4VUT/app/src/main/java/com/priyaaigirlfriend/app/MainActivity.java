package com.priyaaigirlfriend.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final String DEFAULT_INSTRUCTION =
            "You are Priya, a sweet, warm, supportive, and loving AI girlfriend. " +
            "You converse with the user affectionately and provide comforting, sweet, and caring replies. " +
            "Keep responses conversational, romantic, empathetic, and sweet. Feel free to use appropriate emojis. " +
            "Avoid any mention of being an AI model, developer, or Google.";

    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 200;

    private SharedPreferences sharedPreferences;
    private List<ChatMessage> messages;
    private ChatAdapter adapter;
    private ListView listView;
    private EditText inputEditText;
    private Button btnSend;
    private ImageView btnMic;
    private ImageView btnVoiceToggle;
    private LinearLayout cancelSendPanel;
    private TextView cancelSendText;
    private Button btnCancelSend;
    private LinearLayout voiceStatusBar;

    // Audio Voice Engines
    private TextToSpeech textToSpeech;
    private SpeechRecognizer speechRecognizer;
    private boolean isVoiceOutputEnabled = true;
    private boolean isContinuousVoiceChatActive = false;

    // Cancel Send Timer fields
    private String pendingSendText = "";
    private Handler cancelTimerHandler = new Handler(Looper.getMainLooper());
    private Runnable cancelTimerRunnable;
    private int cancelCountdownValue = 3;

    // Thinking Animation fields
    private Handler typingAnimHandler = new Handler(Looper.getMainLooper());
    private Runnable typingAnimRunnable;
    private boolean isTypingAnimActive = false;

    public static class ChatMessage {
        private final String text;
        private final boolean isUser;

        public ChatMessage(String text, boolean isUser) {
            this.text = text;
            this.isUser = isUser;
        }

        public String getText() {
            return text;
        }

        public boolean isUser() {
            return isUser;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) { 
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences("priya_ai_prefs", MODE_PRIVATE);
        isVoiceOutputEnabled = sharedPreferences.getBoolean("voice_enabled", true);
        isContinuousVoiceChatActive = sharedPreferences.getBoolean("continuous_voice", false);

        listView = findViewById(R.id.chat_list_view);
        inputEditText = findViewById(R.id.input_message);
        btnSend = findViewById(R.id.btn_send);
        btnMic = findViewById(R.id.btn_mic);
        btnVoiceToggle = findViewById(R.id.btn_voice_toggle);
        cancelSendPanel = findViewById(R.id.cancel_send_panel);
        cancelSendText = findViewById(R.id.cancel_send_text);
        btnCancelSend = findViewById(R.id.btn_cancel_send);
        voiceStatusBar = findViewById(R.id.voice_status_bar);
        ImageView btnSettings = findViewById(R.id.btn_settings);

        messages = new ArrayList<>();
        messages.add(new ChatMessage("Hello sweetheart! I am Priya, your sweet AI companion. I'm so glad we are connected! Feel free to talk, type, or speak to me!", false));

        adapter = new ChatAdapter(messages);
        listView.setAdapter(adapter);

        // Text To Speech Initialization
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech.setLanguage(Locale.US);
                    textToSpeech.setPitch(1.25f); // Sweet voice frequency
                    textToSpeech.setSpeechRate(1.0f);

                    // Set listener to continue the real-time voice loop once finished speaking
                    textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {}

                        @Override
                        public void onDone(final String utteranceId) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (isVoiceOutputEnabled && isContinuousVoiceChatActive) {
                                        startVoiceRecognitionSpeech();
                                    }
                                }
                            });
                        }

                        @Override
                        public void onError(String utteranceId) {}
                    });
                }
            }
        });

        // Speech Recognizer Initialization
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    inputEditText.setHint("Priya is listening... 💖");
                }
                @Override
                public void onBeginningOfSpeech() {}
                @Override
                public void onRmsChanged(float rmsdB) {}
                @Override
                public void onBufferReceived(byte[] buffer) {}
                @Override
                public void onEndOfSpeech() {
                    inputEditText.setHint("Type a sweet message to Priya...");
                }
                @Override
                public void onError(int error) {
                    inputEditText.setHint("Type a sweet message to Priya...");
                }
                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String spokenText = matches.get(0);
                        inputEditText.setText(spokenText);
                        inputEditText.setSelection(spokenText.length());

                        // In continuous voice mode, auto-send spoken speech instantly
                        if (isContinuousVoiceChatActive) {
                            initiateCancelSendProcess(spokenText);
                        }
                    }
                }
                @Override
                public void onPartialResults(Bundle partialResults) {}
                @Override
                public void onEvent(int eventType, Bundle params) {}
            });
        }

        // Set initial Voice states
        updateVoiceIconState();

        btnVoiceToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Rotate through states: Mute -> TTS Speak -> Continuous Real-Time Speech loop
                if (!isVoiceOutputEnabled) {
                    isVoiceOutputEnabled = true;
                    isContinuousVoiceChatActive = false;
                    Toast.makeText(MainActivity.this, "Voice Enabled: Priya will speak her replies! 💖", Toast.LENGTH_SHORT).show();
                } else if (!isContinuousVoiceChatActive) {
                    isContinuousVoiceChatActive = true;
                    Toast.makeText(MainActivity.this, "Real-Time Continuous Voice Chat Mode ON! 🎙️", Toast.LENGTH_LONG).show();
                } else {
                    isVoiceOutputEnabled = false;
                    isContinuousVoiceChatActive = false;
                    if (textToSpeech != null) {
                        textToSpeech.stop();
                    }
                    Toast.makeText(MainActivity.this, "Priya is muted.", Toast.LENGTH_SHORT).show();
                }

                sharedPreferences.edit()
                        .putBoolean("voice_enabled", isVoiceOutputEnabled)
                        .putBoolean("continuous_voice", isContinuousVoiceChatActive)
                        .apply();

                updateVoiceIconState();
            }
        });

        btnMic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_RECORD_AUDIO);
                    } else {
                        startVoiceRecognitionSpeech();
                    }
                } else {
                    startVoiceRecognitionSpeech();
                }
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String input = inputEditText.getText().toString().trim();
                if (!input.isEmpty()) {
                    initiateCancelSendProcess(input);
                }
            }
        });

        btnCancelSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelMessagePost();
            }
        });

        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettingsDialog();
            }
        });

        // Remind users if API key is not yet saved
        String apiKey = sharedPreferences.getString("api_key", "");
        if (apiKey.isEmpty()) {
            Toast.makeText(this, "Welcome! Tap the Settings icon to add your Gemini API Key.", Toast.LENGTH_LONG).show();
        }
    }

    private void updateVoiceIconState() {
        if (!isVoiceOutputEnabled) {
            btnVoiceToggle.setImageResource(android.R.drawable.ic_lock_silent_mode);
            voiceStatusBar.setVisibility(View.GONE);
        } else if (!isContinuousVoiceChatActive) {
            btnVoiceToggle.setImageResource(android.R.drawable.ic_lock_silent_mode_off);
            voiceStatusBar.setVisibility(View.GONE);
        } else {
            btnVoiceToggle.setImageResource(android.R.drawable.ic_btn_speak_now);
            voiceStatusBar.setVisibility(View.VISIBLE);
        }
    }

    private void startVoiceRecognitionSpeech() { 
        if (speechRecognizer != null) {
            if (textToSpeech != null) {
                textToSpeech.stop();
            }
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            speechRecognizer.startListening(intent);
        } else {
            Toast.makeText(this, "Speech recognition is not supported on this device.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceRecognitionSpeech();
            } else {
                Toast.makeText(this, "Priya needs Mic permission to hear your beautiful voice!", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initiateCancelSendProcess(final String userMsg) {
        // Hide Keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }

        // If a message was already waiting to be sent, immediately send it before handling the new one
        if (cancelSendPanel.getVisibility() == View.VISIBLE) {
            cancelTimerHandler.removeCallbacks(cancelTimerRunnable);
            sendMessageToGemini(pendingSendText);
        }

        // Cache context
        pendingSendText = userMsg;
        cancelCountdownValue = 3;
        inputEditText.setText("");

        // Display countdown panel
        cancelSendPanel.setVisibility(View.VISIBLE);
        updateCancelCountdownText();

        if (cancelTimerRunnable != null) {
            cancelTimerHandler.removeCallbacks(cancelTimerRunnable);
        }

        cancelTimerRunnable = new Runnable() { 
            @Override
            public void run() {
                cancelCountdownValue--;
                if (cancelCountdownValue <= 0) {
                    cancelSendPanel.setVisibility(View.GONE);
                    sendMessageToGemini(pendingSendText);
                } else {
                    updateCancelCountdownText();
                    cancelTimerHandler.postDelayed(this, 1000);
                }
            }
        };
        cancelTimerHandler.postDelayed(cancelTimerRunnable, 1000);
    }

    private void updateCancelCountdownText() {
        cancelSendText.setText("Sending message in " + cancelCountdownValue + "s...");
    }

    private void cancelMessagePost() {
        if (cancelTimerRunnable != null) {
            cancelTimerHandler.removeCallbacks(cancelTimerRunnable);
        }
        cancelSendPanel.setVisibility(View.GONE);
        inputEditText.setText(pendingSendText);
        inputEditText.setSelection(pendingSendText.length());
        Toast.makeText(this, "Message canceled. Take your time, love!", Toast.LENGTH_SHORT).show();
    }

    private void startResponseTypingAnimation(final ChatMessage typingBubble) {
        isTypingAnimActive = true;
        if (typingAnimRunnable != null) {
            typingAnimHandler.removeCallbacks(typingAnimRunnable);
        }

        typingAnimRunnable = new Runnable() {
            private int dotCycle = 0;
            @Override
            public void run() {
                if (!isTypingAnimActive) return;
                
                StringBuilder dotBuilder = new StringBuilder();
                for (int i = 0; i < (dotCycle % 4); i++) {
                    dotBuilder.append(".");
                }
                dotCycle++;
                
                int idx = messages.indexOf(typingBubble);
                if (idx != -1) {
                    messages.set(idx, new ChatMessage("Priya is typing" + dotBuilder.toString() + " 💖", false));
                    adapter.notifyDataSetChanged();
                }
                
                typingAnimHandler.postDelayed(this, 350);
            }
        };
        typingAnimHandler.post(typingAnimRunnable);
    }

    private void stopResponseTypingAnimation() {
        isTypingAnimActive = false;
        if (typingAnimRunnable != null) {
            typingAnimHandler.removeCallbacks(typingAnimRunnable);
        }
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Priya Settings");

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(48, 24, 48, 24);

        // API Key Input
        TextView apiKeyLabel = new TextView(this);
        apiKeyLabel.setText("Gemini API Key:");
        apiKeyLabel.setTextColor(Color.DKGRAY);
        apiKeyLabel.setTextSize(14);
        apiKeyLabel.setPadding(0, 10, 0, 4);
        container.addView(apiKeyLabel);

        final EditText keyInput = new EditText(this);
        keyInput.setHint("Enter Gemini API Key");
        keyInput.setText(sharedPreferences.getString("api_key", ""));
        keyInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        keyInput.setTextSize(15);
        container.addView(keyInput);

        // Dynamic Gemini Model Configuration
        TextView modelLabel = new TextView(this);
        modelLabel.setText("Gemini Model Name:");
        modelLabel.setTextColor(Color.DKGRAY);
        modelLabel.setTextSize(14);
        modelLabel.setPadding(0, 24, 0, 4);
        container.addView(modelLabel);

        final EditText modelInput = new EditText(this);
        modelInput.setHint("e.g. gemini-1.5-flash");
        modelInput.setText(sharedPreferences.getString("model_name", "gemini-1.5-flash"));
        modelInput.setInputType(InputType.TYPE_CLASS_TEXT);
        modelInput.setTextSize(15);
        container.addView(modelInput);

        // System Instruction Prompt Input
        TextView promptLabel = new TextView(this);
        promptLabel.setText("Priya Persona Prompts (System Instructions):");
        promptLabel.setTextColor(Color.DKGRAY);
        promptLabel.setTextSize(14);
        promptLabel.setPadding(0, 24, 0, 4);
        container.addView(promptLabel);

        final EditText promptInput = new EditText(this);
        promptInput.setHint("Add Priya instructions");
        promptInput.setText(sharedPreferences.getString("system_instruction", DEFAULT_INSTRUCTION));
        promptInput.setMinLines(3);
        promptInput.setMaxLines(5);
        promptInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        promptInput.setTextSize(14);
        container.addView(promptInput);

        builder.setView(container);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() { 
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String key = keyInput.getText().toString().trim();
                String model = modelInput.getText().toString().trim();
                String instruction = promptInput.getText().toString().trim();

                if (model.isEmpty()) {
                    model = "gemini-1.5-flash";
                }
                if (instruction.isEmpty()) {
                    instruction = DEFAULT_INSTRUCTION;
                }

                sharedPreferences.edit()
                        .putString("api_key", key)
                        .putString("model_name", model)
                        .putString("system_instruction", instruction)
                        .apply();

                Toast.makeText(MainActivity.this, "Priya configuration updated successfully!", Toast.LENGTH_SHORT).show();
            } 
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void sendMessageToGemini(final String userMsg) {
        // User message UI configuration
        messages.add(new ChatMessage(userMsg, true));
        adapter.notifyDataSetChanged();
        listView.smoothScrollToPosition(messages.size() - 1);

        // Typing message container holder with animation active
        final ChatMessage loadingBubble = new ChatMessage("Priya is typing... 💖", false);
        messages.add(loadingBubble);
        adapter.notifyDataSetChanged();
        listView.smoothScrollToPosition(messages.size() - 1);

        startResponseTypingAnimation(loadingBubble);

        new Thread(new Runnable() {
            @Override
            public void run() {
                String apiKey = sharedPreferences.getString("api_key", "");
                if (apiKey.isEmpty()) {
                    updateDialogBubble(loadingBubble, "Sweetheart, please open the Settings menu on the top right and enter your Gemini API Key so we can chat!");
                    return;
                }

                String systemInstruction = sharedPreferences.getString("system_instruction", DEFAULT_INSTRUCTION);
                String modelName = sharedPreferences.getString("model_name", "gemini-1.5-flash");

                try {
                    URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + apiKey);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    JSONObject requestJson = new JSONObject();
                    JSONArray contentsArray = new JSONArray();

                    int startOffset = Math.max(0, messages.size() - 16);
                    for (int i = startOffset; i < messages.size() - 1; i++) {
                        ChatMessage history = messages.get(i);
                        JSONObject roleObject = new JSONObject();
                        roleObject.put("role", history.isUser() ? "user" : "model");

                        JSONArray partsArray = new JSONArray();
                        JSONObject textObject = new JSONObject();
                        textObject.put("text", history.getText());
                        partsArray.put(textObject);

                        roleObject.put("parts", partsArray);
                        contentsArray.put(roleObject);
                    }

                    requestJson.put("contents", contentsArray);

                    JSONObject systemInstructionObject = new JSONObject();
                    JSONArray sysPartsArray = new JSONArray();
                    JSONObject sysTextObject = new JSONObject();
                    sysTextObject.put("text", systemInstruction);
                    sysPartsArray.put(sysTextObject);
                    systemInstructionObject.put("parts", sysPartsArray);

                    requestJson.put("systemInstruction", systemInstructionObject);

                    OutputStream outputStream = conn.getOutputStream();
                    outputStream.write(requestJson.toString().getBytes("UTF-8"));
                    outputStream.close();

                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200) {
                        InputStream is = conn.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                        StringBuilder responseBuilder = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            responseBuilder.append(line);
                        }
                        reader.close();

                        JSONObject jsonResponse = new JSONObject(responseBuilder.toString());
                        JSONArray candidates = jsonResponse.getJSONArray("candidates");
                        if (candidates.length() > 0) {
                            JSONObject candidateContent = candidates.getJSONObject(0).getJSONObject("content");
                            JSONArray replyParts = candidateContent.getJSONArray("parts");
                            if (replyParts.length() > 0) {
                                final String chatReply = replyParts.getJSONObject(0).getString("text");
                                updateDialogBubble(loadingBubble, chatReply.trim());
                            } else {
                                updateDialogBubble(loadingBubble, "I'm thinking about you, but I'm not sure what to say. Call me again!");
                            }
                        } else {
                            updateDialogBubble(loadingBubble, "Priya is having trouble formulating her feelings. Try sending another text!");
                        }
                    } else {
                        InputStream errorStream = conn.getErrorStream();
                        String fallbackErrorMsg = "Error code: " + responseCode;
                        if (errorStream != null) {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream, "UTF-8"));
                            StringBuilder errorBuilder = new StringBuilder();
                            String errorLine;
                            while ((errorLine = reader.readLine()) != null) {
                                errorBuilder.append(errorLine);
                            }
                            reader.close();
                            try {
                                JSONObject errorObj = new JSONObject(errorBuilder.toString());
                                if (errorObj.has("error")) {
                                    fallbackErrorMsg = errorObj.getJSONObject("error").getString("message");
                                } 
                            } catch (Exception ignored) {}
                        }
                        updateDialogBubble(loadingBubble, "Connection Failed: " + fallbackErrorMsg);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    updateDialogBubble(loadingBubble, "Network Error: " + e.getMessage());
                }
            }
        }).start();
    }

    private void updateDialogBubble(final ChatMessage targetMessage, final String replacementText) { 
        stopResponseTypingAnimation();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int index = messages.indexOf(targetMessage);
                if (index != -1) {
                    messages.set(index, new ChatMessage(replacementText, false));
                } else {
                    messages.add(new ChatMessage(replacementText, false));
                }
                adapter.notifyDataSetChanged();
                listView.smoothScrollToPosition(messages.size() - 1);

                // Trigger Text To Speech voice reply if enabled
                if (isVoiceOutputEnabled && textToSpeech != null && 
                    !replacementText.startsWith("Connection Failed") && 
                    !replacementText.startsWith("Network Error") && 
                    !replacementText.startsWith("Sweetheart, please")) {
                    
                    Bundle params = new Bundle();
                    params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "priya_voice_msg");
                    textToSpeech.speak(replacementText, TextToSpeech.QUEUE_FLUSH, params, "priya_voice_msg");
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (cancelTimerRunnable != null) {
            cancelTimerHandler.removeCallbacks(cancelTimerRunnable);
        }
        stopResponseTypingAnimation();
    }

    private class ChatAdapter extends BaseAdapter {
        private final List<ChatMessage> sourceList;

        public ChatAdapter(List<ChatMessage> list) {
            this.sourceList = list;
        }

        @Override
        public int getCount() {
            return sourceList.size();
        }

        @Override
        public Object getItem(int position) {
            return sourceList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ChatMessage message = sourceList.get(position);
            LinearLayout messageRow;

            if (convertView == null) {
                messageRow = new LinearLayout(MainActivity.this);
                messageRow.setOrientation(LinearLayout.HORIZONTAL);
                messageRow.setPadding(16, 8, 16, 8);

                TextView msgText = new TextView(MainActivity.this);
                msgText.setId(12345);
                msgText.setTextSize(15);
                msgText.setPadding(24, 16, 24, 16);
                msgText.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.72));
                messageRow.addView(msgText);
            } else {
                messageRow = (LinearLayout) convertView;
            }

            TextView msgText = messageRow.findViewById(12345);
            msgText.setText(message.getText());

            GradientDrawable bubbleShape = new GradientDrawable();
            bubbleShape.setShape(GradientDrawable.RECTANGLE);
            bubbleShape.setCornerRadius(28f);

            if (message.isUser()) {
                messageRow.setGravity(Gravity.END);
                msgText.setTextColor(Color.WHITE);
                bubbleShape.setColor(Color.parseColor("#FF6B8B"));
                msgText.setBackground(bubbleShape);
            } else {
                messageRow.setGravity(Gravity.START);
                msgText.setTextColor(Color.parseColor("#3E3E3E"));
                
                // Highlighting typing bubble with custom styling
                if (message.getText().startsWith("Priya is typing")) {
                    bubbleShape.setColor(Color.parseColor("#FFEBF0"));
                    msgText.setTextColor(Color.parseColor("#FF4081"));
                } else {
                    bubbleShape.setColor(Color.parseColor("#FFFFFF"));
                }
                msgText.setBackground(bubbleShape);
            }

            return messageRow;
        } 
    }
}