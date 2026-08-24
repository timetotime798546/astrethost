package com.hospitalaibooker.app;

/*
 * =========================================================================================
 *                      SECURE PRODUCTION-READY BACKEND PHP SCRIPT
 * =========================================================================================
 * Create a file named `book_api.php` on your host server, setup your database,
 * and paste the following content:
 *
 * <?php
 * header("Content-Type: application/json; charset=UTF-8");
 * header("Access-Control-Allow-Origin: *");
 * header("Access-Control-Allow-Methods: POST, GET");
 * header("Access-Control-Allow-Headers: Content-Type");
 *
 * // SECURE DATABASE CONNECTION CREDENTIALS
 * $host = "localhost";
 * $db_user = "YOUR_DB_USER";
 * $db_pass = "YOUR_DB_PASS";
 * $db_name = "hospital_bookings_db";
 *
 * try {
 *     $pdo = new PDO("mysql:host=$host;dbname=$db_name;charset=utf8", $db_user, $db_pass, [
 *         PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
 *         PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC
 *     ]);
 * } catch (PDOException $e) {
 *     echo json_encode(["status" => "error", "message" => "Database connection failed."]);
 *     exit;
 * }
 *
 * // AUTO INITIALIZE APPOINTMENTS SCHEMA TABLE FOR HIGHLY COMPATIBLE INTEGRATION
 * $pdo->exec("CREATE TABLE IF NOT EXISTS appointments (
 *     id INT AUTO_INCREMENT PRIMARY KEY,
 *     name VARCHAR(100) NOT NULL,
 *     age VARCHAR(10) NOT NULL,
 *     phone VARCHAR(50) NOT NULL,
 *     department VARCHAR(100) NOT NULL,
 *     doctor VARCHAR(100) NOT NULL,
 *     date VARCHAR(50) NOT NULL,
 *     time VARCHAR(50) NOT NULL,
 *     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
 * ) ENGINE=InnoDB DEFAULT CHARSET=utf8;");
 *
 * $method = $_SERVER['REQUEST_METHOD'];
 *
 * if ($method === 'POST') {
 *     // INSERT NEW CONFIRMED APPOINTMENT SECURELY VIA SQL PREPARED STATEMENTS
 *     $input = json_decode(file_get_contents('php://input'), true);
 *     
 *     if (empty($input['name']) || empty($input['phone']) || empty($input['doctor'])) {
 *         echo json_encode(["status" => "error", "message" => "Required patient information is missing."]);
 *         exit;
 *     }
 *
 *     $stmt = $pdo->prepare("INSERT INTO appointments (name, age, phone, department, doctor, date, time) VALUES (?, ?, ?, ?, ?, ?, ?)");
 *     try {
 *         $stmt->execute([
 *             htmlspecialchars($input['name']),
 *             htmlspecialchars($input['age']),
 *             htmlspecialchars($input['phone']),
 *             htmlspecialchars($input['department']),
 *             htmlspecialchars($input['doctor']),
 *             htmlspecialchars($input['date']),
 *             htmlspecialchars($input['time'])
 *         ]);
 *         
 *         echo json_encode([
 *             "status" => "success", 
 *             "message" => "Appointment successfully registered in hospital SQL server.",
 *             "booking_id" => $pdo->lastInsertId()
 *         ]);
 *     } catch (PDOException $e) {
 *         echo json_encode(["status" => "error", "message" => "Database insert transaction failed: " . $e->getMessage()]);
 *     }
 *     exit;
 * }
 *
 * if ($method === 'GET') {
 *     // RETRIEVE ALL RECORDED DATA FOR ADMIN PANEL LIST VIEW
 *     try {
 *         $stmt = $pdo->query("SELECT * FROM appointments ORDER BY id DESC");
 *         $rows = $stmt->fetchAll();
 *         echo json_encode(["status" => "success", "data" => $rows]);
 *     } catch (PDOException $e) {
 *         echo json_encode(["status" => "error", "message" => "Fetch transaction failed: " . $e->getMessage()]);
 *     }
 *     exit;
 * }
 * ?>
 */

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
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
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 101;
    private static final String PREFS_NAME = "HospitalPrefs";
    private static final String DEFAULT_SYSTEM_PROMPT = 
        "You are an intelligent Hospital Appointment Assistant. Your job is to converse with the user and collect these 7 pieces of information:\n"
        + "1. patient name\n2. age\n3. phone number\n4. department\n5. doctor\n6. date\n7. time\n\n"
        + "Keep your replies friendly, precise, and professional. Only ask for one or two pieces of information at a time. If the user provides info, save it and update the state metadata.\n"
        + "If any field is missing, continue seeking it naturally.\n"
        + "When all 7 fields are successfully populated, present a formatted list of the summary, set \"is_ready_to_confirm\": true and ask the user if they agree to book.\n"
        + "Wait for the patient to explicitly say words like 'yes', 'confirm', 'book', or 'correct' in their message before setting \"is_confirmed\": true.\n"
        + "CRITICAL: You MUST output ONLY valid JSON format in your response. No codeblocks, no markdown syntax around your JSON!\n"
        + "Format of JSON response schema to return:\n"
        + "{\n"
        + "  \"reply\": \"your conversational voice message text here\",\n"
        + "  \"metadata\": {\n"
        + "    \"name\": \"value or empty\",\n"
        + "    \"age\": \"value or empty\",\n"
        + "    \"phone\": \"value or empty\",\n"
        + "    \"department\": \"value or empty\",\n"
        + "    \"doctor\": \"value or empty\",\n"
        + "    \"date\": \"value or empty\",\n"
        + "    \"time\": \"value or empty\"\n"
        + "  },\n"
        + "  \"is_ready_to_confirm\": true_or_false,\n"
        + "  \"is_confirmed\": true_or_false\n"
        + "}";

    // SharedPreferences values
    private String apiKey = "";
    private String systemPrompt = "";
    private String backendUrl = "";

    // Core local state metadata representing state parameters
    private JSONObject appointmentMetadata;

    // UI Views
    private LinearLayout tabLayoutChat;
    private LinearLayout tabLayoutAdmin;
    private ScrollView tabLayoutSettings;
    private Button btnTabChat, btnTabAdmin, btnTabSettings;
    private TextView connectionStatusText;

    // Chat UI components
    private ScrollView scrollChatView;
    private LinearLayout layoutMessageBubbles;
    private EditText edtTxtUserInput;
    private Button btnSendText;
    private ImageButton btnRecordMic;
    private AiAnimationView aiAnimationIndicatorView;
    private TextView txtAIStatusState;
    
    // Metadata badges indicator
    private TextView badgeName, badgeAge, badgePhone, badgeDept, badgeDoctor, badgeDate, badgeTime, badgeStatus;
    private LinearLayout layoutConfirmBookingContainer;
    private Button btnConfirmDirect, btnResetTracker;

    // Admin views
    private ListView listAdminBookings;
    private Button btnRefreshBookings;
    private TextView txtAdminStatusPlaceholder;

    // Settings inputs
    private EditText edtSettingsApiKey, edtSettingsSystemInstruction, edtSettingsBackendUrl;
    private Button btnSaveSettings;

    // Voice & Speech engines
    private TextToSpeech ttsEngine;
    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;
    private boolean isListening = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize local empty session metadata state representation
        resetStateMetadata();

        // Bind all visual configurations
        initUiElements();
        loadStoredSettings();
        setupVoiceAssistant();
        setupNavigationTabs();

        // Set initial state greeting message
        addChatBubble("AI Assistant", "Hello! I am your virtual hospital coordinator. Please tell me your name, and which department or doctor you would like to visit today so I can book your appointment!", false);
    }

    private void initUiElements() {
        tabLayoutChat = findViewById(R.id.tabLayoutChat);
        tabLayoutAdmin = findViewById(R.id.tabLayoutAdmin);
        tabLayoutSettings = findViewById(R.id.tabLayoutSettings);

        btnTabChat = findViewById(R.id.btnTabChat);
        btnTabAdmin = findViewById(R.id.btnTabAdmin);
        btnTabSettings = findViewById(R.id.btnTabSettings);
        connectionStatusText = findViewById(R.id.connectionStatusText);

        // Chat items
        scrollChatView = findViewById(R.id.scrollChatView);
        layoutMessageBubbles = findViewById(R.id.layoutMessageBubbles);
        edtTxtUserInput = findViewById(R.id.edtTxtUserInput);
        btnSendText = findViewById(R.id.btnSendText);
        btnRecordMic = findViewById(R.id.btnRecordMic);
        aiAnimationIndicatorView = findViewById(R.id.aiAnimationIndicatorView);
        txtAIStatusState = findViewById(R.id.txtAIStatusState);

        // Metadata indicators
        badgeName = findViewById(R.id.badgeName);
        badgeAge = findViewById(R.id.badgeAge);
        badgePhone = findViewById(R.id.badgePhone);
        badgeDept = findViewById(R.id.badgeDept);
        badgeDoctor = findViewById(R.id.badgeDoctor);
        badgeDate = findViewById(R.id.badgeDate);
        badgeTime = findViewById(R.id.badgeTime);
        badgeStatus = findViewById(R.id.badgeStatus);
        layoutConfirmBookingContainer = findViewById(R.id.layoutConfirmBookingContainer);
        btnConfirmDirect = findViewById(R.id.btnConfirmDirect);
        btnResetTracker = findViewById(R.id.btnResetTracker);

        // Admin components
        listAdminBookings = findViewById(R.id.listAdminBookings);
        btnRefreshBookings = findViewById(R.id.btnRefreshBookings);
        txtAdminStatusPlaceholder = findViewById(R.id.txtAdminStatusPlaceholder);

        // Settings components
        edtSettingsApiKey = findViewById(R.id.edtSettingsApiKey);
        edtSettingsSystemInstruction = findViewById(R.id.edtSettingsSystemInstruction);
        edtSettingsBackendUrl = findViewById(R.id.edtSettingsBackendUrl);
        btnSaveSettings = findViewById(R.id.btnSaveSettings);

        // User events bindings
        btnSendText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processUserTextSubmit();
            }
        });

        btnRecordMic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleVoiceRecording();
            }
        });

        btnConfirmDirect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bookAppointmentOnBackend();
            }
        });

        btnResetTracker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetStateMetadata();
                updateMetadataIndicatorUi();
                addChatBubble("System", "Appointment tracking state has been cleared.", true);
            }
        });

        btnSaveSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveConfigurationSettings();
            }
        });

        btnRefreshBookings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchAdminAppointmentsList();
            }
        });
    }

    private void resetStateMetadata() {
        try {
            appointmentMetadata = new JSONObject();
            appointmentMetadata.put("name", "");
            appointmentMetadata.put("age", "");
            appointmentMetadata.put("phone", "");
            appointmentMetadata.put("department", "");
            appointmentMetadata.put("doctor", "");
            appointmentMetadata.put("date", "");
            appointmentMetadata.put("time", "");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadStoredSettings() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        apiKey = preferences.getString("apiKey", "");
        systemPrompt = preferences.getString("systemPrompt", DEFAULT_SYSTEM_PROMPT);
        backendUrl = preferences.getString("backendUrl", "");

        // Display on fields
        edtSettingsApiKey.setText(apiKey);
        edtSettingsSystemInstruction.setText(systemPrompt);
        edtSettingsBackendUrl.setText(backendUrl);

        if (apiKey.isEmpty()) {
            Toast.makeText(this, "Please head to Settings to save your Gemini API Key!", Toast.LENGTH_LONG).show();
        }
    }

    private void saveConfigurationSettings() {
        apiKey = edtSettingsApiKey.getText().toString().trim();
        systemPrompt = edtSettingsSystemInstruction.getText().toString().trim();
        backendUrl = edtSettingsBackendUrl.getText().toString().trim();

        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putString("apiKey", apiKey);
        editor.putString("systemPrompt", systemPrompt);
        editor.putString("backendUrl", backendUrl);
        editor.apply();

        Toast.makeText(this, "Settings configuration saved successfully!", Toast.LENGTH_SHORT).show();
        resetStateMetadata();
        updateMetadataIndicatorUi();
    }

    private void setupVoiceAssistant() {
        // Text-To-Speech initial setup
        ttsEngine = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    ttsEngine.setLanguage(Locale.US);
                    ttsEngine.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    setAiState(AiAnimationView.STATE_SPEAKING, "AI is speaking...");
                                }
                            });
                        }

                        @Override
                        public void onDone(String utteranceId) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    setAiState(AiAnimationView.STATE_IDLE, "AI is sleeping");
                                }
                            });
                        }

                        @Override
                        public void onError(String utteranceId) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    setAiState(AiAnimationView.STATE_IDLE, "AI is sleeping");
                                }
                            });
                        }
                    });
                }
            }
        });

        // Speech-To-Text configurations
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                setAiState(AiAnimationView.STATE_LISTENING, "Listening...");
            }

            @Override
            public void onBeginningOfSpeech() {}

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
                setAiState(AiAnimationView.STATE_THINKING, "Analyzing voice patterns...");
            }

            @Override
            public void onError(int error) {
                isListening = false;
                setAiState(AiAnimationView.STATE_IDLE, "AI is sleeping");
                Toast.makeText(MainActivity.this, "Speech recognition error " + error, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResults(Bundle results) {
                isListening = false;
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String spokenText = matches.get(0);
                    edtTxtUserInput.setText(spokenText);
                    processUserTextSubmit();
                } else {
                    setAiState(AiAnimationView.STATE_IDLE, "AI is sleeping");
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void toggleVoiceRecording() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_RECORD_AUDIO);
            return;
        }

        if (isListening) {
            speechRecognizer.stopListening();
            isListening = false;
            setAiState(AiAnimationView.STATE_IDLE, "AI is sleeping");
        } else {
            speechRecognizer.startListening(speechRecognizerIntent);
            isListening = true;
        }
    }

    private void setAiState(int state, String description) {
        aiAnimationIndicatorView.setUiState(state);
        txtAIStatusState.setText(description);
    }

    private void setupNavigationTabs() {
        btnTabChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(0);
            }
        });
        btnTabAdmin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(1);
            }
        });
        btnTabSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(2);
            }
        });
    }

    private void switchTab(int tabIndex) {
        tabLayoutChat.setVisibility(tabIndex == 0 ? View.VISIBLE : View.GONE);
        tabLayoutAdmin.setVisibility(tabIndex == 1 ? View.VISIBLE : View.GONE);
        tabLayoutSettings.setVisibility(tabIndex == 2 ? View.VISIBLE : View.GONE);

        btnTabChat.setTextColor(tabIndex == 0 ? 0xFF008080 : 0xFF78909C);
        btnTabAdmin.setTextColor(tabIndex == 1 ? 0xFF008080 : 0xFF78909C);
        btnTabSettings.setTextColor(tabIndex == 2 ? 0xFF008080 : 0xFF78909C);

        btnTabChat.setTypeface(null, tabIndex == 0 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        btnTabAdmin.setTypeface(null, tabIndex == 1 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        btnTabSettings.setTypeface(null, tabIndex == 2 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

        if (tabIndex == 1) {
            fetchAdminAppointmentsList();
        }
    }

    private void processUserTextSubmit() {
        String userText = edtTxtUserInput.getText().toString().trim();
        if (userText.isEmpty()) return;

        if (apiKey.isEmpty()) {
            Toast.makeText(this, "Please set your Gemini API Key in Settings first!", Toast.LENGTH_SHORT).show();
            return;
        }

        addChatBubble("You", userText, true);
        edtTxtUserInput.setText("");
        sendRequestToGemini(userText);
    }

    private void addChatBubble(String sender, String text, boolean scroll) {
        LinearLayout containerBubble = new LinearLayout(this);
        containerBubble.setOrientation(LinearLayout.VERTICAL);
        containerBubble.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, 
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layoutParams.topMargin = 8;
        layoutParams.bottomMargin = 8;

        TextView senderLabel = new TextView(this);
        senderLabel.setText(sender);
        senderLabel.setTextSize(10sp);
        senderLabel.setTextColor(0xFF78909C);

        TextView messageBody = new TextView(this);
        messageBody.setText(text);
        messageBody.setTextColor(sender.equals("You") ? Color.WHITE : 0xFF263238);
        messageBody.setTextSize(14sp);
        messageBody.setPadding(12, 10, 12, 10);

        if (sender.equals("You")) {
            containerBubble.setGravity(Gravity.END);
            layoutParams.gravity = Gravity.END;
            layoutParams.leftMargin = 50;
            messageBody.setBackgroundResource(R.drawable.bg_bubble_user);
            senderLabel.setGravity(Gravity.END);
        } else {
            containerBubble.setGravity(Gravity.START);
            layoutParams.gravity = Gravity.START;
            layoutParams.rightMargin = 50;
            messageBody.setBackgroundResource(R.drawable.bg_bubble_ai);
            senderLabel.setGravity(Gravity.START);
        }

        containerBubble.addView(senderLabel);
        containerBubble.addView(messageBody, layoutParams);
        layoutMessageBubbles.addView(containerBubble);

        if (scroll) {
            scrollChatView.post(new Runnable() {
                @Override
                public void run() {
                    scrollChatView.fullScroll(View.FOCUS_DOWN);
                }
            });
        }
    }

    private void sendRequestToGemini(final String promptText) {
        setAiState(AiAnimationView.STATE_THINKING, "AI is thinking...");
        connectionStatusText.setText("Connecting...");
        connectionStatusText.setTextColor(0xFFFFD54F);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Formulate JSON request
                    JSONObject requestObject = new JSONObject();
                    JSONArray contentsArray = new JSONArray();
                    JSONObject partMessage = new JSONObject();
                    partMessage.put("role", "user");
                    
                    JSONArray partsArray = new JSONArray();
                    JSONObject textPayload = new JSONObject();

                    // Construct rich system metadata template to force JSON behavior
                    String compiledPrompt = "System Instructions:\n" + systemPrompt 
                        + "\n\nCurrent Confirmed/Stored State Metadata (Do not re-ask these):\n" + appointmentMetadata.toString()
                        + "\n\nUser Message: \"" + promptText + "\""
                        + "\n\nRemember to strictly update and return ONLY the JSON representation described.";

                    textPayload.put("text", compiledPrompt);
                    partsArray.put(textPayload);
                    partMessage.put("parts", partsArray);
                    contentsArray.put(partMessage);
                    requestObject.put("contents", contentsArray);

                    // Enforce structured output configuration parameters
                    JSONObject generationConfig = new JSONObject();
                    generationConfig.put("responseMimeType", "application/json");
                    requestObject.put("generationConfig", generationConfig);

                    // Establish connection
                    URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setDoOutput(true);
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(15000);

                    OutputStream outputStream = connection.getOutputStream();
                    outputStream.write(requestObject.toString().getBytes("UTF-8"));
                    outputStream.close();

                    int code = connection.getResponseCode();
                    if (code == 200) {
                        InputStream inputStream = connection.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                        reader.close();
                        
                        final String resultString = sb.toString();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                handleGeminiApiResponse(resultString);
                            }
                        });
                    } else {
                        final int errCode = code;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                handleNetworkError("Gemini API returned code: " + errCode);
                            }
                        });
                    }
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            handleNetworkError("Connection failure: " + e.getMessage());
                        }
                    });
                }
            } 
        }).start();
    }

    private void handleGeminiApiResponse(String rawJson) {
        try {
            connectionStatusText.setText("Online");
            connectionStatusText.setTextColor(0xFFA7FFEB);

            JSONObject rawObj = new JSONObject(rawJson);
            JSONArray candidates = rawObj.getJSONArray("candidates");
            JSONObject candidate = candidates.getJSONObject(0);
            JSONObject content = candidate.getJSONObject("content");
            JSONArray parts = content.getJSONArray("parts");
            String innerText = parts.getJSONObject(0).getString("text");

            // Parse Gemini nested state model response
            JSONObject responseJson = new JSONObject(innerText.trim());
            String replyText = responseJson.getString("reply");
            
            // Capture and compare new values
            JSONObject receivedMeta = responseJson.optJSONObject("metadata");
            if (receivedMeta != null) {
                updateSavedMetadata(receivedMeta);
            }

            boolean isReadyToConfirm = responseJson.optBoolean("is_ready_to_confirm", false);
            boolean isConfirmed = responseJson.optBoolean("is_confirmed", false);

            // Output bubble updates
            addChatBubble("AI Assistant", replyText, true);
            speakTextVoice(replyText);

            // Update indicators visual UI
            updateMetadataIndicatorUi();

            if (isReadyToConfirm) {
                layoutConfirmBookingContainer.setVisibility(View.VISIBLE);
                badgeStatus.setText("📋 Ready");
                badgeStatus.setBackgroundColor(0xFFE0F2F1);
                badgeStatus.setTextColor(0xFF00796B);
            } else {
                layoutConfirmBookingContainer.setVisibility(View.GONE);
            }

            if (isConfirmed) {
                // Directly initiate secured insertion flow to SQL backend
                bookAppointmentOnBackend();
            }

        } catch (Exception e) {
            e.printStackTrace();
            addChatBubble("System Error", "Failed parsing internal response payload: " + e.getMessage(), true);
            setAiState(AiAnimationView.STATE_IDLE, "AI is sleeping");
        }
    }

    private void updateSavedMetadata(JSONObject newMeta) {
        String[] keys = {"name", "age", "phone", "department", "doctor", "date", "time"};
        for (String key : keys) {
            String val = newMeta.optString(key, "").trim();
            if (!val.isEmpty() && !val.equals("null")) {
                try {
                    appointmentMetadata.put(key, val);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void updateMetadataIndicatorUi() {
        badgeName.setText("👤 Name: " + getMetaOrPlaceholder("name"));
        badgeAge.setText("🎂 Age: " + getMetaOrPlaceholder("age"));
        badgePhone.setText("📞 Phone: " + getMetaOrPlaceholder("phone"));
        badgeDept.setText("🏥 Dept: " + getMetaOrPlaceholder("department"));
        badgeDoctor.setText("🩺 Doctor: " + getMetaOrPlaceholder("doctor"));
        badgeDate.setText("📅 Date: " + getMetaOrPlaceholder("date"));
        badgeTime.setText("⏰ Time: " + getMetaOrPlaceholder("time"));
        
        boolean fullyCompleted = true;
        String[] keys = {"name", "age", "phone", "department", "doctor", "date", "time"};
        for (String key : keys) {
            if (getMetaOrPlaceholder(key).equals("--")) {
                fullyCompleted = false;
                break;
            }
        }

        if (fullyCompleted) {
            badgeStatus.setText("✓ Complete");
            badgeStatus.setBackgroundColor(0xFFE8F5E9);
            badgeStatus.setTextColor(0xFF2E7D32);
        } else {
            badgeStatus.setText("⚠️ Unfinished");
            badgeStatus.setBackgroundColor(0xFFFFEBEE);
            badgeStatus.setTextColor(0xFFC62828);
        }
    }

    private String getMetaOrPlaceholder(String key) {
        String val = appointmentMetadata.optString(key, "").trim();
        return (val.isEmpty() || val.equals("null")) ? "--" : val;
    }

    private void speakTextVoice(String text) {
        if (ttsEngine != null) {
            ttsEngine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "AI_ASSISTANT_REPLY");
        }
    }

    private void handleNetworkError(String errorMessage) {
        connectionStatusText.setText("Offline");
        connectionStatusText.setTextColor(0xFFFF8A80);
        addChatBubble("System Error", "Network transaction failed: " + errorMessage, true);
        setAiState(AiAnimationView.STATE_IDLE, "AI is sleeping");
    }

    private void bookAppointmentOnBackend() {
        if (backendUrl.isEmpty()) {
            Toast.makeText(this, "Please set up your database PHP link inside Settings!", Toast.LENGTH_LONG).show();
            addChatBubble("System Config Check", "Booking failed: No backend PHP URL provided in settings.", true);
            return;
        }

        setAiState(AiAnimationView.STATE_THINKING, "Transmitting transaction block to hospital database...");
        addChatBubble("System", "Sending appointment request safely to DB system...", true);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(backendUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setDoOutput(true);
                    connection.setConnectTimeout(8000);
                    connection.setReadTimeout(12000);

                    OutputStream os = connection.getOutputStream();
                    os.write(appointmentMetadata.toString().getBytes("UTF-8"));
                    os.close();

                    int code = connection.getResponseCode();
                    if (code == 200) {
                        InputStream is = connection.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                        reader.close();
                        
                        final String dbResult = sb.toString();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                handleBackendBookingSuccess(dbResult);
                            }
                        });
                    } else {
                        final int errCode = code;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                handleBackendBookingError("HTTP code response error: " + errCode);
                            }
                        });
                    }
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            handleBackendBookingError(e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    private void handleBackendBookingSuccess(String rawResponse) {
        setAiState(AiAnimationView.STATE_IDLE, "AI is sleeping");
        try {
            JSONObject response = new JSONObject(rawResponse);
            String status = response.optString("status", "error");
            String message = response.optString("message", "");

            if (status.equals("success")) {
                String bookingId = response.optString("booking_id", "N/A");
                String successMsg = "🎉 Booking Confirmed! ID: " + bookingId + ". " + message;
                addChatBubble("Hospital System", successMsg, true);
                speakTextVoice(successMsg);
                
                // Successfully registered. Reset state memory to start fresh
                resetStateMetadata();
                updateMetadataIndicatorUi();
                layoutConfirmBookingContainer.setVisibility(View.GONE);
            } else {
                addChatBubble("Hospital System Alert", "Failed to book: " + message, true);
            }
        } catch (Exception e) {
            addChatBubble("System Error", "Received non-JSON response from PHP: " + rawResponse, true);
        }
    }

    private void handleBackendBookingError(String message) {
        setAiState(AiAnimationView.STATE_IDLE, "AI is sleeping");
        addChatBubble("System Error", "Failed reaching database transaction portal: " + message, true);
    }

    private void fetchAdminAppointmentsList() {
        if (backendUrl.isEmpty()) {
            txtAdminStatusPlaceholder.setText("Please input PHP endpoint URL inside Settings tab first.");
            return;
        }

        txtAdminStatusPlaceholder.setText("Querying database records...");
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(backendUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(8000);
                    connection.setReadTimeout(12000);

                    int code = connection.getResponseCode();
                    if (code == 200) {
                        InputStream is = connection.getInputStream();
                        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line);
                        }
                        br.close();

                        final String fetchResult = sb.toString();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                displayAdminAppointments(fetchResult);
                            }
                        });
                    } else {
                        final int errCode = code;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                displayAdminError("Server return error code: " + errCode);
                            }
                        });
                    }
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            displayAdminError(e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    private void displayAdminAppointments(String responseStr) {
        try {
            JSONObject mainObj = new JSONObject(responseStr);
            String status = mainObj.optString("status", "error");
            if (status.equals("success")) {
                JSONArray list = mainObj.getJSONArray("data");
                ArrayList<String> formattedItems = new ArrayList<>();
                
                for (int i = 0; i < list.length(); i++) {
                    JSONObject appt = list.getJSONObject(i);
                    String item = "ID: " + appt.getString("id") + " | " + appt.getString("name") + " (Age: " + appt.getString("age") + ")\n"
                            + "Phone: " + appt.getString("phone") + "\n"
                            + "Dept: " + appt.getString("department") + " | Doc: " + appt.getString("doctor") + "\n"
                            + "Schedule: " + appt.getString("date") + " @ " + appt.getString("time");
                    formattedItems.add(item);
                }

                if (formattedItems.isEmpty()) {
                    txtAdminStatusPlaceholder.setText("No booked appointments found inside SQL table.");
                    listAdminBookings.setAdapter(null);
                } else {
                    txtAdminStatusPlaceholder.setText("");
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_list_item_1, formattedItems);
                    listAdminBookings.setAdapter(adapter);
                }
            } else {
                displayAdminError(mainObj.optString("message", "Unknown internal issue."));
            }
        } catch (Exception e) {
            displayAdminError("Non-JSON or corrupt response payload: " + e.getMessage() + "\nResponse: " + responseStr);
        }
    }

    private void displayAdminError(String error) {
        txtAdminStatusPlaceholder.setText("Error fetching items: " + error);
        listAdminBookings.setAdapter(null);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Mic permission granted! Press Record again.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission denied. Voice features disabled.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (ttsEngine != null) {
            ttsEngine.stop();
            ttsEngine.shutdown();
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        super.onDestroy();
    }
}