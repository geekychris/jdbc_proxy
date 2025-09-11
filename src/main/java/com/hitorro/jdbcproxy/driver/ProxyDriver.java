package com.hitorro.jdbcproxy.driver;

import com.hitorro.jdbcproxy.hooks.ProxyConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main JDBC proxy driver that delegates to underlying drivers while providing
 * hooks for logging, SQL rewriting, and result processing.
 */
public class ProxyDriver implements Driver {
    
    private static final Logger logger = LoggerFactory.getLogger(ProxyDriver.class);
    
    public static final String PROXY_PREFIX = "jdbc:proxy:";
    public static final String UNDERLYING_DRIVER_PROPERTY = "proxy.underlyingDriver";
    public static final String CONFIG_KEY_PROPERTY = "proxy.configKey";
    public static final String DEFAULT_CONFIG_KEY = "default";
    
    // Global configurations by key
    private static final ConcurrentHashMap<String, ProxyConfiguration> configurations = new ConcurrentHashMap<>();
    
    static {
        try {
            DriverManager.registerDriver(new ProxyDriver());
            logger.info("JDBC Proxy Driver registered successfully");
        } catch (SQLException e) {
            logger.error("Failed to register JDBC Proxy Driver", e);
        }
        
        // Set up default configuration
        configurations.put(DEFAULT_CONFIG_KEY, new ProxyConfiguration());
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        if (!acceptsURL(url)) {
            return null;
        }

        long startTime = System.currentTimeMillis();
        String user = info != null ? info.getProperty("user") : null;
        SQLException exception = null;
        
        try {
            // Parse the proxy URL to get the underlying URL
            String underlyingUrl = parseUnderlyingUrl(url);
            String underlyingDriverClass = getUnderlyingDriverClass(info);
            String configKey = getConfigKey(info);
            
            ProxyConfiguration config = getConfiguration(configKey);
            
            // Load the underlying driver if specified
            if (underlyingDriverClass != null) {
                try {
                    Class.forName(underlyingDriverClass);
                } catch (ClassNotFoundException e) {
                    throw new SQLException("Unable to load underlying driver: " + underlyingDriverClass, e);
                }
            }
            
            // Get connection from underlying driver
            Connection underlyingConnection = DriverManager.getConnection(underlyingUrl, info);
            
            // Create proxy connection
            ProxyConnection proxyConnection = new ProxyConnection(underlyingConnection, config, underlyingUrl);
            
            long endTime = System.currentTimeMillis();
            
            // Log connection establishment
            config.getLoggers().forEach(jdbcLogger -> {
                try {
                    jdbcLogger.logConnection(underlyingUrl, user, startTime, endTime, null);
                } catch (Exception e) {
                    logger.warn("Error in JDBC logger while logging connection", e);
                }
            });
            
            return proxyConnection;
            
        } catch (SQLException e) {
            exception = e;
            long endTime = System.currentTimeMillis();
            
            // Log failed connection attempt
            String configKey = getConfigKey(info);
            ProxyConfiguration config = getConfiguration(configKey);
            config.getLoggers().forEach(jdbcLogger -> {
                try {
                    jdbcLogger.logConnection(url, user, startTime, endTime, e);
                } catch (Exception logException) {
                    logger.warn("Error in JDBC logger while logging failed connection", logException);
                }
            });
            
            throw e;
        }
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return url != null && url.startsWith(PROXY_PREFIX);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        if (!acceptsURL(url)) {
            return new DriverPropertyInfo[0];
        }
        
        // Return proxy-specific properties plus underlying driver properties
        DriverPropertyInfo[] proxyProperties = new DriverPropertyInfo[] {
            new DriverPropertyInfo(UNDERLYING_DRIVER_PROPERTY, 
                                 info != null ? info.getProperty(UNDERLYING_DRIVER_PROPERTY) : null),
            new DriverPropertyInfo(CONFIG_KEY_PROPERTY,
                                 info != null ? info.getProperty(CONFIG_KEY_PROPERTY, DEFAULT_CONFIG_KEY) : DEFAULT_CONFIG_KEY)
        };
        
        proxyProperties[0].description = "Class name of the underlying JDBC driver";
        proxyProperties[0].required = false;
        
        proxyProperties[1].description = "Configuration key for proxy settings";
        proxyProperties[1].required = false;
        
        try {
            String underlyingUrl = parseUnderlyingUrl(url);
            String underlyingDriverClass = getUnderlyingDriverClass(info);
            
            if (underlyingDriverClass != null) {
                Class.forName(underlyingDriverClass);
            }
            
            Driver underlyingDriver = DriverManager.getDriver(underlyingUrl);
            if (underlyingDriver != null) {
                DriverPropertyInfo[] underlyingProperties = underlyingDriver.getPropertyInfo(underlyingUrl, info);
                
                // Combine proxy and underlying properties
                DriverPropertyInfo[] combined = new DriverPropertyInfo[proxyProperties.length + underlyingProperties.length];
                System.arraycopy(proxyProperties, 0, combined, 0, proxyProperties.length);
                System.arraycopy(underlyingProperties, 0, combined, proxyProperties.length, underlyingProperties.length);
                
                return combined;
            }
        } catch (Exception e) {
            logger.debug("Could not get property info from underlying driver", e);
        }
        
        return proxyProperties;
    }

    @Override
    public int getMajorVersion() {
        return 1;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public boolean jdbcCompliant() {
        // The proxy driver compliance depends on the underlying driver
        return false;
    }

    @Override
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return java.util.logging.Logger.getLogger(ProxyDriver.class.getPackage().getName());
    }

    /**
     * Get or create a configuration for the given key.
     *
     * @param key the configuration key
     * @return the configuration instance
     */
    public static ProxyConfiguration getConfiguration(String key) {
        if (key == null) {
            key = DEFAULT_CONFIG_KEY;
        }
        return configurations.computeIfAbsent(key, k -> new ProxyConfiguration());
    }

    /**
     * Set a configuration for the given key.
     *
     * @param key the configuration key
     * @param config the configuration instance
     */
    public static void setConfiguration(String key, ProxyConfiguration config) {
        if (key == null) {
            key = DEFAULT_CONFIG_KEY;
        }
        configurations.put(key, config);
    }

    /**
     * Remove a configuration for the given key.
     *
     * @param key the configuration key
     */
    public static void removeConfiguration(String key) {
        if (key != null && !DEFAULT_CONFIG_KEY.equals(key)) {
            configurations.remove(key);
        }
    }

    /**
     * Parse the underlying database URL from the proxy URL.
     * Proxy URL format: jdbc:proxy:actualDriverUrl
     *
     * @param proxyUrl the proxy URL
     * @return the underlying database URL
     * @throws SQLException if the URL format is invalid
     */
    private String parseUnderlyingUrl(String proxyUrl) throws SQLException {
        if (!acceptsURL(proxyUrl)) {
            throw new SQLException("Invalid proxy URL: " + proxyUrl);
        }
        
        String underlyingUrl = proxyUrl.substring(PROXY_PREFIX.length());
        if (underlyingUrl.isEmpty()) {
            throw new SQLException("Missing underlying URL in proxy URL: " + proxyUrl);
        }
        
        return underlyingUrl;
    }

    /**
     * Get the underlying driver class name from connection properties.
     *
     * @param info connection properties
     * @return the driver class name, or null if not specified
     */
    private String getUnderlyingDriverClass(Properties info) {
        return info != null ? info.getProperty(UNDERLYING_DRIVER_PROPERTY) : null;
    }

    /**
     * Get the configuration key from connection properties.
     *
     * @param info connection properties
     * @return the configuration key
     */
    private String getConfigKey(Properties info) {
        return info != null ? info.getProperty(CONFIG_KEY_PROPERTY, DEFAULT_CONFIG_KEY) : DEFAULT_CONFIG_KEY;
    }
}
