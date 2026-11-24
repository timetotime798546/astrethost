package com.alfariskirana.app;

public class Purchase {
    private long id;
    private String supplierName;
    private long productId;
    private String productName;
    private double quantity;
    private double purchaseAmount;
    private String purchaseDate;

    public Purchase(long id, String supplierName, long productId, String productName, double quantity, double purchaseAmount, String purchaseDate) {
        this.id = id;
        this.supplierName = supplierName;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.purchaseAmount = purchaseAmount;
        this.purchaseDate = purchaseDate;
    }

    public long getId() { return id; }
    public String getSupplierName() { return supplierName; }
    public long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public double getQuantity() { return quantity; }
    public double getPurchaseAmount() { return purchaseAmount; }
    public String getPurchaseDate() { return purchaseDate; }
}