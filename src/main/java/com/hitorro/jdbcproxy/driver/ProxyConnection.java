package com.hitorro.jdbcproxy.driver;

import com.hitorro.jdbcproxy.hooks.ProxyConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * Proxy Connection that wraps an underlying Connection and provides hooks
 * for logging, SQL rewriting, and result processing.
 */
public class ProxyConnection implements Connection {

    private static final Logger logger = LoggerFactory.getLogger(ProxyConnection.class);

    private final Connection underlyingConnection;
    private final ProxyConfiguration config;
    private final String url;

    public ProxyConnection(Connection underlyingConnection, ProxyConfiguration config, String url) {
        this.underlyingConnection = underlyingConnection;
        this.config = config;
        this.url = url;
    }

    @Override
    public Statement createStatement() throws SQLException {
        Statement underlyingStatement = underlyingConnection.createStatement();
        return new ProxyStatement(underlyingStatement, this, config);
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        String rewrittenSql = applySqlRewriting(sql);
        PreparedStatement underlyingStatement = underlyingConnection.prepareStatement(rewrittenSql);
        return new ProxyPreparedStatement(underlyingStatement, sql, rewrittenSql, this, config);
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        String rewrittenSql = applySqlRewriting(sql);
        CallableStatement underlyingStatement = underlyingConnection.prepareCall(rewrittenSql);
        return new ProxyCallableStatement(underlyingStatement, sql, rewrittenSql, this, config);
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        String rewrittenSql = applySqlRewriting(sql);
        return underlyingConnection.nativeSQL(rewrittenSql);
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        underlyingConnection.setAutoCommit(autoCommit);
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        return underlyingConnection.getAutoCommit();
    }

    @Override
    public void commit() throws SQLException {
        long startTime = System.currentTimeMillis();
        SQLException exception = null;
        
        try {
            underlyingConnection.commit();
        } catch (SQLException e) {
            exception = e;
            throw e;
        } finally {
            long endTime = System.currentTimeMillis();
            logTransaction("COMMIT", startTime, endTime, exception);
        }
    }

    @Override
    public void rollback() throws SQLException {
        long startTime = System.currentTimeMillis();
        SQLException exception = null;
        
        try {
            underlyingConnection.rollback();
        } catch (SQLException e) {
            exception = e;
            throw e;
        } finally {
            long endTime = System.currentTimeMillis();
            logTransaction("ROLLBACK", startTime, endTime, exception);
        }
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        long startTime = System.currentTimeMillis();
        SQLException exception = null;
        
        try {
            underlyingConnection.rollback(savepoint);
        } catch (SQLException e) {
            exception = e;
            throw e;
        } finally {
            long endTime = System.currentTimeMillis();
            logTransaction("ROLLBACK_TO_SAVEPOINT", startTime, endTime, exception);
        }
    }

    @Override
    public void close() throws SQLException {
        underlyingConnection.close();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return underlyingConnection.isClosed();
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        return underlyingConnection.getMetaData();
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        underlyingConnection.setReadOnly(readOnly);
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        return underlyingConnection.isReadOnly();
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        underlyingConnection.setCatalog(catalog);
    }

    @Override
    public String getCatalog() throws SQLException {
        return underlyingConnection.getCatalog();
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        underlyingConnection.setTransactionIsolation(level);
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        return underlyingConnection.getTransactionIsolation();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return underlyingConnection.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        underlyingConnection.clearWarnings();
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        Statement underlyingStatement = underlyingConnection.createStatement(resultSetType, resultSetConcurrency);
        return new ProxyStatement(underlyingStatement, this, config);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        String rewrittenSql = applySqlRewriting(sql);
        PreparedStatement underlyingStatement = underlyingConnection.prepareStatement(rewrittenSql, resultSetType, resultSetConcurrency);
        return new ProxyPreparedStatement(underlyingStatement, sql, rewrittenSql, this, config);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        String rewrittenSql = applySqlRewriting(sql);
        CallableStatement underlyingStatement = underlyingConnection.prepareCall(rewrittenSql, resultSetType, resultSetConcurrency);
        return new ProxyCallableStatement(underlyingStatement, sql, rewrittenSql, this, config);
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return underlyingConnection.getTypeMap();
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        underlyingConnection.setTypeMap(map);
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        underlyingConnection.setHoldability(holdability);
    }

    @Override
    public int getHoldability() throws SQLException {
        return underlyingConnection.getHoldability();
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        long startTime = System.currentTimeMillis();
        SQLException exception = null;
        Savepoint savepoint = null;
        
        try {
            savepoint = underlyingConnection.setSavepoint();
            return savepoint;
        } catch (SQLException e) {
            exception = e;
            throw e;
        } finally {
            long endTime = System.currentTimeMillis();
            logTransaction("SET_SAVEPOINT", startTime, endTime, exception);
        }
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        long startTime = System.currentTimeMillis();
        SQLException exception = null;
        Savepoint savepoint = null;
        
        try {
            savepoint = underlyingConnection.setSavepoint(name);
            return savepoint;
        } catch (SQLException e) {
            exception = e;
            throw e;
        } finally {
            long endTime = System.currentTimeMillis();
            logTransaction("SET_SAVEPOINT_" + name, startTime, endTime, exception);
        }
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        long startTime = System.currentTimeMillis();
        SQLException exception = null;
        
        try {
            underlyingConnection.releaseSavepoint(savepoint);
        } catch (SQLException e) {
            exception = e;
            throw e;
        } finally {
            long endTime = System.currentTimeMillis();
            logTransaction("RELEASE_SAVEPOINT", startTime, endTime, exception);
        }
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        Statement underlyingStatement = underlyingConnection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
        return new ProxyStatement(underlyingStatement, this, config);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        String rewrittenSql = applySqlRewriting(sql);
        PreparedStatement underlyingStatement = underlyingConnection.prepareStatement(rewrittenSql, resultSetType, resultSetConcurrency, resultSetHoldability);
        return new ProxyPreparedStatement(underlyingStatement, sql, rewrittenSql, this, config);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        String rewrittenSql = applySqlRewriting(sql);
        CallableStatement underlyingStatement = underlyingConnection.prepareCall(rewrittenSql, resultSetType, resultSetConcurrency, resultSetHoldability);
        return new ProxyCallableStatement(underlyingStatement, sql, rewrittenSql, this, config);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        String rewrittenSql = applySqlRewriting(sql);
        PreparedStatement underlyingStatement = underlyingConnection.prepareStatement(rewrittenSql, autoGeneratedKeys);
        return new ProxyPreparedStatement(underlyingStatement, sql, rewrittenSql, this, config);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        String rewrittenSql = applySqlRewriting(sql);
        PreparedStatement underlyingStatement = underlyingConnection.prepareStatement(rewrittenSql, columnIndexes);
        return new ProxyPreparedStatement(underlyingStatement, sql, rewrittenSql, this, config);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        String rewrittenSql = applySqlRewriting(sql);
        PreparedStatement underlyingStatement = underlyingConnection.prepareStatement(rewrittenSql, columnNames);
        return new ProxyPreparedStatement(underlyingStatement, sql, rewrittenSql, this, config);
    }

    @Override
    public Clob createClob() throws SQLException {
        return underlyingConnection.createClob();
    }

    @Override
    public Blob createBlob() throws SQLException {
        return underlyingConnection.createBlob();
    }

    @Override
    public NClob createNClob() throws SQLException {
        return underlyingConnection.createNClob();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        return underlyingConnection.createSQLXML();
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        return underlyingConnection.isValid(timeout);
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        underlyingConnection.setClientInfo(name, value);
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        underlyingConnection.setClientInfo(properties);
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        return underlyingConnection.getClientInfo(name);
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        return underlyingConnection.getClientInfo();
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        return underlyingConnection.createArrayOf(typeName, elements);
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        return underlyingConnection.createStruct(typeName, attributes);
    }

    @Override
    public void setSchema(String schema) throws SQLException {
        underlyingConnection.setSchema(schema);
    }

    @Override
    public String getSchema() throws SQLException {
        return underlyingConnection.getSchema();
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        underlyingConnection.abort(executor);
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        underlyingConnection.setNetworkTimeout(executor, milliseconds);
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        return underlyingConnection.getNetworkTimeout();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isAssignableFrom(getClass())) {
            return iface.cast(this);
        }
        return underlyingConnection.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isAssignableFrom(getClass()) || underlyingConnection.isWrapperFor(iface);
    }

    /**
     * Apply SQL rewriting hooks to the given SQL statement.
     *
     * @param sql the original SQL statement
     * @return the rewritten SQL statement
     */
    protected String applySqlRewriting(String sql) {
        if (!config.isEnabled() || config.getRewriters().isEmpty()) {
            return sql;
        }

        String rewrittenSql = sql;
        for (var rewriter : config.getRewriters()) {
            try {
                rewrittenSql = rewriter.rewriteSql(rewrittenSql, this);
            } catch (Exception e) {
                logger.warn("Error in SQL rewriter", e);
            }
        }
        return rewrittenSql;
    }

    /**
     * Log a transaction operation.
     */
    private void logTransaction(String operation, long startTime, long endTime, SQLException exception) {
        if (!config.isEnabled() || config.getLoggers().isEmpty()) {
            return;
        }

        config.getLoggers().forEach(jdbcLogger -> {
            try {
                jdbcLogger.logTransaction(operation, this, startTime, endTime, exception);
            } catch (Exception e) {
                logger.warn("Error in JDBC logger while logging transaction", e);
            }
        });
    }

    /**
     * Get the underlying connection (for internal use by proxy classes).
     *
     * @return the underlying connection
     */
    protected Connection getUnderlyingConnection() {
        return underlyingConnection;
    }

    /**
     * Get the configuration (for internal use by proxy classes).
     *
     * @return the proxy configuration
     */
    protected ProxyConfiguration getConfiguration() {
        return config;
    }

    /**
     * Get the original URL (for internal use by proxy classes).
     *
     * @return the database URL
     */
    protected String getUrl() {
        return url;
    }
}
