package com.codex.flashsale.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codex.flashsale.alerts.AlertDeliveryPublisher;
import com.codex.flashsale.alerts.AlertDeliveryState;
import com.codex.flashsale.alerts.AlertDeliveryStateRepository;
import com.codex.flashsale.alerts.AlertDispatchType;
import com.codex.flashsale.api.OpsAlertResponse;
import com.codex.flashsale.api.OpsAlertSeverity;
import com.codex.flashsale.api.OpsAlertStatus;
import com.codex.flashsale.common.time.TimeProvider;
import com.codex.flashsale.config.ApplicationProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OpsAlertDeliveryServiceTest {

    private final AtomicReference<AlertDeliveryState> storedState = new AtomicReference<>();
    private final MutableTimeProvider timeProvider = new MutableTimeProvider(Instant.parse("2026-03-15T09:00:00Z"));

    private AlertDeliveryStateRepository alertDeliveryStateRepository;
    private OpsAlertService opsAlertService;
    private AlertDeliveryPublisher alertDeliveryPublisher;
    private ApplicationProperties applicationProperties;
    private OpsAlertDeliveryService opsAlertDeliveryService;

    @BeforeEach
    void setUp() {
        alertDeliveryStateRepository = mock(AlertDeliveryStateRepository.class);
        opsAlertService = mock(OpsAlertService.class);
        alertDeliveryPublisher = mock(AlertDeliveryPublisher.class);
        applicationProperties = new ApplicationProperties();
        applicationProperties.getAlerts().getDelivery().setEnabled(true);
        applicationProperties.getAlerts().getDelivery().setReminderInterval(Duration.ofMinutes(15));

        when(alertDeliveryStateRepository.findById(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(storedState.get()));
        when(alertDeliveryStateRepository.saveAndFlush(any(AlertDeliveryState.class)))
                .thenAnswer(invocation -> {
                    AlertDeliveryState state = invocation.getArgument(0);
                    storedState.set(state);
                    return state;
                });

        opsAlertDeliveryService = new OpsAlertDeliveryService(
                opsAlertService,
                alertDeliveryStateRepository,
                alertDeliveryPublisher,
                applicationProperties,
                timeProvider,
                new SimpleMeterRegistry()
        );
    }

    @Test
    void shouldSendActiveAlertOnceAndAgainAfterReminderInterval() {
        when(opsAlertService.getAlerts()).thenAnswer(invocation -> List.of(alert("OUTBOX_FAILED_BACKLOG", OpsAlertStatus.ACTIVE)));

        opsAlertDeliveryService.dispatchCurrentAlerts();
        timeProvider.advance(Duration.ofMinutes(10));
        opsAlertDeliveryService.dispatchCurrentAlerts();
        timeProvider.advance(Duration.ofMinutes(6));
        opsAlertDeliveryService.dispatchCurrentAlerts();

        ArgumentCaptor<AlertDispatchType> dispatchTypeCaptor = ArgumentCaptor.forClass(AlertDispatchType.class);
        verify(alertDeliveryPublisher, times(2)).publish(any(), dispatchTypeCaptor.capture(), any());
        assertThat(dispatchTypeCaptor.getAllValues()).containsExactly(AlertDispatchType.TRANSITION, AlertDispatchType.REMINDER);
        assertThat(storedState.get().getLastNotifiedStatus()).isEqualTo(OpsAlertStatus.ACTIVE);
        assertThat(storedState.get().getConsecutiveFailures()).isZero();
    }

    @Test
    void shouldSendClearNotificationWhenAlertRecovers() {
        AtomicReference<OpsAlertStatus> currentStatus = new AtomicReference<>(OpsAlertStatus.ACTIVE);
        when(opsAlertService.getAlerts()).thenAnswer(invocation -> List.of(alert("RECONCILIATION_RUN_FAILURE", currentStatus.get())));

        opsAlertDeliveryService.dispatchCurrentAlerts();
        currentStatus.set(OpsAlertStatus.INACTIVE);
        timeProvider.advance(Duration.ofMinutes(1));
        opsAlertDeliveryService.dispatchCurrentAlerts();

        ArgumentCaptor<AlertDispatchType> dispatchTypeCaptor = ArgumentCaptor.forClass(AlertDispatchType.class);
        verify(alertDeliveryPublisher, times(2)).publish(any(), dispatchTypeCaptor.capture(), any());
        assertThat(dispatchTypeCaptor.getAllValues()).containsExactly(AlertDispatchType.TRANSITION, AlertDispatchType.TRANSITION);
        assertThat(storedState.get().getLastNotifiedStatus()).isEqualTo(OpsAlertStatus.INACTIVE);
    }

    @Test
    void shouldRecordFailureWithoutThrowingWhenWebhookPublishFails() {
        when(opsAlertService.getAlerts()).thenReturn(List.of(alert("CHANNEL_SYNC_FAILED_BACKLOG", OpsAlertStatus.ACTIVE)));
        doThrow(new IllegalStateException("webhook down")).when(alertDeliveryPublisher).publish(any(), any(), any());

        assertThatCode(() -> opsAlertDeliveryService.dispatchCurrentAlerts()).doesNotThrowAnyException();

        assertThat(storedState.get().getLastNotifiedStatus()).isNull();
        assertThat(storedState.get().getLastError()).contains("webhook down");
        assertThat(storedState.get().getConsecutiveFailures()).isEqualTo(1);
    }

    @Test
    void shouldTrackStateWithoutPublishingWhenDeliveryDisabled() {
        applicationProperties.getAlerts().getDelivery().setEnabled(false);
        opsAlertDeliveryService = new OpsAlertDeliveryService(
                opsAlertService,
                alertDeliveryStateRepository,
                alertDeliveryPublisher,
                applicationProperties,
                timeProvider,
                new SimpleMeterRegistry()
        );
        when(opsAlertService.getAlerts()).thenReturn(List.of(alert("STALE_CHANNEL_SNAPSHOTS", OpsAlertStatus.ACTIVE)));

        opsAlertDeliveryService.dispatchCurrentAlerts();

        verifyNoInteractions(alertDeliveryPublisher);
        assertThat(storedState.get().getLastObservedStatus()).isEqualTo(OpsAlertStatus.ACTIVE);
        assertThat(storedState.get().getLastNotifiedStatus()).isNull();
    }

    private OpsAlertResponse alert(String code, OpsAlertStatus status) {
        return new OpsAlertResponse(
                code,
                OpsAlertSeverity.WARN,
                status,
                "Synthetic alert",
                "1",
                "0",
                timeProvider.now()
        );
    }

    private static final class MutableTimeProvider implements TimeProvider {

        private Instant now;

        private MutableTimeProvider(Instant now) {
            this.now = now;
        }

        @Override
        public Instant now() {
            return now;
        }

        private void advance(Duration duration) {
            now = now.plus(duration);
        }
    }
}
