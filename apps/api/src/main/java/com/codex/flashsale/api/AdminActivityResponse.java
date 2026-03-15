package com.codex.flashsale.api;

import com.codex.flashsale.admin.AdminActivityAction;
import com.codex.flashsale.admin.AdminActivityOutcome;
import com.codex.flashsale.admin.AdminActivityResourceType;
import com.codex.flashsale.admin.AdminRole;
import java.time.Instant;

public record AdminActivityResponse(
        String actorUsername,
        AdminRole actorRole,
        AdminActivityAction action,
        AdminActivityResourceType resourceType,
        String resourceId,
        AdminActivityOutcome outcome,
        String correlationId,
        String details,
        Instant createdAt
) {
}
