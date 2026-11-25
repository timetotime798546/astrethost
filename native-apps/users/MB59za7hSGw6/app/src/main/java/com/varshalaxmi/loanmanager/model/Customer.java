package com.varshalaxmi.loanmanager.model;

public class Customer {
    private long id;
    private String name;
    private String mobile;
    private String address;
    private String createdAt;
    
    // Aggregated statistical fields
    private double totalGiven;
    private double totalPaid;
    private double outstandingBalance;

    public Customer() {}

    public Customer(long id, String name, String mobile, String address, String createdAt) {
        this.id = id;
        this.name = name;
        this.mobile = mobile;
        this.address = address;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public double getTotalGiven() { return totalGiven; }
    public void setTotalGiven(double totalGiven) { this.totalGiven = totalGiven; }

    public double getTotalPaid() { return totalPaid; }
    public void setTotalPaid(double totalPaid) { this.totalPaid = totalPaid; }

    public double getOutstandingBalance() { return outstandingBalance; }
    public void setOutstandingBalance(double outstandingBalance) { this.outstandingBalance = outstandingBalance; }
}