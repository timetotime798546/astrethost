package com.hospitalaibooker.app;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
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
import android.widget.AdapterView;
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
import java.util.List;
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

    private String apiKey = "";
    private String systemPrompt = "";
    private DatabaseHelper dbHelper;
    private JSONObject appointmentMetadata;
    private long currentSessionId = -1;
    private boolean isReadyToConfirmCurrent = false;
    private boolean isConfirmedCurrent = false;

    private LinearLayout tabLayoutChat;
    private LinearLayout tabLayoutAdmin;
    private ScrollView tabLayoutSettings;
    private Button btnTabChat, btnTabAdmin, btnTabSettings;
    private TextView connectionStatusText;

    private ScrollView scrollChatView;
    private LinearLayout layoutMessageBubbles;
    private EditText edtTxtUserInput;
    private Button btnSendText;
    private ImageButton btnRecordMic;
    private AiAnimationView aiAnimationIndicatorView;
    private TextView txtAIStatusState;
    
    private TextView badgeName, badgeAge, badgePhone, badgeDept, badgeDoctor, badgeDate, badgeTime, badgeStatus;
    private LinearLayout layoutConfirmBookingContainer;
    private Button btnConfirmDirect, btnResetTracker, btnNewChat;

    private ListView listAdminBookings;
    private Button btnRefreshBookings;
    private TextView txtAdminStatusPlaceholder;
    private EditText edtAdminSearch;
    private Button btnAdminSearch;

    private EditText edtSettingsApiKey, edtSettingsSystemInstruction;
    private Button btnSaveSettings;

    private TextToSpeech ttsEngine;
    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;
    private boolean isListening = false; 

    public static class DatabaseHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "HospitalAppointments.db";
        private static final int DATABASE_VERSION = 2;

        // Confirmed Appointments Table
        public static final String TABLE_NAME = "appointments";
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_AGE = "age";
        public static final String COLUMN_PHONE = "phone";
        public static final String COLUMN_DEPARTMENT = "department";
        public static final String COLUMN_DOCTOR = "doctor";
        public static final String COLUMN_DATE = "date";
        public static final String COLUMN_TIME = "time";
        public static final String COLUMN_STATUS = "status";
        public static final String COLUMN_CREATED_AT = "created_at";

        // Chat Sessions Table
        public static final String TABLE_SESSIONS = "conversation_sessions";
        public static final String COLUMN_SESS_ID = "id";
        public static final String COLUMN_SESS_CREATED = "created_at";
        public static final String COLUMN_SESS_UPDATED = "updated_at";
        public static final String COLUMN_SESS_STATUS = "status";

        // Chat Messages Table
        public static final String TABLE_MESSAGES = "chat_messages";
        public static final String COLUMN_MSG_ID = "id";
        public static final String COLUMN_MSG_SESS_ID = "session_id";
        public static final String COLUMN_MSG_ROLE = "role";
        public static final String COLUMN_MSG_TEXT = "message";
        public static final String COLUMN_MSG_CREATED = "created_at";

        // Appointment State Metadata Table
        public static final String TABLE_METADATA = "appointment_metadata";
        public static final String COLUMN_META_ID = "id";
        public static final String COLUMN_META_SESS_ID = "session_id";
        public static final String COLUMN_META_NAME = "name";
        public static final String COLUMN_META_AGE = "age";
        public static final String COLUMN_META_PHONE = "phone";
        public static final String COLUMN_META_DEPT = "department";
        public static final String COLUMN_META_DOCTOR = "doctor";
        public static final String COLUMN_META_DATE = "date";
        public static final String COLUMN_META_TIME = "time";
        public static final String COLUMN_META_READY = "is_ready_to_confirm";
        public static final String COLUMN_META_CONFIRMED = "is_confirmed";
        public static final String COLUMN_META_UPDATED = "updated_at";

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            // Base Confirmed table
            String createQuery = "CREATE TABLE " + TABLE_NAME + " (" + 
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_NAME + " TEXT, " +
                    COLUMN_AGE + " TEXT, " +
                    COLUMN_PHONE + " TEXT, " +
                    COLUMN_DEPARTMENT + " TEXT, " +
                    COLUMN_DOCTOR + " TEXT, " +
                    COLUMN_DATE + " TEXT, " +
                    COLUMN_TIME + " TEXT, " +
                    COLUMN_STATUS + " TEXT, " +
                    COLUMN_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP)";
            db.execSQL(createQuery);

            // Sessions table
            String createSessions = "CREATE TABLE " + TABLE_SESSIONS + " (" + 
                    COLUMN_SESS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_SESS_CREATED + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    COLUMN_SESS_UPDATED + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    COLUMN_SESS_STATUS + " TEXT)";
            db.execSQL(createSessions);

            // Messages table
            String createMessages = "CREATE TABLE " + TABLE_MESSAGES + " (" + 
                    COLUMN_MSG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_MSG_SESS_ID + " INTEGER, " +
                    COLUMN_MSG_ROLE + " TEXT, " +
                    COLUMN_MSG_TEXT + " TEXT, " +
                    COLUMN_MSG_CREATED + " DATETIME DEFAULT CURRENT_TIMESTAMP)";
            db.execSQL(createMessages);

            // Metadata state table
            String createMetadata = "CREATE TABLE " + TABLE_METADATA + " (" + 
                    COLUMN_META_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_META_SESS_ID + " INTEGER, " +
                    COLUMN_META_NAME + " TEXT, " +
                    COLUMN_META_AGE + " TEXT, " +
                    COLUMN_META_PHONE + " TEXT, " +
                    COLUMN_META_DEPT + " TEXT, " +
                    COLUMN_META_DOCTOR + " TEXT, " +
                    COLUMN_META_DATE + " TEXT, " +
                    COLUMN_META_TIME + " TEXT, " +
                    COLUMN_META_READY + " INTEGER DEFAULT 0, " +
                    COLUMN_META_CONFIRMED + " INTEGER DEFAULT 0, " +
                    COLUMN_META_UPDATED + " DATETIME DEFAULT CURRENT_TIMESTAMP)";
            db.execSQL(createMetadata);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion < 2) {
                // Create the tables added in version 2
                String createSessions = "CREATE TABLE " + TABLE_SESSIONS + " (" + 
                        COLUMN_SESS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_SESS_CREATED + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                        COLUMN_SESS_UPDATED + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                        COLUMN_SESS_STATUS + " TEXT)";
                db.execSQL(createSessions);

                String createMessages = "CREATE TABLE " + TABLE_MESSAGES + " (" + 
                        COLUMN_MSG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_MSG_SESS_ID + " INTEGER, " +
                        COLUMN_MSG_ROLE + " TEXT, " +
                        COLUMN_MSG_TEXT + " TEXT, " +
                        COLUMN_MSG_CREATED + " DATETIME DEFAULT CURRENT_TIMESTAMP)";
                db.execSQL(createMessages);

                String createMetadata = "CREATE TABLE " + TABLE_METADATA + " (" + 
                        COLUMN_META_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_META_SESS_ID + " INTEGER, " +
                        COLUMN_META_NAME + " TEXT, " +
                        COLUMN_META_AGE + " TEXT, " +
                        COLUMN_META_PHONE + " TEXT, " +
                        COLUMN_META_DEPT + " TEXT, " +
                        COLUMN_META_DOCTOR + " TEXT, " +
                        COLUMN_META_DATE + " TEXT, " +
                        COLUMN_META_TIME + " TEXT, " +
                        COLUMN_META_READY + " INTEGER DEFAULT 0, " +
                        COLUMN_META_CONFIRMED + " INTEGER DEFAULT 0, " +
                        COLUMN_META_UPDATED + " DATETIME DEFAULT CURRENT_TIMESTAMP)";
                db.execSQL(createMetadata);
            }
        }
    }

    private static class AppointmentModel {
        int id;
        String name;
        String age;
        String phone;
        String department;
        String doctor;
        String date;
        String time;
        String status;
        String createdAt;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) { 
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        resetStateMetadata();
        initUiElements();
        loadStoredSettings();
        setupVoiceAssistant();
        setupNavigationTabs();

        // Load latest session or create new
        restoreOrCreateSession();
    }

    private void initUiElements() {
        tabLayoutChat = findViewById(R.id.tabLayoutChat);
        tabLayoutAdmin = findViewById(R.id.tabLayoutAdmin);
        tabLayoutSettings = findViewById(R.id.tabLayoutSettings);

        btnTabChat = findViewById(R.id.btnTabChat);
        btnTabAdmin = findViewById(R.id.btnTabAdmin);
        btnTabSettings = findViewById(R.id.btnTabSettings);
        connectionStatusText = findViewById(R.id.connectionStatusText);

        scrollChatView = findViewById(R.id.scrollChatView);
        layoutMessageBubbles = findViewById(R.id.layoutMessageBubbles);
        edtTxtUserInput = findViewById(R.id.edtTxtUserInput);
        btnSendText = findViewById(R.id.btnSendText);
        btnRecordMic = findViewById(R.id.btnRecordMic);
        aiAnimationIndicatorView = findViewById(R.id.aiAnimationIndicatorView);
        txtAIStatusState = findViewById(R.id.txtAIStatusState);
        btnNewChat = findViewById(R.id.btnNewChat);

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

        listAdminBookings = findViewById(R.id.listAdminBookings);
        btnRefreshBookings = findViewById(R.id.btnRefreshBookings);
        txtAdminStatusPlaceholder = findViewById(R.id.txtAdminStatusPlaceholder);
        edtAdminSearch = findViewById(R.id.edtAdminSearch);
        btnAdminSearch = findViewById(R.id.btnAdminSearch);

        edtSettingsApiKey = findViewById(R.id.edtSettingsApiKey);
        edtSettingsSystemInstruction = findViewById(R.id.edtSettingsSystemInstruction);
        btnSaveSettings = findViewById(R.id.btnSaveSettings);

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
                bookAppointmentInSQLite();
            } 
        });

        btnResetTracker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetStateMetadata();
                isReadyToConfirmCurrent = false;
                isConfirmedCurrent = false;
                saveMetadataToDb();
                updateMetadataIndicatorUi();
                addChatBubble("System", "Appointment tracking state has been cleared.", true);
                saveMessageToDb("System", "Appointment tracking state has been cleared.");
            }
        });

        btnNewChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startNewSession();
                addChatBubble("System", "Started a new conversation session.", true);
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
                edtAdminSearch.setText("");
                fetchAdminAppointmentsList("");
            }
        });

        btnAdminSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = edtAdminSearch.getText().toString().trim();
                fetchAdminAppointmentsList(query);
            } 
        });

        listAdminBookings.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AppointmentModel appt = (AppointmentModel) parent.getItemAtPosition(position);
                showAppointmentDetailsDialog(appt);
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

    private void restoreOrCreateSession() {
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.query(
                    DatabaseHelper.TABLE_SESSIONS,
                    null,
                    null, null, null, null,
                    DatabaseHelper.COLUMN_SESS_ID + " DESC",
                    "1"
            );
            if (cursor.moveToFirst()) {
                currentSessionId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SESS_ID));
                cursor.close();
                
                // Clear and reload layout
                layoutMessageBubbles.removeAllViews();
                
                Cursor msgCursor = db.query(
                        DatabaseHelper.TABLE_MESSAGES,
                        null,
                        DatabaseHelper.COLUMN_MSG_SESS_ID + " = ?",
                        new String[]{String.valueOf(currentSessionId)},
                        null, null, DatabaseHelper.COLUMN_MSG_ID + " ASC"
                );
                while (msgCursor.moveToNext()) {
                    String role = msgCursor.getString(msgCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MSG_ROLE));
                    String text = msgCursor.getString(msgCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MSG_TEXT));
                    addChatBubble(role, text, false);
                }
                msgCursor.close();

                loadMetadataFromDb();
                updateMetadataIndicatorUi();
                
                scrollChatView.post(new Runnable() {
                    @Override
                    public void run() {
                        scrollChatView.fullScroll(View.FOCUS_DOWN);
                    }
                });
            } else {
                cursor.close();
                startNewSession();
            }
        } catch (Exception e) {
            e.printStackTrace();
            startNewSession();
        }
    }

    private void startNewSession() {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COLUMN_SESS_STATUS, "active");
            currentSessionId = db.insert(DatabaseHelper.TABLE_SESSIONS, null, values);
            
            resetStateMetadata();
            isReadyToConfirmCurrent = false;
            isConfirmedCurrent = false;
            
            ContentValues metaValues = new ContentValues();
            metaValues.put(DatabaseHelper.COLUMN_META_SESS_ID, currentSessionId);
            metaValues.put(DatabaseHelper.COLUMN_META_NAME, "");
            metaValues.put(DatabaseHelper.COLUMN_META_AGE, "");
            metaValues.put(DatabaseHelper.COLUMN_META_PHONE, "");
            metaValues.put(DatabaseHelper.COLUMN_META_DEPT, "");
            metaValues.put(DatabaseHelper.COLUMN_META_DOCTOR, "");
            metaValues.put(DatabaseHelper.COLUMN_META_DATE, "");
            metaValues.put(DatabaseHelper.COLUMN_META_TIME, "");
            metaValues.put(DatabaseHelper.COLUMN_META_READY, 0);
            metaValues.put(DatabaseHelper.COLUMN_META_CONFIRMED, 0);
            db.insert(DatabaseHelper.TABLE_METADATA, null, metaValues);

            layoutMessageBubbles.removeAllViews();
            
            String greeting = "Hello! I am your virtual hospital coordinator. Please tell me your name, and which department or doctor you would like to visit today so I can book your appointment!";
            addChatBubble("AI Assistant", greeting, true);
            saveMessageToDb("AI Assistant", greeting);
            
            updateMetadataIndicatorUi();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveMessageToDb(String role, String message) {
        if (currentSessionId == -1) return;
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COLUMN_MSG_SESS_ID, currentSessionId);
            values.put(DatabaseHelper.COLUMN_MSG_ROLE, role);
            values.put(DatabaseHelper.COLUMN_MSG_TEXT, message);
            db.insert(DatabaseHelper.TABLE_MESSAGES, null, values);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveMetadataToDb() {
        if (currentSessionId == -1) return;
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COLUMN_META_NAME, getMetaValueOrEmpty("name"));
            values.put(DatabaseHelper.COLUMN_META_AGE, getMetaValueOrEmpty("age"));
            values.put(DatabaseHelper.COLUMN_META_PHONE, getMetaValueOrEmpty("phone"));
            values.put(DatabaseHelper.COLUMN_META_DEPT, getMetaValueOrEmpty("department"));
            values.put(DatabaseHelper.COLUMN_META_DOCTOR, getMetaValueOrEmpty("doctor"));
            values.put(DatabaseHelper.COLUMN_META_DATE, getMetaValueOrEmpty("date"));
            values.put(DatabaseHelper.COLUMN_META_TIME, getMetaValueOrEmpty("time"));
            values.put(DatabaseHelper.COLUMN_META_READY, isReadyToConfirmCurrent ? 1 : 0);
            values.put(DatabaseHelper.COLUMN_META_CONFIRMED, isConfirmedCurrent ? 1 : 0);

            int rows = db.update(DatabaseHelper.TABLE_METADATA, values, DatabaseHelper.COLUMN_META_SESS_ID + " = ?", new String[]{String.valueOf(currentSessionId)});
            if (rows == 0) {
                values.put(DatabaseHelper.COLUMN_META_SESS_ID, currentSessionId);
                db.insert(DatabaseHelper.TABLE_METADATA, null, values);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadMetadataFromDb() {
        if (currentSessionId == -1) return;
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.query(
                    DatabaseHelper.TABLE_METADATA,
                    null,
                    DatabaseHelper.COLUMN_META_SESS_ID + " = ?",
                    new String[]{String.valueOf(currentSessionId)},
                    null, null, null
            );
            if (cursor.moveToFirst()) {
                appointmentMetadata.put("name", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_META_NAME)));
                appointmentMetadata.put("age", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_META_AGE)));
                appointmentMetadata.put("phone", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_META_PHONE)));
                appointmentMetadata.put("department", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_META_DEPT)));
                appointmentMetadata.put("doctor", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_META_DOCTOR)));
                appointmentMetadata.put("date", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_META_DATE)));
                appointmentMetadata.put("time", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_META_TIME)));
                
                isReadyToConfirmCurrent = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_META_READY)) == 1;
                isConfirmedCurrent = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_META_CONFIRMED)) == 1;
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getChatHistoryContext() {
        StringBuilder sb = new StringBuilder();
        if (currentSessionId == -1) return "";
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.query(
                    DatabaseHelper.TABLE_MESSAGES,
                    null,
                    DatabaseHelper.COLUMN_MSG_SESS_ID + " = ?",
                    new String[]{String.valueOf(currentSessionId)},
                    null, null, DatabaseHelper.COLUMN_MSG_ID + " ASC"
            );
            int startPos = Math.max(0, cursor.getCount() - 20);
            if (cursor.moveToPosition(startPos)) {
                do {
                    String role = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MSG_ROLE));
                    String text = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MSG_TEXT));
                    sb.append(role).append(": ").append(text).append("\n");
                } while (cursor.moveToNext());
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    private void loadStoredSettings() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        apiKey = preferences.getString("apiKey", "");
        systemPrompt = preferences.getString("systemPrompt", DEFAULT_SYSTEM_PROMPT);

        edtSettingsApiKey.setText(apiKey);
        edtSettingsSystemInstruction.setText(systemPrompt);

        if (apiKey.isEmpty()) {
            Toast.makeText(this, "Please head to Settings to save your Gemini API Key!", Toast.LENGTH_LONG).show();
        }
    }

    private void saveConfigurationSettings() {
        apiKey = edtSettingsApiKey.getText().toString().trim();
        systemPrompt = edtSettingsSystemInstruction.getText().toString().trim();

        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putString("apiKey", apiKey);
        editor.putString("systemPrompt", systemPrompt);
        editor.apply();

        Toast.makeText(this, "Settings configuration saved successfully!", Toast.LENGTH_SHORT).show();
        restoreOrCreateSession();
    }

    private void setupVoiceAssistant() {
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
                setAiState(AiAnimationView.STATE_THINKING, "Analyzing voice...");
            }

            @Override
            public void onError(int error) {
                isListening = false;
                setAiState(AiAnimationView.STATE_IDLE, "AI is sleeping");
                Toast.makeText(MainActivity.this, "Speech recognition issue: " + error, Toast.LENGTH_SHORT).show();
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
            fetchAdminAppointmentsList("");
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
        saveMessageToDb("You", userText);
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
        senderLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        senderLabel.setTextColor(0xFF78909C); 

        TextView messageBody = new TextView(this);
        messageBody.setText(text);
        messageBody.setTextColor(sender.equals("You") ? Color.WHITE : 0xFF263238);
        messageBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
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
                    JSONObject requestObject = new JSONObject();
                    JSONArray contentsArray = new JSONArray();
                    JSONObject partMessage = new JSONObject();
                    partMessage.put("role", "user");
                    
                    JSONArray partsArray = new JSONArray();
                    JSONObject textPayload = new JSONObject();

                    String chatHistory = getChatHistoryContext();
                    String compiledPrompt = "System Instructions:\n" + systemPrompt 
                        + "\n\nCurrent Stored State Metadata (Maintain conversational context & do not ask for these again):\n" + appointmentMetadata.toString()
                        + "\n\nRecent Chat History:\n" + chatHistory
                        + "\nUser Message: \"" + promptText + "\""
                        + "\n\nRemember to strictly update and return ONLY the JSON representation described.";

                    textPayload.put("text", compiledPrompt);
                    partsArray.put(textPayload);
                    partMessage.put("parts", partsArray);
                    contentsArray.put(partMessage);
                    requestObject.put("contents", contentsArray);

                    JSONObject generationConfig = new JSONObject();
                    generationConfig.put("responseMimeType", "application/json");
                    requestObject.put("generationConfig", generationConfig);

                    URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey);
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
                                handleNetworkError("Gemini API status: " + errCode);
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

            JSONObject responseJson = new JSONObject(innerText.trim());
            String replyText = responseJson.getString("reply");
            
            JSONObject receivedMeta = responseJson.optJSONObject("metadata");
            if (receivedMeta != null) {
                updateSavedMetadata(receivedMeta);
            }

            isReadyToConfirmCurrent = responseJson.optBoolean("is_ready_to_confirm", false);
            isConfirmedCurrent = responseJson.optBoolean("is_confirmed", false);

            saveMetadataToDb();

            addChatBubble("AI Assistant", replyText, true);
            saveMessageToDb("AI Assistant", replyText);
            speakTextVoice(replyText);
            updateMetadataIndicatorUi();

            if (isConfirmedCurrent) {
                bookAppointmentInSQLite();
            } else {
                setAiState(AiAnimationView.STATE_IDLE, "AI is sleeping");
            }
        } catch (Exception e) {
            e.printStackTrace();
            addChatBubble("System Error", "Failed parsing response payload: " + e.getMessage(), true);
            setAiState(AiAnimationView.STATE_IDLE, "AI is sleeping");
        }
    }

    private void updateSavedMetadata(JSONObject newMeta) {
        String[] keys = {"name", "age", "phone", "department", "doctor", "date", "time"};
        for (String key : keys) {
            String val = newMeta.optString(key, "").trim();
            if (!val.isEmpty() && !val.equalsIgnoreCase("null")) {
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

        if (isConfirmedCurrent) {
            badgeStatus.setText("✓ Confirmed");
            badgeStatus.setBackgroundColor(0xFFE8F5E9);
            badgeStatus.setTextColor(0xFF2E7D32);
            layoutConfirmBookingContainer.setVisibility(View.GONE);
        } else if (fullyCompleted || isReadyToConfirmCurrent) {
            badgeStatus.setText("📋 Ready");
            badgeStatus.setBackgroundColor(0xFFE0F2F1);
            badgeStatus.setTextColor(0xFF00796B);
            layoutConfirmBookingContainer.setVisibility(View.VISIBLE);
        } else {
            badgeStatus.setText("⚠️ Unfinished");
            badgeStatus.setBackgroundColor(0xFFFFEBEE);
            badgeStatus.setTextColor(0xFFC62828);
            layoutConfirmBookingContainer.setVisibility(View.GONE);
        }
    }

    private String getMetaOrPlaceholder(String key) {
        String val = appointmentMetadata.optString(key, "").trim();
        return (val.isEmpty() || val.equalsIgnoreCase("null")) ? "--" : val;
    }

    private String getMetaValueOrEmpty(String key) {
        String val = appointmentMetadata.optString(key, "").trim();
        return (val.equalsIgnoreCase("null")) ? "" : val;
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

    private void bookAppointmentInSQLite() {
        setAiState(AiAnimationView.STATE_THINKING, "Saving appointment locally...");
        addChatBubble("System", "Inserting appointment into local SQLite database...", true);

        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COLUMN_NAME, getMetaValueOrEmpty("name"));
            values.put(DatabaseHelper.COLUMN_AGE, getMetaValueOrEmpty("age"));
            values.put(DatabaseHelper.COLUMN_PHONE, getMetaValueOrEmpty("phone"));
            values.put(DatabaseHelper.COLUMN_DEPARTMENT, getMetaValueOrEmpty("department"));
            values.put(DatabaseHelper.COLUMN_DOCTOR, getMetaValueOrEmpty("doctor"));
            values.put(DatabaseHelper.COLUMN_DATE, getMetaValueOrEmpty("date"));
            values.put(DatabaseHelper.COLUMN_TIME, getMetaValueOrEmpty("time"));
            values.put(DatabaseHelper.COLUMN_STATUS, "Confirmed");

            long newRowId = db.insert(DatabaseHelper.TABLE_NAME, null, values);
            if (newRowId != -1) {
                setAiState(AiAnimationView.STATE_IDLE, "AI is sleeping");
                String successMsg = "🎉 Booking Confirmed! Local ID: " + newRowId + ". Appointment successfully registered in SQLite database.";
                addChatBubble("Hospital System", successMsg, true);
                saveMessageToDb("Hospital System", successMsg);
                speakTextVoice(successMsg);

                isConfirmedCurrent = true;
                isReadyToConfirmCurrent = false;
                saveMetadataToDb();

                // Set session to completed
                ContentValues sessValues = new ContentValues();
                sessValues.put(DatabaseHelper.COLUMN_SESS_STATUS, "completed");
                db.update(DatabaseHelper.TABLE_SESSIONS, sessValues, DatabaseHelper.COLUMN_SESS_ID + " = ?", new String[]{String.valueOf(currentSessionId)});

                updateMetadataIndicatorUi();
                layoutConfirmBookingContainer.setVisibility(View.GONE);
            } else {
                handleLocalBookingError("SQLite insert transaction failed.");
            }
        } catch (Exception e) {
            handleLocalBookingError(e.getMessage());
        }
    }

    private void handleLocalBookingError(String message) {
        setAiState(AiAnimationView.STATE_IDLE, "AI is sleeping");
        addChatBubble("System Error", "Failed writing appointment locally: " + message, true);
    }

    private void fetchAdminAppointmentsList(String query) {
        txtAdminStatusPlaceholder.setText("Querying local database...");
        ArrayList<AppointmentModel> listData = new ArrayList<AppointmentModel>();
        
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            String selection = null;
            String[] selectionArgs = null;

            if (query != null && !query.trim().isEmpty()) {
                selection = DatabaseHelper.COLUMN_NAME + " LIKE ? OR " +
                        DatabaseHelper.COLUMN_PHONE + " LIKE ? OR " +
                        DatabaseHelper.COLUMN_DEPARTMENT + " LIKE ?";
                String bindQuery = "%" + query.trim() + "%";
                selectionArgs = new String[]{bindQuery, bindQuery, bindQuery};
            }

            Cursor cursor = db.query(
                    DatabaseHelper.TABLE_NAME,
                    null,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    DatabaseHelper.COLUMN_ID + " DESC"
            );

            while (cursor.moveToNext()) {
                AppointmentModel appt = new AppointmentModel();
                appt.id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID));
                appt.name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME));
                appt.age = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_AGE));
                appt.phone = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PHONE));
                appt.department = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DEPARTMENT));
                appt.doctor = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DOCTOR));
                appt.date = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DATE));
                appt.time = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TIME));
                appt.status = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_STATUS));
                appt.createdAt = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CREATED_AT));
                listData.add(appt);
            }
            cursor.close();

            if (listData.isEmpty()) {
                txtAdminStatusPlaceholder.setText("No booked appointments found inside SQLite table.");
                listAdminBookings.setAdapter(null);
            } else {
                txtAdminStatusPlaceholder.setText("");
                AppointmentListAdapter adapter = new AppointmentListAdapter(this, listData);
                listAdminBookings.setAdapter(adapter);
            }
        } catch (Exception e) {
            txtAdminStatusPlaceholder.setText("Error reading SQLite: " + e.getMessage());
            listAdminBookings.setAdapter(null);
        }
    }

    private void showAppointmentDetailsDialog(final AppointmentModel appt) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Appointment Details");

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(32, 24, 32, 24);

        TextView detailsText = new TextView(this);
        String detailsStr = "ID: " + appt.id + "\n" +
                "Patient: " + appt.name + " (" + appt.age + " y/o)\n" +
                "Phone: " + appt.phone + "\n" +
                "Department: " + appt.department + "\n" +
                "Doctor: " + appt.doctor + "\n" +
                "Date: " + appt.date + " @ " + appt.time + "\n" +
                "Created: " + appt.createdAt + "\n\n" +
                "Current Status: " + appt.status;
        detailsText.setText(detailsStr);
        detailsText.setTextColor(0xFF37474F);
        detailsText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        container.addView(detailsText);

        TextView statusLabel = new TextView(this);
        statusLabel.setText("Update Status:");
        statusLabel.setTextColor(0xFF008080);
        statusLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        statusLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        labelParams.topMargin = 16;
        labelParams.bottomMargin = 8;
        container.addView(statusLabel, labelParams);

        final android.widget.Spinner statusSpinner = new android.widget.Spinner(this);
        final String[] statuses = {"Confirmed", "Completed", "Cancelled", "Rescheduled", "No Show"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, statuses);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(spinnerAdapter);

        for (int i = 0; i < statuses.length; i++) {
            if (statuses[i].equalsIgnoreCase(appt.status)) {
                statusSpinner.setSelection(i);
                break;
            }
        }
        container.addView(statusSpinner);
        builder.setView(container);

        builder.setPositiveButton("Update Status", new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                String selectedStatus = statusSpinner.getSelectedItem().toString();
                updateAppointmentStatus(appt.id, selectedStatus);
            }
        });

        builder.setNegativeButton("Delete", new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                showDeleteConfirmDialog(appt.id);
            }
        });

        builder.setNeutralButton("Close", null);
        builder.show();
    }

    private void updateAppointmentStatus(int id, String newStatus) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COLUMN_STATUS, newStatus);
            int rows = db.update(DatabaseHelper.TABLE_NAME, values, DatabaseHelper.COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
            if (rows > 0) {
                Toast.makeText(this, "Status updated to: " + newStatus, Toast.LENGTH_SHORT).show();
                fetchAdminAppointmentsList("");
            } else {
                Toast.makeText(this, "Failed to update status.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "SQLite Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteConfirmDialog(final int id) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Confirm Delete");
        builder.setMessage("Are you sure you want to delete this appointment? This action cannot be undone.");
        builder.setPositiveButton("Delete", new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                try {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    int rows = db.delete(DatabaseHelper.TABLE_NAME, DatabaseHelper.COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
                    if (rows > 0) {
                        Toast.makeText(MainActivity.this, "Appointment deleted successfully.", Toast.LENGTH_SHORT).show();
                        fetchAdminAppointmentsList("");
                    } else {
                        Toast.makeText(MainActivity.this, "Failed to delete appointment.", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "SQLite Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private static class AppointmentListAdapter extends ArrayAdapter<AppointmentModel> {
        private final Activity context;
        private final List<AppointmentModel> list;

        public AppointmentListAdapter(Activity context, List<AppointmentModel> list) {
            super(context, android.R.layout.simple_list_item_2, list);
            this.context = context;
            this.list = list;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = context.getLayoutInflater().inflate(android.R.layout.simple_list_item_2, parent, false);
            }

            AppointmentModel appt = list.get(position);
            TextView text1 = view.findViewById(android.R.id.text1);
            TextView text2 = view.findViewById(android.R.id.text2);

            text1.setText(appt.name + " (" + appt.age + " y/o) - " + appt.status);
            text1.setTextColor(0xFF263238);
            text1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            text1.setTypeface(null, android.graphics.Typeface.BOLD);

            text2.setText("Dept: " + appt.department + " | Doctor: " + appt.doctor + "\nSchedule: " + appt.date + " @ " + appt.time);
            text2.setTextColor(0xFF546E7A);
            text2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);

            return view;
        }
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
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }
}