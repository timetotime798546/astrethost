package com.smartocrscanner.app;

public class ScanModel {
    private long id;
    private String text;
    private String source;
    private String timestamp;
    private int charCount;

    public ScanModel(long id, String text, String source, String timestamp, int charCount) {
        this.id = id;
        this.text = text;
        this.source = source;
        this.timestamp = timestamp;
        this.charCount = charCount;
    }

    public long getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public String getSource() {
        return source;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public int getCharCount() {
        return charCount;
    }
}