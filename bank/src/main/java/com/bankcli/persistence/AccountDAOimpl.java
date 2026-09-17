package com.bankcli.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.bankcli.domain.Account;

public class AccountDAOimpl implements AccountDAO {

    public AccountDAOimpl(){
        initializeSchema();
    }

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS account (
                account_id SERIAL PRIMARY KEY,
                pin VARCHAR(255) NOT NULL,
                balance NUMERIC(12, 2) NOT NULL DEFAULT 0.00
            );
            """;


            
    private static final String INSERT_SQL = "INSERT INTO account (pin, balance) VALUES (?, ?) RETURNING account_id"; //Account ID is SERIAL, it is generated automatically.
    private static final String FIND_BY_ID_SQL = "SELECT account_id, pin, balance FROM account WHERE account_id = ?";
    private static final String FIND_ALL_SQL = "SELECT account_id, pin, balance FROM account ORDER BY account_id";
    private static final String UPDATE_SQL = "UPDATE account SET pin = ?, balance = ? WHERE account_id = ?";
    private static final String DELETE_SQL = "DELETE FROM account WHERE account_id = ?";
    

    @Override
    public void addAccount(Account account) {
        try (Connection connection = ConnectionFactory.getConnectionFactory().getConnection();
                PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {

            statement.setString(1, account.getPin());
            statement.setDouble(2, account.getBalance());

            ResultSet resultSet = statement.executeQuery(); // executeQuery, since RETURNING produces a result set
            if (resultSet.next()) {
                account.setAccountId(resultSet.getInt("account_id"));
            }

        } catch (SQLException e) {
            throw databaseError("Could not add account. ", e);
        }
    }

    @Override
    public Account getAccountById(int id) {
        try (Connection connection = ConnectionFactory.getConnectionFactory().getConnection()) {
            return getAccountById(connection, id);
        }catch (SQLException e) {
            throw databaseError("Could not retrieve account info. ", e);
        }
    }

    @Override
    public Account getAccountById(Connection connection, int id) {
        try (PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
            statement.setInt(1, id);
        try (ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return mapAccount(resultSet);
            }
        }
        return null;
        } catch (SQLException e) {
            throw databaseError("Could not retrieve account info. ", e);
        }
    }


    @Override
    public List<Account> getAllAccounts() {

        List<Account> accounts = new ArrayList<>();

        try (Connection connection = ConnectionFactory.getConnectionFactory().getConnection();
                PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL)) {
                ResultSet resultSet = statement.executeQuery();

                while(resultSet.next()){
                    accounts.add(mapAccount(resultSet));

                }
                return accounts;

        } catch (SQLException e) {
            throw databaseError("Could not retrieve all accounts. ", e);
        }
        
    }

    @Override
    public void updateAccount(Connection connection, Account updatedAccount) {
        try(PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)){
                statement.setString(1, updatedAccount.getPin());
                statement.setDouble(2, updatedAccount.getBalance());
                statement.setInt(3, updatedAccount.getAccountId());
                statement.executeUpdate();
            
        } catch (SQLException e) {
            throw databaseError("Could not update account. ", e);
        }
        
    }

    @Override
    public void updateAccount(Account account) {
        try (Connection connection = ConnectionFactory.getConnectionFactory().getConnection()) {
            updateAccount(connection, account);
        } catch (SQLException e) {
            throw databaseError("Could not update account. ", e);
        }
    }

    @Override
    public void deleteAccount(int id) {
        try(Connection connection = ConnectionFactory.getConnectionFactory().getConnection();
                PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
                statement.setInt(1, id);
                statement.executeUpdate();

            
        } catch (SQLException e) {
            throw databaseError("Could not delete account. ", e);
        }
    }

     private IllegalStateException databaseError(String message, SQLException cause) {
        return new IllegalStateException(message, cause);
    }

    private Account mapAccount(ResultSet resultSet) throws SQLException {
        return new Account(
                resultSet.getInt("account_id"),
                resultSet.getString("pin"),
                resultSet.getDouble("balance"));
    }

    private void initializeSchema() {
        try (Connection connection = ConnectionFactory.getConnectionFactory().getConnection();
                PreparedStatement statement = connection.prepareStatement(CREATE_TABLE_SQL)) {
            statement.executeUpdate();
        } catch (SQLException e) {
            throw databaseError("Could not initialize database schema", e);
        }
    }

    

    
}
