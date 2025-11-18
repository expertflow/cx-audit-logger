package com.ef.audit.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * A data structure representing the JSON payload of an audit log entry.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogPayload {

    /**
     * The timestamp of the event in ISO 8601 format.
     * Example: "2025-09-02T14:35:00.123Z"
     */
    private String timestamp;

    /**
     * The unique identifier of the user who performed the action.
     * The @JsonProperty annotation maps the Java field 'userId' to the JSON key 'user_id'.
     */
    @JsonProperty("user_id")
    private String userId;

    /**
     * The name of the user who performed the action.
     * Maps the Java field 'userName' to the JSON key 'user_name'.
     */
    @JsonProperty("user_name")
    private String userName;

    /**
     * The type of action performed.
     * Example: "CREATE", "UPDATE", "DELETE", "LOGIN_SUCCESS", "EXPORT"
     */
    private String action;

    /**
     * The type of resource that was acted upon.
     * Example: "Team", "UserProfile", "Document"
     */
    private String resource;

    /**
     * The unique identifier of the resource that was acted upon.
     * Maps the Java field 'resourceId' to the JSON key 'resource_id'.
     */
    @JsonProperty("resource_id")
    private String resourceId;

    /**
     * The IP address from which the request originated.
     * Maps the Java field 'sourceIpAddress' to the JSON key 'source_ip_address'.
     */
    @JsonProperty("source_ip_address")
    private String sourceIpAddress;

    /**
     * A flexible map for additional, nested context.
     * Using Map<String, Object> allows for values that are strings, numbers, or even other nested objects.
     * Example: {"service": "unified_admin", "updated_data": {"name": "EFCX-team"}}
     */
    private Map<String, Object> attributes;

    /**
     * A fixed string to identify the log type.
     * Example: "audit_logging"
     */
    private String type;

    /**
     * The severity level of the log.
     * Example: "info", "warn", "error"
     */
    private String level;
}