package com.quickpaywallet.app;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class CalculationHelper {

    private CalculationHelper() {}

    /**
     * Calculates cashback reward: deposit_amount * (cashback_percent / 100.0)
     */
    public static double calculateCashback(double amount, double cashbackPercent) {
        if (amount <= 0 || cashbackPercent <= 0) {
            return 0.0;
        }
        BigDecimal bAmount = BigDecimal.valueOf(amount);
        BigDecimal bPercent = BigDecimal.valueOf(cashbackPercent);
        BigDecimal hundred = BigDecimal.valueOf(100.0);
        BigDecimal result = bAmount.multiply(bPercent).divide(hundred, 2, RoundingMode.HALF_UP);
        return result.doubleValue();
    }

    /**
     * Calculates total credit amount: deposit_amount + cashback
     */
    public static double calculateTotalCredit(double amount, double cashback) {
        BigDecimal bAmount = BigDecimal.valueOf(amount);
        BigDecimal bCashback = BigDecimal.valueOf(cashback);
        return bAmount.add(bCashback).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * Computes sum of total credited amount from fetched transaction records
     */
    public static double calculateTotalBalance(List<Double> credits) {
        if (credits == null || credits.isEmpty()) {
            return 0.0;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < credits.size(); i++) {
            Double val = credits.get(i);
            if (val != null && val > 0) {
                sum = sum.add(BigDecimal.valueOf(val));
            }
        }
        return sum.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * Validates whether requested deposit is within dynamic admin bounds
     */
    public static boolean isWithinLimits(double amount, double minLimit, double maxLimit) {
        return amount >= minLimit && amount <= maxLimit;
    }
}