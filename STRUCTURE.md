# JDBC Proxy Driver - Project Structure

## Overview

This project implements a comprehensive JDBC driver proxy that provides hooks for logging, SQL rewriting, and result processing while maintaining full compatibility with existing JDBC applications.

## Project Structure

```
jdbc_proxy/
├── pom.xml                             # Maven project configuration
├── README.md                           # Comprehensive documentation
├── STRUCTURE.md                        # This file
│
├── src/main/java/com/example/jdbcproxy/
│   ├── driver/                         # Core proxy driver implementation
│   │   ├── ProxyDriver.java           # Main driver class implementing java.sql.Driver
│   │   ├── ProxyConnection.java       # Connection proxy with transaction logging
│   │   ├── BaseProxyStatement.java    # Common statement functionality
│   │   ├── ProxyStatement.java        # Basic statement proxy
│   │   ├── ProxyPreparedStatement.java # PreparedStatement with parameter tracking
│   │   ├── ProxyCallableStatement.java # CallableStatement proxy
│   │   └── ProxyResultSet.java        # ResultSet with processing hooks
│   │
│   ├── hooks/                          # Hook interfaces and configuration
│   │   ├── JdbcLogger.java            # Interface for logging JDBC operations
│   │   ├── SqlRewriter.java           # Interface for SQL modification
│   │   ├── ResultProcessor.java       # Interface for result processing
│   │   └── ProxyConfiguration.java    # Configuration management
│   │
│   ├── extensions/                     # Built-in extension implementations
│   │   ├── ConsoleJdbcLogger.java     # Console logging implementation
│   │   ├── TablePrefixRewriter.java   # Multi-tenant table prefixing
│   │   └── DataMaskingProcessor.java  # Sensitive data masking
│   │
│   └── examples/                       # Example applications
│       └── ExampleApplication.java    # Comprehensive usage examples
│
├── src/main/resources/
│   └── META-INF/services/
│       └── java.sql.Driver            # Service registration for JDBC driver
│
└── src/test/java/com/example/jdbcproxy/
    └── ProxyDriverIntegrationTest.java # Comprehensive integration tests
```

## Core Components

### 1. Driver Layer (`driver/`)
- **ProxyDriver**: Main entry point, handles URL parsing and connection establishment
- **ProxyConnection**: Wraps database connections, applies SQL rewriting, logs transactions
- **Statement Proxies**: Intercept SQL execution, apply hooks, track parameters
- **ProxyResultSet**: Processes result data, applies transformations, tracks access patterns

### 2. Hook System (`hooks/`)
- **JdbcLogger**: Extensible logging for all JDBC operations
- **SqlRewriter**: SQL modification before execution (security, multi-tenancy, optimization)
- **ResultProcessor**: Result data transformation and access logging
- **ProxyConfiguration**: Thread-safe configuration management with priorities

### 3. Built-in Extensions (`extensions/`)
- **ConsoleJdbcLogger**: Development-friendly console logging
- **TablePrefixRewriter**: Automatic table name prefixing for multi-tenancy
- **DataMaskingProcessor**: Automatic masking of sensitive data columns

## Key Features

### URL Format
```
jdbc:proxy:<original-jdbc-url>
```

### Configuration Properties
- `proxy.configKey`: Select configuration profile
- `proxy.underlyingDriver`: Force specific driver loading

### Hook Priority System
All hooks support priority ordering (higher numbers execute first):
- SQL Rewriters: Applied in priority order before execution
- Result Processors: Applied in priority order during result retrieval
- Loggers: Called in priority order for all events

### Error Handling
- Robust exception handling prevents hook failures from breaking SQL operations
- Failed hooks are logged but don't propagate
- Underlying JDBC operations continue normally

### Thread Safety
- All components are thread-safe for concurrent access
- CopyOnWriteArrayList used for hook collections
- Volatile fields for configuration state

### Performance Considerations
- Minimal overhead when no hooks are configured
- Hooks only invoked when enabled
- Parameter tracking only when logging is active
- Compatible with connection pooling

## Usage Examples

### Basic Logging
```java
ProxyConfiguration config = ProxyDriver.getConfiguration("default");
config.addLogger(new ConsoleJdbcLogger());

Connection conn = DriverManager.getConnection("jdbc:proxy:jdbc:h2:mem:test", "sa", "");
```

### Multi-tenancy
```java
config.addRewriter(new TablePrefixRewriter("tenant1_"));
// "SELECT * FROM users" becomes "SELECT * FROM tenant1_users"
```

### Data Security
```java
config.addProcessor(new DataMaskingProcessor());
// Sensitive columns automatically masked in results
```

### Multiple Environments
```java
ProxyConfiguration devConfig = new ProxyConfiguration()
    .addLogger(new ConsoleJdbcLogger(true, true))
    .addRewriter(new TablePrefixRewriter("dev_"));

ProxyDriver.setConfiguration("development", devConfig);

props.setProperty("proxy.configKey", "development");
```

## Testing

The project includes comprehensive integration tests demonstrating:
- Basic proxy functionality with H2 database
- SQL rewriting capabilities
- Data masking functionality
- Transaction logging
- Batch operation handling
- Multiple configuration usage
- Error handling and logging

## Dependencies

- **Java 21+**: Modern Java features and performance
- **SLF4J 2.0+**: Flexible logging abstraction
- **H2 Database**: In-memory testing (test scope)
- **JUnit 5**: Modern testing framework (test scope)
- **Mockito**: Mocking framework (test scope)

## Extension Points

The architecture is designed for easy extension:

1. **Custom Loggers**: Implement `JdbcLogger` for custom logging destinations
2. **SQL Rewriters**: Implement `SqlRewriter` for custom SQL modifications
3. **Result Processors**: Implement `ResultProcessor` for data transformation
4. **Configuration**: Multiple named configurations for different environments

## Production Readiness

- Comprehensive error handling and logging
- Thread-safe implementation
- Minimal performance impact
- Graceful degradation when hooks fail
- Extensive test coverage
- Clear documentation and examples
- Maven-based build system
