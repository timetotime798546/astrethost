package com.modernexpensetracker.app;

public class CategorySummary {
    private String category;
    private double totalAmount;
    private double percentage;

    public CategorySummary(String category, double totalAmount, double percentage) {
        this.category = category;
        this.totalAmount = totalAmount;
        this.percentage = percentage;
    }

    public String getCategory() {
        return category;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public double getPercentage() {
        return percentage;
    }
}