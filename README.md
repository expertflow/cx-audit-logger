# CX Audit Logger

CX Audit Logger is a high-performance, framework-agnostic audit logging library designed for modern Java applications. It produces structured JSON-formatted logs and includes advanced features for deep recursive diffing and support for asynchronous or virtual thread environments.

## Features

- **Zero-Config Async Support**: Preserves correct Class, Method, and Line numbers even when logging from `@Async` or Virtual Threads (requires Logback).
- **Deep Recursive Diffing**: Built-in utility to calculate the difference between "Old" and "New" objects. It minimizes log volume by only recording what actually changed.
- **High Performance**: Uses cached reflection handles to inject caller data with near-zero overhead.
- **JSON-formatted output**: Structured logging for better analysis (ELK/OpenSearch compatible).
- **Simple API**: Dual-mode API (Sync & Async) for maximum flexibility.
- **Robust error handling**: "Never throws" design - errors are logged safely rather than propagated.

## Installation

### Maven

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.expertflow</groupId>
    <artifactId>cx-audit-logger</artifactId>
    <version>1.2.0</version>
</dependency>
```

## Usage

### 1. Standard Synchronous Usage
For standard synchronous code, usage remains unchanged. The library automatically detects the calling class, method, and line number.

```java
import com.ef.auditlogger.AuditLogger;
import com.ef.auditlogger.dtos.AuditInput;

public class UserService {
    private final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final String FQCN = UserService.class.getName();
    // Inject or instantiate
    private final AuditLogger auditLogger = new AuditLogger(new ObjectMapper());

    public void createUser() {
        AuditInput input = AuditInput.builder()
                .userId("123")
                .action("CREATE")
                .resource("User")
                .updatedData(Map.of("role", "admin"))
                .build();

        // Standard call - Auto-detects stack trace
        auditLogger.log(logger, input, FQCN);
    }
}
```

### 2. Asynchronous / Virtual Thread Usage
In `@Async` methods or Virtual Threads, the standard stack trace is lost (pointing to internal JVM proxy classes). To fix this, capture the stack frame in the main thread and pass it to the logger.

The library uses optimized reflection to inject this data into Logback, ensuring your logs show the original service location instead of `CompletableFuture` or `DirectMethodHandleAccessor`.

```java
public class AsyncWorker {
    
    public void processAsync() {
        // 1. Capture the caller frame in the MAIN thread
        StackTraceElement caller = Thread.currentThread().getStackTrace()[1];
        
        // 2. Pass it to your async method/thread
        CompletableFuture.runAsync(() -> {
             AuditInput input = AuditInput.builder().action("UPDATE").build();
             
             // 3. Call the overloaded log method
             auditLogger.log(logger, input, FQCN, caller); 
        });
    }
}
```

### 3. Using the Diff Calculator
The library now includes `AuditDiffCalculator` to generate minimal JSON diffs. It handles nested objects, lists, and identity matching (by `id` or `key`).

```java
import com.ef.auditlogger.utils.AuditDiffCalculator;

AuditDiffCalculator calculator = new AuditDiffCalculator(new ObjectMapper());

Map<String, Object> oldData = Map.of("status", "ACTIVE", "retries", 0);
Map<String, Object> newData = Map.of("status", "INACTIVE", "retries", 0);

// Returns only: {"status": "INACTIVE"}
Object diff = calculator.calculateDiff(oldData, newData); 

AuditInput input = AuditInput.builder()
        .updatedData(diff)
        .build();
```

## Spring Boot Configuration

Define the beans in your configuration:

```java
@Configuration
public class AuditConfig {

    @Bean
    public AuditLogger auditLogger(ObjectMapper objectMapper) {
        return new AuditLogger(objectMapper);
    }

    @Bean
    public AuditDiffCalculator auditDiffCalculator(ObjectMapper objectMapper) {
        return new AuditDiffCalculator(objectMapper);
    }
}
```

## Output Format

The logger generates structured JSON. When using the Diff Calculator, the `updated_data` field contains only changed values.

```json
{
  "timestamp": "2026-02-03T14:35:00.123Z",
  "user_id": "123",
  "action": "UPDATE",
  "resource": "ChannelConnector",
  "resource_id": "conn-1",
  "attributes": {
    "service": "ChannelManager",
    "tenantId": "expertflow",
    "updated_data": {
      "channelProviderConfigs": [
        {
          "key": "SMTP-PORT",
          "value": 587
        }
      ]
    }
  },
  "type": "audit_logging",
  "level": "info"
}
```

## API Documentation

### `AuditLogger` Class

*   **`log(Logger logger, AuditInput input, String fqcn)`**
    *   Standard method. Uses SLF4J `LocationAwareLogger` to auto-detect caller info.
*   **`log(Logger logger, AuditInput input, String fqcn, StackTraceElement caller)`**
    *   Manually injects the provided `StackTraceElement` into the logging event.
    *   Supports standard Logback features (`%class`, `%method`, `%line`) even when running on Virtual Threads.
    *   Falls back gracefully to standard logging if Reflection fails or if not using Logback.

### `AuditDiffCalculator` Class

*   **`calculateDiff(Object oldData, Object newData)`**
    *   Recursively compares two objects.
    *   Returns `null` or empty Map if objects are identical.
    *   Smart-matches List items by `id` or `key` fields.
    *   Converts JSON primitives (IntNode, TextNode) to Java primitives (Integer, String) for cleaner logs.

## Requirements

*   **Java**: 17 or higher.
*   **Logging Framework**: SLF4J 2.0+.
    *   *Note:* The "Async Caller Injection" feature works best with **Logback Classic**. If using Log4j2 or others, the library falls back to standard logging (JSON is preserved, but line numbers in async threads may point to the proxy).

## License

This project is licensed under the Apache License, Version 2.0.

## Author

- **Azan Rashid** - [ExpertFlow](https://www.expertflow.com/)