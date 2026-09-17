package com.bankcli.persistence;
import java.sql.Connection;
import java.util.List;

import com.bankcli.domain.Account;

public interface AccountDAO {


    void addAccount(Account account);

    Account getAccountById(int id);

    List<Account> getAllAccounts();

    void updateAccount(Account account);

    void deleteAccount(int id);

    // New overloads — accept an existing connection so callers can control the transaction
    Account getAccountById(Connection connection, int id);
    void updateAccount(Connection connection, Account account);


    
}
