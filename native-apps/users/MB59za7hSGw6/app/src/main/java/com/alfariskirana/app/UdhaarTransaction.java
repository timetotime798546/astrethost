package com.alfariskirana.app;

public class UdhaarTransaction {
    private long id;
    private long customerId;
    private String type; // "CREDIT" or "PAYMENT"
    private double amount;
    private double pendingAmount;
    private String dateTime;
    private String notes;
    private String paymentMethod;
    private long parentUdhaarId;

    public UdhaarTransaction() {}

    public UdhaarTransaction(long id, long customerId, String type, double amount, double pendingAmount, String dateTime, String notes, String paymentMethod, long parentUdhaarId) {
        this.id = id;
        this.customerId = customerId;
        this.type = type;
        this.amount = amount;
        this.pendingAmount = pendingAmount;
        this.dateTime = dateTime;
        this.notes = notes;
        this.paymentMethod = paymentMethod;
        this.parentUdhaarId = parentUdhaarId;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getCustomerId() { return customerId; }
    public void setCustomerId(long customerId) { this.customerId = customerId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public double getPendingAmount() { return pendingAmount; }
    public void setPendingAmount(double pendingAmount) { this.pendingAmount = pendingAmount; }

    public String getDateTime() { return dateTime; }
    public void setDateTime(String dateTime) { this.dateTime = dateTime; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public long getParentUdhaarId() { return parentUdhaarId; }
    public void setParentUdhaarId(long parentUdhaarId) { this.parentUdhaarId = parentUdhaarId; }
}