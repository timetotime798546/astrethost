package com.voicecontrolassistant.app;

import android.accessibilityservice.AccessibilityService;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.PixelFormat;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.TextView;
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

public class VoiceAccessibilityService extends AccessibilityService implements RecognitionListener {

    private static final String TAG = "VoiceAssistant";
    private static final long SESSION_TIMEOUT_MS = 600000; // 10 minutes

    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;
    private FrameLayout micContainer;
    private TextView micText;
    private TextView statusTextOverlay;
    private View orbGlow;

    private SpeechRecognizer speechRecognizer;
    private boolean isSessionActive = false;
    private long listeningSessionStartTime = 0;

    // TextToSpeech Integration fields
    private TextToSpeech tts;
    private boolean isTtsReady = false;
    private boolean isSpeaking = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Handler sessionTimeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable statusRunnable;
    private ValueAnimator orbAnimator;

    // Sequential Queue structures
    private List<ParsedAction> pendingActions = new ArrayList<>();
    private int currentStepIndex = 0;
    private boolean isExecutingQueue = false;
    private int currentQueueSessionId = 0;

    static class ParsedAction {
        String actionType; // OPEN, CLICK, TYPE, SCROLL_DOWN, SCROLL_UP, BACK, HOME, SUBMIT, VOLUME_UP, VOLUME_DOWN, TORCH_ON, TORCH_OFF, RESTART_PHONE, WAIT
        String originalAction = "";
        String target = "";
        String query = "";
        String rawText = "";

        @Override
        public String toString() {
            return "ParsedAction{" +
                    "actionType='" + actionType + '\'' +
                    ", originalAction='" + originalAction + '\'' +
                    ", target='" + target + '\'' +
                    ", query='" + query + '\'' +
                    '}';
        }
    }

    interface ActionCallback {
        void onSuccess();
        void onFailure(String errorMsg);
    }

    private final Runnable sessionTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            if (isSessionActive) {
                stopListeningSession();
                displayFloatingStatus("Listening timed out");
                setOrbState("error");
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setOrbState("idle");
                    }
                }, 3000);
            }
        }
    };

    private void cancelRunningQueue() {
        currentQueueSessionId++;
        pendingActions.clear();
        currentStepIndex = 0;
        isExecutingQueue = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("COMMAND_TEXT")) {
            final String text = intent.getStringExtra("COMMAND_TEXT");
            Log.d(TAG, "Executing text command from Intent: " + text);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    cancelRunningQueue();
                    processCommandWithGemini(text);
                }
            });
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || 
            eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            CharSequence pkg = event.getPackageName();
            if (pkg != null) {
                Log.d(TAG, "UI transition in: " + pkg.toString());
            }
        }
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        createFloatingMic();
        initializeSpeechRecognizer();
        initializeTextToSpeech();
    }

    private void createFloatingMic() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_mic_layout, null);

        int typeParam;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            typeParam = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
        } else {
            typeParam = WindowManager.LayoutParams.TYPE_PHONE;
        }

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                typeParam,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 100;
        params.y = 300;

        micContainer = (FrameLayout) floatingView.findViewById(R.id.mic_container);
        micText = (TextView) floatingView.findViewById(R.id.mic_text);
        statusTextOverlay = (TextView) floatingView.findViewById(R.id.status_text_overlay);
        orbGlow = floatingView.findViewById(R.id.orb_glow);

        final int touchSlop = ViewConfiguration.get(this).getScaledTouchSlop();

        floatingView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            private boolean isDragging = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        isDragging = false;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float deltaX = event.getRawX() - initialTouchX;
                        float deltaY = event.getRawY() - initialTouchY;

                        if (!isDragging && (Math.abs(deltaX) > touchSlop || Math.abs(deltaY) > touchSlop)) {
                            isDragging = true;
                        }

                        if (isDragging) {
                            params.x = initialX + (int) deltaX;
                            params.y = initialY + (int) deltaY;
                            windowManager.updateViewLayout(floatingView, params);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        float finalDeltaX = Math.abs(event.getRawX() - initialTouchX);
                        float finalDeltaY = Math.abs(event.getRawY() - initialTouchY);
                        if (!isDragging && finalDeltaX < touchSlop && finalDeltaY < touchSlop) {
                            toggleListeningSession();
                        }
                        return true;
                }
                return false;
            }
        });

        windowManager.addView(floatingView, params);
        setOrbState("idle");
    }

    private void initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(this);
        } else {
            displayFloatingStatus("Speech not supported");
        }
    }

    private void initializeTextToSpeech() {
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(Locale.US);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e(TAG, "Language is not supported or missing data");
                    }
                    isTtsReady = true;

                    tts.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {
                            Log.d(TAG, "Assistant started speaking");
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    muteSpeechRecognizer();
                                }
                            });
                        }

                        @Override
                        public void onDone(String utteranceId) {
                            Log.d(TAG, "Assistant finished speaking");
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    unmuteSpeechRecognizerAndResume();
                                }
                            });
                        }

                        @Override
                        public void onError(String utteranceId) {
                            Log.e(TAG, "Assistant speaking error occurred");
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    unmuteSpeechRecognizerAndResume();
                                }
                            });
                        }
                    });
                } else {
                    Log.e(TAG, "TTS Engine initialization failed");
                    isTtsReady = false;
                }
            }
        });
    }

    private void speakText(String text) {
        if (isTtsReady && tts != null) {
            Log.d(TAG, "Assistant Speaking: " + text);
            isSpeaking = true;
            muteSpeechRecognizer();

            Bundle params = new Bundle();
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "AssistantResponse");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, "AssistantResponse");
            } else {
                java.util.HashMap<String, String> map = new java.util.HashMap<>();
                map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "AssistantResponse");
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, map);
            }
        } else {
            Log.e(TAG, "TTS Engine not loaded. Continuing in silent mode.");
        }
    }

    private void muteSpeechRecognizer() {
        isSpeaking = true;
        try {
            if (speechRecognizer != null) {
                speechRecognizer.cancel();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling SpeechRecognizer: " + e.getMessage());
        }
    }

    private void unmuteSpeechRecognizerAndResume() {
        isSpeaking = false;
        if (isSessionActive) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isSessionActive && !isSpeaking) {
                        startSpeechRecognizerListening();
                    }
                }
            }, 600);
        }
    }

    private void toggleListeningSession() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                displayFloatingStatus("Mic permission required");
                return;
            }
        }

        if (isSessionActive) {
            stopListeningSession();
            displayFloatingStatus("Stopped");
            setOrbState("idle");
        } else {
            isSessionActive = true;
            listeningSessionStartTime = System.currentTimeMillis();
            sessionTimeoutHandler.removeCallbacks(sessionTimeoutRunnable);
            sessionTimeoutHandler.postDelayed(sessionTimeoutRunnable, SESSION_TIMEOUT_MS);
            startSpeechRecognizerListening();
        }
    }

    private void startSpeechRecognizerListening() {
        if (isSpeaking) {
            Log.d(TAG, "Preventing speech start because TTS is active.");
            return;
        }
        try {
            if (speechRecognizer != null) {
                speechRecognizer.destroy();
            }
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(this);

            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
            intent.putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", new String[]{"hi-IN"});
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

            speechRecognizer.startListening(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error starting SpeechRecognizer: " + e.getMessage());
        }
    }

    private void stopListeningSession() {
        isSessionActive = false;
        sessionTimeoutHandler.removeCallbacks(sessionTimeoutRunnable);
        try {
            if (speechRecognizer != null) {
                speechRecognizer.stopListening();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping recognizer: " + e.getMessage());
        }
    }

    private void setOrbState(String state) {
        if (orbAnimator != null) {
            orbAnimator.cancel();
        }

        if (orbGlow == null) return;

        if ("idle".equals(state)) {
            orbGlow.setBackgroundResource(R.drawable.circle_default);
            micContainer.setBackgroundResource(R.drawable.circle_default);
            micText.setText("✦");

            orbAnimator = ValueAnimator.ofFloat(1.0f, 1.3f);
            orbAnimator.setDuration(2400);
            orbAnimator.setRepeatMode(ValueAnimator.REVERSE);
            orbAnimator.setRepeatCount(ValueAnimator.INFINITE);
            orbAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float val = (Float) animation.getAnimatedValue();
                    orbGlow.setScaleX(val);
                    orbGlow.setScaleY(val);
                    orbGlow.setAlpha(0.6f - (val - 1.0f) * 1.5f);
                }
            });
            orbAnimator.start();

        } else if ("listening".equals(state)) {
            orbGlow.setBackgroundResource(R.drawable.circle_listening);
            micContainer.setBackgroundResource(R.drawable.circle_listening);
            micText.setText("🎙️");

            orbAnimator = ValueAnimator.ofFloat(1.0f, 1.6f);
            orbAnimator.setDuration(1000);
            orbAnimator.setRepeatMode(ValueAnimator.REVERSE);
            orbAnimator.setRepeatCount(ValueAnimator.INFINITE);
            orbAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float val = (Float) animation.getAnimatedValue();
                    orbGlow.setScaleX(val);
                    orbGlow.setScaleY(val);
                    orbGlow.setAlpha(0.8f - (val - 1.0f) * 1.2f);
                }
            });
            orbAnimator.start();

        } else if ("processing".equals(state)) {
            orbGlow.setBackgroundResource(R.drawable.circle_processing);
            micContainer.setBackgroundResource(R.drawable.circle_processing);
            micText.setText("⚙️");

            orbAnimator = ValueAnimator.ofFloat(1.0f, 1.4f);
            orbAnimator.setDuration(500);
            orbAnimator.setRepeatMode(ValueAnimator.REVERSE);
            orbAnimator.setRepeatCount(ValueAnimator.INFINITE);
            orbAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float val = (Float) animation.getAnimatedValue();
                    orbGlow.setScaleX(val);
                    orbGlow.setScaleY(val);
                    orbGlow.setAlpha(0.9f - (val - 1.0f) * 2.0f);
                }
            });
            orbAnimator.start();

        } else if ("error".equals(state)) {
            orbGlow.setBackgroundResource(R.drawable.circle_error);
            micContainer.setBackgroundResource(R.drawable.circle_error);
            micText.setText("⚠️");

            orbAnimator = ValueAnimator.ofFloat(1.0f, 1.25f);
            orbAnimator.setDuration(350);
            orbAnimator.setRepeatMode(ValueAnimator.REVERSE);
            orbAnimator.setRepeatCount(6);
            orbAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float val = (Float) animation.getAnimatedValue();
                    orbGlow.setScaleX(val);
                    orbGlow.setScaleY(val);
                    orbGlow.setAlpha(0.5f);
                }
            });
            orbAnimator.start();
        }
    }

    private void displayFloatingStatus(String message) {
        if (statusTextOverlay == null) return;
        statusTextOverlay.setVisibility(View.VISIBLE);
        statusTextOverlay.setText(message);

        if (statusRunnable != null) {
            handler.removeCallbacks(statusRunnable);
        }

        statusRunnable = new Runnable() {
            @Override
            public void run() {
                if (statusTextOverlay != null) {
                    statusTextOverlay.setVisibility(View.GONE);
                }
            }
        };
        handler.postDelayed(statusRunnable, 4500);
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
        if (isSessionActive && !isSpeaking) {
            setOrbState("listening");
            displayFloatingStatus("Listening...");
        }
    }

    @Override
    public void onBeginningOfSpeech() {}

    @Override
    public void onRmsChanged(float rmsdB) {}

    @Override
    public void onBufferReceived(byte[] buffer) {}

    @Override
    public void onEndOfSpeech() {}

    @Override
    public void onError(int error) {
        if (isSpeaking) {
            return;
        }
        if (isSessionActive) {
            long elapsed = System.currentTimeMillis() - listeningSessionStartTime;
            if (elapsed < SESSION_TIMEOUT_MS) {
                startSpeechRecognizerListening();
            } else {
                stopListeningSession();
                setOrbState("idle");
                displayFloatingStatus("Timed out");
            }
        } else {
            setOrbState("idle");
        }
    }

    @Override
    public void onResults(Bundle results) {
        if (!isSessionActive) return;

        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) {
            final String recognizedText = matches.get(0);
            displayFloatingStatus("\"" + recognizedText + "\"");

            if (handleConversationalGreetings(recognizedText)) {
                return;
            }

            handler.post(new Runnable() {
                @Override
                public void run() {
                    cancelRunningQueue();
                    processCommandWithGemini(recognizedText);
                }
            });
        } else {
            if (!isSpeaking) {
                startSpeechRecognizerListening();
            }
        }
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        if (isSpeaking) return;
        ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) {
            statusTextOverlay.setVisibility(View.VISIBLE);
            statusTextOverlay.setText(matches.get(0));
        }
    }

    @Override
    public void onEvent(int eventType, Bundle params) {}

    private boolean handleConversationalGreetings(String input) {
        String clean = input.toLowerCase().trim().replaceAll("[.,!?]", "");

        if (clean.equals("hello") || clean.equals("hi") || clean.equals("hey") ||
            clean.equals("hello assistant") || clean.equals("hi assistant")) {
            String resp = "Hello! How can I help you?";
            displayFloatingStatus(resp);
            speakText(resp);
            setOrbState("idle");
            returnOrbToReadyDelayed();
            return true;
        }

        if (clean.equals("good morning")) {
            String resp = "Good morning! How can I help you?";
            displayFloatingStatus(resp);
            speakText(resp);
            setOrbState("idle");
            returnOrbToReadyDelayed();
            return true;
        }

        if (clean.equals("good afternoon")) {
            String resp = "Good afternoon! How can I help you?";
            displayFloatingStatus(resp);
            speakText(resp);
            setOrbState("idle");
            returnOrbToReadyDelayed();
            return true;
        }

        if (clean.equals("good evening")) {
            String resp = "Good evening! How can I help you?";
            displayFloatingStatus(resp);
            speakText(resp);
            setOrbState("idle");
            returnOrbToReadyDelayed();
            return true;
        }

        if (clean.equals("namaste") || clean.equals("नमस्ते")) {
            String resp = "Namaste! Main aapki kya sahayata kar sakti hoon?";
            displayFloatingStatus(resp);
            speakText(resp);
            setOrbState("idle");
            returnOrbToReadyDelayed();
            return true;
        }

        if (clean.contains("hello bhai") || clean.contains("hi bhai")) {
            String resp = "Hello! Talk to me, main kya karun?";
            displayFloatingStatus(resp);
            speakText(resp);
            setOrbState("idle");
            returnOrbToReadyDelayed();
            return true;
        }

        if (clean.contains("kya haal hai") || clean.contains("kya chal raha hai")) {
            String resp = "Main badhiya hoon! Aap kya karna chahte hain?";
            displayFloatingStatus(resp);
            speakText(resp);
            setOrbState("idle");
            returnOrbToReadyDelayed();
            return true;
        }

        if (clean.contains("sun rahe ho") || clean.contains("suno assistant")) {
            String resp = "Haan, main sun raha hoon.";
            displayFloatingStatus(resp);
            speakText(resp);
            setOrbState("idle");
            returnOrbToReadyDelayed();
            return true;
        }

        return false;
    }

    private void processCommandWithGemini(final String recognizedText) {
        final SharedPreferences prefs = getSharedPreferences("VoiceControlPrefs", Context.MODE_PRIVATE);
        final String apiKey = prefs.getString("gemini_api_key", "").trim();
        final String modelPref = prefs.getString("gemini_model", "Gemini 2.5 Flash");
        final String customModel = prefs.getString("gemini_custom_model", "").trim();

        if (apiKey.isEmpty()) {
            displayFloatingStatus("AI Config Missing: Enter API Key");
            setOrbState("error");
            speakText("Please configure your Gemini API Key in Settings first.");
            returnOrbToReadyDelayed();
            return;
        }

        String modelCode = "gemini-2.5-flash";
        if (modelPref.contains("2.5 Pro")) {
            modelCode = "gemini-2.5-pro";
        } else if (modelPref.contains("1.5 Flash")) {
            modelCode = "gemini-1.5-flash";
        } else if (modelPref.contains("1.5 Pro")) {
            modelCode = "gemini-1.5-pro";
        } else if (modelPref.contains("Custom Model") && !customModel.isEmpty()) {
            modelCode = customModel;
        }

        final String finalModel = modelCode;

        setOrbState("processing");
        displayFloatingStatus("Understanding your command...");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String response = makeGeminiCall(apiKey, finalModel, recognizedText);
                    final String cleanedResponse = cleanGeminiResponse(response);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Log.d(TAG, "USER TEXT: " + recognizedText);
                                displayFloatingStatus("Preparing Android actions...");

                                JSONObject json = new JSONObject(cleanedResponse);
                                if (json.has("error") && "UNSUPPORTED_COMMAND".equals(json.getString("error"))) {
                                    throw new IllegalArgumentException("UNSUPPORTED_COMMAND");
                                }

                                if (!json.has("commands")) {
                                    Log.e(TAG, "GEMINI COMMAND ARRAY MISSING");
                                    throw new IllegalArgumentException("Missing commands array");
                                }

                                JSONArray commands = json.getJSONArray("commands");
                                Log.d(TAG, "GEMINI COMMAND COUNT: " + commands.length());

                                final List<ParsedAction> actions = new ArrayList<>();

                                for (int i = 0; i < commands.length(); i++) {
                                    JSONObject cmd = commands.getJSONObject(i);
                                    String actionName = cmd.optString("action", "");

                                    if (actionName.isEmpty()) {
                                        Log.e(TAG, "COMMAND FAILED: INVALID_COMMAND");
                                        continue;
                                    }

                                    ParsedAction parsed = new ParsedAction();
                                    parsed.rawText = recognizedText;
                                    parsed.originalAction = actionName;

                                    boolean isValid = true;

                                    if ("OPEN_APP".equals(actionName)) {
                                        parsed.actionType = "OPEN";
                                        parsed.target = cmd.optString("app", "");
                                        if (parsed.target.isEmpty()) {
                                            Log.e(TAG, "COMMAND FAILED: INVALID_COMMAND");
                                            isValid = false;
                                        }
                                    } else if ("OPEN_CHAT".equals(actionName)) {
                                        parsed.actionType = "CLICK";
                                        parsed.target = cmd.optString("contact", "");
                                        if (parsed.target.isEmpty()) {
                                            Log.e(TAG, "COMMAND FAILED: INVALID_COMMAND");
                                            isValid = false;
                                        }
                                    } else if ("TYPE_MESSAGE".equals(actionName)) {
                                        parsed.actionType = "TYPE";
                                        parsed.query = cmd.optString("text", "");
                                        if (parsed.query.isEmpty()) {
                                            Log.e(TAG, "COMMAND FAILED: INVALID_COMMAND");
                                            isValid = false;
                                        }
                                    } else if ("SEND_MESSAGE".equals(actionName)) {
                                        parsed.actionType = "SUBMIT";
                                    } else if ("TYPE_TEXT".equals(actionName) || "SEARCH".equals(actionName)) {
                                        parsed.actionType = "TYPE";
                                        parsed.query = cmd.optString("text", cmd.optString("query", ""));
                                        if (parsed.query.isEmpty()) {
                                            Log.e(TAG, "COMMAND FAILED: INVALID_COMMAND");
                                            isValid = false;
                                        }
                                    } else if ("SUBMIT".equals(actionName) || "ENTER".equals(actionName)) {
                                        parsed.actionType = "SUBMIT";
                                    } else if ("CLICK".equals(actionName)) {
                                        parsed.actionType = "CLICK";
                                        parsed.target = cmd.optString("target", "");
                                        if (parsed.target.isEmpty()) {
                                            Log.e(TAG, "COMMAND FAILED: INVALID_COMMAND");
                                            isValid = false;
                                        }
                                    } else if ("SCROLL".equals(actionName)) {
                                        String dir = cmd.optString("direction", "DOWN").toUpperCase();
                                        parsed.actionType = "UP".equals(dir) ? "SCROLL_UP" : "SCROLL_DOWN";
                                    } else if ("BACK".equals(actionName)) {
                                        parsed.actionType = "BACK";
                                    } else if ("HOME".equals(actionName)) {
                                        parsed.actionType = "HOME";
                                    } else if ("VOLUME_UP".equals(actionName)) {
                                        parsed.actionType = "VOLUME_UP";
                                    } else if ("VOLUME_DOWN".equals(actionName)) {
                                        parsed.actionType = "VOLUME_DOWN";
                                    } else if ("TORCH_ON".equals(actionName)) {
                                        parsed.actionType = "TORCH_ON";
                                    } else if ("TORCH_OFF".equals(actionName)) {
                                        parsed.actionType = "TORCH_OFF";
                                    } else if ("RESTART_PHONE".equals(actionName)) {
                                        parsed.actionType = "RESTART_PHONE";
                                    } else if ("WAIT".equals(actionName)) {
                                        parsed.actionType = "WAIT";
                                        parsed.target = String.valueOf(cmd.optInt("duration", 1000));
                                    } else {
                                        Log.e(TAG, "COMMAND FAILED: INVALID_COMMAND");
                                        isValid = false;
                                    }

                                    if (isValid) {
                                        String paramVal = !parsed.target.isEmpty() ? parsed.target : parsed.query;
                                        Log.d(TAG, "COMMAND " + (i + 1) + ": " + actionName + (paramVal.isEmpty() ? "" : " " + paramVal));
                                        actions.add(parsed);
                                    }
                                }

                                if (actions.isEmpty()) {
                                    handleParsingFailure("Empty commands list");
                                } else {
                                    pendingActions = actions;
                                    currentStepIndex = 0;
                                    isExecutingQueue = true;
                                    executeNextActionSequence();
                                }
                            } catch (Exception parseEx) {
                                Log.e(TAG, "JSON parsing error", parseEx);
                                handleParsingFailure(parseEx.getMessage());
                            }
                        }
                    });

                } catch (final Exception e) {
                    Log.e(TAG, "Gemini network communication failure", e);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            handleParsingFailure(e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    private void handleParsingFailure(String errorMsg) {
        displayFloatingStatus("Unable to complete that command.");
        setOrbState("error");
        speakText("I couldn't understand that command. Please try saying it another way.");
        returnOrbToReadyDelayed();
    }

    private String cleanGeminiResponse(String input) {
        if (input == null) return "";
        String text = input.trim();
        
        int firstBrace = text.indexOf('{');
        int lastBrace = text.lastIndexOf('}');
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            text = text.substring(firstBrace, lastBrace + 1);
        }
        return text.trim();
    }

    private String makeGeminiCall(String apiKey, String model, String userPrompt) throws Exception {
        URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);

        String systemPrompt = "You are an Android natural-language command parser.\n" +
                "Convert the user's complete natural-language request (English, Hindi, or Hinglish) into one or more commands supported by the existing Android command executor.\n" +
                "Return JSON only inside a single object containing a commands array. Do not include markdown codeblocks or explanations outside of JSON.\n" +
                "Never execute actions yourself. Never invent unsupported action names. Use only these provided commands:\n\n" +
                "1. OPEN_APP: parameter 'app' (e.g. 'WhatsApp', 'Chrome', 'Settings', 'YouTube')\n" +
                "2. OPEN_CHAT: parameter 'contact' (e.g. 'Sunil', 'Rahul')\n" +
                "3. TYPE_MESSAGE: parameter 'text' (exact text message intended for contact. Must not contain command words like 'Sunil ko bolo' or 'WhatsApp open karo')\n" +
                "4. SEND_MESSAGE: no parameters (clicks send button)\n" +
                "5. TYPE_TEXT: parameter 'text' (exact search text input, e.g. 'Android phone', 'mobile repair')\n" +
                "6. SUBMIT: no parameters (performs search query enter/submit trigger)\n" +
                "7. CLICK: parameter 'target' (matching UI button or text label)\n" +
                "8. SCROLL: parameter 'direction' ('DOWN' or 'UP')\n" +
                "9. BACK: no parameters\n" +
                "10. HOME: no parameters\n" +
                "11. VOLUME_UP: no parameters\n" +
                "12. VOLUME_DOWN: no parameters\n" +
                "13. TORCH_ON: no parameters\n" +
                "14. TORCH_OFF: no parameters\n" +
                "15. RESTART_PHONE: no parameters\n" +
                "16. WAIT: parameter 'duration' (milliseconds)\n\n" +
                "Split sentences containing multiple tasks into explicit separate sequential commands. For example:\n" +
                "- \"WhatsApp kholo aur Sunil ko bolo kal dukaan par 10 baje milna hai\" maps to: " +
                "[OPEN_APP app='WhatsApp', OPEN_CHAT contact='Sunil', TYPE_MESSAGE text='kal dukaan par 10 baje milna hai', SEND_MESSAGE]\n" +
                "- \"Chrome kholo aur Google par Android phone search karo\" maps to: " +
                "[OPEN_APP app='Chrome', TYPE_TEXT text='Android phone', SUBMIT]\n" +
                "- \"Google me mobile repair search karo\" maps to: [TYPE_TEXT text='mobile repair', SUBMIT]\n\n" +
                "If the request cannot be represented using these commands, return: {\"commands\": [], \"error\": \"UNSUPPORTED_COMMAND\"}.";

        JSONObject jsonPayload = new JSONObject();
        JSONArray contentsArray = new JSONArray();
        JSONObject contentObj = new JSONObject();
        JSONArray partsArray = new JSONArray();
        JSONObject partObj = new JSONObject();

        partObj.put("text", systemPrompt + "\n\nUser Input: \"" + userPrompt + "\"\nJSON:");
        partsArray.put(partObj);
        contentObj.put("parts", partsArray);
        contentsArray.put(contentObj);
        jsonPayload.put("contents", contentsArray);

        JSONObject generationConfig = new JSONObject();
        generationConfig.put("responseMimeType", "application/json");
        jsonPayload.put("generationConfig", generationConfig);

        String jsonString = jsonPayload.toString();

        OutputStream os = conn.getOutputStream();
        os.write(jsonString.getBytes("UTF-8"));
        os.flush();
        os.close();

        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            InputStream is = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            is.close();

            JSONObject responseJson = new JSONObject(sb.toString());
            JSONArray candidates = responseJson.getJSONArray("candidates");
            if (candidates.length() > 0) {
                JSONObject candidate = candidates.getJSONObject(0);
                JSONObject content = candidate.getJSONObject("content");
                JSONArray parts = content.getJSONArray("parts");
                if (parts.length() > 0) {
                    return parts.getJSONObject(0).getString("text");
                }
            }
            throw new Exception("Empty candidates returned");
        } else {
            throw new Exception("HTTP response failure: " + responseCode);
        }
    }

    private void speakActionAnnouncement(ParsedAction action) {
        String speech = null;
        switch (action.actionType) {
            case "OPEN":
                speech = "Opening " + action.target + ".";
                break;
            case "CLICK":
                speech = "Clicking " + action.target + ".";
                break;
            case "TYPE":
                speech = "Typing " + action.query + ".";
                break;
            case "SCROLL_DOWN":
                speech = "Scrolling down.";
                break;
            case "SCROLL_UP":
                speech = "Scrolling up.";
                break;
            case "BACK":
                speech = "Going back.";
                break;
            case "HOME":
                speech = "Going home.";
                break;
            case "SUBMIT":
                speech = "Submitting.";
                break;
            case "VOLUME_UP":
                speech = "Increasing volume.";
                break;
            case "VOLUME_DOWN":
                speech = "Decreasing volume.";
                break;
            case "TORCH_ON":
                speech = "Turning flashlight on.";
                break;
            case "TORCH_OFF":
                speech = "Turning flashlight off.";
                break;
            case "RESTART_PHONE":
                speech = "Rebooting.";
                break;
            case "WAIT":
                speech = "Waiting.";
                break;
        }
        if (speech != null) {
            speakText(speech);
        }
    }

    private void speakErrorResponse(ParsedAction action) {
        String speech = "Action failed.";
        switch (action.actionType) {
            case "OPEN":
                speech = "I couldn't open " + action.target;
                break;
            case "CLICK":
                speech = "I couldn't find " + action.target;
                break;
            case "TYPE":
                speech = "Typing failed.";
                break;
            case "VOLUME_UP":
            case "VOLUME_DOWN":
                speech = "Failed to adjust volume.";
                break;
            case "TORCH_ON":
            case "TORCH_OFF":
                speech = "Flashlight not responding.";
                break;
        }
        speakText(speech);
    }

    private void executeNextActionSequence() {
        if (pendingActions == null || currentStepIndex >= pendingActions.size()) {
            displayFloatingStatus("Command completed.");
            setOrbState("idle");
            speakText("Completed.");
            isExecutingQueue = false;
            returnOrbToReadyDelayed();
            return;
        }

        final ParsedAction action = pendingActions.get(currentStepIndex);
        final int stepNum = currentStepIndex + 1;
        final int total = pendingActions.size();
        final int sessionId = currentQueueSessionId;

        displayFloatingStatus("Action " + stepNum + "/" + total + ": " + action.actionType);
        Log.d(TAG, "EXECUTING: " + action.originalAction);

        speakActionAnnouncement(action);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (sessionId != currentQueueSessionId) {
                    return;
                }
                executeActionWithRetry(action, 1, 10, new ActionCallback() {
                    @Override
                    public void onSuccess() {
                        if (sessionId != currentQueueSessionId) {
                            Log.d(TAG, "Ignoring old queue callback onSuccess");
                            return;
                        }
                        Log.d(TAG, "COMMAND COMPLETED: " + action.originalAction);
                        currentStepIndex++;
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (sessionId == currentQueueSessionId) {
                                    executeNextActionSequence();
                                }
                            }
                        }, 800);
                    }

                    @Override
                    public void onFailure(String errorMsg) {
                        if (sessionId != currentQueueSessionId) {
                            Log.d(TAG, "Ignoring old queue callback onFailure");
                            return;
                        }
                        Log.d(TAG, "COMMAND FAILED: " + action.originalAction);
                        displayFloatingStatus("Action failed: " + errorMsg);
                        setOrbState("error");
                        speakErrorResponse(action);
                        cancelRunningQueue();
                        returnOrbToReadyDelayed();
                    }
                });
            }
        }, 1200);
    }

    private void executeActionWithRetry(final ParsedAction action, final int attempt, final int maxAttempts, final ActionCallback callback) {
        final int sessionId = currentQueueSessionId;
        executeActionInternal(action, new ActionCallback() {
            @Override
            public void onSuccess() {
                if (sessionId == currentQueueSessionId) {
                    callback.onSuccess();
                }
            }

            @Override
            public void onFailure(final String errorMsg) {
                if (sessionId != currentQueueSessionId) {
                    return;
                }
                if (attempt < maxAttempts) {
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (sessionId == currentQueueSessionId) {
                                executeActionWithRetry(action, attempt + 1, maxAttempts, callback);
                            }
                        }
                    }, 500);
                } else {
                    callback.onFailure(errorMsg);
                }
            }
        });
    }

    private void executeActionInternal(ParsedAction action, ActionCallback callback) {
        switch (action.actionType) {
            case "OPEN":
                executeOpenAppAction(action.target, callback);
                break;
            case "CLICK":
                executeClickAction(action.target, callback);
                break;
            case "TYPE":
                executeTypeAction(action.query, callback);
                break;
            case "SCROLL_DOWN":
                executeScrollAction(true, callback);
                break;
            case "SCROLL_UP":
                executeScrollAction(false, callback);
                break;
            case "BACK":
                performGlobalAction(GLOBAL_ACTION_BACK);
                callback.onSuccess();
                break;
            case "HOME":
                performGlobalAction(GLOBAL_ACTION_HOME);
                callback.onSuccess();
                break;
            case "SUBMIT":
                performSubmitAction(callback);
                break;
            case "VOLUME_UP":
                executeVolumeAction(true, callback);
                break;
            case "VOLUME_DOWN":
                executeVolumeAction(false, callback);
                break;
            case "TORCH_ON":
                executeTorchAction(true, callback);
                break;
            case "TORCH_OFF":
                executeTorchAction(false, callback);
                break;
            case "RESTART_PHONE":
                executeRestartAction(callback);
                break;
            case "WAIT":
                int duration = 1000;
                try {
                    duration = Integer.parseInt(action.target);
                } catch (Exception e) {}
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        callback.onSuccess();
                    }
                }, duration);
                break;
            default:
                callback.onFailure("Unsupported action: " + action.actionType);
                break;
        }
    }

    private void executeVolumeAction(boolean increase, ActionCallback callback) {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            int action = increase ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER;
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, action, AudioManager.FLAG_SHOW_UI);
            audioManager.adjustStreamVolume(AudioManager.STREAM_RING, action, AudioManager.FLAG_SHOW_UI);
            callback.onSuccess();
        } else {
            callback.onFailure("Audio service unavailable");
        }
    }

    private void executeTorchAction(boolean enable, ActionCallback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            if (cameraManager != null) {
                try {
                    String[] cameraIdList = cameraManager.getCameraIdList();
                    if (cameraIdList.length > 0) {
                        cameraManager.setTorchMode(cameraIdList[0], enable);
                        callback.onSuccess();
                    } else {
                        callback.onFailure("No camera flashlight detected");
                    }
                } catch (Exception e) {
                    callback.onFailure("Flashlight access error: " + e.getMessage());
                }
            } else {
                callback.onFailure("Camera service unavailable");
            }
        } else {
            callback.onFailure("Torch control not supported on this Android version");
        }
    }

    private void executeRestartAction(ActionCallback callback) {
        boolean success = false;
        if (Build.VERSION.SDK_INT >= 28) {
            success = performGlobalAction(6); // GLOBAL_ACTION_POWER_DIALOG
        }
        if (success) {
            speakText("Opening power menu. Please select Restart.");
            callback.onSuccess();
        } else {
            try {
                Process p = Runtime.getRuntime().exec("reboot");
                p.waitFor();
                callback.onSuccess();
            } catch (Exception e) {
                callback.onFailure("Power dialog trigger error.");
            }
        }
    }

    private void executeOpenAppAction(final String appName, final ActionCallback callback) {
        boolean launched = launchAppByName(appName);
        if (launched) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    callback.onSuccess();
                }
            }, 1800);
        } else {
            callback.onFailure("Application '" + appName + "' not found");
        }
    }

    private void executeClickAction(final String target, final ActionCallback callback) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            callback.onFailure("Active viewport missing");
            return;
        }

        List<AccessibilityNodeInfo> matches = findMatchingNodes(root, target);
        root.recycle();

        if (!matches.isEmpty()) {
            boolean clicked = false;
            for (AccessibilityNodeInfo node : matches) {
                if (!clicked) {
                    clicked = clickNodeOrParent(node);
                }
                node.recycle();
            }
            if (clicked) {
                callback.onSuccess();
            } else {
                callback.onFailure("Target matching '" + target + "' is not interactive");
            }
        } else {
            callback.onFailure("No element matches '" + target + "'");
        }
    }

    private void executeTypeAction(final String text, final ActionCallback callback) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            callback.onFailure("Active layout window unavailable");
            return;
        }
        final AccessibilityNodeInfo editNode = findEditableNode(root);
        root.recycle();

        if (editNode != null) {
            boolean typed = typeTextInNode(editNode, text);
            if (typed) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        editNode.recycle();
                        callback.onSuccess();
                    }
                }, 500);
            } else {
                editNode.recycle();
                callback.onFailure("Failed to input text characters");
            }
        } else {
            callback.onFailure("No focused input fields discovered");
        }
    }

    private void executeScrollAction(boolean down, ActionCallback callback) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            callback.onFailure("Root window empty");
            return;
        }

        List<AccessibilityNodeInfo> scrollables = findScrollableNodes(root);
        if (scrollables.isEmpty()) {
            root.recycle();
            callback.onFailure("No scrollable views discovered");
            return;
        }

        boolean scrolled = false;
        int action = down ? AccessibilityNodeInfo.ACTION_SCROLL_FORWARD : AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD;
        for (AccessibilityNodeInfo node : scrollables) {
            if (!scrolled) {
                if (node.performAction(action)) {
                    scrolled = true;
                }
            }
            node.recycle();
        }
        root.recycle();

        if (scrolled) {
            callback.onSuccess();
        } else {
            callback.onFailure("Scroll event rejected by system hierarchy");
        }
    }

    private void performSubmitAction(ActionCallback callback) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            callback.onFailure("Root layout tree unavailable");
            return;
        }

        AccessibilityNodeInfo submitBtn = findSearchSubmitButton(root);
        if (submitBtn != null) {
            boolean clicked = clickNodeOrParent(submitBtn);
            submitBtn.recycle();
            root.recycle();
            if (clicked) {
                callback.onSuccess();
                return;
            }
        }

        AccessibilityNodeInfo editNode = findEditableNode(root);
        root.recycle();
        if (editNode != null) {
            // Since there is no constant 'ACTION_IME_ACTION' on AccessibilityNodeInfo in standard Android SDK,
            // we safely perform a simulated click or trigger action on the active focus or enter key to submit inputs.
            boolean clicked = clickNodeOrParent(editNode);
            editNode.recycle();
            if (clicked) {
                callback.onSuccess();
                return;
            }
        }

        callback.onFailure("Submit button or enter action not found");
    }

    private boolean launchAppByName(String appName) {
        String cleanName = appName.toLowerCase().trim();
        PackageManager pm = getPackageManager();

        String targetPackage = null;
        if (cleanName.contains("youtube")) {
            targetPackage = "com.google.android.youtube";
        } else if (cleanName.contains("chrome")) {
            targetPackage = "com.android.chrome";
        } else if (cleanName.contains("maps") || cleanName.contains("google maps")) {
            targetPackage = "com.google.android.apps.maps";
        } else if (cleanName.contains("whatsapp")) {
            targetPackage = "com.whatsapp";
        } else if (cleanName.contains("settings")) {
            targetPackage = "com.android.settings";
        }

        if (targetPackage != null) {
            Intent intent = pm.getLaunchIntentForPackage(targetPackage);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return true;
            }
        }

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(mainIntent, 0);
        for (ResolveInfo info : apps) {
            String label = info.loadLabel(pm).toString().toLowerCase();
            if (label.contains(cleanName)) {
                Intent launchIntent = pm.getLaunchIntentForPackage(info.activityInfo.packageName);
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(launchIntent);
                    return true;
                }
            }
        }
        return false;
    }

    private List<AccessibilityNodeInfo> findMatchingNodes(AccessibilityNodeInfo root, String target) {
        List<AccessibilityNodeInfo> results = new ArrayList<>();
        findMatchingNodesRecursive(root, target.toLowerCase(), results);
        return results;
    }

    private void findMatchingNodesRecursive(AccessibilityNodeInfo node, String target, List<AccessibilityNodeInfo> results) {
        if (node == null) return;

        boolean match = false;
        CharSequence text = node.getText();
        if (text != null && text.toString().toLowerCase().contains(target)) {
            match = true;
        }

        if (!match) {
            CharSequence desc = node.getContentDescription();
            if (desc != null && desc.toString().toLowerCase().contains(target)) {
                match = true;
            }
        }

        if (!match && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            String viewId = node.getViewIdResourceName();
            if (viewId != null && viewId.toLowerCase().contains(target)) {
                match = true;
            }
        }

        if (match) {
            results.add(AccessibilityNodeInfo.obtain(node));
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                findMatchingNodesRecursive(child, target, results);
                child.recycle();
            }
        }
    }

    private boolean clickNodeOrParent(AccessibilityNodeInfo node) {
        if (node == null) return false;
        AccessibilityNodeInfo current = AccessibilityNodeInfo.obtain(node);
        while (current != null) {
            if (current.isClickable() && current.isEnabled()) {
                boolean success = current.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                if (success) {
                    current.recycle();
                    return true;
                }
            }
            AccessibilityNodeInfo parent = current.getParent();
            current.recycle();
            current = parent;
        }
        return false;
    }

    private boolean typeTextInNode(AccessibilityNodeInfo node, String text) {
        if (node == null) return false;
        node.performAction(AccessibilityNodeInfo.FOCUS_INPUT);
        node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);

        Bundle arguments = new Bundle();
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
        boolean success = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);

        if (success) {
            CharSequence currentText = node.getText();
            return currentText != null && currentText.toString().equals(text);
        }
        return false;
    }

    private AccessibilityNodeInfo findEditableNode(AccessibilityNodeInfo root) {
        if (root == null) return null;
        AccessibilityNodeInfo focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (focused != null && (focused.isEditable() || "android.widget.EditText".equals(focused.getClassName()))) {
            return focused;
        }
        return findFirstEditableNodeRecursive(root);
    }

    private AccessibilityNodeInfo findFirstEditableNodeRecursive(AccessibilityNodeInfo node) {
        if (node == null) return null;
        CharSequence className = node.getClassName();
        String classStr = className != null ? className.toString() : "";
        if (node.isEditable() || classStr.contains("EditText")) {
            return AccessibilityNodeInfo.obtain(node);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                AccessibilityNodeInfo result = findFirstEditableNodeRecursive(child);
                child.recycle();
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private List<AccessibilityNodeInfo> findScrollableNodes(AccessibilityNodeInfo node) {
        List<AccessibilityNodeInfo> result = new ArrayList<>();
        findScrollableNodesRecursive(node, result);
        return result;
    }

    private void findScrollableNodesRecursive(AccessibilityNodeInfo node, List<AccessibilityNodeInfo> result) {
        if (node == null) return;
        CharSequence className = node.getClassName();
        String classStr = className != null ? className.toString().toLowerCase() : "";
        boolean scrollableClass = classStr.contains("scrollview") ||
                classStr.contains("listview") ||
                classStr.contains("recyclerview") ||
                classStr.contains("gridview") ||
                classStr.contains("nestedscrollview") ||
                classStr.contains("viewpager");

        if ((node.isScrollable() || scrollableClass) && node.isVisibleToUser() && node.isEnabled()) {
            result.add(AccessibilityNodeInfo.obtain(node));
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                findScrollableNodesRecursive(child, result);
                child.recycle();
            }
        }
    }

    private boolean isIgnoredVoiceControlElement(String text, String desc, String viewId) {
        String t = text != null ? text.toLowerCase() : "";
        String d = desc != null ? desc.toLowerCase() : "";
        String id = viewId != null ? viewId.toLowerCase() : "";
        return t.contains("mic") || t.contains("voice") || t.contains("speak") || t.contains("audio") || t.contains("record")
                || d.contains("mic") || d.contains("voice") || d.contains("speak") || d.contains("audio") || d.contains("record")
                || id.contains("mic") || id.contains("voice") || id.contains("speak") || id.contains("audio") || id.contains("record");
    }

    private AccessibilityNodeInfo findSearchSubmitButton(AccessibilityNodeInfo root) {
        if (root == null) return null;
        List<AccessibilityNodeInfo> results = new ArrayList<>();
        findSubmitCandidatesRecursive(root, results);
        if (!results.isEmpty()) {
            AccessibilityNodeInfo best = results.get(0);
            for (int i = 1; i < results.size(); i++) {
                results.get(i).recycle();
            }
            return best;
        }
        return null;
    }

    private void findSubmitCandidatesRecursive(AccessibilityNodeInfo node, List<AccessibilityNodeInfo> results) {
        if (node == null) return;

        CharSequence text = node.getText();
        CharSequence desc = node.getContentDescription();
        String txt = text != null ? text.toString().toLowerCase() : "";
        String dsc = desc != null ? desc.toString().toLowerCase() : "";
        String viewId = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            String vId = node.getViewIdResourceName();
            if (vId != null) {
                viewId = vId.toLowerCase();
            }
        }

        if (!isIgnoredVoiceControlElement(txt, dsc, viewId)) {
            boolean match = false;
            if (node.isClickable() && node.isEnabled()) {
                if (txt.equals("search") || txt.equals("go") || txt.equals("done") || txt.equals("submit") || txt.contains("search") || txt.contains("go") || txt.contains("send") || txt.equals("send")) {
                    match = true;
                } else if (dsc.contains("search") || dsc.contains("submit") || dsc.contains("go") || dsc.contains("done") || dsc.contains("send") || dsc.contains("magnify") || dsc.contains("find")) {
                    match = true;
                } else if (!viewId.isEmpty()) {
                    if (viewId.contains("search_button") || viewId.contains("search_btn") || viewId.contains("search_icon") || viewId.contains("submit") || viewId.contains("send") || viewId.contains("go_button") || viewId.contains("go_btn") || viewId.contains("search_go") || viewId.contains("btn_search")) {
                        match = true;
                    }
                }
            }

            if (match) {
                results.add(AccessibilityNodeInfo.obtain(node));
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                findSubmitCandidatesRecursive(child, results);
                child.recycle();
            }
        }
    }

    private void returnOrbToReadyDelayed() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isSessionActive) {
                    setOrbState("listening");
                    startSpeechRecognizerListening();
                } else {
                    setOrbState("idle");
                }
            }
        }, 1500);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null && windowManager != null) {
            windowManager.removeView(floatingView);
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (orbAnimator != null) {
            orbAnimator.cancel();
        }
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}