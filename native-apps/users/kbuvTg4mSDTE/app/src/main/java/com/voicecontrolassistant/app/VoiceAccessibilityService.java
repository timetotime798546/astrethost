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
        // Handled reactively on voice triggers instead.
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

        // Hinglish parser template support: "<app> par/me <query> search karo"
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
        PackageManager pm = getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(mainIntent, 0);
        for (ResolveInfo info : apps) {
            String label = info.loadLabel(pm).toString().toLowerCase();
            if (label.contains(appName.toLowerCase())) {
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
        if (root == null) return;
        List<AccessibilityNodeInfo> scrollableNodes = findScrollableNodes(root);
        if (!scrollableNodes.isEmpty()) {
            AccessibilityNodeInfo scrollNode = scrollableNodes.get(0);
            scrollNode.performAction(scrollDown ? AccessibilityNodeInfo.ACTION_SCROLL_FORWARD : AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
            displayFloatingStatus(scrollDown ? "Scrolled Down" : "Scrolled Up");
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
        if (node.isScrollable()) {
            result.add(AccessibilityNodeInfo.obtain(node));
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            findScrollableNodesRecursive(node.getChild(i), result);
        }
    }

    private void executeMultiStepSearch(final String appName, final String searchQuery) {
        displayFloatingStatus("Opening " + appName);
        boolean launched = launchAppByName(appName);
        if (!launched) {
            displayFloatingStatus("Failed: " + appName + " not found");
            return;
        }

        // Wait for launcher loading, then click search bar
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                AccessibilityNodeInfo root = getRootInActiveWindow();
                AccessibilityNodeInfo searchNode = findSearchNode(root);
                if (searchNode != null) {
                    performClickAction(searchNode);
                    displayFloatingStatus("Typing: " + searchQuery);

                    // Wait for soft input active state, then type text
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            AccessibilityNodeInfo currentRoot = getRootInActiveWindow();
                            AccessibilityNodeInfo editNode = findFocusedOrEditableNode(currentRoot);
                            if (editNode != null) {
                                typeTextInNode(editNode, searchQuery);

                                // Perform click on keyboard Search/Go indicator
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        AccessibilityNodeInfo lastRoot = getRootInActiveWindow();
                                        AccessibilityNodeInfo goBtn = findSearchSubmitButton(lastRoot);
                                        if (goBtn != null) {
                                            performClickAction(goBtn);
                                        } else {
                                            displayFloatingStatus("Searching...");
                                        }
                                    }
                                }, 1000);
                            } else {
                                displayFloatingStatus("Error locating active text entry field");
                            }
                        }
                    }, 1500);
                } else {
                    displayFloatingStatus("Searching backup editable elements...");
                }
            }
        }, 3000); // 3s initial timeout wait
    }

    private void clickOnScreenElement(String text) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            displayFloatingStatus("Active screen content unavailable");
            return;
        }
        List<AccessibilityNodeInfo> matched = findNodesByText(root, text);
        if (!matched.isEmpty()) {
            AccessibilityNodeInfo clickNode = matched.get(0);
            boolean success = performClickAction(clickNode);
            if (success) {
                displayFloatingStatus("Clicked " + text);
            } else {
                displayFloatingStatus("Failed to tap matching item");
            }
        } else {
            displayFloatingStatus("No visible text match: " + text);
        }
    }

    private void typeOnScreen(String text) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        AccessibilityNodeInfo targetNode = findFocusedOrEditableNode(root);
        if (targetNode != null) {
            boolean success = typeTextInNode(targetNode, text);
            if (success) {
                displayFloatingStatus("Typed: " + text);
            } else {
                displayFloatingStatus("Failed to enter text");
            }
        } else {
            displayFloatingStatus("Focus an edit field first");
        }
    }

    private List<AccessibilityNodeInfo> findNodesByText(AccessibilityNodeInfo root, String query) {
        List<AccessibilityNodeInfo> matches = new ArrayList<>();
        findNodesByTextRecursive(root, query.toLowerCase(), matches);
        return matches;
    }

    private void findNodesByTextRecursive(AccessibilityNodeInfo node, String query, List<AccessibilityNodeInfo> results) {
        if (node == null) return;
        CharSequence text = node.getText();
        CharSequence description = node.getContentDescription();
        if ((text != null && text.toString().toLowerCase().contains(query)) ||
            (description != null && description.toString().toLowerCase().contains(query))) {
            results.add(AccessibilityNodeInfo.obtain(node));
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            findNodesByTextRecursive(node.getChild(i), query, results);
        }
    }

    private AccessibilityNodeInfo findSearchNode(AccessibilityNodeInfo root) {
        if (root == null) return null;
        List<AccessibilityNodeInfo> result = new ArrayList<>();
        findSearchNodesRecursive(root, result);
        if (!result.isEmpty()) {
            return result.get(0);
        }
        // Fallback: look for the first editable text field
        return findFirstEditableNode(root);
    }

    private void findSearchNodesRecursive(AccessibilityNodeInfo node, List<AccessibilityNodeInfo> result) {
        if (node == null) return;
        CharSequence text = node.getText();
        CharSequence desc = node.getContentDescription();
        String txt = text != null ? text.toString().toLowerCase() : "";
        String dsc = desc != null ? desc.toString().toLowerCase() : "";
        if (txt.contains("search") || dsc.contains("search") || txt.contains("find") || dsc.contains("find") || txt.contains("khoj")) {
            result.add(AccessibilityNodeInfo.obtain(node));
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            findSearchNodesRecursive(node.getChild(i), result);
        }
    }

    private AccessibilityNodeInfo findFirstEditableNode(AccessibilityNodeInfo root) {
        if (root == null) return null;
        if (root.isEditable()) {
            return AccessibilityNodeInfo.obtain(root);
        }
        for (int i = 0; i < root.getChildCount(); i++) {
            AccessibilityNodeInfo res = findFirstEditableNode(root.getChild(i));
            if (res != null) return res;
        }
        return null;
    }

    private AccessibilityNodeInfo findFocusedOrEditableNode(AccessibilityNodeInfo root) {
        if (root == null) return null;
        if (root.isFocused() && root.isEditable()) {
            return AccessibilityNodeInfo.obtain(root);
        }
        if (root.isEditable()) {
            return AccessibilityNodeInfo.obtain(root);
        }
        for (int i = 0; i < root.getChildCount(); i++) {
            AccessibilityNodeInfo res = findFocusedOrEditableNode(root.getChild(i));
            if (res != null) return res;
        }
        return null;
    }

    private AccessibilityNodeInfo findSearchSubmitButton(AccessibilityNodeInfo root) {
        if (root == null) return null;
        List<AccessibilityNodeInfo> matches = new ArrayList<>();
        findSearchSubmitButtonsRecursive(root, matches);
        if (!matches.isEmpty()) {
            return matches.get(0);
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
            findSearchSubmitButtonsRecursive(node.getChild(i), result);
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
        if (node.isEditable()) {
            Bundle arguments = new Bundle();
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
            return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
        }
        return false;
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