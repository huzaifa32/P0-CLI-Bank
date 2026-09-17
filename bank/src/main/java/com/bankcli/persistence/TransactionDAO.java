package com.bankcli.persistence;
import com.bankcli.domain.Transaction;

import java.sql.Connection;
import java.util.List;

public interface TransactionDAO {
        
    void addTransaction(Transaction transaction);

    Transaction getTransactionById(int id);

    List<Transaction> getAllTransactions();

    void deleteTransaction(int id);

    //overload for atomicity
    void addTransaction(Connection connection, Transaction transaction);



    
}
