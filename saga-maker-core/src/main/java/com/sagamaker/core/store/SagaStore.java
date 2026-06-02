package com.sagamaker.core.store;

import com.sagamaker.core.model.SagaDefinition;
import com.sagamaker.core.model.SagaInstance;

import java.util.List;
import java.util.Optional;

public interface SagaStore {
    void saveDefinition(SagaDefinition definition);
    Optional<SagaDefinition> findDefinition(String name);
    void saveInstance(SagaInstance instance);
    Optional<SagaInstance> findInstance(String id);
    List<SagaInstance> findAllInstances();
    List<SagaInstance> findInstancesByStatus(String sagaName, com.sagamaker.core.model.SagaStatus status);
}
