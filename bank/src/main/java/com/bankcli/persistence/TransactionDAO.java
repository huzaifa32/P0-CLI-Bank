package com.bankcli.persistence;

import java.sql.Connection;
import java.util.List;

import com.bankcli.domain.Transaction;

public interface TransactionDAO {

    void addTransaction(Transaction transaction);
    void addTransaction(Connection connection, Transaction transaction);

    Transaction getTransactionById(int id);
    Transaction getTransactionById(Connection connection, int id);

    List<Transaction> getAllTransactions();
    List<Transaction> getAllTransactions(Connection connection);

    void deleteTransaction(int id);
    void deleteTransaction(Connection connection, int id);

    List<Transaction> getTransactionHistoryById(Connection connection, int id);
    List<Transaction> getTransactionHistoryById(int id);
}