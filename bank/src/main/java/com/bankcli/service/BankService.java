package com.bankcli.service;



import com.bankcli.domain.Account;

public interface BankService {
    Account openAccount(String pin, Double initialBalance);
    boolean authenticate(int accountId, String pin);

    Account deposit(int accountId, Double amount);
    Account withdraw(int accountId, Double amount);
    void transfer(int fromAccountId, int toAccountId, Double amount);
    
    Account getAccount(int accountId);
    Double getBalance(int accountId);

}
