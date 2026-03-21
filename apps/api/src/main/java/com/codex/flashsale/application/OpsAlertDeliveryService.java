package com.codex.flashsale.application;

import com.codex.flashsale.alerts.AlertDeliveryPublisher;
import com.codex.flashsale.alerts.AlertDeliveryState;
import com.codex.flashsale.alerts.AlertDeliveryStateRepository;
import com.codex.flashsale.alerts.AlertDispatchType;
import com.codex.flashsale.api.OpsAlertResponse;
import com.codex.flashsale.api.OpsAlertStatus;
import com.codex.flashsale.common.time.TimeProvider;
import com.codex.flashsale.config.ApplicationProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.lang.Nullable;

@Service
public class OpsAlertDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(OpsAlertDeliveryService.class);

    private final OpsAlertService opsAlertService;
    private final AlertDeliveryStateRepository alertDeliveryStateRepository;
    private final List<AlertDeliveryPublisher> alertDeliveryPublishers;
    private final ApplicationProperties.AlertDelivery deliveryProperties;
    private final TimeProvider timeProvider;
    private final Counter alertDeliverySuccessCounter;
    private final Counter alertDeliveryFailureCounter;

    public OpsAlertDeliveryService(
            OpsAlertService opsAlertService,
            AlertDeliveryStateRepository alertDeliveryStateRepository,
            @Nullable List<AlertDeliveryPublisher> alertDeliveryPublishers,
            ApplicationProperties applicationProperties,
            TimeProvider timeProvider,
            MeterRegistry meterRegistry
    ) {
        this.opsAlertService = opsAlertService;
        this.alertDeliveryStateRepository = alertDeliveryStateRepository;
        this.alertDeliveryPublishers = alertDeliveryPublishers != null ? List.copyOf(alertDeliveryPublishers) : List.of();
        this.deliveryProperties = applicationProperties.getAlerts().getDelivery();
        this.timeProvider = timeProvider;
        this.alertDeliverySuccessCounter = meterRegistry.counter("ops.alert.delivery.success");
        this.alertDeliveryFailureCounter = meterRegistry.counter("ops.alert.delivery.failure");
    }

    public void dispatchCurrentAlerts() {
        List<OpsAlertResponse> alerts = opsAlertService.getAlerts();
        for (OpsAlertResponse alert : alerts) {
            dispatchAlert(alert);
        }
    }

    private void dispatchAlert(OpsAlertResponse alert) {
        Instant now = timeProvider.now();
        Instant observedAt = alert.observedAt() != null ? alert.observedAt() : now;
        AlertDeliveryState state = alertDeliveryStateRepository.findById(alert.code())
                .orElseGet(() -> new AlertDeliveryState(alert.code()));
        state.observe(alert.status(), observedAt);

        if (!hasAnyDeliveryTarget()) {
            alertDeliveryStateRepository.saveAndFlush(state);
            return;
        }

        AlertDispatchType dispatchType = determineDispatchType(alert, state, now);
        if (dispatchType == null) {
            alertDeliveryStateRepository.saveAndFlush(state);
            return;
        }

        try {
            if (alertDeliveryPublishers.isEmpty()) {
                alertDeliveryStateRepository.saveAndFlush(state);
                return;
            }
            for (AlertDeliveryPublisher publisher : alertDeliveryPublishers) {
                publisher.publish(alert, dispatchType, now);
            }
            state.markSent(alert.status(), now);
            alertDeliverySuccessCounter.increment();
        } catch (RuntimeException exception) {
            state.markFailed(exception.getMessage());
            alertDeliveryFailureCounter.increment();
            log.warn("Failed to dispatch alert {}", alert.code(), exception);
        }
        alertDeliveryStateRepository.saveAndFlush(state);
    }

    private AlertDispatchType determineDispatchType(OpsAlertResponse alert, AlertDeliveryState state, Instant now) {
        if (alert.status() == OpsAlertStatus.ACTIVE) {
            if (state.getLastNotifiedStatus() != OpsAlertStatus.ACTIVE) {
                return AlertDispatchType.TRANSITION;
            }
            if (state.getLastSentAt() != null
                    && !now.isBefore(state.getLastSentAt().plus(deliveryProperties.getReminderInterval()))) {
                return AlertDispatchType.REMINDER;
            }
            return null;
        }

        if (state.getLastNotifiedStatus() == OpsAlertStatus.ACTIVE) {
            return AlertDispatchType.TRANSITION;
        }
        return null;
    }

    private boolean hasAnyDeliveryTarget() {
        return deliveryProperties.isEnabled()
                || deliveryProperties.getSlack().isEnabled()
                || deliveryProperties.getPagerDuty().isEnabled();
    }
}
