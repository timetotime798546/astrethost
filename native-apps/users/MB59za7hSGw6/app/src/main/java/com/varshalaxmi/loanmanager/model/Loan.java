package com.varshalaxmi.loanmanager.model;

public class Loan {
    private long id;
    private long customerId;
    private String customerName;
    private double principalAmount;
    private double interestRate;
    private String interestMethod; // Simple, Flat, Monthly, Daily, Reducing, None
    private String issueDate;
    private String notes;
    private String status; // ACTIVE, CLOSED
    
    private double accruedInterest;
    private double totalPaid;
    private double remainingBalance;

    public Loan() {}

    public Loan(long id, long customerId, String customerName, double principalAmount, double interestRate, String interestMethod, String issueDate, String notes, String status) {
        this.id = id;
        this.customerId = customerId;
        this.customerName = customerName;
        this.principalAmount = principalAmount;
        this.interestRate = interestRate;
        this.interestMethod = interestMethod;
        this.issueDate = issueDate;
        this.notes = notes;
        this.status = status;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getCustomerId() { return customerId; }
    public void setCustomerId(long customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public double getPrincipalAmount() { return principalAmount; }
    public void setPrincipalAmount(double principalAmount) { this.principalAmount = principalAmount; }

    public double getInterestRate() { return interestRate; }
    public void setInterestRate(double interestRate) { this.interestRate = interestRate; }

    public String getInterestMethod() { return interestMethod; }
    public void setInterestMethod(String interestMethod) { this.interestMethod = interestMethod; }

    public String getIssueDate() { return issueDate; }
    public void setIssueDate(String issueDate) { this.issueDate = issueDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getAccruedInterest() { return accruedInterest; }
    public void setAccruedInterest(double accruedInterest) { this.accruedInterest = accruedInterest; }

    public double getTotalPaid() { return totalPaid; }
    public void setTotalPaid(double totalPaid) { this.totalPaid = totalPaid; }

    public double getRemainingBalance() { return remainingBalance; }
    public void setRemainingBalance(double remainingBalance) { this.remainingBalance = remainingBalance; }
}