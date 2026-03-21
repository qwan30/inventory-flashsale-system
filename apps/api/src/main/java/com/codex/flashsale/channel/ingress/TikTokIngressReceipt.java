package com.codex.flashsale.channel.ingress;

import com.codex.flashsale.channel.SalesChannel;
import com.codex.flashsale.common.persistence.AuditTimestamps;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "channel_ingress_receipt")
public class TikTokIngressReceipt extends AuditTimestamps {

    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    private SalesChannel channel;

    @Column(name = "receipt_type", nullable = false)
    private String receiptType;

    @Column(name = "external_receipt_id", nullable = false)
    private String externalReceiptId;

    @Column(name = "payload_hash", nullable = false)
    private String payloadHash;

    @Column(name = "outcome", nullable = false)
    private String outcome;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected TikTokIngressReceipt() {
    }

    public TikTokIngressReceipt(
            String id,
            SalesChannel channel,
            String receiptType,
            String externalReceiptId,
            String payloadHash,
            String outcome,
            Instant processedAt
    ) {
        this.id = id;
        this.channel = channel;
        this.receiptType = receiptType;
        this.externalReceiptId = externalReceiptId;
        this.payloadHash = payloadHash;
        this.outcome = outcome;
        this.processedAt = processedAt;
    }

    public String getId() {
        return id;
    }

    public SalesChannel getChannel() {
        return channel;
    }

    public String getReceiptType() {
        return receiptType;
    }

    public String getExternalReceiptId() {
        return externalReceiptId;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    public String getOutcome() {
        return outcome;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
