package com.expensetrackerpro.app;

public class Transaction {
    private int id;
    private double amount;
    private String type; // INCOME or EXPENSE
    private String category;
    private String date;
    private String note;

    public Transaction(int id, double amount, String type, String category, String date, String note) {
        this.id = id;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.date = date;
        this.note = note;
    }

    public int getId() { return id; }
    public double getAmount() { return amount; }
    public String getType() { return type; }
    public String getCategory() { return category; }
    public String getDate() { return date; }
    public String getNote() { return note; }
}