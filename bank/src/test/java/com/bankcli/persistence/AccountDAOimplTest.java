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
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bankcli.domain.Account;

@ExtendWith(MockitoExtension.class)
class AccountDAOimplTest {

    @Mock
    private ConnectionFactory connectionFactory;
    @Mock
    private Connection connection;
    @Mock
    private PreparedStatement statement;
    @Mock
    private ResultSet resultSet;

    private AccountDAOimpl accountDAO;

    @BeforeEach
    void setUp() throws SQLException {
        // Applies to every test, including the constructor's initializeSchema() call.
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);

        accountDAO = new AccountDAOimpl(connectionFactory);

        // The constructor's initializeSchema() call already invoked statement.executeUpdate()
        // once (for CREATE TABLE). Reset counts here so each test verifies only its own calls.
        clearInvocations(connectionFactory, connection, statement);
    }

    // ==================== addAccount(Account) ====================

    @Test
    void addAccount_validAccount_setsGeneratedId() throws SQLException {
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("account_id")).thenReturn(42);

        Account newAccount = new Account("5678", 100.00);
        accountDAO.addAccount(newAccount);

        assertEquals(42, newAccount.getAccountId());
        verify(statement).setString(1, "5678");
        verify(statement).setDouble(2, 100.00);
    }

    @Test
    void addAccount_sqlException_throwsIllegalStateException() throws SQLException {
        when(statement.executeQuery()).thenThrow(new SQLException("insert failed"));

        Account newAccount = new Account("5678", 100.00);

        assertThrows(IllegalStateException.class, () -> accountDAO.addAccount(newAccount));
    }

    // ==================== addAccount(Connection, Account) ====================

    @Test
    void addAccount_withConnection_validAccount_setsGeneratedId() throws SQLException {
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("account_id")).thenReturn(7);

        Account newAccount = new Account("1111", 50.00);
        accountDAO.addAccount(connection, newAccount);

        assertEquals(7, newAccount.getAccountId());
        verify(statement).setString(1, "1111");
        verify(statement).setDouble(2, 50.00);
    }

    @Test
    void addAccount_withConnection_sqlException_throwsIllegalStateException() throws SQLException {
        when(statement.executeQuery()).thenThrow(new SQLException("insert failed"));

        Account newAccount = new Account("1111", 50.00);

        assertThrows(IllegalStateException.class, () -> accountDAO.addAccount(connection, newAccount));
    }

    // ==================== getAccountById(int) ====================

    @Test
    void getAccountById_existingId_returnsAccount() throws SQLException {
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("account_id")).thenReturn(1);
        when(resultSet.getString("pin")).thenReturn("1234");
        when(resultSet.getDouble("balance")).thenReturn(500.00);

        Account result = accountDAO.getAccountById(1);

        assertNotNull(result);
        assertEquals(1, result.getAccountId());
        assertEquals("1234", result.getPin());
        assertEquals(500.00, result.getBalance());
        verify(statement).setInt(1, 1);
    }

    @Test
    void getAccountById_nonExistentId_returnsNull() throws SQLException {
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Account result = accountDAO.getAccountById(999);

        assertNull(result);
    }

    @Test
    void getAccountById_sqlException_throwsIllegalStateException() throws SQLException {
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("Connection refused"));

        assertThrows(IllegalStateException.class, () -> accountDAO.getAccountById(1));
    }

    // ==================== getAccountById(Connection, int) ====================

    @Test
    void getAccountById_withConnection_existingId_returnsAccount() throws SQLException {
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("account_id")).thenReturn(1);
        when(resultSet.getString("pin")).thenReturn("1234");
        when(resultSet.getDouble("balance")).thenReturn(500.00);

        Account result = accountDAO.getAccountById(connection, 1);

        assertNotNull(result);
        assertEquals(1, result.getAccountId());
        verify(statement).setInt(1, 1);
    }

    @Test
    void getAccountById_withConnection_nonExistentId_returnsNull() throws SQLException {
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Account result = accountDAO.getAccountById(connection, 999);

        assertNull(result);
    }

    // ==================== getAllAccounts() ====================

    @Test
    void getAllAccounts_multipleRows_returnsAllAccounts() throws SQLException {
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, false); // two rows, then done
        when(resultSet.getInt("account_id")).thenReturn(1, 2);
        when(resultSet.getString("pin")).thenReturn("1111", "2222");
        when(resultSet.getDouble("balance")).thenReturn(100.00, 200.00);

        List<Account> result = accountDAO.getAllAccounts();

        assertEquals(2, result.size());
    }

    @Test
    void getAllAccounts_noRows_returnsEmptyList() throws SQLException {
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        List<Account> result = accountDAO.getAllAccounts();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getAllAccounts_sqlException_throwsIllegalStateException() throws SQLException {
        when(statement.executeQuery()).thenThrow(new SQLException("select failed"));

        assertThrows(IllegalStateException.class, () -> accountDAO.getAllAccounts());
    }

    // ==================== getAllAccounts(Connection) ====================

    @Test
    void getAllAccounts_withConnection_multipleRows_returnsAllAccounts() throws SQLException {
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getInt("account_id")).thenReturn(1);
        when(resultSet.getString("pin")).thenReturn("1111");
        when(resultSet.getDouble("balance")).thenReturn(100.00);

        List<Account> result = accountDAO.getAllAccounts(connection);

        assertEquals(1, result.size());
    }

    // ==================== updateAccount(Account) ====================

    @Test
    void updateAccount_validAccount_executesUpdate() throws SQLException {
        Account account = new Account(1, "9999", 250.00);

        accountDAO.updateAccount(account);

        verify(statement).setString(1, "9999");
        verify(statement).setDouble(2, 250.00);
        verify(statement).setInt(3, 1);
        verify(statement).executeUpdate();
    }

    @Test
    void updateAccount_sqlException_throwsIllegalStateException() throws SQLException {
        when(statement.executeUpdate()).thenThrow(new SQLException("update failed"));

        Account account = new Account(1, "9999", 250.00);

        assertThrows(IllegalStateException.class, () -> accountDAO.updateAccount(account));
    }

    // ==================== updateAccount(Connection, Account) ====================

    @Test
    void updateAccount_withConnection_validAccount_executesUpdate() throws SQLException {
        Account account = new Account(1, "9999", 250.00);

        accountDAO.updateAccount(connection, account);

        verify(statement).setString(1, "9999");
        verify(statement).setDouble(2, 250.00);
        verify(statement).setInt(3, 1);
        verify(statement).executeUpdate();
    }

    @Test
    void updateAccount_withConnection_sqlException_throwsIllegalStateException() throws SQLException {
        when(statement.executeUpdate()).thenThrow(new SQLException("update failed"));

        Account account = new Account(1, "9999", 250.00);

        assertThrows(IllegalStateException.class, () -> accountDAO.updateAccount(connection, account));
    }

    // ==================== deleteAccount(int) ====================

    @Test
    void deleteAccount_validId_executesDelete() throws SQLException {
        accountDAO.deleteAccount(1);

        verify(statement).setInt(1, 1);
        verify(statement).executeUpdate();
    }

    @Test
    void deleteAccount_sqlException_throwsIllegalStateException() throws SQLException {
        when(statement.executeUpdate()).thenThrow(new SQLException("delete failed"));

        assertThrows(IllegalStateException.class, () -> accountDAO.deleteAccount(1));
    }

    // ==================== deleteAccount(Connection, int) ====================

    @Test
    void deleteAccount_withConnection_validId_executesDelete() throws SQLException {
        accountDAO.deleteAccount(connection, 1);

        verify(statement).setInt(1, 1);
        verify(statement).executeUpdate();
    }

    @Test
    void deleteAccount_withConnection_sqlException_throwsIllegalStateException() throws SQLException {
        when(statement.executeUpdate()).thenThrow(new SQLException("delete failed"));

        assertThrows(IllegalStateException.class, () -> accountDAO.deleteAccount(connection, 1));
    }
}