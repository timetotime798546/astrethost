package com.inventorysalesmanager.app;

public class Product {
    private long id;
    private String name;
    private String sku;
    private double purchasePrice;
    private double sellingPrice;
    private int stockQty;
    private int minStock;

    public Product() {
    }

    public Product(long id, String name, String sku, double purchasePrice, double sellingPrice, int stockQty, int minStock) {
        this.id = id;
        this.name = name;
        this.sku = sku;
        this.purchasePrice = purchasePrice;
        this.sellingPrice = sellingPrice;
        this.stockQty = stockQty;
        this.minStock = minStock;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public double getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(double purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public double getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(double sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public int getStockQty() {
        return stockQty;
    }

    public void setStockQty(int stockQty) {
        this.stockQty = stockQty;
    }

    public int getMinStock() {
        return minStock;
    }

    public void setMinStock(int minStock) {
        this.minStock = minStock;
    }

    public boolean isLowStock() {
        return this.stockQty <= this.minStock;
    }

    @Override
    public String toString() {
        return name + " (" + sku + ") - Qty: " + stockQty;
    }
}