package com.sagamaker.core.exception;

public class CompensationException extends RuntimeException {
    private final String sagaId;
    private final String stepName;

    public CompensationException(String sagaId, String stepName, String message, Throwable cause) {
        super(message, cause);
        this.sagaId = sagaId;
        this.stepName = stepName;
    }

    public String getSagaId() { return sagaId; }
    public String getStepName() { return stepName; }
}
