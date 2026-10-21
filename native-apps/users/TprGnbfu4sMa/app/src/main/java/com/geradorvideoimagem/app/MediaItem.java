package com.geradorvideoimagem.app;

import java.io.File;

public class MediaItem {
    public static final int TYPE_IMAGE = 1;
    public static final int TYPE_VIDEO = 2;

    private final String title;
    private final File file;
    private final int type;
    private final long createdAt;

    public MediaItem(String title, File file, int type) {
        this.title = title;
        this.file = file;
        this.type = type;
        this.createdAt = System.currentTimeMillis();
    }

    public String getTitle() {
        return title;
    }

    public File getFile() {
        return file;
    }

    public int getType() {
        return type;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}