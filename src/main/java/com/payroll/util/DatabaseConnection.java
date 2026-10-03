package com.payroll.util;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DatabaseConnection {
    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());

    private static final Properties FILE_SETTINGS = loadFileSettings();

    private static Properties loadFileSettings() {
        Properties prop = new Properties();
        try (InputStream input = DatabaseConnection.class.getClassLoader()
                .getResourceAsStream("config/db.properties")) {
            if (input != null) {
                prop.load(input);
            } else {
                LOGGER.warning("config/db.properties not found; using environment settings only");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Could not load config/db.properties", e);
        }
        return prop;
    }

    private DatabaseConnection() {
    }

    private static String setting(String systemProperty, String environmentVariable, String fallback) {
        String value = System.getProperty(systemProperty);
        if (value == null || value.isBlank()) {
            value = System.getenv(environmentVariable);
        }
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public static Connection getConnection() throws SQLException {
        String url = getUrl();
        String username = setting("scootph.db.user", "SCOOTPH_DB_USER", FILE_SETTINGS.getProperty("db.username"));
        String password = setting("scootph.db.password", "SCOOTPH_DB_PASSWORD", FILE_SETTINGS.getProperty("db.password"));
        if (url == null) {
            throw new SQLException("No database URL configured (config/db.properties or SCOOTPH_DB_URL)");
        }
        return DriverManager.getConnection(url, username, password);
    }

    public static String getUrl() {
        return setting("scootph.db.url", "SCOOTPH_DB_URL", FILE_SETTINGS.getProperty("db.url"));
    }
}
