package com.chargingvoicealert.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Build;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import java.util.HashMap;
import java.util.Locale;

public class ChargingMonitorService extends Service {

    private static final String CHANNEL_ID = "ChargingVoiceAlertServiceChannel";
    private static final int NOTIFICATION_ID = 4521;

    private TextToSpeech tts;
    private boolean isTtsReady = false;

    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        private boolean isFirstRun = true;
        private int lastAnnouncedPercent = -1;
        private boolean lastIsCharging = false;
        private boolean wasFullAnnounced = false;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            String action = intent.getAction();

            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);

                float batteryPct = level * 100 / (float) scale;
                int currentPercent = (int) batteryPct;
                boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING 
                        || status == BatteryManager.BATTERY_STATUS_FULL;

                if (isFirstRun) {
                    lastIsCharging = isCharging;
                    lastAnnouncedPercent = currentPercent;
                    wasFullAnnounced = (currentPercent == 100);
                    isFirstRun = false;
                    
                    broadcastLocalUpdate(currentPercent, isCharging, chargePlug);
                    return;
                }

                // Connection Speech Event Trigger
                if (isCharging && !lastIsCharging) {
                    speakConnected();
                    lastIsCharging = true;
                    lastAnnouncedPercent = currentPercent;
                    wasFullAnnounced = (currentPercent == 100);
                    showStatusNotification("Charging Started", "Mobile started charging.");
                }
                // Disconnection Speech Event Trigger
                else if (!isCharging && lastIsCharging) {
                    speakDisconnected();
                    lastIsCharging = false;
                    lastAnnouncedPercent = currentPercent;
                    wasFullAnnounced = false;
                    showStatusNotification("Charger Disconnected", "Mobile removed from charger.");
                }

                // Percentage tracking and full alerts
                if (isCharging) {
                    if (currentPercent == 100) {
                        if (!wasFullAnnounced) {
                            speakFullyCharged();
                            wasFullAnnounced = true;
                            showStatusNotification("Battery Fully Charged", "Battery reached 100%.");
                        }
                    } else {
                        if (currentPercent > lastAnnouncedPercent) {
                            int difference = currentPercent - lastAnnouncedPercent;
                            if (difference == 1) {
                                speakPercentIncrease(currentPercent);
                            } else if (difference > 1) {
                                speakPercentJump(currentPercent);
                            }
                            lastAnnouncedPercent = currentPercent;
                        } else if (currentPercent < lastAnnouncedPercent) {
                            // Reset state baseline if value unexpectedly drops
                            lastAnnouncedPercent = currentPercent;
                        }
                    }
                } else {
                    lastAnnouncedPercent = currentPercent;
                    wasFullAnnounced = false;
                }

                // Broadcast back real-time live telemetry to running MainActivity
                broadcastLocalUpdate(currentPercent, isCharging, chargePlug);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        // Start Foreground Notification
        try {
            startForeground(NOTIFICATION_ID, buildMonitorNotification());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Initialize Text-To-Speech
        initTtsEngine();

        // Register system sticky battery changes dynamically
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryReceiver, filter);
    }

    private void initTtsEngine() {
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    // Initialize with standard Hindi locale context first
                    int result = tts.setLanguage(new Locale("hi", "IN"));
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts.setLanguage(Locale.getDefault());
                    }
                    isTtsReady = true;
                }
            }
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Charging Voice Alert Monitoring",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Required for reliable background power monitoring alert features.");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildMonitorNotification() {
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        builder.setContentTitle("Charging Voice Alert")
               .setContentText("Monitoring battery and charging status")
               .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
               .setContentIntent(pendingIntent)
               .setOngoing(true);

        return builder.build();
    }

    private void showStatusNotification(String title, String text) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            Notification.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder = new Notification.Builder(this, CHANNEL_ID);
            } else {
                builder = new Notification.Builder(this);
            }

            builder.setContentTitle(title)
                   .setContentText(text)
                   .setSmallIcon(android.R.drawable.ic_dialog_info)
                   .setAutoCancel(true);

            manager.notify(NOTIFICATION_ID + 2, builder.build());
        }
    }

    private void speak(String text) {
        SharedPreferences prefs = getSharedPreferences("ChargingVoicePrefs", MODE_PRIVATE);
        boolean overallVoiceEnabled = prefs.getBoolean("pref_voice_alerts_enabled", true);
        if (!overallVoiceEnabled) return;

        if (isTtsReady && tts != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts.speak(text, TextToSpeech.QUEUE_ADD, null, "ChargingVoiceAlertUtteranceID");
            } else {
                HashMap<String, String> params = new HashMap<>();
                params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "ChargingVoiceAlertUtteranceID");
                tts.speak(text, TextToSpeech.QUEUE_ADD, params);
            }
        }
    }

    private void speakConnected() {
        SharedPreferences prefs = getSharedPreferences("ChargingVoicePrefs", MODE_PRIVATE);
        if (prefs.getBoolean("pref_alert_connected_enabled", true)) {
            speak("Apna mobile charger hone laga hai.");
        }
    }

    private void speakDisconnected() {
        SharedPreferences prefs = getSharedPreferences("ChargingVoicePrefs", MODE_PRIVATE);
        if (prefs.getBoolean("pref_alert_disconnected_enabled", true)) {
            speak("Charging remove kar diya hai.");
        }
    }

    private void speakPercentIncrease(int currentPercent) {
        SharedPreferences prefs = getSharedPreferences("ChargingVoicePrefs", MODE_PRIVATE);
        boolean pEnabled = prefs.getBoolean("pref_percent_alerts_enabled", true);
        boolean iEnabled = prefs.getBoolean("pref_alert_percent_enabled", true);
        if (pEnabled && iEnabled) {
            speak("Battery 1 percent badh gayi hai. Ab total battery " + currentPercent + " percent hai.");
        }
    }

    private void speakPercentJump(int currentPercent) {
        SharedPreferences prefs = getSharedPreferences("ChargingVoicePrefs", MODE_PRIVATE);
        boolean pEnabled = prefs.getBoolean("pref_percent_alerts_enabled", true);
        boolean iEnabled = prefs.getBoolean("pref_alert_percent_enabled", true);
        if (pEnabled && iEnabled) {
            speak("Battery badh gayi hai. Ab total battery " + currentPercent + " percent hai.");
        }
    }

    private void speakFullyCharged() {
        SharedPreferences prefs = getSharedPreferences("ChargingVoicePrefs", MODE_PRIVATE);
        if (prefs.getBoolean("pref_alert_full_enabled", true)) {
            speak("Battery poori charge ho gayi hai.");
        }
    }

    private void broadcastLocalUpdate(int level, boolean isCharging, int plugged) {
        Intent updateIntent = new Intent("com.chargingvoicealert.app.BATTERY_UPDATE");
        updateIntent.putExtra("level", level);
        updateIntent.putExtra("isCharging", isCharging);
        updateIntent.putExtra("plugged", plugged);
        sendBroadcast(updateIntent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(batteryReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (tts != null) {
            try {
                tts.stop();
                tts.shutdown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        stopForeground(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}