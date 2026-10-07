package com.shozicapremor.app;

public class Product {
    private final String id;
    private final String name;
    private final String category;
    private final double price;
    private final String description;
    private final String[] imageUrls;

    public Product(String id, String name, String category, double price, String description, String[] imageUrls) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.description = description;
        this.imageUrls = imageUrls;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public double getPrice() { return price; }
    public String getDescription() { return description; }
    public String[] getImageUrls() { return imageUrls; }
}