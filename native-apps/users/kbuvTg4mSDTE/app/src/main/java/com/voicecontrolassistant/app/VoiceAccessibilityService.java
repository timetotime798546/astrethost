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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class VoiceAccessibilityService extends AccessibilityService {

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

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Handled reactively on voice triggers.
    }

    @Override
    public void onInterrupt() {
        // Handle interruptions safely.
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

        // Add Drag and Drop & Touch listener combined
        floatingView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingView, params);
                        return true;
                    case MotionEvent.ACTION_UP:
                        float deltaX = Math.abs(event.getRawX() - initialTouchX);
                        float deltaY = Math.abs(event.getRawY() - initialTouchY);
                        if (deltaX < 15 && deltaY < 15) {
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
                speechRecognizer.startListening(intent);
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

    private void processCommand(String commandText) {
        final String query = commandText.toLowerCase().trim();

        // 1. GLOBAL GESTURES / CONTROLS
        if (query.equals("go back") || query.equals("back") || query.equals("pichhe jao") || query.equals("piche jao")) {
            performGlobalAction(GLOBAL_ACTION_BACK);
            displayFloatingStatus("Going back");
            return;
        }
        if (query.equals("go home") || query.equals("home") || query.equals("screen par jao")) {
            performGlobalAction(GLOBAL_ACTION_HOME);
            displayFloatingStatus("Going home");
            return;
        }
        if (query.contains("scroll down") || query.contains("niche scroll") || query.contains("niche jao")) {
            performScroll(true);
            return;
        }
        if (query.contains("scroll up") || query.contains("upar scroll") || query.contains("upar jao")) {
            performScroll(false);
            return;
        }

        // 2. MULTI-STEP COMMAND: "Search [App] for [Query]"
        if (query.startsWith("search ") && query.contains(" for ")) {
            int searchIdx = query.indexOf("search ");
            int forIdx = query.indexOf(" for ");
            String appName = query.substring(searchIdx + 7, forIdx).trim();
            String searchQuery = query.substring(forIdx + 5).trim();
            executeMultiStepSearch(appName, searchQuery);
            return;
        }

        // Hinglish parser support: "<app> par/me <query> search karo"
        if (query.contains(" par ") || query.contains(" me ")) {
            String appName = "";
            String searchQuery = "";
            if (query.contains(" par ")) {
                int parIdx = query.indexOf(" par ");
                appName = query.substring(0, parIdx).trim();
                searchQuery = query.substring(parIdx + 5).replace("search karo", "").replace("search", "").replace("dhundo", "").trim();
            } else if (query.contains(" me ")) {
                int meIdx = query.indexOf(" me ");
                appName = query.substring(0, meIdx).trim();
                searchQuery = query.substring(meIdx + 4).replace("search karo", "").replace("search", "").replace("dhundo", "").trim();
            }
            if (!appName.isEmpty() && !searchQuery.isEmpty()) {
                executeMultiStepSearch(appName, searchQuery);
                return;
            }
        }

        // 3. OPEN APP COMMAND
        if (query.startsWith("open ") || query.startsWith("kholo ") || query.startsWith("chalao ")) {
            String appName = query.replace("open ", "").replace("kholo ", "").replace("chalao ", "").trim();
            boolean success = launchAppByName(appName);
            if (success) {
                displayFloatingStatus("Launching " + appName);
            } else {
                displayFloatingStatus("App not found: " + appName);
            }
            return;
        }

        // 4. TYPE TEXT
        if (query.startsWith("type ") || query.startsWith("likho ")) {
            String textToType = commandText.substring(commandText.indexOf(" ") + 1).trim();
            typeOnScreen(textToType);
            return;
        }

        // 5. CLICK ELEMENT
        if (query.startsWith("click ") || query.startsWith("touch ") || query.startsWith("dabao ")) {
            String targetText = commandText.substring(commandText.indexOf(" ") + 1).trim();
            clickOnScreenElement(targetText);
            return;
        }

        // Fallback search match to find and click element containing text
        clickOnScreenElement(commandText);
    }

    private boolean launchAppByName(String appName) {
        String cleanName = appName.toLowerCase().trim();
        PackageManager pm = getPackageManager();

        // Direct package launches for high accuracy of popular targets
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

        // Fallback package querying scan
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

    private void performScroll(boolean scrollDown) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            displayFloatingStatus("No screen root context found");
            return;
        }
        List<AccessibilityNodeInfo> scrollableNodes = findScrollableNodes(root);
        root.recycle();

        if (!scrollableNodes.isEmpty()) {
            boolean scrolled = false;
            for (AccessibilityNodeInfo scrollNode : scrollableNodes) {
                int action = scrollDown ? AccessibilityNodeInfo.ACTION_SCROLL_FORWARD : AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD;
                if (scrollNode.performAction(action)) {
                    scrolled = true;
                    scrollNode.recycle();
                    break;
                }
                scrollNode.recycle();
            }
            if (scrolled) {
                displayFloatingStatus(scrollDown ? "Scrolled Down" : "Scrolled Up");
            } else {
                displayFloatingStatus("Could not scroll element");
            }
        } else {
            displayFloatingStatus("No scrollable container found");
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
                AccessibilityNodeInfo root = getRootInActiveWindow();
                if (root == null) {
                    displayFloatingStatus("Could not read screen to search");
                    return;
                }

                AccessibilityNodeInfo searchNode = findSearchNode(root);
                root.recycle();

                if (searchNode != null) {
                    performClickAction(searchNode);
                    searchNode.recycle();
                    displayFloatingStatus("Clicked search. Waiting...");

                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            AccessibilityNodeInfo currentRoot = getRootInActiveWindow();
                            if (currentRoot == null) return;

                            AccessibilityNodeInfo editNode = findFocusedOrEditableNode(currentRoot);
                            currentRoot.recycle();

                            if (editNode != null) {
                                typeTextInNode(editNode, searchQuery);
                                editNode.recycle();
                                displayFloatingStatus("Typed: " + searchQuery);

                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        AccessibilityNodeInfo lastRoot = getRootInActiveWindow();
                                        if (lastRoot == null) return;

                                        AccessibilityNodeInfo goBtn = findSearchSubmitButton(lastRoot);
                                        if (goBtn != null) {
                                            performClickAction(goBtn);
                                            goBtn.recycle();
                                            displayFloatingStatus("Searching...");
                                        } else {
                                            AccessibilityNodeInfo finalEdit = findFocusedOrEditableNode(lastRoot);
                                            if (finalEdit != null) {
                                                finalEdit.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                                                finalEdit.recycle();
                                            }
                                            displayFloatingStatus("Searching...");
                                        }
                                        lastRoot.recycle();
                                    }
                                }, 1000);
                            } else {
                                displayFloatingStatus("Could not find input field");
                            }
                        }
                    }, 1500);
                } else {
                    displayFloatingStatus("Could not find search icon");
                }
            }
        }, 3000);
    }

    private void clickOnScreenElement(String text) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            displayFloatingStatus("Active screen content unavailable");
            return;
        }
        List<AccessibilityNodeInfo> matched = new ArrayList<>();
        findNodesRecursive(root, text.toLowerCase().trim(), matched);
        root.recycle();

        if (!matched.isEmpty()) {
            boolean clicked = false;
            for (AccessibilityNodeInfo node : matched) {
                if (performClickAction(node)) {
                    clicked = true;
                    node.recycle();
                    break;
                }
                node.recycle();
            }
            if (clicked) {
                displayFloatingStatus("Clicked " + text);
            } else {
                displayFloatingStatus("Could not click " + text);
            }
        } else {
            displayFloatingStatus("No visible match: " + text);
        }
    }

    private void typeOnScreen(String text) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        AccessibilityNodeInfo targetNode = findFocusedOrEditableNode(root);
        root.recycle();

        if (targetNode != null) {
            boolean success = typeTextInNode(targetNode, text);
            targetNode.recycle();
            if (success) {
                displayFloatingStatus("Typed: " + text);
            } else {
                displayFloatingStatus("Failed to enter text");
            }
        } else {
            displayFloatingStatus("Focus/edit field not active");
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

        return findFirstEditableNode(root);
    }

    private AccessibilityNodeInfo findFirstEditableNode(AccessibilityNodeInfo root) {
        if (root == null) return null;
        if (root.isEditable()) {
            return AccessibilityNodeInfo.obtain(root);
        }
        for (int i = 0; i < root.getChildCount(); i++) {
            AccessibilityNodeInfo child = root.getChild(i);
            if (child != null) {
                AccessibilityNodeInfo res = findFirstEditableNode(child);
                child.recycle();
                if (res != null) {
                    return res;
                }
            }
        }
        return null;
    }

    private AccessibilityNodeInfo findFocusedOrEditableNode(AccessibilityNodeInfo root) {
        if (root == null) return null;

        AccessibilityNodeInfo focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (focused != null) {
            return focused;
        }

        List<AccessibilityNodeInfo> editables = new ArrayList<>();
        findEditableNodesRecursive(root, editables);
        if (!editables.isEmpty()) {
            AccessibilityNodeInfo result = editables.get(0);
            for (int i = 1; i < editables.size(); i++) {
                editables.get(i).recycle();
            }
            return result;
        }
        return null;
    }

    private void findEditableNodesRecursive(AccessibilityNodeInfo node, List<AccessibilityNodeInfo> results) {
        if (node == null) return;
        CharSequence className = node.getClassName();
        boolean isEditText = className != null && className.toString().contains("EditText");
        if (node.isEditable() || node.isFocused() || isEditText) {
            results.add(AccessibilityNodeInfo.obtain(node));
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                findEditableNodesRecursive(child, results);
                child.recycle();
            }
        }
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

    private boolean performClickAction(AccessibilityNodeInfo node) {
        if (node == null) return false;
        if (node.isClickable()) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
        AccessibilityNodeInfo parent = node.getParent();
        if (parent != null) {
            boolean success = performClickAction(parent);
            parent.recycle();
            return success;
        }
        return false;
    }

    private boolean typeTextInNode(AccessibilityNodeInfo node, String text) {
        if (node == null) return false;

        node.performAction(AccessibilityNodeInfo.FOCUS_INPUT);
        node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);

        Bundle arguments = new Bundle();
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
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