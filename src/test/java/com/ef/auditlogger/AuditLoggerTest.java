package com.ef.auditlogger;

import com.ef.auditlogger.dtos.AuditInput;
import com.ef.auditlogger.models.AuditLogPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
                .tenantId("expertflow")
                .service("UserService")
                .updatedData(Map.of("role", "admin"))
                .type("audit_logging")
                .level("info")
                .build();

        // Act
        auditLogger.log(mockLogger, input, this.getClass().getName());

        // Assert
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockLogger).info(captor.capture());
        verify(mockLogger, never()).error(anyString(), any(Throwable.class));

        String loggedJson = captor.getValue();
        assertNotNull(loggedJson);

        AuditLogPayload loggedPayload = realObjectMapper.readValue(loggedJson, AuditLogPayload.class);
        assertEquals("u123", loggedPayload.getUserId());
        assertEquals("CREATE", loggedPayload.getAction());
        assertEquals("UserService", loggedPayload.getAttributes().get("service"));
    }

    @Test
    void log_shouldLogError_whenSerializationFails() throws JsonProcessingException {
        // Arrange
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        JsonProcessingException testException = new JsonProcessingException("Test Serialization Failure") {};
        when(failingMapper.writeValueAsString(any())).thenThrow(testException);

        AuditLogger failingLogger = new AuditLogger(failingMapper);
        AuditInput input = AuditInput.builder()
                .userId("u123")
                .type("audit_logging")
                .level("info")
                .build();

        // Act
        assertDoesNotThrow(() -> {
            failingLogger.log(mockLogger, input, this.getClass().getName());
        });

        // Assert
        verify(mockLogger).error(eq("Audit logging failed"), eq(testException));
        verify(mockLogger, never()).info(anyString());
    }

    @Test
    void log_shouldLogError_whenInputIsNull() {
        // Act
        assertDoesNotThrow(() -> {
            auditLogger.log(mockLogger, null, this.getClass().getName());
        });

        // Assert
        verify(mockLogger).error(eq("Audit logging failed"), any(NullPointerException.class));
        verify(mockLogger, never()).info(anyString());
    }

    @Test
    void log_shouldLogError_whenInputCausesOtherRuntimeException() {
        // Arrange
        AuditInput buggyInput = mock(AuditInput.class);
        RuntimeException testException = new IllegalStateException("Unexpected bug!");

        when(buggyInput.getService()).thenThrow(testException);

        // Act
        assertDoesNotThrow(() -> {
            auditLogger.log(mockLogger, buggyInput, this.getClass().getName());
        });

        // Assert
        verify(mockLogger).error(eq("Audit logging failed"), eq(testException));
        verify(mockLogger, never()).info(anyString());
    }
}