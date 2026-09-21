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

public class VoiceAccessibilityService extends AccessibilityService implements RecognitionListener {

    private static final String TAG = "VoiceAssistant";
    private static final long SESSION_TIMEOUT_MS = 600000; // Exactly 10 minutes

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

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Handler sessionTimeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable statusRunnable;
    private ValueAnimator orbAnimator;

    private static class Command {
        String action; // CLICK, TYPE, SEARCH_APP, SCROLL_DOWN, SCROLL_UP, GLOBAL_BACK, GLOBAL_HOME, OPEN_APP, SUBMIT
        String target;
        String query;
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
        int eventType = event.getEventType();
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || 
            eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
            eventType == AccessibilityEvent.TYPE_VIEW_CLICKED ||
            eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED ||
            eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            
            CharSequence packageName = event.getPackageName();
            if (packageName != null) {
                Log.d(TAG, "Accessibility Tree Update: " + packageName.toString());
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
        if (isSessionActive) {
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
            setOrbState("processing");
            processCommand(recognizedText);
        } else {
            startSpeechRecognizerListening();
        }
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) {
            statusTextOverlay.setVisibility(View.VISIBLE);
            statusTextOverlay.setText(matches.get(0));
        }
    }

    @Override
    public void onEvent(int eventType, Bundle params) {}

    private boolean isVoiceControlElement(String text, String desc, String viewId) {
        String t = text != null ? text.toLowerCase() : "";
        String d = desc != null ? desc.toLowerCase() : "";
        String id = viewId != null ? viewId.toLowerCase() : "";
        return t.contains("mic") || t.contains("voice") || t.contains("speak") || t.contains("audio") || t.contains("record")
                || d.contains("mic") || d.contains("voice") || d.contains("speak") || d.contains("audio") || d.contains("record")
                || id.contains("mic") || id.contains("voice") || id.contains("speak") || id.contains("audio") || id.contains("record");
    }

    private Command parseVoiceCommand(String rawCommand) {
        Command cmd = new Command();
        String clean = rawCommand.toLowerCase().trim();

        // 1. GLOBAL ACTIONS
        if (clean.equals("go back") || clean.equals("back") || clean.equals("pichhe jao") || clean.equals("piche jao") || clean.equals("vaapas")) {
            cmd.action = "GLOBAL_BACK";
            return cmd;
        }
        if (clean.equals("go home") || clean.equals("home") || clean.equals("screen par jao") || clean.equals("home screen")) {
            cmd.action = "GLOBAL_HOME";
            return cmd;
        }

        // 2. SCROLL ENGINE ACTIONS
        if (clean.contains("scroll down") || clean.contains("niche scroll") || clean.contains("niche jao") || clean.equals("scroll niche") || clean.contains("down more")) {
            cmd.action = "SCROLL_DOWN";
            return cmd;
        }
        if (clean.contains("scroll up") || clean.contains("upar scroll") || clean.contains("upar jao") || clean.equals("scroll upar") || clean.contains("up more")) {
            cmd.action = "SCROLL_UP";
            return cmd;
        }

        // 3. ENTER / SUBMIT / DONE ACTIONS
        if (clean.equals("enter") || clean.equals("press enter") || clean.equals("search") || clean.equals("submit") || clean.equals("go") || clean.equals("done")) {
            cmd.action = "SUBMIT";
            return cmd;
        }

        // 4. COMPLEX SEARCH COMMANDS (English + Hinglish)
        if (clean.startsWith("search ") && clean.contains(" for ")) {
            int searchIdx = clean.indexOf("search ");
            int forIdx = clean.indexOf(" for ");
            cmd.action = "SEARCH_APP";
            cmd.target = rawCommand.substring(searchIdx + 7, forIdx).trim();
            cmd.query = rawCommand.substring(forIdx + 5).trim();
            return cmd;
        }

        if (clean.contains(" par ") && (clean.contains("search") || clean.contains("dhundo"))) {
            int parIdx = clean.indexOf(" par ");
            cmd.action = "SEARCH_APP";
            cmd.target = rawCommand.substring(0, parIdx).trim();
            String q = rawCommand.substring(parIdx + 5).trim();
            q = q.replace("search", "").replace("karo", "").replace("dhundo", "").trim();
            cmd.query = q;
            return cmd;
        }

        // 5. OPEN APPLICATION
        if (clean.startsWith("open ") || clean.startsWith("launch ") || clean.startsWith("start ")) {
            cmd.action = "OPEN_APP";
            cmd.target = rawCommand.substring(rawCommand.indexOf(" ") + 1).trim();
            return cmd;
        }
        if (clean.endsWith(" kholo") || clean.endsWith(" chalao")) {
            cmd.action = "OPEN_APP";
            String targetStr = rawCommand;
            if (clean.endsWith(" kholo")) {
                targetStr = rawCommand.substring(0, rawCommand.length() - 6).trim();
            } else if (clean.endsWith(" chalao")) {
                targetStr = rawCommand.substring(0, rawCommand.length() - 7).trim();
            }
            cmd.target = targetStr;
            return cmd;
        }

        // 6. TEXT TYPING INJECTION WITH AUTOMATIC SUBMISSION
        if (clean.startsWith("type ") || clean.startsWith("write ")) {
            cmd.action = "TYPE";
            cmd.query = rawCommand.substring(rawCommand.indexOf(" ") + 1).trim();
            return cmd;
        }
        if (clean.startsWith("likho ")) {
            cmd.action = "TYPE";
            cmd.query = rawCommand.substring(6).trim();
            return cmd;
        }

        // 7. GENERAL CLICK ACTIONS
        if (clean.startsWith("click ") || clean.startsWith("tap ") || clean.startsWith("touch ") || clean.startsWith("dabao ")) {
            cmd.action = "CLICK";
            cmd.target = rawCommand.substring(rawCommand.indexOf(" ") + 1).trim();
            return cmd;
        }

        // Fallback: Click Target matching raw unrecognized text
        cmd.action = "CLICK";
        cmd.target = rawCommand.trim();
        return cmd;
    }

    private void processCommand(String commandText) {
        Command cmd = parseVoiceCommand(commandText);
        if (cmd == null || cmd.action == null) {
            displayFloatingStatus("Interpretation failed");
            setOrbState("idle");
            return;
        }

        Log.d(TAG, "Parsed: Action=" + cmd.action + " Target=" + cmd.target + " Query=" + cmd.query);

        switch (cmd.action) {
            case "GLOBAL_BACK":
                performGlobalAction(GLOBAL_ACTION_BACK);
                displayFloatingStatus("Back executed");
                returnOrbToReadyDelayed();
                break;

            case "GLOBAL_HOME":
                performGlobalAction(GLOBAL_ACTION_HOME);
                displayFloatingStatus("Home executed");
                returnOrbToReadyDelayed();
                break;

            case "SCROLL_DOWN":
                performScroll(true);
                break;

            case "SCROLL_UP":
                performScroll(false);
                break;

            case "SUBMIT":
                performSubmitAction();
                break;

            case "SEARCH_APP":
                executeSearchPipeline(cmd.target, cmd.query);
                break;

            case "OPEN_APP":
                boolean opened = launchAppByName(cmd.target);
                if (opened) {
                    displayFloatingStatus("Launching " + cmd.target);
                } else {
                    displayFloatingStatus("App not found: " + cmd.target);
                }
                returnOrbToReadyDelayed();
                break;

            case "TYPE":
                performTypeAndSubmitWithRetry(cmd.query, 1, 3);
                break;

            case "CLICK":
                performClickWithRetry(cmd.target, 1, 3);
                break;

            default:
                setOrbState("idle");
                break;
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

    private boolean launchAppByName(String appName) {
        String cleanName = appName.toLowerCase().trim();
        PackageManager pm = getPackageManager();

        String targetPackage = null;
        if (cleanName.contains("youtube")) {
            targetPackage = "com.google.android.youtube";
        } else if (cleanName.contains("chrome")) {
            targetPackage = "com.android.chrome";
        } else if (cleanName.contains("maps")) {
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

        boolean matchFound = false;

        CharSequence text = node.getText();
        if (text != null && text.toString().toLowerCase().contains(target)) {
            matchFound = true;
        }

        if (!matchFound) {
            CharSequence desc = node.getContentDescription();
            if (desc != null && desc.toString().toLowerCase().contains(target)) {
                matchFound = true;
            }
        }

        if (!matchFound && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            String viewId = node.getViewIdResourceName();
            if (viewId != null && viewId.toLowerCase().contains(target)) {
                matchFound = true;
            }
        }

        if (!matchFound) {
            CharSequence className = node.getClassName();
            if (className != null && className.toString().toLowerCase().endsWith(target)) {
                matchFound = true;
            }
        }

        if (matchFound) {
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

    private void performClickWithRetry(final String target, final int attempt, final int maxAttempts) {
        AccessibilityNodeInfo rootBefore = getRootInActiveWindow();
        final String stateBefore;
        if (rootBefore != null) {
            StringBuilder sbBefore = new StringBuilder();
            getVisibleContentState(rootBefore, sbBefore);
            stateBefore = sbBefore.toString();
            rootBefore.recycle();
        } else {
            stateBefore = "";
        }

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            if (attempt < maxAttempts) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        performClickWithRetry(target, attempt + 1, maxAttempts);
                    }
                }, 600);
            } else {
                displayFloatingStatus("Active UI root invalid");
                setOrbState("error");
                returnOrbToReadyDelayed();
            }
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
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        AccessibilityNodeInfo rootAfter = getRootInActiveWindow();
                        if (rootAfter != null) {
                            StringBuilder sbAfter = new StringBuilder();
                            getVisibleContentState(rootAfter, sbAfter);
                            String stateAfter = sbAfter.toString();
                            rootAfter.recycle();

                            if (!stateBefore.equals(stateAfter)) {
                                displayFloatingStatus("Clicked " + target + " (UI updated)");
                            } else {
                                displayFloatingStatus("Clicked " + target);
                            }
                        } else {
                            displayFloatingStatus("Clicked " + target);
                        }
                        returnOrbToReadyDelayed();
                    }
                }, 800);
            } else {
                retryClick(target, attempt, maxAttempts);
            }
        } else {
            retryClick(target, attempt, maxAttempts);
        }
    }

    private void retryClick(final String target, final int attempt, final int maxAttempts) {
        if (attempt < maxAttempts) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    performClickWithRetry(target, attempt + 1, maxAttempts);
                }
            }, 600);
        } else {
            displayFloatingStatus("Cannot click " + target);
            setOrbState("error");
            returnOrbToReadyDelayed();
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

    private void performTypeAndSubmitWithRetry(final String text, final int attempt, final int maxAttempts) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            if (attempt < maxAttempts) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        performTypeAndSubmitWithRetry(text, attempt + 1, maxAttempts);
                    }
                }, 600);
            } else {
                displayFloatingStatus("Type UI missing");
                setOrbState("error");
                returnOrbToReadyDelayed();
            }
            return;
        }

        final AccessibilityNodeInfo editNode = findEditableNode(root);
        root.recycle();

        if (editNode != null) {
            boolean typed = typeTextInNode(editNode, text);
            if (typed) {
                displayFloatingStatus("Typed: " + text);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        submitAfterTyping(editNode, text);
                    }
                }, 400);
            } else {
                editNode.recycle();
                retryTypeAndSubmit(text, attempt, maxAttempts);
            }
        } else {
            retryTypeAndSubmit(text, attempt, maxAttempts);
        }
    }

    private void retryTypeAndSubmit(final String text, final int attempt, final int maxAttempts) {
        if (attempt < maxAttempts) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    performTypeAndSubmitWithRetry(text, attempt + 1, maxAttempts);
                }
            }, 600);
        } else {
            displayFloatingStatus("Editable field not found");
            setOrbState("error");
            returnOrbToReadyDelayed();
        }
    }

    private void submitAfterTyping(AccessibilityNodeInfo editNode, final String typedText) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        boolean submitted = false;

        if (root != null) {
            AccessibilityNodeInfo submitBtn = findSearchSubmitButton(root);
            if (submitBtn != null) {
                submitted = clickNodeOrParent(submitBtn);
                submitBtn.recycle();
            }
            root.recycle();
        }

        if (submitted) {
            displayFloatingStatus("Submitted successfully");
            verifyUIChangedAfterDelay();
            if (editNode != null) {
                editNode.recycle();
            }
            return;
        }

        if (editNode != null) {
            editNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                submitted = editNode.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.getId());
            }
            editNode.recycle();
        }

        if (submitted) {
            displayFloatingStatus("Submitted via IME");
        } else {
            displayFloatingStatus("Text injected");
        }
        verifyUIChangedAfterDelay();
    }

    private void verifyUIChangedAfterDelay() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                AccessibilityNodeInfo root = getRootInActiveWindow();
                if (root != null) {
                    displayFloatingStatus("Action Verified");
                    root.recycle();
                }
                returnOrbToReadyDelayed();
            }
        }, 1200);
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

    private void performScroll(final boolean scrollDown) {
        performScrollWithRetry(scrollDown, 1, 2);
    }

    private void performScrollWithRetry(final boolean scrollDown, final int attempt, final int maxAttempts) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            if (attempt < maxAttempts) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        performScrollWithRetry(scrollDown, attempt + 1, maxAttempts);
                    }
                }, 500);
            } else {
                displayFloatingStatus("Scrollable content not found");
                setOrbState("error");
                returnOrbToReadyDelayed();
            }
            return;
        }

        StringBuilder sbBefore = new StringBuilder();
        getVisibleContentState(root, sbBefore);
        final String stateBefore = sbBefore.toString();

        List<AccessibilityNodeInfo> scrollableNodes = findScrollableNodes(root);
        if (scrollableNodes.isEmpty()) {
            root.recycle();
            displayFloatingStatus("Scrollable content not found");
            setOrbState("error");
            returnOrbToReadyDelayed();
            return;
        }

        boolean scrolled = false;
        int action = scrollDown ? AccessibilityNodeInfo.ACTION_SCROLL_FORWARD : AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD;

        for (AccessibilityNodeInfo scrollNode : scrollableNodes) {
            if (!scrolled) {
                if (scrollNode.performAction(action)) {
                    scrolled = true;
                } else {
                    AccessibilityNodeInfo parent = scrollNode.getParent();
                    while (parent != null) {
                        if (parent.isScrollable() && parent.isVisibleToUser() && parent.isEnabled()) {
                            if (parent.performAction(action)) {
                                scrolled = true;
                                parent.recycle();
                                break;
                            }
                        }
                        AccessibilityNodeInfo temp = parent.getParent();
                        parent.recycle();
                        parent = temp;
                    }
                }
            }
            scrollNode.recycle();
        }
        root.recycle();

        if (scrolled) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    AccessibilityNodeInfo rootAfter = getRootInActiveWindow();
                    if (rootAfter != null) {
                        StringBuilder sbAfter = new StringBuilder();
                        getVisibleContentState(rootAfter, sbAfter);
                        String stateAfter = sbAfter.toString();
                        rootAfter.recycle();

                        if (!stateBefore.equals(stateAfter)) {
                            displayFloatingStatus(scrollDown ? "Scrolled Down Successfully" : "Scrolled Up Successfully");
                        } else {
                            if (attempt < maxAttempts) {
                                performScrollWithRetry(scrollDown, attempt + 1, maxAttempts);
                            } else {
                                displayFloatingStatus("Scroll limit reached");
                            }
                        }
                    } else {
                        displayFloatingStatus(scrollDown ? "Scrolled Down" : "Scrolled Up");
                    }
                    returnOrbToReadyDelayed();
                }
            }, 600);
        } else {
            if (attempt < maxAttempts) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        performScrollWithRetry(scrollDown, attempt + 1, maxAttempts);
                    }
                }, 500);
            } else {
                displayFloatingStatus("Scroll action failed");
                setOrbState("error");
                returnOrbToReadyDelayed();
            }
        }
    }

    private void getVisibleContentState(AccessibilityNodeInfo node, StringBuilder sb) {
        if (node == null) return;
        if (node.isVisibleToUser()) {
            CharSequence text = node.getText();
            if (text != null) {
                sb.append(text.toString()).append("|");
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                String viewId = node.getViewIdResourceName();
                if (viewId != null) {
                    sb.append(viewId).append("|");
                }
            }
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                getVisibleContentState(child, sb);
                child.recycle();
            }
        }
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
        boolean isScrollClass = classStr.contains("scrollview") ||
                classStr.contains("listview") ||
                classStr.contains("recyclerview") ||
                classStr.contains("gridview") ||
                classStr.contains("webview") ||
                classStr.contains("nestedscrollview");

        if ((node.isScrollable() || isScrollClass) && node.isVisibleToUser() && node.isEnabled()) {
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

    private void performSubmitAction() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            setOrbState("idle");
            return;
        }

        AccessibilityNodeInfo submitBtn = findSearchSubmitButton(root);
        root.recycle();

        if (submitBtn != null) {
            boolean clicked = clickNodeOrParent(submitBtn);
            submitBtn.recycle();
            if (clicked) {
                displayFloatingStatus("Search submitted");
                verifyUIChangedAfterDelay();
                return;
            }
        }
        displayFloatingStatus("Submit controls missing");
        setOrbState("error");
        returnOrbToReadyDelayed();
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

        if (!isVoiceControlElement(txt, dsc, viewId)) {
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

    private void executeSearchPipeline(final String appName, final String searchQuery) {
        displayFloatingStatus("Opening " + appName);
        boolean launched = launchAppByName(appName);
        if (!launched) {
            displayFloatingStatus("Failed to open " + appName);
            setOrbState("error");
            returnOrbToReadyDelayed();
            return;
        }

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                clickSearchIconWithRetry(appName, searchQuery, 1, 5);
            }
        }, 2200);
    }

    private void clickSearchIconWithRetry(final String appName, final String searchQuery, final int attempt, final int maxAttempts) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            if (attempt < maxAttempts) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        clickSearchIconWithRetry(appName, searchQuery, attempt + 1, maxAttempts);
                    }
                }, 600);
            } else {
                displayFloatingStatus("Timeout loading " + appName);
                setOrbState("error");
                returnOrbToReadyDelayed();
            }
            return;
        }

        AccessibilityNodeInfo searchNode = findSearchNode(root);
        root.recycle();

        if (searchNode != null) {
            boolean clicked = clickNodeOrParent(searchNode);
            searchNode.recycle();
            if (clicked) {
                displayFloatingStatus("Clicking Search...");
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        typeSearchInputWithRetry(searchQuery, 1, 5);
                    }
                }, 1200);
            } else {
                retrySearchIcon(appName, searchQuery, attempt, maxAttempts);
            }
        } else {
            retrySearchIcon(appName, searchQuery, attempt, maxAttempts);
        }
    }

    private void retrySearchIcon(final String appName, final String searchQuery, final int attempt, final int maxAttempts) {
        if (attempt < maxAttempts) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    clickSearchIconWithRetry(appName, searchQuery, attempt + 1, maxAttempts);
                }
            }, 600);
        } else {
            displayFloatingStatus("Search button not found");
            setOrbState("error");
            returnOrbToReadyDelayed();
        }
    }

    private void typeSearchInputWithRetry(final String searchQuery, final int attempt, final int maxAttempts) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            if (attempt < maxAttempts) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        typeSearchInputWithRetry(searchQuery, attempt + 1, maxAttempts);
                    }
                }, 600);
            }
            return;
        }

        AccessibilityNodeInfo editNode = findEditableNode(root);
        root.recycle();

        if (editNode != null) {
            boolean success = typeTextInNode(editNode, searchQuery);
            editNode.recycle();
            if (success) {
                displayFloatingStatus("Searching: " + searchQuery);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        performSubmitAction();
                    }
                }, 1000);
            } else {
                retryTypeInput(searchQuery, attempt, maxAttempts);
            }
        } else {
            retryTypeInput(searchQuery, attempt, maxAttempts);
        }
    }

    private void retryTypeInput(final String searchQuery, final int attempt, final int maxAttempts) {
        if (attempt < maxAttempts) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    typeSearchInputWithRetry(searchQuery, attempt + 1, maxAttempts);
                }
            }, 600);
        } else {
            displayFloatingStatus("Search field missing");
            setOrbState("error");
            returnOrbToReadyDelayed();
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

        if (!isVoiceControlElement(txt, dsc, viewId)) {
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
    }
}