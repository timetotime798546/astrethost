package com.lockplaytube.app;

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
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

public class BackgroundAudioService extends Service implements AudioManager.OnAudioFocusChangeListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    private static final String CHANNEL_ID = "yt_bg_channel";
    private static final int NOTIFICATION_ID = 1001;
    public static final String ACTION_TOGGLE_PLAYBACK = "com.lockplaytube.app.TOGGLE_PLAYBACK";
    public static final String ACTION_NEXT_TRACK = "com.lockplaytube.app.NEXT_TRACK";

    private final IBinder binder = new LocalBinder();
    private AudioManager audioManager;
    private PowerManager.WakeLock wakeLock;
    private MediaSession mediaSession;
    private MediaPlayer mediaPlayer;

    private boolean isPlaying = false;
    private String currentTitle = "LockPlayTube Audio";
    private String currentArtist = "Background Audio Player";
    private MediaStateChangeListener mediaStateChangeListener;
    private Object audioFocusRequestObj;
    private boolean hasAudioFocus = false;
    private TrialPaywallManager trialPaywallManager;

    public interface MediaStateChangeListener {
        void onPlayStateChanged(boolean playing);
        void onTrackCompleted();
        void onErrorOccurred(String message);
    }

    public class LocalBinder extends Binder {
        public BackgroundAudioService getService() {
            return BackgroundAudioService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        trialPaywallManager = new TrialPaywallManager();

        if (!trialPaywallManager.isAccessGranted(this)) {
            stopForegroundAndAudio();
            return;
        }

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        createNotificationChannel();

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LockPlayTube:AudioWakeLock");
            wakeLock.setReferenceCounted(false);
        }

        setupMediaPlayer();
        setupMediaSession();
    }

    private void setupMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
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

    private void setupMediaSession() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaSession = new MediaSession(this, "LockPlayTubeSession");
            mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);

            mediaSession.setCallback(new MediaSession.Callback() {
                @Override
                public void onPlay() {
                    super.onPlay();
                    resumeAudio();
                }

                @Override
                public void onPause() {
                    super.onPause();
                    pauseAudio();
                }

                @Override
                public void onSkipToNext() {
                    super.onSkipToNext();
                    if (mediaStateChangeListener != null) {
                        mediaStateChangeListener.onTrackCompleted();
                    }
                }
            });

            updatePlaybackState(PlaybackState.STATE_STOPPED);
            mediaSession.setActive(true);
        }
    }

    public void playStream(String streamUrl, String title, String artist) {
        if (!trialPaywallManager.isAccessGranted(this)) {
            stopForegroundAndAudio();
            return;
        }

        this.currentTitle = (title != null && !title.isEmpty()) ? title : "Audio Track";
        this.currentArtist = (artist != null && !artist.isEmpty()) ? artist : "LockPlayTube Channel";

        if (!requestAudioFocus()) {
            return;
        }

        try {
            if (mediaPlayer == null) {
                setupMediaPlayer();
            } else {
                mediaPlayer.reset();
            }

            mediaPlayer.setDataSource(this, Uri.parse(streamUrl));
            mediaPlayer.prepareAsync();
            updateMediaMetadata(currentTitle, currentArtist);
        } catch (Exception e) {
            e.printStackTrace();
            if (mediaStateChangeListener != null) {
                mediaStateChangeListener.onErrorOccurred("Unable to load audio track");
            }
        }
    }

    public void pauseAudio() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPlaying = false;
            updatePlaybackState(PlaybackState.STATE_PAUSED);
            if (mediaStateChangeListener != null) {
                mediaStateChangeListener.onPlayStateChanged(false);
            }
        }
    }

    public void resumeAudio() {
        if (!trialPaywallManager.isAccessGranted(this)) {
            stopForegroundAndAudio();
            return;
        }

        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            if (requestAudioFocus()) {
                mediaPlayer.start();
                isPlaying = true;
                updatePlaybackState(PlaybackState.STATE_PLAYING);
                if (mediaStateChangeListener != null) {
                    mediaStateChangeListener.onPlayStateChanged(true);
                }
            }
        }
    }

    public void seekTo(int positionMs) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(positionMs);
        }
    }

    public int getCurrentPosition() {
        return (mediaPlayer != null && isPrepared()) ? mediaPlayer.getCurrentPosition() : 0;
    }

    public int getDuration() {
        return (mediaPlayer != null && isPrepared()) ? mediaPlayer.getDuration() : 0;
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    private boolean isPrepared() {
        return mediaPlayer != null;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if (!trialPaywallManager.isAccessGranted(this)) {
            stopForegroundAndAudio();
            return;
        }
        mp.start();
        isPlaying = true;
        updatePlaybackState(PlaybackState.STATE_PLAYING);
        if (mediaStateChangeListener != null) {
            mediaStateChangeListener.onPlayStateChanged(true);
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        isPlaying = false;
        updatePlaybackState(PlaybackState.STATE_STOPPED);
        if (mediaStateChangeListener != null) {
            mediaStateChangeListener.onPlayStateChanged(false);
            mediaStateChangeListener.onTrackCompleted();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        isPlaying = false;
        updatePlaybackState(PlaybackState.STATE_ERROR);
        if (mediaStateChangeListener != null) {
            mediaStateChangeListener.onErrorOccurred("Streaming playback error occurred");
        }
        return true;
    }

    public boolean requestAudioFocus() {
        if (!trialPaywallManager.isAccessGranted(this)) {
            stopForegroundAndAudio();
            return false;
        }
        if (audioManager == null || hasAudioFocus) return true;
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
        if (!trialPaywallManager.isAccessGranted(this)) {
            stopForegroundAndAudio();
            return;
        }

        this.isPlaying = (state == PlaybackState.STATE_PLAYING);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mediaSession != null) {
            long actions = PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_SKIP_TO_NEXT;
            PlaybackState pbState = new PlaybackState.Builder()
                    .setActions(actions)
                    .setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1.0f)
                    .build();
            mediaSession.setPlaybackState(pbState);
        }

        if (isPlaying) {
            if (wakeLock != null && !wakeLock.isHeld()) {
                try {
                    wakeLock.acquire(3600000);
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
        }

        updateNotification();
    }

    public void updateMediaMetadata(String title, String artist) {
        if (!trialPaywallManager.isAccessGranted(this)) {
            stopForegroundAndAudio();
            return;
        }

        this.currentTitle = (title != null && !title.isEmpty()) ? title : "LockPlayTube Audio";
        this.currentArtist = (artist != null && !artist.isEmpty()) ? artist : "LockPlayTube Channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mediaSession != null) {
            MediaMetadata metadata = new MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_TITLE, currentTitle)
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, currentArtist)
                    .build();
            mediaSession.setMetadata(metadata);
        }
        updateNotification();
    }

    public void stopForegroundAndAudio() {
        isPlaying = false;
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        abandonAudioFocus();
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
                mediaSession = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        stopForeground(true);
        stopSelf();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (trialPaywallManager == null) {
            trialPaywallManager = new TrialPaywallManager();
        }
        if (!trialPaywallManager.isAccessGranted(this)) {
            stopForegroundAndAudio();
            return START_NOT_STICKY;
        }

        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_TOGGLE_PLAYBACK.equals(action)) {
                if (isPlaying()) {
                    pauseAudio();
                } else {
                    resumeAudio();
                }
            } else if (ACTION_NEXT_TRACK.equals(action)) {
                if (mediaStateChangeListener != null) {
                    mediaStateChangeListener.onTrackCompleted();
                }
            } else {
                startForeground(NOTIFICATION_ID, buildNotification());
            }
        } else {
            startForeground(NOTIFICATION_ID, buildNotification());
        }
        return START_STICKY;
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
                    "LockPlayTube Audio",
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
                .setContentText(currentArtist + " - " + (isPlaying ? "Playing in background" : "Paused"))
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

    @Override
    public void onAudioFocusChange(int focusChange) {
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            hasAudioFocus = false;
            pauseAudio();
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            pauseAudio();
        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            resumeAudio();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        stopForegroundAndAudio();
        super.onDestroy();
    }
}