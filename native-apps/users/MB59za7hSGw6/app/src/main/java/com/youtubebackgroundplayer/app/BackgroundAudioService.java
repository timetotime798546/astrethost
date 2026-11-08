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
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

public class BackgroundAudioService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    public static final String CHANNEL_ID = "yt_bg_media_channel";
    public static final int NOTIFICATION_ID = 1002;

    public static final String ACTION_PLAY = "com.youtubebackgroundplayer.app.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.youtubebackgroundplayer.app.ACTION_PAUSE";
    public static final String ACTION_STOP = "com.youtubebackgroundplayer.app.ACTION_STOP";
    public static final String ACTION_NEXT = "com.youtubebackgroundplayer.app.ACTION_NEXT";
    public static final String ACTION_PREV = "com.youtubebackgroundplayer.app.ACTION_PREV";

    private final IBinder binder = new LocalBinder();
    private MediaPlayer mediaPlayer;
    private MediaSession mediaSession;
    private AudioManager audioManager;
    private AudioTrack currentTrack;
    private boolean isPrepared = false;

    public interface OnTrackChangeListener {
        void onTrackStateChanged(boolean isPlaying, int currentPosition, int duration);
    }

    private OnTrackChangeListener trackChangeListener;

    public class LocalBinder extends Binder {
        public BackgroundAudioService getService() {
            return BackgroundAudioService.this;
        }
    }

    public void setOnTrackChangeListener(OnTrackChangeListener listener) {
        this.trackChangeListener = listener;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        initMediaPlayer();
        initMediaSession();
        createNotificationChannel();
        requestAudioFocus();
        startForegroundWithNotification();
    }

    private void initMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setOnErrorListener(this);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mediaPlayer.setAudioAttributes(
                        new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                );
            } else {
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            }
        }
    }

    private void initMediaSession() {
        mediaSession = new MediaSession(this, "YTBgAudioSession");
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setCallback(new MediaSession.Callback() {
            @Override
            public void onPlay() {
                playAudio();
            }

            @Override
            public void onPause() {
                pauseAudio();
            }

            @Override
            public void onStop() {
                stopSelf();
            }
        });
        mediaSession.setActive(true);
        updatePlaybackState(PlaybackState.STATE_NONE);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "YouTube Background Playback",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Controls for background audio playback session");
            channel.setShowBadge(false);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void startForegroundWithNotification() {
        Notification notification = buildNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    public void playTrack(AudioTrack track) {
        this.currentTrack = track;
        try {
            isPrepared = false;
            mediaPlayer.reset();
            mediaPlayer.setDataSource(track.getStreamUrl());
            mediaPlayer.prepareAsync();
            updateNotification();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playAudio() {
        if (mediaPlayer != null && isPrepared) {
            mediaPlayer.start();
            updatePlaybackState(PlaybackState.STATE_PLAYING);
            updateNotification();
            if (trackChangeListener != null) {
                trackChangeListener.onTrackStateChanged(true, getCurrentPosition(), getDuration());
            }
        }
    }

    public void pauseAudio() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            updatePlaybackState(PlaybackState.STATE_PAUSED);
            updateNotification();
            if (trackChangeListener != null) {
                trackChangeListener.onTrackStateChanged(false, getCurrentPosition(), getDuration());
            }
        }
    }

    public void seekTo(int positionMs) {
        if (mediaPlayer != null && isPrepared) {
            mediaPlayer.seekTo(positionMs);
        }
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public int getCurrentPosition() {
        if (mediaPlayer != null && isPrepared) {
            try {
                return mediaPlayer.getCurrentPosition();
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }

    public int getDuration() {
        if (mediaPlayer != null && isPrepared) {
            try {
                return mediaPlayer.getDuration();
            } catch (Exception e) {
                return 0;
            }
        }
        return currentTrack != null ? currentTrack.getDurationSeconds() * 1000 : 0;
    }

    public AudioTrack getCurrentTrack() {
        return currentTrack;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        isPrepared = true;
        mp.start();
        updatePlaybackState(PlaybackState.STATE_PLAYING);
        updateNotification();
        if (trackChangeListener != null) {
            trackChangeListener.onTrackStateChanged(true, 0, getDuration());
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        updatePlaybackState(PlaybackState.STATE_STOPPED);
        if (trackChangeListener != null) {
            trackChangeListener.onTrackStateChanged(false, getDuration(), getDuration());
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        isPrepared = false;
        return true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            if (ACTION_PLAY.equals(action)) {
                playAudio();
            } else if (ACTION_PAUSE.equals(action)) {
                pauseAudio();
            } else if (ACTION_STOP.equals(action)) {
                pauseAudio();
                stopForeground(true);
                stopSelf();
            }
        }
        return START_STICKY;
    }

    private void requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioFocusRequest focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                            new AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                    .build()
                    )
                    .setAcceptsDelayedFocusGain(true)
                    .build();
            audioManager.requestAudioFocus(focusRequest);
        } else {
            audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
    }

    private void updatePlaybackState(int state) {
        PlaybackState.Builder builder = new PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_STOP | PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS)
                .setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1.0f);
        mediaSession.setPlaybackState(builder.build());
    }

    private void updateNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, buildNotification());
        }
    }

    private Notification buildNotification() {
        Intent openAppIntent = new Intent(this, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                this, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = currentTrack != null ? currentTrack.getTitle() : "YouTube Audio Player";
        String artist = currentTrack != null ? currentTrack.getArtist() : "Background Engine Active";

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        builder.setContentTitle(title)
                .setContentText(artist)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentIntent(contentPendingIntent)
                .setOngoing(isPlaying())
                .setVisibility(Notification.VISIBILITY_PUBLIC);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setStyle(new Notification.MediaStyle()
                    .setMediaSession(mediaSession.getSessionToken()));
        }

        return builder.build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (mediaSession != null) {
            mediaSession.release();
        }
        super.onDestroy();
    }
}