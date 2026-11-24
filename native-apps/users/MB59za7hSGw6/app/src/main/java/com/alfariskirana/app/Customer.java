package com.alfariskirana.app;

public class Customer {
    private long id;
    private String name;
    private String mobile;
    private double totalUdhaar;
    private double totalJama;
    private double pendingBalance;
    private double advanceBalance;

    public Customer() {}

    public Customer(long id, String name, String mobile, double totalUdhaar, double totalJama, double pendingBalance, double advanceBalance) {
        this.id = id;
        this.name = name;
        this.mobile = mobile;
        this.totalUdhaar = totalUdhaar;
        this.totalJama = totalJama;
        this.pendingBalance = pendingBalance;
        this.advanceBalance = advanceBalance;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public double getTotalUdhaar() { return totalUdhaar; }
    public void setTotalUdhaar(double totalUdhaar) { this.totalUdhaar = totalUdhaar; }

    public double getTotalJama() { return totalJama; }
    public void setTotalJama(double totalJama) { this.totalJama = totalJama; }

    public double getPendingBalance() { return pendingBalance; }
    public void setPendingBalance(double pendingBalance) { this.pendingBalance = pendingBalance; }

    public double getAdvanceBalance() { return advanceBalance; }
    public void setAdvanceBalance(double advanceBalance) { this.advanceBalance = advanceBalance; }
}