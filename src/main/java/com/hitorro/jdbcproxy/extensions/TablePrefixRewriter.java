package com.hitorro.jdbcproxy.extensions;

import com.hitorro.jdbcproxy.hooks.SqlRewriter;

import java.sql.Connection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * SQL rewriter that adds a prefix to table names.
 * Useful for multi-tenant applications where each tenant has a table prefix.
 * 
 * Example: "users" becomes "tenant1_users" if prefix is "tenant1_"
 */
public class TablePrefixRewriter implements SqlRewriter {

    private final String prefix;
    private final Pattern tablePattern;

    public TablePrefixRewriter(String prefix) {
        this.prefix = prefix;
        // Match common SQL patterns: FROM/JOIN/UPDATE/INSERT INTO/DELETE FROM followed by table name
        this.tablePattern = Pattern.compile(
            "\\b(?:FROM|JOIN|UPDATE|INSERT\\s+INTO|DELETE\\s+FROM)\\s+([a-zA-Z_][a-zA-Z0-9_]*)",
            Pattern.CASE_INSENSITIVE
        );
    }

    @Override
    public String rewriteSql(String sql, Connection connection) {
        if (sql == null || sql.trim().isEmpty() || prefix == null || prefix.isEmpty()) {
            return sql;
        }

        Matcher matcher = tablePattern.matcher(sql);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String tableName = matcher.group(1);
            
            // Don't add prefix if table already has it
            if (!tableName.startsWith(prefix)) {
                String replacement = matcher.group(0).replace(tableName, prefix + tableName);
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            }
        }
        
        matcher.appendTail(result);
        return result.toString();
    }

    @Override
    public String rewriteParameterizedSql(String sql, List<Object> parameters, Connection connection) {
        // For parameterized statements, we use the same logic
        return rewriteSql(sql, connection);
    }

    @Override
    public int getPriority() {
        return 100; // High priority to run early
    }

    public String getPrefix() {
        return prefix;
    }
}
