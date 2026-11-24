package com.alfariskirana.app;

public class UdhaarTransaction {
    private long id;
    private long customerId;
    private String type; // "CREDIT" or "PAYMENT"
    private double amount;
    private String dateTime;
    private String notes;
    private String paymentMethod;
    private double runningPending;

    public UdhaarTransaction() {}

    public UdhaarTransaction(long id, long customerId, String type, double amount, String dateTime, String notes, String paymentMethod, double runningPending) {
        this.id = id;
        this.customerId = customerId;
        this.type = type;
        this.amount = amount;
        this.dateTime = dateTime;
        this.notes = notes;
        this.paymentMethod = paymentMethod;
        this.runningPending = runningPending;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getCustomerId() { return customerId; }
    public void setCustomerId(long customerId) { this.customerId = customerId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getDateTime() { return dateTime; }
    public void setDateTime(String dateTime) { this.dateTime = dateTime; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public double getRunningPending() { return runningPending; }
    public void setRunningPending(double runningPending) { this.runningPending = runningPending; }
}