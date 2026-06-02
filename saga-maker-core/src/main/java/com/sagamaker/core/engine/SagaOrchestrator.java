package com.sagamaker.core.engine;

import com.sagamaker.core.exception.CompensationException;
import com.sagamaker.core.exception.SagaExecutionException;
import com.sagamaker.core.model.*;
import com.sagamaker.core.store.SagaStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ListIterator;

public class SagaOrchestrator {
    private static final Logger log = LoggerFactory.getLogger(SagaOrchestrator.class);

    private final SagaStore sagaStore;

    public SagaOrchestrator(SagaStore sagaStore) {
        this.sagaStore = sagaStore;
    }

    public SagaInstance startSaga(String sagaName, SagaContext context) {
        SagaDefinition definition = sagaStore.findDefinition(sagaName)
                .orElseThrow(() -> new IllegalArgumentException("No saga found with name: " + sagaName));

        SagaInstance instance = new SagaInstance(sagaName);
        instance.setStatus(SagaStatus.STARTED);
        sagaStore.saveInstance(instance);

        log.info("Saga [{}] instance [{}] started with {} steps",
                sagaName, instance.getId(), definition.getSteps().size());

        try {
            executeSteps(instance, definition, context);
            instance.setStatus(SagaStatus.COMPLETED);
            instance.setCompletedAt(System.currentTimeMillis());
            log.info("Saga [{}] instance [{}] completed successfully", sagaName, instance.getId());
        } catch (Exception e) {
            instance.setStatus(SagaStatus.FAILED);
            instance.setFailureReason(e.getMessage());
            log.error("Saga [{}] instance [{}] failed at step: {}", sagaName, instance.getId(), e.getMessage());
            compensate(instance, definition, context);
        }

        sagaStore.saveInstance(instance);
        return instance;
    }

    private void executeSteps(SagaInstance instance, SagaDefinition definition, SagaContext context) {
        for (SagaStepDefinition step : definition.getSteps()) {
            executeStepWithRetry(instance, step, context, definition.getMaxRetries(), definition.getRetryDelayMs());
        }
    }

    private void executeStepWithRetry(SagaInstance instance, SagaStepDefinition step,
                                       SagaContext context, int maxRetries, long retryDelayMs) {
        int attempts = 0;
        SagaStepExecution execution = new SagaStepExecution(step.getName());
        execution.setStatus(StepStatus.EXECUTING);
        execution.setStartedAt(System.currentTimeMillis());
        instance.addStepExecution(execution);

        while (attempts <= maxRetries) {
            try {
                step.getAction().accept(context);
                execution.setStatus(StepStatus.COMPLETED);
                execution.setCompletedAt(System.currentTimeMillis());
                log.info("Step [{}] completed (attempt {})", step.getName(), attempts + 1);
                return;
            } catch (Exception e) {
                attempts++;
                execution.incrementRetryCount();
                execution.setErrorMessage(e.getMessage());
                log.warn("Step [{}] failed (attempt {}/{}): {}",
                        step.getName(), attempts, maxRetries + 1, e.getMessage());

                if (attempts > maxRetries) {
                    execution.setStatus(StepStatus.FAILED);
                    instance.setFailureReason("Step '" + step.getName() + "' failed after " + attempts + " attempts: " + e.getMessage());
                    throw new SagaExecutionException(instance.getId(), step.getName(),
                            "Step '" + step.getName() + "' failed after " + attempts + " attempts", e);
                }

                try {
                    Thread.sleep(retryDelayMs * (long) Math.pow(2, attempts - 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new SagaExecutionException(instance.getId(), step.getName(), "Step interrupted during retry delay", ie);
                }
            }
        }
    }

    private void compensate(SagaInstance instance, SagaDefinition definition, SagaContext context) {
        instance.setStatus(SagaStatus.COMPENSATING);
        sagaStore.saveInstance(instance);
        log.info("Starting compensation for saga [{}] instance [{}]", instance.getSagaName(), instance.getId());

        ListIterator<SagaStepDefinition> iterator = definition.getSteps()
                .listIterator(definition.getSteps().size());

        while (iterator.hasPrevious()) {
            SagaStepDefinition step = iterator.previous();
            SagaStepExecution stepExecution = instance.getStepExecutions().stream()
                    .filter(e -> e.getStepName().equals(step.getName()))
                    .findFirst().orElse(null);

            if (stepExecution != null && stepExecution.getStatus() == StepStatus.COMPLETED) {
                try {
                    stepExecution.setStatus(StepStatus.COMPENSATING);
                    sagaStore.saveInstance(instance);
                    step.getCompensation().accept(context);
                    stepExecution.setStatus(StepStatus.COMPENSATED);
                    log.info("Compensation for step [{}] completed", step.getName());
                } catch (Exception e) {
                    stepExecution.setStatus(StepStatus.COMPENSATION_FAILED);
                    log.error("Compensation for step [{}] failed: {}", step.getName(), e.getMessage());
                    throw new CompensationException(instance.getId(), step.getName(),
                            "Compensation failed for step: " + step.getName(), e);
                }
            }
        }

        instance.setStatus(SagaStatus.COMPENSATED);
        instance.setCompletedAt(System.currentTimeMillis());
        sagaStore.saveInstance(instance);
        log.info("Compensation completed for saga [{}] instance [{}]", instance.getSagaName(), instance.getId());
    }

    public SagaInstance getSagaStatus(String instanceId) {
        return sagaStore.findInstance(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("No saga instance found with id: " + instanceId));
    }
}
