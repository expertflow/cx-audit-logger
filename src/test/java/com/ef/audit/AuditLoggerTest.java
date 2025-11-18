package com.ef.audit;

import com.ef.audit.dtos.AuditInput;
import com.ef.audit.models.AuditLogPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the provided AuditLogger implementation.
 * This test class validates the "Never Throws" behavior and correct payload generation.
 */
@ExtendWith(MockitoExtension.class)
class AuditLoggerTest {

    @Mock
    private Logger mockLogger;

    private final ObjectMapper realObjectMapper = new ObjectMapper();

    private AuditLogger auditLogger;

    @BeforeEach
    void setUp() {
        auditLogger = new AuditLogger(realObjectMapper);
    }

    @Test
    void log_shouldLogCorrectlyFormattedPayload_whenInputIsValid() throws JsonProcessingException {
        // Arrange
        AuditInput input = AuditInput.builder()
                .userId("u123")
                .userName("John Doe")
                .action("CREATE")
                .resource("User")
                .resourceId("user-456")
                .ip("192.168.1.1")
                .service("UserService")
                .updatedData(Map.of("role", "admin"))
                .build();

        // Act
        auditLogger.log(mockLogger, input);

        // Assert
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockLogger).info(captor.capture());
        verify(mockLogger, never()).error(anyString(), any(), any());

        String loggedJson = captor.getValue();
        assertNotNull(loggedJson);

        AuditLogPayload loggedPayload = realObjectMapper.readValue(loggedJson, AuditLogPayload.class);
        assertEquals("u123", loggedPayload.getUserId());
        assertEquals("CREATE", loggedPayload.getAction());
        assertEquals("UserService", loggedPayload.getAttributes().get("service"));
        assertEquals("audit_logging", loggedPayload.getType());
    }

    @Test
    void log_shouldLogError_whenSerializationFails() throws JsonProcessingException {
        // Arrange
        // Create a mocked ObjectMapper that is programmed to fail
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        JsonProcessingException testException = new JsonProcessingException("Test Serialization Failure") {};
        when(failingMapper.writeValueAsString(any())).thenThrow(testException);

        AuditLogger failingLogger = new AuditLogger(failingMapper);
        AuditInput input = AuditInput.builder().userId("u123").build();

        // Act
        assertDoesNotThrow(() -> {
            failingLogger.log(mockLogger, input);
        });

        // Assert
        verify(mockLogger).error("Failed to serialize audit log: {}, Error: {}", input, testException.getMessage());
        verify(mockLogger, never()).info(anyString());
    }

    @Test
    void log_shouldLogError_whenInputIsNull() {
        AuditInput nullInput = null;

        assertDoesNotThrow(() -> {
            auditLogger.log(mockLogger, nullInput);
        });

        verify(mockLogger).error(eq("Unexpected error during audit logging: {}"), anyString());
        verify(mockLogger, never()).info(anyString());
    }

    @Test
    void log_shouldLogError_whenInputCausesOtherRuntimeException() {
        AuditInput buggyInput = mock(AuditInput.class);
        RuntimeException testException = new IllegalStateException("Unexpected bug!");
        when(buggyInput.getService()).thenThrow(testException);

        assertDoesNotThrow(() -> {
            auditLogger.log(mockLogger, buggyInput);
        });

        verify(mockLogger).error("Unexpected error during audit logging: {}", testException.getMessage());
        verify(mockLogger, never()).info(anyString());
    }

    @Test
    void testLogIncludesCallerInfo() {
        AuditInput input = AuditInput.builder()
                .userId("123")
                .userName("testUser")
                .action("CREATE")
                .resource("User")
                .resourceId("456")
                .ip("127.0.0.1")
                .service("testService")
                .build();

        auditLogger.log(mockLogger, input);

        verify(mockLogger).info(anyString());
    }
}