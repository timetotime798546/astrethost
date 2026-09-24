package com.popeen.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
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

    private VoiceState currentState = VoiceState.STOPPED;
    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;
    private TextToSpeech tts;
    private boolean isTtsInitialized = false;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable restartRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        currentState = VoiceState.WAKE_WORD_LISTENING;

        // Start Foreground immediately to fulfill API 34 strict foreground service execution window
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, getNotification("Starting Popeen Voice Assistant..."));

        // Initialize TTS engine
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    isTtsInitialized = true;
                    updateTtsLanguage();
                    setupTtsCallbacks();
                } else {
                    sendUpdateBroadcast(currentState.name(), "TextToSpeech Engine initialization failed.");
                }
            }
        });

        // Start Speech Engine listening loop
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
                // Reinitialize listener configured language parameters without dropping active flow
                if (currentState == VoiceState.WAKE_WORD_LISTENING) {
                    transitionToState(VoiceState.WAKE_WORD_LISTENING);
                }
                return START_STICKY;
            }
        }

        // Maintain persistent background context
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        currentState = VoiceState.STOPPED;
        
        if (restartRunnable != null) {
            mainHandler.removeCallbacks(restartRunnable);
        }

        if (speechRecognizer != null) {
            try {
                speechRecognizer.cancel();
                speechRecognizer.destroy();
            } catch (Exception e) {}
        }

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
        sendBroadcast(intent);
    }

    private synchronized void transitionToState(VoiceState newState) {
        if (currentState == VoiceState.STOPPED) return;

        currentState = newState;
        String statusText = "";

        if (restartRunnable != null) {
            mainHandler.removeCallbacks(restartRunnable);
        }

        switch (newState) {
            case WAKE_WORD_LISTENING:
                statusText = "Listening for \"Hey Popeen\"";
                updateNotification(statusText);
                sendUpdateBroadcast(newState.name(), "Listening continuously for Wake Phrase...");
                scheduleRestartSpeech(100);
                break;

            case WAKE_WORD_DETECTED:
                statusText = "Wake phrase detected!";
                updateNotification(statusText);
                sendUpdateBroadcast(newState.name(), "Wake phrase matched! Prompting user...");
                speakWakeWordPrompt();
                break;

            case COMMAND_LISTENING:
                statusText = "Listening for command…";
                updateNotification(statusText);
                sendUpdateBroadcast(newState.name(), "Listening for commands (Time queries only)...");
                startCommandListening();
                break;

            case PROCESSING_COMMAND:
                statusText = "Processing command…";
                updateNotification(statusText);
                sendUpdateBroadcast(newState.name(), "Analyzing speech input...");
                break;

            case SPEAKING:
                statusText = "Speaking…";
                updateNotification(statusText);
                break;
        }
    }

    private void initSpeech() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            sendUpdateBroadcast(currentState.name(), "CRITICAL ERROR: System speech recognition service unavailable.");
            return;
        }

        if (speechRecognizer != null) {
            try {
                speechRecognizer.cancel();
                speechRecognizer.destroy();
            } catch (Exception e) {}
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {}

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
                handleSpeechError(error);
            }

            @Override
            public void onResults(Bundle results) {
                handleSpeechResults(results);
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                handlePartialResults(partialResults);
            }

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });

        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        // Map system language preference to speech recognizer
        String lang = PreferencesHelper.getLanguage(this);
        if ("hi".equals(lang)) {
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN");
        } else {
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        }
    }

    private void scheduleRestartSpeech(long delayMs) {
        if (restartRunnable != null) {
            mainHandler.removeCallbacks(restartRunnable);
        }
        restartRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentState == VoiceState.WAKE_WORD_LISTENING) {
                    try {
                        initSpeech();
                        if (speechRecognizer != null) {
                            speechRecognizer.startListening(recognizerIntent);
                        }
                    } catch (Exception e) {
                        sendUpdateBroadcast(currentState.name(), "Speech activation exception: " + e.getMessage());
                        scheduleRestartSpeech(2000);
                    }
                }
            }
        };
        mainHandler.postDelayed(restartRunnable, delayMs);
    }

    private void startCommandListening() {
        try {
            initSpeech();
            if (speechRecognizer != null) {
                speechRecognizer.startListening(recognizerIntent);
            }
        } catch (Exception e) {
            sendUpdateBroadcast(currentState.name(), "Failed to start single-shot command listener: " + e.getMessage());
            transitionToState(VoiceState.WAKE_WORD_LISTENING);
        }
    }

    private void handleSpeechResults(Bundle results) {
        if (results == null) return;
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches == null || matches.isEmpty()) {
            if (currentState == VoiceState.WAKE_WORD_LISTENING) {
                scheduleRestartSpeech(500);
            } else if (currentState == VoiceState.COMMAND_LISTENING) {
                transitionToState(VoiceState.WAKE_WORD_LISTENING);
            }
            return;
        }

        if (currentState == VoiceState.WAKE_WORD_LISTENING) {
            boolean detected = false;
            for (String val : matches) {
                if (matchesWakePhrase(val)) {
                    detected = true;
                    break;
                }
            }
            if (detected) {
                transitionToState(VoiceState.WAKE_WORD_DETECTED);
            } else {
                scheduleRestartSpeech(500);
            }
        } else if (currentState == VoiceState.COMMAND_LISTENING) {
            String command = matches.get(0);
            processUserCommand(command);
        }
    }

    private void handlePartialResults(Bundle partialResults) {
        if (currentState != VoiceState.WAKE_WORD_LISTENING) return;
        if (partialResults == null) return;
        ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches == null) return;

        for (String val : matches) {
            if (matchesWakePhrase(val)) {
                // Cancel current recognizer immediately for optimal latency
                if (speechRecognizer != null) {
                    try {
                        speechRecognizer.cancel();
                    } catch (Exception e) {}
                }
                transitionToState(VoiceState.WAKE_WORD_DETECTED);
                break;
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
               lower.contains("popteen");
    }

    private void handleSpeechError(int error) {
        if (currentState == VoiceState.STOPPED) return;

        String errorMsg;
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO: errorMsg = "Audio error"; break;
            case SpeechRecognizer.ERROR_CLIENT: errorMsg = "Client side error"; break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: errorMsg = "Permissions missing"; break;
            case SpeechRecognizer.ERROR_NETWORK: errorMsg = "Network error"; break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: errorMsg = "Network timeout"; break;
            case SpeechRecognizer.ERROR_NO_MATCH: errorMsg = "No vocal matches"; break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: errorMsg = "Recognizer busy"; break;
            case SpeechRecognizer.ERROR_SERVER: errorMsg = "Server validation failed"; break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: errorMsg = "Silence timeout"; break;
            default: errorMsg = "Code: " + error; break;
        }

        sendUpdateBroadcast(currentState.name(), "Speech Engine notification: " + errorMsg);

        if (currentState == VoiceState.WAKE_WORD_LISTENING) {
            // Re-schedule continuous polling safely
            scheduleRestartSpeech(800);
        } else if (currentState == VoiceState.COMMAND_LISTENING) {
            // Timeout or recognition error returns system back to wake listening
            transitionToState(VoiceState.WAKE_WORD_LISTENING);
        }
    }

    private void processUserCommand(String speech) {
        transitionToState(VoiceState.PROCESSING_COMMAND);

        if (isTimeCommand(speech)) {
            sendUpdateBroadcast(currentState.name(), "Classified command: [TIME_COMMAND] from raw: \"" + speech + "\"");
            speakTimeResponse();
        } else {
            sendUpdateBroadcast(currentState.name(), "Command unmapped or unsupported: \"" + speech + "\"");
            speakUnsupportedResponse();
        }
    }

    private boolean isTimeCommand(String speech) {
        if (speech == null) return false;
        String lower = speech.toLowerCase().trim();

        // Standard semantic mapping across English, Hindi, and Hinglish transliterations
        return lower.contains("time") || 
               lower.contains("clock") || 
               lower.contains("hour") || 
               lower.contains("समय") || 
               lower.contains("बजे") || 
               lower.contains("वक्त") || 
               lower.contains("बजा") || 
               lower.contains("घड़ी") || 
               lower.contains("samay") || 
               lower.contains("baje") || 
               lower.contains("baja") || 
               lower.contains("kitne") || 
               lower.contains("batao");
    }

    private void updateTtsLanguage() {
        if (tts == null || !isTtsInitialized) return;
        String lang = PreferencesHelper.getLanguage(this);
        if ("hi".equals(lang)) {
            tts.setLanguage(new Locale("hi", "IN"));
        } else if ("hinglish".equals(lang)) {
            // Speak hinglish in custom accent utilizing an Indian-localized English engine
            tts.setLanguage(new Locale("en", "IN"));
        } else {
            tts.setLanguage(Locale.US);
        }
    }

    private void setupTtsCallbacks() {
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {}

            @Override
            public void onDone(final String utteranceId) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if ("WAKE_PROMPT".equals(utteranceId)) {
                            transitionToState(VoiceState.COMMAND_LISTENING);
                        } else if ("COMMAND_RESPONSE".equals(utteranceId)) {
                            transitionToState(VoiceState.WAKE_WORD_LISTENING);
                        }
                    }
                });
            }

            @Override
            public void onError(final String utteranceId) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if ("WAKE_PROMPT".equals(utteranceId)) {
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
        if ("hi".equals(lang)) {
            promptText = "जी?";
        } else if ("hinglish".equals(lang)) {
            promptText = "Haan boliyay?";
        } else {
            promptText = "Yes?";
        }

        speakDirectly(promptText, "WAKE_PROMPT");
    }

    private void speakTimeResponse() {
        Calendar cal = Calendar.getInstance();
        int hour12 = cal.get(Calendar.HOUR);
        if (hour12 == 0) hour12 = 12;
        int minute = cal.get(Calendar.MINUTE);
        int amPm = cal.get(Calendar.AM_PM);
        String amPmStr = (amPm == Calendar.AM) ? "AM" : "PM";

        String lang = PreferencesHelper.getLanguage(this);
        String response;

        if ("hi".equals(lang)) {
            if (minute == 0) {
                response = "अभी " + hour12 + " बजे हैं।";
            } else {
                response = "अभी " + hour12 + " बजकर " + minute + " मिनट हुए हैं।";
            }
        } else if ("hinglish".equals(lang)) {
            if (minute == 0) {
                response = "Abhi " + hour12 + " baje hain.";
            } else {
                response = "Abhi " + hour12 + " bajkar " + minute + " minute hue hain.";
            }
        } else {
            response = "It's " + hour12 + ":" + String.format(Locale.US, "%02d", minute) + " " + amPmStr + ".";
        }

        speakDirectly(response, "COMMAND_RESPONSE");
    }

    private void speakUnsupportedResponse() {
        String lang = PreferencesHelper.getLanguage(this);
        String response;

        if ("hi".equals(lang)) {
            response = "मैं अभी सिर्फ समय बता सकता हूँ।";
        } else if ("hinglish".equals(lang)) {
            response = "Main abhi sirf time bata sakta hoon.";
        } else {
            response = "I can currently only tell you the time.";
        }

        speakDirectly(response, "COMMAND_RESPONSE");
    }

    private void speakDirectly(String text, String utteranceId) {
        if (tts == null || !isTtsInitialized) {
            transitionToState(VoiceState.WAKE_WORD_LISTENING);
            return;
        }

        transitionToState(VoiceState.SPEAKING);
        sendUpdateBroadcast(currentState.name(), "Speaking response: \"" + text + "\"");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
        } else {
            HashMap<String, String> params = new HashMap<>();
            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, params);
        }
    }
}