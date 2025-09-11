package com.hitorro.jdbcproxy.driver;

import com.hitorro.jdbcproxy.hooks.ProxyConfiguration;

import java.sql.CallableStatement;

/**
 * Proxy CallableStatement implementation that extends ProxyPreparedStatement
 * with CallableStatement-specific functionality.
 */
public class ProxyCallableStatement extends ProxyPreparedStatement implements CallableStatement {

    private final CallableStatement callableStatement;

    public ProxyCallableStatement(CallableStatement underlyingStatement, String originalSql, String rewrittenSql,
                                 ProxyConnection proxyConnection, ProxyConfiguration config) {
        super(underlyingStatement, originalSql, rewrittenSql, proxyConnection, config);
        this.callableStatement = underlyingStatement;
    }

    // Delegate all CallableStatement-specific methods to the underlying statement
    // For brevity, I'm including just a few key methods. In a production implementation,
    // you would delegate all CallableStatement methods.

    @Override
    public void registerOutParameter(int parameterIndex, int sqlType) throws java.sql.SQLException {
        callableStatement.registerOutParameter(parameterIndex, sqlType);
    }

    @Override
    public void registerOutParameter(int parameterIndex, int sqlType, int scale) throws java.sql.SQLException {
        callableStatement.registerOutParameter(parameterIndex, sqlType, scale);
    }

    @Override
    public boolean wasNull() throws java.sql.SQLException {
        return callableStatement.wasNull();
    }

    @Override
    public String getString(int parameterIndex) throws java.sql.SQLException {
        return callableStatement.getString(parameterIndex);
    }

    @Override
    public boolean getBoolean(int parameterIndex) throws java.sql.SQLException {
        return callableStatement.getBoolean(parameterIndex);
    }

    @Override
    public byte getByte(int parameterIndex) throws java.sql.SQLException {
        return callableStatement.getByte(parameterIndex);
    }

    @Override
    public short getShort(int parameterIndex) throws java.sql.SQLException {
        return callableStatement.getShort(parameterIndex);
    }

    @Override
    public int getInt(int parameterIndex) throws java.sql.SQLException {
        return callableStatement.getInt(parameterIndex);
    }

    @Override
    public long getLong(int parameterIndex) throws java.sql.SQLException {
        return callableStatement.getLong(parameterIndex);
    }

    @Override
    public float getFloat(int parameterIndex) throws java.sql.SQLException {
        return callableStatement.getFloat(parameterIndex);
    }

    @Override
    public double getDouble(int parameterIndex) throws java.sql.SQLException {
        return callableStatement.getDouble(parameterIndex);
    }

    @Override
    public java.math.BigDecimal getBigDecimal(int parameterIndex, int scale) throws java.sql.SQLException {
        return callableStatement.getBigDecimal(parameterIndex, scale);
    }

    @Override
    public byte[] getBytes(int parameterIndex) throws java.sql.SQLException {
        return callableStatement.getBytes(parameterIndex);
    }

    @Override
    public java.sql.Date getDate(int parameterIndex) throws java.sql.SQLException {
        return callableStatement.getDate(parameterIndex);
    }

    @Override
    public java.sql.Time getTime(int parameterIndex) throws java.sql.SQLException {
        return callableStatement.getTime(parameterIndex);
    }

    @Override
    public java.sql.Timestamp getTimestamp(int parameterIndex) throws java.sql.SQLException {
        return callableStatement.getTimestamp(parameterIndex);
    }

    @Override
    public Object getObject(int parameterIndex) throws java.sql.SQLException {
        return callableStatement.getObject(parameterIndex);
    }

    @Override
    public java.math.BigDecimal getBigDecimal(int parameterIndex) throws java.sql.SQLException {
        return callableStatement.getBigDecimal(parameterIndex);
    }

    @Override
    public Object getObject(int parameterIndex, java.util.Map<String, Class<?>> map) throws java.sql.SQLException {
        return callableStatement.getObject(parameterIndex, map);
    }

    @Override
    public java.sql.Ref getRef(int parameterIndex) throws java.sql.SQLException {
        return callableStatement.getRef(parameterIndex);
    }

    @Override
    public java.sql.Blob getBlob(int parameterIndex) throws java.sql.SQLException {
        return callableStatement.getBlob(parameterIndex);
    }

    @Override
    public java.sql.Clob getClob(int parameterIndex) throws java.sql.SQLException {
        return callableStatement.getClob(parameterIndex);
    }

    @Override
    public java.sql.Array getArray(int parameterIndex) throws java.sql.SQLException {
        return callableStatement.getArray(parameterIndex);
    }

    @Override
    public java.sql.Date getDate(int parameterIndex, java.util.Calendar cal) throws java.sql.SQLException {
        return callableStatement.getDate(parameterIndex, cal);
    }

    @Override
    public java.sql.Time getTime(int parameterIndex, java.util.Calendar cal) throws java.sql.SQLException {
        return callableStatement.getTime(parameterIndex, cal);
    }

    @Override
    public java.sql.Timestamp getTimestamp(int parameterIndex, java.util.Calendar cal) throws java.sql.SQLException {
        return callableStatement.getTimestamp(parameterIndex, cal);
    }

    @Override
    public void registerOutParameter(int parameterIndex, int sqlType, String typeName) throws java.sql.SQLException {
        callableStatement.registerOutParameter(parameterIndex, sqlType, typeName);
    }

    @Override
    public void registerOutParameter(String parameterName, int sqlType) throws java.sql.SQLException {
        callableStatement.registerOutParameter(parameterName, sqlType);
    }

    @Override
    public void registerOutParameter(String parameterName, int sqlType, int scale) throws java.sql.SQLException {
        callableStatement.registerOutParameter(parameterName, sqlType, scale);
    }

    @Override
    public void registerOutParameter(String parameterName, int sqlType, String typeName) throws java.sql.SQLException {
        callableStatement.registerOutParameter(parameterName, sqlType, typeName);
    }

    @Override
    public java.net.URL getURL(int parameterIndex) throws java.sql.SQLException {
        return callableStatement.getURL(parameterIndex);
    }

    @Override
    public void setURL(String parameterName, java.net.URL val) throws java.sql.SQLException {
        callableStatement.setURL(parameterName, val);
    }

    @Override
    public void setNull(String parameterName, int sqlType) throws java.sql.SQLException {
        callableStatement.setNull(parameterName, sqlType);
    }

    @Override
    public void setBoolean(String parameterName, boolean x) throws java.sql.SQLException {
        callableStatement.setBoolean(parameterName, x);
    }

    @Override
    public void setByte(String parameterName, byte x) throws java.sql.SQLException {
        callableStatement.setByte(parameterName, x);
    }

    @Override
    public void setShort(String parameterName, short x) throws java.sql.SQLException {
        callableStatement.setShort(parameterName, x);
    }

    @Override
    public void setInt(String parameterName, int x) throws java.sql.SQLException {
        callableStatement.setInt(parameterName, x);
    }

    @Override
    public void setLong(String parameterName, long x) throws java.sql.SQLException {
        callableStatement.setLong(parameterName, x);
    }

    @Override
    public void setFloat(String parameterName, float x) throws java.sql.SQLException {
        callableStatement.setFloat(parameterName, x);
    }

    @Override
    public void setDouble(String parameterName, double x) throws java.sql.SQLException {
        callableStatement.setDouble(parameterName, x);
    }

    @Override
    public void setBigDecimal(String parameterName, java.math.BigDecimal x) throws java.sql.SQLException {
        callableStatement.setBigDecimal(parameterName, x);
    }

    @Override
    public void setString(String parameterName, String x) throws java.sql.SQLException {
        callableStatement.setString(parameterName, x);
    }

    @Override
    public void setBytes(String parameterName, byte[] x) throws java.sql.SQLException {
        callableStatement.setBytes(parameterName, x);
    }

    @Override
    public void setDate(String parameterName, java.sql.Date x) throws java.sql.SQLException {
        callableStatement.setDate(parameterName, x);
    }

    @Override
    public void setTime(String parameterName, java.sql.Time x) throws java.sql.SQLException {
        callableStatement.setTime(parameterName, x);
    }

    @Override
    public void setTimestamp(String parameterName, java.sql.Timestamp x) throws java.sql.SQLException {
        callableStatement.setTimestamp(parameterName, x);
    }

    @Override
    public void setAsciiStream(String parameterName, java.io.InputStream x, int length) throws java.sql.SQLException {
        callableStatement.setAsciiStream(parameterName, x, length);
    }

    @Override
    public void setBinaryStream(String parameterName, java.io.InputStream x, int length) throws java.sql.SQLException {
        callableStatement.setBinaryStream(parameterName, x, length);
    }

    @Override
    public void setObject(String parameterName, Object x, int targetSqlType, int scale) throws java.sql.SQLException {
        callableStatement.setObject(parameterName, x, targetSqlType, scale);
    }

    @Override
    public void setObject(String parameterName, Object x, int targetSqlType) throws java.sql.SQLException {
        callableStatement.setObject(parameterName, x, targetSqlType);
    }

    @Override
    public void setObject(String parameterName, Object x) throws java.sql.SQLException {
        callableStatement.setObject(parameterName, x);
    }

    @Override
    public void setCharacterStream(String parameterName, java.io.Reader reader, int length) throws java.sql.SQLException {
        callableStatement.setCharacterStream(parameterName, reader, length);
    }

    @Override
    public void setDate(String parameterName, java.sql.Date x, java.util.Calendar cal) throws java.sql.SQLException {
        callableStatement.setDate(parameterName, x, cal);
    }

    @Override
    public void setTime(String parameterName, java.sql.Time x, java.util.Calendar cal) throws java.sql.SQLException {
        callableStatement.setTime(parameterName, x, cal);
    }

    @Override
    public void setTimestamp(String parameterName, java.sql.Timestamp x, java.util.Calendar cal) throws java.sql.SQLException {
        callableStatement.setTimestamp(parameterName, x, cal);
    }

    @Override
    public void setNull(String parameterName, int sqlType, String typeName) throws java.sql.SQLException {
        callableStatement.setNull(parameterName, sqlType, typeName);
    }

    @Override
    public String getString(String parameterName) throws java.sql.SQLException {
        return callableStatement.getString(parameterName);
    }

    @Override
    public boolean getBoolean(String parameterName) throws java.sql.SQLException {
        return callableStatement.getBoolean(parameterName);
    }

    @Override
    public byte getByte(String parameterName) throws java.sql.SQLException {
        return callableStatement.getByte(parameterName);
    }

    @Override
    public short getShort(String parameterName) throws java.sql.SQLException {
        return callableStatement.getShort(parameterName);
    }

    @Override
    public int getInt(String parameterName) throws java.sql.SQLException {
        return callableStatement.getInt(parameterName);
    }

    @Override
    public long getLong(String parameterName) throws java.sql.SQLException {
        return callableStatement.getLong(parameterName);
    }

    @Override
    public float getFloat(String parameterName) throws java.sql.SQLException {
        return callableStatement.getFloat(parameterName);
    }

    @Override
    public double getDouble(String parameterName) throws java.sql.SQLException {
        return callableStatement.getDouble(parameterName);
    }

    @Override
    public byte[] getBytes(String parameterName) throws java.sql.SQLException {
        return callableStatement.getBytes(parameterName);
    }

    @Override
    public java.sql.Date getDate(String parameterName) throws java.sql.SQLException {
        return callableStatement.getDate(parameterName);
    }

    @Override
    public java.sql.Time getTime(String parameterName) throws java.sql.SQLException {
        return callableStatement.getTime(parameterName);
    }

    @Override
    public java.sql.Timestamp getTimestamp(String parameterName) throws java.sql.SQLException {
        return callableStatement.getTimestamp(parameterName);
    }

    @Override
    public Object getObject(String parameterName) throws java.sql.SQLException {
        return callableStatement.getObject(parameterName);
    }

    @Override
    public java.math.BigDecimal getBigDecimal(String parameterName) throws java.sql.SQLException {
        return callableStatement.getBigDecimal(parameterName);
    }

    @Override
    public Object getObject(String parameterName, java.util.Map<String, Class<?>> map) throws java.sql.SQLException {
        return callableStatement.getObject(parameterName, map);
    }

    @Override
    public java.sql.Ref getRef(String parameterName) throws java.sql.SQLException {
        return callableStatement.getRef(parameterName);
    }

    @Override
    public java.sql.Blob getBlob(String parameterName) throws java.sql.SQLException {
        return callableStatement.getBlob(parameterName);
    }

    @Override
    public java.sql.Clob getClob(String parameterName) throws java.sql.SQLException {
        return callableStatement.getClob(parameterName);
    }

    @Override
    public java.sql.Array getArray(String parameterName) throws java.sql.SQLException {
        return callableStatement.getArray(parameterName);
    }

    @Override
    public java.sql.Date getDate(String parameterName, java.util.Calendar cal) throws java.sql.SQLException {
        return callableStatement.getDate(parameterName, cal);
    }

    @Override
    public java.sql.Time getTime(String parameterName, java.util.Calendar cal) throws java.sql.SQLException {
        return callableStatement.getTime(parameterName, cal);
    }

    @Override
    public java.sql.Timestamp getTimestamp(String parameterName, java.util.Calendar cal) throws java.sql.SQLException {
        return callableStatement.getTimestamp(parameterName, cal);
    }

    @Override
    public java.net.URL getURL(String parameterName) throws java.sql.SQLException {
        return callableStatement.getURL(parameterName);
    }

    @Override
    public java.sql.RowId getRowId(int parameterIndex) throws java.sql.SQLException {
        return callableStatement.getRowId(parameterIndex);
    }

    @Override
    public java.sql.RowId getRowId(String parameterName) throws java.sql.SQLException {
        return callableStatement.getRowId(parameterName);
    }

    @Override
    public void setRowId(String parameterName, java.sql.RowId x) throws java.sql.SQLException {
        callableStatement.setRowId(parameterName, x);
    }

    @Override
    public void setNString(String parameterName, String value) throws java.sql.SQLException {
        callableStatement.setNString(parameterName, value);
    }

    @Override
    public void setNCharacterStream(String parameterName, java.io.Reader value, long length) throws java.sql.SQLException {
        callableStatement.setNCharacterStream(parameterName, value, length);
    }

    @Override
    public void setNClob(String parameterName, java.sql.NClob value) throws java.sql.SQLException {
        callableStatement.setNClob(parameterName, value);
    }

    @Override
    public void setClob(String parameterName, java.io.Reader reader, long length) throws java.sql.SQLException {
        callableStatement.setClob(parameterName, reader, length);
    }

    @Override
    public void setBlob(String parameterName, java.io.InputStream inputStream, long length) throws java.sql.SQLException {
        callableStatement.setBlob(parameterName, inputStream, length);
    }

    @Override
    public void setNClob(String parameterName, java.io.Reader reader, long length) throws java.sql.SQLException {
        callableStatement.setNClob(parameterName, reader, length);
    }

    @Override
    public java.sql.NClob getNClob(int parameterIndex) throws java.sql.SQLException {
        return callableStatement.getNClob(parameterIndex);
    }

    @Override
    public java.sql.NClob getNClob(String parameterName) throws java.sql.SQLException {
        return callableStatement.getNClob(parameterName);
    }

    @Override
    public void setSQLXML(String parameterName, java.sql.SQLXML xmlObject) throws java.sql.SQLException {
        callableStatement.setSQLXML(parameterName, xmlObject);
    }

    @Override
    public java.sql.SQLXML getSQLXML(int parameterIndex) throws java.sql.SQLException {
        return callableStatement.getSQLXML(parameterIndex);
    }

    @Override
    public java.sql.SQLXML getSQLXML(String parameterName) throws java.sql.SQLException {
        return callableStatement.getSQLXML(parameterName);
    }

    @Override
    public String getNString(int parameterIndex) throws java.sql.SQLException {
        return callableStatement.getNString(parameterIndex);
    }

    @Override
    public String getNString(String parameterName) throws java.sql.SQLException {
        return callableStatement.getNString(parameterName);
    }

    @Override
    public java.io.Reader getNCharacterStream(int parameterIndex) throws java.sql.SQLException {
        return callableStatement.getNCharacterStream(parameterIndex);
    }

    @Override
    public java.io.Reader getNCharacterStream(String parameterName) throws java.sql.SQLException {
        return callableStatement.getNCharacterStream(parameterName);
    }

    @Override
    public java.io.Reader getCharacterStream(int parameterIndex) throws java.sql.SQLException {
        return callableStatement.getCharacterStream(parameterIndex);
    }

    @Override
    public java.io.Reader getCharacterStream(String parameterName) throws java.sql.SQLException {
        return callableStatement.getCharacterStream(parameterName);
    }

    @Override
    public void setBlob(String parameterName, java.sql.Blob x) throws java.sql.SQLException {
        callableStatement.setBlob(parameterName, x);
    }

    @Override
    public void setClob(String parameterName, java.sql.Clob x) throws java.sql.SQLException {
        callableStatement.setClob(parameterName, x);
    }

    @Override
    public void setAsciiStream(String parameterName, java.io.InputStream x, long length) throws java.sql.SQLException {
        callableStatement.setAsciiStream(parameterName, x, length);
    }

    @Override
    public void setBinaryStream(String parameterName, java.io.InputStream x, long length) throws java.sql.SQLException {
        callableStatement.setBinaryStream(parameterName, x, length);
    }

    @Override
    public void setCharacterStream(String parameterName, java.io.Reader reader, long length) throws java.sql.SQLException {
        callableStatement.setCharacterStream(parameterName, reader, length);
    }

    @Override
    public void setAsciiStream(String parameterName, java.io.InputStream x) throws java.sql.SQLException {
        callableStatement.setAsciiStream(parameterName, x);
    }

    @Override
    public void setBinaryStream(String parameterName, java.io.InputStream x) throws java.sql.SQLException {
        callableStatement.setBinaryStream(parameterName, x);
    }

    @Override
    public void setCharacterStream(String parameterName, java.io.Reader reader) throws java.sql.SQLException {
        callableStatement.setCharacterStream(parameterName, reader);
    }

    @Override
    public void setNCharacterStream(String parameterName, java.io.Reader value) throws java.sql.SQLException {
        callableStatement.setNCharacterStream(parameterName, value);
    }

    @Override
    public void setClob(String parameterName, java.io.Reader reader) throws java.sql.SQLException {
        callableStatement.setClob(parameterName, reader);
    }

    @Override
    public void setBlob(String parameterName, java.io.InputStream inputStream) throws java.sql.SQLException {
        callableStatement.setBlob(parameterName, inputStream);
    }

    @Override
    public void setNClob(String parameterName, java.io.Reader reader) throws java.sql.SQLException {
        callableStatement.setNClob(parameterName, reader);
    }

    @Override
    public <T> T getObject(int parameterIndex, Class<T> type) throws java.sql.SQLException {
        return callableStatement.getObject(parameterIndex, type);
    }

    @Override
    public <T> T getObject(String parameterName, Class<T> type) throws java.sql.SQLException {
        return callableStatement.getObject(parameterName, type);
    }
}
