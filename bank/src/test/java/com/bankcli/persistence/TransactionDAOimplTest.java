package com.bankcli.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bankcli.domain.Transaction;

@ExtendWith(MockitoExtension.class)
class TransactionDAOimplTest {

    @Mock
    private ConnectionFactory connectionFactory;
    @Mock
    private Connection connection;
    @Mock
    private PreparedStatement statement;
    @Mock
    private ResultSet resultSet;

    private TransactionDAOimpl transactionDAO;

    @BeforeEach
    void setUp() throws SQLException {
        // Applies to every test, including the constructor's initializeSchema() call.
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);

        transactionDAO = new TransactionDAOimpl(connectionFactory);

        // The constructor's initializeSchema() call already invoked statement.executeUpdate()
        // once (for CREATE TABLE). Reset counts here so each test verifies only its own calls.
        clearInvocations(connectionFactory, connection, statement);
    }

    // ==================== addTransaction(Transaction) ====================

    @Test
    void addTransaction_validTransfer_setsGeneratedIdAndTimestamp() throws SQLException {
        Timestamp now = new Timestamp(System.currentTimeMillis());

        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("transaction_id")).thenReturn(10);
        when(resultSet.getTimestamp("timestamp")).thenReturn(now);

        Transaction transaction = new Transaction(1, "TRANSFER", 150.00, 2);
        transactionDAO.addTransaction(transaction);

        assertEquals(10, transaction.getTransactionId());
        assertEquals(now, transaction.getTimestamp());
        verify(statement).setInt(1, 1);
        verify(statement).setString(2, "TRANSFER");
        verify(statement).setDouble(3, 150.00);
        verify(statement).setInt(4, 2);
    }

    @Test
    void addTransaction_noRelatedAccount_setsNullForRelatedAccountId() throws SQLException {
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("transaction_id")).thenReturn(11);
        when(resultSet.getTimestamp("timestamp")).thenReturn(new Timestamp(System.currentTimeMillis()));

        // DEPOSIT has no related account
        Transaction transaction = new Transaction(1, "DEPOSIT", 75.00, null);
        transactionDAO.addTransaction(transaction);

        verify(statement).setNull(4, Types.INTEGER);
    }

    @Test
    void addTransaction_sqlException_throwsIllegalStateException() throws SQLException {
        when(statement.executeQuery()).thenThrow(new SQLException("insert failed"));

        Transaction transaction = new Transaction(1, "WITHDRAW", 20.00, null);

        assertThrows(IllegalStateException.class, () -> transactionDAO.addTransaction(transaction));
    }

    // ==================== addTransaction(Connection, Transaction) ====================

    @Test
    void addTransaction_withConnection_validTransfer_setsGeneratedIdAndTimestamp() throws SQLException {
        Timestamp now = new Timestamp(System.currentTimeMillis());

        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("transaction_id")).thenReturn(20);
        when(resultSet.getTimestamp("timestamp")).thenReturn(now);

        Transaction transaction = new Transaction(1, "TRANSFER", 300.00, 2);
        transactionDAO.addTransaction(connection, transaction);

        assertEquals(20, transaction.getTransactionId());
        verify(statement).setInt(1, 1);
        verify(statement).setInt(4, 2);
    }

    @Test
    void addTransaction_withConnection_sqlException_throwsIllegalStateException() throws SQLException {
        when(statement.executeQuery()).thenThrow(new SQLException("insert failed"));

        Transaction transaction = new Transaction(1, "WITHDRAW", 20.00, null);

        assertThrows(IllegalStateException.class,
                () -> transactionDAO.addTransaction(connection, transaction));
    }

    // ==================== getTransactionById(int) ====================

    @Test
    void getTransactionById_existingId_returnsTransaction() throws SQLException {
        Timestamp now = new Timestamp(System.currentTimeMillis());

        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("transaction_id")).thenReturn(5);
        when(resultSet.getInt("account_id")).thenReturn(1);
        when(resultSet.getString("type")).thenReturn("DEPOSIT");
        when(resultSet.getDouble("amount")).thenReturn(100.00);
        when(resultSet.getInt("related_account_id")).thenReturn(0);
        when(resultSet.wasNull()).thenReturn(true); // no related account
        when(resultSet.getTimestamp("timestamp")).thenReturn(now);

        Transaction result = transactionDAO.getTransactionById(5);

        assertNotNull(result);
        assertEquals(5, result.getTransactionId());
        assertEquals("DEPOSIT", result.getType());
        assertNull(result.getRelatedAccountId());
        verify(statement).setInt(1, 5);
    }

    @Test
    void getTransactionById_nonExistentId_returnsNull() throws SQLException {
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Transaction result = transactionDAO.getTransactionById(999);

        assertNull(result);
    }

    @Test
    void getTransactionById_sqlException_throwsIllegalStateException() throws SQLException {
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("connection refused"));

        assertThrows(IllegalStateException.class, () -> transactionDAO.getTransactionById(5));
    }

    // ==================== getTransactionById(Connection, int) ====================

    @Test
    void getTransactionById_withConnection_relatedAccountPresent_returnsTransaction() throws SQLException {
        Timestamp now = new Timestamp(System.currentTimeMillis());

        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("transaction_id")).thenReturn(6);
        when(resultSet.getInt("account_id")).thenReturn(1);
        when(resultSet.getString("type")).thenReturn("TRANSFER");
        when(resultSet.getDouble("amount")).thenReturn(250.00);
        when(resultSet.getInt("related_account_id")).thenReturn(2);
        when(resultSet.wasNull()).thenReturn(false); // related account present
        when(resultSet.getTimestamp("timestamp")).thenReturn(now);

        Transaction result = transactionDAO.getTransactionById(connection, 6);

        assertNotNull(result);
        assertEquals(2, result.getRelatedAccountId());
    }

    @Test
    void getTransactionById_withConnection_nonExistentId_returnsNull() throws SQLException {
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Transaction result = transactionDAO.getTransactionById(connection, 999);

        assertNull(result);
    }

    // ==================== getAllTransactions() ====================

    @Test
    void getAllTransactions_multipleRows_returnsAllTransactions() throws SQLException {
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getInt("transaction_id")).thenReturn(1, 2);
        when(resultSet.getInt("account_id")).thenReturn(1, 1);
        when(resultSet.getString("type")).thenReturn("DEPOSIT", "WITHDRAW");
        when(resultSet.getDouble("amount")).thenReturn(100.00, 50.00);
        when(resultSet.getInt("related_account_id")).thenReturn(0, 0);
        when(resultSet.wasNull()).thenReturn(true, true);
        when(resultSet.getTimestamp("timestamp")).thenReturn(
                new Timestamp(System.currentTimeMillis()), new Timestamp(System.currentTimeMillis()));

        List<Transaction> result = transactionDAO.getAllTransactions();

        assertEquals(2, result.size());
    }

    @Test
    void getAllTransactions_noRows_returnsEmptyList() throws SQLException {
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        List<Transaction> result = transactionDAO.getAllTransactions();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getAllTransactions_sqlException_throwsIllegalStateException() throws SQLException {
        when(statement.executeQuery()).thenThrow(new SQLException("select failed"));

        assertThrows(IllegalStateException.class, () -> transactionDAO.getAllTransactions());
    }

    // ==================== getAllTransactions(Connection) ====================

    @Test
    void getAllTransactions_withConnection_multipleRows_returnsAllTransactions() throws SQLException {
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getInt("transaction_id")).thenReturn(1);
        when(resultSet.getInt("account_id")).thenReturn(1);
        when(resultSet.getString("type")).thenReturn("DEPOSIT");
        when(resultSet.getDouble("amount")).thenReturn(100.00);
        when(resultSet.getInt("related_account_id")).thenReturn(0);
        when(resultSet.wasNull()).thenReturn(true);
        when(resultSet.getTimestamp("timestamp")).thenReturn(new Timestamp(System.currentTimeMillis()));

        List<Transaction> result = transactionDAO.getAllTransactions(connection);

        assertEquals(1, result.size());
    }

    // ==================== deleteTransaction(int) ====================

    @Test
    void deleteTransaction_validId_executesDelete() throws SQLException {
        transactionDAO.deleteTransaction(1);

        verify(statement).setInt(1, 1);
        verify(statement).executeUpdate();
    }

    @Test
    void deleteTransaction_sqlException_throwsIllegalStateException() throws SQLException {
        when(statement.executeUpdate()).thenThrow(new SQLException("delete failed"));

        assertThrows(IllegalStateException.class, () -> transactionDAO.deleteTransaction(1));
    }

    // ==================== deleteTransaction(Connection, int) ====================

    @Test
    void deleteTransaction_withConnection_validId_executesDelete() throws SQLException {
        transactionDAO.deleteTransaction(connection, 1);

        verify(statement).setInt(1, 1);
        verify(statement).executeUpdate();
    }

    @Test
    void deleteTransaction_withConnection_sqlException_throwsIllegalStateException() throws SQLException {
        when(statement.executeUpdate()).thenThrow(new SQLException("delete failed"));

        assertThrows(IllegalStateException.class, () -> transactionDAO.deleteTransaction(connection, 1));
    }
}