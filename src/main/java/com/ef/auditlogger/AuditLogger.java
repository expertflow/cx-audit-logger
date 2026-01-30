package com.ef.auditlogger;

import com.ef.auditlogger.dtos.AuditInput;
import com.ef.auditlogger.models.AuditLogPayload;
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
    public static final String AUDIT_LOGGING = "audit_logging";
    private final ObjectMapper objectMapper;

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
    public void log(Logger logger, AuditInput input, String fqcn) {
        try {
            Map<String, Object> attributes = new HashMap<>();
            if (input.getService() != null) {
                attributes.put("service", input.getService());
            }

            if (input.getTenantId() != null) {
                attributes.put("tenantId", input.getTenantId());
            }

            attributes.put("updated_data",
                    input.getUpdatedData() != null ? input.getUpdatedData() : Map.of()
            );

            int level;

            if (input.getLevel() == null) {
                input.setLevel("INFO");
            }

            switch (input.getLevel().toUpperCase()) {
                case "TRACE" -> level = LocationAwareLogger.TRACE_INT;
                case "DEBUG" -> level = LocationAwareLogger.DEBUG_INT;
                case "WARN" -> level = LocationAwareLogger.WARN_INT;
                case "ERROR" -> level = LocationAwareLogger.ERROR_INT;
                default -> level = LocationAwareLogger.INFO_INT;
            }

            AuditLogPayload payload = AuditLogPayload.builder()
                    .timestamp(Instant.now().toString())
                    .type(sanitizeType(input.getType()))
                    .level(input.getLevel().toLowerCase())
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
                        fqcn,
                        level,
                        jsonMessage,
                        null,
                        null
                );
            } else {
                if (level == LocationAwareLogger.ERROR_INT) {
                    logger.error(jsonMessage);
                } else if (level == LocationAwareLogger.WARN_INT) {
                    logger.warn(jsonMessage);
                } else if (level == LocationAwareLogger.DEBUG_INT) {
                    logger.debug(jsonMessage);
                } else if (level == LocationAwareLogger.TRACE_INT) {
                    logger.trace(jsonMessage);
                } else {
                    logger.info(jsonMessage);
                }
            }
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize audit log: {}, Error: {}", input, e.getMessage());
        } catch (RuntimeException e) {
            logger.error("Unexpected error during audit logging: {}", e.getMessage());
        }
    }

    /**
     * Enforces naming convention: audit_logging, metrics, or tracing
     */
    private String sanitizeType(String inputType) {
        if (inputType == null) return AUDIT_LOGGING;

        String normalized = inputType.toLowerCase().trim();

        if (normalized.contains("audit")) return AUDIT_LOGGING;
        if (normalized.contains("metric")) return "metrics";
        if (normalized.contains("trace") || normalized.contains("tracing")) return "tracing";

        return AUDIT_LOGGING;
    }
}