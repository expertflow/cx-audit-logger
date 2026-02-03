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
        return extractPrimitive(newNode);
    }

    private Object extractPrimitive(JsonNode node) {
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isNumber()) {
            return node.numberValue();
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        return null;
    }

    private Map<String, Object> diffObject(JsonNode oldNode, JsonNode newNode) {
        Map<String, Object> diffMap = new HashMap<>();
        newNode.properties().forEach(entry -> {
            String key = entry.getKey();
            JsonNode newValue = entry.getValue();
            JsonNode oldValue = (oldNode != null) ? oldNode.get(key) : null;

            if (oldValue == null || !oldValue.equals(newValue)) {
                appendObjectDiff(diffMap, key, oldValue, newValue);
            }
        });
        return diffMap.isEmpty() ? null : diffMap;
    }

    private void appendObjectDiff(Map<String, Object> diffMap, String key, JsonNode oldValue, JsonNode newValue) {
        if (newValue.isContainerNode()) {
            Object childDiff = findDiffNested(oldValue, newValue);
            if (childDiff != null) {
                diffMap.put(key, childDiff);
            }
        } else {
            diffMap.put(key, extractPrimitive(newValue));
        }
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