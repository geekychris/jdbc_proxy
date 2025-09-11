package com.hitorro.jdbcproxy.extensions;

import com.hitorro.jdbcproxy.hooks.JdbcLogger;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Console logger implementation that outputs JDBC operations to System.out.
 * Useful for debugging and development.
 */
public class ConsoleJdbcLogger implements JdbcLogger {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private final boolean logParameters;
    private final boolean logTiming;

    public ConsoleJdbcLogger() {
        this(true, true);
    }

    public ConsoleJdbcLogger(boolean logParameters, boolean logTiming) {
        this.logParameters = logParameters;
        this.logTiming = logTiming;
    }

    @Override
    public void logConnection(String url, String user, long startTime, long endTime, SQLException exception) {
        String timestamp = formatTime(startTime);
        String duration = logTiming ? String.format(" [%dms]", endTime - startTime) : "";
        
        if (exception == null) {
            System.out.printf("[%s] CONNECTION%s: Connected to %s (user: %s)%n", 
                             timestamp, duration, url, user != null ? user : "none");
        } else {
            System.out.printf("[%s] CONNECTION%s: Failed to connect to %s (user: %s) - %s%n", 
                             timestamp, duration, url, user != null ? user : "none", exception.getMessage());
        }
    }

    @Override
    public void logStatement(String originalSql, String rewrittenSql, List<Object> parameters, 
                           Connection connection, long startTime, long endTime, 
                           int rowCount, SQLException exception) {
        String timestamp = formatTime(startTime);
        String duration = logTiming ? String.format(" [%dms]", endTime - startTime) : "";
        
        // Use rewritten SQL if different from original
        String sqlToLog = originalSql.equals(rewrittenSql) ? originalSql : 
                         originalSql + " -> " + rewrittenSql;
        
        if (exception == null) {
            System.out.printf("[%s] STATEMENT%s: %s", timestamp, duration, sqlToLog);
            
            if (logParameters && parameters != null && !parameters.isEmpty()) {
                System.out.print(" | Parameters: " + formatParameters(parameters));
            }
            
            if (rowCount >= 0) {
                System.out.print(" | Rows: " + rowCount);
            }
            
            System.out.println();
        } else {
            System.out.printf("[%s] STATEMENT%s: %s | ERROR: %s%n", 
                             timestamp, duration, sqlToLog, exception.getMessage());
        }
    }

    @Override
    public void logBatch(String sql, int batchSize, Connection connection, 
                        long startTime, long endTime, int[] updateCounts, SQLException exception) {
        String timestamp = formatTime(startTime);
        String duration = logTiming ? String.format(" [%dms]", endTime - startTime) : "";
        
        if (exception == null) {
            int totalUpdates = updateCounts != null ? java.util.Arrays.stream(updateCounts).sum() : 0;
            System.out.printf("[%s] BATCH%s: %s | Batch size: %d | Total updates: %d%n", 
                             timestamp, duration, sql, batchSize, totalUpdates);
        } else {
            System.out.printf("[%s] BATCH%s: %s | Batch size: %d | ERROR: %s%n", 
                             timestamp, duration, sql, batchSize, exception.getMessage());
        }
    }

    @Override
    public void logTransaction(String operation, Connection connection, 
                             long startTime, long endTime, SQLException exception) {
        String timestamp = formatTime(startTime);
        String duration = logTiming ? String.format(" [%dms]", endTime - startTime) : "";
        
        if (exception == null) {
            System.out.printf("[%s] TRANSACTION%s: %s%n", timestamp, duration, operation);
        } else {
            System.out.printf("[%s] TRANSACTION%s: %s | ERROR: %s%n", 
                             timestamp, duration, operation, exception.getMessage());
        }
    }

    @Override
    public void logResultSetAccess(String sql, Connection connection, int rowsRead, List<String> columnsAccessed) {
        String timestamp = formatTime(System.currentTimeMillis());
        System.out.printf("[%s] RESULTSET: %s | Rows read: %d | Columns accessed: %s%n", 
                         timestamp, sql, rowsRead, columnsAccessed);
    }

    @Override
    public int getPriority() {
        return 0;
    }

    private String formatTime(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
                           .format(TIME_FORMATTER);
    }

    private String formatParameters(List<Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return "[]";
        }
        
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) sb.append(", ");
            
            Object param = parameters.get(i);
            if (param == null) {
                sb.append("null");
            } else if (param instanceof String) {
                String str = (String) param;
                // Truncate long strings
                if (str.length() > 50) {
                    sb.append("\"").append(str.substring(0, 47)).append("...\"");
                } else {
                    sb.append("\"").append(str).append("\"");
                }
            } else {
                sb.append(param.toString());
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
