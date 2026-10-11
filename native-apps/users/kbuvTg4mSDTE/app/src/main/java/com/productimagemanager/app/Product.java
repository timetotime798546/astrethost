package com.productimagemanager.app;

public class Product {
    private long id;
    private String name;
    private double price;
    private String description;
    private String imageId;
    private long createdAt;

    public Product() {
    }

    public Product(long id, String name, double price, String description, String imageId, long createdAt) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.description = description;
        this.imageId = imageId;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name != null ? name : "";
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getDescription() {
        return description != null ? description : "";
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageId() {
        return imageId != null ? imageId : "";
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}