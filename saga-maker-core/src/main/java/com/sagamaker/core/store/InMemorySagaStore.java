package com.sagamaker.core.store;

import com.sagamaker.core.model.SagaDefinition;
import com.sagamaker.core.model.SagaInstance;
import com.sagamaker.core.model.SagaStatus;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemorySagaStore implements SagaStore {
    private final Map<String, SagaDefinition> definitions = new ConcurrentHashMap<>();
    private final Map<String, SagaInstance> instances = new ConcurrentHashMap<>();

    @Override
    public void saveDefinition(SagaDefinition definition) {
        definitions.put(definition.getName(), definition);
    }

    @Override
    public Optional<SagaDefinition> findDefinition(String name) {
        return Optional.ofNullable(definitions.get(name));
    }

    @Override
    public void saveInstance(SagaInstance instance) {
        instances.put(instance.getId(), instance);
    }

    @Override
    public Optional<SagaInstance> findInstance(String id) {
        return Optional.ofNullable(instances.get(id));
    }

    @Override
    public List<SagaInstance> findAllInstances() {
        return new ArrayList<>(instances.values());
    }

    @Override
    public List<SagaInstance> findInstancesByStatus(String sagaName, SagaStatus status) {
        return instances.values().stream()
                .filter(i -> i.getSagaName().equals(sagaName) && i.getStatus() == status)
                .collect(Collectors.toList());
    }
}
