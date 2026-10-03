package com.payroll.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SchemaMigrator {

    private static final Logger LOGGER = Logger.getLogger(SchemaMigrator.class.getName());
    private static final String LOCATION = "db/migration/";
    private static final Pattern NAME = Pattern.compile("V(\\d+)__(.+)\\.sql");

    private SchemaMigrator() {
    }

    public record Migration(int version, String description, String resource) {
    }

    public static List<Migration> migrate(Connection connection) throws SQLException {
        ensureVersionTable(connection);
        Set<Integer> applied = appliedVersions(connection);
        List<Migration> ran = new ArrayList<>();
        for (Migration migration : available()) {
            if (applied.contains(migration.version())) {
                continue;
            }
            apply(connection, migration);
            ran.add(migration);
            LOGGER.info(() -> "Applied database migration V" + migration.version() + " " + migration.description());
        }
        return ran;
    }

    public static List<Migration> available() {
        List<Migration> migrations = new ArrayList<>();
        try (InputStream index = resource(LOCATION + "index.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(index, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                Matcher m = NAME.matcher(line);
                if (!m.matches()) {
                    throw new IllegalStateException("Bad migration file name: " + line);
                }
                migrations.add(new Migration(Integer.parseInt(m.group(1)),
                        m.group(2).replace('_', ' '), LOCATION + line));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read migration index", e);
        }
        migrations.sort((a, b) -> Integer.compare(a.version(), b.version()));
        return migrations;
    }

    private static void ensureVersionTable(Connection connection) throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS public.scootph_schema_version ("
                    + " version integer PRIMARY KEY,"
                    + " description text NOT NULL,"
                    + " applied_at timestamptz NOT NULL DEFAULT now())");
        }
    }

    private static Set<Integer> appliedVersions(Connection connection) throws SQLException {
        Set<Integer> versions = new HashSet<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT version FROM public.scootph_schema_version")) {
            while (rs.next()) {
                versions.add(rs.getInt(1));
            }
        }
        return versions;
    }

    private static void apply(Connection connection, Migration migration) throws SQLException {
        String sql;
        try (InputStream in = resource(migration.resource())) {
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new SQLException("Cannot read " + migration.resource(), e);
        }
        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (Statement st = connection.createStatement();
             PreparedStatement record = connection.prepareStatement(
                     "INSERT INTO public.scootph_schema_version (version, description) VALUES (?, ?)")) {
            st.execute(sql);
            record.setInt(1, migration.version());
            record.setString(2, migration.description());
            record.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw new SQLException("Migration V" + migration.version() + " (" + migration.description()
                    + ") failed: " + e.getMessage(), e);
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }

    private static InputStream resource(String path) throws IOException {
        InputStream in = SchemaMigrator.class.getClassLoader().getResourceAsStream(path);
        if (in == null) {
            throw new IOException("Missing classpath resource " + path);
        }
        return in;
    }
}
