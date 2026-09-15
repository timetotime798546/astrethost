package com.chargingvoicealert.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences prefs = context.getSharedPreferences("ChargingVoicePrefs", Context.MODE_PRIVATE);
            boolean isServiceEnabled = prefs.getBoolean("pref_service_enabled", false);
            
            if (isServiceEnabled) {
                Intent serviceIntent = new Intent(context, ChargingMonitorService.class);
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent);
                    } else {
                        context.startService(serviceIntent);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}