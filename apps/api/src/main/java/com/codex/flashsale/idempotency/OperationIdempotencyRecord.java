package com.codex.flashsale.idempotency;

import com.codex.flashsale.common.persistence.AuditTimestamps;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "operation_idempotency")
public class OperationIdempotencyRecord extends AuditTimestamps {

    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false)
    private OperationIdempotencyType operationType;

    @Column(name = "resource_id", nullable = false)
    private String resourceId;

    @Column(name = "operation_value", nullable = false)
    private String operationValue;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Lob
    @Column(name = "response_payload", nullable = false, columnDefinition = "TEXT")
    private String responsePayload;

    protected OperationIdempotencyRecord() {
    }

    public OperationIdempotencyRecord(
            String id,
            OperationIdempotencyType operationType,
            String resourceId,
            String operationValue,
            String idempotencyKey,
            String responsePayload
    ) {
        this.id = id;
        this.operationType = operationType;
        this.resourceId = resourceId;
        this.operationValue = operationValue;
        this.idempotencyKey = idempotencyKey;
        this.responsePayload = responsePayload;
    }

    public String getId() {
        return id;
    }

    public OperationIdempotencyType getOperationType() {
        return operationType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getOperationValue() {
        return operationValue;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getResponsePayload() {
        return responsePayload;
    }
}
