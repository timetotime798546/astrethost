package com.alfariskirana.app;

public class Customer {
    private long id;
    private String name;
    private String mobile;
    private double pendingBalance;

    public Customer() {}

    public Customer(long id, String name, String mobile, double pendingBalance) {
        this.id = id;
        this.name = name;
        this.mobile = mobile;
        this.pendingBalance = pendingBalance;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public double getPendingBalance() { return pendingBalance; }
    public void setPendingBalance(double pendingBalance) { this.pendingBalance = pendingBalance; }
}