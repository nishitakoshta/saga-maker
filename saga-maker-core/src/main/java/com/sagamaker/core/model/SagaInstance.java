package com.sagamaker.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SagaInstance {
    private final String id;
    private final String sagaName;
    private SagaStatus status;
    private final List<SagaStepExecution> stepExecutions;
    private String failureReason;
    private int retryCount;
    private final long createdAt;
    private long completedAt;

    public SagaInstance(String sagaName) {
        this.id = UUID.randomUUID().toString();
        this.sagaName = sagaName;
        this.status = SagaStatus.CREATED;
        this.stepExecutions = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
        this.retryCount = 0;
    }

    public String getId() { return id; }
    public String getSagaName() { return sagaName; }
    public SagaStatus getStatus() { return status; }
    public void setStatus(SagaStatus status) { this.status = status; }
    public List<SagaStepExecution> getStepExecutions() { return stepExecutions; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public int getRetryCount() { return retryCount; }
    public void incrementRetryCount() { this.retryCount++; }
    public long getCreatedAt() { return createdAt; }
    public long getCompletedAt() { return completedAt; }
    public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }

    public void addStepExecution(SagaStepExecution execution) {
        stepExecutions.add(execution);
    }
}
