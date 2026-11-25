package config;

import exception.DatabaseOperationException;
import util.ConsoleFormatter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Singleton database connection manager using JDBC and MySQL 8.x Connector.
 * Prioritizes environment variables and untracked local configurations over source-controlled files.
 * Protects existing business records by refusing destructive DROP operations during initialization.
 */
public class DBConnection {

    private static String url = "jdbc:mysql://localhost:3306/inventory_business_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true";
    private static String user = "root";
    private static String password = "dilli@2007";
    private static String driver = "com.mysql.cj.jdbc.Driver";

    static {
        loadConfiguration();
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            System.err.println("[CRITICAL] MySQL JDBC Driver not found: " + driver);
        }
    }

    /**
     * Loads database configuration with the following strict priority:
     * 1. Environment variables (DB_URL, DB_USER, DB_PASSWORD or MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD)
     * 2. Untracked local configuration file (database.local.properties)
     * 3. Source-controlled resource template (database.properties)
     */
    public static void loadConfiguration() {
        Properties props = new Properties();

        // 1. Try loading source-controlled template as base
        File propFile = new File("src/main/resources/database.properties");
        if (propFile.exists()) {
            try (InputStream in = new FileInputStream(propFile)) {
                props.load(in);
            } catch (Exception ignored) {}
        } else {
            try (InputStream in = DBConnection.class.getClassLoader().getResourceAsStream("database.properties")) {
                if (in != null) {
                    props.load(in);
                }
            } catch (Exception ignored) {}
        }

        // 2. Override with untracked local configuration file if present
        File localPropFile = new File("database.local.properties");
        if (localPropFile.exists()) {
            try (InputStream in = new FileInputStream(localPropFile)) {
                props.load(in);
            } catch (Exception ignored) {}
        }

        url = props.getProperty("db.url", url);
        user = props.getProperty("db.user", user);
        password = props.getProperty("db.password", password);
        driver = props.getProperty("db.driver", driver);

        // 3. Override with environment variables if set
        String envUrl = System.getenv("DB_URL");
        if (envUrl == null || envUrl.isEmpty()) envUrl = System.getenv("MYSQL_URL");
        if (envUrl != null && !envUrl.isEmpty()) url = envUrl;

        String envUser = System.getenv("DB_USER");
        if (envUser == null || envUser.isEmpty()) envUser = System.getenv("MYSQL_USER");
        if (envUser != null && !envUser.isEmpty()) user = envUser;

        String envPass = System.getenv("DB_PASSWORD");
        if (envPass == null) envPass = System.getenv("MYSQL_PASSWORD");
        if (envPass != null) password = envPass;
    }

    /**
     * Obtains a new JDBC Connection from DriverManager.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Configures database credentials dynamically at runtime.
     */
    public static void setCredentials(String newUrl, String newUser, String newPassword) {
        if (newUrl != null && !newUrl.trim().isEmpty()) {
            url = newUrl.trim();
        }
        if (newUser != null) {
            user = newUser.trim();
        }
        if (newPassword != null) {
            password = newPassword;
        }
    }

    /**
     * Tests database connectivity and reports diagnostic status.
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Non-destructively verifies if database schema exists, initializing only if required tables are absent.
     * Never executes destructive DROP DATABASE or DROP TABLE commands.
     */
    public static boolean ensureDatabaseInitialized() {
        try (Connection conn = getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SHOW TABLES LIKE 'users'");
            if (rs.next()) {
                return true; // Tables already exist, preserve all data
            }

            ConsoleFormatter.printInfo("Database tables not found. Initializing missing schema from database.sql...");
            File sqlFile = new File("database.sql");
            if (!sqlFile.exists()) {
                ConsoleFormatter.printWarning("database.sql not found at project root. Please import manually.");
                return false;
            }

            executeSqlScript(conn, sqlFile);
            ConsoleFormatter.printSuccess("Database schema initialized non-destructively!");
            return true;
        } catch (SQLException e) {
            ConsoleFormatter.printError("Database connection or initialization check failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Executes SQL script non-destructively, strictly filtering out any DROP commands.
     */
    public static void executeSqlScript(Connection conn, File sqlFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(sqlFile));
             Statement stmt = conn.createStatement()) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("--") || line.startsWith("//") || line.isEmpty()) {
                    continue;
                }
                sb.append(line).append(" ");
                if (line.endsWith(";")) {
                    String sql = sb.toString().trim();
                    if (sql.length() > 1) {
                        String cleanSql = sql.substring(0, sql.length() - 1).trim();
                        String upper = cleanSql.toUpperCase();

                        // Strict guard against destructive actions during normal application lifecycle
                        if (upper.startsWith("DROP DATABASE") || upper.startsWith("DROP TABLE")) {
                            System.out.println("[GUARD] Skipped destructive command during initialization: " + cleanSql);
                            sb.setLength(0);
                            continue;
                        }

                        if (!upper.startsWith("USE ")) {
                            try {
                                stmt.execute(cleanSql);
                            } catch (SQLException ex) {
                                // Ignore non-critical notices (e.g. table already exists)
                                if (!ex.getMessage().contains("already exists")) {
                                    System.err.println("[WARN] SQL statement notice: " + ex.getMessage());
                                }
                            }
                        }
                    }
                    sb.setLength(0);
                }
            }
        } catch (Exception e) {
            throw new DatabaseOperationException("Failed to run database initialization script: " + e.getMessage(), e);
        }
    }

    public static String getUrl() {
        return url;
    }

    public static String getUser() {
        return user;
    }
}
