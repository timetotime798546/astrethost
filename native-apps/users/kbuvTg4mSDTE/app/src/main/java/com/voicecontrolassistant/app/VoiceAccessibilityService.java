package com.voicecontrolassistant.app;

import android.accessibilityservice.AccessibilityService;
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

public class VoiceAccessibilityService extends AccessibilityService {

    private static final String TAG = "VoiceAssistant";

    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;
    private FrameLayout micContainer;
    private TextView micText;
    private TextView statusTextOverlay;

    private SpeechRecognizer speechRecognizer;
    private boolean isListening = false;
    private final Handler statusHandler = new Handler(Looper.getMainLooper());
    private Runnable statusRunnable;

    // Structured Voice Command Container
    private static class Command {
        String action; // CLICK, TYPE, SEARCH_APP, SCROLL_DOWN, SCROLL_UP, GLOBAL_BACK, GLOBAL_HOME, OPEN_APP
        String target; // Name of app or target button/field/label
        String query;  // Text to type or search query
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || 
            eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            CharSequence packageName = event.getPackageName();
            if (packageName != null) {
                Log.d(TAG, "Window updated or changed for package: " + packageName.toString());
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
        params.x = 120;
        params.y = 200;

        micContainer = (FrameLayout) floatingView.findViewById(R.id.mic_container);
        micText = (TextView) floatingView.findViewById(R.id.mic_text);
        statusTextOverlay = (TextView) floatingView.findViewById(R.id.status_text_overlay);

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
                            toggleSpeechListening();
                        }
                        return true;
                }
                return false;
            }
        });

        windowManager.addView(floatingView, params);
    }

    private void initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    isListening = true;
                    setMicColorState(true);
                    displayFloatingStatus("Listening...");
                }

                @Override
                public void onBeginningOfSpeech() {}

                @Override
                public void onRmsChanged(float rmsdB) {}

                @Override
                public void onBufferReceived(byte[] buffer) {}

                @Override
                public void onEndOfSpeech() {
                    isListening = false;
                    setMicColorState(false);
                }

                @Override
                public void onError(int error) {
                    isListening = false;
                    setMicColorState(false);
                    displayFloatingStatus("Error: Try again");
                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String recognizedText = matches.get(0);
                        displayFloatingStatus("\"" + recognizedText + "\"");
                        processCommand(recognizedText);
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                    ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        statusTextOverlay.setText(matches.get(0));
                    }
                }

                @Override
                public void onEvent(int eventType, Bundle params) {}
            });
        } else {
            displayFloatingStatus("Speech not supported");
        }
    }

    private void toggleSpeechListening() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                displayFloatingStatus("Mic permission required! Open main app.");
                return;
            }
        }

        if (isListening) {
            speechRecognizer.stopListening();
            isListening = false;
            setMicColorState(false);
        } else {
            if (speechRecognizer == null) {
                initializeSpeechRecognizer();
            }
            if (speechRecognizer != null) {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
                intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
                try {
                    speechRecognizer.startListening(intent);
                } catch (Exception e) {
                    displayFloatingStatus("Recognizer Error: " + e.getMessage());
                }
            }
        }
    }

    private void setMicColorState(boolean listening) {
        if (listening) {
            micContainer.setBackgroundResource(R.drawable.circle_listening);
            micText.setText("🎙️");
        } else {
            micContainer.setBackgroundResource(R.drawable.circle_default);
            micText.setText("🎙️");
        }
    }

    private void displayFloatingStatus(String message) {
        if (statusTextOverlay == null) return;
        statusTextOverlay.setVisibility(View.VISIBLE);
        statusTextOverlay.setText(message);

        if (statusRunnable != null) {
            statusHandler.removeCallbacks(statusRunnable);
        }

        statusRunnable = new Runnable() {
            @Override
            public void run() {
                if (statusTextOverlay != null) {
                    statusTextOverlay.setVisibility(View.GONE);
                }
            }
        };
        statusHandler.postDelayed(statusRunnable, 4000);
    }

    // Dynamic Voice Command Analyzer & Parser
    private Command parseVoiceCommand(String rawCommand) {
        Command cmd = new Command();
        String clean = rawCommand.toLowerCase().trim();

        // 1. HOME & BACK GLOBAL ACTIONS
        if (clean.equals("go back") || clean.equals("back") || clean.equals("pichhe jao") || clean.equals("piche jao")) {
            cmd.action = "GLOBAL_BACK";
            return cmd;
        }
        if (clean.equals("go home") || clean.equals("home") || clean.equals("screen par jao")) {
            cmd.action = "GLOBAL_HOME";
            return cmd;
        }

        // 2. SCROLL ENGINE ACTIONS
        if (clean.contains("scroll down") || clean.contains("niche scroll") || clean.contains("niche jao") || clean.equals("scroll niche")) {
            cmd.action = "SCROLL_DOWN";
            return cmd;
        }
        if (clean.contains("scroll up") || clean.contains("upar scroll") || clean.contains("upar jao") || clean.equals("scroll upar")) {
            cmd.action = "SCROLL_UP";
            return cmd;
        }

        // 3. TARGETED APP INTERACTIVE SEARCH
        if (clean.startsWith("search ") && clean.contains(" for ")) {
            int searchIdx = clean.indexOf("search ");
            int forIdx = clean.indexOf(" for ");
            cmd.action = "SEARCH_APP";
            cmd.target = rawCommand.substring(searchIdx + 7, forIdx).trim();
            cmd.query = rawCommand.substring(forIdx + 5).trim();
            return cmd;
        }

        // Hinglish/Alternative Targeted Searches
        if (clean.contains(" par ") || clean.contains(" me ") || clean.contains(" mein ")) {
            String appName = "";
            String searchQuery = "";
            if (clean.contains(" par ")) {
                int idx = clean.indexOf(" par ");
                appName = rawCommand.substring(0, idx).trim();
                searchQuery = rawCommand.substring(idx + 5).replace("search karo", "").replace("search", "").replace("dhundo", "").trim();
            } else if (clean.contains(" me ")) {
                int idx = clean.indexOf(" me ");
                appName = rawCommand.substring(0, idx).trim();
                searchQuery = rawCommand.substring(idx + 4).replace("search karo", "").replace("search", "").replace("dhundo", "").trim();
            } else if (clean.contains(" mein ")) {
                int idx = clean.indexOf(" mein ");
                appName = rawCommand.substring(0, idx).trim();
                searchQuery = rawCommand.substring(idx + 6).replace("search karo", "").replace("search", "").replace("dhundo", "").trim();
            }
            if (!appName.isEmpty() && !searchQuery.isEmpty()) {
                cmd.action = "SEARCH_APP";
                cmd.target = appName;
                cmd.query = searchQuery;
                return cmd;
            }
        }

        // 4. OPEN APP INTERACTION
        if (clean.startsWith("open ") || clean.startsWith("kholo ") || clean.startsWith("chalao ")) {
            cmd.action = "OPEN_APP";
            cmd.target = rawCommand.substring(rawCommand.indexOf(" ") + 1).trim();
            return cmd;
        }

        // 5. TEXT INJECTION / TYPE ENTRY
        if (clean.startsWith("type ") || clean.startsWith("likho ")) {
            cmd.action = "TYPE";
            cmd.query = rawCommand.substring(rawCommand.indexOf(" ") + 1).trim();
            return cmd;
        }

        // 6. GENERAL CLICK TRIGGER
        if (clean.startsWith("click ") || clean.startsWith("touch ") || clean.startsWith("dabao ")) {
            cmd.action = "CLICK";
            cmd.target = rawCommand.substring(rawCommand.indexOf(" ") + 1).trim();
            return cmd;
        }

        // Fallback: Default to finding and clicking raw input string
        cmd.action = "CLICK";
        cmd.target = rawCommand.trim();
        return cmd;
    }

    private void processCommand(String commandText) {
        Command cmd = parseVoiceCommand(commandText);
        if (cmd == null || cmd.action == null) {
            displayFloatingStatus("Could not interpret command");
            return;
        }

        Log.d(TAG, "Executing Action: " + cmd.action + " Target: " + cmd.target + " Query: " + cmd.query);

        switch (cmd.action) {
            case "GLOBAL_BACK":
                performGlobalAction(GLOBAL_ACTION_BACK);
                displayFloatingStatus("Going back");
                break;

            case "GLOBAL_HOME":
                performGlobalAction(GLOBAL_ACTION_HOME);
                displayFloatingStatus("Going home");
                break;

            case "SCROLL_DOWN":
                performScroll(true);
                break;

            case "SCROLL_UP":
                performScroll(false);
                break;

            case "SEARCH_APP":
                executeMultiStepSearch(cmd.target, cmd.query);
                break;

            case "OPEN_APP":
                boolean launchSuccess = launchAppByName(cmd.target);
                if (launchSuccess) {
                    displayFloatingStatus("Launching " + cmd.target);
                } else {
                    displayFloatingStatus("App not found: " + cmd.target);
                }
                break;

            case "TYPE":
                performTypeWithRetry(cmd.query, 1, 3);
                break;

            case "CLICK":
                performClickWithRetry(cmd.target, 1, 3);
                break;

            default:
                displayFloatingStatus("Command execution failed");
                break;
        }
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

    // Recursive UI Node Finder using multiple matched attributes
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

    // Bubble up strategy to discover and click appropriate clickable parent container
    private boolean clickNodeOrParent(AccessibilityNodeInfo node) {
        if (node == null) return false;
        AccessibilityNodeInfo current = AccessibilityNodeInfo.obtain(node);
        while (current != null) {
            logNodeDetails("Checking click compatibility on:", current);
            if (current.isClickable()) {
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

    // Click UI node with dynamic refreshing & attempt retries
    private void performClickWithRetry(final String target, final int attempt, final int maxAttempts) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            if (attempt < maxAttempts) {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        performClickWithRetry(target, attempt + 1, maxAttempts);
                    }
                }, 600);
            } else {
                displayFloatingStatus("Content layer unavailable");
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
                displayFloatingStatus("Clicked " + target);
            } else {
                if (attempt < maxAttempts) {
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            performClickWithRetry(target, attempt + 1, maxAttempts);
                        }
                    }, 600);
                } else {
                    displayFloatingStatus("Click failed on " + target);
                }
            }
        } else {
            if (attempt < maxAttempts) {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        performClickWithRetry(target, attempt + 1, maxAttempts);
                    }
                }, 600);
            } else {
                displayFloatingStatus("No matched element found: " + target);
            }
        }
    }

    // Type text safely into focused or newly discovered input node
    private void performTypeWithRetry(final String text, final int attempt, final int maxAttempts) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            if (attempt < maxAttempts) {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        performTypeWithRetry(text, attempt + 1, maxAttempts);
                    }
                }, 600);
            } else {
                displayFloatingStatus("Screen reading failed");
            }
            return;
        }

        AccessibilityNodeInfo inputNode = findEditableNode(root);
        root.recycle();

        if (inputNode != null) {
            boolean success = typeTextInNode(inputNode, text);
            inputNode.recycle();

            if (success) {
                displayFloatingStatus("Typed: " + text);
            } else {
                if (attempt < maxAttempts) {
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            performTypeWithRetry(text, attempt + 1, maxAttempts);
                        }
                    }, 600);
                } else {
                    displayFloatingStatus("Typing process failed");
                }
            }
        } else {
            if (attempt < maxAttempts) {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        performTypeWithRetry(text, attempt + 1, maxAttempts);
                    }
                }, 600);
            } else {
                displayFloatingStatus("No editable input field present");
            }
        }
    }

    private boolean typeTextInNode(AccessibilityNodeInfo node, String text) {
        if (node == null) return false;
        node.performAction(AccessibilityNodeInfo.FOCUS_INPUT);
        node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);

        Bundle arguments = new Bundle();
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
    }

    // Adaptive search configuration for fields
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
        if (node.isEditable() || classStr.contains("EditText") || node.isFocused()) {
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

    // Dynamic Scrolling Engine
    private void performScroll(boolean scrollDown) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            displayFloatingStatus("No active window to scroll");
            return;
        }
        List<AccessibilityNodeInfo> scrollableNodes = findScrollableNodes(root);
        root.recycle();

        if (!scrollableNodes.isEmpty()) {
            boolean scrolled = false;
            for (AccessibilityNodeInfo scrollNode : scrollableNodes) {
                if (!scrolled) {
                    int action = scrollDown ? AccessibilityNodeInfo.ACTION_SCROLL_FORWARD : AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD;
                    logNodeDetails("Scrolling node element:", scrollNode);
                    if (scrollNode.performAction(action)) {
                        scrolled = true;
                        displayFloatingStatus(scrollDown ? "Scrolled Down" : "Scrolled Up");
                    }
                }
                scrollNode.recycle();
            }
            if (!scrolled) {
                displayFloatingStatus("Target refuses to scroll");
            }
        } else {
            displayFloatingStatus("No scrollable viewport detected");
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
                classStr.contains("viewpager");

        if (node.isScrollable() || isScrollClass) {
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

    // Multi-Step Automation Search pipeline with robust wait cycles
    private void executeMultiStepSearch(final String appName, final String searchQuery) {
        displayFloatingStatus("Opening " + appName);
        boolean launched = launchAppByName(appName);
        if (!launched) {
            displayFloatingStatus("Failed: " + appName + " not found");
            return;
        }

        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                clickSearchButtonWithRetry(searchQuery, 1, 5);
            }
        }, 2500);
    }

    private void clickSearchButtonWithRetry(final String searchQuery, final int attempt, final int maxAttempts) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            if (attempt < maxAttempts) {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        clickSearchButtonWithRetry(searchQuery, attempt + 1, maxAttempts);
                    }
                }, 600);
            } else {
                displayFloatingStatus("Could not find loaded window");
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
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        typeSearchQueryWithRetry(searchQuery, 1, 5);
                    }
                }, 1200);
            } else {
                retryClickSearch(searchQuery, attempt, maxAttempts);
            }
        } else {
            retryClickSearch(searchQuery, attempt, maxAttempts);
        }
    }

    private void retryClickSearch(final String searchQuery, final int attempt, final int maxAttempts) {
        if (attempt < maxAttempts) {
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    clickSearchButtonWithRetry(searchQuery, attempt + 1, maxAttempts);
                }
            }, 600);
        } else {
            displayFloatingStatus("Could not find search button");
        }
    }

    private void typeSearchQueryWithRetry(final String searchQuery, final int attempt, final int maxAttempts) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            if (attempt < maxAttempts) {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        typeSearchQueryWithRetry(searchQuery, attempt + 1, maxAttempts);
                    }
                }, 600);
            }
            return;
        }

        AccessibilityNodeInfo editNode = findEditableNode(root);
        root.recycle();

        if (editNode != null) {
            boolean typed = typeTextInNode(editNode, searchQuery);
            editNode.recycle();
            if (typed) {
                displayFloatingStatus("Typed: " + searchQuery);
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        submitSearchWithRetry(1, 4);
                    }
                }, 1000);
            } else {
                retryTypeQuery(searchQuery, attempt, maxAttempts);
            }
        } else {
            retryTypeQuery(searchQuery, attempt, maxAttempts);
        }
    }

    private void retryTypeQuery(final String searchQuery, final int attempt, final int maxAttempts) {
        if (attempt < maxAttempts) {
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    typeSearchQueryWithRetry(searchQuery, attempt + 1, maxAttempts);
                }
            }, 600);
        } else {
            displayFloatingStatus("Could not find search input field");
        }
    }

    private void submitSearchWithRetry(final int attempt, final int maxAttempts) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            if (attempt < maxAttempts) {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        submitSearchWithRetry(attempt + 1, maxAttempts);
                    }
                }, 600);
            }
            return;
        }

        AccessibilityNodeInfo goBtn = findSearchSubmitButton(root);
        root.recycle();

        if (goBtn != null) {
            boolean success = clickNodeOrParent(goBtn);
            goBtn.recycle();
            if (success) {
                displayFloatingStatus("Searching...");
            } else {
                retrySubmit(attempt, maxAttempts);
            }
        } else {
            retrySubmit(attempt, maxAttempts);
        }
    }

    private void retrySubmit(final int attempt, final int maxAttempts) {
        if (attempt < maxAttempts) {
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    submitSearchWithRetry(attempt + 1, maxAttempts);
                }
            }, 600);
        } else {
            displayFloatingStatus("Search command complete");
        }
    }

    private AccessibilityNodeInfo findSearchNode(AccessibilityNodeInfo root) {
        if (root == null) return null;
        List<AccessibilityNodeInfo> matches = new ArrayList<>();
        findNodesRecursive(root, "search", matches);
        if (!matches.isEmpty()) {
            AccessibilityNodeInfo bestMatch = matches.get(0);
            for (int i = 1; i < matches.size(); i++) {
                matches.get(i).recycle();
            }
            return bestMatch;
        }

        findNodesRecursive(root, "find", matches);
        if (!matches.isEmpty()) {
            AccessibilityNodeInfo bestMatch = matches.get(0);
            for (int i = 1; i < matches.size(); i++) {
                matches.get(i).recycle();
            }
            return bestMatch;
        }

        AccessibilityNodeInfo searchById = findNodeByViewIdSubstring(root, "search");
        if (searchById != null) {
            return searchById;
        }

        return findFirstEditableNodeRecursive(root);
    }

    private AccessibilityNodeInfo findNodeByViewIdSubstring(AccessibilityNodeInfo node, String substring) {
        if (node == null) return null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            String viewId = node.getViewIdResourceName();
            if (viewId != null && viewId.toLowerCase().contains(substring)) {
                return AccessibilityNodeInfo.obtain(node);
            }
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                AccessibilityNodeInfo result = findNodeByViewIdSubstring(child, substring);
                child.recycle();
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private AccessibilityNodeInfo findSearchSubmitButton(AccessibilityNodeInfo root) {
        if (root == null) return null;
        List<AccessibilityNodeInfo> matches = new ArrayList<>();
        findSearchSubmitButtonsRecursive(root, matches);
        if (!matches.isEmpty()) {
            AccessibilityNodeInfo result = matches.get(0);
            for (int i = 1; i < matches.size(); i++) {
                matches.get(i).recycle();
            }
            return result;
        }
        return null;
    }

    private void findSearchSubmitButtonsRecursive(AccessibilityNodeInfo node, List<AccessibilityNodeInfo> result) {
        if (node == null) return;
        CharSequence text = node.getText();
        CharSequence desc = node.getContentDescription();
        String txt = text != null ? text.toString().toLowerCase() : "";
        String dsc = desc != null ? desc.toString().toLowerCase() : "";
        if (node.isClickable() && (txt.contains("search") || dsc.contains("search") || txt.equals("go") || txt.equals("find") || dsc.contains("submit"))) {
            result.add(AccessibilityNodeInfo.obtain(node));
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                findSearchSubmitButtonsRecursive(child, result);
                child.recycle();
            }
        }
    }

    private void findNodesRecursive(AccessibilityNodeInfo node, String query, List<AccessibilityNodeInfo> results) {
        if (node == null) return;

        CharSequence text = node.getText();
        if (text != null && text.toString().toLowerCase().contains(query)) {
            results.add(AccessibilityNodeInfo.obtain(node));
        } else {
            CharSequence desc = node.getContentDescription();
            if (desc != null && desc.toString().toLowerCase().contains(query)) {
                results.add(AccessibilityNodeInfo.obtain(node));
            } else {
                String viewId = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    viewId = node.getViewIdResourceName();
                }
                if (viewId != null && viewId.toLowerCase().contains(query)) {
                    results.add(AccessibilityNodeInfo.obtain(node));
                }
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                findNodesRecursive(child, query, results);
                child.recycle();
            }
        }
    }

    // Diagnostic Tree Node detail logger
    private void logNodeDetails(String prefix, AccessibilityNodeInfo node) {
        if (node == null) return;
        CharSequence className = node.getClassName();
        CharSequence text = node.getText();
        CharSequence contentDesc = node.getContentDescription();
        String viewId = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            viewId = node.getViewIdResourceName();
        }
        Log.d(TAG, prefix + " Class=" + className + 
            ", Text=" + text + 
            ", Desc=" + contentDesc + 
            ", Id=" + viewId + 
            ", Clickable=" + node.isClickable() + 
            ", Focusable=" + node.isFocusable() + 
            ", Editable=" + node.isEditable() + 
            ", Scrollable=" + node.isScrollable());
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
    }
}