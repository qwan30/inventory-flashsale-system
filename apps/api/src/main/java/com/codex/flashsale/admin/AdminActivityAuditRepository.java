package com.codex.flashsale.admin;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminActivityAuditRepository extends JpaRepository<AdminActivityAudit, String> {

    List<AdminActivityAudit> findByResourceTypeAndResourceIdOrderByCreatedAtDesc(
            AdminActivityResourceType resourceType,
            String resourceId
    );

    java.util.Optional<AdminActivityAudit> findTopByActionOrderByCreatedAtDesc(AdminActivityAction action);
}
