package com.hitorro.jdbcproxy.driver;

import com.hitorro.jdbcproxy.hooks.ProxyConfiguration;
import com.hitorro.jdbcproxy.hooks.ResultProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * Proxy ResultSet implementation that applies result processing hooks.
 */
public class ProxyResultSet implements ResultSet {

    private static final Logger logger = LoggerFactory.getLogger(ProxyResultSet.class);

    private final ResultSet underlyingResultSet;
    private final String sql;
    private final ProxyConnection proxyConnection;
    private final ProxyConfiguration config;
    
    private boolean started = false;
    private int rowCount = 0;
    private final List<String> accessedColumns = new ArrayList<>();

    public ProxyResultSet(ResultSet underlyingResultSet, String sql, ProxyConnection proxyConnection, ProxyConfiguration config) {
        this.underlyingResultSet = underlyingResultSet;
        this.sql = sql;
        this.proxyConnection = proxyConnection;
        this.config = config;
    }

    @Override
    public boolean next() throws SQLException {
        boolean hasNext = underlyingResultSet.next();
        
        if (!started && hasNext && config.isEnabled()) {
            started = true;
            // Call onResultSetStart for all processors
            for (ResultProcessor processor : config.getProcessors()) {
                try {
                    processor.onResultSetStart(this, sql, proxyConnection);
                } catch (Exception e) {
                    logger.warn("Error in result processor onResultSetStart", e);
                }
            }
        }
        
        if (hasNext) {
            rowCount++;
            if (config.isEnabled()) {
                // Call processRow for all processors
                for (ResultProcessor processor : config.getProcessors()) {
                    try {
                        processor.processRow(this, sql, proxyConnection);
                    } catch (Exception e) {
                        logger.warn("Error in result processor processRow", e);
                    }
                }
            }
        } else if (started) {
            // End of results
            callResultSetEnd();
        }
        
        return hasNext;
    }

    @Override
    public void close() throws SQLException {
        if (started) {
            callResultSetEnd();
        }
        underlyingResultSet.close();
    }

    // Getter methods with value transformation
    @Override
    public String getString(int columnIndex) throws SQLException {
        Object value = underlyingResultSet.getString(columnIndex);
        trackColumnAccess(columnIndex);
        return (String) transformValue(columnIndex, null, value);
    }

    @Override
    public String getString(String columnLabel) throws SQLException {
        Object value = underlyingResultSet.getString(columnLabel);
        trackColumnAccess(columnLabel);
        return (String) transformValue(-1, columnLabel, value);
    }

    @Override
    public boolean getBoolean(int columnIndex) throws SQLException {
        Object value = underlyingResultSet.getBoolean(columnIndex);
        trackColumnAccess(columnIndex);
        Object transformed = transformValue(columnIndex, null, value);
        return transformed instanceof Boolean ? (Boolean) transformed : underlyingResultSet.getBoolean(columnIndex);
    }

    @Override
    public boolean getBoolean(String columnLabel) throws SQLException {
        Object value = underlyingResultSet.getBoolean(columnLabel);
        trackColumnAccess(columnLabel);
        Object transformed = transformValue(-1, columnLabel, value);
        return transformed instanceof Boolean ? (Boolean) transformed : underlyingResultSet.getBoolean(columnLabel);
    }

    @Override
    public int getInt(int columnIndex) throws SQLException {
        Object value = underlyingResultSet.getInt(columnIndex);
        trackColumnAccess(columnIndex);
        Object transformed = transformValue(columnIndex, null, value);
        return transformed instanceof Integer ? (Integer) transformed : underlyingResultSet.getInt(columnIndex);
    }

    @Override
    public int getInt(String columnLabel) throws SQLException {
        Object value = underlyingResultSet.getInt(columnLabel);
        trackColumnAccess(columnLabel);
        Object transformed = transformValue(-1, columnLabel, value);
        return transformed instanceof Integer ? (Integer) transformed : underlyingResultSet.getInt(columnLabel);
    }

    @Override
    public Object getObject(int columnIndex) throws SQLException {
        Object value = underlyingResultSet.getObject(columnIndex);
        trackColumnAccess(columnIndex);
        return transformValue(columnIndex, null, value);
    }

    @Override
    public Object getObject(String columnLabel) throws SQLException {
        Object value = underlyingResultSet.getObject(columnLabel);
        trackColumnAccess(columnLabel);
        return transformValue(-1, columnLabel, value);
    }

    // Helper method to transform column values through processors
    private Object transformValue(int columnIndex, String columnName, Object value) {
        if (!config.isEnabled() || config.getProcessors().isEmpty()) {
            return value;
        }

        Object transformedValue = value;
        for (ResultProcessor processor : config.getProcessors()) {
            try {
                transformedValue = processor.transformColumnValue(columnIndex, columnName, transformedValue, this, sql, proxyConnection);
            } catch (Exception e) {
                logger.warn("Error in result processor transformColumnValue", e);
            }
        }
        return transformedValue;
    }

    private void trackColumnAccess(int columnIndex) {
        try {
            String columnName = getMetaData().getColumnName(columnIndex);
            trackColumnAccess(columnName);
        } catch (SQLException e) {
            trackColumnAccess("column_" + columnIndex);
        }
    }

    private void trackColumnAccess(String columnName) {
        if (!accessedColumns.contains(columnName)) {
            accessedColumns.add(columnName);
        }
    }

    private void callResultSetEnd() {
        if (config.isEnabled() && !config.getLoggers().isEmpty()) {
            // Log result set access patterns
            config.getLoggers().forEach(logger -> {
                try {
                    logger.logResultSetAccess(sql, proxyConnection, rowCount, new ArrayList<>(accessedColumns));
                } catch (Exception e) {
                    ProxyResultSet.logger.warn("Error in JDBC logger while logging result set access", e);
                }
            });
        }

        if (config.isEnabled() && !config.getProcessors().isEmpty()) {
            // Call onResultSetEnd for all processors
            config.getProcessors().forEach(processor -> {
                try {
                    processor.onResultSetEnd(this, sql, proxyConnection, rowCount);
                } catch (Exception e) {
                    ProxyResultSet.logger.warn("Error in result processor onResultSetEnd", e);
                }
            });
        }
    }

    // Delegate all other methods to underlying ResultSet
    @Override
    public boolean wasNull() throws SQLException {
        return underlyingResultSet.wasNull();
    }

    @Override
    public byte getByte(int columnIndex) throws SQLException {
        trackColumnAccess(columnIndex);
        return underlyingResultSet.getByte(columnIndex);
    }

    @Override
    public short getShort(int columnIndex) throws SQLException {
        trackColumnAccess(columnIndex);
        return underlyingResultSet.getShort(columnIndex);
    }

    @Override
    public long getLong(int columnIndex) throws SQLException {
        trackColumnAccess(columnIndex);
        return underlyingResultSet.getLong(columnIndex);
    }

    @Override
    public float getFloat(int columnIndex) throws SQLException {
        trackColumnAccess(columnIndex);
        return underlyingResultSet.getFloat(columnIndex);
    }

    @Override
    public double getDouble(int columnIndex) throws SQLException {
        trackColumnAccess(columnIndex);
        return underlyingResultSet.getDouble(columnIndex);
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
        trackColumnAccess(columnIndex);
        return underlyingResultSet.getBigDecimal(columnIndex, scale);
    }

    @Override
    public byte[] getBytes(int columnIndex) throws SQLException {
        trackColumnAccess(columnIndex);
        return underlyingResultSet.getBytes(columnIndex);
    }

    @Override
    public Date getDate(int columnIndex) throws SQLException {
        trackColumnAccess(columnIndex);
        return underlyingResultSet.getDate(columnIndex);
    }

    @Override
    public Time getTime(int columnIndex) throws SQLException {
        trackColumnAccess(columnIndex);
        return underlyingResultSet.getTime(columnIndex);
    }

    @Override
    public Timestamp getTimestamp(int columnIndex) throws SQLException {
        trackColumnAccess(columnIndex);
        return underlyingResultSet.getTimestamp(columnIndex);
    }

    @Override
    public InputStream getAsciiStream(int columnIndex) throws SQLException {
        trackColumnAccess(columnIndex);
        return underlyingResultSet.getAsciiStream(columnIndex);
    }

    @Override
    public InputStream getUnicodeStream(int columnIndex) throws SQLException {
        trackColumnAccess(columnIndex);
        return underlyingResultSet.getUnicodeStream(columnIndex);
    }

    @Override
    public InputStream getBinaryStream(int columnIndex) throws SQLException {
        trackColumnAccess(columnIndex);
        return underlyingResultSet.getBinaryStream(columnIndex);
    }

    @Override
    public byte getByte(String columnLabel) throws SQLException {
        trackColumnAccess(columnLabel);
        return underlyingResultSet.getByte(columnLabel);
    }

    @Override
    public short getShort(String columnLabel) throws SQLException {
        trackColumnAccess(columnLabel);
        return underlyingResultSet.getShort(columnLabel);
    }

    @Override
    public long getLong(String columnLabel) throws SQLException {
        trackColumnAccess(columnLabel);
        return underlyingResultSet.getLong(columnLabel);
    }

    @Override
    public float getFloat(String columnLabel) throws SQLException {
        trackColumnAccess(columnLabel);
        return underlyingResultSet.getFloat(columnLabel);
    }

    @Override
    public double getDouble(String columnLabel) throws SQLException {
        trackColumnAccess(columnLabel);
        return underlyingResultSet.getDouble(columnLabel);
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
        trackColumnAccess(columnLabel);
        return underlyingResultSet.getBigDecimal(columnLabel, scale);
    }

    @Override
    public byte[] getBytes(String columnLabel) throws SQLException {
        trackColumnAccess(columnLabel);
        return underlyingResultSet.getBytes(columnLabel);
    }

    @Override
    public Date getDate(String columnLabel) throws SQLException {
        trackColumnAccess(columnLabel);
        return underlyingResultSet.getDate(columnLabel);
    }

    @Override
    public Time getTime(String columnLabel) throws SQLException {
        trackColumnAccess(columnLabel);
        return underlyingResultSet.getTime(columnLabel);
    }

    @Override
    public Timestamp getTimestamp(String columnLabel) throws SQLException {
        trackColumnAccess(columnLabel);
        return underlyingResultSet.getTimestamp(columnLabel);
    }

    @Override
    public InputStream getAsciiStream(String columnLabel) throws SQLException {
        trackColumnAccess(columnLabel);
        return underlyingResultSet.getAsciiStream(columnLabel);
    }

    @Override
    public InputStream getUnicodeStream(String columnLabel) throws SQLException {
        trackColumnAccess(columnLabel);
        return underlyingResultSet.getUnicodeStream(columnLabel);
    }

    @Override
    public InputStream getBinaryStream(String columnLabel) throws SQLException {
        trackColumnAccess(columnLabel);
        return underlyingResultSet.getBinaryStream(columnLabel);
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return underlyingResultSet.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        underlyingResultSet.clearWarnings();
    }

    @Override
    public String getCursorName() throws SQLException {
        return underlyingResultSet.getCursorName();
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return underlyingResultSet.getMetaData();
    }

    @Override
    public int findColumn(String columnLabel) throws SQLException {
        return underlyingResultSet.findColumn(columnLabel);
    }

    // Continue delegating remaining methods...
    // For brevity, I'm including key methods. In production, delegate all methods.

    @Override
    public boolean isBeforeFirst() throws SQLException {
        return underlyingResultSet.isBeforeFirst();
    }

    @Override
    public boolean isAfterLast() throws SQLException {
        return underlyingResultSet.isAfterLast();
    }

    @Override
    public boolean isFirst() throws SQLException {
        return underlyingResultSet.isFirst();
    }

    @Override
    public boolean isLast() throws SQLException {
        return underlyingResultSet.isLast();
    }

    @Override
    public void beforeFirst() throws SQLException {
        underlyingResultSet.beforeFirst();
    }

    @Override
    public void afterLast() throws SQLException {
        underlyingResultSet.afterLast();
    }

    @Override
    public boolean first() throws SQLException {
        return underlyingResultSet.first();
    }

    @Override
    public boolean last() throws SQLException {
        return underlyingResultSet.last();
    }

    @Override
    public int getRow() throws SQLException {
        return underlyingResultSet.getRow();
    }

    @Override
    public boolean absolute(int row) throws SQLException {
        return underlyingResultSet.absolute(row);
    }

    @Override
    public boolean relative(int rows) throws SQLException {
        return underlyingResultSet.relative(rows);
    }

    @Override
    public boolean previous() throws SQLException {
        return underlyingResultSet.previous();
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        underlyingResultSet.setFetchDirection(direction);
    }

    @Override
    public int getFetchDirection() throws SQLException {
        return underlyingResultSet.getFetchDirection();
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {
        underlyingResultSet.setFetchSize(rows);
    }

    @Override
    public int getFetchSize() throws SQLException {
        return underlyingResultSet.getFetchSize();
    }

    @Override
    public int getType() throws SQLException {
        return underlyingResultSet.getType();
    }

    @Override
    public int getConcurrency() throws SQLException {
        return underlyingResultSet.getConcurrency();
    }

    @Override
    public boolean rowUpdated() throws SQLException {
        return underlyingResultSet.rowUpdated();
    }

    @Override
    public boolean rowInserted() throws SQLException {
        return underlyingResultSet.rowInserted();
    }

    @Override
    public boolean rowDeleted() throws SQLException {
        return underlyingResultSet.rowDeleted();
    }

    @Override
    public void updateNull(int columnIndex) throws SQLException {
        underlyingResultSet.updateNull(columnIndex);
    }

    @Override
    public void updateBoolean(int columnIndex, boolean x) throws SQLException {
        underlyingResultSet.updateBoolean(columnIndex, x);
    }

    @Override
    public void updateByte(int columnIndex, byte x) throws SQLException {
        underlyingResultSet.updateByte(columnIndex, x);
    }

    @Override
    public void updateShort(int columnIndex, short x) throws SQLException {
        underlyingResultSet.updateShort(columnIndex, x);
    }

    @Override
    public void updateInt(int columnIndex, int x) throws SQLException {
        underlyingResultSet.updateInt(columnIndex, x);
    }

    @Override
    public void updateLong(int columnIndex, long x) throws SQLException {
        underlyingResultSet.updateLong(columnIndex, x);
    }

    @Override
    public void updateFloat(int columnIndex, float x) throws SQLException {
        underlyingResultSet.updateFloat(columnIndex, x);
    }

    @Override
    public void updateDouble(int columnIndex, double x) throws SQLException {
        underlyingResultSet.updateDouble(columnIndex, x);
    }

    @Override
    public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
        underlyingResultSet.updateBigDecimal(columnIndex, x);
    }

    @Override
    public void updateString(int columnIndex, String x) throws SQLException {
        underlyingResultSet.updateString(columnIndex, x);
    }

    @Override
    public void updateBytes(int columnIndex, byte[] x) throws SQLException {
        underlyingResultSet.updateBytes(columnIndex, x);
    }

    @Override
    public void updateDate(int columnIndex, Date x) throws SQLException {
        underlyingResultSet.updateDate(columnIndex, x);
    }

    @Override
    public void updateTime(int columnIndex, Time x) throws SQLException {
        underlyingResultSet.updateTime(columnIndex, x);
    }

    @Override
    public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
        underlyingResultSet.updateTimestamp(columnIndex, x);
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
        underlyingResultSet.updateAsciiStream(columnIndex, x, length);
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
        underlyingResultSet.updateBinaryStream(columnIndex, x, length);
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
        underlyingResultSet.updateCharacterStream(columnIndex, x, length);
    }

    @Override
    public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException {
        underlyingResultSet.updateObject(columnIndex, x, scaleOrLength);
    }

    @Override
    public void updateObject(int columnIndex, Object x) throws SQLException {
        underlyingResultSet.updateObject(columnIndex, x);
    }

    @Override
    public void updateNull(String columnLabel) throws SQLException {
        underlyingResultSet.updateNull(columnLabel);
    }

    @Override
    public void updateBoolean(String columnLabel, boolean x) throws SQLException {
        underlyingResultSet.updateBoolean(columnLabel, x);
    }

    @Override
    public void updateByte(String columnLabel, byte x) throws SQLException {
        underlyingResultSet.updateByte(columnLabel, x);
    }

    @Override
    public void updateShort(String columnLabel, short x) throws SQLException {
        underlyingResultSet.updateShort(columnLabel, x);
    }

    @Override
    public void updateInt(String columnLabel, int x) throws SQLException {
        underlyingResultSet.updateInt(columnLabel, x);
    }

    @Override
    public void updateLong(String columnLabel, long x) throws SQLException {
        underlyingResultSet.updateLong(columnLabel, x);
    }

    @Override
    public void updateFloat(String columnLabel, float x) throws SQLException {
        underlyingResultSet.updateFloat(columnLabel, x);
    }

    @Override
    public void updateDouble(String columnLabel, double x) throws SQLException {
        underlyingResultSet.updateDouble(columnLabel, x);
    }

    @Override
    public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {
        underlyingResultSet.updateBigDecimal(columnLabel, x);
    }

    @Override
    public void updateString(String columnLabel, String x) throws SQLException {
        underlyingResultSet.updateString(columnLabel, x);
    }

    @Override
    public void updateBytes(String columnLabel, byte[] x) throws SQLException {
        underlyingResultSet.updateBytes(columnLabel, x);
    }

    @Override
    public void updateDate(String columnLabel, Date x) throws SQLException {
        underlyingResultSet.updateDate(columnLabel, x);
    }

    @Override
    public void updateTime(String columnLabel, Time x) throws SQLException {
        underlyingResultSet.updateTime(columnLabel, x);
    }

    @Override
    public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {
        underlyingResultSet.updateTimestamp(columnLabel, x);
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException {
        underlyingResultSet.updateAsciiStream(columnLabel, x, length);
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException {
        underlyingResultSet.updateBinaryStream(columnLabel, x, length);
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException {
        underlyingResultSet.updateCharacterStream(columnLabel, reader, length);
    }

    @Override
    public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {
        underlyingResultSet.updateObject(columnLabel, x, scaleOrLength);
    }

    @Override
    public void updateObject(String columnLabel, Object x) throws SQLException {
        underlyingResultSet.updateObject(columnLabel, x);
    }

    @Override
    public void insertRow() throws SQLException {
        underlyingResultSet.insertRow();
    }

    @Override
    public void updateRow() throws SQLException {
        underlyingResultSet.updateRow();
    }

    @Override
    public void deleteRow() throws SQLException {
        underlyingResultSet.deleteRow();
    }

    @Override
    public void refreshRow() throws SQLException {
        underlyingResultSet.refreshRow();
    }

    @Override
    public void cancelRowUpdates() throws SQLException {
        underlyingResultSet.cancelRowUpdates();
    }

    @Override
    public void moveToInsertRow() throws SQLException {
        underlyingResultSet.moveToInsertRow();
    }

    @Override
    public void moveToCurrentRow() throws SQLException {
        underlyingResultSet.moveToCurrentRow();
    }

    @Override
    public Statement getStatement() throws SQLException {
        return underlyingResultSet.getStatement();
    }

    @Override
    public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
        trackColumnAccess(columnIndex);
        return underlyingResultSet.getObject(columnIndex, map);
    }

    @Override
    public Ref getRef(int columnIndex) throws SQLException {
        trackColumnAccess(columnIndex);
        return underlyingResultSet.getRef(columnIndex);
    }

    @Override
    public Blob getBlob(int columnIndex) throws SQLException {
        trackColumnAccess(columnIndex);
        return underlyingResultSet.getBlob(columnIndex);
    }

    @Override
    public Clob getClob(int columnIndex) throws SQLException {
        trackColumnAccess(columnIndex);
        return underlyingResultSet.getClob(columnIndex);
    }

    @Override
    public Array getArray(int columnIndex) throws SQLException {
        trackColumnAccess(columnIndex);
        return underlyingResultSet.getArray(columnIndex);
    }

    @Override
    public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
        trackColumnAccess(columnLabel);
        return underlyingResultSet.getObject(columnLabel, map);
    }

    @Override
    public Ref getRef(String columnLabel) throws SQLException {
        trackColumnAccess(columnLabel);
        return underlyingResultSet.getRef(columnLabel);
    }

    @Override
    public Blob getBlob(String columnLabel) throws SQLException {
        trackColumnAccess(columnLabel);
        return underlyingResultSet.getBlob(columnLabel);
    }

    @Override
    public Clob getClob(String columnLabel) throws SQLException {
        trackColumnAccess(columnLabel);
        return underlyingResultSet.getClob(columnLabel);
    }

    @Override
    public Array getArray(String columnLabel) throws SQLException {
        trackColumnAccess(columnLabel);
        return underlyingResultSet.getArray(columnLabel);
    }

    @Override
    public Date getDate(int columnIndex, Calendar cal) throws SQLException {
        trackColumnAccess(columnIndex);
        return underlyingResultSet.getDate(columnIndex, cal);
    }

    @Override
    public Date getDate(String columnLabel, Calendar cal) throws SQLException {
        trackColumnAccess(columnLabel);
        return underlyingResultSet.getDate(columnLabel, cal);
    }

    @Override
    public Time getTime(int columnIndex, Calendar cal) throws SQLException {
        trackColumnAccess(columnIndex);
        return underlyingResultSet.getTime(columnIndex, cal);
    }

    @Override
    public Time getTime(String columnLabel, Calendar cal) throws SQLException {
        trackColumnAccess(columnLabel);
        return underlyingResultSet.getTime(columnLabel, cal);
    }

    @Override
    public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
        trackColumnAccess(columnIndex);
        return underlyingResultSet.getTimestamp(columnIndex, cal);
    }

    @Override
    public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
        trackColumnAccess(columnLabel);
        return underlyingResultSet.getTimestamp(columnLabel, cal);
    }

    @Override
    public URL getURL(int columnIndex) throws SQLException {
        trackColumnAccess(columnIndex);
        return underlyingResultSet.getURL(columnIndex);
    }

    @Override
    public URL getURL(String columnLabel) throws SQLException {
        trackColumnAccess(columnLabel);
        return underlyingResultSet.getURL(columnLabel);
    }

    @Override
    public void updateRef(int columnIndex, Ref x) throws SQLException {
        underlyingResultSet.updateRef(columnIndex, x);
    }

    @Override
    public void updateRef(String columnLabel, Ref x) throws SQLException {
        underlyingResultSet.updateRef(columnLabel, x);
    }

    @Override
    public void updateBlob(int columnIndex, Blob x) throws SQLException {
        underlyingResultSet.updateBlob(columnIndex, x);
    }

    @Override
    public void updateBlob(String columnLabel, Blob x) throws SQLException {
        underlyingResultSet.updateBlob(columnLabel, x);
    }

    @Override
    public void updateClob(int columnIndex, Clob x) throws SQLException {
        underlyingResultSet.updateClob(columnIndex, x);
    }

    @Override
    public void updateClob(String columnLabel, Clob x) throws SQLException {
        underlyingResultSet.updateClob(columnLabel, x);
    }

    @Override
    public void updateArray(int columnIndex, Array x) throws SQLException {
        underlyingResultSet.updateArray(columnIndex, x);
    }

    @Override
    public void updateArray(String columnLabel, Array x) throws SQLException {
        underlyingResultSet.updateArray(columnLabel, x);
    }

    @Override
    public RowId getRowId(int columnIndex) throws SQLException {
        trackColumnAccess(columnIndex);
        return underlyingResultSet.getRowId(columnIndex);
    }

    @Override
    public RowId getRowId(String columnLabel) throws SQLException {
        trackColumnAccess(columnLabel);
        return underlyingResultSet.getRowId(columnLabel);
    }

    @Override
    public void updateRowId(int columnIndex, RowId x) throws SQLException {
        underlyingResultSet.updateRowId(columnIndex, x);
    }

    @Override
    public void updateRowId(String columnLabel, RowId x) throws SQLException {
        underlyingResultSet.updateRowId(columnLabel, x);
    }

    @Override
    public int getHoldability() throws SQLException {
        return underlyingResultSet.getHoldability();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return underlyingResultSet.isClosed();
    }

    @Override
    public void updateNString(int columnIndex, String nString) throws SQLException {
        underlyingResultSet.updateNString(columnIndex, nString);
    }

    @Override
    public void updateNString(String columnLabel, String nString) throws SQLException {
        underlyingResultSet.updateNString(columnLabel, nString);
    }

    @Override
    public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
        underlyingResultSet.updateNClob(columnIndex, nClob);
    }

    @Override
    public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
        underlyingResultSet.updateNClob(columnLabel, nClob);
    }

    @Override
    public NClob getNClob(int columnIndex) throws SQLException {
        trackColumnAccess(columnIndex);
        return underlyingResultSet.getNClob(columnIndex);
    }

    @Override
    public NClob getNClob(String columnLabel) throws SQLException {
        trackColumnAccess(columnLabel);
        return underlyingResultSet.getNClob(columnLabel);
    }

    @Override
    public SQLXML getSQLXML(int columnIndex) throws SQLException {
        trackColumnAccess(columnIndex);
        return underlyingResultSet.getSQLXML(columnIndex);
    }

    @Override
    public SQLXML getSQLXML(String columnLabel) throws SQLException {
        trackColumnAccess(columnLabel);
        return underlyingResultSet.getSQLXML(columnLabel);
    }

    @Override
    public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
        underlyingResultSet.updateSQLXML(columnIndex, xmlObject);
    }

    @Override
    public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
        underlyingResultSet.updateSQLXML(columnLabel, xmlObject);
    }

    @Override
    public String getNString(int columnIndex) throws SQLException {
        trackColumnAccess(columnIndex);
        return underlyingResultSet.getNString(columnIndex);
    }

    @Override
    public String getNString(String columnLabel) throws SQLException {
        trackColumnAccess(columnLabel);
        return underlyingResultSet.getNString(columnLabel);
    }

    @Override
    public Reader getNCharacterStream(int columnIndex) throws SQLException {
        trackColumnAccess(columnIndex);
        return underlyingResultSet.getNCharacterStream(columnIndex);
    }

    @Override
    public Reader getNCharacterStream(String columnLabel) throws SQLException {
        trackColumnAccess(columnLabel);
        return underlyingResultSet.getNCharacterStream(columnLabel);
    }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        underlyingResultSet.updateNCharacterStream(columnIndex, x, length);
    }

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        underlyingResultSet.updateNCharacterStream(columnLabel, reader, length);
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
        underlyingResultSet.updateAsciiStream(columnIndex, x, length);
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
        underlyingResultSet.updateBinaryStream(columnIndex, x, length);
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        underlyingResultSet.updateCharacterStream(columnIndex, x, length);
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
        underlyingResultSet.updateAsciiStream(columnLabel, x, length);
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
        underlyingResultSet.updateBinaryStream(columnLabel, x, length);
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        underlyingResultSet.updateCharacterStream(columnLabel, reader, length);
    }

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
        underlyingResultSet.updateBlob(columnIndex, inputStream, length);
    }

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
        underlyingResultSet.updateBlob(columnLabel, inputStream, length);
    }

    @Override
    public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
        underlyingResultSet.updateClob(columnIndex, reader, length);
    }

    @Override
    public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
        underlyingResultSet.updateClob(columnLabel, reader, length);
    }

    @Override
    public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
        underlyingResultSet.updateNClob(columnIndex, reader, length);
    }

    @Override
    public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
        underlyingResultSet.updateNClob(columnLabel, reader, length);
    }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
        underlyingResultSet.updateNCharacterStream(columnIndex, x);
    }

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
        underlyingResultSet.updateNCharacterStream(columnLabel, reader);
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
        underlyingResultSet.updateAsciiStream(columnIndex, x);
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
        underlyingResultSet.updateBinaryStream(columnIndex, x);
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
        underlyingResultSet.updateCharacterStream(columnIndex, x);
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
        underlyingResultSet.updateAsciiStream(columnLabel, x);
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
        underlyingResultSet.updateBinaryStream(columnLabel, x);
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
        underlyingResultSet.updateCharacterStream(columnLabel, reader);
    }

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
        underlyingResultSet.updateBlob(columnIndex, inputStream);
    }

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
        underlyingResultSet.updateBlob(columnLabel, inputStream);
    }

    @Override
    public void updateClob(int columnIndex, Reader reader) throws SQLException {
        underlyingResultSet.updateClob(columnIndex, reader);
    }

    @Override
    public void updateClob(String columnLabel, Reader reader) throws SQLException {
        underlyingResultSet.updateClob(columnLabel, reader);
    }

    @Override
    public void updateNClob(int columnIndex, Reader reader) throws SQLException {
        underlyingResultSet.updateNClob(columnIndex, reader);
    }

    @Override
    public void updateNClob(String columnLabel, Reader reader) throws SQLException {
        underlyingResultSet.updateNClob(columnLabel, reader);
    }

    @Override
    public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
        trackColumnAccess(columnIndex);
        return underlyingResultSet.getObject(columnIndex, type);
    }

    @Override
    public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
        trackColumnAccess(columnLabel);
        return underlyingResultSet.getObject(columnLabel, type);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isAssignableFrom(getClass())) {
            return iface.cast(this);
        }
        return underlyingResultSet.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isAssignableFrom(getClass()) || underlyingResultSet.isWrapperFor(iface);
    }

    // Additional methods may need to be added for full coverage
    @Override
    public Reader getCharacterStream(int columnIndex) throws SQLException {
        trackColumnAccess(columnIndex);
        return underlyingResultSet.getCharacterStream(columnIndex);
    }

    @Override
    public Reader getCharacterStream(String columnLabel) throws SQLException {
        trackColumnAccess(columnLabel);
        return underlyingResultSet.getCharacterStream(columnLabel);
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
        trackColumnAccess(columnIndex);
        return underlyingResultSet.getBigDecimal(columnIndex);
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
        trackColumnAccess(columnLabel);
        return underlyingResultSet.getBigDecimal(columnLabel);
    }
}
