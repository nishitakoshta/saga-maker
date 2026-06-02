package com.sagamaker.core.model;

public enum SagaStatus {
    CREATED,
    STARTED,
    COMPLETED,
    FAILED,
    COMPENSATING,
    COMPENSATED,
    COMPENSATION_FAILED
}
