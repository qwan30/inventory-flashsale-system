package com.codex.flashsale.scheduler;

import com.codex.flashsale.application.OpsAlertDeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OpsAlertDispatchScheduler {

    private static final Logger log = LoggerFactory.getLogger(OpsAlertDispatchScheduler.class);

    private final OpsAlertDeliveryService opsAlertDeliveryService;

    public OpsAlertDispatchScheduler(OpsAlertDeliveryService opsAlertDeliveryService) {
        this.opsAlertDeliveryService = opsAlertDeliveryService;
    }

    @Scheduled(fixedDelayString = "${app.scheduler.alert-delivery-delay:30s}")
    public void dispatchAlerts() {
        try {
            opsAlertDeliveryService.dispatchCurrentAlerts();
        } catch (RuntimeException exception) {
            log.warn("Scheduled alert delivery run failed", exception);
        }
    }
}
