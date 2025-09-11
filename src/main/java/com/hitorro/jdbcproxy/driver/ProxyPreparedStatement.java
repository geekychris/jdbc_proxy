package com.hitorro.jdbcproxy.driver;

import com.hitorro.jdbcproxy.hooks.ProxyConfiguration;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.sql.Date;
import java.util.*;

/**
 * Proxy PreparedStatement implementation with parameter tracking.
 */
public class ProxyPreparedStatement extends BaseProxyStatement<PreparedStatement> implements PreparedStatement {

    private final String originalSql;
    private final String rewrittenSql;
    private final Map<Integer, Object> parameters = new HashMap<>();

    public ProxyPreparedStatement(PreparedStatement underlyingStatement, String originalSql, String rewrittenSql, 
                                 ProxyConnection proxyConnection, ProxyConfiguration config) {
        super(underlyingStatement, proxyConnection, config);
        this.originalSql = originalSql;
        this.rewrittenSql = rewrittenSql;
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        return executeWithParameterLogging(() -> {
            ResultSet underlyingResultSet = underlyingStatement.executeQuery();
            return new ProxyResultSet(underlyingResultSet, originalSql, proxyConnection, config);
        });
    }

    @Override
    public int executeUpdate() throws SQLException {
        return executeWithParameterLogging(() -> underlyingStatement.executeUpdate());
    }

    @Override
    public boolean execute() throws SQLException {
        return executeWithParameterLogging(() -> underlyingStatement.execute());
    }

    // Parameter setters with tracking
    @Override
    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        parameters.put(parameterIndex, null);
        underlyingStatement.setNull(parameterIndex, sqlType);
    }

    @Override
    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        parameters.put(parameterIndex, x);
        underlyingStatement.setBoolean(parameterIndex, x);
    }

    @Override
    public void setByte(int parameterIndex, byte x) throws SQLException {
        parameters.put(parameterIndex, x);
        underlyingStatement.setByte(parameterIndex, x);
    }

    @Override
    public void setShort(int parameterIndex, short x) throws SQLException {
        parameters.put(parameterIndex, x);
        underlyingStatement.setShort(parameterIndex, x);
    }

    @Override
    public void setInt(int parameterIndex, int x) throws SQLException {
        parameters.put(parameterIndex, x);
        underlyingStatement.setInt(parameterIndex, x);
    }

    @Override
    public void setLong(int parameterIndex, long x) throws SQLException {
        parameters.put(parameterIndex, x);
        underlyingStatement.setLong(parameterIndex, x);
    }

    @Override
    public void setFloat(int parameterIndex, float x) throws SQLException {
        parameters.put(parameterIndex, x);
        underlyingStatement.setFloat(parameterIndex, x);
    }

    @Override
    public void setDouble(int parameterIndex, double x) throws SQLException {
        parameters.put(parameterIndex, x);
        underlyingStatement.setDouble(parameterIndex, x);
    }

    @Override
    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
        parameters.put(parameterIndex, x);
        underlyingStatement.setBigDecimal(parameterIndex, x);
    }

    @Override
    public void setString(int parameterIndex, String x) throws SQLException {
        parameters.put(parameterIndex, x);
        underlyingStatement.setString(parameterIndex, x);
    }

    @Override
    public void setBytes(int parameterIndex, byte[] x) throws SQLException {
        parameters.put(parameterIndex, x != null ? Arrays.toString(x) : null);
        underlyingStatement.setBytes(parameterIndex, x);
    }

    @Override
    public void setDate(int parameterIndex, Date x) throws SQLException {
        parameters.put(parameterIndex, x);
        underlyingStatement.setDate(parameterIndex, x);
    }

    @Override
    public void setTime(int parameterIndex, Time x) throws SQLException {
        parameters.put(parameterIndex, x);
        underlyingStatement.setTime(parameterIndex, x);
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
        parameters.put(parameterIndex, x);
        underlyingStatement.setTimestamp(parameterIndex, x);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
        parameters.put(parameterIndex, "<AsciiStream>");
        underlyingStatement.setAsciiStream(parameterIndex, x, length);
    }

    @Override
    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
        parameters.put(parameterIndex, "<UnicodeStream>");
        underlyingStatement.setUnicodeStream(parameterIndex, x, length);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
        parameters.put(parameterIndex, "<BinaryStream>");
        underlyingStatement.setBinaryStream(parameterIndex, x, length);
    }

    @Override
    public void clearParameters() throws SQLException {
        parameters.clear();
        underlyingStatement.clearParameters();
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
        parameters.put(parameterIndex, x);
        underlyingStatement.setObject(parameterIndex, x, targetSqlType);
    }

    @Override
    public void setObject(int parameterIndex, Object x) throws SQLException {
        parameters.put(parameterIndex, x);
        underlyingStatement.setObject(parameterIndex, x);
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
        parameters.put(parameterIndex, "<CharacterStream>");
        underlyingStatement.setCharacterStream(parameterIndex, reader, length);
    }

    @Override
    public void setRef(int parameterIndex, Ref x) throws SQLException {
        parameters.put(parameterIndex, x);
        underlyingStatement.setRef(parameterIndex, x);
    }

    @Override
    public void setBlob(int parameterIndex, Blob x) throws SQLException {
        parameters.put(parameterIndex, "<Blob>");
        underlyingStatement.setBlob(parameterIndex, x);
    }

    @Override
    public void setClob(int parameterIndex, Clob x) throws SQLException {
        parameters.put(parameterIndex, "<Clob>");
        underlyingStatement.setClob(parameterIndex, x);
    }

    @Override
    public void setArray(int parameterIndex, Array x) throws SQLException {
        parameters.put(parameterIndex, x);
        underlyingStatement.setArray(parameterIndex, x);
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return underlyingStatement.getMetaData();
    }

    @Override
    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
        parameters.put(parameterIndex, x);
        underlyingStatement.setDate(parameterIndex, x, cal);
    }

    @Override
    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
        parameters.put(parameterIndex, x);
        underlyingStatement.setTime(parameterIndex, x, cal);
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
        parameters.put(parameterIndex, x);
        underlyingStatement.setTimestamp(parameterIndex, x, cal);
    }

    @Override
    public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
        parameters.put(parameterIndex, null);
        underlyingStatement.setNull(parameterIndex, sqlType, typeName);
    }

    @Override
    public void setURL(int parameterIndex, URL x) throws SQLException {
        parameters.put(parameterIndex, x);
        underlyingStatement.setURL(parameterIndex, x);
    }

    @Override
    public ParameterMetaData getParameterMetaData() throws SQLException {
        return underlyingStatement.getParameterMetaData();
    }

    @Override
    public void setRowId(int parameterIndex, RowId x) throws SQLException {
        parameters.put(parameterIndex, x);
        underlyingStatement.setRowId(parameterIndex, x);
    }

    @Override
    public void setNString(int parameterIndex, String value) throws SQLException {
        parameters.put(parameterIndex, value);
        underlyingStatement.setNString(parameterIndex, value);
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
        parameters.put(parameterIndex, "<NCharacterStream>");
        underlyingStatement.setNCharacterStream(parameterIndex, value, length);
    }

    @Override
    public void setNClob(int parameterIndex, NClob value) throws SQLException {
        parameters.put(parameterIndex, "<NClob>");
        underlyingStatement.setNClob(parameterIndex, value);
    }

    @Override
    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
        parameters.put(parameterIndex, "<Clob>");
        underlyingStatement.setClob(parameterIndex, reader, length);
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
        parameters.put(parameterIndex, "<Blob>");
        underlyingStatement.setBlob(parameterIndex, inputStream, length);
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
        parameters.put(parameterIndex, "<NClob>");
        underlyingStatement.setNClob(parameterIndex, reader, length);
    }

    @Override
    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
        parameters.put(parameterIndex, "<SQLXML>");
        underlyingStatement.setSQLXML(parameterIndex, xmlObject);
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {
        parameters.put(parameterIndex, x);
        underlyingStatement.setObject(parameterIndex, x, targetSqlType, scaleOrLength);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
        parameters.put(parameterIndex, "<AsciiStream>");
        underlyingStatement.setAsciiStream(parameterIndex, x, length);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
        parameters.put(parameterIndex, "<BinaryStream>");
        underlyingStatement.setBinaryStream(parameterIndex, x, length);
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
        parameters.put(parameterIndex, "<CharacterStream>");
        underlyingStatement.setCharacterStream(parameterIndex, reader, length);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
        parameters.put(parameterIndex, "<AsciiStream>");
        underlyingStatement.setAsciiStream(parameterIndex, x);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
        parameters.put(parameterIndex, "<BinaryStream>");
        underlyingStatement.setBinaryStream(parameterIndex, x);
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
        parameters.put(parameterIndex, "<CharacterStream>");
        underlyingStatement.setCharacterStream(parameterIndex, reader);
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
        parameters.put(parameterIndex, "<NCharacterStream>");
        underlyingStatement.setNCharacterStream(parameterIndex, value);
    }

    @Override
    public void setClob(int parameterIndex, Reader reader) throws SQLException {
        parameters.put(parameterIndex, "<Clob>");
        underlyingStatement.setClob(parameterIndex, reader);
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
        parameters.put(parameterIndex, "<Blob>");
        underlyingStatement.setBlob(parameterIndex, inputStream);
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader) throws SQLException {
        parameters.put(parameterIndex, "<NClob>");
        underlyingStatement.setNClob(parameterIndex, reader);
    }

    @Override
    public void addBatch() throws SQLException {
        underlyingStatement.addBatch();
    }

    @Override
    protected String getLastExecutedSql() {
        return originalSql;
    }

    /**
     * Execute with parameter logging support.
     */
    private <R> R executeWithParameterLogging(SqlOperation<R> operation) throws SQLException {
        if (!config.isEnabled() || config.getLoggers().isEmpty()) {
            return operation.execute();
        }

        long startTime = System.currentTimeMillis();
        SQLException exception = null;
        R result = null;
        
        try {
            result = operation.execute();
            return result;
        } catch (SQLException e) {
            exception = e;
            throw e;
        } finally {
            long endTime = System.currentTimeMillis();
            
            int rowCount = -1;
            if (result instanceof Integer) {
                rowCount = (Integer) result;
            }
            
            // Convert parameters to list for logging
            List<Object> parameterList = new ArrayList<>();
            for (int i = 1; i <= parameters.size(); i++) {
                parameterList.add(parameters.get(i));
            }
            
            logStatement(originalSql, rewrittenSql, parameterList, startTime, endTime, rowCount, exception);
        }
    }
}
