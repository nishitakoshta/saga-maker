package com.sagamaker.sample;

import com.sagamaker.core.model.SagaDefinition;
import com.sagamaker.core.model.SagaStepDefinition;
import com.sagamaker.core.store.SagaStore;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootApplication(scanBasePackages = "com.sagamaker")
public class SagaSampleApplication {

    private static final Logger log = LoggerFactory.getLogger(SagaSampleApplication.class);

    public static final Map<String, Double> INVENTORY = new ConcurrentHashMap<>();
    public static final Map<String, Double> USER_BALANCE = new ConcurrentHashMap<>();
    public static final Map<String, Boolean> ORDER_STORE = new ConcurrentHashMap<>();

    static {
        INVENTORY.put("item-1", 100.0);
        INVENTORY.put("item-2", 50.0);
        USER_BALANCE.put("user-1", 5000.0);
    }

    private final SagaStore sagaStore;

    public SagaSampleApplication(SagaStore sagaStore) {
        this.sagaStore = sagaStore;
    }

    public static void main(String[] args) {
        SpringApplication.run(SagaSampleApplication.class, args);
    }

    @PostConstruct
    public void registerOrderSaga() {
        SagaDefinition orderSaga = new SagaDefinition("order-creation")
                .addStep(new SagaStepDefinition("reserve-inventory",
                        ctx -> {
                            String itemId = ctx.get("itemId");
                            Double quantity = ctx.get("quantity");
                            Double available = INVENTORY.getOrDefault(itemId, 0.0);
                            if (available < quantity) {
                                throw new RuntimeException("Insufficient inventory for " + itemId +
                                        ". Required: " + quantity + ", Available: " + available);
                            }
                            INVENTORY.put(itemId, available - quantity);
                            ctx.putStepResult("reserve-inventory", Map.of(
                                    "reserved", quantity, "remaining", available - quantity));
                            log.info("Reserved {} units of {} (remaining: {})", quantity, itemId, available - quantity);
                        },
                        ctx -> {
                            String itemId = ctx.get("itemId");
                            Double quantity = ctx.get("quantity");
                            INVENTORY.merge(itemId, quantity, Double::sum);
                            log.info("Compensated: Released {} units of {}", quantity, itemId);
                        }))
                .addStep(new SagaStepDefinition("process-payment",
                        ctx -> {
                            String userId = ctx.get("userId");
                            Double amount = ctx.get("amount");
                            Double balance = USER_BALANCE.getOrDefault(userId, 0.0);
                            if (balance < amount) {
                                throw new RuntimeException("Insufficient balance for user " + userId +
                                        ". Required: " + amount + ", Available: " + balance);
                            }
                            USER_BALANCE.put(userId, balance - amount);
                            ctx.putStepResult("process-payment", Map.of("charged", amount, "remaining", balance - amount));
                            log.info("Charged {} from user {} (remaining: {})", amount, userId, balance - amount);
                        },
                        ctx -> {
                            String userId = ctx.get("userId");
                            Double amount = ctx.get("amount");
                            USER_BALANCE.merge(userId, amount, Double::sum);
                            log.info("Compensated: Refunded {} to user {}", amount, userId);
                        }))
                .addStep(new SagaStepDefinition("confirm-order",
                        ctx -> {
                            String orderId = ctx.get("orderId");
                            ORDER_STORE.put(orderId, true);
                            ctx.putStepResult("confirm-order", Map.of("orderId", orderId, "status", "CONFIRMED"));
                            log.info("Order {} confirmed", orderId);
                        },
                        ctx -> {
                            String orderId = ctx.get("orderId");
                            ORDER_STORE.remove(orderId);
                            log.info("Compensated: Order {} cancelled", orderId);
                        }));

        sagaStore.saveDefinition(orderSaga);
        log.info("Registered saga: order-creation (3 steps)");
    }
}
