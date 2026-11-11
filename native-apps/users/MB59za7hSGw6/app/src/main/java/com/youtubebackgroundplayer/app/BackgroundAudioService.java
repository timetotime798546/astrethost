package com.youtubebackgroundplayer.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

public class BackgroundAudioService extends Service implements AudioManager.OnAudioFocusChangeListener {

    private static final String CHANNEL_ID = "yt_bg_channel";
    private static final int NOTIFICATION_ID = 1001;
    public static final String ACTION_TOGGLE_PLAYBACK = "com.youtubebackgroundplayer.app.TOGGLE_PLAYBACK";

    private final IBinder binder = new LocalBinder();
    private AudioManager audioManager;
    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;
    private MediaSession mediaSession;
    private boolean isPlaying = false;
    private String currentTitle = "YouTube Background Audio";
    private MediaStateChangeListener mediaStateChangeListener;
    private Object audioFocusRequestObj;
    private boolean hasAudioFocus = false;

    public interface MediaStateChangeListener {
        void onPlayRequested();
        void onPauseRequested();
    }

    public class LocalBinder extends Binder {
        public BackgroundAudioService getService() {
            return BackgroundAudioService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        createNotificationChannel();

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "YouTubeBackgroundPlayer:AudioServiceLock");
            wakeLock.setReferenceCounted(false);
        }

        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wm != null) {
            wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "YouTubeBackgroundPlayer:WifiLock");
            wifiLock.setReferenceCounted(false);
        }

        setupMediaSession();
        requestAudioFocus();
    }

    private void setupMediaSession() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaSession = new MediaSession(this, "YouTubeBackgroundSession");
            mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);

            mediaSession.setCallback(new MediaSession.Callback() {
                @Override
                public void onPlay() {
                    super.onPlay();
                    requestAudioFocus();
                    if (mediaStateChangeListener != null) {
                        mediaStateChangeListener.onPlayRequested();
                    }
                }

                @Override
                public void onPause() {
                    super.onPause();
                    if (mediaStateChangeListener != null) {
                        mediaStateChangeListener.onPauseRequested();
                    }
                }
            });

            updatePlaybackState(PlaybackState.STATE_STOPPED);
            mediaSession.setActive(true);
        }
    }

    public boolean requestAudioFocus() {
        if (audioManager == null) return false;
        if (hasAudioFocus) return true;

        int result;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioAttributes playbackAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            AudioFocusRequest focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(this)
                    .build();
            audioFocusRequestObj = focusRequest;
            result = audioManager.requestAudioFocus(focusRequest);
        } else {
            result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
        hasAudioFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
        return hasAudioFocus;
    }

    public void abandonAudioFocus() {
        if (audioManager == null || !hasAudioFocus) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequestObj != null) {
            audioManager.abandonAudioFocusRequest((AudioFocusRequest) audioFocusRequestObj);
        } else {
            audioManager.abandonAudioFocus(this);
        }
        hasAudioFocus = false;
    }

    public void setMediaStateChangeListener(MediaStateChangeListener listener) {
        this.mediaStateChangeListener = listener;
    }

    public void updatePlaybackState(int state) {
        boolean newStateIsPlaying = (state == PlaybackState.STATE_PLAYING);
        this.isPlaying = newStateIsPlaying;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mediaSession != null) {
            long actions = PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY_PAUSE;
            PlaybackState pbState = new PlaybackState.Builder()
                    .setActions(actions)
                    .setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1.0f)
                    .build();
            mediaSession.setPlaybackState(pbState);
        }

        if (isPlaying) {
            requestAudioFocus();
            if (wakeLock != null && !wakeLock.isHeld()) {
                try {
                    wakeLock.acquire(24 * 60 * 60 * 1000L);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (wifiLock != null && !wifiLock.isHeld()) {
                try {
                    wifiLock.acquire();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            if (wakeLock != null && wakeLock.isHeld()) {
                try {
                    wakeLock.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (wifiLock != null && wifiLock.isHeld()) {
                try {
                    wifiLock.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        updateNotification();
    }

    public void updateMediaMetadata(String title) {
        if (title != null && !title.trim().isEmpty() && !title.equalsIgnoreCase("YouTube")) {
            this.currentTitle = title;
        } else if (this.currentTitle == null || this.currentTitle.isEmpty()) {
            this.currentTitle = "YouTube Background Audio";
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mediaSession != null) {
            MediaMetadata metadata = new MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_TITLE, currentTitle)
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, "Playing in Background")
                    .build();
            mediaSession.setMetadata(metadata);
        }
        updateNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_TOGGLE_PLAYBACK.equals(intent.getAction())) {
            if (isPlaying) {
                if (mediaStateChangeListener != null) {
                    mediaStateChangeListener.onPauseRequested();
                }
            } else {
                if (mediaStateChangeListener != null) {
                    mediaStateChangeListener.onPlayRequested();
                }
            }
        } else {
            startForegroundInternal();
        }
        return START_STICKY;
    }

    private void startForegroundInternal() {
        Notification notification = buildNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        if (isPlaying) {
            updateNotification();
        }
    }

    private void updateNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            try {
                manager.notify(NOTIFICATION_ID, buildNotification());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "YouTube Background Audio",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Background audio session control notification");
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                this, 0, intent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        Intent toggleIntent = new Intent(this, BackgroundAudioService.class);
        toggleIntent.setAction(ACTION_TOGGLE_PLAYBACK);
        PendingIntent togglePendingIntent = PendingIntent.getService(
                this, 1, toggleIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT : PendingIntent.FLAG_UPDATE_CURRENT
        );

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        builder.setContentTitle(currentTitle)
                .setContentText(isPlaying ? "Audio playing in background" : "Audio paused")
                .setSmallIcon(R.drawable.ic_play)
                .setContentIntent(contentIntent)
                .setOngoing(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int iconRes = isPlaying ? R.drawable.ic_pause : R.drawable.ic_play;
            String actionTitle = isPlaying ? "Pause" : "Play";
            Notification.Action action = new Notification.Action.Builder(
                    iconRes, actionTitle, togglePendingIntent
            ).build();
            builder.addAction(action);

            if (mediaSession != null) {
                Notification.MediaStyle mediaStyle = new Notification.MediaStyle();
                mediaStyle.setMediaSession(mediaSession.getSessionToken());
                mediaStyle.setShowActionsInCompactView(0);
                builder.setStyle(mediaStyle);
            }
        }

        return builder.build();
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            hasAudioFocus = false;
        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            hasAudioFocus = true;
            if (mediaStateChangeListener != null) {
                mediaStateChangeListener.onPlayRequested();
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        abandonAudioFocus();
        if (wakeLock != null && wakeLock.isHeld()) {
            try {
                wakeLock.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (wifiLock != null && wifiLock.isHeld()) {
            try {
                wifiLock.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
        }
        stopForeground(true);
        super.onDestroy();
    }
}