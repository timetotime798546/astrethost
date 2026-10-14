package com.vyaparpro.app;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BusinessLogic {

    public static double calculateProfit(double sellPrice, double buyPrice, int quantity) {
        return (sellPrice - buyPrice) * quantity;
    }

    public static double calculateInvoiceTotal(double subtotal, double discount, double taxPercent) {
        double discounted = subtotal - discount;
        if (discounted < 0) discounted = 0;
        double tax = (discounted * taxPercent) / 100.0;
        return discounted + tax;
    }

    public static double calculateCustomerBalance(double totalGiven, double totalReceived) {
        return totalGiven - totalReceived;
    }

    public static boolean isLowStock(int stockQuantity) {
        return stockQuantity <= 5;
    }

    public static String getFormattedCurrency(double amount) {
        return String.format(Locale.US, "₹%.2f", amount);
    }

    public static String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.US);
        return sdf.format(new Date());
    }
}