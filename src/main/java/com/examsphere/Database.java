package com.examsphere;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Central place for the MySQL connection settings. */
public final class Database {
    private static final String URL = System.getenv().getOrDefault(
            "EXAMSPHERE_DB_URL", "jdbc:mysql://localhost:3306/examsphere?useSSL=false&serverTimezone=UTC");
    private static final String USER = System.getenv().getOrDefault("EXAMSPHERE_DB_USER", "root");
    private static final String PASSWORD = System.getenv().getOrDefault("EXAMSPHERE_DB_PASSWORD", "");

    private Database() {}

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
