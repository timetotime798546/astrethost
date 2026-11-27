package com.boxstockmanager.app;

public class StockTransaction {
    private long id;
    private long productId;
    private String productName;
    private String productCode;
    private String type; // IN or OUT
    private int quantity;
    private int previousStock;
    private int updatedStock;
    private String transactionDate;
    private String notes;

    public StockTransaction() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getProductId() { return productId; }
    public void setProductId(long productId) { this.productId = productId; }

    public String getProductName() { return productName != null ? productName : ""; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductCode() { return productCode != null ? productCode : ""; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public String getType() { return type != null ? type : "IN"; }
    public void setType(String type) { this.type = type; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getPreviousStock() { return previousStock; }
    public void setPreviousStock(int previousStock) { this.previousStock = previousStock; }

    public int getUpdatedStock() { return updatedStock; }
    public void setUpdatedStock(int updatedStock) { this.updatedStock = updatedStock; }

    public String getTransactionDate() { return transactionDate != null ? transactionDate : ""; }
    public void setTransactionDate(String transactionDate) { this.transactionDate = transactionDate; }

    public String getNotes() { return notes != null ? notes : ""; }
    public void setNotes(String notes) { this.notes = notes; }
}