package com.alfariskirana.app;

import android.content.Context;
import android.content.Intent;

public class WhatsAppShareHelper {

    public static void shareText(Context context, String text, String chooserTitle) {
        if (context == null || text == null || text.trim().isEmpty()) {
            return;
        }
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        sendIntent.setType("text/plain");

        Intent chooserIntent = Intent.createChooser(sendIntent, chooserTitle != null ? chooserTitle : "Share via");
        context.startActivity(chooserIntent);
    }
}