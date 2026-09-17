package com.bankcli.domain;


/*

CREATE TABLE account (
    account_id SERIAL PRIMARY KEY,
    pin VARCHAR(255) NOT NULL,
    balance NUMERIC(12, 2) NOT NULL DEFAULT 0.00
);

 */
public class Account {
    private int accountId; //this is a SERIAL value
    private String pin;
    private double balance;


    //for NEW accounts, accountId will be assigned a new SERIAL value.
    public Account(String pin, double balance) {
        this.pin = pin;
        this.balance = balance;
  
    }

    //for existing accounts with known account id.
    public Account(int accountId,String pin, double balance){
        this.accountId=accountId;
        this.pin=pin;
        this.balance=balance;
    }

    @Override
    public String toString() {
        return String.format("Account ID: %d ", accountId);
    }

    //getters and setters
    public int getAccountId(){
        return accountId;
    }
    
    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }
}
