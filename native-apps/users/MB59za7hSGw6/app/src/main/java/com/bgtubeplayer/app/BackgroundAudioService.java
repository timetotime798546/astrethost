package com.bgtubeplayer.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import com.bgtubeplayer.app.R;

public class BackgroundAudioService extends Service implements AudioManager.OnAudioFocusChangeListener {

    private static final String CHANNEL_ID = "yt_bg_channel";
    private static final int NOTIFICATION_ID = 1001;
    public static final String ACTION_TOGGLE_PLAYBACK = "com.bgtubeplayer.app.TOGGLE_PLAYBACK";
    public static final String ACTION_PLAY = "com.bgtubeplayer.app.PLAY";
    public static final String ACTION_PAUSE = "com.bgtubeplayer.app.PAUSE";

    private final IBinder binder = new LocalBinder();
    private AudioManager audioManager;
    private PowerManager.WakeLock wakeLock;
    private MediaSession mediaSession;
    private boolean isPlaying = false;
    private String currentTitle = "BG Tube Player Audio";
    private MediaStateChangeListener mediaStateChangeListener;

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

        try {
            audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            createNotificationChannel();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BGTubePlayer:AudioWakeLock");
                if (wakeLock != null) {
                    wakeLock.setReferenceCounted(false);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            setupMediaSession();
        } catch (Exception e) {
            e.printStackTrace();
        }

        startForegroundSafely();
    }

    public void ensureInitialized() {
        try {
            createNotificationChannel();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mediaSession != null) {
                mediaSession.setActive(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startForegroundSafely() {
        try {
            createNotificationChannel();
            Notification notification = buildNotification();
            if (notification != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
                } else {
                    startForeground(NOTIFICATION_ID, notification);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupMediaSession() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                if (mediaSession == null) {
                    mediaSession = new MediaSession(this, "BGTubePlayerSession");
                    mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);

                    mediaSession.setCallback(new MediaSession.Callback() {
                        @Override
                        public void onPlay() {
                            super.onPlay();
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
                }

                updatePlaybackState(PlaybackState.STATE_STOPPED);
                mediaSession.setActive(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setMediaStateChangeListener(MediaStateChangeListener listener) {
        this.mediaStateChangeListener = listener;
    }

    public void updatePlaybackState(int state) {
        try {
            boolean newStateIsPlaying = (state == PlaybackState.STATE_PLAYING);
            if (this.isPlaying == newStateIsPlaying && state != PlaybackState.STATE_STOPPED) {
                return;
            }
            this.isPlaying = newStateIsPlaying;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mediaSession != null) {
                long actions = PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY_PAUSE;
                PlaybackState pbState = new PlaybackState.Builder()
                        .setActions(actions)
                        .setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1.0f)
                        .build();
                mediaSession.setPlaybackState(pbState);
                mediaSession.setActive(true);
            }

            if (isPlaying) {
                if (wakeLock != null && !wakeLock.isHeld()) {
                    try {
                        wakeLock.acquire(3600000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                startForegroundSafely();
            } else {
                if (wakeLock != null && wakeLock.isHeld()) {
                    try {
                        wakeLock.release();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                updateNotification();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateMediaMetadata(String title) {
        try {
            if (title != null && !title.trim().isEmpty() && !title.equalsIgnoreCase("YouTube")) {
                this.currentTitle = title;
            } else if (this.currentTitle == null || this.currentTitle.isEmpty()) {
                this.currentTitle = "BG Tube Player Audio";
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mediaSession != null) {
                MediaMetadata metadata = new MediaMetadata.Builder()
                        .putString(MediaMetadata.METADATA_KEY_TITLE, currentTitle)
                        .putString(MediaMetadata.METADATA_KEY_ARTIST, "Playing in Background")
                        .build();
                mediaSession.setMetadata(metadata);
            }
            updateNotification();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void pauseAndClearAudio() {
        try {
            isPlaying = false;
            if (wakeLock != null && wakeLock.isHeld()) {
                try {
                    wakeLock.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mediaSession != null) {
                try {
                    PlaybackState pbState = new PlaybackState.Builder()
                            .setActions(0)
                            .setState(PlaybackState.STATE_PAUSED, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 0)
                            .build();
                    mediaSession.setPlaybackState(pbState);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            stopForeground(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopForegroundAndAudio() {
        try {
            pauseAndClearAudio();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mediaSession != null) {
                try {
                    mediaSession.setActive(false);
                    mediaSession.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            stopSelf();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            if (intent != null) {
                String action = intent.getAction();
                if (ACTION_TOGGLE_PLAYBACK.equals(action)) {
                    if (isPlaying) {
                        if (mediaStateChangeListener != null) {
                            mediaStateChangeListener.onPauseRequested();
                        }
                    } else {
                        if (mediaStateChangeListener != null) {
                            mediaStateChangeListener.onPlayRequested();
                        }
                    }
                } else if (ACTION_PLAY.equals(action)) {
                    if (mediaStateChangeListener != null) {
                        mediaStateChangeListener.onPlayRequested();
                    }
                } else if (ACTION_PAUSE.equals(action)) {
                    if (mediaStateChangeListener != null) {
                        mediaStateChangeListener.onPauseRequested();
                    }
                } else {
                    startForegroundSafely();
                }
            } else {
                startForegroundSafely();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return START_STICKY;
    }

    private void updateNotification() {
        try {
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                Notification notification = buildNotification();
                if (notification != null) {
                    manager.notify(NOTIFICATION_ID, notification);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "BG Tube Player Audio",
                        NotificationManager.IMPORTANCE_LOW
                );
                channel.setDescription("Background audio session control notification");
                NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (manager != null) {
                    manager.createNotificationChannel(channel);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Notification buildNotification() {
        try {
            createNotificationChannel();

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

            String titleToUse = (currentTitle != null && !currentTitle.trim().isEmpty()) ? currentTitle : "BG Tube Player Audio";

            builder.setContentTitle(titleToUse)
                    .setContentText(isPlaying ? "Audio playing in background" : "Audio paused")
                    .setSmallIcon(R.drawable.ic_play)
                    .setContentIntent(contentIntent)
                    .setOngoing(isPlaying);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                int iconRes = isPlaying ? R.drawable.ic_pause : R.drawable.ic_play;
                String actionTitle = isPlaying ? "Pause" : "Play";
                Notification.Action action = new Notification.Action.Builder(
                        iconRes, actionTitle, togglePendingIntent
                ).build();
                builder.addAction(action);

                if (mediaSession != null) {
                    try {
                        MediaSession.Token token = mediaSession.getSessionToken();
                        if (token != null) {
                            Notification.MediaStyle mediaStyle = new Notification.MediaStyle();
                            mediaStyle.setMediaSession(token);
                            mediaStyle.setShowActionsInCompactView(0);
                            builder.setStyle(mediaStyle);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            return builder.build();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            if (mediaStateChangeListener != null) {
                mediaStateChangeListener.onPauseRequested();
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        if (wakeLock != null && wakeLock.isHeld()) {
            try {
                wakeLock.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mediaSession != null) {
            try {
                mediaSession.setActive(false);
                mediaSession.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            stopForeground(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }
}