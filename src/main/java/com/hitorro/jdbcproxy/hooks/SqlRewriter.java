package com.hitorro.jdbcproxy.hooks;

import java.sql.Connection;
import java.util.List;

/**
 * Interface for rewriting SQL statements before execution.
 * Implementations can modify SQL for various purposes such as:
 * - Adding query hints
 * - Modifying table names (e.g., for multi-tenancy)
 * - Adding WHERE clauses for security
 * - Query optimization
 */
public interface SqlRewriter {

    /**
     * Rewrite a SQL statement before execution.
     *
     * @param sql the original SQL statement
     * @param connection the connection that will execute the statement
     * @return the rewritten SQL statement, or the original if no changes needed
     */
    String rewriteSql(String sql, Connection connection);

    /**
     * Rewrite a parameterized SQL statement before execution.
     * This method is called for PreparedStatement SQL.
     *
     * @param sql the original SQL statement with parameter placeholders
     * @param parameters the parameters that will be bound (may be null or incomplete)
     * @param connection the connection that will execute the statement
     * @return the rewritten SQL statement, or the original if no changes needed
     */
    default String rewriteParameterizedSql(String sql, List<Object> parameters, Connection connection) {
        return rewriteSql(sql, connection);
    }

    /**
     * Returns the priority of this rewriter. Higher priority rewriters are applied first.
     * Default priority is 0.
     *
     * @return the priority value
     */
    default int getPriority() {
        return 0;
    }
}
