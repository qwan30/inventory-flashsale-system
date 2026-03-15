package com.codex.flashsale.admin;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AdminActivityAuditService {

    private final AdminActivityAuditRepository repository;

    public AdminActivityAuditService(AdminActivityAuditRepository repository) {
        this.repository = repository;
    }

    public void record(
            String actorUsername,
            AdminRole actorRole,
            AdminActivityAction action,
            AdminActivityResourceType resourceType,
            String resourceId,
            AdminActivityOutcome outcome,
            String correlationId,
            String details
    ) {
        repository.save(AdminActivityAudit.record(
                actorUsername,
                actorRole,
                action,
                resourceType,
                resourceId,
                outcome,
                correlationId,
                details
        ));
    }

    public List<AdminActivityAudit> findResourceActivity(AdminActivityResourceType resourceType, String resourceId) {
        return repository.findByResourceTypeAndResourceIdOrderByCreatedAtDesc(resourceType, resourceId);
    }
}
