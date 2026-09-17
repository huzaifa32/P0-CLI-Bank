package com.bankcli.domain;

import java.sql.Timestamp;

public class Transaction {
    private int transactionId;
    private int accountId;
    private String type;
    private Double amount;
    private Integer relatedAccountId; // Foreign Key Account for Transfer transactions. nullable, so use the wrapper Integer, not int
    private Timestamp timestamp;

    // Constructor for a NEW transaction, not yet saved to the DB.
    // No transactionId (SERIAL) and no timestamp (has a DB-side DEFAULT).
    public Transaction(int accountId, String type, Double amount, Integer relatedAccountId) {
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.relatedAccountId = relatedAccountId;
    }

    // Convenience overload for transaction types that don't involve another account
    // (e.g., a simple deposit or withdrawal, no transfer)
    public Transaction(int accountId, String type, Double amount) {
        this(accountId, type, amount, null);
    }

    // Constructor for an EXISTING transaction, read back from the DB.
    // Includes transactionId and timestamp since both are already known.
    public Transaction(int transactionId, int accountId, String type, Double amount,
                        Integer relatedAccountId, Timestamp timestamp) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.relatedAccountId = relatedAccountId;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    public int getAccountId() {
        return accountId;
    }

    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Integer getRelatedAccountId() {
        return relatedAccountId;
    }

    public void setRelatedAccountId(Integer relatedAccountId) {
        this.relatedAccountId = relatedAccountId;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}