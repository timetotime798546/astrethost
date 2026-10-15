package com.morgbilling.app;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BillingCalculator {

    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,##0.00");

    public static double calculateItemSubtotal(double quantity, double unitPrice) {
        BigDecimal qty = BigDecimal.valueOf(quantity);
        BigDecimal price = BigDecimal.valueOf(unitPrice);
        return qty.multiply(price).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public static double calculateDiscountAmount(double subtotal, double discountPercent) {
        if (discountPercent <= 0) return 0.0;
        BigDecimal sub = BigDecimal.valueOf(subtotal);
        BigDecimal percent = BigDecimal.valueOf(discountPercent).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return sub.multiply(percent).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public static double calculateTaxAmount(double taxableAmount, double taxPercent) {
        if (taxPercent <= 0) return 0.0;
        BigDecimal base = BigDecimal.valueOf(taxableAmount);
        BigDecimal percent = BigDecimal.valueOf(taxPercent).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return base.multiply(percent).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public static double calculateInvoiceTotal(double subtotal, double discountAmount, double taxAmount) {
        BigDecimal sub = BigDecimal.valueOf(subtotal);
        BigDecimal disc = BigDecimal.valueOf(discountAmount);
        BigDecimal tax = BigDecimal.valueOf(taxAmount);
        BigDecimal total = sub.subtract(disc).add(tax);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            return 0.0;
        }
        return total.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public static double calculateBalanceDue(double totalAmount, double paidAmount) {
        BigDecimal total = BigDecimal.valueOf(totalAmount);
        BigDecimal paid = BigDecimal.valueOf(paidAmount);
        BigDecimal balance = total.subtract(paid);
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            return 0.0;
        }
        return balance.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public static String formatMoney(double amount) {
        return "$" + CURRENCY_FORMAT.format(amount);
    }

    public static String generateInvoiceNumber() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmm", Locale.US);
        return "INV-" + sdf.format(new Date());
    }

    public static String getCurrentDateTimeFormatted() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US);
        return sdf.format(new Date());
    }
}