package com.hitorro.jdbcproxy;

import com.hitorro.jdbcproxy.driver.ProxyDriver;
import com.hitorro.jdbcproxy.extensions.ConsoleJdbcLogger;
import com.hitorro.jdbcproxy.extensions.DataMaskingProcessor;
import com.hitorro.jdbcproxy.extensions.TablePrefixRewriter;
import com.hitorro.jdbcproxy.hooks.ProxyConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.*;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test demonstrating the JDBC proxy functionality with H2 database.
 */
public class ProxyDriverIntegrationTest {

    private static final String H2_URL = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
    private static final String PROXY_URL = "jdbc:proxy:" + H2_URL;

    @BeforeEach
    public void setUp() throws SQLException {
        // Clear any existing configuration
        ProxyDriver.getConfiguration("default").clearAll();
        
        // Set up H2 database with test data
        try (Connection conn = DriverManager.getConnection(H2_URL, "sa", "")) {
            Statement stmt = conn.createStatement();
            
            // Create test tables
            stmt.execute("DROP TABLE IF EXISTS users");
            stmt.execute("CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(100), email VARCHAR(100), password VARCHAR(100))");
            
            stmt.execute("DROP TABLE IF EXISTS orders");
            stmt.execute("CREATE TABLE orders (id INT PRIMARY KEY, user_id INT, amount DECIMAL(10,2))");
            
            // Insert test data
            stmt.execute("INSERT INTO users VALUES (1, 'John Doe', 'john@example.com', 'secret123')");
            stmt.execute("INSERT INTO users VALUES (2, 'Jane Smith', 'jane@example.com', 'password456')");
            stmt.execute("INSERT INTO orders VALUES (1, 1, 100.50)");
            stmt.execute("INSERT INTO orders VALUES (2, 2, 75.25)");
        }
    }

    @Test
    public void testBasicProxyFunctionality() throws SQLException {
        // Configure proxy with console logger
        ProxyConfiguration config = ProxyDriver.getConfiguration("default");
        config.addLogger(new ConsoleJdbcLogger(true, true));

        Properties props = new Properties();
        props.setProperty("user", "sa");
        props.setProperty("password", "");

        try (Connection conn = DriverManager.getConnection(PROXY_URL, props)) {
            assertNotNull(conn);
            assertFalse(conn.isClosed());

            // Test basic query
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("SELECT * FROM users WHERE id = 1")) {
                    assertTrue(rs.next());
                    assertEquals(1, rs.getInt("id"));
                    assertEquals("John Doe", rs.getString("name"));
                    assertEquals("john@example.com", rs.getString("email"));
                    assertFalse(rs.next());
                }
            }
        }
    }

    @Test
    public void testPreparedStatementWithLogging() throws SQLException {
        ProxyConfiguration config = ProxyDriver.getConfiguration("default");
        config.addLogger(new ConsoleJdbcLogger(true, true));

        Properties props = new Properties();
        props.setProperty("user", "sa");
        props.setProperty("password", "");

        try (Connection conn = DriverManager.getConnection(PROXY_URL, props)) {
            String sql = "SELECT * FROM users WHERE name = ?";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, "Jane Smith");
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(2, rs.getInt("id"));
                    assertEquals("Jane Smith", rs.getString("name"));
                    assertFalse(rs.next());
                }
            }
        }
    }

    @Test
    public void testTablePrefixRewriter() throws SQLException {
        ProxyConfiguration config = ProxyDriver.getConfiguration("default");
        config.addLogger(new ConsoleJdbcLogger(true, true));
        config.addRewriter(new TablePrefixRewriter("test_"));

        Properties props = new Properties();
        props.setProperty("user", "sa");
        props.setProperty("password", "");

        try (Connection conn = DriverManager.getConnection(PROXY_URL, props)) {
            // Create table with prefix
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE test_customers (id INT PRIMARY KEY, name VARCHAR(100))");
                stmt.execute("INSERT INTO test_customers VALUES (1, 'Customer 1')");
            }

            // Query should be rewritten from "customers" to "test_customers"
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("SELECT * FROM customers")) {
                    assertTrue(rs.next());
                    assertEquals("Customer 1", rs.getString("name"));
                }
            }
        }
    }

    @Test
    public void testDataMaskingProcessor() throws SQLException {
        ProxyConfiguration config = ProxyDriver.getConfiguration("default");
        config.addLogger(new ConsoleJdbcLogger(true, true));
        config.addProcessor(new DataMaskingProcessor());

        Properties props = new Properties();
        props.setProperty("user", "sa");
        props.setProperty("password", "");

        try (Connection conn = DriverManager.getConnection(PROXY_URL, props)) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("SELECT name, email, password FROM users WHERE id = 1")) {
                    assertTrue(rs.next());
                    
                    // Name should not be masked
                    assertEquals("John Doe", rs.getString("name"));
                    
                    // Email should be masked (contains sensitive pattern)
                    String email = rs.getString("email");
                    assertTrue(email.startsWith("j") && email.endsWith("m") && email.contains("*"));
                    
                    // Password should be masked
                    String password = rs.getString("password");
                    assertTrue(password.startsWith("s") && password.endsWith("3") && password.contains("*"));
                }
            }
        }
    }

    @Test
    public void testTransactionLogging() throws SQLException {
        ProxyConfiguration config = ProxyDriver.getConfiguration("default");
        config.addLogger(new ConsoleJdbcLogger(true, true));

        Properties props = new Properties();
        props.setProperty("user", "sa");
        props.setProperty("password", "");

        try (Connection conn = DriverManager.getConnection(PROXY_URL, props)) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO users VALUES (?, ?, ?, ?)")) {
                pstmt.setInt(1, 3);
                pstmt.setString(2, "Bob Wilson");
                pstmt.setString(3, "bob@example.com");
                pstmt.setString(4, "bobsecret");
                pstmt.executeUpdate();
            }
            
            conn.commit(); // This should be logged
            
            // Verify the insert worked
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
                    assertTrue(rs.next());
                    assertEquals(3, rs.getInt(1));
                }
            }
        }
    }

    @Test
    public void testBatchOperations() throws SQLException {
        ProxyConfiguration config = ProxyDriver.getConfiguration("default");
        config.addLogger(new ConsoleJdbcLogger(true, true));

        Properties props = new Properties();
        props.setProperty("user", "sa");
        props.setProperty("password", "");

        try (Connection conn = DriverManager.getConnection(PROXY_URL, props)) {
            try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO orders VALUES (?, ?, ?)")) {
                pstmt.setInt(1, 3);
                pstmt.setInt(2, 1);
                pstmt.setBigDecimal(3, new java.math.BigDecimal("50.00"));
                pstmt.addBatch();
                
                pstmt.setInt(1, 4);
                pstmt.setInt(2, 2);
                pstmt.setBigDecimal(3, new java.math.BigDecimal("25.75"));
                pstmt.addBatch();
                
                int[] results = pstmt.executeBatch(); // This should be logged
                assertEquals(2, results.length);
                assertEquals(1, results[0]);
                assertEquals(1, results[1]);
            }
        }
    }

    @Test
    public void testMultipleConfigurations() throws SQLException {
        // Set up different configuration for different purposes
        ProxyConfiguration debugConfig = new ProxyConfiguration();
        debugConfig.addLogger(new ConsoleJdbcLogger(true, true));
        debugConfig.addRewriter(new TablePrefixRewriter("debug_"));
        ProxyDriver.setConfiguration("debug", debugConfig);

        ProxyConfiguration prodConfig = new ProxyConfiguration();
        prodConfig.addLogger(new ConsoleJdbcLogger(false, false)); // Less verbose
        prodConfig.addProcessor(new DataMaskingProcessor());
        ProxyDriver.setConfiguration("prod", prodConfig);

        // Test debug configuration
        Properties debugProps = new Properties();
        debugProps.setProperty("user", "sa");
        debugProps.setProperty("password", "");
        debugProps.setProperty("proxy.configKey", "debug");

        try (Connection conn = DriverManager.getConnection(PROXY_URL, debugProps)) {
            // Create table for debug config
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE debug_test (id INT PRIMARY KEY, data VARCHAR(100))");
                stmt.execute("INSERT INTO debug_test VALUES (1, 'Debug Data')");
            }
            
            // This query should use debug_ prefix due to rewriter
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("SELECT * FROM test")) {
                    assertTrue(rs.next());
                    assertEquals("Debug Data", rs.getString("data"));
                }
            }
        }
    }

    @Test
    public void testErrorHandling() throws SQLException {
        ProxyConfiguration config = ProxyDriver.getConfiguration("default");
        config.addLogger(new ConsoleJdbcLogger(true, true));

        Properties props = new Properties();
        props.setProperty("user", "sa");
        props.setProperty("password", "");

        try (Connection conn = DriverManager.getConnection(PROXY_URL, props)) {
            try (Statement stmt = conn.createStatement()) {
                // This should fail and be logged
                assertThrows(SQLException.class, () -> {
                    stmt.executeQuery("SELECT * FROM nonexistent_table");
                });
            }
        }
    }
}
