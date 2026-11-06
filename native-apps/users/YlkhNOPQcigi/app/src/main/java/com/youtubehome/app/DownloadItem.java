package com.youtubehome.app;

public class DownloadItem {
    private String fileName;
    private String downloadUrl;
    private String timestamp;
    private String status;

    public DownloadItem(String fileName, String downloadUrl, String timestamp, String status) {
        this.fileName = fileName;
        this.downloadUrl = downloadUrl;
        this.timestamp = timestamp;
        this.status = status;
    }

    public String getFileName() {
        return fileName;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getStatus() {
        return status;
    }
}