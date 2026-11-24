package com.alfariskirana.app;

public class SaleRecord {
    private long id;
    private long customerId;
    private String customerName;
    private double totalAmount;
    private double discount;
    private double finalAmount;
    private String paymentMode;
    private String dateTime;

    public SaleRecord(long id, long customerId, String customerName, double totalAmount, double discount, double finalAmount, String paymentMode, String dateTime) {
        this.id = id;
        this.customerId = customerId;
        this.customerName = customerName;
        this.totalAmount = totalAmount;
        this.discount = discount;
        this.finalAmount = finalAmount;
        this.paymentMode = paymentMode;
        this.dateTime = dateTime;
    }

    public long getId() { return id; }
    public long getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public double getTotalAmount() { return totalAmount; }
    public double getDiscount() { return discount; }
    public double getFinalAmount() { return finalAmount; }
    public String getPaymentMode() { return paymentMode; }
    public String getDateTime() { return dateTime; }
}