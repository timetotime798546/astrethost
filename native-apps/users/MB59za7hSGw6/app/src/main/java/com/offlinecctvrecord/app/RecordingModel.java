package com.offlinecctvrecord.app;

import java.io.File;

public class RecordingModel {
    private File file;
    private String name;
    private long sizeBytes;
    private long lastModified;

    public RecordingModel(File file) {
        this.file = file;
        this.name = file != null ? file.getName() : "Unknown";
        this.sizeBytes = file != null ? file.length() : 0;
        this.lastModified = file != null ? file.lastModified() : 0;
    }

    public File getFile() {
        return file;
    }

    public String getName() {
        return name;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public long getLastModified() {
        return lastModified;
    }

    public String getFormattedSize() {
        return StorageHelper.formatBytes(sizeBytes);
    }
}