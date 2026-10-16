package com.dukaanprofit.app.utils;

public class ProfitCalculator {

    public static double calculateGrossProfit(double salesRevenue, double costOfGoodsSold) {
        return salesRevenue - costOfGoodsSold;
    }

    public static double calculateNetProfit(double grossProfit, double totalExpenses) {
        return grossProfit - totalExpenses;
    }

    public static double calculateMarginPercentage(double netProfit, double revenue) {
        if (revenue <= 0) return 0.0;
        return (netProfit / revenue) * 100.0;
    }
}