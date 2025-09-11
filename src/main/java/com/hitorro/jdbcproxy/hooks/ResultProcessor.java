package com.hitorro.jdbcproxy.hooks;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Interface for processing ResultSet data after query execution.
 * Implementations can:
 * - Transform result data
 * - Filter rows
 * - Add computed columns
 * - Log result data
 * - Cache results
 */
public interface ResultProcessor {

    /**
     * Process a row in the ResultSet. This method is called for each row
     * as it's being read from the ResultSet.
     *
     * @param resultSet the ResultSet being processed
     * @param sql the SQL statement that produced this ResultSet
     * @param connection the connection used to execute the statement
     * @throws SQLException if processing fails
     */
    default void processRow(ResultSet resultSet, String sql, Connection connection) throws SQLException {
        // Default implementation does nothing
    }

    /**
     * Called before the first row is processed.
     *
     * @param resultSet the ResultSet about to be processed
     * @param sql the SQL statement that produced this ResultSet
     * @param connection the connection used to execute the statement
     * @throws SQLException if initialization fails
     */
    default void onResultSetStart(ResultSet resultSet, String sql, Connection connection) throws SQLException {
        // Default implementation does nothing
    }

    /**
     * Called after the last row is processed or when the ResultSet is closed.
     *
     * @param resultSet the ResultSet that was processed
     * @param sql the SQL statement that produced this ResultSet
     * @param connection the connection used to execute the statement
     * @param rowCount the total number of rows processed
     * @throws SQLException if cleanup fails
     */
    default void onResultSetEnd(ResultSet resultSet, String sql, Connection connection, int rowCount) throws SQLException {
        // Default implementation does nothing
    }

    /**
     * Transform a column value before it's returned to the caller.
     *
     * @param columnIndex the 1-based column index
     * @param columnName the column name
     * @param value the original value
     * @param resultSet the ResultSet containing the value
     * @param sql the SQL statement that produced this ResultSet
     * @param connection the connection used to execute the statement
     * @return the transformed value, or the original if no transformation needed
     * @throws SQLException if transformation fails
     */
    default Object transformColumnValue(int columnIndex, String columnName, Object value, 
                                      ResultSet resultSet, String sql, Connection connection) throws SQLException {
        return value;
    }

    /**
     * Returns the priority of this processor. Higher priority processors are applied first.
     * Default priority is 0.
     *
     * @return the priority value
     */
    default int getPriority() {
        return 0;
    }
}
