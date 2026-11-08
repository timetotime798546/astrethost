package com.youtubebackgroundplayer.app;

import java.util.ArrayList;
import java.util.List;

public class AudioTrack {

    private String id;
    private String title;
    private String artist;
    private String category;
    private int durationSeconds;
    private String streamUrl;
    private String icon;
    private boolean isFavorite;

    public AudioTrack(String id, String title, String artist, String category, int durationSeconds, String streamUrl, String icon) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.category = category;
        this.durationSeconds = durationSeconds;
        this.streamUrl = streamUrl;
        this.icon = icon;
        this.isFavorite = false;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getCategory() { return category; }
    public int getDurationSeconds() { return durationSeconds; }
    public String getStreamUrl() { return streamUrl; }
    public String getIcon() { return icon; }
    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }

    public String getFormattedDuration() {
        int minutes = durationSeconds / 60;
        int seconds = durationSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    public static List<AudioTrack> getSampleCatalog() {
        List<AudioTrack> catalog = new ArrayList<AudioTrack>();
        catalog.add(new AudioTrack("1", "Synthwave Synth Odyssey", "YouTube Chill Audio", "Trending", 212, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3", "🎵"));
        catalog.add(new AudioTrack("2", "Lo-Fi Midnight Study Beat", "Lofi Girl Stream", "Lo-Fi", 425, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3", "☕"));
        catalog.add(new AudioTrack("3", "Tech Talks & Background Deep Dive", "Android Dev Podcast", "Podcasts", 320, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3", "🎙️"));
        catalog.add(new AudioTrack("4", "Acoustic Sunset Melody", "Royalty Free Ambient", "Trending", 185, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3", "🎸"));
        catalog.add(new AudioTrack("5", "Rainy Window Piano Dreams", "Relaxation Waves", "Lo-Fi", 240, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3", "🎹"));
        catalog.add(new AudioTrack("6", "Deep Work Focus Electro", "Background Beats", "Trending", 295, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3", "🎧"));
        catalog.add(new AudioTrack("7", "AI & Future Technology Ep. 42", "Futurist Weekly", "Podcasts", 510, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3", "📡"));
        catalog.add(new AudioTrack("8", "Coffee Shop Ambient Jazz", "Background Audio", "Lo-Fi", 310, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3", "🎷"));
        return catalog;
    }
}