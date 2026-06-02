package com.sagamaker.core.model;

import java.util.function.Consumer;

public class SagaStepDefinition {
    private final String name;
    private final Consumer<SagaContext> action;
    private final Consumer<SagaContext> compensation;

    public SagaStepDefinition(String name, Consumer<SagaContext> action, Consumer<SagaContext> compensation) {
        this.name = name;
        this.action = action;
        this.compensation = compensation;
    }

    public String getName() { return name; }
    public Consumer<SagaContext> getAction() { return action; }
    public Consumer<SagaContext> getCompensation() { return compensation; }
}
