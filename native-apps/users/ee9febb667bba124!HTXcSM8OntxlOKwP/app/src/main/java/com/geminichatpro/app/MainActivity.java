package com.geminichatpro.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity implements ChatAdapter.OnSpeakClickListener {

    // 40 Default Placeholder / Rotational Pool Keys
    private static final String[] TEMPLATE_KEYS = {
        "AIzaSyKeyPlaceholder01_Replace_In_Settings_Or_Click_StatusBar",
        "AIzaSyKeyPlaceholder02_Replace_In_Settings_Or_Click_StatusBar",
        "AIzaSyKeyPlaceholder03_Replace_In_Settings_Or_Click_StatusBar",
        "AIzaSyKeyPlaceholder04_Replace_In_Settings_Or_Click_StatusBar",
        "AIzaSyKeyPlaceholder05_Replace_In_Settings_Or_Click_StatusBar",
        "AIzaSyKeyPlaceholder06_Replace_In_Settings_Or_Click_StatusBar",
        "AIzaSyKeyPlaceholder07_Replace_In_Settings_Or_Click_StatusBar",
        "AIzaSyKeyPlaceholder08_Replace_In_Settings_Or_Click_StatusBar",
        "AIzaSyKeyPlaceholder09_Replace_In_Settings_Or_Click_StatusBar",
        "AIzaSyKeyPlaceholder10_Replace_In_Settings_Or_Click_StatusBar",
        "AIzaSyKeyPlaceholder11_Replace_In_Settings_Or_Click_StatusBar",
        "AIzaSyKeyPlaceholder12_Replace_In_Settings_Or_Click_StatusBar",
        "AIzaSyKeyPlaceholder13_Replace_In_Settings_Or_Click_StatusBar",
        "AIzaSyKeyPlaceholder14_Replace_In_Settings_Or_Click_StatusBar",
        "AIzaSyKeyPlaceholder15_Replace_In_Settings_Or_Click_StatusBar",
        "AIzaSyKeyPlaceholder16_Replace_In_Settings_Or_Click_StatusBar",
        "AIzaSyKeyPlaceholder17_Replace_In_Settings_Or_Click_StatusBar",
        "AIzaSyKeyPlaceholder18_Replace_In_Settings_Or_Click_StatusBar",
        "AIzaSyKeyPlaceholder19_Replace_In_Settings_Or_Click_StatusBar",
        "AIzaSyKeyPlaceholder20_Replace_In_Settings_Or_Click_StatusBar",
        "AIzaSyKeyPlaceholder21_Replace_In_Settings_Or_Click_StatusBar",
        "AIzaSyKeyPlaceholder22_Replace_In_Settings_Or_Click_StatusBar",
        "AIzaSyKeyPlaceholder23_Replace_In_Settings_Or_Click_StatusBar",
        "AIzaSyKeyPlaceholder24_Replace_In_Settings_Or_Click_StatusBar",
        "AIzaSyKeyPlaceholder25_Replace_In_Settings_Or_Click_StatusBar",
        "AIzaSyKeyPlaceholder26_Replace_In_Settings_Or_Click_StatusBar",
        "AIzaSyKeyPlaceholder27_Replace_In_Settings_Or_Click_StatusBar",
        "AIzaSyKeyPlaceholder28_Replace_In_Settings_Or_Click_StatusBar",
        "AIzaSyKeyPlaceholder29_Replace_In_Settings_Or_Click_StatusBar",
        "AIzaSyKeyPlaceholder30_Replace_In_Settings_Or_Click_StatusBar",
        "AIzaSyKeyPlaceholder31_Replace_In_Settings_Or_Click_StatusBar",
        "AIzaSyKeyPlaceholder32_Replace_In_Settings_Or_Click_StatusBar",
        "AIzaSyKeyPlaceholder33_Replace_In_Settings_Or_Click_StatusBar",
        "AIzaSyKeyPlaceholder34_Replace_In_Settings_Or_Click_StatusBar",
        "AIzaSyKeyPlaceholder35_Replace_In_Settings_Or_Click_StatusBar",
        "AIzaSyKeyPlaceholder36_Replace_In_Settings_Or_Click_StatusBar",
        "AIzaSyKeyPlaceholder37_Replace_In_Settings_Or_Click_StatusBar",
        "AIzaSyKeyPlaceholder38_Replace_In_Settings_Or_Click_StatusBar",
        "AIzaSyKeyPlaceholder39_Replace_In_Settings_Or_Click_StatusBar",
        "AIzaSyKeyPlaceholder40_Replace_In_Settings_Or_Click_StatusBar"
    };

    private List<String> apiKeysList = new ArrayList<>();
    private int activeKeyIndex = 0;
    private boolean isAutoTtsEnabled = true;

    private ListView lvChat;
    private EditText etInput;
    private ImageButton btnSend;
    private ImageButton btnAutoTts;
    private RelativeLayout layoutProgress;
    private LinearLayout layoutKeyStatus;
    private TextView tvRotationStatus;
    private TextView tvActiveKeyIndex;
    private Button btnRotateNow;

    private List<Message> messageList = new ArrayList<>();
    private ChatAdapter adapter;
    private TextToSpeech tts;
    private boolean isTtsReady = false;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize local rotation pool of 40 keys
        for (String key : TEMPLATE_KEYS) {
            apiKeysList.add(key);
        }

        // Bind views
        lvChat = (ListView) findViewById(R.id.lv_chat);
        etInput = (EditText) findViewById(R.id.et_input);
        btnSend = (ImageButton) findViewById(R.id.btn_send);
        btnAutoTts = (ImageButton) findViewById(R.id.btn_auto_tts);
        layoutProgress = (RelativeLayout) findViewById(R.id.layout_progress);
        layoutKeyStatus = (LinearLayout) findViewById(R.id.layout_key_status);
        tvRotationStatus = (TextView) findViewById(R.id.tv_rotation_status);
        tvActiveKeyIndex = (TextView) findViewById(R.id.tv_active_key_index);
        btnRotateNow = (Button) findViewById(R.id.btn_rotate_now);

        // Setup ListView adapter
        adapter = new ChatAdapter(this, messageList, this);
        lvChat.setAdapter(adapter);

        // Welcome greeting message
        addMessage("Hello! I am Gemini Chat assistant. Type any request below. You can easily click on my responses to hear them offline, or edit our rotating pool of 40 distinct keys!", false);

        // Initialize TextToSpeech engine safely
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    isTtsReady = true;
                    tts.setLanguage(Locale.US);
                } else {
                    isTtsReady = false;
                    Toast.makeText(MainActivity.this, "TTS initialization failed", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Auto TTS state change action
        btnAutoTts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isAutoTtsEnabled = !isAutoTtsEnabled;
                if (isAutoTtsEnabled) {
                    btnAutoTts.setImageResource(R.drawable.ic_speak);
                    Toast.makeText(MainActivity.this, "Auto TTS Enabled", Toast.LENGTH_SHORT).show();
                } else {
                    btnAutoTts.setImageResource(R.drawable.ic_mute);
                    Toast.makeText(MainActivity.this, "Auto TTS Disabled", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Force immediate round-robin rotation manually
        btnRotateNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rotateApiKey();
                Toast.makeText(MainActivity.this, "Manually rotated to Key index #" + (activeKeyIndex + 1), Toast.LENGTH_SHORT).show();
            }
        });

        // Click pool status layout to edit keys dynamically
        layoutKeyStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditKeysDialog();
            }
        });

        // Action sending text prompt
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String prompt = etInput.getText().toString().trim();
                if (!prompt.isEmpty()) {
                    etInput.setText("");
                    addMessage(prompt, true);
                    sendMessageToGemini(prompt);
                }
            }
        });

        updateRotationStatusUI();
    }

    private void addMessage(String text, boolean isUser) {
        String currentTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
        messageList.add(new Message(text, isUser, currentTime));
        adapter.notifyDataSetChanged();
        lvChat.post(new Runnable() {
            @Override
            public void run() {
                lvChat.setSelection(adapter.getCount() - 1);
            }
        });
    }

    private void rotateApiKey() {
        activeKeyIndex = (activeKeyIndex + 1) % apiKeysList.size();
        updateRotationStatusUI();
    }

    private void updateRotationStatusUI() {
        tvRotationStatus.setText("API Pool: " + apiKeysList.size() + " Keys Ready");
        tvActiveKeyIndex.setText("Active: Key #" + (activeKeyIndex + 1) + " (Click bar to overwrite or load custom keys)");
    }

    private void showEditKeysDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("API Rotation Hub (Key #" + (activeKeyIndex + 1) + ")");
        builder.setMessage("Enter single real Gemini API Key below to update this slot, or paste multiple keys separated by commas (,) to pre-populate successive pools:");

        final EditText input = new EditText(this);
        input.setHint("AIzaSy...");
        input.setText(apiKeysList.get(activeKeyIndex));
        builder.setView(input);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String keysText = input.getText().toString().trim();
                if (!keysText.isEmpty()) {
                    if (keysText.contains(",")) {
                        String[] keysArray = keysText.split(",");
                        int updatedCount = 0;
                        for (int i = 0; i < keysArray.length && i < apiKeysList.size(); i++) {
                            apiKeysList.set(i, keysArray[i].trim());
                            updatedCount++;
                        }
                        Toast.makeText(MainActivity.this, "Successfully loaded " + updatedCount + " custom keys into pool!", Toast.LENGTH_SHORT).show();
                    } else {
                        apiKeysList.set(activeKeyIndex, keysText);
                        Toast.makeText(MainActivity.this, "Active key slot updated successfully!", Toast.LENGTH_SHORT).show();
                    }
                    updateRotationStatusUI();
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void sendMessageToGemini(final String userPrompt) {
        layoutProgress.setVisibility(View.VISIBLE);

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                int retries = 0;
                boolean apiCallSuccess = false;
                String outputText = "";

                while (retries < apiKeysList.size() && !apiCallSuccess) {
                    final int currentIdx = activeKeyIndex;
                    final String apiKey = apiKeysList.get(currentIdx);

                    try {
                        URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Content-Type", "application/json");
                        conn.setDoOutput(true);
                        conn.setConnectTimeout(10000);
                        conn.setReadTimeout(10000);

                        // Build Request JSON body manually
                        JSONObject body = new JSONObject();
                        JSONArray contents = new JSONArray();
                        JSONObject contentItem = new JSONObject();
                        JSONArray parts = new JSONArray();
                        JSONObject partItem = new JSONObject();

                        partItem.put("text", userPrompt);
                        parts.put(partItem);
                        contentItem.put("parts", parts);
                        contents.put(contentItem);
                        body.put("contents", contents);

                        // Write request stream
                        OutputStream os = conn.getOutputStream();
                        os.write(body.toString().getBytes("UTF-8"));
                        os.flush();
                        os.close();

                        int responseCode = conn.getResponseCode();
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                            StringBuilder responseBuilder = new StringBuilder();
                            String line;
                            while ((line = br.readLine()) != null) {
                                responseBuilder.append(line);
                            }
                            br.close();

                            // Parse response payload
                            JSONObject resJson = new JSONObject(responseBuilder.toString());
                            JSONArray candidates = resJson.getJSONArray("candidates");
                            if (candidates.length() > 0) {
                                JSONObject firstCand = candidates.getJSONObject(0);
                                JSONObject innerContent = firstCand.getJSONObject("content");
                                JSONArray innerParts = innerContent.getJSONArray("parts");
                                if (innerParts.length() > 0) {
                                    outputText = innerParts.getJSONObject(0).getString("text");
                                    apiCallSuccess = true;
                                }
                            }
                        } else {
                            throw new Exception("HTTP Code " + responseCode);
                        }

                    } catch (final Exception e) {
                        // Current key failed, rotate and try another
                        retries++;
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Key #" + (currentIdx + 1) + " failed: " + e.getMessage() + ". Rotating...", Toast.LENGTH_SHORT).show();
                            }
                        });
                        rotateApiKey();
                    }
                }

                final boolean finalSuccess = apiCallSuccess;
                final String finalResponseText = outputText;

                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        layoutProgress.setVisibility(View.GONE);
                        if (finalSuccess) {
                            addMessage(finalResponseText, false);
                            if (isAutoTtsEnabled) {
                                speakText(finalResponseText);
                            }
                        } else {
                            addMessage("All 40 keys failed to connect. Please make sure you have entered a valid custom Gemini API key by clicking the status bar key helper.", false);
                        }
                    }
                });
            }
        });
    }

    @Override
    public void onSpeakClick(String text) {
        speakText(text);
    }

    private void speakText(String text) {
        if (!isTtsReady || tts == null) {
            Toast.makeText(this, "TextToSpeech Engine not ready!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Detect language content type dynamically (Hindi range detection vs default English locale)
        boolean matchesHindi = false;
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (character >= '\u0900' && character <= '\u097f') {
                matchesHindi = true;
                break;
            }
        }

        if (matchesHindi) {
            tts.setLanguage(new Locale("hi", "IN"));
        } else {
            tts.setLanguage(Locale.US);
        }

        // Perform safe speech query execution
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "GeminiSpeakUtteranceId");
        } else {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
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