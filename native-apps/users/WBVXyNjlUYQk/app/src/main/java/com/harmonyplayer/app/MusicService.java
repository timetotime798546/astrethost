package com.harmonyplayer.app;

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
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MusicService extends Service implements MediaPlayer.OnPreparedListener, 
        MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, AudioManager.OnAudioFocusChangeListener {

    private final IBinder musicBind = new MusicBinder();
    private MediaPlayer player;
    private List<Song> playlist = new ArrayList<>();
    private int currentSongIndex = -1;
    private boolean isPrepared = false;
    private AudioManager audioManager;
    private AudioFocusRequest focusRequest; // Requires API 26+

    public static final String ACTION_STATUS_CHANGED = "com.harmonyplayer.app.STATUS_CHANGED";

    @Override
    public void onCreate() {
        super.onCreate();
        player = new MediaPlayer();
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    }

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return musicBind;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        player.stop();
        player.release();
        return false;
    }

    public void setPlaylist(List<Song> songs) {
        this.playlist = songs;
    }

    public void playSong(int index) {
        if (index < 0 || index >= playlist.size()) return;
        currentSongIndex = index;
        Song playSong = playlist.get(index);
        isPrepared = false;

        player.reset();
        try {
            player.setDataSource(playSong.getPath());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                player.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());
            } else {
                player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            }

            if (requestAudioFocus()) {
                player.prepareAsync();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void playPause() {
        if (player.isPlaying()) {
            player.pause();
            broadcastStatus(false);
        } else {
            if (isPrepared && requestAudioFocus()) {
                player.start();
                broadcastStatus(true);
            } else if (currentSongIndex != -1) {
                playSong(currentSongIndex);
            } else if (!playlist.isEmpty()) {
                playSong(0);
            }
        }
    }

    public void next() {
        if (playlist.isEmpty()) return;
        int nextIndex = currentSongIndex + 1;
        if (nextIndex >= playlist.size()) {
            nextIndex = 0;
        }
        playSong(nextIndex);
    }

    public void prev() {
        if (playlist.isEmpty()) return;
        int prevIndex = currentSongIndex - 1;
        if (prevIndex < 0) {
            prevIndex = playlist.size() - 1;
        }
        playSong(prevIndex);
    }

    public boolean isPlaying() {
        try {
            return player != null && player.isPlaying();
        } catch (Exception e) {
            return false;
        }
    }

    public int getPosition() {
        if (isPrepared) {
            return player.getCurrentPosition();
        }
        return 0;
    }

    public int getDuration() {
        if (isPrepared) {
            return player.getDuration();
        }
        return 0;
    }

    public void seekTo(int pos) {
        if (isPrepared) {
            player.seekTo(pos);
        }
    }

    public int getCurrentSongIndex() {
        return currentSongIndex;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        isPrepared = true;
        mp.start();
        broadcastStatus(true);
        startForegroundServiceNotification();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        next();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        isPrepared = false;
        return false;
    }

    private boolean requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build())
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(this)
                    .build();
            return audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        } else {
            return audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }
    }

    private void abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (focusRequest != null) {
                audioManager.abandonAudioFocusRequest(focusRequest);
            }
        } else {
            audioManager.abandonAudioFocus(this);
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                if (player != null && !player.isPlaying() && isPrepared) {
                    player.start();
                    broadcastStatus(true);
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                if (player != null && player.isPlaying()) {
                    player.pause();
                    broadcastStatus(false);
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (player != null && player.isPlaying()) {
                    player.pause();
                    broadcastStatus(false);
                }
                break;
        }
    }

    private void broadcastStatus(boolean playing) {
        Intent intent = new Intent(ACTION_STATUS_CHANGED);
        intent.putExtra("playing", playing);
        intent.putExtra("index", currentSongIndex);
        sendBroadcast(intent);
    }

    private void startForegroundServiceNotification() {
        if (currentSongIndex < 0 || currentSongIndex >= playlist.size()) return;
        Song song = playlist.get(currentSongIndex);

        String channelId = "harmony_music_channel";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, "Harmony Player Controls", NotificationManager.IMPORTANCE_LOW);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, channelId);
        } else {
            builder = new Notification.Builder(this);
        }

        Notification notification = builder
                .setContentTitle(song.getTitle())
                .setContentText(song.getArtist())
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentIntent(pendingIntent)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(101, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(101, notification);
        }
    }

    @Override
    public void onDestroy() {
        abandonAudioFocus();
        if (player != null) {
            player.stop();
            player.release();
        }
        stopForeground(true);
        super.onDestroy();
    }
}