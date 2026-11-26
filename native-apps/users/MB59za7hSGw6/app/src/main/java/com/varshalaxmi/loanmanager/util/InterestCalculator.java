package com.varshalaxmi.loanmanager.util;

import com.varshalaxmi.loanmanager.model.LoanAddition;
import com.varshalaxmi.loanmanager.model.Payment;
import com.varshalaxmi.loanmanager.model.PeriodBreakdownItem;
import com.varshalaxmi.loanmanager.model.PeriodBreakdownResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

public class InterestCalculator {

    private static final String RUPEE = "\u20B9";

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

    public static PeriodBreakdownResult computePeriodBreakdown(
            double initialPrincipal,
            double ratePercentage,
            String method,
            String issueDateStr,
            List<LoanAddition> additions,
            List<Payment> payments,
            String calcStartDateStr,
            String calcEndDateStr,
            String durationLabel) {

        PeriodBreakdownResult res = new PeriodBreakdownResult();
        res.setLoanStartDate((issueDateStr != null && !issueDateStr.isEmpty()) ? issueDateStr : getTodayDate());

        String start = (calcStartDateStr != null && !calcStartDateStr.isEmpty()) ? calcStartDateStr : res.getLoanStartDate();
        String end = (calcEndDateStr != null && !calcEndDateStr.isEmpty()) ? calcEndDateStr : getTodayDate();

        if (end.compareTo(start) < 0) {
            end = start;
        }

        res.setCalcStartDate(start);
        res.setCalcEndDate(end);
        res.setSelectedDuration(durationLabel != null ? durationLabel : "Custom Date");

        String cleanMethod = "No Interest";
        if (method != null) {
            if (method.toLowerCase().contains("daily")) cleanMethod = "Daily Interest";
            else if (method.toLowerCase().contains("monthly")) cleanMethod = "Monthly Interest";
        }
        if ("No Interest".equals(cleanMethod) || ratePercentage <= 0) {
            ratePercentage = 0;
            cleanMethod = "No Interest";
        }

        List<TimelineEvent> allEvents = new ArrayList<>();
        if (additions != null) {
            for (LoanAddition add : additions) {
                allEvents.add(new TimelineEvent(add.getAdditionDate(), 1, add.getAmount(), "ADDITION"));
            }
        }
        if (payments != null) {
            for (Payment p : payments) {
                allEvents.add(new TimelineEvent(p.getPaymentDate(), 2, p.getAmount(), p.getPaymentType()));
            }
        }

        Collections.sort(allEvents, new Comparator<TimelineEvent>() {
            @Override
            public int compare(TimelineEvent e1, TimelineEvent e2) {
                return e1.date.compareTo(e2.date);
            }
        });

        double activePrincipal = initialPrincipal;
        double accumulatedInterestPaid = 0.0;

        for (TimelineEvent event : allEvents) {
            if (event.date.compareTo(start) < 0) {
                if (event.eventType == 1) {
                    activePrincipal += event.amount;
                } else if (event.eventType == 2) {
                    if ("Principal Payment".equalsIgnoreCase(event.paymentType)) {
                        activePrincipal = Math.max(0.0, activePrincipal - event.amount);
                    } else if ("Interest Payment".equalsIgnoreCase(event.paymentType)) {
                        accumulatedInterestPaid += event.amount;
                    } else {
                        activePrincipal = Math.max(0.0, activePrincipal - event.amount);
                    }
                }
            }
        }

        TreeSet<String> dateMilestones = new TreeSet<>();
        dateMilestones.add(start);
        dateMilestones.add(end);

        for (TimelineEvent event : allEvents) {
            if (event.date.compareTo(start) >= 0 && event.date.compareTo(end) <= 0) {
                dateMilestones.add(event.date);
            }
        }

        List<String> sortedMilestones = new ArrayList<>(dateMilestones);
        double totalCalculatedInterest = 0.0;
        List<PeriodBreakdownItem> itemsList = new ArrayList<>();

        for (int i = 0; i < sortedMilestones.size() - 1; i++) {
            String periodStart = sortedMilestones.get(i);
            String periodEnd = sortedMilestones.get(i + 1);

            StringBuilder noteBuilder = new StringBuilder();
            for (TimelineEvent event : allEvents) {
                if (event.date.equals(periodStart) && event.date.compareTo(start) >= 0) {
                    if (event.eventType == 1) {
                        activePrincipal += event.amount;
                        if (noteBuilder.length() > 0) noteBuilder.append("; ");
                        noteBuilder.append("Top-Up Loan +").append(RUPEE).append(String.format(Locale.US, "%.2f", event.amount));
                    } else if (event.eventType == 2) {
                        if ("Principal Payment".equalsIgnoreCase(event.paymentType)) {
                            activePrincipal = Math.max(0.0, activePrincipal - event.amount);
                            if (noteBuilder.length() > 0) noteBuilder.append("; ");
                            noteBuilder.append("Principal Paid -").append(RUPEE).append(String.format(Locale.US, "%.2f", event.amount));
                        } else if ("Interest Payment".equalsIgnoreCase(event.paymentType)) {
                            accumulatedInterestPaid += event.amount;
                            if (noteBuilder.length() > 0) noteBuilder.append("; ");
                            noteBuilder.append("Interest Paid ").append(RUPEE).append(String.format(Locale.US, "%.2f", event.amount));
                        } else {
                            activePrincipal = Math.max(0.0, activePrincipal - event.amount);
                            if (noteBuilder.length() > 0) noteBuilder.append("; ");
                            noteBuilder.append("Repayment -").append(RUPEE).append(String.format(Locale.US, "%.2f", event.amount));
                        }
                    }
                }
            }

            long days = getDaysBetween(periodStart, periodEnd);
            double periodInterest = roundTwoDecimals(computePeriodInterest(activePrincipal, ratePercentage, cleanMethod, days));
            totalCalculatedInterest += periodInterest;

            String label = formatDateLabel(periodStart) + " – " + formatDateLabel(periodEnd);
            String note = noteBuilder.length() == 0 ? "Standard Principal Accrual" : noteBuilder.toString();

            itemsList.add(new PeriodBreakdownItem(
                    periodStart,
                    periodEnd,
                    label,
                    roundTwoDecimals(activePrincipal),
                    days,
                    periodInterest,
                    note
            ));
        }

        double interestPaidInWindow = 0.0;
        for (TimelineEvent event : allEvents) {
            if (event.eventType == 2 && event.date.compareTo(start) >= 0 && event.date.compareTo(end) <= 0) {
                if ("Interest Payment".equalsIgnoreCase(event.paymentType) || "Combined Payment".equalsIgnoreCase(event.paymentType)) {
                    interestPaidInWindow += event.amount;
                }
            }
        }
        if (interestPaidInWindow == 0.0) {
            interestPaidInWindow = accumulatedInterestPaid;
        }

        res.setItems(itemsList);
        res.setTotalCalculatedInterest(roundTwoDecimals(totalCalculatedInterest));
        res.setInterestAlreadyPaid(roundTwoDecimals(interestPaidInWindow));
        res.setRemainingUnpaidInterest(roundTwoDecimals(Math.max(0.0, res.getTotalCalculatedInterest() - res.getInterestAlreadyPaid())));
        res.setCurrentOutstandingPrincipal(roundTwoDecimals(activePrincipal));
        res.setExpectedTotalPayable(roundTwoDecimals(res.getCurrentOutstandingPrincipal() + res.getRemainingUnpaidInterest()));

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

    public static String calculateEndDateForDuration(String startDateStr, String durationOption) {
        if (startDateStr == null || startDateStr.isEmpty()) startDateStr = getTodayDate();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        try {
            Date d = sdf.parse(startDateStr);
            Calendar cal = Calendar.getInstance();
            cal.setTime(d);

            if ("1 Month".equalsIgnoreCase(durationOption)) {
                cal.add(Calendar.MONTH, 1);
            } else if ("2 Months".equalsIgnoreCase(durationOption)) {
                cal.add(Calendar.MONTH, 2);
            } else if ("3 Months".equalsIgnoreCase(durationOption)) {
                cal.add(Calendar.MONTH, 3);
            } else if ("6 Months".equalsIgnoreCase(durationOption)) {
                cal.add(Calendar.MONTH, 6);
            } else if ("1 Year".equalsIgnoreCase(durationOption)) {
                cal.add(Calendar.YEAR, 1);
            }
            return sdf.format(cal.getTime());
        } catch (Exception e) {
            return startDateStr;
        }
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

    public static String formatDateLabel(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "";
        try {
            SimpleDateFormat sdfIn = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date d = sdfIn.parse(dateStr);
            SimpleDateFormat sdfOut = new SimpleDateFormat("d MMM", Locale.US);
            return sdfOut.format(d);
        } catch (Exception e) {
            return dateStr;
        }
    }

    public static String getTodayDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    public static double roundTwoDecimals(double val) {
        BigDecimal bd = new BigDecimal(val);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
