package com.ef.audit;

import com.ef.audit.dtos.AuditInput;
import com.ef.audit.models.AuditLogPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.spi.LocationAwareLogger;

/**
 * An ultra-minimal audit logger with a single, convenient method.
 * This class is framework-agnostic and must be instantiated by the user.
 */
public class AuditLogger {
    private final ObjectMapper objectMapper;

    private static final String FQCN = AuditLogger.class.getName();

    public AuditLogger(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * The single, convenient method to create and write an audit log entry.
     * It takes a simple input object, transforms it into the full log payload,
     * and writes it to the log.
     *
     * @param logger The SLF4J logger instance from the calling class.
     * @param input  The simple object containing the necessary audit data.
     */
    public void log(Logger logger, AuditInput input) {
        try {
            Map<String, Object> attributes = new HashMap<>();
            if (input.getService() != null) {
                attributes.put("service", input.getService());
            }

            attributes.put("updated_data",
                    input.getUpdatedData() != null ? input.getUpdatedData() : Map.of()
            );

            AuditLogPayload payload = AuditLogPayload.builder()
                    .timestamp(Instant.now().toString())
                    .type("audit_logging")
                    .level("info")
                    .userId(input.getUserId())
                    .userName(input.getUserName())
                    .action(input.getAction())
                    .resource(input.getResource())
                    .resourceId(input.getResourceId())
                    .sourceIpAddress(input.getIp())
                    .attributes(attributes)
                    .build();

            String jsonMessage = objectMapper.writeValueAsString(payload);

            if (logger instanceof LocationAwareLogger locationAwareLogger) {
                locationAwareLogger.log(
                        null,
                        FQCN,
                        LocationAwareLogger.INFO_INT,
                        jsonMessage,
                        null,
                        null
                );
            } else {
                logger.info(jsonMessage);
            }
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize audit log: {}, Error: {}", input, e.getMessage());
        } catch (RuntimeException e) {
            logger.error("Unexpected error during audit logging: {}", e.getMessage());
        }
    }
}