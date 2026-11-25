package com.varshalaxmi.loanmanager.model;

import java.util.ArrayList;
import java.util.List;

public class PeriodBreakdownResult {
    private String loanStartDate;
    private String calcStartDate;
    private String calcEndDate;
    private String selectedDuration;
    private List<PeriodBreakdownItem> items = new ArrayList<>();
    
    private double totalCalculatedInterest;
    private double interestAlreadyPaid;
    private double remainingUnpaidInterest;
    private double currentOutstandingPrincipal;
    private double expectedTotalPayable;

    public PeriodBreakdownResult() {}

    public String getLoanStartDate() { return loanStartDate; }
    public void setLoanStartDate(String loanStartDate) { this.loanStartDate = loanStartDate; }

    public String getCalcStartDate() { return calcStartDate; }
    public void setCalcStartDate(String calcStartDate) { this.calcStartDate = calcStartDate; }

    public String getCalcEndDate() { return calcEndDate; }
    public void setCalcEndDate(String calcEndDate) { this.calcEndDate = calcEndDate; }

    public String getSelectedDuration() { return selectedDuration; }
    public void setSelectedDuration(String selectedDuration) { this.selectedDuration = selectedDuration; }

    public List<PeriodBreakdownItem> getItems() { return items; }
    public void setItems(List<PeriodBreakdownItem> items) { this.items = items; }

    public double getTotalCalculatedInterest() { return totalCalculatedInterest; }
    public void setTotalCalculatedInterest(double totalCalculatedInterest) { this.totalCalculatedInterest = totalCalculatedInterest; }

    public double getInterestAlreadyPaid() { return interestAlreadyPaid; }
    public void setInterestAlreadyPaid(double interestAlreadyPaid) { this.interestAlreadyPaid = interestAlreadyPaid; }

    public double getRemainingUnpaidInterest() { return remainingUnpaidInterest; }
    public void setRemainingUnpaidInterest(double remainingUnpaidInterest) { this.remainingUnpaidInterest = remainingUnpaidInterest; }

    public double getCurrentOutstandingPrincipal() { return currentOutstandingPrincipal; }
    public void setCurrentOutstandingPrincipal(double currentOutstandingPrincipal) { this.currentOutstandingPrincipal = currentOutstandingPrincipal; }

    public double getExpectedTotalPayable() { return expectedTotalPayable; }
    public void setExpectedTotalPayable(double expectedTotalPayable) { this.expectedTotalPayable = expectedTotalPayable; }
}