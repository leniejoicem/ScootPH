package com.payroll.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;
import java.util.logging.Logger;

public final class ResilientConnection implements InvocationHandler {

    private static final Logger LOGGER = Logger.getLogger(ResilientConnection.class.getName());
    private static final Set<String> STATEMENT_FACTORIES = Set.of("createStatement", "prepareStatement", "prepareCall");
    private static final long CHECK_INTERVAL_MS = 15_000;

    @FunctionalInterface
    public interface Opener {
        Connection open() throws SQLException;
    }

    private final Opener opener;
    private Connection delegate;
    private long lastChecked;

    private ResilientConnection(Opener opener) throws SQLException {
        this.opener = opener;
        this.delegate = opener.open();
        this.lastChecked = System.currentTimeMillis();
    }

    public static Connection open(Opener opener) throws SQLException {
        return (Connection) Proxy.newProxyInstance(ResilientConnection.class.getClassLoader(),
                new Class<?>[]{Connection.class}, new ResilientConnection(opener));
    }

    public static Connection open() throws SQLException {
        return open(DatabaseConnection::getConnection);
    }

    @Override
    public synchronized Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (STATEMENT_FACTORIES.contains(method.getName())) {
            ensureAlive();
        }
        try {
            return method.invoke(delegate, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    private void ensureAlive() throws SQLException {
        long now = System.currentTimeMillis();
        if (now - lastChecked < CHECK_INTERVAL_MS) {
            return;
        }
        try {
            if (!delegate.getAutoCommit()) {
                return;
            }
        } catch (SQLException e) {
        }
        lastChecked = now;
        boolean alive;
        try {
            alive = !delegate.isClosed() && delegate.isValid(3);
        } catch (SQLException e) {
            alive = false;
        }
        if (!alive) {
            LOGGER.warning("Database connection lost; reconnecting");
            try {
                delegate.close();
            } catch (SQLException ignored) {
            }
            delegate = opener.open();
        }
    }
}
