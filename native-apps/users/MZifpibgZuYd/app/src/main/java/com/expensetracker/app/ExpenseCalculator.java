package com.expensetracker.app;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpenseCalculator {

    public static double calculateTotalIncome(List<TransactionModel> transactions) {
        double total = 0.0;
        if (transactions == null) return total;
        for (int i = 0; i < transactions.size(); i++) {
            TransactionModel t = transactions.get(i);
            if (t.isIncome()) {
                total += t.getAmount();
            }
        }
        return total;
    }

    public static double calculateTotalExpense(List<TransactionModel> transactions) {
        double total = 0.0;
        if (transactions == null) return total;
        for (int i = 0; i < transactions.size(); i++) {
            TransactionModel t = transactions.get(i);
            if (!t.isIncome()) {
                total += t.getAmount();
            }
        }
        return total;
    }

    public static double calculateBalance(double totalIncome, double totalExpense) {
        return totalIncome - totalExpense;
    }

    public static Map<String, Double> calculateCategoryExpenses(List<TransactionModel> transactions, String filterMonth) {
        Map<String, Double> map = new HashMap<String, Double>();
        if (transactions == null) return map;

        for (int i = 0; i < transactions.size(); i++) {
            TransactionModel t = transactions.get(i);
            if (!t.isIncome()) {
                if (filterMonth == null || filterMonth.isEmpty() || (t.getDate() != null && t.getDate().startsWith(filterMonth))) {
                    String cat = t.getCategory();
                    if (cat == null || cat.trim().isEmpty()) {
                        cat = "General";
                    }
                    double existing = map.containsKey(cat) ? map.get(cat) : 0.0;
                    map.put(cat, existing + t.getAmount());
                }
            }
        }
        return map;
    }

    public static double calculatePercentage(double partial, double total) {
        if (total <= 0.0001) return 0.0;
        return (partial / total) * 100.0;
    }
}