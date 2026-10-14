package com.caresyncpro.app;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class BillingCalculator {

    private static final DecimalFormat df = new DecimalFormat("0.00");

    // Local deterministic calculation for consultation/prescription checkout
    public static double calculateNetTotal(double subtotal, double discount, double tax) {
        double discounted = subtotal - discount;
        if (discounted < 0) {
            discounted = 0;
        }
        double net = discounted + tax;
        return Math.round(net * 100.0) / 100.0;
    }

    public static String formatCurrency(double amount) {
        return "$" + df.format(amount);
    }

    // Deterministic calculation of patient age from YYYY-MM-DD
    public static int calculateAge(String dobStr) {
        if (dobStr == null || dobStr.trim().length() == 0) {
            return 0;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date dob = sdf.parse(dobStr.trim());
            Calendar dobCal = Calendar.getInstance();
            dobCal.setTime(dob);

            Calendar today = Calendar.getInstance();
            int age = today.get(Calendar.YEAR) - dobCal.get(Calendar.YEAR);
            if (today.get(Calendar.DAY_OF_YEAR) < dobCal.get(Calendar.DAY_OF_YEAR)) {
                age--;
            }
            return Math.max(age, 0);
        } catch (Exception e) {
            return 0;
        }
    }

    // Deterministic expiration check: alerts if expiry is within 30 days
    public static boolean isExpiringWithinDays(String expiryDateStr, int daysThreshold) {
        if (expiryDateStr == null || expiryDateStr.trim().length() == 0) {
            return false;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date expiry = sdf.parse(expiryDateStr.trim());
            Calendar expCal = Calendar.getInstance();
            expCal.setTime(expiry);

            Calendar limitCal = Calendar.getInstance();
            limitCal.add(Calendar.DAY_OF_YEAR, daysThreshold);

            return expCal.before(limitCal);
        } catch (Exception e) {
            return false;
        }
    }
}