package com.expensetracker.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mandatory offline calculation engine implemented purely in native Java.
 * Computes balances, categories distribution, and monthly aggregations locally.
 */
public class ExpenseCalculator {

    public static double calculateTotalIncome(List<TransactionModel> transactions) {
        double sum = 0.0;
        if (transactions == null) return sum;
        for (int i = 0; i < transactions.size(); i++) {
            TransactionModel item = transactions.get(i);
            if (!item.isExpense()) {
                sum += item.getAmount();
            }
        }
        return Math.round(sum * 100.0) / 100.0;
    }

    public static double calculateTotalExpense(List<TransactionModel> transactions) {
        double sum = 0.0;
        if (transactions == null) return sum;
        for (int i = 0; i < transactions.size(); i++) {
            TransactionModel item = transactions.get(i);
            if (item.isExpense()) {
                sum += item.getAmount();
            }
        }
        return Math.round(sum * 100.0) / 100.0;
    }

    public static double calculateBalance(List<TransactionModel> transactions) {
        double income = calculateTotalIncome(transactions);
        double expense = calculateTotalExpense(transactions);
        return Math.round((income - expense) * 100.0) / 100.0;
    }

    public static Map<String, Double> calculateExpensesByCategory(List<TransactionModel> transactions) {
        Map<String, Double> map = new HashMap<String, Double>();
        if (transactions == null) return map;
        for (int i = 0; i < transactions.size(); i++) {
            TransactionModel item = transactions.get(i);
            if (item.isExpense()) {
                String cat = item.getCategory();
                if (cat == null || cat.trim().isEmpty()) {
                    cat = "Other";
                }
                Double existing = map.get(cat);
                double val = (existing != null ? existing : 0.0) + item.getAmount();
                map.put(cat, Math.round(val * 100.0) / 100.0);
            }
        }
        return map;
    }

    public static List<TransactionModel> filterByMonth(List<TransactionModel> transactions, String yearMonth) {
        List<TransactionModel> filtered = new ArrayList<TransactionModel>();
        if (transactions == null || yearMonth == null) return filtered;
        for (int i = 0; i < transactions.size(); i++) {
            TransactionModel item = transactions.get(i);
            if (item.getDate() != null && item.getDate().startsWith(yearMonth)) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    public static double calculatePercentage(double portion, double total) {
        if (total <= 0.0) return 0.0;
        double p = (portion / total) * 100.0;
        return Math.round(p * 10.0) / 10.0;
    }
}