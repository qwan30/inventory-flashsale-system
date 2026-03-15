package com.codex.flashsale.admin;

import com.codex.flashsale.common.persistence.AuditTimestamps;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "admin_activity_audit")
public class AdminActivityAudit extends AuditTimestamps {

    @Id
    private String id;

    @Column(name = "actor_username", nullable = false)
    private String actorUsername;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_role")
    private AdminRole actorRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private AdminActivityAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false)
    private AdminActivityResourceType resourceType;

    @Column(name = "resource_id")
    private String resourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false)
    private AdminActivityOutcome outcome;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "details", length = 2000)
    private String details;

    protected AdminActivityAudit() {
    }

    private AdminActivityAudit(
            String id,
            String actorUsername,
            AdminRole actorRole,
            AdminActivityAction action,
            AdminActivityResourceType resourceType,
            String resourceId,
            AdminActivityOutcome outcome,
            String correlationId,
            String details
    ) {
        this.id = id;
        this.actorUsername = actorUsername;
        this.actorRole = actorRole;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.outcome = outcome;
        this.correlationId = correlationId;
        this.details = details;
    }

    public static AdminActivityAudit record(
            String actorUsername,
            AdminRole actorRole,
            AdminActivityAction action,
            AdminActivityResourceType resourceType,
            String resourceId,
            AdminActivityOutcome outcome,
            String correlationId,
            String details
    ) {
        return new AdminActivityAudit(
                UUID.randomUUID().toString(),
                actorUsername,
                actorRole,
                action,
                resourceType,
                resourceId,
                outcome,
                correlationId,
                details
        );
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public AdminRole getActorRole() {
        return actorRole;
    }

    public AdminActivityAction getAction() {
        return action;
    }

    public AdminActivityResourceType getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public AdminActivityOutcome getOutcome() {
        return outcome;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getDetails() {
        return details;
    }
}
