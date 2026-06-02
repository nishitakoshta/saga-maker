package com.sagamaker.core.exception;

public class SagaExecutionException extends RuntimeException {
    private final String sagaId;
    private final String failedStep;

    public SagaExecutionException(String sagaId, String failedStep, String message) {
        super(message);
        this.sagaId = sagaId;
        this.failedStep = failedStep;
    }

    public SagaExecutionException(String sagaId, String failedStep, String message, Throwable cause) {
        super(message, cause);
        this.sagaId = sagaId;
        this.failedStep = failedStep;
    }

    public String getSagaId() { return sagaId; }
    public String getFailedStep() { return failedStep; }
}
