package com.locktrutube.player;

public class HistoryItem {
    private long id;
    private String title;
    private String url;
    private long timestamp;

    public HistoryItem(long id, String title, String url, long timestamp) {
        this.id = id;
        this.title = title;
        this.url = url;
        this.timestamp = timestamp;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public long getTimestamp() {
        return timestamp;
    }
}