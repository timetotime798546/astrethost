package com.varshalaxmi.loanmanager.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;

public class InterestCalculator {

    public static double computeInterest(double principal, double annualRatePercentage, String method, String startDateStr, String endDateStr) {
        if (principal <= 0 || annualRatePercentage <= 0 || "No Interest".equalsIgnoreCase(method)) {
            return 0.0;
        }

        long days = getDaysBetween(startDateStr, endDateStr);
        if (days <= 0) days = 0;

        double rateFraction = annualRatePercentage / 100.0;
        double interest = 0.0;

        if ("Daily Interest".equalsIgnoreCase(method)) {
            // Rate per day = rate / 365
            interest = principal * (rateFraction / 365.0) * days;
        } else if ("Monthly Interest".equalsIgnoreCase(method)) {
            // Interest calculated monthly
            double months = days / 30.0;
            interest = principal * (rateFraction / 12.0) * months;
        } else if ("Yearly Interest".equalsIgnoreCase(method) || "Simple Interest".equalsIgnoreCase(method)) {
            double years = days / 365.0;
            interest = principal * rateFraction * years;
        } else if ("Flat Interest".equalsIgnoreCase(method)) {
            // Flat annual calculation
            double years = Math.max(1.0, days / 365.0);
            interest = principal * rateFraction * years;
        } else if ("Reducing Balance".equalsIgnoreCase(method)) {
            double years = days / 365.0;
            interest = principal * (Math.pow(1.0 + (rateFraction / 12.0), 12.0 * years) - 1.0);
        } else {
            double years = days / 365.0;
            interest = principal * rateFraction * years;
        }

        BigDecimal bd = new BigDecimal(interest);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public static long getDaysBetween(String startDateStr, String endDateStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date d1 = sdf.parse(startDateStr);
            Date d2 = endDateStr != null ? sdf.parse(endDateStr) : new Date();
            long diffMillis = d2.getTime() - d1.getTime();
            return diffMillis / (24 * 60 * 60 * 1000);
        } catch (Exception e) {
            return 0;
        }
    }
}