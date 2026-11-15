package com.lockplaytube.app;

public class MediaTrack {
    private String id;
    private String title;
    private String artist;
    private String category;
    private String streamUrl;
    private String durationStr;
    private String imageUrl;

    public MediaTrack(String id, String title, String artist, String category, String streamUrl, String durationStr, String imageUrl) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.category = category;
        this.streamUrl = streamUrl;
        this.durationStr = durationStr;
        this.imageUrl = imageUrl;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getCategory() {
        return category;
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    public String getDurationStr() {
        return durationStr;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}