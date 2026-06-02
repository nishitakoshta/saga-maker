package com.sagamaker.core.config;

import com.sagamaker.core.engine.SagaOrchestrator;
import com.sagamaker.core.store.InMemorySagaStore;
import com.sagamaker.core.store.SagaStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SagaMakerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SagaStore sagaStore() {
        return new InMemorySagaStore();
    }

    @Bean
    public SagaOrchestrator sagaOrchestrator(SagaStore sagaStore) {
        return new SagaOrchestrator(sagaStore);
    }
}
