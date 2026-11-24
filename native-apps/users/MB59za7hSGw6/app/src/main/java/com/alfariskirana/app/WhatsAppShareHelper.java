package com.alfariskirana.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import java.net.URLEncoder;

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

    public static void openDirectWhatsAppChat(Context context, String rawPhone, String text) {
        if (context == null || text == null || text.trim().isEmpty()) {
            return;
        }
        String formattedPhone = formatPhoneNumber(rawPhone);
        if (formattedPhone == null || formattedPhone.trim().isEmpty()) {
            Toast.makeText(context, "ग्राहक का मान्य मोबाइल नंबर उपलब्ध नहीं है!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String encodedMessage = URLEncoder.encode(text, "UTF-8");
            String url = "https://wa.me/" + formattedPhone + "?text=" + encodedMessage;
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "WhatsApp खोलने में समस्या हुई: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public static String formatPhoneNumber(String rawPhone) {
        if (rawPhone == null || rawPhone.trim().isEmpty()) {
            return null;
        }
        String cleaned = rawPhone.replaceAll("[^0-9]", "");
        if (cleaned.isEmpty()) {
            return null;
        }
        // Handle 10-digit Indian numbers by prepending country code 91
        if (cleaned.length() == 10) {
            cleaned = "91" + cleaned;
        }
        return cleaned;
    }
}