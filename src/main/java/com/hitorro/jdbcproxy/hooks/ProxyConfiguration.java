package com.hitorro.jdbcproxy.hooks;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Configuration class for the JDBC proxy driver.
 * Manages the registration and retrieval of hooks (loggers, rewriters, processors).
 */
public class ProxyConfiguration {

    private final List<JdbcLogger> loggers = new CopyOnWriteArrayList<>();
    private final List<SqlRewriter> rewriters = new CopyOnWriteArrayList<>();
    private final List<ResultProcessor> processors = new CopyOnWriteArrayList<>();
    
    private volatile boolean enabled = true;
    private volatile boolean logParameterValues = true;
    private volatile int maxParameterValueLength = 1000;

    /**
     * Add a JDBC logger to the configuration.
     *
     * @param logger the logger to add
     * @return this configuration instance for method chaining
     */
    public ProxyConfiguration addLogger(JdbcLogger logger) {
        if (logger != null) {
            loggers.add(logger);
            sortByPriority(loggers);
        }
        return this;
    }

    /**
     * Remove a JDBC logger from the configuration.
     *
     * @param logger the logger to remove
     * @return this configuration instance for method chaining
     */
    public ProxyConfiguration removeLogger(JdbcLogger logger) {
        loggers.remove(logger);
        return this;
    }

    /**
     * Add a SQL rewriter to the configuration.
     *
     * @param rewriter the rewriter to add
     * @return this configuration instance for method chaining
     */
    public ProxyConfiguration addRewriter(SqlRewriter rewriter) {
        if (rewriter != null) {
            rewriters.add(rewriter);
            sortByPriority(rewriters);
        }
        return this;
    }

    /**
     * Remove a SQL rewriter from the configuration.
     *
     * @param rewriter the rewriter to remove
     * @return this configuration instance for method chaining
     */
    public ProxyConfiguration removeRewriter(SqlRewriter rewriter) {
        rewriters.remove(rewriter);
        return this;
    }

    /**
     * Add a result processor to the configuration.
     *
     * @param processor the processor to add
     * @return this configuration instance for method chaining
     */
    public ProxyConfiguration addProcessor(ResultProcessor processor) {
        if (processor != null) {
            processors.add(processor);
            sortByPriority(processors);
        }
        return this;
    }

    /**
     * Remove a result processor from the configuration.
     *
     * @param processor the processor to remove
     * @return this configuration instance for method chaining
     */
    public ProxyConfiguration removeProcessor(ResultProcessor processor) {
        processors.remove(processor);
        return this;
    }

    /**
     * Get all registered JDBC loggers, sorted by priority.
     *
     * @return immutable list of loggers
     */
    public List<JdbcLogger> getLoggers() {
        return Collections.unmodifiableList(loggers);
    }

    /**
     * Get all registered SQL rewriters, sorted by priority.
     *
     * @return immutable list of rewriters
     */
    public List<SqlRewriter> getRewriters() {
        return Collections.unmodifiableList(rewriters);
    }

    /**
     * Get all registered result processors, sorted by priority.
     *
     * @return immutable list of processors
     */
    public List<ResultProcessor> getProcessors() {
        return Collections.unmodifiableList(processors);
    }

    /**
     * Check if the proxy functionality is enabled.
     *
     * @return true if enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enable or disable the proxy functionality.
     *
     * @param enabled true to enable, false to disable
     * @return this configuration instance for method chaining
     */
    public ProxyConfiguration setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * Check if parameter values should be logged.
     *
     * @return true if parameter values should be logged
     */
    public boolean isLogParameterValues() {
        return logParameterValues;
    }

    /**
     * Set whether parameter values should be logged.
     *
     * @param logParameterValues true to log parameter values
     * @return this configuration instance for method chaining
     */
    public ProxyConfiguration setLogParameterValues(boolean logParameterValues) {
        this.logParameterValues = logParameterValues;
        return this;
    }

    /**
     * Get the maximum length for parameter values in logs.
     *
     * @return maximum parameter value length
     */
    public int getMaxParameterValueLength() {
        return maxParameterValueLength;
    }

    /**
     * Set the maximum length for parameter values in logs.
     *
     * @param maxParameterValueLength maximum length (values longer than this will be truncated)
     * @return this configuration instance for method chaining
     */
    public ProxyConfiguration setMaxParameterValueLength(int maxParameterValueLength) {
        this.maxParameterValueLength = Math.max(0, maxParameterValueLength);
        return this;
    }

    /**
     * Clear all registered hooks.
     *
     * @return this configuration instance for method chaining
     */
    public ProxyConfiguration clearAll() {
        loggers.clear();
        rewriters.clear();
        processors.clear();
        return this;
    }

    /**
     * Sort a list of objects by their priority (higher priority first).
     * Objects must have a getPriority() method.
     */
    @SuppressWarnings("unchecked")
    private <T> void sortByPriority(List<T> list) {
        list.sort((a, b) -> {
            try {
                int priorityA = (Integer) a.getClass().getMethod("getPriority").invoke(a);
                int priorityB = (Integer) b.getClass().getMethod("getPriority").invoke(b);
                return Integer.compare(priorityB, priorityA); // Higher priority first
            } catch (Exception e) {
                return 0; // If priority comparison fails, maintain current order
            }
        });
    }
}
