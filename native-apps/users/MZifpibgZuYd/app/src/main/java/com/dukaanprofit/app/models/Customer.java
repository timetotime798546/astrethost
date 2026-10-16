package com.dukaanprofit.app.models;

public class Customer {
    private long id;
    private String name;
    private String phone;
    private double outstandingBalance;

    public Customer(long id, String name, String phone, double outstandingBalance) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.outstandingBalance = outstandingBalance;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public double getOutstandingBalance() { return outstandingBalance; }

    @Override
    public String toString() {
        return name + (phone != null && !phone.isEmpty() ? " (" + phone + ")" : "");
    }
}