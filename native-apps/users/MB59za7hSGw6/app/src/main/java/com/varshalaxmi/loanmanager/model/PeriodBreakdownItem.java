package com.varshalaxmi.loanmanager.model;

public class PeriodBreakdownItem {
    private String startDate;
    private String endDate;
    private String periodLabel;
    private double principal;
    private long days;
    private double interest;
    private String note;

    public PeriodBreakdownItem() {}

    public PeriodBreakdownItem(String startDate, String endDate, String periodLabel, double principal, long days, double interest, String note) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.periodLabel = periodLabel;
        this.principal = principal;
        this.days = days;
        this.interest = interest;
        this.note = note;
    }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public String getPeriodLabel() { return periodLabel; }
    public void setPeriodLabel(String periodLabel) { this.periodLabel = periodLabel; }

    public double getPrincipal() { return principal; }
    public void setPrincipal(double principal) { this.principal = principal; }

    public long getDays() { return days; }
    public void setDays(long days) { this.days = days; }

    public double getInterest() { return interest; }
    public void setInterest(double interest) { this.interest = interest; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
