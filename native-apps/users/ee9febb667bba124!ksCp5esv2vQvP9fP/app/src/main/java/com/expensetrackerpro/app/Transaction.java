package com.expensetrackerpro.app;

public class Transaction {
    private int id;
    private String type; // "INCOME" or "EXPENSE"
    private double amount;
    private String category;
    private String date; // YYYY-MM-DD
    private String note;

    public Transaction(int id, String type, double amount, String category, String date, String note) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.note = note;
    }

    public int getId() { return id; }
    public String getType() { return type; }
    public double getAmount() { return amount; }
    public String getCategory() { return category; }
    public String getDate() { return date; }
    public String getNote() { return note; }
}