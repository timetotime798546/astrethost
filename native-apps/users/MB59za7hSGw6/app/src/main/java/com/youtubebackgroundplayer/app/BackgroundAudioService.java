package com.youtubebackgroundplayer.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.IBinder;

public class BackgroundAudioService extends Service {

    public static final String ACTION_START = "com.youtubebackgroundplayer.app.action.START";
    public static final String ACTION_PLAY = "com.youtubebackgroundplayer.app.action.PLAY";
    public static final String ACTION_PAUSE = "com.youtubebackgroundplayer.app.action.PAUSE";
    public static final String ACTION_STOP = "com.youtubebackgroundplayer.app.action.STOP";
    public static final String ACTION_UPDATE_TITLE = "com.youtubebackgroundplayer.app.action.UPDATE_TITLE";
    public static final String EXTRA_TITLE = "extra_title";

    private static final String CHANNEL_ID = "yt_bg_channel";
    private static final int NOTIFICATION_ID = 1001;

    private MediaSession mediaSession;
    private NotificationManager notificationManager;
    private String currentTitle = "YouTube Audio Player";
    private boolean isPlaying = true;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        initMediaSession();
        createNotificationChannel();
    }

    private void initMediaSession() {
        mediaSession = new MediaSession(this, "YTBgPlayerSession");
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setCallback(new MediaSession.Callback() {
            @Override
            public void onPlay() {
                super.onPlay();
                isPlaying = true;
                updatePlaybackState(PlaybackState.STATE_PLAYING);
                buildAndShowNotification();
            }

            @Override
            public void onPause() {
                super.onPause();
                isPlaying = false;
                updatePlaybackState(PlaybackState.STATE_PAUSED);
                buildAndShowNotification();
            }

            @Override
            public void onStop() {
                super.onStop();
                stopForeground(true);
                stopSelf();
            }
        });
        mediaSession.setActive(true);
        updatePlaybackState(PlaybackState.STATE_PLAYING);
    }

    private void updatePlaybackState(int state) {
        PlaybackState.Builder builder = new PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_STOP |
                        PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS)
                .setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1.0f);
        mediaSession.setPlaybackState(builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "YouTube Background Playback",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Keeps audio playing in background and provides lock screen media controls.");
            channel.setShowBadge(false);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();

            if (ACTION_START.equals(action)) {
                if (intent.hasExtra(EXTRA_TITLE)) {
                    String title = intent.getStringExtra(EXTRA_TITLE);
                    if (title != null && !title.trim().isEmpty()) {
                        currentTitle = title;
                    }
                }
                isPlaying = true;
                updatePlaybackState(PlaybackState.STATE_PLAYING);
                startForeground(NOTIFICATION_ID, buildNotification());
            } else if (ACTION_UPDATE_TITLE.equals(action)) {
                if (intent.hasExtra(EXTRA_TITLE)) {
                    String title = intent.getStringExtra(EXTRA_TITLE);
                    if (title != null && !title.trim().isEmpty()) {
                        currentTitle = title;
                        updateMetadata(currentTitle);
                        buildAndShowNotification();
                    }
                }
            } else if (ACTION_PLAY.equals(action)) {
                isPlaying = true;
                updatePlaybackState(PlaybackState.STATE_PLAYING);
                buildAndShowNotification();
            } else if (ACTION_PAUSE.equals(action)) {
                isPlaying = false;
                updatePlaybackState(PlaybackState.STATE_PAUSED);
                buildAndShowNotification();
            } else if (ACTION_STOP.equals(action)) {
                isPlaying = false;
                stopForeground(true);
                stopSelf();
            }
        }
        return START_STICKY;
    }

    private void updateMetadata(String title) {
        if (mediaSession != null) {
            MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder();
            metadataBuilder.putString(MediaMetadata.METADATA_KEY_TITLE, title);
            metadataBuilder.putString(MediaMetadata.METADATA_KEY_ARTIST, "YouTube Background Player");
            mediaSession.setMetadata(metadataBuilder.build());
        }
    }

    private Notification buildNotification() {
        Intent openAppIntent = new Intent(this, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                this, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent playPauseIntent = new Intent(this, BackgroundAudioService.class);
        playPauseIntent.setAction(isPlaying ? ACTION_PAUSE : ACTION_PLAY);
        PendingIntent playPausePendingIntent = PendingIntent.getService(
                this, 1, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(this, BackgroundAudioService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 2, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        builder.setContentTitle(currentTitle)
                .setContentText("YouTube Background Audio Playing")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentIntent(contentPendingIntent)
                .setOngoing(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setStyle(new Notification.MediaStyle()
                    .setMediaSession(mediaSession.getSessionToken())
                    .setShowActionsInCompactView(0, 1));
        }

        if (isPlaying) {
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", playPausePendingIntent);
        } else {
            builder.addAction(android.R.drawable.ic_media_play, "Play", playPausePendingIntent);
        }
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent);

        return builder.build();
    }

    private void buildAndShowNotification() {
        Notification notification = buildNotification();
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {

        if (mediaSession != null) {
            mediaSession.release();
        }
        super.onDestroy();
    }
}