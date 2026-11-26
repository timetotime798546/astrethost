package com.varshalaxmi.loanmanager.model;

public class LoanAddition {
    private long id;
    private long loanId;
    private double amount;
    private String additionDate;
    private String note;

    public LoanAddition() {}

    public LoanAddition(long id, long loanId, double amount, String additionDate, String note) {
        this.id = id;
        this.loanId = loanId;
        this.amount = amount;
        this.additionDate = additionDate;
        this.note = note;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getLoanId() { return loanId; }
    public void setLoanId(long loanId) { this.loanId = loanId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getAdditionDate() { return additionDate; }
    public void setAdditionDate(String additionDate) { this.additionDate = additionDate; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
