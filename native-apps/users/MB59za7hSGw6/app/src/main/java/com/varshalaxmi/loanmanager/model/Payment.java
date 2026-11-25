package com.varshalaxmi.loanmanager.model;

public class Payment {
    private long id;
    private long loanId;
    private long customerId;
    private String customerName;
    private double amount;
    private String paymentType; // Combined Payment, Principal Payment, Interest Payment
    private String paymentDate;
    private String note;

    public Payment() {}

    public Payment(long id, long loanId, long customerId, String customerName, double amount, String paymentType, String paymentDate, String note) {
        this.id = id;
        this.loanId = loanId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.amount = amount;
        this.paymentType = paymentType;
        this.paymentDate = paymentDate;
        this.note = note;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getLoanId() { return loanId; }
    public void setLoanId(long loanId) { this.loanId = loanId; }

    public long getCustomerId() { return customerId; }
    public void setCustomerId(long customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }

    public String getPaymentDate() { return paymentDate; }
    public void setPaymentDate(String paymentDate) { this.paymentDate = paymentDate; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}