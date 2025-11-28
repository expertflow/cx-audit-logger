# EFCX JSON Logger

EFCX JSON Logger is a lightweight, framework-agnostic audit logging library that produces structured JSON-formatted logs for enhanced log analysis and monitoring. This library was developed for ExpertFlow's CIM project to standardize audit logging across services.

## Features

- **Simple API**: Single method for audit logging with minimal configuration
- **JSON-formatted output**: Structured logging for better analysis and parsing
- **Thread-safe implementation**: Safe to use in multi-threaded environments
- **SLF4J integration**: Works with any SLF4J-compatible logging framework
- **Flexible attribute mapping**: Support for custom data fields via attributes map
- **Robust error handling**: "Never throws" design - errors are logged rather than propagated
- **Lombok integration**: Clean, concise DTOs and models

## Installation

### Maven

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.expertflow</groupId>
    <artifactId>cx-audit-logger</artifactId>
    <version>1.0</version>
</dependency>
```

### Gradle

If using Gradle, add to your `build.gradle`:

## Usage

### Basic Usage

```java
import com.ef.auditlogger.AuditLogger;
import com.ef.auditlogger.dtos.AuditInput;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyService {
    private final Logger logger = LoggerFactory.getLogger(MyService.class);
    private final AuditLogger auditLogger = new AuditLogger(new ObjectMapper());

    public void performAction() {
        AuditInput input = AuditInput.builder()
                .userId("123")
                .userName("John Doe")
                .action("CREATE")
                .resource("User")
                .resourceId("456")
                .ip("192.168.1.1")
                .service("UserService")
                .updatedData(Map.of("name", "New User", "role", "admin"))
                .build();

        auditLogger.log(logger, input);
    }
}
```

### Spring Boot Configuration

If using Spring Boot, you can define the AuditLogger as a bean.

```java
@Configuration
public class AuditLoggerConfig {
    
    @Bean
    public AuditLogger auditLogger(ObjectMapper objectMapper) {
        return new AuditLogger(objectMapper);
    }
}
```

Then inject it into your service:

```java
@Service
public class MyService {
    private final Logger logger = LoggerFactory.getLogger(MyService.class);
    private final AuditLogger auditLogger;

    public MyService(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    public void performAction() {
        AuditInput input = AuditInput.builder()
            .userId("123")
            .userName("John Doe")
            .action("CREATE")
            .resource("User")
            .resourceId("456")
            .ip("192.168.1.1")
            .service("UserService")
            .updatedData(Map.of("name", "New User", "role", "admin"))
            .build();

        auditLogger.log(logger, input);
    }
}
```

### Advanced Usage with Custom Data

```java
AuditInput input = AuditInput.builder()
    .userId("user-789")
    .userName("Jane Smith")
    .action("UPDATE")
    .resource("Profile")
    .resourceId("profile-101")
    .ip("203.0.113.5")
    .service("ProfileService")
    .updatedData(Map.of(
        "name", "Updated Name",
        "permissions", List.of("read", "write"),
        "settings", Map.of("theme", "dark", "notifications", true)
    ))
    .build();

auditLogger.log(logger, input);
```

## Output Format

The logger generates structured JSON logs with the following format:

```json
{
  "timestamp": "2025-09-02T14:35:00.123Z",
  "user_id": "123",
  "user_name": "John Doe",
  "action": "CREATE",
  "resource": "User",
  "resource_id": "456",
  "source_ip_address": "192.168.1.1",
  "attributes": {
    "service": "UserService",
    "updated_data": {
      "name": "New User",
      "role": "admin"
    }
  },
  "type": "audit_logging",
  "level": "info"
}
```

## API Documentation

### AuditLogger Class

The main class with a single public method:

- `log(Logger logger, AuditInput input)`: Creates and writes an audit log entry

### AuditInput DTO

Contains the essential details for audit logging:

- `userId`: Unique identifier of the user
- `userName`: Name of the user
- `action`: Type of action performed (e.g., "CREATE", "UPDATE", "DELETE")
- `resource`: Type of resource acted upon (e.g., "Team", "UserProfile")
- `resourceId`: Unique identifier of the resource
- `ip`: IP address of the request origin
- `service`: Name of the service performing the action
- `updatedData`: Flexible object containing the changed data

## Dependencies

This library includes:

- **SLF4J API**: For logging abstraction (version 2.0.9)
- **Jackson Databind**: For JSON serialization (version 2.15.3)
- **Jackson Annotations**: For JSON annotations (version 2.15.3)
- **Lombok**: For code generation (version 1.18.30)
- **Logback Classic**: For default logging implementation (version 1.5.13)

## Testing

The library includes comprehensive unit tests using:

- JUnit 5 for test framework
- Mockito for mocking dependencies
- Real ObjectMapper instances for serialization testing

To run tests:
```bash
mvn test
```

## Configuration

The logger uses any SLF4J-compatible logging framework. Configure your chosen logging framework as needed. The library itself requires no additional configuration.

## License

This project is licensed under the Apache License, Version 2.0 - see the [LICENSE](LICENSE) file for details.

## Author

- **Azan Rashid** - [ExpertFlow](https://www.expertflow.com/)

## Development

### Prerequisites

- Java 17 or higher
- Maven 3.6.0 or higher

### Building

To build the project:
```bash
mvn clean install
```

### Publishing

This library is published to Maven Central. The publishing process is automated through the Sonatype Central Publishing Plugin.