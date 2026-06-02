package com.sagamaker.sample.controller;

import com.sagamaker.core.engine.SagaOrchestrator;
import com.sagamaker.core.model.SagaContext;
import com.sagamaker.core.model.SagaInstance;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final SagaOrchestrator orchestrator;

    public OrderController(SagaOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody com.sagamaker.sample.dto.OrderRequest request) {
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        SagaContext context = new SagaContext(orderId);
        context.put("orderId", orderId);
        context.put("userId", request.getUserId());
        context.put("itemId", request.getItemId());
        context.put("quantity", request.getQuantity());
        context.put("amount", request.getAmount());

        SagaInstance instance = orchestrator.startSaga("order-creation", context);

        return ResponseEntity.ok(Map.of(
                "orderId", orderId,
                "sagaId", instance.getId(),
                "status", instance.getStatus().name(),
                "failureReason", instance.getFailureReason() != null ? instance.getFailureReason() : ""
        ));
    }
}
