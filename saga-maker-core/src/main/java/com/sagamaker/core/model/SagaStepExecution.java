package com.sagamaker.core.model;

public class SagaStepExecution {
    private final String stepName;
    private StepStatus status;
    private long startedAt;
    private long completedAt;
    private String errorMessage;
    private int retryCount;

    public SagaStepExecution(String stepName) {
        this.stepName = stepName;
        this.status = StepStatus.PENDING;
        this.retryCount = 0;
    }

    public String getStepName() { return stepName; }
    public StepStatus getStatus() { return status; }
    public void setStatus(StepStatus status) { this.status = status; }
    public long getStartedAt() { return startedAt; }
    public void setStartedAt(long startedAt) { this.startedAt = startedAt; }
    public long getCompletedAt() { return completedAt; }
    public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public int getRetryCount() { return retryCount; }
    public void incrementRetryCount() { this.retryCount++; }
}
