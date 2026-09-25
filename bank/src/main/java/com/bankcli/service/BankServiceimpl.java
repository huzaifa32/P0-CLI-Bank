package com.bankcli.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

import com.bankcli.domain.Account;
import com.bankcli.domain.Transaction;
import com.bankcli.persistence.AccountDAO;
import com.bankcli.persistence.ConnectionFactory;
import com.bankcli.persistence.TransactionDAO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BankServiceimpl implements BankService {

    private final AccountDAO accountDAO;
    private final TransactionDAO transactionDAO;
    private final ConnectionFactory connectionFactory;
    private static final Logger logger = LoggerFactory.getLogger(BankServiceimpl.class);

    public BankServiceimpl(AccountDAO accountDAO, TransactionDAO transactionDAO, ConnectionFactory connectionFactory) {
        this.accountDAO = accountDAO;
        this.transactionDAO = transactionDAO;
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Account openAccount(String pin, Double initialBalance) {
        if (pin == null || pin.isBlank()) {
            throw new IllegalArgumentException("PIN cannot be empty");
        }
    
        if (initialBalance == null || initialBalance < 0) {
            throw new IllegalArgumentException("Initial balance cannot be negative");
        }

        Account account = new Account(pin, initialBalance); // "new account" constructor, no ID
        accountDAO.addAccount(account);
        logger.info("account {} opened with initial balance of ${}",account.getAccountId(),account.getBalance());
        return account;
        
    }

    @Override
    public boolean authenticate(int accountId, String pin) {
        Account account = accountDAO.getAccountById(accountId);

        if (account == null) {
            logger.error("failed to authenticate, account with ID {} does not exist.",accountId);
            return false; // account does not exist
        }

        boolean matches = account.getPin().equals(pin);
        if (matches) {
            logger.info("Account {} authenticated successfully", accountId);
        } else {
            logger.error("Authentication failed: incorrect PIN entered for account {}", accountId);
        }
        return matches;
    }

    @Override
    public Account deposit(int accountId, Double amount) {
        try (Connection connection = connectionFactory.getConnection()) {
            connection.setAutoCommit(false);

            try {
                Account account = accountDAO.getAccountById(connection, accountId);
                if (account == null) {
                    logger.error("Deposit failed. Account {} not found",accountId);
                    throw new IllegalArgumentException("Account not found");
                }
                if(amount<0){
                    logger.error("Deposit failed. Amount must be positive");
                    throw new IllegalArgumentException("Amount must be positive");
                }

                account.setBalance(account.getBalance() + amount);
                accountDAO.updateAccount(connection, account);
                transactionDAO.addTransaction(connection, new Transaction(accountId, "DEPOSIT", amount));

                connection.commit();
                logger.info("Deposited {} to account ID {}, new balance is ${}",amount,accountId,account.getBalance());
                return account;

            } catch (RuntimeException e) {
                
                connection.rollback();
                throw e;
            }

        } catch (SQLException e) {
            logger.error("Deposit to account ID {} failed due to SQL Error",accountId,e);
            throw new IllegalStateException("Deposit failed", e);
        }
    }

    @Override
    public Account withdraw(int accountId, Double amount) {
        try (Connection connection = connectionFactory.getConnection()) {
            connection.setAutoCommit(false);

            try {
                Account account = accountDAO.getAccountById(connection, accountId);
                if (account == null) {
                    logger.error("Withdraw failed. Account {} not found",accountId);
                    throw new IllegalArgumentException("Account not found");
                }
                if (account.getBalance() - amount < 0) {
                    logger.error("Withdraw failed. Account {} has insufficient funds. Balance: ${}",accountId,account.getBalance());
                    throw new IllegalStateException("Insufficient funds");
                }
                if(amount<0){
                    logger.error("Withdraw failed. Amount must be positive");
                    throw new IllegalArgumentException("Amount must be positive");
                }

                account.setBalance(account.getBalance() - amount);
                accountDAO.updateAccount(connection, account);
                transactionDAO.addTransaction(connection, new Transaction(accountId, "WITHDRAWAL", amount));

                logger.info("Withdrawn {} from account ID {}, new balance is ${}",amount,accountId,account.getBalance());
                connection.commit();
                return account;

            } catch (RuntimeException e) {
                connection.rollback();
                throw e;
            }

        } catch (SQLException e) {
            logger.error("Withdrawal from account ID {} failed due to SQL Error",accountId,e);
            throw new IllegalStateException("Withdrawal failed", e);
        }
    }

    @Override
    public void transfer(int fromAccountId, int toAccountId, Double amount) {
        try (Connection connection = connectionFactory.getConnection()) {
            connection.setAutoCommit(false);

            try {
                Account fromAccount = accountDAO.getAccountById(connection, fromAccountId);
                Account toAccount = accountDAO.getAccountById(connection, toAccountId);

                if (fromAccount == null || toAccount == null) {
                    logger.error("One or both accounts not found: {}, {}",fromAccountId,toAccountId);
                    throw new IllegalArgumentException("One or both accounts not found");
                }
                if (fromAccount.getBalance() - amount < 0) {
                    logger.error("Transfer failed. Account {} has insufficient funds. Balance: ${}",fromAccountId,fromAccount.getBalance());
                    throw new IllegalStateException("Insufficient funds");
                }
                if(amount<0){
                    logger.error("Transfer failed. Amount must be positive");
                    throw new IllegalArgumentException("Amount must be positive");
                }

                fromAccount.setBalance(fromAccount.getBalance() - amount);
                toAccount.setBalance(toAccount.getBalance() + amount);

                accountDAO.updateAccount(connection, fromAccount);
                accountDAO.updateAccount(connection, toAccount);

                transactionDAO.addTransaction(connection, new Transaction(fromAccountId, "TRANSFER_OUT", amount, toAccountId));
                transactionDAO.addTransaction(connection, new Transaction(toAccountId, "TRANSFER_IN", amount, fromAccountId));

                logger.info("Transferred {} from account ID {} to account ID {}",amount,fromAccountId,toAccountId);
                connection.commit();

            } catch (RuntimeException e) {
                connection.rollback();
                throw e;
            }

        } catch (SQLException e) {
            logger.error("Transfer from account ID {} to account id {} failed due to SQL Error",fromAccountId,toAccountId,e);
            throw new IllegalStateException("Transfer failed", e);
        }
    }

    @Override
    public Account getAccount(int accountId) {
        return accountDAO.getAccountById(accountId);
    }

    @Override
    public Double getBalance(int accountId) {
        return accountDAO.getAccountById(accountId).getBalance();
    }

    @Override 
    public List<Transaction> getTransactionHistoryById(int accountId){
        return transactionDAO.getTransactionHistoryById(accountId);
    }

}