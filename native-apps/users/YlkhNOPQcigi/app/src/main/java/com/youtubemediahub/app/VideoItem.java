package com.youtubemediahub.app;

public class VideoItem {
    private String title;
    private String channel;
    private String duration;
    private String youtubeUrl;
    private String streamUrl;
    private String category;

    public VideoItem(String title, String channel, String duration, String youtubeUrl, String streamUrl, String category) {
        this.title = title;
        this.channel = channel;
        this.duration = duration;
        this.youtubeUrl = youtubeUrl;
        this.streamUrl = streamUrl;
        this.category = category;
    }

    public String getTitle() {
        return title;
    }

    public String getChannel() {
        return channel;
    }

    public String getDuration() {
        return duration;
    }

    public String getYoutubeUrl() {
        return youtubeUrl;
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    public String getCategory() {
        return category;
    }
}