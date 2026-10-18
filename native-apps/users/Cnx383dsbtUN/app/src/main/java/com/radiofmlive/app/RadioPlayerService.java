package com.radiofmlive.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

public class RadioPlayerService extends Service implements
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnBufferingUpdateListener,
        AudioManager.OnAudioFocusChangeListener {

    public static final String ACTION_PLAY = "com.radiofmlive.app.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.radiofmlive.app.ACTION_PAUSE";
    public static final String ACTION_STOP = "com.radiofmlive.app.ACTION_STOP";
    public static final String ACTION_TOGGLE = "com.radiofmlive.app.ACTION_TOGGLE";

    public static final int STATE_STOPPED = 0;
    public static final int STATE_BUFFERING = 1;
    public static final int STATE_PLAYING = 2;
    public static final int STATE_PAUSED = 3;
    public static final int STATE_ERROR = 4;

    private static final String CHANNEL_ID = "radio_playback_channel";
    private static final int NOTIFICATION_ID = 101;

    private final IBinder binder = new LocalBinder();
    private MediaPlayer mediaPlayer;
    private MediaSession mediaSession;
    private AudioManager audioManager;
    private PowerManager.WakeLock wakeLock;

    private RadioStation currentStation;
    private int currentState = STATE_STOPPED;
    private String errorMessage = "";
    private ServiceCallback callback;

    private CountDownTimer sleepTimer;
    private long sleepTimeRemaining = 0;

    private boolean isBecomingNoisyRegistered = false;
    private final BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                pause();
            }
        }
    };

    public interface ServiceCallback {
        void onPlaybackStateChanged(int state, String message);
        void onStationChanged(RadioStation station);
        void onSleepTimerTick(long millisUntilFinished);
    }

    public class LocalBinder extends Binder {
        public RadioPlayerService getService() {
            return RadioPlayerService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RadioFMLive::WakeLock");
            wakeLock.setReferenceCounted(false);
        }

        createNotificationChannel();
        initMediaSession();
        registerNoisyReceiver();
    }

    private void initMediaSession() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaSession = new MediaSession(this, "RadioFMLiveSession");
            mediaSession.setCallback(new MediaSession.Callback() {
                @Override
                public void onPlay() {
                    play();
                }

                @Override
                public void onPause() {
                    pause();
                }

                @Override
                public void onStop() {
                    stop();
                }
            });
            mediaSession.setActive(true);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.channel_name),
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription(getString(R.string.channel_description));
            channel.setShowBadge(false);
            channel.setSound(null, null);

            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void registerNoisyReceiver() {
        if (!isBecomingNoisyRegistered) {
            IntentFilter filter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
            registerReceiver(becomingNoisyReceiver, filter);
            isBecomingNoisyRegistered = true;
        }
    }

    private void unregisterNoisyReceiver() {
        if (isBecomingNoisyRegistered) {
            try {
                unregisterReceiver(becomingNoisyReceiver);
            } catch (Exception ignored) {
            }
            isBecomingNoisyRegistered = false;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            if (ACTION_PLAY.equals(action)) {
                play();
            } else if (ACTION_PAUSE.equals(action)) {
                pause();
            } else if (ACTION_STOP.equals(action)) {
                stop();
            } else if (ACTION_TOGGLE.equals(action)) {
                if (currentState == STATE_PLAYING) {
                    pause();
                } else {
                    play();
                }
            }
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setCallback(ServiceCallback cb) {
        this.callback = cb;
        if (callback != null) {
            callback.onPlaybackStateChanged(currentState, errorMessage);
            if (currentStation != null) {
                callback.onStationChanged(currentStation);
            }
            callback.onSleepTimerTick(sleepTimeRemaining);
        }
    }

    public void playStation(RadioStation station) {
        if (station == null) return;
        this.currentStation = station;
        if (callback != null) {
            callback.onStationChanged(currentStation);
        }

        // Check network connection first
        if (!isNetworkAvailable()) {
            updateState(STATE_ERROR, "No active internet connection.");
            return;
        }

        requestAudioFocusAndStart(station.getStreamUrl());
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    private void requestAudioFocusAndStart(final String url) {
        int result = audioManager.requestAudioFocus(
                this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
        );

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            startStreaming(url);
        } else {
            updateState(STATE_ERROR, "Audio focus denied.");
        }
    }

    private void startStreaming(String url) {
        releasePlayer();

        updateState(STATE_BUFFERING, "Connecting to stream…");
        startForeground(NOTIFICATION_ID, buildNotification());

        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire(10 * 60 * 1000L); // 10 minutes temporary lock during buffering
        }

        try {
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

            mediaPlayer.setDataSource(url);
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnErrorListener(this);
            mediaPlayer.setOnBufferingUpdateListener(this);
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            updateState(STATE_ERROR, "Failed to initialize stream.");
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        try {
            mp.start();
            updateState(STATE_PLAYING, "Live Broadcast");
            updateNotification();
        } catch (Exception e) {
            updateState(STATE_ERROR, "Playback start error.");
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        updateState(STATE_ERROR, "Stream connection failed or server offline.");
        releasePlayer();
        stopForeground(true);
        return true;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        // Broadcast buffering progress if buffering state
        if (currentState == STATE_BUFFERING && callback != null) {
            callback.onPlaybackStateChanged(STATE_BUFFERING, "Buffering: " + percent + "%");
        }
    }

    public void play() {
        if (currentStation != null && currentState == STATE_PAUSED && mediaPlayer != null) {
            int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mediaPlayer.start();
                updateState(STATE_PLAYING, "Live Broadcast");
                updateNotification();
            }
        } else if (currentStation != null) {
            playStation(currentStation);
        }
    }

    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            updateState(STATE_PAUSED, "Paused");
            updateNotification();
        }
    }

    public void stop() {
        releasePlayer();
        updateState(STATE_STOPPED, "Stopped");
        stopForeground(true);
    }

    private void releasePlayer() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        if (mediaPlayer != null) {
            try {
                mediaPlayer.reset();
                mediaPlayer.release();
            } catch (Exception ignored) {
            }
            mediaPlayer = null;
        }
    }

    private void updateState(int state, String message) {
        this.currentState = state;
        this.errorMessage = message;
        if (callback != null) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    if (callback != null) {
                        callback.onPlaybackStateChanged(currentState, errorMessage);
                    }
                }
            });
        }
    }

    private Notification buildNotification() {
        String stationTitle = currentStation != null ? currentStation.getName() : "Radio FM Live";
        String statusText = (currentState == STATE_PLAYING) ? "Playing Live • " + (currentStation != null ? currentStation.getFrequency() : "") :
                (currentState == STATE_BUFFERING ? "Buffering..." : "Paused");

        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                this, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        Intent toggleIntent = new Intent(this, RadioPlayerService.class);
        toggleIntent.setAction(ACTION_TOGGLE);
        PendingIntent togglePendingIntent = PendingIntent.getService(
                this, 1, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        Intent stopIntent = new Intent(this, RadioPlayerService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 2, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        int playPauseIcon = (currentState == STATE_PLAYING) ? R.drawable.ic_pause : R.drawable.ic_play;
        String playPauseTitle = (currentState == STATE_PLAYING) ? "Pause" : "Play";

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        builder.setSmallIcon(R.drawable.ic_radio)
                .setContentTitle(stationTitle)
                .setContentText(statusText)
                .setContentIntent(contentPendingIntent)
                .setOngoing(currentState == STATE_PLAYING || currentState == STATE_BUFFERING)
                .addAction(playPauseIcon, playPauseTitle, togglePendingIntent)
                .addAction(R.drawable.ic_stop, "Stop", stopPendingIntent);

        return builder.build();
    }

    private void updateNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, buildNotification());
        }
    }

    public void setSleepTimer(long minutes) {
        if (sleepTimer != null) {
            sleepTimer.cancel();
            sleepTimer = null;
        }

        if (minutes <= 0) {
            sleepTimeRemaining = 0;
            if (callback != null) {
                callback.onSleepTimerTick(0);
            }
            return;
        }

        long millis = minutes * 60 * 1000L;
        sleepTimeRemaining = millis;

        sleepTimer = new CountDownTimer(millis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                sleepTimeRemaining = millisUntilFinished;
                if (callback != null) {
                    callback.onSleepTimerTick(millisUntilFinished);
                }
            }

            @Override
            public void onFinish() {
                sleepTimeRemaining = 0;
                if (callback != null) {
                    callback.onSleepTimerTick(0);
                }
                stop();
            }
        }.start();
    }

    public long getSleepTimeRemaining() {
        return sleepTimeRemaining;
    }

    public int getCurrentState() {
        return currentState;
    }

    public RadioStation getCurrentStation() {
        return currentStation;
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS:
                stop();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (mediaPlayer != null) {
                    mediaPlayer.setVolume(0.2f, 0.2f);
                }
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                if (mediaPlayer != null) {
                    mediaPlayer.setVolume(1.0f, 1.0f);
                    if (currentState == STATE_PAUSED) {
                        play();
                    }
                }
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sleepTimer != null) {
            sleepTimer.cancel();
        }
        unregisterNoisyReceiver();
        releasePlayer();
        if (mediaSession != null) {
            mediaSession.release();
        }
        audioManager.abandonAudioFocus(this);
    }
}