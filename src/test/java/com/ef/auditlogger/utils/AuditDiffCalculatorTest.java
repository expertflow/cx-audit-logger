package com.ef.auditlogger.utils;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuditDiffCalculatorTest {

    private AuditDiffCalculator calculator;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        calculator = new AuditDiffCalculator(mapper);
    }

    @Test
    @DisplayName("Should detect simple primitive field changes")
    void testSimpleFieldChange() {
        Map<String, Object> oldData = Map.of("name", "John", "age", 30);
        Map<String, Object> newData = Map.of("name", "John", "age", 31);

        Object result = calculator.calculateDiff(oldData, newData);

        assertTrue(result instanceof Map);
        Map<String, Object> diff = (Map<String, Object>) result;
        
        assertEquals(31, diff.get("age"));
        assertFalse(diff.containsKey("name"), "Unchanged fields should be excluded");
    }

    @Test
    @DisplayName("Should detect changes in nested objects")
    void testNestedObjectChange() {
        Map<String, Object> oldData = Map.of("user", Map.of("id", 1, "status", "ACTIVE"));
        Map<String, Object> newData = Map.of("user", Map.of("id", 1, "status", "INACTIVE"));

        Object result = calculator.calculateDiff(oldData, newData);

        Map<String, Object> diff = (Map<String, Object>) result;
        assertTrue(diff.containsKey("user"));
        
        Map<String, Object> userDiff = (Map<String, Object>) diff.get("user");
        assertEquals("INACTIVE", userDiff.get("status"));
        assertFalse(userDiff.containsKey("id"), "Unchanged nested fields should be excluded");
    }

    @Test
    @DisplayName("Should handle list diffs using 'key' for identity matching")
    void testListDiffWithKeyIdentity() {
        List<Map<String, Object>> oldConfigs = List.of(
                Map.of("key", "TIMEOUT", "value", 100),
                Map.of("key", "RETRY", "value", 3)
        );
        List<Map<String, Object>> newConfigs = List.of(
                Map.of("key", "TIMEOUT", "value", 500), // Changed
                Map.of("key", "RETRY", "value", 3)      // Unchanged
        );

        Object result = calculator.calculateDiff(Map.of("cfg", oldConfigs), Map.of("cfg", newConfigs));

        Map<String, Object> diff = (Map<String, Object>) result;
        List<Map<String, Object>> cfgDiff = (List<Map<String, Object>>) diff.get("cfg");

        assertEquals(1, cfgDiff.size(), "Only the changed list item should be present");
        assertEquals("TIMEOUT", cfgDiff.get(0).get("key"), "Identity metadata 'key' should be injected");
        assertEquals(500, cfgDiff.get(0).get("value"));
    }

    @Test
    @DisplayName("Should handle list diffs using 'id' for identity matching")
    void testListDiffWithIdIdentity() {
        List<Map<String, Object>> oldItems = List.of(Map.of("id", "uuid-1", "val", "A"));
        List<Map<String, Object>> newItems = List.of(Map.of("id", "uuid-1", "val", "B"));

        Object result = calculator.calculateDiff(Map.of("items", oldItems), Map.of("items", newItems));

        Map<String, Object> diff = (Map<String, Object>) result;
        List<Map<String, Object>> itemsDiff = (List<Map<String, Object>>) diff.get("items");

        assertEquals("uuid-1", itemsDiff.get(0).get("id"), "Identity metadata 'id' should be injected");
        assertEquals("B", itemsDiff.get(0).get("val"));
    }

    @Test
    @DisplayName("Should return null when there are no changes")
    void testNoChanges() {
        Map<String, Object> data = Map.of("a", 1, "b", List.of(1, 2));
        
        Object result = calculator.calculateDiff(data, data);

        assertNull(result, "Should return null if objects are identical");
    }

    @Test
    @DisplayName("Should convert Jackson nodes to Java primitives in output")
    void testPrimitiveConversion() {
        Map<String, Object> oldData = Map.of("val", 10);
        Map<String, Object> newData = Map.of("val", 20);

        Object result = calculator.calculateDiff(oldData, newData);
        Map<String, Object> diff = (Map<String, Object>) result;

        // Verify that it is a Java Integer, not a Jackson IntNode
        assertEquals(Integer.class, diff.get("val").getClass());
        assertEquals(20, diff.get("val"));
    }

    @Test
    @DisplayName("Should include new fields that didn't exist in old data")
    void testNewFieldAddition() {
        Map<String, Object> oldData = Map.of("existing", "value");
        Map<String, Object> newData = Map.of("existing", "value", "newField", "added");

        Object result = calculator.calculateDiff(oldData, newData);
        Map<String, Object> diff = (Map<String, Object>) result;

        assertEquals("added", diff.get("newField"));
        assertEquals(1, diff.size());
    }

    @Test
    @DisplayName("Should fallback to returning new data if an exception occurs")
    void testExceptionFallback() {
        // We pass objects that will cause Jackson issues or use a broken mapper 
        // effectively simulated by the try-catch in calculateDiff
        AuditDiffCalculator brokenCalculator = new AuditDiffCalculator(null);
        
        Map<String, String> newData = Map.of("test", "data");
        Object result = brokenCalculator.calculateDiff(Map.of(), newData);

        assertEquals(newData, result, "Should return newData on exception");
    }

    @Test
    @DisplayName("Should handle null leaf nodes correctly")
    void testNullLeafNode() {
        Map<String, Object> oldData = new HashMap<>();
        oldData.put("val", "not null");
        
        Map<String, Object> newData = new HashMap<>();
        newData.put("val", null);

        Object result = calculator.calculateDiff(oldData, newData);
        Map<String, Object> diff = (Map<String, Object>) result;

        assertTrue(diff.containsKey("val"));
        assertNull(diff.get("val"));
    }
}