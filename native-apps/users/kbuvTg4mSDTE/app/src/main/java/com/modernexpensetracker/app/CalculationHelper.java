package com.modernexpensetracker.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalculationHelper {

    /**
     * Calculates total income from a list of transactions.
     * All calculations are executed locally in native Java.
     */
    public static double calculateTotalIncome(List<TransactionModel> transactions) {
        double sum = 0.0;
        if (transactions == null) return sum;
        for (int i = 0; i < transactions.size(); i++) {
            TransactionModel t = transactions.get(i);
            if (!t.isExpense()) {
                sum += t.getAmount();
            }
        }
        return Math.round(sum * 100.0) / 100.0;
    }

    /**
     * Calculates total expenses from a list of transactions.
     */
    public static double calculateTotalExpense(List<TransactionModel> transactions) {
        double sum = 0.0;
        if (transactions == null) return sum;
        for (int i = 0; i < transactions.size(); i++) {
            TransactionModel t = transactions.get(i);
            if (t.isExpense()) {
                sum += t.getAmount();
            }
        }
        return Math.round(sum * 100.0) / 100.0;
    }

    /**
     * Calculates net balance (income - expense).
     */
    public static double calculateNetBalance(double totalIncome, double totalExpense) {
        return Math.round((totalIncome - totalExpense) * 100.0) / 100.0;
    }

    /**
     * Calculates savings rate percentage.
     */
    public static double calculateSavingsRate(double totalIncome, double totalExpense) {
        if (totalIncome <= 0.0) {
            return 0.0;
        }
        double rate = ((totalIncome - totalExpense) / totalIncome) * 100.0;
        return Math.round(rate * 10.0) / 10.0;
    }

    /**
     * Calculates category totals and percentages for expenses.
     */
    public static List<CategorySummary> calculateCategoryBreakdown(List<TransactionModel> transactions) {
        List<CategorySummary> summaries = new ArrayList<CategorySummary>();
        if (transactions == null || transactions.isEmpty()) {
            return summaries;
        }

        Map<String, Double> map = new HashMap<String, Double>();
        double totalExpense = 0.0;

        for (int i = 0; i < transactions.size(); i++) {
            TransactionModel t = transactions.get(i);
            if (t.isExpense()) {
                totalExpense += t.getAmount();
                Double cur = map.get(t.getCategory());
                if (cur == null) {
                    map.put(t.getCategory(), t.getAmount());
                } else {
                    map.put(t.getCategory(), cur + t.getAmount());
                }
            }
        }

        if (totalExpense <= 0.0) {
            return summaries;
        }

        for (Map.Entry<String, Double> entry : map.entrySet()) {
            double catAmount = Math.round(entry.getValue() * 100.0) / 100.0;
            double percent = Math.round((catAmount / totalExpense) * 1000.0) / 10.0;
            summaries.add(new CategorySummary(entry.getKey(), catAmount, percent));
        }

        Collections.sort(summaries, new Comparator<CategorySummary>() {
            @Override
            public int compare(CategorySummary o1, CategorySummary o2) {
                return Double.compare(o2.getTotalAmount(), o1.getTotalAmount());
            }
        });

        return summaries;
    }

    public static String getCategoryIcon(String category) {
        if (category == null) return "📦";
        String lower = category.toLowerCase();
        if (lower.contains("food") || lower.contains("grocer") || lower.contains("dining")) return "🍔";
        if (lower.contains("salary") || lower.contains("paycheck") || lower.contains("bonus")) return "💵";
        if (lower.contains("rent") || lower.contains("mortgage") || lower.contains("home")) return "🏠";
        if (lower.contains("transport") || lower.contains("fuel") || lower.contains("car")) return "🚗";
        if (lower.contains("shop") || lower.contains("cloth")) return "🛍️";
        if (lower.contains("bill") || lower.contains("utilit")) return "💡";
        if (lower.contains("health") || lower.contains("medic")) return "💊";
        if (lower.contains("entertain") || lower.contains("movie")) return "🎬";
        if (lower.contains("invest") || lower.contains("dividend")) return "📈";
        return "💳";
    }
}