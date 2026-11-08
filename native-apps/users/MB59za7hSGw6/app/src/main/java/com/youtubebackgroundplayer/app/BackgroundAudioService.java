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
import android.media.MediaPlayer;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

public class BackgroundAudioService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    private static final String TAG = "BgAudioService";
    public static final String CHANNEL_ID = "yt_bg_media_channel";
    public static final int NOTIFICATION_ID = 1002;

    public static final String ACTION_PLAY = "com.youtubebackgroundplayer.app.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.youtubebackgroundplayer.app.ACTION_PAUSE";
    public static final String ACTION_TOGGLE = "com.youtubebackgroundplayer.app.ACTION_TOGGLE";
    public static final String ACTION_NEXT = "com.youtubebackgroundplayer.app.ACTION_NEXT";
    public static final String ACTION_PREV = "com.youtubebackgroundplayer.app.ACTION_PREV";
    public static final String ACTION_STOP = "com.youtubebackgroundplayer.app.ACTION_STOP";

    public interface OnAudioStateChangeListener {
        void onTrackPrepared(int durationMs);
        void onPlaybackStateChanged(boolean isPlaying);
        void onTrackCompleted();
        void onError(String message);
    }

    private final IBinder binder = new LocalBinder();
    private MediaPlayer mediaPlayer;
    private MediaSession mediaSession;
    private AudioManager audioManager;
    private OnAudioStateChangeListener listener;

    private String currentTitle = "No Track Selected";
    private String currentArtist = "YouTube Background Engine";
    private String currentStreamUrl = "";

    private boolean isPrepared = false;
    private boolean isPlayingState = false;

    public class LocalBinder extends Binder {
        public BackgroundAudioService getService() {
            return BackgroundAudioService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        initMediaPlayer();
        initMediaSession();
        createNotificationChannel();
    }

    private void initMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mediaPlayer.setAudioAttributes(
                        new AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build()
                );
            } else {
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            }
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setOnErrorListener(this);
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
            public void onSkipToNext() {
                if (listener != null) listener.onTrackCompleted();
            }

            @Override
            public void onStop() {
                stopAudio();
            }
        });
        mediaSession.setActive(true);
        updatePlaybackState(PlaybackState.STATE_STOPPED);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "YouTube Audio Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Background audio playback session controls");
            channel.setShowBadge(false);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            if (ACTION_PLAY.equals(action)) {
                playAudio();
            } else if (ACTION_PAUSE.equals(action)) {
                pauseAudio();
            } else if (ACTION_TOGGLE.equals(action)) {
                togglePlayPause();
            } else if (ACTION_STOP.equals(action)) {
                stopAudio();
            }
        }
        return START_STICKY;
    }

    public void playTrack(String streamUrl, String title, String artist) {
        this.currentStreamUrl = streamUrl;
        this.currentTitle = title;
        this.currentArtist = artist;
        this.isPrepared = false;

        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(streamUrl);
            mediaPlayer.prepareAsync();
            updateMediaMetadata(title, artist);
            startForeground(NOTIFICATION_ID, buildNotification());
        } catch (Exception e) {
            Log.e(TAG, "Error playing audio track: " + e.getMessage());
            if (listener != null) listener.onError("Failed to play track: " + e.getMessage());
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        isPrepared = true;
        playAudio();
        if (listener != null) {
            listener.onTrackPrepared(mp.getDuration());
        }
    }

    public void playAudio() {
        if (mediaPlayer != null && isPrepared) {
            requestAudioFocus();
            mediaPlayer.start();
            isPlayingState = true;
            updatePlaybackState(PlaybackState.STATE_PLAYING);
            buildAndShowNotification();
            if (listener != null) listener.onPlaybackStateChanged(true);
        }
    }

    public void pauseAudio() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPlayingState = false;
            updatePlaybackState(PlaybackState.STATE_PAUSED);
            buildAndShowNotification();
            if (listener != null) listener.onPlaybackStateChanged(false);
        }
    }

    public void togglePlayPause() {
        if (isPlayingState) {
            pauseAudio();
        } else {
            playAudio();
        }
    }

    public void stopAudio() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            isPlayingState = false;
            isPrepared = false;
            updatePlaybackState(PlaybackState.STATE_STOPPED);
        }
        stopForeground(true);
        if (listener != null) listener.onPlaybackStateChanged(false);
    }

    public void seekTo(int positionMs) {
        if (mediaPlayer != null && isPrepared) {
            mediaPlayer.seekTo(positionMs);
        }
    }

    public int getCurrentPosition() {
        if (mediaPlayer != null && isPrepared) {
            return mediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    public int getDuration() {
        if (mediaPlayer != null && isPrepared) {
            return mediaPlayer.getDuration();
        }
        return 0;
    }

    public boolean isPlaying() {
        return isPlayingState && mediaPlayer != null && mediaPlayer.isPlaying();
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
                    .setOnAudioFocusChangeListener(new AudioManager.OnAudioFocusChangeListener() {
                        @Override
                        public void onAudioFocusChange(int focusChange) {
                            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT || focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                                pauseAudio();
                            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                                playAudio();
                            }
                        }
                    })
                    .build();
            audioManager.requestAudioFocus(focusRequest);
        } else {
            audioManager.requestAudioFocus(new AudioManager.OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(int focusChange) {
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                        pauseAudio();
                    }
                }
            }, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
    }

    private void updatePlaybackState(int state) {
        PlaybackState.Builder builder = new PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_STOP)
                .setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1.0f);
        mediaSession.setPlaybackState(builder.build());
    }

    private void updateMediaMetadata(String title, String artist) {
        MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder();
        metadataBuilder.putString(MediaMetadata.METADATA_KEY_TITLE, title);
        metadataBuilder.putString(MediaMetadata.METADATA_KEY_ARTIST, artist);
        mediaSession.setMetadata(metadataBuilder.build());
    }

    private Notification buildNotification() {
        Intent openAppIntent = new Intent(this, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                this, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent toggleIntent = new Intent(this, BackgroundAudioService.class);
        toggleIntent.setAction(ACTION_TOGGLE);
        PendingIntent togglePendingIntent = PendingIntent.getService(
                this, 1, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

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
                .setContentText(currentArtist)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentIntent(contentPendingIntent)
                .setOngoing(isPlayingState)
                .setVisibility(Notification.VISIBILITY_PUBLIC);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setStyle(new Notification.MediaStyle()
                    .setMediaSession(mediaSession.getSessionToken())
                    .setShowActionsInCompactView(0, 1));
        }

        int playIcon = isPlayingState ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
        String playTitle = isPlayingState ? "Pause" : "Play";

        builder.addAction(playIcon, playTitle, togglePendingIntent);
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent);

        return builder.build();
    }

    private void buildAndShowNotification() {
        Notification notification = buildNotification();
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, notification);
        }
    }

    public void setOnAudioStateChangeListener(OnAudioStateChangeListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        isPlayingState = false;
        updatePlaybackState(PlaybackState.STATE_STOPPED);
        if (listener != null) {
            listener.onTrackCompleted();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        isPlayingState = false;
        if (listener != null) {
            listener.onError("Playback network stream error code: " + what);
        }
        return true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (mediaSession != null) {
            mediaSession.release();
        }
        super.onDestroy();
    }
}