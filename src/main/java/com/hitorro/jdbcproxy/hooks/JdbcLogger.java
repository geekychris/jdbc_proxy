package com.hitorro.jdbcproxy.hooks;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Interface for logging JDBC operations.
 * Implementations can log to various destinations such as:
 * - Standard logging frameworks (SLF4J, Log4j, etc.)
 * - Files
 * - Databases
 * - External monitoring systems
 */
public interface JdbcLogger {

    /**
     * Log a connection being established.
     *
     * @param url the database URL
     * @param user the username (may be null)
     * @param startTime the time when connection establishment started
     * @param endTime the time when connection establishment completed
     * @param exception any exception that occurred (null if successful)
     */
    default void logConnection(String url, String user, long startTime, long endTime, SQLException exception) {
        // Default implementation does nothing
    }

    /**
     * Log a SQL statement execution.
     *
     * @param originalSql the original SQL statement
     * @param rewrittenSql the SQL statement after rewriting (may be same as original)
     * @param parameters the parameters bound to the statement (may be null or empty)
     * @param connection the connection used
     * @param startTime the time when execution started
     * @param endTime the time when execution completed
     * @param rowCount the number of rows affected/returned (-1 if unknown)
     * @param exception any exception that occurred (null if successful)
     */
    default void logStatement(String originalSql, String rewrittenSql, List<Object> parameters, 
                            Connection connection, long startTime, long endTime, 
                            int rowCount, SQLException exception) {
        // Default implementation does nothing
    }

    /**
     * Log a batch execution.
     *
     * @param sql the SQL statement
     * @param batchSize the number of statements in the batch
     * @param connection the connection used
     * @param startTime the time when execution started
     * @param endTime the time when execution completed
     * @param updateCounts the update counts for each statement in the batch
     * @param exception any exception that occurred (null if successful)
     */
    default void logBatch(String sql, int batchSize, Connection connection, 
                        long startTime, long endTime, int[] updateCounts, SQLException exception) {
        // Default implementation does nothing
    }

    /**
     * Log a transaction operation (commit, rollback, etc.).
     *
     * @param operation the operation performed (COMMIT, ROLLBACK, SAVEPOINT, etc.)
     * @param connection the connection used
     * @param startTime the time when operation started
     * @param endTime the time when operation completed
     * @param exception any exception that occurred (null if successful)
     */
    default void logTransaction(String operation, Connection connection, 
                              long startTime, long endTime, SQLException exception) {
        // Default implementation does nothing
    }

    /**
     * Log ResultSet access patterns (optional, for detailed debugging).
     *
     * @param sql the SQL statement that produced the ResultSet
     * @param connection the connection used
     * @param rowsRead the number of rows read
     * @param columnsAccessed the column names/indices accessed
     */
    default void logResultSetAccess(String sql, Connection connection, int rowsRead, List<String> columnsAccessed) {
        // Default implementation does nothing
    }

    /**
     * Returns the priority of this logger. Higher priority loggers are called first.
     * Default priority is 0.
     *
     * @return the priority value
     */
    default int getPriority() {
        return 0;
    }
}
