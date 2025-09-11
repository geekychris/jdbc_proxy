package com.hitorro.jdbcproxy.extensions;

import com.hitorro.jdbcproxy.hooks.ResultProcessor;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Result processor that masks sensitive data in ResultSet columns.
 * Useful for protecting sensitive information like credit card numbers,
 * social security numbers, phone numbers, etc.
 */
public class DataMaskingProcessor implements ResultProcessor {

    private final Set<String> sensitiveColumns = new HashSet<>();
    private final Set<Pattern> sensitivePatterns = new HashSet<>();
    private final String maskCharacter;
    private final boolean enabled;

    public DataMaskingProcessor() {
        this("*", true);
    }

    public DataMaskingProcessor(String maskCharacter, boolean enabled) {
        this.maskCharacter = maskCharacter;
        this.enabled = enabled;
        
        // Add common sensitive column names
        addSensitiveColumn("password");
        addSensitiveColumn("passwd");
        addSensitiveColumn("secret");
        addSensitiveColumn("token");
        addSensitiveColumn("api_key");
        addSensitiveColumn("credit_card");
        addSensitiveColumn("cc_number");
        addSensitiveColumn("ssn");
        addSensitiveColumn("social_security");
        addSensitiveColumn("phone");
        addSensitiveColumn("email");
        
        // Add patterns for sensitive data
        addSensitivePattern(Pattern.compile(".*password.*", Pattern.CASE_INSENSITIVE));
        addSensitivePattern(Pattern.compile(".*secret.*", Pattern.CASE_INSENSITIVE));
        addSensitivePattern(Pattern.compile(".*token.*", Pattern.CASE_INSENSITIVE));
        addSensitivePattern(Pattern.compile(".*key.*", Pattern.CASE_INSENSITIVE));
    }

    public DataMaskingProcessor addSensitiveColumn(String columnName) {
        if (columnName != null) {
            sensitiveColumns.add(columnName.toLowerCase());
        }
        return this;
    }

    public DataMaskingProcessor addSensitivePattern(Pattern pattern) {
        if (pattern != null) {
            sensitivePatterns.add(pattern);
        }
        return this;
    }

    @Override
    public Object transformColumnValue(int columnIndex, String columnName, Object value,
                                     ResultSet resultSet, String sql, Connection connection) throws SQLException {
        if (!enabled || value == null) {
            return value;
        }

        // Check if this column should be masked
        if (shouldMaskColumn(columnName)) {
            return maskValue(value);
        }

        return value;
    }

    @Override
    public int getPriority() {
        return 50; // Medium priority
    }

    private boolean shouldMaskColumn(String columnName) {
        if (columnName == null) {
            return false;
        }

        String lowerColumnName = columnName.toLowerCase();

        // Check exact match
        if (sensitiveColumns.contains(lowerColumnName)) {
            return true;
        }

        // Check pattern match
        for (Pattern pattern : sensitivePatterns) {
            if (pattern.matcher(columnName).matches()) {
                return true;
            }
        }

        return false;
    }

    private Object maskValue(Object value) {
        if (value == null) {
            return null;
        }

        String stringValue = value.toString();
        
        if (stringValue.length() <= 4) {
            // For short values, mask everything
            return maskCharacter.repeat(stringValue.length());
        } else {
            // For longer values, show first and last character
            return stringValue.charAt(0) + 
                   maskCharacter.repeat(stringValue.length() - 2) + 
                   stringValue.charAt(stringValue.length() - 1);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getMaskCharacter() {
        return maskCharacter;
    }

    public Set<String> getSensitiveColumns() {
        return new HashSet<>(sensitiveColumns);
    }
}
