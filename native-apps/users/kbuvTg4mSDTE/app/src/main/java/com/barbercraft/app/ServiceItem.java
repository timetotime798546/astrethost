package com.barbercraft.app;

import java.io.Serializable;

public class ServiceItem implements Serializable {
    private String id;
    private String name;
    private String description;
    private double price;
    private int durationMinutes;
    private String thumbnailUrl;
    private String[] galleryImages;

    public ServiceItem(String id, String name, String description, double price, int durationMinutes, String thumbnailUrl, String[] galleryImages) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.durationMinutes = durationMinutes;
        this.thumbnailUrl = thumbnailUrl;
        this.galleryImages = galleryImages;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }
    public int getDurationMinutes() { return durationMinutes; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public String[] getGalleryImages() { return galleryImages; }
}