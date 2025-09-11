package com.hitorro.jdbcproxy.examples;

import com.hitorro.jdbcproxy.driver.ProxyDriver;
import com.hitorro.jdbcproxy.extensions.ConsoleJdbcLogger;
import com.hitorro.jdbcproxy.extensions.DataMaskingProcessor;
import com.hitorro.jdbcproxy.extensions.TablePrefixRewriter;
import com.hitorro.jdbcproxy.hooks.ProxyConfiguration;

import java.sql.*;
import java.util.Properties;

/**
 * Example application demonstrating JDBC proxy driver functionality.
 */
public class ExampleApplication {

    public static void main(String[] args) throws SQLException {
        System.out.println("=== JDBC Proxy Driver Example ===\n");
        
        // Setup H2 in-memory database with test data
        setupTestData();
        
        // Example 1: Basic logging
        System.out.println("1. Basic Logging Example:");
        basicLoggingExample();
        
        System.out.println("\n" + "=".repeat(50) + "\n");
        
        // Example 2: SQL rewriting
        System.out.println("2. SQL Rewriting Example:");
        sqlRewritingExample();
        
        System.out.println("\n" + "=".repeat(50) + "\n");
        
        // Example 3: Data masking
        System.out.println("3. Data Masking Example:");
        dataMaskingExample();
        
        System.out.println("\n" + "=".repeat(50) + "\n");
        
        // Example 4: Multiple configurations
        System.out.println("4. Multiple Configurations Example:");
        multipleConfigurationsExample();
    }

    private static void setupTestData() throws SQLException {
        String directUrl = "jdbc:h2:mem:example;DB_CLOSE_DELAY=-1";
        
        try (Connection conn = DriverManager.getConnection(directUrl, "sa", "")) {
            Statement stmt = conn.createStatement();
            
            // Create users table
            stmt.execute("CREATE TABLE users (" +
                        "id INT PRIMARY KEY, " +
                        "name VARCHAR(100), " +
                        "email VARCHAR(100), " +
                        "password VARCHAR(100), " +
                        "ssn VARCHAR(11)" +
                        ")");
            
            // Insert test data
            stmt.execute("INSERT INTO users VALUES " +
                        "(1, 'John Doe', 'john@example.com', 'secret123', '123-45-6789'), " +
                        "(2, 'Jane Smith', 'jane@example.com', 'password456', '987-65-4321')");
            
            // Create orders table
            stmt.execute("CREATE TABLE orders (" +
                        "id INT PRIMARY KEY, " +
                        "user_id INT, " +
                        "amount DECIMAL(10,2), " +
                        "order_date DATE" +
                        ")");
            
            stmt.execute("INSERT INTO orders VALUES " +
                        "(1, 1, 100.50, '2023-01-15'), " +
                        "(2, 2, 75.25, '2023-01-16'), " +
                        "(3, 1, 200.00, '2023-01-17')");
        }
    }

    private static void basicLoggingExample() throws SQLException {
        // Configure proxy with console logger
        ProxyConfiguration config = ProxyDriver.getConfiguration("basic");
        config.clearAll(); // Clear any previous configuration
        config.addLogger(new ConsoleJdbcLogger(true, true)); // Log parameters and timing

        String proxyUrl = "jdbc:proxy:jdbc:h2:mem:example";
        
        Properties props = new Properties();
        props.setProperty("user", "sa");
        props.setProperty("password", "");
        props.setProperty("proxy.configKey", "basic");

        try (Connection conn = DriverManager.getConnection(proxyUrl, props)) {
            // Simple query
            try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE id = ?")) {
                stmt.setInt(1, 1);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        System.out.printf("Found user: %s (%s)%n", rs.getString("name"), rs.getString("email"));
                    }
                }
            }
            
            // Batch operation
            try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO orders VALUES (?, ?, ?, ?)")) {
                stmt.setInt(1, 4);
                stmt.setInt(2, 2);
                stmt.setBigDecimal(3, new java.math.BigDecimal("150.00"));
                stmt.setDate(4, Date.valueOf("2023-01-18"));
                stmt.addBatch();
                
                stmt.setInt(1, 5);
                stmt.setInt(2, 1);
                stmt.setBigDecimal(3, new java.math.BigDecimal("300.00"));
                stmt.setDate(4, Date.valueOf("2023-01-19"));
                stmt.addBatch();
                
                stmt.executeBatch();
            }
            
            // Transaction
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("UPDATE users SET email = 'newemail@example.com' WHERE id = 1");
                conn.commit();
            }
        }
    }

    private static void sqlRewritingExample() throws SQLException {
        // Configure proxy with table prefix rewriter
        ProxyConfiguration config = ProxyDriver.getConfiguration("rewriter");
        config.clearAll();
        config.addLogger(new ConsoleJdbcLogger(true, true));
        config.addRewriter(new TablePrefixRewriter("tenant1_"));

        String proxyUrl = "jdbc:proxy:jdbc:h2:mem:example";
        
        Properties props = new Properties();
        props.setProperty("user", "sa");
        props.setProperty("password", "");
        props.setProperty("proxy.configKey", "rewriter");

        // First, create the tenant table directly
        try (Connection directConn = DriverManager.getConnection("jdbc:h2:mem:example", "sa", "")) {
            Statement stmt = directConn.createStatement();
            stmt.execute("CREATE TABLE tenant1_customers (" +
                        "id INT PRIMARY KEY, " +
                        "name VARCHAR(100), " +
                        "tenant_id VARCHAR(50)" +
                        ")");
            stmt.execute("INSERT INTO tenant1_customers VALUES " +
                        "(1, 'Customer A', 'tenant1'), " +
                        "(2, 'Customer B', 'tenant1')");
        }

        try (Connection conn = DriverManager.getConnection(proxyUrl, props)) {
            // This query will be rewritten from "customers" to "tenant1_customers"
            System.out.println("Querying 'customers' table (will be rewritten to 'tenant1_customers'):");
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("SELECT * FROM customers WHERE tenant_id = 'tenant1'")) {
                    while (rs.next()) {
                        System.out.printf("Customer: %s (tenant: %s)%n", 
                                        rs.getString("name"), rs.getString("tenant_id"));
                    }
                }
            }
        }
    }

    private static void dataMaskingExample() throws SQLException {
        // Configure proxy with data masking processor
        ProxyConfiguration config = ProxyDriver.getConfiguration("masking");
        config.clearAll();
        config.addLogger(new ConsoleJdbcLogger(false, false)); // Minimal logging to focus on masking
        config.addProcessor(new DataMaskingProcessor());

        String proxyUrl = "jdbc:proxy:jdbc:h2:mem:example";
        
        Properties props = new Properties();
        props.setProperty("user", "sa");
        props.setProperty("password", "");
        props.setProperty("proxy.configKey", "masking");

        try (Connection conn = DriverManager.getConnection(proxyUrl, props)) {
            System.out.println("Querying users with sensitive data (password and SSN will be masked):");
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("SELECT name, email, password, ssn FROM users")) {
                    while (rs.next()) {
                        System.out.printf("User: %s%n", rs.getString("name"));
                        System.out.printf("  Email: %s%n", rs.getString("email"));
                        System.out.printf("  Password: %s%n", rs.getString("password"));
                        System.out.printf("  SSN: %s%n", rs.getString("ssn"));
                        System.out.println();
                    }
                }
            }
        }
    }

    private static void multipleConfigurationsExample() throws SQLException {
        // Set up development configuration
        ProxyConfiguration devConfig = new ProxyConfiguration();
        devConfig.addLogger(new ConsoleJdbcLogger(true, true));
        devConfig.addRewriter(new TablePrefixRewriter("dev_"));
        ProxyDriver.setConfiguration("development", devConfig);

        // Set up production configuration
        ProxyConfiguration prodConfig = new ProxyConfiguration();
        prodConfig.addLogger(new ConsoleJdbcLogger(false, false));
        prodConfig.addProcessor(new DataMaskingProcessor());
        ProxyDriver.setConfiguration("production", prodConfig);

        String proxyUrl = "jdbc:proxy:jdbc:h2:mem:example";

        // Create dev table
        try (Connection directConn = DriverManager.getConnection("jdbc:h2:mem:example", "sa", "")) {
            Statement stmt = directConn.createStatement();
            stmt.execute("CREATE TABLE dev_products (" +
                        "id INT PRIMARY KEY, " +
                        "name VARCHAR(100), " +
                        "price DECIMAL(10,2)" +
                        ")");
            stmt.execute("INSERT INTO dev_products VALUES (1, 'Test Product', 99.99)");
        }

        // Use development configuration
        System.out.println("Using DEVELOPMENT configuration:");
        Properties devProps = new Properties();
        devProps.setProperty("user", "sa");
        devProps.setProperty("password", "");
        devProps.setProperty("proxy.configKey", "development");

        try (Connection conn = DriverManager.getConnection(proxyUrl, devProps)) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("SELECT * FROM products")) {
                    while (rs.next()) {
                        System.out.printf("Product: %s - $%.2f%n", 
                                        rs.getString("name"), rs.getDouble("price"));
                    }
                }
            }
        }

        System.out.println("\nUsing PRODUCTION configuration:");
        Properties prodProps = new Properties();
        prodProps.setProperty("user", "sa");
        prodProps.setProperty("password", "");
        prodProps.setProperty("proxy.configKey", "production");

        try (Connection conn = DriverManager.getConnection(proxyUrl, prodProps)) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("SELECT name, email, password FROM users LIMIT 1")) {
                    while (rs.next()) {
                        System.out.printf("User data (with masking): %s, %s, %s%n",
                                        rs.getString("name"),
                                        rs.getString("email"),
                                        rs.getString("password"));
                    }
                }
            }
        }
    }
}
