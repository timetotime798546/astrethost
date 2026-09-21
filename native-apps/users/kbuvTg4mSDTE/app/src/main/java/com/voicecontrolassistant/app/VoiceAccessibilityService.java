package com.voicecontrolassistant.app;

import android.accessibilityservice.AccessibilityService;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.PixelFormat;
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

    // Multi-action flow structures
    private List<ParsedAction> pendingActions = new ArrayList<>();
    private int currentStepIndex = 0;
    private ParsedAction currentActionContext = null;

    static class ParsedAction {
        String actionType; // OPEN, SEARCH, CLICK, TYPE, SCROLL_DOWN, SCROLL_UP, PLAY, BACK, HOME, SUBMIT
        String target = "";
        String query = "";
        String rawText = "";

        @Override
        public String toString() {
            return "ParsedAction{" +
                    "actionType='" + actionType + '\'' +
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

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Log updates when layout transitions happen
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

                    // Coordination: pause SpeechRecognizer when assistant's own voice starts
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
            }, 600); // Small silence window to avoid loop-hearing
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
            // Ignore speech recognizer errors resulting from programmatic muting
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
            String recognizedText = matches.get(0);
            displayFloatingStatus("\"" + recognizedText + "\"");

            // Greeting and conversational parser validation check
            if (handleConversationalGreetings(recognizedText)) {
                return;
            }

            setOrbState("processing");
            
            // Core parser integration
            List<ParsedAction> actions = parseMultiStepCommand(recognizedText);
            if (actions.isEmpty()) {
                displayFloatingStatus("Could not interpret commands");
                setOrbState("idle");
                speakText("I couldn't understand that command.");
                returnOrbToReadyDelayed();
                return;
            }

            // Sequential Execution Engine activation
            pendingActions = actions;
            currentStepIndex = 0;
            executeNextActionSequence();
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

    // ==========================================
    // CONVERSATIONAL GREETINGS & HINGLISH PARSER
    // ==========================================

    private boolean handleConversationalGreetings(String input) {
        String clean = input.toLowerCase().trim().replaceAll("[.,!?]", "");

        // Native English greetings validation
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

        if (clean.equals("namaste")) {
            String resp = "Namaste! How can I help you?";
            displayFloatingStatus(resp);
            speakText(resp);
            setOrbState("idle");
            returnOrbToReadyDelayed();
            return true;
        }

        // Hinglish greetings validation
        if (clean.contains("hello bhai") || clean.contains("hi bhai")) {
            String resp = "Hello! Bataiye, main kya karun?";
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

    // ==========================================
    // MULTI-STEP VOICE COMMAND PARSER ENGINE
    // ==========================================

    private List<ParsedAction> parseMultiStepCommand(String spokenText) {
        Log.d(TAG, "COMMAND RAW: " + spokenText);

        // Split by sequential logical indicators (English + Hinglish variants)
        String[] chunks = spokenText.split("(?i)\\b(?:and then|followed by|after that|then|, then|next|and|phir|aur|phir se|then after|,|\\.)\\b");
        
        List<String> validChunks = new ArrayList<>();
        for (String c : chunks) {
            String trimmed = c.trim();
            if (!trimmed.isEmpty()) {
                validChunks.add(trimmed);
            }
        }

        // Apply merge rules to ensure search query arguments containing connectors are not falsely fragmented
        List<String> combinedSegments = new ArrayList<>();
        for (int i = 0; i < validChunks.size(); i++) {
            String chunk = validChunks.get(i);
            if (combinedSegments.isEmpty()) {
                combinedSegments.add(chunk);
            } else {
                if (containsActionVerb(chunk)) {
                    combinedSegments.add(chunk);
                } else {
                    int lastIndex = combinedSegments.size() - 1;
                    String merged = combinedSegments.get(lastIndex) + " and " + chunk;
                    combinedSegments.set(lastIndex, merged);
                }
            }
        }

        List<ParsedAction> actions = new ArrayList<>();
        for (String segment : combinedSegments) {
            ParsedAction act = parseSingleActionSegment(segment);
            if (act != null) {
                actions.add(act);
            }
        }

        // Print final parsed step structure internally for debug purposes
        Log.d(TAG, "PARSED STEPS COUNT: " + actions.size());
        for (int idx = 0; idx < actions.size(); idx++) {
            Log.d(TAG, "Parsed Action [" + (idx + 1) + "]: " + actions.get(idx).toString());
        }

        return actions;
    }

    private boolean containsActionVerb(String text) {
        String clean = text.toLowerCase().trim();
        String[] actionWords = {
                "open", "launch", "start", "kholo", "chalao",
                "search", "find", "dhundo",
                "click", "tap", "touch", "dabao",
                "type", "write", "likho", "send", "bhejo",
                "scroll", "niche", "niche scroll", "scroll down", "scroll up", "upar", "upar scroll",
                "play", "chalao", "bhejo",
                "back", "piche", "pichhe", "home",
                "enter", "submit", "go", "done"
        };
        for (String word : actionWords) {
            if (clean.startsWith(word) || clean.contains(" " + word + " ") || clean.endsWith(" " + word)) {
                return true;
            }
        }
        return false;
    }

    private ParsedAction parseSingleActionSegment(String segment) {
        String clean = segment.toLowerCase().trim();
        ParsedAction act = new ParsedAction();
        act.rawText = segment;

        // 1. SCROLL DOWN/UP
        if (clean.contains("scroll down") || clean.contains("niche scroll") || clean.contains("niche jao") || clean.contains("scroll niche") || clean.contains("neeche scroll")) {
            act.actionType = "SCROLL_DOWN";
            return act;
        }
        if (clean.contains("scroll up") || clean.contains("upar scroll") || clean.contains("upar jao") || clean.contains("scroll upar")) {
            act.actionType = "SCROLL_UP";
            return act;
        }

        // 2. BACK / HOME / SUBMIT
        if (clean.equals("go back") || clean.equals("back") || clean.contains("pichhe jao") || clean.contains("piche jao") || clean.equals("vaapas")) {
            act.actionType = "BACK";
            return act;
        }
        if (clean.equals("go home") || clean.equals("home") || clean.contains("screen par jao") || clean.contains("home screen")) {
            act.actionType = "HOME";
            return act;
        }
        if (clean.equals("enter") || clean.equals("press enter") || clean.equals("submit") || clean.equals("go") || clean.equals("done")) {
            act.actionType = "SUBMIT";
            return act;
        }

        // 3. PLAY ACTION
        if (clean.startsWith("play ") || clean.endsWith(" chalao") || clean.contains("play video") || clean.contains("video chalao")) {
            act.actionType = "PLAY";
            if (clean.startsWith("play ")) {
                act.target = segment.substring(5).trim();
            } else if (clean.endsWith(" chalao")) {
                act.target = segment.substring(0, segment.length() - 7).trim();
            } else {
                act.target = "video";
            }
            return act;
        }

        // 4. SEARCH ACTION
        if (clean.startsWith("search for ")) {
            act.actionType = "SEARCH";
            act.query = segment.substring(11).trim();
            return act;
        }
        if (clean.startsWith("search ")) {
            act.actionType = "SEARCH";
            act.query = segment.substring(7).trim();
            return act;
        }
        if (clean.startsWith("find ")) {
            act.actionType = "SEARCH";
            act.query = segment.substring(5).trim();
            return act;
        }
        if (clean.endsWith(" search karo") || clean.endsWith(" search")) {
            act.actionType = "SEARCH";
            int len = clean.endsWith(" search karo") ? 12 : 7;
            act.query = segment.substring(0, segment.length() - len).trim();
            return act;
        }
        if (clean.endsWith(" dhundo")) {
            act.actionType = "SEARCH";
            act.query = segment.substring(0, segment.length() - 7).trim();
            return act;
        }

        // 5. TYPE TEXT INJECTION
        if (clean.startsWith("type ")) {
            act.actionType = "TYPE";
            act.query = segment.substring(5).trim();
            return act;
        }
        if (clean.startsWith("write ")) {
            act.actionType = "TYPE";
            act.query = segment.substring(6).trim();
            return act;
        }
        if (clean.endsWith(" likho")) {
            act.actionType = "TYPE";
            act.query = segment.substring(0, segment.length() - 6).trim();
            return act;
        }
        if (clean.startsWith("send ")) {
            act.actionType = "TYPE";
            act.query = segment.substring(5).trim();
            return act;
        }
        if (clean.endsWith(" bhejo")) {
            act.actionType = "TYPE";
            act.query = segment.substring(0, segment.length() - 6).trim();
            return act;
        }

        // 6. OPEN APPLICATION
        if (clean.startsWith("open ")) {
            String target = segment.substring(5).trim();
            if (target.toLowerCase().contains("chat") || target.toLowerCase().contains("group") || target.toLowerCase().contains("contact")) {
                act.actionType = "CLICK";
            } else {
                act.actionType = "OPEN";
            }
            act.target = target;
            return act;
        }
        if (clean.startsWith("launch ")) {
            act.actionType = "OPEN";
            act.target = segment.substring(7).trim();
            return act;
        }
        if (clean.endsWith(" kholo")) {
            String target = segment.substring(0, segment.length() - 6).trim();
            if (target.toLowerCase().contains("chat") || target.toLowerCase().contains("group") || target.toLowerCase().contains("contact")) {
                act.actionType = "CLICK";
            } else {
                act.actionType = "OPEN";
            }
            act.target = target;
            return act;
        }

        // 7. CLICK
        if (clean.startsWith("click on ")) {
            act.actionType = "CLICK";
            act.target = segment.substring(9).trim();
            return act;
        }
        if (clean.startsWith("click ")) {
            act.actionType = "CLICK";
            act.target = segment.substring(6).trim();
            return act;
        }
        if (clean.startsWith("tap on ")) {
            act.actionType = "CLICK";
            act.target = segment.substring(7).trim();
            return act;
        }
        if (clean.startsWith("tap ")) {
            act.actionType = "CLICK";
            act.target = segment.substring(4).trim();
            return act;
        }
        if (clean.startsWith("touch ")) {
            act.actionType = "CLICK";
            act.target = segment.substring(6).trim();
            return act;
        }
        if (clean.endsWith(" par click karo") || clean.endsWith(" click karo")) {
            act.actionType = "CLICK";
            int len = clean.endsWith(" par click karo") ? 15 : 11;
            act.target = segment.substring(0, segment.length() - len).trim();
            return act;
        }
        if (clean.endsWith(" dabao")) {
            act.actionType = "CLICK";
            act.target = segment.substring(0, segment.length() - 6).trim();
            return act;
        }

        // Fallback target matching raw text input directly as click
        act.actionType = "CLICK";
        act.target = segment;
        return act;
    }


    // ==========================================
    // SEQUENTIAL EXECUTION ENGINE & ANNOUNCEMENTS
    // ==========================================

    private void speakActionAnnouncement(ParsedAction action) {
        String speech = null;
        switch (action.actionType) {
            case "OPEN":
                speech = "Opening " + action.target + ".";
                break;
            case "SEARCH":
                speech = "Searching " + action.query + ".";
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
            case "PLAY":
                speech = "Playing video.";
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
        }
        if (speech != null) {
            speakText(speech);
        }
    }

    private void speakErrorResponse(ParsedAction action) {
        String speech = "Action failed.";
        switch (action.actionType) {
            case "OPEN":
                speech = "I couldn't open that app.";
                break;
            case "SEARCH":
                speech = "Search failed.";
                break;
            case "CLICK":
                speech = "I couldn't find that button.";
                break;
            case "TYPE":
                speech = "Typing failed.";
                break;
            case "SCROLL_DOWN":
            case "SCROLL_UP":
                speech = "Scroll failed.";
                break;
            case "PLAY":
                speech = "I couldn't play that video.";
                break;
        }
        speakText(speech);
    }

    private void executeNextActionSequence() {
        if (pendingActions == null || currentStepIndex >= pendingActions.size()) {
            displayFloatingStatus("Multi-action steps complete!");
            setOrbState("idle");
            speakText("Done.");
            returnOrbToReadyDelayed();
            return;
        }

        final ParsedAction action = pendingActions.get(currentStepIndex);
        currentActionContext = action;
        final int stepNum = currentStepIndex + 1;
        final int total = pendingActions.size();

        displayFloatingStatus("Step " + stepNum + "/" + total + ": " + action.actionType + " " + (!action.target.isEmpty() ? action.target : action.query));

        Log.d(TAG, "STEP START - [" + stepNum + "/" + total + "] Type: " + action.actionType);

        // Speak the natural language announcement before executing
        speakActionAnnouncement(action);

        // Delay execution slightly to ensure TTS starts and blocks double-captures cleanly
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                executeActionWithRetry(action, 1, 2, new ActionCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "STEP SUCCESS - [" + stepNum + "/" + total + "]");
                        currentStepIndex++;
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                executeNextActionSequence();
                            }
                        }, 1200); // UI cooling transition delay
                    }

                    @Override
                    public void onFailure(String errorMsg) {
                        Log.d(TAG, "STEP FAILED - [" + stepNum + "/" + total + "] Info: " + errorMsg);
                        displayFloatingStatus("Step " + stepNum + " failed: " + errorMsg);
                        setOrbState("error");
                        
                        // Speak conversational failure feedback
                        speakErrorResponse(action);

                        pendingActions.clear(); // Halt remaining cascade pipeline steps
                        returnOrbToReadyDelayed();
                    }
                });
            }
        }, 1200);
    }

    private void executeActionWithRetry(final ParsedAction action, final int attempt, final int maxAttempts, final ActionCallback callback) {
        executeActionInternal(action, new ActionCallback() {
            @Override
            public void onSuccess() {
                callback.onSuccess();
            }

            @Override
            public void onFailure(final String errorMsg) {
                if (attempt < maxAttempts) {
                    Log.d(TAG, "Action " + action.actionType + " failed. Retry in 1000ms...");
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            executeActionWithRetry(action, attempt + 1, maxAttempts, callback);
                        }
                    }, 1000);
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
            case "SEARCH":
                executeSearchAction(action.query, callback);
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
            case "PLAY":
                executePlayAction(action.target, callback);
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
            default:
                callback.onFailure("Unsupported action: " + action.actionType);
                break;
        }
    }

    private void executeOpenAppAction(final String appName, final ActionCallback callback) {
        boolean launched = launchAppByName(appName);
        if (launched) {
            // Wait for targeted window transition frame
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    AccessibilityNodeInfo root = getRootInActiveWindow();
                    if (root != null) {
                        root.recycle();
                    }
                    callback.onSuccess();
                }
            }, 1800);
        } else {
            callback.onFailure("Launcher package matching '" + appName + "' not found");
        }
    }

    private void executeSearchAction(final String query, final ActionCallback callback) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        AccessibilityNodeInfo searchNode = findSearchNode(root);
        if (root != null) {
            root.recycle();
        }

        if (searchNode != null) {
            boolean clicked = clickNodeOrParent(searchNode);
            searchNode.recycle();
            if (clicked) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        typeAndSubmitSearchText(query, callback);
                    }
                }, 1000);
            } else {
                typeAndSubmitSearchText(query, callback);
            }
        } else {
            typeAndSubmitSearchText(query, callback);
        }
    }

    private void typeAndSubmitSearchText(final String query, final ActionCallback callback) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        final AccessibilityNodeInfo editNode = findEditableNode(root);
        if (root != null) {
            root.recycle();
        }

        if (editNode != null) {
            boolean typed = typeTextInNode(editNode, query);
            if (typed) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        boolean submitted = false;
                        AccessibilityNodeInfo r = getRootInActiveWindow();
                        if (r != null) {
                            AccessibilityNodeInfo submitBtn = findSearchSubmitButton(r);
                            if (submitBtn != null) {
                                submitted = clickNodeOrParent(submitBtn);
                                submitBtn.recycle();
                            }
                            r.recycle();
                        }
                        if (!submitted) {
                            editNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                editNode.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.getId());
                            }
                        }
                        editNode.recycle();
                        callback.onSuccess();
                    }
                }, 600);
            } else {
                editNode.recycle();
                callback.onFailure("Failed to write to search layout input field");
            }
        } else {
            callback.onFailure("No search layout or editable input field accessible");
        }
    }

    private void executeClickAction(final String target, final ActionCallback callback) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            callback.onFailure("Active window tree missing");
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
                callback.onFailure("Target matching '" + target + "' found but not interactive");
            }
        } else {
            callback.onFailure("No node matches text matching '" + target + "'");
        }
    }

    private void executeTypeAction(final String text, final ActionCallback callback) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            callback.onFailure("Active window invalid");
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
                        boolean submitted = false;
                        AccessibilityNodeInfo subR = getRootInActiveWindow();
                        if (subR != null) {
                            AccessibilityNodeInfo submitBtn = findSearchSubmitButton(subR);
                            if (submitBtn != null) {
                                submitted = clickNodeOrParent(submitBtn);
                                submitBtn.recycle();
                            }
                            subR.recycle();
                        }
                        if (!submitted) {
                            editNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                editNode.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.getId());
                            }
                        }
                        editNode.recycle();
                        callback.onSuccess();
                    }
                }, 500);
            } else {
                editNode.recycle();
                callback.onFailure("Failed to output programmatic characters");
            }
        } else {
            callback.onFailure("No editable focus input elements are active");
        }
    }

    private void executeScrollAction(boolean down, ActionCallback callback) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            callback.onFailure("Active viewport missing");
            return;
        }

        List<AccessibilityNodeInfo> scrollables = findScrollableNodes(root);
        if (scrollables.isEmpty()) {
            root.recycle();
            callback.onFailure("No programmatic scrollable layout views discovered");
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
            callback.onFailure("Scroll layout action was rejected by platform hierarchy");
        }
    }

    private void executePlayAction(final String target, final ActionCallback callback) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            callback.onFailure("No active layout tree loaded");
            return;
        }

        // Check if layout lists items, prioritizing video list components (e.g. YouTube elements)
        List<AccessibilityNodeInfo> scrollables = findScrollableNodes(root);
        if (!scrollables.isEmpty()) {
            for (AccessibilityNodeInfo scroll : scrollables) {
                for (int i = 0; i < scroll.getChildCount(); i++) {
                    AccessibilityNodeInfo child = scroll.getChild(i);
                    if (child != null) {
                        if (child.isClickable() && child.isVisibleToUser() && isVideoResultLayout(child)) {
                            boolean clicked = clickNodeOrParent(child);
                            child.recycle();
                            if (clicked) {
                                scroll.recycle();
                                root.recycle();
                                callback.onSuccess();
                                return;
                            }
                        }
                        child.recycle();
                    }
                }
                scroll.recycle();
            }
        }

        // General heuristic fallback
        List<AccessibilityNodeInfo> targets = new ArrayList<>();
        findPlayCandidatesRecursive(root, targets);
        if (!targets.isEmpty()) {
            boolean clicked = false;
            for (AccessibilityNodeInfo node : targets) {
                if (!clicked) {
                    clicked = clickNodeOrParent(node);
                }
                node.recycle();
            }
            root.recycle();
            if (clicked) {
                callback.onSuccess();
            } else {
                callback.onFailure("Discovered video play candidate elements but could not perform click actions");
            }
        } else {
            root.recycle();
            callback.onFailure("No video, play buttons, or content results found");
        }
    }

    private boolean isVideoResultLayout(AccessibilityNodeInfo node) {
        CharSequence desc = node.getContentDescription();
        if (desc != null) {
            String d = desc.toString().toLowerCase();
            if ((d.contains("views") || d.contains("minutes") || d.contains("hours") || d.contains("ago") || d.contains("play video") || d.contains("watch"))
                    && !d.contains("microphone") && !d.contains("search") && !d.contains("menu")) {
                return true;
            }
        }
        CharSequence text = node.getText();
        if (text != null) {
            String t = text.toString().toLowerCase();
            if (t.contains("views") || t.contains("play") || t.contains("video")) {
                return true;
            }
        }
        return false;
    }

    private void findPlayCandidatesRecursive(AccessibilityNodeInfo node, List<AccessibilityNodeInfo> results) {
        if (node == null) return;
        CharSequence text = node.getText();
        CharSequence desc = node.getContentDescription();
        String txt = text != null ? text.toString().toLowerCase() : "";
        String dsc = desc != null ? desc.toString().toLowerCase() : "";

        if (node.isClickable() && node.isVisibleToUser()) {
            if (txt.contains("play") || txt.contains("video") || txt.contains("song") || txt.contains("watching") ||
                    dsc.contains("play") || dsc.contains("video") || dsc.contains("song") || dsc.contains("watching") || dsc.contains("thumbnail")) {
                results.add(AccessibilityNodeInfo.obtain(node));
            }
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                findPlayCandidatesRecursive(child, results);
                child.recycle();
            }
        }
    }

    private void performSubmitAction(ActionCallback callback) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            callback.onFailure("No active window viewport loaded");
            return;
        }

        AccessibilityNodeInfo submitBtn = findSearchSubmitButton(root);
        root.recycle();

        if (submitBtn != null) {
            boolean clicked = clickNodeOrParent(submitBtn);
            submitBtn.recycle();
            if (clicked) {
                callback.onSuccess();
                return;
            }
        }
        callback.onFailure("No search or submit action trigger controls exposed");
    }


    // ==========================================
    // PROGRAMMATIC CLICK & SELECTION UTILS
    // ==========================================

    private boolean launchAppByName(String appName) {
        String cleanName = appName.toLowerCase().trim();
        PackageManager pm = getPackageManager();

        // High priority mapping packages
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

        // Generic fuzzy package scanner
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

    private AccessibilityNodeInfo findSearchNode(AccessibilityNodeInfo root) {
        if (root == null) return null;
        List<AccessibilityNodeInfo> matches = new ArrayList<>();
        findNodesByQueryRecursive(root, "search", matches);
        if (!matches.isEmpty()) {
            AccessibilityNodeInfo best = matches.get(0);
            for (int i = 1; i < matches.size(); i++) {
                matches.get(i).recycle();
            }
            return best;
        }

        findNodesByQueryRecursive(root, "find", matches);
        if (!matches.isEmpty()) {
            AccessibilityNodeInfo best = matches.get(0);
            for (int i = 1; i < matches.size(); i++) {
                matches.get(i).recycle();
            }
            return best;
        }

        return findFirstEditableNodeRecursive(root);
    }

    private void findNodesByQueryRecursive(AccessibilityNodeInfo node, String query, List<AccessibilityNodeInfo> results) {
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
            if (node.isClickable() && (txt.contains(query) || dsc.contains(query))) {
                results.add(AccessibilityNodeInfo.obtain(node));
            } else if (!viewId.isEmpty() && viewId.contains(query)) {
                results.add(AccessibilityNodeInfo.obtain(node));
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                findNodesByQueryRecursive(child, query, results);
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
                if (txt.equals("search") || txt.equals("go") || txt.equals("done") || txt.equals("submit") || txt.contains("search") || txt.contains("go")) {
                    match = true;
                } else if (dsc.contains("search") || dsc.contains("submit") || dsc.contains("go") || dsc.contains("done") || dsc.contains("magnify") || dsc.contains("find")) {
                    match = true;
                } else if (!viewId.isEmpty()) {
                    if (viewId.contains("search_button") || viewId.contains("search_btn") || viewId.contains("search_icon") || viewId.contains("submit") || viewId.contains("go_button") || viewId.contains("go_btn") || viewId.contains("search_go") || viewId.contains("btn_search")) {
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