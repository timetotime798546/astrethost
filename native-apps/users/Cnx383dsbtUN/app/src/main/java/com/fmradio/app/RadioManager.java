package com.fmradio.app;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;

public class RadioManager {
    public interface RadioListener {
        void onPlaybackStateChanged(boolean isPlaying, boolean isBuffering);
        void onStationTuned(RadioStation station, double frequency, boolean isSignalLocked);
        void onError(String message);
    }

    private final Context context;
    private final RadioListener listener;
    private MediaPlayer mediaPlayer;
    private final List<RadioStation> knownStations = new ArrayList<RadioStation>();
    private double currentFrequency = 98.5; // Default standard frequency
    private boolean isPlaying = false;
    private boolean isBuffering = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public RadioManager(Context context, RadioListener listener) {
        this.context = context;
        this.listener = listener;
        initializePresetStations();
    }

    private void initializePresetStations() {
        // Broad dynamic selection of live stable international FM public feeds
        knownStations.add(new RadioStation(88.5, "NPR News FM", "Public Radio & News", "Washington DC", "https://npr-ice.streamguys1.com/live.mp3"));
        knownStations.add(new RadioStation(91.1, "Jazz Wave FM", "Smooth Jazz & Soul", "Chicago", "https://icecast.walmradio.com:8443/jazz"));
        knownStations.add(new RadioStation(94.9, "Retro Gold FM", "Classic Hits 80s & 90s", "London", "https://stream.antenne.de/oldies-but-goldies/stream/mp3"));
        knownStations.add(new RadioStation(98.5, "Wave Hits 1", "Contemporary Top 40", "New York", "https://streams.ilovemusic.de/iloveradio1.mp3"));
        knownStations.add(new RadioStation(101.3, "Classical Symphonia", "Classical & Opera", "Vienna", "https://stream.klassikradio.de/live/mp3-128"));
        knownStations.add(new RadioStation(104.7, "Rock Anthem FM", "Modern & Classic Rock", "Berlin", "https://stream.rockantenne.de/rockantenne/stream/mp3"));
        knownStations.add(new RadioStation(107.5, "Chill Lounge FM", "Ambient & Lo-Fi Beats", "Paris", "https://stream.zeno.fm/f3wvbbqmdg8uv"));
    }

    public List<RadioStation> getKnownStations() {
        return knownStations;
    }

    public double getCurrentFrequency() {
        return currentFrequency;
    }

    public RadioStation findNearestStation(double freq) {
        RadioStation closest = null;
        double minDiff = 0.25; // Locked within +/- 0.25 MHz
        for (int i = 0; i < knownStations.size(); i++) {
            RadioStation s = knownStations.get(i);
            double diff = Math.abs(s.getFrequency() - freq);
            if (diff <= minDiff) {
                closest = s;
                minDiff = diff;
            }
        }
        return closest;
    }

    public void tuneTo(double frequency) {
        // Clamp between FM bounds 87.5 MHz and 108.0 MHz
        if (frequency < 87.5) frequency = 87.5;
        if (frequency > 108.0) frequency = 108.0;

        // Round to 1 decimal place
        this.currentFrequency = Math.round(frequency * 10.0) / 10.0;

        RadioStation station = findNearestStation(currentFrequency);
        boolean locked = (station != null);

        if (listener != null) {
            listener.onStationTuned(station, currentFrequency, locked);
        }

        if (isPlaying) {
            playCurrent();
        }
    }

    public void stepFrequency(double delta) {
        double newFreq = currentFrequency + delta;
        if (newFreq > 108.0) newFreq = 87.5;
        if (newFreq < 87.5) newFreq = 108.0;
        tuneTo(newFreq);
    }

    public void seekNext() {
        for (int i = 0; i < knownStations.size(); i++) {
            RadioStation s = knownStations.get(i);
            if (s.getFrequency() > (currentFrequency + 0.1)) {
                tuneTo(s.getFrequency());
                return;
            }
        }
        // If at top of band, wrap to first station
        if (!knownStations.isEmpty()) {
            tuneTo(knownStations.get(0).getFrequency());
        }
    }

    public void seekPrevious() {
        for (int i = knownStations.size() - 1; i >= 0; i--) {
            RadioStation s = knownStations.get(i);
            if (s.getFrequency() < (currentFrequency - 0.1)) {
                tuneTo(s.getFrequency());
                return;
            }
        }
        // If at bottom of band, wrap to last station
        if (!knownStations.isEmpty()) {
            tuneTo(knownStations.get(knownStations.size() - 1).getFrequency());
        }
    }

    public void playCurrent() {
        releasePlayer();
        final RadioStation station = findNearestStation(currentFrequency);

        if (station == null) {
            // Frequency has static / white noise condition
            isPlaying = true;
            isBuffering = false;
            if (listener != null) {
                listener.onPlaybackStateChanged(true, false);
            }
            return;
        }

        try {
            isBuffering = true;
            if (listener != null) {
                listener.onPlaybackStateChanged(false, true);
            }

            mediaPlayer = new MediaPlayer();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());
            } else {
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            }

            mediaPlayer.setDataSource(context, Uri.parse(station.getStreamUrl()));
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    if (mediaPlayer != null) {
                        mediaPlayer.start();
                        isPlaying = true;
                        isBuffering = false;
                        if (listener != null) {
                            listener.onPlaybackStateChanged(true, false);
                        }
                    }
                }
            });

            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    isBuffering = false;
                    isPlaying = false;
                    if (listener != null) {
                        listener.onPlaybackStateChanged(false, false);
                        listener.onError("Radio broadcast stream unavailable. Please retry or tune.");
                    }
                    return true;
                }
            });

            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            isBuffering = false;
            isPlaying = false;
            if (listener != null) {
                listener.onPlaybackStateChanged(false, false);
                listener.onError("Unable to connect to FM frequency: " + e.getMessage());
            }
        }
    }

    public void togglePlayPause() {
        if (isPlaying || isBuffering) {
            pause();
        } else {
            playCurrent();
        }
    }

    public void pause() {
        isPlaying = false;
        isBuffering = false;
        releasePlayer();
        if (listener != null) {
            listener.onPlaybackStateChanged(false, false);
        }
    }

    private void releasePlayer() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.reset();
                mediaPlayer.release();
            } catch (Exception ignored) {
            }
            mediaPlayer = null;
        }
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public boolean isBuffering() {
        return isBuffering;
    }

    public void release() {
        releasePlayer();
    }
}