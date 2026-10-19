package com.payquick.app;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class WalletCalculator {

    public static final double CASHBACK_PERCENTAGE = 0.02; // 2% Cashback bonus

    public static double calculateCashback(double depositAmount) {
        if (depositAmount <= 0) {
            return 0.0;
        }
        BigDecimal amount = BigDecimal.valueOf(depositAmount);
        BigDecimal rate = BigDecimal.valueOf(CASHBACK_PERCENTAGE);
        BigDecimal cashback = amount.multiply(rate);
        return cashback.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public static double calculateTotalCredit(double depositAmount) {
        if (depositAmount <= 0) {
            return 0.0;
        }
        double cashback = calculateCashback(depositAmount);
        BigDecimal total = BigDecimal.valueOf(depositAmount).add(BigDecimal.valueOf(cashback));
        return total.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public static double round(double val) {
        BigDecimal bd = BigDecimal.valueOf(val);
        return bd.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}