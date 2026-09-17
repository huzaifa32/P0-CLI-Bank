package com.bankcli.service;

import java.sql.Connection;
import java.sql.SQLException;

import com.bankcli.domain.Account;
import com.bankcli.domain.Transaction;
import com.bankcli.persistence.AccountDAO;
import com.bankcli.persistence.ConnectionFactory;
import com.bankcli.persistence.TransactionDAO;

public class BankServiceimpl implements BankService {

    private final AccountDAO accountDAO;
    private final TransactionDAO transactionDAO;

    public BankServiceimpl(AccountDAO accountDAO, TransactionDAO transactionDAO) {
        this.accountDAO = accountDAO;
        this.transactionDAO = transactionDAO;
    }

    @Override
    public Account openAccount(String pin, Double initialBalance) {
        if (pin == null || pin.isBlank()) {
            throw new IllegalArgumentException("PIN cannot be empty");
        }
        if (initialBalance < 0) {
            throw new IllegalArgumentException("Initial balance cannot be negative");
        }

        Account account = new Account(pin, initialBalance); // "new account" constructor, no ID
        accountDAO.addAccount(account);
        return account;
    }

    @Override
    public boolean authenticate(int accountId, String pin) {
        Account account = accountDAO.getAccountById(accountId);

        if (account == null) {
            return false; // account does not exist
        }

        return account.getPin().equals(pin);
    }

    @Override
    public Account deposit(int accountId, Double amount) {
        try (Connection connection = ConnectionFactory.getConnectionFactory().getConnection()) {
            connection.setAutoCommit(false);

            try {
                Account account = accountDAO.getAccountById(connection, accountId);
                if (account == null) {
                    throw new IllegalArgumentException("Account not found");
                }

                account.setBalance(account.getBalance() + amount);
                accountDAO.updateAccount(connection, account);
                transactionDAO.addTransaction(connection, new Transaction(accountId, "DEPOSIT", amount));

                connection.commit();
                return account;

            } catch (RuntimeException e) {
                connection.rollback();
                throw e;
            }

        } catch (SQLException e) {
            throw new IllegalStateException("Deposit failed", e);
        }
    }

    @Override
    public Account withdraw(int accountId, Double amount) {
        try (Connection connection = ConnectionFactory.getConnectionFactory().getConnection()) {
            connection.setAutoCommit(false);

            try {
                Account account = accountDAO.getAccountById(connection, accountId);
                if (account == null) {
                    throw new IllegalArgumentException("Account not found");
                }
                if (account.getBalance() - amount < 0) {
                    throw new IllegalStateException("Insufficient funds");
                }

                account.setBalance(account.getBalance() - amount);
                accountDAO.updateAccount(connection, account);
                transactionDAO.addTransaction(connection, new Transaction(accountId, "WITHDRAWAL", amount));

                connection.commit();
                return account;

            } catch (RuntimeException e) {
                connection.rollback();
                throw e;
            }

        } catch (SQLException e) {
            throw new IllegalStateException("Withdrawal failed", e);
        }
    }

    @Override
    public void transfer(int fromAccountId, int toAccountId, Double amount) {
        try (Connection connection = ConnectionFactory.getConnectionFactory().getConnection()) {
            connection.setAutoCommit(false);

            try {
                Account fromAccount = accountDAO.getAccountById(connection, fromAccountId);
                Account toAccount = accountDAO.getAccountById(connection, toAccountId);

                if (fromAccount == null || toAccount == null) {
                    throw new IllegalArgumentException("One or both accounts not found");
                }
                if (fromAccount.getBalance() - amount < 0) {
                    throw new IllegalStateException("Insufficient funds");
                }

                fromAccount.setBalance(fromAccount.getBalance() - amount);
                toAccount.setBalance(toAccount.getBalance() + amount);

                accountDAO.updateAccount(connection, fromAccount);
                accountDAO.updateAccount(connection, toAccount);

                transactionDAO.addTransaction(connection, new Transaction(fromAccountId, "TRANSFER_OUT", amount, toAccountId));
                transactionDAO.addTransaction(connection, new Transaction(toAccountId, "TRANSFER_IN", amount, fromAccountId));

                connection.commit();

            } catch (RuntimeException e) {
                connection.rollback();
                throw e;
            }

        } catch (SQLException e) {
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


    
}
