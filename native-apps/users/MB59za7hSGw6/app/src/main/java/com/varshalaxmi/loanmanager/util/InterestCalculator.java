package com.varshalaxmi.loanmanager.util;

import com.varshalaxmi.loanmanager.model.LoanAddition;
import com.varshalaxmi.loanmanager.model.Payment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class InterestCalculator {

    public static class TimelineEvent {
        public String date;
        public int eventType; // 1: Addition, 2: Payment
        public double amount;
        public String paymentType;

        public TimelineEvent(String date, int eventType, double amount, String paymentType) {
            this.date = date;
            this.eventType = eventType;
            this.amount = amount;
            this.paymentType = paymentType;
        }
    }

    public static class CalculationResult {
        public double currentPrincipal;
        public double totalDisbursed;
        public double totalAccruedInterest;
        public double totalPrincipalPaid;
        public double totalInterestPaid;
        public double totalPaid;
        public double remainingBalance;
    }

    public static CalculationResult calculateLoanLedger(
            double initialPrincipal,
            double ratePercentage,
            String method,
            String issueDateStr,
            List<LoanAddition> additions,
            List<Payment> payments,
            String targetDateStr) {

        CalculationResult res = new CalculationResult();
        res.totalDisbursed = initialPrincipal;

        // Clean method string for backwards compatibility
        String cleanMethod = "No Interest";
        if (method != null) {
            if (method.toLowerCase().contains("daily")) cleanMethod = "Daily Interest";
            else if (method.toLowerCase().contains("monthly")) cleanMethod = "Monthly Interest";
        }

        if ("No Interest".equals(cleanMethod) || ratePercentage <= 0) {
            ratePercentage = 0;
            cleanMethod = "No Interest";
        }

        List<TimelineEvent> events = new ArrayList<>();
        if (additions != null) {
            for (LoanAddition add : additions) {
                events.add(new TimelineEvent(add.getAdditionDate(), 1, add.getAmount(), "ADDITION"));
                res.totalDisbursed += add.getAmount();
            }
        }

        if (payments != null) {
            for (Payment p : payments) {
                events.add(new TimelineEvent(p.getPaymentDate(), 2, p.getAmount(), p.getPaymentType()));
                res.totalPaid += p.getAmount();
            }
        }

        // Sort timeline events chronologically
        Collections.sort(events, new Comparator<TimelineEvent>() {
            @Override
            public int compare(TimelineEvent e1, TimelineEvent e2) {
                return e1.date.compareTo(e2.date);
            }
        });

        String currentDate = (issueDateStr != null && !issueDateStr.isEmpty()) ? issueDateStr : getTodayDate();
        double activePrincipal = initialPrincipal;
        double accumulatedAccruedInterest = 0.0;
        double accumulatedInterestPaid = 0.0;
        double accumulatedPrincipalPaid = 0.0;

        for (TimelineEvent event : events) {
            if (event.date.compareTo(currentDate) > 0) {
                long days = getDaysBetween(currentDate, event.date);
                double periodInterest = computePeriodInterest(activePrincipal, ratePercentage, cleanMethod, days);
                accumulatedAccruedInterest += periodInterest;
                currentDate = event.date;
            }

            if (event.eventType == 1) { // Principal Addition (Top-Up)
                activePrincipal += event.amount;
            } else if (event.eventType == 2) { // Payment
                String pType = event.paymentType != null ? event.paymentType : "Combined Payment";
                if ("Interest Payment".equalsIgnoreCase(pType)) {
                    accumulatedInterestPaid += event.amount;
                } else if ("Principal Payment".equalsIgnoreCase(pType)) {
                    accumulatedPrincipalPaid += event.amount;
                    activePrincipal = Math.max(0.0, activePrincipal - event.amount);
                } else { // Combined Payment
                    double unpaidInterest = Math.max(0.0, accumulatedAccruedInterest - accumulatedInterestPaid);
                    if (event.amount <= unpaidInterest) {
                        accumulatedInterestPaid += event.amount;
                    } else {
                        double intPortion = unpaidInterest;
                        double prinPortion = event.amount - intPortion;
                        accumulatedInterestPaid += intPortion;
                        accumulatedPrincipalPaid += prinPortion;
                        activePrincipal = Math.max(0.0, activePrincipal - prinPortion);
                    }
                }
            }
        }

        // Accrue interest from last event date up to targetDate (e.g. today)
        if (targetDateStr != null && targetDateStr.compareTo(currentDate) > 0) {
            long days = getDaysBetween(currentDate, targetDateStr);
            double periodInterest = computePeriodInterest(activePrincipal, ratePercentage, cleanMethod, days);
            accumulatedAccruedInterest += periodInterest;
        }

        res.currentPrincipal = roundTwoDecimals(activePrincipal);
        res.totalDisbursed = roundTwoDecimals(res.totalDisbursed);
        res.totalAccruedInterest = roundTwoDecimals(accumulatedAccruedInterest);
        res.totalInterestPaid = roundTwoDecimals(accumulatedInterestPaid);
        res.totalPrincipalPaid = roundTwoDecimals(accumulatedPrincipalPaid);
        res.totalPaid = roundTwoDecimals(res.totalPaid);

        double netInterestDue = Math.max(0.0, res.totalAccruedInterest - res.totalInterestPaid);
        res.remainingBalance = roundTwoDecimals(res.currentPrincipal + netInterestDue);

        return res;
    }

    public static double computePeriodInterest(double principal, double ratePercentage, String method, long days) {
        if (principal <= 0 || ratePercentage <= 0 || days <= 0 || "No Interest".equalsIgnoreCase(method)) {
            return 0.0;
        }

        double rateFraction = ratePercentage / 100.0;
        double interest = 0.0;

        if ("Daily Interest".equalsIgnoreCase(method)) {
            interest = principal * rateFraction * days;
        } else if ("Monthly Interest".equalsIgnoreCase(method)) {
            interest = principal * rateFraction * (days / 30.0);
        }

        return interest;
    }

    public static long getDaysBetween(String startDateStr, String endDateStr) {
        if (startDateStr == null || endDateStr == null) return 0;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        try {
            Date d1 = sdf.parse(startDateStr);
            Date d2 = sdf.parse(endDateStr);
            long diffMillis = d2.getTime() - d1.getTime();
            return Math.max(0, diffMillis / (24 * 60 * 60 * 1000));
        } catch (Exception e) {
            return 0;
        }
    }

    public static String getTodayDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    private static double roundTwoDecimals(double val) {
        BigDecimal bd = new BigDecimal(val);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}