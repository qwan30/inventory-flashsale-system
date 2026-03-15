package com.codex.flashsale.channel.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codex.flashsale.channel.SalesChannel;
import com.codex.flashsale.common.time.TimeProvider;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ChannelSyncServiceTest {

    @Test
    void shouldResolveInboundSnapshotsByChannel() {
        ChannelSyncAttemptRepository attemptRepository = mock(ChannelSyncAttemptRepository.class);
        ChannelInventorySnapshotRepository snapshotRepository = mock(ChannelInventorySnapshotRepository.class);
        TimeProvider timeProvider = mock(TimeProvider.class);
        when(timeProvider.now()).thenReturn(Instant.parse("2026-03-15T08:00:00Z"));

        ChannelInboundPort webInboundPort = mock(ChannelInboundPort.class);
        ChannelInboundPort shopeeInboundPort = mock(ChannelInboundPort.class);
        when(webInboundPort.channel()).thenReturn(SalesChannel.WEB);
        when(shopeeInboundPort.channel()).thenReturn(SalesChannel.SHOPEE);

        ChannelInventorySnapshotView shopeeSnapshot = new ChannelInventorySnapshotView(
                SalesChannel.SHOPEE,
                "SKU-TEST-001",
                19,
                1,
                0,
                Instant.parse("2026-03-15T08:00:00Z"),
                "evt-1"
        );
        when(shopeeInboundPort.fetchInventorySnapshot("SKU-TEST-001")).thenReturn(Optional.of(shopeeSnapshot));

        ChannelSyncPort webSyncPort = mock(ChannelSyncPort.class);
        when(webSyncPort.channel()).thenReturn(SalesChannel.WEB);

        ChannelSyncService service = new ChannelSyncService(
                attemptRepository,
                snapshotRepository,
                List.of(webInboundPort, shopeeInboundPort),
                List.of(webSyncPort),
                timeProvider,
                50,
                Duration.ofSeconds(15),
                3,
                new SimpleMeterRegistry()
        );

        assertThat(service.fetchSnapshot(SalesChannel.SHOPEE, "SKU-TEST-001")).contains(shopeeSnapshot);
        verify(shopeeInboundPort).fetchInventorySnapshot("SKU-TEST-001");
        verify(webInboundPort, never()).fetchInventorySnapshot("SKU-TEST-001");
    }

    @Test
    void shouldReturnEmptyWhenInboundPortIsMissingForChannel() {
        ChannelSyncAttemptRepository attemptRepository = mock(ChannelSyncAttemptRepository.class);
        ChannelInventorySnapshotRepository snapshotRepository = mock(ChannelInventorySnapshotRepository.class);
        TimeProvider timeProvider = mock(TimeProvider.class);
        when(timeProvider.now()).thenReturn(Instant.parse("2026-03-15T08:00:00Z"));

        ChannelInboundPort webInboundPort = mock(ChannelInboundPort.class);
        when(webInboundPort.channel()).thenReturn(SalesChannel.WEB);

        ChannelSyncPort webSyncPort = mock(ChannelSyncPort.class);
        when(webSyncPort.channel()).thenReturn(SalesChannel.WEB);

        ChannelSyncService service = new ChannelSyncService(
                attemptRepository,
                snapshotRepository,
                List.of(webInboundPort),
                List.of(webSyncPort),
                timeProvider,
                50,
                Duration.ofSeconds(15),
                3,
                new SimpleMeterRegistry()
        );

        assertThat(service.fetchSnapshot(SalesChannel.SHOPEE, "SKU-TEST-001")).isEmpty();
        verify(webInboundPort, never()).fetchInventorySnapshot("SKU-TEST-001");
    }
}
