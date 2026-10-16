package com.dukaanprofit.app.models;

public class Product {
    private long id;
    private String name;
    private String sku;
    private double costPrice;
    private double sellPrice;
    private int quantity;
    private int lowStockThreshold;

    public Product(long id, String name, String sku, double costPrice, double sellPrice, int quantity, int lowStockThreshold) {
        this.id = id;
        this.name = name;
        this.sku = sku;
        this.costPrice = costPrice;
        this.sellPrice = sellPrice;
        this.quantity = quantity;
        this.lowStockThreshold = lowStockThreshold;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public String getSku() { return sku; }
    public double getCostPrice() { return costPrice; }
    public double getSellPrice() { return sellPrice; }
    public int getQuantity() { return quantity; }
    public int getLowStockThreshold() { return lowStockThreshold; }

    public void setQuantity(int quantity) { this.quantity = quantity; }

    public boolean isLowStock() {
        return quantity <= lowStockThreshold;
    }

    public double getInventoryCostValue() {
        return quantity * costPrice;
    }

    public double getInventoryRetailValue() {
        return quantity * sellPrice;
    }

    @Override
    public String toString() {
        return name + " (" + sku + ") - ₹" + String.format("%.2f", sellPrice) + " [Stock: " + quantity + "]";
    }
}