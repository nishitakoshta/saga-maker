package com.sagamaker.core.model;

import java.util.ArrayList;
import java.util.List;

public class SagaDefinition {
    private final String name;
    private final List<SagaStepDefinition> steps;
    private int maxRetries;
    private long retryDelayMs;

    public SagaDefinition(String name) {
        this.name = name;
        this.steps = new ArrayList<>();
        this.maxRetries = 3;
        this.retryDelayMs = 1000;
    }

    public String getName() { return name; }
    public List<SagaStepDefinition> getSteps() { return steps; }
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    public long getRetryDelayMs() { return retryDelayMs; }
    public void setRetryDelayMs(long retryDelayMs) { this.retryDelayMs = retryDelayMs; }

    public SagaDefinition addStep(SagaStepDefinition step) {
        steps.add(step);
        return this;
    }
}
