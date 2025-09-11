package com.hitorro.jdbcproxy.driver;

import com.hitorro.jdbcproxy.hooks.ProxyConfiguration;

import java.sql.Statement;

/**
 * Proxy Statement implementation.
 */
public class ProxyStatement extends BaseProxyStatement<Statement> {

    private String lastExecutedSql = "";

    public ProxyStatement(Statement underlyingStatement, ProxyConnection proxyConnection, ProxyConfiguration config) {
        super(underlyingStatement, proxyConnection, config);
    }

    @Override
    protected String getLastExecutedSql() {
        return lastExecutedSql;
    }

    @Override
    protected <R> R executeWithLogging(String sql, SqlOperation<R> operation) throws java.sql.SQLException {
        this.lastExecutedSql = sql;
        return super.executeWithLogging(sql, operation);
    }
}
