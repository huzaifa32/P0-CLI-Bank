package com.bankcli.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bankcli.domain.Account;
import com.bankcli.domain.Transaction;
import com.bankcli.persistence.AccountDAO;
import com.bankcli.persistence.ConnectionFactory;
import com.bankcli.persistence.TransactionDAO;

@ExtendWith(MockitoExtension.class)
class BankServiceimplTest {

    @Mock
    private AccountDAO accountDAO;
    @Mock
    private TransactionDAO transactionDAO;
    @Mock
    private ConnectionFactory connectionFactory;
    @Mock
    private Connection connection;

    private BankServiceimpl bankService;

    @BeforeEach
    void setUp() {
        bankService = new BankServiceimpl(accountDAO, transactionDAO, connectionFactory);
    }

    // ==================== openAccount ====================

    @Test
    void openAccount_validPinAndBalance_createsAccount() {
        Account result = bankService.openAccount("1234", 100.00);

        assertEquals("1234", result.getPin());
        assertEquals(100.00, result.getBalance());
        verify(accountDAO).addAccount(result);
    }

    @Test
    void openAccount_blankPin_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> bankService.openAccount("  ", 100.00));
        verify(accountDAO, never()).addAccount(any());
    }

    @Test
    void openAccount_negativeBalance_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> bankService.openAccount("1234", -50.00));
        verify(accountDAO, never()).addAccount(any());
    }

    // ==================== authenticate ====================

    @Test
    void authenticate_correctPin_returnsTrue() {
        Account account = new Account(1, "1234", 500.00);
        when(accountDAO.getAccountById(1)).thenReturn(account);

        assertTrue(bankService.authenticate(1, "1234"));
    }

    @Test
    void authenticate_incorrectPin_returnsFalse() {
        Account account = new Account(1, "1234", 500.00);
        when(accountDAO.getAccountById(1)).thenReturn(account);

        assertFalse(bankService.authenticate(1, "9999"));
    }

    @Test
    void authenticate_accountNotFound_returnsFalse() {
        when(accountDAO.getAccountById(999)).thenReturn(null);

        assertFalse(bankService.authenticate(999, "1234"));
    }

    // ==================== deposit ====================

    @Test
    void deposit_validAmount_increasesBalanceAndCommits() throws SQLException {
        when(connectionFactory.getConnection()).thenReturn(connection);
        Account account = new Account(1, "1234", 500.00);
        when(accountDAO.getAccountById(connection, 1)).thenReturn(account);

        Account result = bankService.deposit(1, 100.00);

        assertEquals(600.00, result.getBalance());
        verify(accountDAO).updateAccount(connection, account);
        verify(connection).commit();
        verify(connection, never()).rollback();

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionDAO).addTransaction(org.mockito.ArgumentMatchers.eq(connection), captor.capture());
        assertEquals("DEPOSIT", captor.getValue().getType());
        assertEquals(100.00, captor.getValue().getAmount());
    }

    @Test
    void deposit_accountNotFound_rollsBackAndThrows() throws SQLException {
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(accountDAO.getAccountById(connection, 999)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> bankService.deposit(999, 100.00));

        verify(connection).rollback();
        verify(connection, never()).commit();
        verify(accountDAO, never()).updateAccount(any(Connection.class), any(Account.class));
    }

    // ==================== withdraw ====================

    @Test
    void withdraw_sufficientFunds_decreasesBalanceAndCommits() throws SQLException {
        when(connectionFactory.getConnection()).thenReturn(connection);
        Account account = new Account(1, "1234", 500.00);
        when(accountDAO.getAccountById(connection, 1)).thenReturn(account);

        Account result = bankService.withdraw(1, 200.00);

        assertEquals(300.00, result.getBalance());
        verify(accountDAO).updateAccount(connection, account);
        verify(connection).commit();
        verify(connection, never()).rollback();
    }

    @Test
    void withdraw_insufficientFunds_rollsBackAndThrows() throws SQLException {
        when(connectionFactory.getConnection()).thenReturn(connection);
        Account account = new Account(1, "1234", 50.00);
        when(accountDAO.getAccountById(connection, 1)).thenReturn(account);

        assertThrows(IllegalStateException.class, () -> bankService.withdraw(1, 200.00));

        verify(connection).rollback();
        verify(connection, never()).commit();
        verify(accountDAO, never()).updateAccount(any(Connection.class), any(Account.class));
    }

    // ==================== transfer ====================

    @Test
    void transfer_sufficientFunds_updatesBothAccountsAndCommits() throws SQLException {
        when(connectionFactory.getConnection()).thenReturn(connection);
        Account fromAccount = new Account(1, "1234", 500.00);
        Account toAccount = new Account(2, "5678", 100.00);
        when(accountDAO.getAccountById(connection, 1)).thenReturn(fromAccount);
        when(accountDAO.getAccountById(connection, 2)).thenReturn(toAccount);

        bankService.transfer(1, 2, 150.00);

        assertEquals(350.00, fromAccount.getBalance());
        assertEquals(250.00, toAccount.getBalance());
        verify(accountDAO).updateAccount(connection, fromAccount);
        verify(accountDAO).updateAccount(connection, toAccount);
        verify(transactionDAO, times(2)).addTransaction(org.mockito.ArgumentMatchers.eq(connection), any(Transaction.class));
        verify(connection).commit();
        verify(connection, never()).rollback();
    }

    @Test
    void transfer_insufficientFunds_rollsBackAndThrows() throws SQLException {
        when(connectionFactory.getConnection()).thenReturn(connection);
        Account fromAccount = new Account(1, "1234", 50.00);
        Account toAccount = new Account(2, "5678", 100.00);
        when(accountDAO.getAccountById(connection, 1)).thenReturn(fromAccount);
        when(accountDAO.getAccountById(connection, 2)).thenReturn(toAccount);

        assertThrows(IllegalStateException.class, () -> bankService.transfer(1, 2, 200.00));

        verify(connection).rollback();
        verify(connection, never()).commit();
        verify(accountDAO, never()).updateAccount(any(Connection.class), any(Account.class));
    }

    @Test
    void transfer_toAccountNotFound_rollsBackAndThrows() throws SQLException {
        when(connectionFactory.getConnection()).thenReturn(connection);
        Account fromAccount = new Account(1, "1234", 500.00);
        when(accountDAO.getAccountById(connection, 1)).thenReturn(fromAccount);
        when(accountDAO.getAccountById(connection, 2)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> bankService.transfer(1, 2, 100.00));

        verify(connection).rollback();
        verify(connection, never()).commit();
    }

    // ==================== getAccount ====================

    @Test
    void getAccount_existingId_returnsAccount() {
        Account account = new Account(1, "1234", 500.00);
        when(accountDAO.getAccountById(1)).thenReturn(account);

        assertEquals(account, bankService.getAccount(1));
    }

    @Test
    void getAccount_nonExistentId_returnsNull() {
        when(accountDAO.getAccountById(anyInt())).thenReturn(null);

        assertNull(bankService.getAccount(999));
    }

    // ==================== getBalance ====================

    @Test
    void getBalance_existingAccount_returnsBalance() {
        Account account = new Account(1, "1234", 750.00);
        when(accountDAO.getAccountById(1)).thenReturn(account);

        assertEquals(750.00, bankService.getBalance(1));
    }

    @Test
    void getBalance_nonExistentAccount_throwsNullPointerException() {
        when(accountDAO.getAccountById(999)).thenReturn(null);

        // Documents current behavior: getBalance() does not null-check before calling
        // .getBalance() on the result. Consider having the service throw a clearer
        // IllegalArgumentException("Account not found") instead, matching deposit/withdraw/transfer.
        assertThrows(NullPointerException.class, () -> bankService.getBalance(999));
    }
}