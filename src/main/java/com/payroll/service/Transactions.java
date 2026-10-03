package com.payroll.service;

import com.payroll.DAO.DataAccessException;
import java.sql.Connection;
import java.sql.SQLException;

public final class Transactions {

    @FunctionalInterface
    public interface Work<T> {
        T run() throws SQLException;
    }

    private Transactions() {
    }

    public static <T> T inTransaction(Connection connection, Work<T> work) {
        try {
            boolean autoCommit = connection.getAutoCommit();
            if (!autoCommit) {
                return work.run();
            }
            connection.setAutoCommit(false);
            try {
                T result = work.run();
                connection.commit();
                return result;
            } catch (RuntimeException | SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Transaction failed", e);
        }
    }

    public static void inTransaction(Connection connection, Runnable work) {
        inTransaction(connection, () -> {
            work.run();
            return null;
        });
    }
}
