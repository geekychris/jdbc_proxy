# JDBC Proxy Driver - System Design Document

## Overview

The JDBC Proxy Driver is a comprehensive middleware solution that sits between applications and database drivers, providing transparent interception and instrumentation of JDBC operations. This document details the architectural decisions, design patterns, and system interactions.

## Architecture

### 1. High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Application Layer                       │
│  ┌───────────────┐  ┌─────────────────┐  ┌─────────────────┐   │
│  │  Spring Boot  │  │   Plain JDBC    │  │   JPA/Hibernate │   │
│  │  Application  │  │   Application   │  │   Application   │   │
│  └───────────────┘  └─────────────────┘  └─────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                                │
                    jdbc:proxy:... URL
                                │
┌─────────────────────────────────────────────────────────────────┐
│                     JDBC Proxy Driver                          │
│                                                                 │
│  ┌─────────────────┐    ┌─────────────────┐                   │
│  │  ProxyDriver    │    │ ProxyConfiguration                  │
│  │  (Entry Point)  │◄──►│  - Hook Registry │                   │
│  └─────────────────┘    │  - Thread Safety │                   │
│           │              └─────────────────┘                   │
│           │                                                     │
│  ┌─────────────────┐    ┌─────────────────┐                   │
│  │ ProxyConnection │    │   Hook System   │                   │
│  │  - Wraps JDBC   │◄──►│  ┌─────────────┐│                   │
│  │  - Applies Hooks│    │  │SqlRewriter  ││                   │
│  └─────────────────┘    │  │JdbcLogger   ││                   │
│           │              │  │ResultProc.  ││                   │
│           │              │  └─────────────┘│                   │
│  ┌─────────────────┐    └─────────────────┘                   │
│  │ ProxyStatement  │                                           │
│  │ ProxyPrepStmt   │    ┌─────────────────┐                   │
│  │ ProxyCallableStmt│◄──►│  Built-in Ext.  │                   │
│  └─────────────────┘    │  ┌─────────────┐│                   │
│           │              │  │Console Log  ││                   │
│           │              │  │Table Prefix ││                   │
│  ┌─────────────────┐    │  │Data Masking ││                   │
│  │ ProxyResultSet  │    │  └─────────────┘│                   │
│  │  - Row Processing│    └─────────────────┘                   │
│  │  - Value Transform│                                          │
│  └─────────────────┘                                           │
└─────────────────────────────────────────────────────────────────┘
                                │
                         Delegates to
                                │
┌─────────────────────────────────────────────────────────────────┐
│                    Underlying JDBC Drivers                     │
│  ┌───────────────┐  ┌─────────────────┐  ┌─────────────────┐   │
│  │   H2 Driver   │  │PostgreSQL Driver│  │  MySQL Driver   │   │
│  └───────────────┘  └─────────────────┘  └─────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                                │
┌─────────────────────────────────────────────────────────────────┐
│                         Databases                               │
│  ┌───────────────┐  ┌─────────────────┐  ┌─────────────────┐   │
│  │ H2 Database   │  │   PostgreSQL    │  │     MySQL       │   │
│  └───────────────┘  └─────────────────┘  └─────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

## 2. Component Design

### Core Components

#### ProxyDriver
```mermaid
classDiagram
    class ProxyDriver {
        -ConcurrentHashMap~String, ProxyConfiguration~ configurations
        +connect(String url, Properties info) Connection
        +acceptsURL(String url) boolean
        +getPropertyInfo(String url, Properties info) DriverPropertyInfo[]
        +getConfiguration(String key) ProxyConfiguration
        +setConfiguration(String key, ProxyConfiguration config)
        -parseUnderlyingUrl(String proxyUrl) String
        -getConfigKey(Properties info) String
    }
    
    class ProxyConfiguration {
        -List~JdbcLogger~ loggers
        -List~SqlRewriter~ rewriters  
        -List~ResultProcessor~ processors
        -boolean enabled
        +addLogger(JdbcLogger logger) ProxyConfiguration
        +addRewriter(SqlRewriter rewriter) ProxyConfiguration
        +addProcessor(ResultProcessor processor) ProxyConfiguration
        +getLoggers() List~JdbcLogger~
        +getRewriters() List~SqlRewriter~
        +getProcessors() List~ResultProcessor~
    }
    
    ProxyDriver --> ProxyConfiguration : manages
```

#### Connection Layer
```mermaid
classDiagram
    class ProxyConnection {
        -Connection underlyingConnection
        -ProxyConfiguration config
        -String url
        +createStatement() Statement
        +prepareStatement(String sql) PreparedStatement
        +commit()
        +rollback()
        -applySqlRewriting(String sql) String
        -logTransaction(String operation, long startTime, long endTime, SQLException exception)
    }
    
    class BaseProxyStatement~T~ {
        #T underlyingStatement
        #ProxyConnection proxyConnection
        #ProxyConfiguration config
        +executeQuery(String sql) ResultSet
        +executeUpdate(String sql) int
        +execute(String sql) boolean
        #applySqlRewriting(String sql) String
        #executeWithLogging(String sql, SqlOperation operation) R
        #logStatement(...) void
        #logBatch(...) void
    }
    
    class ProxyPreparedStatement {
        -String originalSql
        -String rewrittenSql
        -Map~Integer, Object~ parameters
        +executeQuery() ResultSet
        +executeUpdate() int
        +setInt(int parameterIndex, int x)
        +setString(int parameterIndex, String x)
        -executeWithParameterLogging(SqlOperation operation) R
    }
    
    ProxyConnection --> BaseProxyStatement : creates
    BaseProxyStatement <|-- ProxyPreparedStatement : extends
```

#### Result Processing
```mermaid
classDiagram
    class ProxyResultSet {
        -ResultSet underlyingResultSet
        -String sql
        -ProxyConnection proxyConnection
        -ProxyConfiguration config
        -boolean started
        -int rowCount
        -List~String~ accessedColumns
        +next() boolean
        +getString(int columnIndex) String
        +getInt(int columnIndex) int
        +getObject(int columnIndex) Object
        -transformValue(int columnIndex, String columnName, Object value) Object
        -trackColumnAccess(String columnName)
        -callResultSetEnd()
    }
    
    ProxyResultSet --> ResultProcessor : uses for transformation
```

### Hook System Design

```mermaid
graph TB
    subgraph "Hook Interface Layer"
        JL[JdbcLogger Interface]
        SR[SqlRewriter Interface] 
        RP[ResultProcessor Interface]
    end
    
    subgraph "Built-in Implementations"
        CJL[ConsoleJdbcLogger]
        TPR[TablePrefixRewriter]
        DMP[DataMaskingProcessor]
    end
    
    subgraph "Custom Implementations"
        FJL[FileJdbcLogger]
        SEC[SecurityRewriter]
        AUD[AuditProcessor]
    end
    
    subgraph "Proxy Components"
        PD[ProxyDriver]
        PC[ProxyConnection] 
        PS[ProxyStatement]
        PRS[ProxyResultSet]
    end
    
    JL --> CJL
    JL --> FJL
    SR --> TPR
    SR --> SEC
    RP --> DMP
    RP --> AUD
    
    PD --> JL
    PC --> SR
    PS --> JL
    PRS --> RP
```

## 3. Interaction Flows

### Connection Establishment Flow

```mermaid
sequenceDiagram
    participant App as Application
    participant DM as DriverManager
    participant PD as ProxyDriver
    participant UD as Underlying Driver
    participant DB as Database
    participant JL as JdbcLogger
    participant PC as ProxyConfiguration

    App->>DM: getConnection("jdbc:proxy:jdbc:h2:mem:test", props)
    DM->>PD: connect(url, props)
    
    Note over PD: Parse proxy URL
    PD->>PD: parseUnderlyingUrl("jdbc:proxy:jdbc:h2:mem:test")
    PD->>PD: getConfigKey(props)
    PD->>PC: getConfiguration("configKey")
    
    Note over PD: Load underlying driver if needed
    PD->>UD: Class.forName("driverClass") [if specified]
    PD->>DM: getConnection("jdbc:h2:mem:test", props)
    DM->>UD: connect()
    UD->>DB: establish connection
    DB-->>UD: connection established
    UD-->>DM: Connection
    DM-->>PD: Connection
    
    Note over PD: Wrap and log connection
    PD->>PD: new ProxyConnection(underlying, config, url)
    PD->>JL: logConnection(url, user, startTime, endTime, null)
    PD-->>App: ProxyConnection
```

### SQL Execution Flow

```mermaid
sequenceDiagram
    participant App as Application
    participant PC as ProxyConnection
    participant PS as ProxyStatement/PreparedStatement  
    participant SR as SqlRewriter
    participant US as Underlying Statement
    participant DB as Database
    participant JL as JdbcLogger
    participant PRS as ProxyResultSet
    participant RP as ResultProcessor

    App->>PC: prepareStatement("SELECT * FROM users WHERE id = ?")
    PC->>SR: rewriteSql("SELECT * FROM users WHERE id = ?")
    SR-->>PC: rewritten SQL
    PC->>US: prepareStatement(rewrittenSql)
    US-->>PC: PreparedStatement
    PC->>PS: new ProxyPreparedStatement(underlying, originalSql, rewrittenSql)
    PC-->>App: ProxyPreparedStatement

    App->>PS: setInt(1, 123)
    PS->>US: setInt(1, 123)
    PS->>PS: track parameter: parameters[1] = 123

    App->>PS: executeQuery()
    PS->>US: executeQuery()
    US->>DB: execute rewritten SQL with parameters
    DB-->>US: ResultSet
    US-->>PS: ResultSet
    
    PS->>JL: logStatement(originalSql, rewrittenSql, parameters, ...)
    PS->>PRS: new ProxyResultSet(underlying, sql, connection, config)
    PS-->>App: ProxyResultSet

    loop For each row access
        App->>PRS: next()
        PRS->>RP: processRow(resultSet, sql, connection)
        PRS-->>App: boolean

        App->>PRS: getString("name")
        PRS->>RP: transformColumnValue(index, "name", value, ...)
        RP-->>PRS: transformed value
        PRS-->>App: String value
    end

    App->>PRS: close()
    PRS->>JL: logResultSetAccess(sql, connection, rowCount, accessedColumns)
```

## 4. Configuration System

### Configuration Lifecycle

```mermaid
stateDiagram-v2
    [*] --> Uninitialized
    
    Uninitialized --> DefaultConfig : ProxyDriver class loading
    DefaultConfig --> CustomConfig : setConfiguration()
    CustomConfig --> CustomConfig : addLogger/addRewriter/addProcessor
    
    DefaultConfig --> InUse : getConnection()
    CustomConfig --> InUse : getConnection()
    
    InUse --> InUse : JDBC operations
    InUse --> Cleanup : connection.close()
    
    Cleanup --> [*]
    
    note right of DefaultConfig : Empty configuration\nwith no hooks
    
    note right of CustomConfig : User-defined configuration\nwith hooks and processors
    
    note right of InUse : Configuration is immutable\nduring connection lifetime
```

### Hook Priority System

```mermaid
graph LR
    subgraph "SQL Rewriters (Execution Order)"
        SR1[SecurityRewriter<br/>Priority: 1000]
        SR2[TablePrefixRewriter<br/>Priority: 100] 
        SR3[CustomRewriter<br/>Priority: 50]
    end
    
    subgraph "Result Processors (Execution Order)"
        RP1[AuditProcessor<br/>Priority: 200]
        RP2[DataMaskingProcessor<br/>Priority: 50]
        RP3[CustomProcessor<br/>Priority: 10]
    end
    
    subgraph "JDBC Loggers (Execution Order)"
        JL1[FileLogger<br/>Priority: 100]
        JL2[ConsoleLogger<br/>Priority: 50]
        JL3[MetricsLogger<br/>Priority: 0]
    end
    
    SR1 --> SR2 --> SR3
    RP1 --> RP2 --> RP3  
    JL1 --> JL2 --> JL3
    
    Note1[Higher priority = Earlier execution]
```

## 5. Thread Safety Model

### Concurrency Design

```mermaid
graph TD
    subgraph "Static/Shared State"
        SC[Static Configurations<br/>ConcurrentHashMap]
        
        subgraph "Per-Configuration"
            HL[Hook Lists<br/>CopyOnWriteArrayList]
            CS[Config Settings<br/>volatile fields]
        end
    end
    
    subgraph "Per-Connection State"
        PC[ProxyConnection]
        PS[ProxyStatement]
        PRS[ProxyResultSet]
    end
    
    subgraph "Thread Safety Mechanisms"
        CWA[CopyOnWriteArrayList<br/>for hook collections]
        VOL[volatile fields<br/>for configuration state]
        IMM[Immutable objects<br/>for hook implementations]
    end
    
    SC --> HL
    HL --> CWA
    CS --> VOL
    
    PC --> HL
    PS --> HL
    PRS --> HL
    
    Note1[Multiple threads can safely:<br/>- Read configurations<br/>- Execute JDBC operations<br/>- Add/remove hooks]
```

## 6. Error Handling Strategy

### Exception Flow

```mermaid
graph TD
    JDBC[JDBC Operation] --> TRY{Try Block}
    TRY --> HOOKS[Execute Hooks]
    HOOKS --> HEXC{Hook Exception?}
    HEXC -->|Yes| HLOG[Log Hook Exception]
    HEXC -->|No| EXEC[Execute Underlying JDBC]
    HLOG --> EXEC
    
    EXEC --> JEXC{JDBC Exception?}
    JEXC -->|Yes| PROP[Propagate to Application]
    JEXC -->|No| SUCCESS[Return Result]
    
    SUCCESS --> FHOOKS[Execute Final Hooks]
    FHOOKS --> FEXC{Final Hook Exception?}
    FEXC -->|Yes| FLOG[Log Final Hook Exception]
    FEXC -->|No| RET[Return to Application]
    FLOG --> RET
    
    PROP --> APP[Application Handles]
    RET --> APP
    
    style HLOG fill:#ffcccc
    style FLOG fill:#ffcccc
    style PROP fill:#ffaaaa
    
    Note1[Hook exceptions never<br/>break JDBC operations]
```

## 7. Performance Characteristics

### Performance Profile

```mermaid
graph LR
    subgraph "No Hooks Configured"
        NH1[Connection: ~0ms overhead]
        NH2[Statement: ~0ms overhead]
        NH3[ResultSet: ~0ms overhead]
    end
    
    subgraph "Logging Only"
        L1[Connection: ~1ms overhead]
        L2[Statement: ~0.1ms overhead]
        L3[ResultSet: ~0.01ms per row]
    end
    
    subgraph "SQL Rewriting"
        R1[Statement Creation: ~0.5ms]
        R2[Regex Processing: ~0.1-1ms]
        R3[String Operations: ~0.1ms]
    end
    
    subgraph "Result Processing"  
        RP1[Row Processing: ~0.01ms]
        RP2[Value Transform: ~0.001ms]
        RP3[Column Access Track: ~0.001ms]
    end
    
    Note1[Overhead scales linearly<br/>with number of hooks]
    Note2[JVM optimization reduces<br/>overhead after warm-up]
```

### Memory Usage

```mermaid
graph TB
    subgraph "Per-JVM Memory"
        SC[Static Configurations<br/>~1KB per config]
        HL[Hook Lists<br/>~100B per hook]
    end
    
    subgraph "Per-Connection Memory"
        PC[ProxyConnection<br/>~500B overhead]
        PS[ProxyStatement<br/>~200B overhead]
    end
    
    subgraph "Per-ResultSet Memory"
        PRS[ProxyResultSet<br/>~300B overhead]
        CA[Column Access Tracking<br/>~20B per column]
        RC[Row Count Tracking<br/>~8B]
    end
    
    subgraph "Hook-Specific Memory"
        PM[Parameter Map<br/>~50B per parameter]
        LB[Log Buffers<br/>~1KB per statement]
    end
    
    Note1[Memory usage is minimal<br/>and proportional to activity]
```

## 8. Deployment Architecture

### Embedded Library Model

```mermaid
graph TB
    subgraph "Application JVM Process"
        APP[Application Code]
        
        subgraph "Application Classpath"
            PROXY[JDBC Proxy Driver JAR]
            HOOKS[Custom Hook Classes]
            UNDER[Underlying JDBC Drivers]
        end
        
        subgraph "Memory Layout"
            HEAP[JVM Heap]
            CODE[Method Area/Code Cache]
        end
    end
    
    subgraph "External Systems"
        DB1[(Primary Database)]
        DB2[(Secondary Database)]
        LOG[Log Files/Services]
        MON[Monitoring Systems]
    end
    
    APP --> PROXY
    PROXY --> HOOKS
    PROXY --> UNDER
    
    PROXY --> HEAP
    PROXY --> CODE
    
    UNDER --> DB1
    UNDER --> DB2
    HOOKS --> LOG
    HOOKS --> MON
    
    Note1[No separate processes<br/>No network overhead<br/>Simple deployment]
```

### Multi-Environment Configuration

```mermaid
graph TB
    subgraph "Development Environment"
        DEV[dev-config]
        DEV --> DEVLOG[Verbose Console Logging]
        DEV --> DEVPREF[dev_ Table Prefixes]
        DEV --> DEVSQL[SQL Query Logging]
    end
    
    subgraph "Testing Environment" 
        TEST[test-config]
        TEST --> TESTLOG[File Logging]
        TEST --> TESTPREF[test_ Table Prefixes]
        TEST --> TESTMASK[Limited Data Masking]
    end
    
    subgraph "Production Environment"
        PROD[prod-config]
        PROD --> PRODLOG[Structured Logging]
        PROD --> PRODMASK[Full Data Masking]
        PROD --> PRODAUDIT[Audit Logging]
        PROD --> PRODMON[Metrics Collection]
    end
    
    APP[Application] --> |proxy.configKey=dev| DEV
    APP --> |proxy.configKey=test| TEST  
    APP --> |proxy.configKey=prod| PROD
    
    Note1[Same codebase<br/>Different configurations<br/>Environment-specific behavior]
```

## 9. Extension Points

### Plugin Architecture

```mermaid
classDiagram
    class HookInterface {
        <<interface>>
        +getPriority() int
    }
    
    class JdbcLogger {
        <<interface>>
        +logConnection(...)
        +logStatement(...)
        +logTransaction(...)
        +logBatch(...)
    }
    
    class SqlRewriter {
        <<interface>>
        +rewriteSql(sql, connection) String
        +rewriteParameterizedSql(sql, params, conn) String
    }
    
    class ResultProcessor {
        <<interface>>
        +processRow(resultSet, sql, connection)
        +onResultSetStart(...)
        +onResultSetEnd(...)
        +transformColumnValue(...)
    }
    
    HookInterface <|-- JdbcLogger
    HookInterface <|-- SqlRewriter
    HookInterface <|-- ResultProcessor
    
    class CustomExtension1 {
        +getPriority() int
        +customMethod()
    }
    
    class CustomExtension2 {
        +getPriority() int
        +anotherCustomMethod()
    }
    
    JdbcLogger <|.. CustomExtension1
    SqlRewriter <|.. CustomExtension2
    
    Note1[Extensions implement interfaces<br/>and are automatically integrated<br/>via configuration system]
```

## 10. Security Considerations

### Security Boundaries

```mermaid
graph TB
    subgraph "Application Security Context"
        APP[Application Code]
        CREDS[Database Credentials]
    end
    
    subgraph "Proxy Security Controls"
        MASK[Data Masking]
        AUDIT[Audit Logging]
        REWRITE[SQL Rewriting]
        PARAM[Parameter Validation]
    end
    
    subgraph "Database Security"
        DB[Database]
        AUTH[Authentication]
        AUTHZ[Authorization]
        ENCRYPT[Encryption at Rest/Transit]
    end
    
    APP --> CREDS
    CREDS --> AUTH
    
    APP --> MASK
    APP --> AUDIT
    APP --> REWRITE
    APP --> PARAM
    
    MASK --> DB
    AUDIT --> LOG[Audit Logs]
    REWRITE --> DB
    PARAM --> DB
    
    DB --> AUTHZ
    DB --> ENCRYPT
    
    Note1[Proxy provides additional<br/>security layer without<br/>replacing database security]
```

## 11. Monitoring and Observability

### Observable Events

```mermaid
graph LR
    subgraph "Connection Events"
        CE1[Connection Established]
        CE2[Connection Failed]
        CE3[Connection Closed]
    end
    
    subgraph "Statement Events"
        SE1[Statement Prepared]
        SE2[Statement Executed]
        SE3[Statement Failed]
        SE4[Batch Executed]
    end
    
    subgraph "Transaction Events"
        TE1[Transaction Started]
        TE2[Transaction Committed]
        TE3[Transaction Rolled Back]
        TE4[Savepoint Created]
    end
    
    subgraph "Result Events"
        RE1[ResultSet Opened]
        RE2[Rows Processed]
        RE3[Columns Accessed]
        RE4[ResultSet Closed]
    end
    
    subgraph "Hook Events"
        HE1[SQL Rewritten]
        HE2[Data Masked]
        HE3[Hook Failed]
    end
    
    CE1 --> LOGS[Structured Logs]
    SE1 --> METRICS[Metrics/Monitoring]
    TE1 --> TRACES[Distributed Tracing]
    RE1 --> AUDIT[Audit Events]
    HE1 --> ALERTS[Alert Systems]
```

This comprehensive design document provides the technical foundation for understanding, extending, and operating the JDBC Proxy Driver in production environments.
