package com.youtubebackgroundplayer.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
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
    private AudioFocusRequest focusRequest;
    private PowerManager.WakeLock wakeLock;
    private MediaSession mediaSession;
    private boolean isPlaying = false;
    private String currentTitle = "YouTube Player";
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
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        createNotificationChannel();

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "YouTubeBackgroundPlayer:AudioWakeLock");
            wakeLock.setReferenceCounted(false);
        }

        setupMediaSession();
    }

    private void setupMediaSession() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaSession = new MediaSession(this, "YouTubeBackgroundSession");
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

            updatePlaybackState(PlaybackState.STATE_STOPPED);
            mediaSession.setActive(true);
        }
    }

    public void setMediaStateChangeListener(MediaStateChangeListener listener) {
        this.mediaStateChangeListener = listener;
    }

    public void updatePlaybackState(int state) {
        isPlaying = (state == PlaybackState.STATE_PLAYING);

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
                wakeLock.acquire();
            }
        } else {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        }

        updateNotification();
    }

    public void updateMediaMetadata(String title) {
        if (title != null && !title.trim().isEmpty()) {
            this.currentTitle = title;
        } else {
            this.currentTitle = "YouTube Audio Session";
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mediaSession != null) {
            MediaMetadata metadata = new MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_TITLE, currentTitle)
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, "YouTube Background Player")
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
            startForeground(NOTIFICATION_ID, buildNotification());
        }
        return START_STICKY;
    }

    private void updateNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, buildNotification());
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
        PendingIntent contentIntent = PendingIntent.getActivity(
                this, 0, intent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        Intent toggleIntent = new Intent(this, BackgroundAudioService.class);
        toggleIntent.setAction(ACTION_TOGGLE_PLAYBACK);
        PendingIntent togglePendingIntent = PendingIntent.getService(
                this, 1, toggleIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        builder.setContentTitle(currentTitle)
                .setContentText(isPlaying ? "Playing in background" : "Paused")
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
                Notification.MediaStyle mediaStyle = new Notification.MediaStyle();
                mediaStyle.setMediaSession(mediaSession.getSessionToken());
                mediaStyle.setShowActionsInCompactView(0);
                builder.setStyle(mediaStyle);
            }
        }

        return builder.build();
    }

    public void requestAudioFocus() {
        if (audioManager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioAttributes playbackAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();

            focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(this)
                    .build();

            audioManager.requestAudioFocus(focusRequest);
        } else {
            audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
    }

    public void abandonAudioFocus() {
        if (audioManager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
            audioManager.abandonAudioFocusRequest(focusRequest);
        } else {
            audioManager.abandonAudioFocus(this);
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                if (mediaStateChangeListener != null) {
                    mediaStateChangeListener.onPlayRequested();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (mediaStateChangeListener != null) {
                    mediaStateChangeListener.onPauseRequested();
                }
                break;
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
            wakeLock.release();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
        }
        stopForeground(true);
        super.onDestroy();
    }
}