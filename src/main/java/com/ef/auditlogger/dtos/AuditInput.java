package com.ef.auditlogger.dtos;

import lombok.Builder;
import lombok.Data;

/**
 * A simple data object for providing the essential details for an audit log entry.
 */
@Data
@Builder
public class AuditInput {
    private String userId;
    private String userName;
    private String action;
    private String resource;
    private String resourceId;
    private String ip;
    private String service;
    private Object updatedData;
}