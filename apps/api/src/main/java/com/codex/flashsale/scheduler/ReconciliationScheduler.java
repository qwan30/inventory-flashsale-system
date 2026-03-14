package com.codex.flashsale.scheduler;

import com.codex.flashsale.application.OpsApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReconciliationScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationScheduler.class);

    private final OpsApplicationService opsApplicationService;

    public ReconciliationScheduler(OpsApplicationService opsApplicationService) {
        this.opsApplicationService = opsApplicationService;
    }

    @Scheduled(fixedDelayString = "${app.scheduler.reconciliation-delay:60s}")
    public void runScheduledReconciliation() {
        try {
            opsApplicationService.runScheduledReconciliation();
        } catch (RuntimeException exception) {
            log.warn("Scheduled reconciliation run failed", exception);
        }
    }
}
