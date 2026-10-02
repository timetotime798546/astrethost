package com.localgeminibridge.app;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.util.Log;

public class GeminiAutoSubmitService extends AccessibilityService {

    private static final String TAG = "GeminiAutoSubmit";
    
    // Globally accessible flag to coordinate submit action only when triggered from HTTP server or test UI
    public static volatile boolean shouldTriggerSubmit = false;

    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    private boolean isPendingClick = false;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!shouldTriggerSubmit) {
            return;
        }

        CharSequence pkgNameChar = event.getPackageName();
        if (pkgNameChar == null) {
            return;
        }
        String pkgName = pkgNameChar.toString();

        // 1. Detect when the active window is com.google.android.apps.bard
        if ("com.google.android.apps.bard".equals(pkgName)) {
            
            int eventType = event.getEventType();
            if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || 
                eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
                
                if (!isPendingClick) {
                    isPendingClick = true;
                    log("Gemini window detected: " + pkgName);
                    log("Scheduling auto-submit click with 400ms delay...");
                    
                    // 2. Retry and delay handler: approx 400ms delay to let text fully populate
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            isPendingClick = false;
                            
                            // Re-verify that submit flag remains armed
                            if (!shouldTriggerSubmit) {
                                return;
                            }
                            
                            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                            if (rootNode != null) {
                                log("Searching active tree recursively for Send/Submit button...");
                                boolean clicked = findAndClickSendButton(rootNode);
                                rootNode.recycle();
                                if (clicked) {
                                    shouldTriggerSubmit = false;
                                    log("Click executed successfully. Auto-submit done.");
                                } else {
                                    log("Send/Submit button search completed, but no target button was clicked.");
                                }
                            } else {
                                log("Failed to retrieve active window root node.");
                            }
                        }
                    }, 400);
                }
            }
        }
    }

    private boolean findAndClickSendButton(AccessibilityNodeInfo node) {
        if (node == null) {
            return false;
        }

        // 1. Robust Node Search Logic: check current node matches target criteria
        if (isMatch(node)) {
            log("Send button found! ID: " + node.getViewIdResourceName() + ", Desc: " + node.getContentDescription());
            if (performClick(node)) {
                return true;
            }
        }

        // 3. Inspect the entire AccessibilityNodeInfo tree recursively
        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                boolean found = findAndClickSendButton(child);
                child.recycle();
                if (found) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isMatch(AccessibilityNodeInfo node) {
        if (node == null) return false;

        // a) ContentDescription containing (case-insensitive): "send", "submit", "bhej", "bheje"
        CharSequence desc = node.getContentDescription();
        if (desc != null) {
            String descStr = desc.toString().toLowerCase();
            if (descStr.contains("send") || 
                descStr.contains("submit") || 
                descStr.contains("bhej") || 
                descStr.contains("bheje")) {
                return true;
            }
        }

        // b) View ID ending with: "send_button", "send_icon", "submit_button" (or containing them)
        String viewId = node.getViewIdResourceName();
        if (viewId != null) {
            String idStr = viewId.toLowerCase();
            if (idStr.endsWith("send_button") || 
                idStr.endsWith("send_icon") || 
                idStr.endsWith("submit_button") ||
                idStr.contains("send_button") ||
                idStr.contains("submit_button") ||
                idStr.contains("sendbutton") ||
                idStr.contains("submitbutton")) {
                return true;
            }
        }

        // c) Any clickable Button/ImageView with an image or icon inside the prompt bar container
        CharSequence className = node.getClassName();
        if (className != null) {
            String classStr = className.toString();
            if (classStr.contains("Button") || classStr.contains("ImageView")) {
                if (viewId != null) {
                    String idStr = viewId.toLowerCase();
                    if (idStr.contains("send") || idStr.contains("submit") || idStr.contains("input_action") || idStr.contains("send_icon")) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean performClick(AccessibilityNodeInfo node) {
        if (node == null) return false;
        
        // If node is clickable, perform click immediately
        if (node.isClickable()) {
            boolean success = node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            if (success) {
                return true;
            }
        }
        
        // 2. Click Parent Handling: if not clickable, traverse up to nearest clickable parent
        AccessibilityNodeInfo parent = node.getParent();
        while (parent != null) {
            if (parent.isClickable()) {
                boolean success = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                if (success) {
                    parent.recycle();
                    return true;
                }
            }
            AccessibilityNodeInfo nextParent = parent.getParent();
            parent.recycle();
            parent = nextParent;
        }
        
        return false;
    }

    private void log(String message) {
        Log.d(TAG, message);
        MainActivity.logBridge("[ACCESSIBILITY]: " + message);
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Accessibility Service onInterrupt invoked");
    }
}