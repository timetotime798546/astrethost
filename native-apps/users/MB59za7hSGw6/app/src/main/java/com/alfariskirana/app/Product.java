package com.alfariskirana.app;

public class Product {
    private long id;
    private String name;
    private String category;
    private double purchasePrice;
    private double sellingPrice;
    private double stock;
    private String unit;

    public Product() {}

    public Product(long id, String name, String category, double purchasePrice, double sellingPrice, double stock, String unit) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.purchasePrice = purchasePrice;
        this.sellingPrice = sellingPrice;
        this.stock = stock;
        this.unit = unit;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(double purchasePrice) { this.purchasePrice = purchasePrice; }

    public double getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(double sellingPrice) { this.sellingPrice = sellingPrice; }

    public double getStock() { return stock; }
    public void setStock(double stock) { this.stock = stock; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
}