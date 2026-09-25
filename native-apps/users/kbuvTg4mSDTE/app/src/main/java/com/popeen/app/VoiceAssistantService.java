package com.popeen.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VoiceAssistantService extends Service {

    public enum VoiceState {
        WAKE_WORD_LISTENING,
        WAKE_WORD_DETECTED,
        COMMAND_LISTENING,
        PROCESSING_COMMAND,
        SPEAKING,
        STOPPED
    }

    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "popeen_channel";
    private static final long SESSION_DURATION_MS = 10 * 60 * 1000; // 10 Minutes in milliseconds

    private static VoiceState currentState = VoiceState.STOPPED;
    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;
    private TextToSpeech tts;
    private boolean isTtsInitialized = false;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable restartRunnable;

    private static long wakeSessionEndTime = 0;

    // State guards to prevent repeated beep loops
    private boolean isSpeechRecognizerActive = false;
    private long lastStartListeningTime = 0;
    private static final long MIN_START_INTERVAL_MS = 1500; // Debounce lock to prevent rapid ON/OFF cycles

    // Track state to update UI and support contextual response repeats
    private static String lastCommandText = "";
    private static String lastResponseText = "";
    private static String lastCommandType = ""; // Context tracker

    public static VoiceState getCurrentState() {
        return currentState;
    }

    public static String getLastCommandText() {
        return lastCommandText;
    }

    public static String getLastResponseText() {
        return lastResponseText;
    }

    public static long getSessionEndTime() {
        if (currentState == VoiceState.STOPPED || currentState == VoiceState.WAKE_WORD_LISTENING) {
            return 0;
        }
        return wakeSessionEndTime;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        currentState = VoiceState.WAKE_WORD_LISTENING;

        createNotificationChannel();
        startForeground(NOTIFICATION_ID, getNotification("Starting Popeen Voice Assistant..."));

        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    isTtsInitialized = true;
                    updateTtsLanguage();
                    setupTtsCallbacks();
                    sendUpdateBroadcast(currentState.name(), "TTS Engine initialized successfully.");
                } else {
                    sendUpdateBroadcast(currentState.name(), "TextToSpeech Engine initialization failed.");
                }
            }
        });

        transitionToState(VoiceState.WAKE_WORD_LISTENING);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if ("ACTION_STOP_SERVICE".equals(action)) {
                stopSelf();
                return START_NOT_STICKY;
            } else if ("ACTION_UPDATE_LANG".equals(action)) {
                updateTtsLanguage();
                if (currentState == VoiceState.WAKE_WORD_LISTENING) {
                    transitionToState(VoiceState.WAKE_WORD_LISTENING);
                }
                return START_STICKY;
            } else if ("ACTION_MANUAL_WAKE".equals(action)) {
                transitionToState(VoiceState.WAKE_WORD_DETECTED);
                return START_STICKY;
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        currentState = VoiceState.STOPPED;
        wakeSessionEndTime = 0;
        
        if (restartRunnable != null) {
            mainHandler.removeCallbacks(restartRunnable);
        }

        destroySpeech();

        if (tts != null) {
            try {
                tts.stop();
                tts.shutdown();
            } catch (Exception e) {}
        }

        sendUpdateBroadcast("STOPPED", "Popeen Voice Assistant service stopped.");
        stopForeground(true);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Popeen Background Assistant Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Keeps Popeen Voice Assistant active to listen for wake words.");
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification getNotification(String text) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        );

        Intent stopIntent = new Intent(this, VoiceAssistantService.class);
        stopIntent.setAction("ACTION_STOP_SERVICE");
        PendingIntent stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        builder.setContentTitle("Popeen Voice Assistant")
               .setContentText(text)
               .setSmallIcon(R.drawable.icon)
               .setContentIntent(pendingIntent)
               .setOngoing(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            Notification.Action stopAction = new Notification.Action.Builder(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop Assistant",
                stopPendingIntent
            ).build();
            builder.addAction(stopAction);
        } else {
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Assistant", stopPendingIntent);
        }

        return builder.build();
    }

    private void updateNotification(String text) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, getNotification(text));
        }
    }

    private void sendUpdateBroadcast(String state, String log) {
        Intent intent = new Intent("com.popeen.app.STATUS_UPDATE");
        intent.putExtra("state", state);
        intent.putExtra("log", log);
        intent.putExtra("last_command", lastCommandText);
        intent.putExtra("last_response", lastResponseText);
        intent.putExtra("session_end_time", getSessionEndTime());
        sendBroadcast(intent);
    }

    private boolean isSessionActive() {
        return System.currentTimeMillis() < wakeSessionEndTime;
    }

    private synchronized void transitionToState(VoiceState newState) {
        if (currentState == VoiceState.STOPPED && newState != VoiceState.WAKE_WORD_LISTENING) return;

        currentState = newState;
        String statusText = "";

        if (restartRunnable != null) {
            mainHandler.removeCallbacks(restartRunnable);
        }

        switch (newState) {
            case WAKE_WORD_LISTENING:
                statusText = "Listening for \"Hey Popeen\"";
                updateNotification(statusText);
                sendUpdateBroadcast(newState.name(), "Say 'Hey Popeen' to activate assistant.");
                scheduleListening(1000); // Wait safely to avoid echo/feedback loops
                break;

            case WAKE_WORD_DETECTED:
                statusText = "Wake phrase detected!";
                updateNotification(statusText);
                wakeSessionEndTime = System.currentTimeMillis() + SESSION_DURATION_MS;
                sendUpdateBroadcast(newState.name(), "Active session started (10 minutes).");
                speakWakeWordPrompt();
                break;

            case COMMAND_LISTENING:
                long remainingSec = (wakeSessionEndTime - System.currentTimeMillis()) / 1000;
                if (remainingSec < 0) remainingSec = 0;
                
                statusText = "Listening for command…";
                updateNotification(statusText);
                sendUpdateBroadcast(newState.name(), "Command session active (" + remainingSec + "s left)");
                scheduleListening(1200); // Safety pause specifically to avoid instant recurse
                break;

            case PROCESSING_COMMAND:
                statusText = "Processing command…";
                updateNotification(statusText);
                sendUpdateBroadcast(newState.name(), "Parsing speech patterns...");
                break;

            case SPEAKING:
                statusText = "Speaking…";
                updateNotification(statusText);
                break;
        }
    }

    private synchronized void initSpeech() {
        if (speechRecognizer != null) {
            setupRecognizerIntent();
            return;
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            sendUpdateBroadcast(currentState.name(), "Error: Native Speech Recognition unavailable.");
            return;
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                if (currentState == VoiceState.SPEAKING || currentState == VoiceState.PROCESSING_COMMAND || currentState == VoiceState.STOPPED) {
                    return;
                }
                isSpeechRecognizerActive = true;
                if (isSessionActive() && currentState == VoiceState.COMMAND_LISTENING) {
                    sendUpdateBroadcast(currentState.name(), "Listening now...");
                } else {
                    sendUpdateBroadcast(currentState.name(), "Microphone active...");
                }
            }

            @Override
            public void onBeginningOfSpeech() {
                if (currentState == VoiceState.SPEAKING || currentState == VoiceState.PROCESSING_COMMAND || currentState == VoiceState.STOPPED) {
                    return;
                }
                isSpeechRecognizerActive = true;
            }

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
                if (currentState == VoiceState.SPEAKING || currentState == VoiceState.PROCESSING_COMMAND || currentState == VoiceState.STOPPED) {
                    return;
                }
                isSpeechRecognizerActive = false;
            }

            @Override
            public void onError(int error) {
                if (currentState == VoiceState.SPEAKING || currentState == VoiceState.PROCESSING_COMMAND || currentState == VoiceState.STOPPED) {
                    return;
                }
                isSpeechRecognizerActive = false;
                handleSpeechError(error);
            }

            @Override
            public void onResults(Bundle results) {
                if (currentState == VoiceState.SPEAKING || currentState == VoiceState.PROCESSING_COMMAND || currentState == VoiceState.STOPPED) {
                    return;
                }
                isSpeechRecognizerActive = false;
                handleSpeechResults(results);
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                if (currentState == VoiceState.SPEAKING || currentState == VoiceState.PROCESSING_COMMAND || currentState == VoiceState.STOPPED) {
                    return;
                }
                handlePartialResults(partialResults);
            }

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });

        setupRecognizerIntent();
    }

    private void setupRecognizerIntent() {
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);

        String lang = PreferencesHelper.getLanguage(this);
        if ("hi".equals(lang)) {
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN");
        } else if ("hinglish".equals(lang)) {
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN");
        } else {
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        }
    }

    private void stopSpeechListening() {
        isSpeechRecognizerActive = false;
        if (speechRecognizer != null) {
            try {
                speechRecognizer.cancel();
            } catch (Exception e) {}
        }
    }

    private void destroySpeech() {
        isSpeechRecognizerActive = false;
        if (speechRecognizer != null) {
            try {
                speechRecognizer.cancel();
                speechRecognizer.destroy();
            } catch (Exception e) {}
            speechRecognizer = null;
        }
    }

    private synchronized void startListeningInternal() {
        if (currentState == VoiceState.STOPPED || currentState == VoiceState.SPEAKING || currentState == VoiceState.PROCESSING_COMMAND) {
            isSpeechRecognizerActive = false;
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastStartListeningTime < MIN_START_INTERVAL_MS) {
            // Respect lock/debounce time - prevent loops by pushing schedule backwards
            long delay = MIN_START_INTERVAL_MS - (now - lastStartListeningTime);
            scheduleListening(delay);
            return;
        }

        if (isSpeechRecognizerActive) {
            // Already listening, bypass start operation to avoid system click sounds
            return;
        }

        try {
            initSpeech();
            if (speechRecognizer != null) {
                isSpeechRecognizerActive = true;
                lastStartListeningTime = System.currentTimeMillis();
                speechRecognizer.startListening(recognizerIntent);
            }
        } catch (Exception e) {
            isSpeechRecognizerActive = false;
            sendUpdateBroadcast(currentState.name(), "Recognizer start exception: " + e.getMessage());
            scheduleListening(2500);
        }
    }

    private synchronized void scheduleListening(long delayMs) {
        if (restartRunnable != null) {
            mainHandler.removeCallbacks(restartRunnable);
        }
        restartRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentState == VoiceState.STOPPED || currentState == VoiceState.SPEAKING || currentState == VoiceState.PROCESSING_COMMAND) {
                    isSpeechRecognizerActive = false;
                    return;
                }

                if (currentState == VoiceState.COMMAND_LISTENING && !isSessionActive()) {
                    sendUpdateBroadcast("WAKE_WORD_LISTENING", "10-minute session closed. Say 'Hey Popeen'.");
                    transitionToState(VoiceState.WAKE_WORD_LISTENING);
                    return;
                }

                startListeningInternal();
            }
        };
        mainHandler.postDelayed(restartRunnable, delayMs);
    }

    private void handleSpeechResults(Bundle results) {
        if (results == null) {
            restartListeningOrReset();
            return;
        }
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches == null || matches.isEmpty()) {
            restartListeningOrReset();
            return;
        }

        String heardText = matches.get(0);
        lastCommandText = heardText;
        sendUpdateBroadcast(currentState.name(), "Heard: \"" + heardText + "\"");

        if (currentState == VoiceState.WAKE_WORD_LISTENING) {
            boolean detected = false;
            for (int i = 0; i < matches.size(); i++) {
                if (matchesWakePhrase(matches.get(i))) {
                    detected = true;
                    break;
                }
            }
            if (detected) {
                transitionToState(VoiceState.WAKE_WORD_DETECTED);
            } else {
                scheduleListening(1000);
            }
        } 
        else if (currentState == VoiceState.COMMAND_LISTENING) {
            boolean redundantWake = false;
            for (int i = 0; i < matches.size(); i++) {
                if (matchesWakePhrase(matches.get(i))) {
                    redundantWake = true;
                    break;
                }
            }

            if (redundantWake) {
                wakeSessionEndTime = System.currentTimeMillis() + SESSION_DURATION_MS; 
                sendUpdateBroadcast(currentState.name(), "Active session time refreshed.");
                speakWakeWordPrompt();
            } else {
                processUserCommand(heardText);
            }
        }
    }

    private void handlePartialResults(Bundle partialResults) {
        if (currentState != VoiceState.WAKE_WORD_LISTENING) return;
        if (partialResults == null) return;
        ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches == null) return;

        for (int i = 0; i < matches.size(); i++) {
            if (matchesWakePhrase(matches.get(i))) {
                stopSpeechListening();
                transitionToState(VoiceState.WAKE_WORD_DETECTED);
                break;
            }
        }
    }

    private void restartListeningOrReset() {
        if (currentState == VoiceState.WAKE_WORD_LISTENING) {
            scheduleListening(1000);
        } else if (currentState == VoiceState.COMMAND_LISTENING) {
            if (isSessionActive()) {
                scheduleListening(1200);
            } else {
                sendUpdateBroadcast("WAKE_WORD_LISTENING", "10-minute session closed. Say 'Hey Popeen'.");
                transitionToState(VoiceState.WAKE_WORD_LISTENING);
            }
        }
    }

    private boolean matchesWakePhrase(String phrase) {
        if (phrase == null) return false;
        String lower = phrase.toLowerCase().trim();
        return lower.contains("popeen") || 
               lower.contains("pop in") || 
               lower.contains("pop-in") || 
               lower.contains("poppin") || 
               lower.contains("popeye") || 
               lower.contains("pop teen") || 
               lower.contains("popteen") ||
               lower.contains("pope") ||
               lower.contains("hello popeen") ||
               lower.contains("hey popeen");
    }

    private void handleSpeechError(int error) {
        if (currentState == VoiceState.STOPPED || currentState == VoiceState.SPEAKING || currentState == VoiceState.PROCESSING_COMMAND) return;

        String errorMsg;
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO: errorMsg = "Audio error"; break;
            case SpeechRecognizer.ERROR_CLIENT: errorMsg = "Client error"; break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: errorMsg = "Permissions missing"; break;
            case SpeechRecognizer.ERROR_NETWORK: errorMsg = "Network error"; break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: errorMsg = "Network timeout"; break;
            case SpeechRecognizer.ERROR_NO_MATCH: errorMsg = "No match"; break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: errorMsg = "Recognizer busy"; break;
            case SpeechRecognizer.ERROR_SERVER: errorMsg = "Server connection error"; break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: errorMsg = "Speech timeout"; break;
            default: errorMsg = "Error " + error; break;
        }

        if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
            sendUpdateBroadcast(currentState.name(), "Speech error: " + errorMsg);
        }

        // Handle error and pause without creating duplicate start schedules
        stopSpeechListening();

        long delay = 1500;
        if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
            delay = 2500;
        }

        if (currentState == VoiceState.WAKE_WORD_LISTENING) {
            scheduleListening(delay);
        } else if (currentState == VoiceState.COMMAND_LISTENING) {
            if (isSessionActive()) {
                scheduleListening(delay);
            } else {
                sendUpdateBroadcast("WAKE_WORD_LISTENING", "Inactivity timeout. Returning to wake word mode.");
                transitionToState(VoiceState.WAKE_WORD_LISTENING);
            }
        }
    }

    private boolean isHindiLanguage(String text) {
        if (text == null) return false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 0x0900 && c <= 0x097F) {
                return true;
            }
        }

        String lower = text.toLowerCase().trim();
        String[] hindiKeywords = {
            "kya", "hua", "hai", "hain", "baje", "baja", "samay", "vakt", "waqt", 
            "kitne", "kitna", "mera", "naam", "kal", "mausam", "kaisa", "batao", 
            "bataiye", "boliye", "bolo", "tum", "aap", "kaise", "ho", "ji", 
            "gadi", "ghadi", "kam", "shukriya", "on", "off", "torch", "lagao", "kholo"
        };

        if (lower.contains("kya hua") || lower.contains("kya hai") || lower.contains("kaisa hai") ||
            lower.contains("kitne baje") || lower.contains("samay kya") || lower.contains("time kya") ||
            lower.contains("batao") || lower.contains("bataiye") || lower.contains("naam kya") ||
            lower.contains("torch on") || lower.contains("torch off") || lower.contains("timer lagao") ||
            lower.contains("alarm lagao") || lower.contains("kholo")) {
            return true;
        }

        String[] tokens = lower.split("\\s+");
        int matchedHindiCount = 0;
        for (int i = 0; i < tokens.length; i++) {
            String cleanToken = tokens[i].replaceAll("[^a-zA-Z]", "");
            for (int j = 0; j < hindiKeywords.length; j++) {
                if (cleanToken.equals(hindiKeywords[j])) {
                    matchedHindiCount++;
                    break;
                }
            }
        }
        return matchedHindiCount > 0;
    }

    private void processUserCommand(String speech) {
        transitionToState(VoiceState.PROCESSING_COMMAND);
        String lower = speech.toLowerCase().trim();
        boolean isHindi = isHindiLanguage(speech);

        // 1. STOP / CANCEL
        if (lower.contains("stop") || lower.contains("cancel") || lower.contains("bas") || lower.contains("chup") || lower.contains("stop listening")) {
            wakeSessionEndTime = 0;
            String resp = isHindi ? "ठीक है, रोक रही हूँ।" : "Okay, stopping.";
            speakDirectly(resp, "COMMAND_RESPONSE", isHindi ? new Locale("hi", "IN") : Locale.US);
            transitionToState(VoiceState.WAKE_WORD_LISTENING);
            return;
        }

        // 2. REPEAT
        if (lower.contains("repeat") || lower.contains("dobara bolo") || lower.contains("phir se bolo") || lower.contains("fir se bolo") || lower.contains("dobara")) {
            if (lastResponseText.isEmpty()) {
                String resp = isHindi ? "मैंने अभी तक कुछ नहीं बोला है।" : "I haven't spoken anything yet.";
                speakDirectly(resp, "COMMAND_RESPONSE", isHindi ? new Locale("hi", "IN") : Locale.US);
            } else {
                speakDirectly(lastResponseText, "COMMAND_RESPONSE", isHindiResponse(lastResponseText) ? new Locale("hi", "IN") : Locale.US);
            }
            return;
        }

        // 3. HELP
        if (lower.contains("help") || lower.contains("what can you do") || lower.contains("tum kya kya kar sakte ho") || lower.contains("kaam")) {
            String resp = isHindi ? "मैं समय, तारीख, दिन, बैटरी लेवल, टॉर्च चालू बंद करना, वॉल्यूम बदलना, अलार्म और टाइमर लगाना, कैलकुलेटर और ऐप्स खोलने में मदद कर सकती हूँ।" :
                                   "I can help you with time, date, day, battery percent, flashlight, volume, alarms, timers, local calculations, launching apps, and remembering names.";
            speakDirectly(resp, "COMMAND_RESPONSE", isHindi ? new Locale("hi", "IN") : Locale.US);
            return;
        }

        // 4. MEMORY HANDLING (Username memory)
        String memoryResponse = handleMemoryCommand(speech, isHindi);
        if (memoryResponse != null) {
            speakDirectly(memoryResponse, "COMMAND_RESPONSE", isHindi ? new Locale("hi", "IN") : Locale.US);
            return;
        }

        // 5. BATTERY & CHARGING STATUS
        if (lower.contains("battery") || lower.contains("charging") || lower.contains("percentage") || lower.contains("percent")) {
            if (lastCommandType.equals("BATTERY") && (lower.contains("charging") || lower.contains("charge") || lower.contains("aur"))) {
                boolean charging = isPhoneCharging();
                String resp = isHindi ? (charging ? "हाँ, आपका फ़ोन चार्ज हो रहा है।" : "नहीं, फ़ोन अभी चार्ज नहीं हो रहा है।") :
                                        (charging ? "Yes, your phone is charging." : "No, your phone is not charging.");
                speakDirectly(resp, "COMMAND_RESPONSE", isHindi ? new Locale("hi", "IN") : Locale.US);
                return;
            }

            int level = getBatteryPercentage();
            boolean charging = isPhoneCharging();
            lastCommandType = "BATTERY";
            
            String resp;
            if (isHindi) {
                resp = "बैटरी अभी " + level + " प्रतिशत है और " + (charging ? "फ़ोन चार्ज हो रहा है।" : "फ़ोन चार्ज नहीं हो रहा है।");
            } else {
                resp = "Your battery is at " + level + " percent and " + (charging ? "is charging." : "is not charging.");
            }
            speakDirectly(resp, "COMMAND_RESPONSE", isHindi ? new Locale("hi", "IN") : Locale.US);
            return;
        }

        // 6. FLASHLIGHT
        if (lower.contains("torch") || lower.contains("flashlight") || lower.contains("jalaye") || lower.contains("ujala")) {
            boolean turnOn = lower.contains("on") || lower.contains("chala") || lower.contains("jalao") || lower.contains("chalu");
            boolean status = toggleFlashlight(turnOn);
            String resp;
            if (status) {
                if (isHindi) {
                    resp = turnOn ? "टॉर्च चालू कर दी है।" : "टॉर्च बंद कर दी है।";
                } else {
                    resp = turnOn ? "Flashlight is now turned on." : "Flashlight is now turned off.";
                }
            } else {
                resp = isHindi ? "माफ़ कीजिये, मैं टॉर्च चालू नहीं कर पाई।" : "Sorry, I could not control the flashlight.";
            }
            speakDirectly(resp, "COMMAND_RESPONSE", isHindi ? new Locale("hi", "IN") : Locale.US);
            return;
        }

        // 7. VOLUME
        if (lower.contains("volume") || lower.contains("sound") || lower.contains("mute") || lower.contains("awaaz") || lower.contains("awaz")) {
            boolean success = false;
            String resp = "";
            if (lower.contains("mute") || lower.contains("silent") || lower.contains("band")) {
                success = adjustVolume(false, true, null);
                resp = isHindi ? "आवाज़ म्यूट कर दी है।" : "Volume muted.";
            } else if (lower.contains("50") || lower.contains("fifty") || lower.contains("aadha") || lower.contains("aadhi")) {
                success = adjustVolume(false, false, 50);
                resp = isHindi ? "आवाज़ पचास प्रतिशत कर दी है।" : "Volume set to 50 percent.";
            } else if (lower.contains("badhao") || lower.contains("up") || lower.contains("increase") || lower.contains("tez") || lower.contains("zyada")) {
                success = adjustVolume(true, false, null);
                resp = isHindi ? "आवाज़ बढ़ा दी है।" : "Volume increased.";
            } else if (lower.contains("kam") || lower.contains("down") || lower.contains("decrease") || lower.contains("dheeme")) {
                success = adjustVolume(false, false, null);
                resp = isHindi ? "आवाज़ कम कर दी है।" : "Volume decreased.";
            }

            if (!success || resp.isEmpty()) {
                resp = isHindi ? "माफ़ कीजिये, मैं आवाज़ नियंत्रित नहीं कर सकी।" : "Unable to adjust volume settings.";
            }
            speakDirectly(resp, "COMMAND_RESPONSE", isHindi ? new Locale("hi", "IN") : Locale.US);
            return;
        }

        // 8. ALARM
        if (lower.contains("alarm")) {
            parseAndSetAlarm(lower, isHindi);
            return;
        }

        // 9. TIMER
        if (lower.contains("timer")) {
            parseAndSetTimer(lower, isHindi);
            return;
        }

        // 10. CALCULATOR
        if (lower.contains("plus") || lower.contains("minus") || lower.contains("multiplied") || 
            lower.contains("divided") || lower.contains("percent") || lower.contains("+") || 
            lower.contains("-") || lower.contains("*") || lower.contains("/") || 
            lower.contains("into") || lower.contains("aur") || lower.contains("bhag") || lower.contains("guna")) {
            
            String calcResponse = evaluateMath(lower, isHindi);
            if (calcResponse != null) {
                speakDirectly(calcResponse, "COMMAND_RESPONSE", isHindi ? new Locale("hi", "IN") : Locale.US);
                return;
            }
        }

        // 11. SETTINGS SHORTCUTS
        if (lower.contains("setting") || lower.contains("settings")) {
            String target = "";
            if (lower.contains("wifi") || lower.contains("wi-fi")) target = "wifi";
            else if (lower.contains("bluetooth")) target = "bluetooth";
            else if (lower.contains("display") || lower.contains("screen")) target = "display";
            else if (lower.contains("sound") || lower.contains("volume")) target = "sound";
            else if (lower.contains("battery") || lower.contains("power")) target = "battery";
            else if (lower.contains("location") || lower.contains("gps")) target = "location";
            else if (lower.contains("app") || lower.contains("application")) target = "app";

            if (!target.isEmpty()) {
                boolean ok = openSettingsShortcut(target);
                String resp = isHindi ? "सेटिंग्स खोल रही हूँ।" : "Opening requested settings panel.";
                if (!ok) resp = isHindi ? "माफ़ कीजिये, सेटिंग्स नहीं खोल सकी।" : "Failed to open settings panel.";
                speakDirectly(resp, "COMMAND_RESPONSE", isHindi ? new Locale("hi", "IN") : Locale.US);
                return;
            }
        }

        // 12. APP LAUNCHER
        if (lower.contains("kholo") || lower.contains("open") || lower.contains("launch")) {
            String appName = "";
            if (lower.contains("whatsapp")) appName = "WhatsApp";
            else if (lower.contains("youtube")) appName = "YouTube";
            else if (lower.contains("chrome")) appName = "Chrome";
            else if (lower.contains("settings")) appName = "Settings";

            if (!appName.isEmpty()) {
                boolean launched = launchApp(appName);
                String resp = launched ? (isHindi ? appName + " खोल रही हूँ।" : "Opening " + appName + ".") :
                                         (isHindi ? appName + " आपके फ़ोन में नहीं मिला।" : appName + " is not installed on this device.");
                speakDirectly(resp, "COMMAND_RESPONSE", isHindi ? new Locale("hi", "IN") : Locale.US);
                return;
            }
        }

        // 13. PHONE INFO & DEVICE STATUS
        if (lower.contains("phone") || lower.contains("android version") || lower.contains("model") || lower.contains("specification") || lower.contains("manufacture")) {
            String manufacturer = Build.MANUFACTURER;
            String model = Build.MODEL;
            String version = Build.VERSION.RELEASE;
            
            String resp;
            if (isHindi) {
                resp = "आपका फ़ोन " + manufacturer + " का " + model + " मॉडल है और यह एंड्राइड वर्शन " + version + " पर चल रहा है।";
            } else {
                resp = "Your device is a " + manufacturer + " " + model + " running Android version " + version + ".";
            }
            speakDirectly(resp, "COMMAND_RESPONSE", isHindi ? new Locale("hi", "IN") : Locale.US);
            return;
        }

        // 14. DATE / DAY / YEAR
        if (lower.contains("date") || lower.contains("day") || lower.contains("year") || lower.contains("month") || 
            lower.contains("tarikh") || lower.contains("tareekh") || lower.contains("din") || lower.contains("saal") || 
            lower.contains("mahina") || lower.contains("aaj")) {
            
            Calendar cal = Calendar.getInstance();
            String[] daysHindi = {"रविवार", "सोमवार", "मंगलवार", "बुधवार", "गुरुवार", "शुक्रवार", "शनिवार"};
            String[] daysEng = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
            String[] monthsHindi = {"जनवरी", "फ़रवरी", "मार्च", "अप्रैल", "मई", "जून", "जुलाई", "अगस्त", "सितंबर", "अक्टूबर", "नवंबर", "दिसंबर"};
            String[] monthsEng = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};

            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            int date = cal.get(Calendar.DATE);
            int month = cal.get(Calendar.MONTH);
            int year = cal.get(Calendar.YEAR);

            String resp;
            if (isHindi) {
                resp = "आज " + daysHindi[dayOfWeek - 1] + " है, तारीख " + date + " " + monthsHindi[month] + " " + year + " है।";
            } else {
                resp = "Today is " + daysEng[dayOfWeek - 1] + ", " + monthsEng[month] + " " + date + ", " + year + ".";
            }
            speakDirectly(resp, "COMMAND_RESPONSE", isHindi ? new Locale("hi", "IN") : Locale.US);
            return;
        }

        // 15. TIME
        if (lower.contains("time") || lower.contains("clock") || lower.contains("hour") || 
            lower.contains("समय") || lower.contains("बजे") || lower.contains("वक्त") || lower.contains("baje") || lower.contains("samay")) {
            speakTimeResponse(isHindi);
            return;
        }

        // 16. OFFLINE DEFAULT FALLBACK
        sendUpdateBroadcast(currentState.name(), "Offline command fallthrough: \"" + speech + "\"");
        speakUnsupportedResponse(isHindi);
    }

    private String handleMemoryCommand(String text, boolean isHindi) {
        String lower = text.toLowerCase();
        android.content.SharedPreferences prefs = getSharedPreferences("popeen_memory", MODE_PRIVATE);
        
        if (lower.contains("my name is ") || lower.contains("call me ") || lower.contains("mera naam ") || lower.contains("mujhe ")) {
            String name = "";
            if (lower.contains("my name is ")) {
                name = text.substring(lower.indexOf("my name is ") + 11).trim();
            } else if (lower.contains("call me ")) {
                name = text.substring(lower.indexOf("call me ") + 8).trim();
            } else if (lower.contains("mera naam ")) {
                int idx = lower.indexOf("mera naam ");
                int endIdx = lower.indexOf(" hai");
                if (endIdx > idx) {
                    name = text.substring(idx + 10, endIdx).trim();
                } else {
                    name = text.substring(idx + 10).trim();
                }
            } else if (lower.contains("mujhe ") && lower.contains(" bolo")) {
                int idx = lower.indexOf("mujhe ");
                int endIdx = lower.indexOf(" bolo");
                name = text.substring(idx + 6, endIdx).trim();
            }
            
            if (!name.isEmpty()) {
                name = name.replaceAll("[\\.\\?\\!]", "");
                prefs.edit().putString("user_name", name).apply();
                return isHindi ? "ठीक है, मैंने याद रख लिया कि आपका नाम " + name + " है।" :
                                 "Alright, I will remember that your name is " + name + ".";
            }
        }
        
        if (lower.contains("what is my name") || lower.contains("mera naam kya hai") || lower.contains("who am i") || lower.contains("main kaun")) {
            String savedName = prefs.getString("user_name", "");
            if (savedName.isEmpty()) {
                return isHindi ? "मुझे अभी आपका नाम नहीं पता। आप बोल सकते हैं: मेरा नाम राहुल है।" :
                                 "I don't know your name yet. You can say: My name is John.";
            } else {
                return isHindi ? "आपका नाम " + savedName + " है।" : "Your name is " + savedName + ".";
            }
        }
        
        if (lower.contains("forget my name") || lower.contains("forget me") || lower.contains("naam bhool jao") || lower.contains("naam delete")) {
            prefs.edit().remove("user_name").apply();
            return isHindi ? "ठीक है, मैंने आपका नाम मिटा दिया है।" : "Okay, I have forgotten your name.";
        }
        
        return null;
    }

    private void parseAndSetAlarm(String speech, boolean isHindi) {
        int hour = 7; 
        int minute = 0;
        boolean isPm = speech.contains("pm") || speech.contains("evening") || speech.contains("night") || speech.contains("sham") || speech.contains("raat") || speech.contains("dopahar");
        boolean isAm = speech.contains("am") || speech.contains("morning") || speech.contains("subah");

        Matcher m = Pattern.compile("(\\d+)\\s*(:?)\\s*(\\d*)").matcher(speech);
        if (m.find()) {
            try {
                hour = Integer.parseInt(m.group(1));
                String minG = m.group(3);
                if (minG != null && !minG.isEmpty()) {
                    minute = Integer.parseInt(minG);
                }
            } catch (Exception e) {}
        }

        if (isPm && hour < 12) hour += 12;
        else if (isAm && hour == 12) hour = 0;

        try {
            Intent intent = new Intent(android.provider.AlarmClock.ACTION_SET_ALARM);
            intent.putExtra(android.provider.AlarmClock.EXTRA_HOUR, hour);
            intent.putExtra(android.provider.AlarmClock.EXTRA_MINUTES, minute);
            intent.putExtra(android.provider.AlarmClock.EXTRA_MESSAGE, "Popeen Alarm");
            intent.putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI, true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            String reply = isHindi ? "मैंने " + hour + " बजकर " + minute + " मिनट का अलार्म लगा दिया है।" :
                                     "I have set an alarm for " + hour + ":" + String.format(Locale.US, "%02d", minute) + ".";
            speakDirectly(reply, "COMMAND_RESPONSE", isHindi ? new Locale("hi", "IN") : Locale.US);
        } catch (Exception e) {
            String reply = isHindi ? "माफ़ कीजिये, मैं अलार्म नहीं सेट कर पाई।" : "Unable to configure system alarm.";
            speakDirectly(reply, "COMMAND_RESPONSE", isHindi ? new Locale("hi", "IN") : Locale.US);
        }
    }

    private void parseAndSetTimer(String speech, boolean isHindi) {
        int durationSec = 60; // Default 1 min
        Matcher m = Pattern.compile("(\\d+)").matcher(speech);
        if (m.find()) {
            try {
                int num = Integer.parseInt(m.group(1));
                if (speech.contains("hour") || speech.contains("ghanta") || speech.contains("ghante")) {
                    durationSec = num * 3600;
                } else if (speech.contains("second") || speech.contains("sec")) {
                    durationSec = num;
                } else {
                    durationSec = num * 60;
                }
            } catch (Exception e) {}
        }

        try {
            Intent intent = new Intent(android.provider.AlarmClock.ACTION_SET_TIMER);
            intent.putExtra(android.provider.AlarmClock.EXTRA_LENGTH, durationSec);
            intent.putExtra(android.provider.AlarmClock.EXTRA_MESSAGE, "Popeen Timer");
            intent.putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI, true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            String reply = isHindi ? "मैंने " + (durationSec / 60) + " मिनट का टाइमर चालू कर दिया है।" :
                                     "Timer set for " + (durationSec / 60) + " minutes.";
            speakDirectly(reply, "COMMAND_RESPONSE", isHindi ? new Locale("hi", "IN") : Locale.US);
        } catch (Exception e) {
            String reply = isHindi ? "माफ़ कीजिये, मैं टाइमर चालू नहीं कर पाई।" : "Could not start countdown timer.";
            speakDirectly(reply, "COMMAND_RESPONSE", isHindi ? new Locale("hi", "IN") : Locale.US);
        }
    }

    private String evaluateMath(String input, boolean isHindi) {
        String cleaned = input.toLowerCase().replaceAll("[^a-z0-9\\+\\-\\*/%\\s]", " ");
        double val1 = Double.NaN;
        double val2 = Double.NaN;
        String operator = null;

        if (cleaned.contains("plus") || cleaned.contains("+") || cleaned.contains("aur")) operator = "+";
        else if (cleaned.contains("minus") || cleaned.contains("-") || cleaned.contains("kam")) operator = "-";
        else if (cleaned.contains("into") || cleaned.contains("multiply") || cleaned.contains("*") || cleaned.contains("times") || cleaned.contains("guna")) operator = "*";
        else if (cleaned.contains("divided") || cleaned.contains("divide") || cleaned.contains("/") || cleaned.contains("bhag")) operator = "/";
        else if (cleaned.contains("percent") || cleaned.contains("%")) operator = "%";

        Matcher m = Pattern.compile("\\d+").matcher(cleaned);
        if (m.find()) {
            try { val1 = Double.parseDouble(m.group()); } catch (Exception e) {}
        }
        if (m.find()) {
            try { val2 = Double.parseDouble(m.group()); } catch (Exception e) {}
        }

        if (Double.isNaN(val1) || Double.isNaN(val2) || operator == null) {
            return null;
        }

        double result = 0;
        switch (operator) {
            case "+": result = val1 + val2; break;
            case "-": result = val1 - val2; break;
            case "*": result = val1 * val2; break;
            case "/": 
                if (val2 == 0) {
                    return isHindi ? "शून्य से भाग संभव नहीं है।" : "Division by zero is undefined.";
                }
                result = val1 / val2; 
                break;
            case "%": 
                result = (val1 * val2) / 100.0;
                break;
            default: return null;
        }

        String resStr = (result == (long) result) ? String.valueOf((long) result) : String.format(Locale.US, "%.2f", result);
        return isHindi ? "जवाब " + resStr + " है।" : "The calculated result is " + resStr + ".";
    }

    private int getBatteryPercentage() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, filter);
        if (batteryStatus != null) {
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            return (int) ((level / (float) scale) * 100);
        }
        return 50;
    }

    private boolean isPhoneCharging() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, filter);
        if (batteryStatus != null) {
            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
        }
        return false;
    }

    private boolean toggleFlashlight(boolean turnOn) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            CameraManager cm = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            if (cm != null) {
                try {
                    String[] ids = cm.getCameraIdList();
                    if (ids.length > 0) {
                        cm.setTorchMode(ids[0], turnOn);
                        return true;
                    }
                } catch (Exception e) {}
            }
        }
        return false;
    }

    private boolean adjustVolume(boolean increase, boolean mute, Integer setVal) {
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (am != null) {
            int stream = AudioManager.STREAM_MUSIC;
            int max = am.getStreamMaxVolume(stream);
            int current = am.getStreamVolume(stream);
            if (mute) {
                am.setStreamVolume(stream, 0, AudioManager.FLAG_SHOW_UI);
                return true;
            } else if (setVal != null) {
                int target = (max * setVal) / 100;
                am.setStreamVolume(stream, target, AudioManager.FLAG_SHOW_UI);
                return true;
            } else if (increase) {
                int target = current + 2;
                if (target > max) target = max;
                am.setStreamVolume(stream, target, AudioManager.FLAG_SHOW_UI);
                return true;
            } else {
                int target = current - 2;
                if (target < 0) target = 0;
                am.setStreamVolume(stream, target, AudioManager.FLAG_SHOW_UI);
                return true;
            }
        }
        return false;
    }

    private boolean launchApp(String appName) {
        PackageManager pm = getPackageManager();
        String pack = null;
        String lower = appName.toLowerCase();
        if (lower.contains("whatsapp")) pack = "com.whatsapp";
        else if (lower.contains("youtube")) pack = "com.google.android.youtube";
        else if (lower.contains("chrome")) pack = "com.android.chrome";
        else if (lower.contains("settings")) pack = "com.android.settings";

        if (pack != null) {
            try {
                Intent i = pm.getLaunchIntentForPackage(pack);
                if (i != null) {
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    return true;
                }
            } catch (Exception e) {}
        }
        return false;
    }

    private boolean openSettingsShortcut(String type) {
        try {
            Intent i;
            if ("wifi".equals(type)) i = new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS);
            else if ("bluetooth".equals(type)) i = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
            else if ("display".equals(type)) i = new Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS);
            else if ("sound".equals(type)) i = new Intent(android.provider.Settings.ACTION_SOUND_SETTINGS);
            else if ("battery".equals(type)) i = new Intent(android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS);
            else if ("location".equals(type)) i = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            else i = new Intent(android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
            
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void updateTtsLanguage() {
        if (tts == null || !isTtsInitialized) return;
        String lang = PreferencesHelper.getLanguage(this);
        if ("hi".equals(lang)) {
            tts.setLanguage(new Locale("hi", "IN"));
        } else if ("hinglish".equals(lang)) {
            tts.setLanguage(new Locale("hi", "IN"));
        } else {
            tts.setLanguage(Locale.US);
        }
    }

    private void setupTtsCallbacks() {
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                stopSpeechListening();
            }

            @Override
            public void onDone(final String utteranceId) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if ("WAKE_PROMPT".equals(utteranceId)) {
                            transitionToState(VoiceState.COMMAND_LISTENING);
                        } else if ("COMMAND_RESPONSE".equals(utteranceId)) {
                            if (isSessionActive()) {
                                transitionToState(VoiceState.COMMAND_LISTENING);
                            } else {
                                transitionToState(VoiceState.WAKE_WORD_LISTENING);
                            }
                        }
                    }
                });
            }

            @Override
            public void onError(final String utteranceId) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isSessionActive()) {
                            transitionToState(VoiceState.COMMAND_LISTENING);
                        } else {
                            transitionToState(VoiceState.WAKE_WORD_LISTENING);
                        }
                    }
                });
            }
        });
    }

    private void speakWakeWordPrompt() {
        if (tts == null || !isTtsInitialized) {
            transitionToState(VoiceState.COMMAND_LISTENING);
            return;
        }

        String lang = PreferencesHelper.getLanguage(this);
        String promptText;
        Locale locale;

        if ("hi".equals(lang)) {
            promptText = "जी बोलिए?";
            locale = new Locale("hi", "IN");
        } else if ("hinglish".equals(lang)) {
            promptText = "Haan boliye?";
            locale = new Locale("hi", "IN"); 
        } else {
            promptText = "Yes, please?";
            locale = Locale.US;
        }

        speakDirectly(promptText, "WAKE_PROMPT", locale);
    }

    private void speakTimeResponse(boolean isHindi) {
        Calendar cal = Calendar.getInstance();
        int hour12 = cal.get(Calendar.HOUR);
        if (hour12 == 0) hour12 = 12;
        int minute = cal.get(Calendar.MINUTE);
        int amPm = cal.get(Calendar.AM_PM);
        String amPmStr = (amPm == Calendar.AM) ? "AM" : "PM";

        String response;
        Locale locale;

        if (isHindi) {
            locale = new Locale("hi", "IN");
            if (minute == 0) {
                response = "अभी " + hour12 + " बजे हैं।";
            } else {
                response = "अभी " + hour12 + " बजकर " + minute + " मिनट हुए हैं।";
            }
        } else {
            locale = Locale.US;
            response = "It is " + hour12 + ":" + String.format(Locale.US, "%02d", minute) + " " + amPmStr + ".";
        }

        speakDirectly(response, "COMMAND_RESPONSE", locale);
    }

    private void speakUnsupportedResponse(boolean isHindi) {
        String response;
        Locale locale;

        if (isHindi) {
            locale = new Locale("hi", "IN");
            response = "माफ़ कीजिये, मैं यह क्रिया अभी नहीं कर सकती। क्या आप कुछ और पूछना चाहते हैं?";
        } else {
            locale = Locale.US;
            response = "I am sorry, I am unable to perform that operation. Is there something else I can help you with?";
        }

        speakDirectly(response, "COMMAND_RESPONSE", locale);
    }

    private boolean isHindiResponse(String text) {
        if (text == null) return false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 0x0900 && c <= 0x097F) {
                return true;
            }
        }
        return false;
    }

    private void speakDirectly(String text, String utteranceId, Locale locale) {
        lastResponseText = text;
        transitionToState(VoiceState.SPEAKING);
        sendUpdateBroadcast(currentState.name(), "Speaking: \"" + text + "\"");

        stopSpeechListening();

        if (tts == null || !isTtsInitialized) {
            if (isSessionActive()) {
                transitionToState(VoiceState.COMMAND_LISTENING);
            } else {
                transitionToState(VoiceState.WAKE_WORD_LISTENING);
            }
            return;
        }

        try {
            int availability = tts.isLanguageAvailable(locale);
            if (availability >= TextToSpeech.LANG_AVAILABLE) {
                tts.setLanguage(locale);
            } else {
                tts.setLanguage(Locale.US);
            }
        } catch (Exception e) {
            try {
                tts.setLanguage(Locale.US);
            } catch (Exception ex) {}
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
        } else {
            HashMap<String, String> params = new HashMap<>();
            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, params);
        }
    }
}