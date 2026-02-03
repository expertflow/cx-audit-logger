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

public class AuditLogger {
    public static final String AUDIT_LOGGING = "audit_logging";
    private final ObjectMapper objectMapper;

    private static final LogbackReflector REFLECTOR = new LogbackReflector();

    public AuditLogger(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Legacy support
     */
    public void log(Logger logger, AuditInput input, String fqcn) {
        log(logger, input, fqcn, null);
    }

    /**
     * Enhanced log method supporting manual StackTraceElement injection
     */
    public void log(Logger logger, AuditInput input, String fqcn, StackTraceElement caller) {
        try {
            String jsonMessage = buildJsonMessage(input);

            if (caller != null && REFLECTOR.isAvailable() && isLogback(logger)) {
                REFLECTOR.log(logger, input.getLevel(), jsonMessage, caller);
                return;
            }

            int levelInt = getSlf4jLevel(input.getLevel());
            if (logger instanceof LocationAwareLogger locationAwareLogger) {
                locationAwareLogger.log(null, fqcn, levelInt, jsonMessage, null, null);
            } else {
                logStandard(logger, levelInt, jsonMessage);
            }
        } catch (Exception e) {
            logger.error("Audit logging failed", e);
        }
    }

    private String buildJsonMessage(AuditInput input) throws JsonProcessingException {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("service", input.getService());
        attributes.put("tenantId", input.getTenantId());
        attributes.put("updated_data", input.getUpdatedData() != null ? input.getUpdatedData() : Map.of());

        AuditLogPayload payload = AuditLogPayload.builder()
                .timestamp(Instant.now().toString())
                .type(sanitizeType(input.getType()))
                .level(input.getLevel() != null ? input.getLevel().toLowerCase() : "info")
                .userId(input.getUserId())
                .userName(input.getUserName())
                .action(input.getAction())
                .resource(input.getResource())
                .resourceId(input.getResourceId())
                .sourceIpAddress(input.getIp())
                .attributes(attributes)
                .build();

        return objectMapper.writeValueAsString(payload);
    }

    private boolean isLogback(Logger logger) {
        return logger.getClass().getName().startsWith("ch.qos.logback");
    }

    private int getSlf4jLevel(String levelStr) {
        if (levelStr == null) return LocationAwareLogger.INFO_INT;
        return switch (levelStr.toUpperCase()) {
            case "TRACE" -> LocationAwareLogger.TRACE_INT;
            case "DEBUG" -> LocationAwareLogger.DEBUG_INT;
            case "WARN" -> LocationAwareLogger.WARN_INT;
            case "ERROR" -> LocationAwareLogger.ERROR_INT;
            default -> LocationAwareLogger.INFO_INT;
        };
    }

    private void logStandard(Logger logger, int level, String msg) {
        switch (level) {
            case LocationAwareLogger.ERROR_INT -> logger.error(msg);
            case LocationAwareLogger.WARN_INT -> logger.warn(msg);
            case LocationAwareLogger.DEBUG_INT -> logger.debug(msg);
            default -> logger.info(msg);
        }
    }

    private String sanitizeType(String inputType) {
        if (inputType == null) return AUDIT_LOGGING;
        String normalized = inputType.toLowerCase().trim();
        if (normalized.contains("audit")) return AUDIT_LOGGING;
        if (normalized.contains("metric")) return "metrics";
        if (normalized.contains("trace")) return "tracing";
        return AUDIT_LOGGING;
    }
}