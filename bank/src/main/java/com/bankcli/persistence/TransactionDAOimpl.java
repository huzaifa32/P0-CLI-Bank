package com.bankcli.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.bankcli.domain.Transaction;

public class TransactionDAOimpl implements TransactionDAO {

    private final ConnectionFactory connectionFactory;

    public TransactionDAOimpl(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
        initializeSchema();
    }

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS transaction (
                transaction_id SERIAL PRIMARY KEY,
                account_id INTEGER NOT NULL REFERENCES account(account_id),
                type VARCHAR(20) NOT NULL,
                amount NUMERIC(12, 2) NOT NULL,
                related_account_id INTEGER REFERENCES account(account_id),
                timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            );
            """;

    private static final String INSERT_SQL =
            "INSERT INTO transaction (account_id, type, amount, related_account_id) VALUES (?, ?, ?, ?) RETURNING transaction_id, timestamp";

    private static final String FIND_BY_ID_SQL =
            "SELECT transaction_id, account_id, type, amount, related_account_id, timestamp FROM transaction WHERE transaction_id = ?";

    private static final String FIND_ALL_SQL =
            "SELECT transaction_id, account_id, type, amount, related_account_id, timestamp FROM transaction ORDER BY transaction_id";

    private static final String DELETE_SQL =
            "DELETE FROM transaction WHERE transaction_id = ?";
    
    private static final String FIND_HISTORY_BY_ID = 
            "SELECT transaction_id, account_id, type, amount, related_account_id, timestamp FROM transaction WHERE account_id = ?";

    // ---------- Single place connection lifecycle is handled ----------
    private <T> T withConnection(Function<Connection, T> work) {
        try (Connection connection = connectionFactory.getConnection()) {
            return work.apply(connection);
        } catch (SQLException e) {
            throw databaseError("Database operation failed. ", e);
        }
    }

    // ---------- addTransaction ----------
    @Override
    public void addTransaction(Transaction transaction) {
        withConnection(connection -> {
            addTransaction(connection, transaction);
            return null;
        });
    }

    @Override
    public void addTransaction(Connection connection, Transaction transaction) {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            statement.setInt(1, transaction.getAccountId());
            statement.setString(2, transaction.getType());
            statement.setDouble(3, transaction.getAmount());

            if (transaction.getRelatedAccountId() != null) {
                statement.setInt(4, transaction.getRelatedAccountId());
            } else {
                statement.setNull(4, Types.INTEGER);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    transaction.setTransactionId(resultSet.getInt("transaction_id"));
                    transaction.setTimestamp(resultSet.getTimestamp("timestamp"));
                }
            }
        } catch (SQLException e) {
            throw databaseError("Could not add transaction. ", e);
        }
    }

    // ---------- getTransactionById ----------
    @Override
    public Transaction getTransactionById(int id) {
        return withConnection(connection -> getTransactionById(connection, id));
    }

    @Override
    public Transaction getTransactionById(Connection connection, int id) {
        try (PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapTransaction(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw databaseError("Could not retrieve transaction. ", e);
        }
    }

    // ---------- getAllTransactions ----------
    @Override
    public List<Transaction> getAllTransactions() {
        return withConnection(this::getAllTransactions);
    }

    @Override
    public List<Transaction> getAllTransactions(Connection connection) {
        List<Transaction> transactions = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL);
                ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                transactions.add(mapTransaction(resultSet));
            }
            return transactions;

        } catch (SQLException e) {
            throw databaseError("Could not retrieve all transactions. ", e);
        }
    }

    @Override
    public List<Transaction> getTransactionHistoryById(int id) {
        return withConnection(connection -> getTransactionHistoryById(connection, id));
    }

    @Override 
    public List<Transaction> getTransactionHistoryById(Connection connection, int id) {
        List<Transaction> history = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(FIND_HISTORY_BY_ID)) {
            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    history.add(mapTransaction(resultSet));
                }
            }
            return history;

        } catch (SQLException e) {
            throw databaseError("Could not retrieve transaction history. ", e);
        }
    }

    // ---------- deleteTransaction ----------
    @Override
    public void deleteTransaction(int id) {
        withConnection(connection -> {
            deleteTransaction(connection, id);
            return null;
        });
    }

    @Override
    public void deleteTransaction(Connection connection, int id) {
        try (PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw databaseError("Could not delete transaction. ", e);
        }
    }

    // ---------- helpers ----------
    private Transaction mapTransaction(ResultSet resultSet) throws SQLException {
        int relatedAccountId = resultSet.getInt("related_account_id");
        Integer relatedAccountIdOrNull = resultSet.wasNull() ? null : relatedAccountId;

        return new Transaction(
                resultSet.getInt("transaction_id"),
                resultSet.getInt("account_id"),
                resultSet.getString("type"),
                resultSet.getDouble("amount"),
                relatedAccountIdOrNull,
                resultSet.getTimestamp("timestamp"));
    }

    private IllegalStateException databaseError(String message, SQLException cause) {
        return new IllegalStateException(message, cause);
    }

    private void initializeSchema() {
        withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(CREATE_TABLE_SQL)) {
                statement.executeUpdate();
                return null;
            } catch (SQLException e) {
                throw databaseError("Could not initialize database schema", e);
            }
        });
    }
}