package com.barbercraft.app;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CalculationHelper {

    public static double calculateTax(double basePrice, double taxRate) {
        BigDecimal base = new BigDecimal(Double.toString(basePrice));
        BigDecimal rate = new BigDecimal(Double.toString(taxRate));
        return base.multiply(rate).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public static double calculateDiscount(double basePrice, double discountPercentage) {
        BigDecimal base = new BigDecimal(Double.toString(basePrice));
        BigDecimal rate = new BigDecimal(Double.toString(discountPercentage));
        return base.multiply(rate).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public static double calculateFinalTotal(double basePrice, double taxRate, double discountPercentage) {
        double tax = calculateTax(basePrice, taxRate);
        double discount = calculateDiscount(basePrice, discountPercentage);
        BigDecimal total = new BigDecimal(Double.toString(basePrice))
                .add(new BigDecimal(Double.toString(tax)))
                .subtract(new BigDecimal(Double.toString(discount)));
        return total.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public static double sumAppointmentsTotal(double[] amounts) {
        BigDecimal sum = BigDecimal.ZERO;
        if (amounts != null) {
            for (int i = 0; i < amounts.length; i++) {
                sum = sum.add(new BigDecimal(Double.toString(amounts[i])));
            }
        }
        return sum.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}