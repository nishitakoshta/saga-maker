package com.sagamaker.core.endpoint;

import com.sagamaker.core.engine.SagaOrchestrator;
import com.sagamaker.core.model.SagaInstance;
import com.sagamaker.core.store.SagaStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/_saga")
@ConditionalOnClass(name = "org.springframework.web.bind.annotation.RestController")
public class SagaEndpoint {

    private final SagaOrchestrator orchestrator;
    private final SagaStore sagaStore;

    public SagaEndpoint(SagaOrchestrator orchestrator, SagaStore sagaStore) {
        this.orchestrator = orchestrator;
        this.sagaStore = sagaStore;
    }

    @GetMapping("/instances")
    public List<SagaInstance> listInstances() {
        return sagaStore.findAllInstances();
    }

    @GetMapping("/instances/{id}")
    public ResponseEntity<SagaInstance> getInstance(@PathVariable String id) {
        return sagaStore.findInstance(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "sagaCount", String.valueOf(sagaStore.findAllInstances().size()));
    }
}
