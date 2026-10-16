package com.dukaanprofit.app.models;

public class Sale {
    private long id;
    private long customerId;
    private String customerName;
    private double totalAmount;
    private double totalCost;
    private double discount;
    private double finalAmount;
    private String paymentType; // "CASH" or "CREDIT"
    private String createdAt;

    public Sale(long id, long customerId, String customerName, double totalAmount, double totalCost, double discount, double finalAmount, String paymentType, String createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.customerName = customerName;
        this.totalAmount = totalAmount;
        this.totalCost = totalCost;
        this.discount = discount;
        this.finalAmount = finalAmount;
        this.paymentType = paymentType;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public long getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public double getTotalAmount() { return totalAmount; }
    public double getTotalCost() { return totalCost; }
    public double getDiscount() { return discount; }
    public double getFinalAmount() { return finalAmount; }
    public String getPaymentType() { return paymentType; }
    public String getCreatedAt() { return createdAt; }

    public double getGrossProfit() {
        return (finalAmount - totalCost);
    }
}