package com.localgeminibridge.app;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.util.Log;

public class GeminiAutoSubmitService extends AccessibilityService {

    private static final String TAG = "GeminiAutoSubmit";
    
    // Globally accessible flag to coordinate submit action only when triggered from HTTP server or test UI
    public static volatile boolean shouldTriggerSubmit = false;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!shouldTriggerSubmit) {
            return;
        }

        int eventType = event.getEventType();
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || 
            eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode != null) {
                boolean clicked = findAndClickSendButton(rootNode);
                rootNode.recycle();
                if (clicked) {
                    shouldTriggerSubmit = false;
                    Log.d(TAG, "Triggered send button clicked successfully.");
                }
            }
        }
    }

    private boolean findAndClickSendButton(AccessibilityNodeInfo node) {
        if (node == null) {
            return false;
        }

        // 1. Search criteria based on Content Description attributes
        CharSequence desc = node.getContentDescription();
        if (desc != null) {
            String descStr = desc.toString().toLowerCase();
            if (descStr.equals("send") || descStr.equals("submit") || 
                descStr.contains("send button") || descStr.contains("submit prompt")) {
                
                if (performClickOnNode(node)) {
                    return true;
                }
            }
        }

        // 2. Search criteria based on Resource View ID attributes
        String viewId = node.getViewIdResourceName();
        if (viewId != null) {
            String idStr = viewId.toLowerCase();
            if (idStr.contains("send_button") || idStr.contains("sendbutton") || 
                idStr.contains("send") || idStr.contains("submit")) {
                
                if (performClickOnNode(node)) {
                    return true;
                }
            }
        }

        // 3. Recursive iteration through hierarchy children
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

    private boolean performClickOnNode(AccessibilityNodeInfo node) {
        if (node.isClickable()) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            return true;
        } else {
            // Traverse up ancestry to find an interactive container
            AccessibilityNodeInfo parent = node.getParent();
            while (parent != null) {
                if (parent.isClickable()) {
                    parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    return true;
                }
                parent = parent.getParent();
            }
        }
        return false;
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Accessibility Service onInterrupt invoked");
    }
}