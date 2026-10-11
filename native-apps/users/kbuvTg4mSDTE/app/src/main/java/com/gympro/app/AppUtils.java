package com.gympro.app;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AppUtils {

    public static String getTodayDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    public static String calculateEndDate(String startDateStr, int durationMonths) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = sdf.parse(startDateStr);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            cal.add(Calendar.MONTH, durationMonths);
            return sdf.format(cal.getTime());
        } catch (Exception e) {
            return startDateStr;
        }
    }

    public static boolean isExpiringSoon(String endDateStr, int thresholdDays) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date endDate = sdf.parse(endDateStr);
            Date today = new Date();

            long diffInMillis = endDate.getTime() - today.getTime();
            long diffInDays = diffInMillis / (1000 * 60 * 60 * 24);

            return diffInDays >= 0 && diffInDays <= thresholdDays;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isExpired(String endDateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date endDate = sdf.parse(endDateStr);
            Date today = new Date();
            return endDate.before(today);
        } catch (Exception e) {
            return false;
        }
    }

    public static String formatCurrency(double amount) {
        return String.format(Locale.getDefault(), "₹%.2f", amount);
    }
}