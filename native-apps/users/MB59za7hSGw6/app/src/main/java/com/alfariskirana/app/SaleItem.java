package com.alfariskirana.app;

public class SaleItem {
    private long productId;
    private String productName;
    private double quantity;
    private double unitPrice;
    private double totalPrice;
    private String unit;

    public SaleItem(long productId, String productName, double quantity, double unitPrice, double totalPrice, String unit) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = totalPrice;
        this.unit = unit;
    }

    public long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { 
        this.quantity = quantity; 
        this.totalPrice = this.quantity * this.unitPrice;
    }
    public double getUnitPrice() { return unitPrice; }
    public double getTotalPrice() { return totalPrice; }
    public String getUnit() { return unit; }
}