package com.ef.auditlogger.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility to calculate deep recursive diffs between objects for Audit Logging.
 */
public class AuditDiffCalculator {

    private final ObjectMapper objectMapper;

    public AuditDiffCalculator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @SuppressWarnings("java:S7467")
    public Object calculateDiff(Object oldData, Object newData) {
        try {
            JsonNode oldNode = objectMapper.valueToTree(oldData);
            JsonNode newNode = objectMapper.valueToTree(newData);
            return findDiffNested(oldNode, newNode);
        } catch (Exception e) {
            return newData;
        }
    }

    private Object findDiffNested(JsonNode oldNode, JsonNode newNode) {
        if (newNode.isObject()) {
            return diffObject(oldNode, newNode);
        }
        if (newNode.isArray()) {
            return diffArray(oldNode, newNode);
        }

        if (newNode.isTextual()) {
            return newNode.asText();
        }
        if (newNode.isNumber()) {
            return newNode.numberValue();
        }
        if (newNode.isBoolean()) {
            return newNode.asBoolean();
        }
        if (newNode.isNull()) {
            return null;
        }

        return newNode;
    }

    private Map<String, Object> diffObject(JsonNode oldNode, JsonNode newNode) {
        Map<String, Object> diffMap = new HashMap<>();
        newNode.properties().forEach(entry -> {
            JsonNode oldValue = (oldNode != null) ? oldNode.get(entry.getKey()) : null;
            JsonNode newValue = entry.getValue();

            if (oldValue == null || !oldValue.equals(newValue)) {
                Object childDiff = findDiffNested(oldValue, newValue);
                if (childDiff != null) {
                    diffMap.put(entry.getKey(), childDiff);
                }
            }
        });
        return diffMap.isEmpty() ? null : diffMap;
    }

    private List<Object> diffArray(JsonNode oldNode, JsonNode newNode) {
        List<Object> diffList = new ArrayList<>();
        for (int i = 0; i < newNode.size(); i++) {
            JsonNode newItem = newNode.get(i);
            JsonNode oldItem = findMatchingOldItem(oldNode, newItem, i);

            if (oldItem == null || !oldItem.equals(newItem)) {
                processArrayItemDiff(diffList, oldItem, newItem);
            }
        }
        return diffList.isEmpty() ? null : diffList;
    }

    private void processArrayItemDiff(List<Object> diffList, JsonNode oldItem, JsonNode newItem) {
        Object itemDiff = findDiffNested(oldItem, newItem);
        if (itemDiff == null) {
            return;
        }

        if (newItem.isObject() && itemDiff instanceof Map) {
            Map<String, Object> diffMap = (Map<String, Object>) itemDiff;
            injectIdentityMetadata(diffMap, newItem);
            diffList.add(diffMap);
        } else {
            diffList.add(itemDiff);
        }
    }

    private void injectIdentityMetadata(Map<String, Object> diffMap, JsonNode sourceNode) {
        if (sourceNode.has("key")) {
            diffMap.putIfAbsent("key", sourceNode.get("key").asText());
        }
        if (sourceNode.has("id")) {
            diffMap.putIfAbsent("id", sourceNode.get("id").asText());
        }
    }

    private JsonNode findMatchingOldItem(JsonNode oldArray, JsonNode newItem, int index) {
        if (oldArray == null || !oldArray.isArray()) {
            return null;
        }
        if (newItem.isObject() && (newItem.has("key") || newItem.has("id"))) {
            String idField = newItem.has("key") ? "key" : "id";
            String val = newItem.get(idField).asText();
            for (JsonNode candidate : oldArray) {
                if (candidate.has(idField) && candidate.get(idField).asText().equals(val)) {
                    return candidate;
                }
            }
        }
        return (index < oldArray.size()) ? oldArray.get(index) : null;
    }
}