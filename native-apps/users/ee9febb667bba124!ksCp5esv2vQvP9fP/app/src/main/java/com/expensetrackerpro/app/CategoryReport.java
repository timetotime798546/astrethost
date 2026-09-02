package com.expensetrackerpro.app;

public class CategoryReport {
    private String category;
    private double amount;
    private int percentage;

    public CategoryReport(String category, double amount, int percentage) {
        this.category = category;
        this.amount = amount;
        this.percentage = percentage;
    }

    public String getCategory() { return category; }
    public double getAmount() { return amount; }
    public int getPercentage() { return percentage; }
}