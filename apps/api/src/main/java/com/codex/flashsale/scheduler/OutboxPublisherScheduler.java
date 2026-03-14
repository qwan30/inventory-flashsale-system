package com.codex.flashsale.scheduler;

import com.codex.flashsale.outbox.OutboxService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxPublisherScheduler {

    private final OutboxService outboxService;

    public OutboxPublisherScheduler(OutboxService outboxService) {
        this.outboxService = outboxService;
    }

    @Scheduled(fixedDelayString = "${app.scheduler.outbox-delay:5s}")
    public void publishPendingEvents() {
        outboxService.publishPendingEvents();
    }
}

