package com.bankcli.persistence;

import java.sql.Connection;
import java.util.List;

import com.bankcli.domain.Account;
import com.bankcli.domain.Transaction;

public interface AccountDAO {

    void addAccount(Account account);
    void addAccount(Connection connection, Account account);

    Account getAccountById(int id);
    Account getAccountById(Connection connection, int id);

    List<Account> getAllAccounts();
    List<Account> getAllAccounts(Connection connection);

    void updateAccount(Account account);
    void updateAccount(Connection connection, Account updatedAccount);

    void deleteAccount(int id);
    void deleteAccount(Connection connection, int id);




}