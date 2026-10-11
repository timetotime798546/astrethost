package com.productimagemanager.app;

public class Product {
    private String id;
    private String name;
    private double price;
    private String description;
    private String imageId;
    private String createdAt;

    public Product() {
        this.id = "";
        this.name = "";
        this.price = 0.0;
        this.description = "";
        this.imageId = "";
        this.createdAt = "";
    }

    public Product(String id, String name, double price, String description, String imageId, String createdAt) {
        this.id = id != null ? id : "";
        this.name = name != null ? name : "";
        this.price = price;
        this.description = description != null ? description : "";
        this.imageId = imageId != null ? imageId : "";
        this.createdAt = createdAt != null ? createdAt : "";
    }

    public String getId() {
        return id != null ? id : "";
    }

    public void setId(String id) {
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

    public String getCreatedAt() {
        return createdAt != null ? createdAt : "";
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}