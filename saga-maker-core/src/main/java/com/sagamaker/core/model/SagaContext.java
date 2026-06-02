package com.sagamaker.core.model;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class SagaContext {
    private final String sagaId;
    private final Map<String, Object> data;
    private final Map<String, Object> stepResults;

    public SagaContext(String sagaId) {
        this.sagaId = sagaId;
        this.data = new ConcurrentHashMap<>();
        this.stepResults = new ConcurrentHashMap<>();
    }

    public String getSagaId() {
        return sagaId;
    }

    public void put(String key, Object value) {
        data.put(key, value);
    }

    public <T> T get(String key) {
        return (T) data.get(key);
    }

    public void putStepResult(String stepName, Object result) {
        stepResults.put(stepName, result);
    }

    public <T> T getStepResult(String stepName) {
        return (T) stepResults.get(stepName);
    }
}
