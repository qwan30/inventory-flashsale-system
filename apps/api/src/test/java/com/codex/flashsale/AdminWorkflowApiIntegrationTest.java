package com.codex.flashsale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codex.flashsale.api.AdminActivityResponse;
import com.codex.flashsale.api.AdminAuthResponse;
import com.codex.flashsale.api.AdminCampaignResponse;
import com.codex.flashsale.api.AdminLoginRequest;
import com.codex.flashsale.api.AdminTikTokIngressReplayRequest;
import com.codex.flashsale.api.ChannelHealthResponse;
import com.codex.flashsale.api.OutboxEventSummaryResponse;
import com.codex.flashsale.api.ReconciliationRunResponse;
import com.codex.flashsale.channel.reconciliation.ReconciliationTriggerType;
import com.codex.flashsale.flashsale.CampaignStatus;
import com.codex.flashsale.outbox.OutboxEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "app.scheduler.expired-reservation-delay=1h",
        "app.scheduler.outbox-delay=1h",
        "app.scheduler.channel-sync-delay=1h",
        "app.scheduler.alert-delivery-delay=1h"
})
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AdminWorkflowApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        resetDatabase(
                20,
                20,
                Instant.now().minus(Duration.ofMinutes(5)),
                Instant.now().plus(Duration.ofHours(1)),
                CampaignStatus.ACTIVE
        );
    }

    @Test
    void shouldFetchCampaignDetailAndPreserveCampaignAuditEndpoint() throws Exception {
        AdminAuthResponse admin = login("admin", "Admin123!");

        MvcResult campaignResult = mockMvc.perform(get("/api/v1/admin/campaigns/{campaignId}", BASE_CAMPAIGN_ID)
                        .header(AUTHORIZATION, bearer(admin.accessToken())))
                .andExpect(status().isOk())
                .andReturn();

        AdminCampaignResponse campaign = objectMapper.readValue(
                campaignResult.getResponse().getContentAsByteArray(),
                AdminCampaignResponse.class
        );

        MvcResult auditResult = mockMvc.perform(get("/api/v1/admin/campaigns/{campaignId}/audits", BASE_CAMPAIGN_ID)
                        .header(AUTHORIZATION, bearer(admin.accessToken())))
                .andExpect(status().isOk())
                .andReturn();

        List<AdminActivityResponse> audits = objectMapper.readValue(
                auditResult.getResponse().getContentAsByteArray(),
                new TypeReference<>() {
                }
        );

        mockMvc.perform(get("/api/v1/admin/campaigns/{campaignId}", "campaign-missing-001")
                        .header(AUTHORIZATION, bearer(admin.accessToken())))
                .andExpect(status().isNotFound());

        assertThat(campaign.id()).isEqualTo(BASE_CAMPAIGN_ID);
        assertThat(campaign.sku()).isEqualTo(BASE_SKU);
        assertThat(audits).isEmpty();
    }

    @Test
    void shouldListFailedOutboxEventsNewestFirstForAdminOpsSurface() throws Exception {
        AdminAuthResponse admin = login("admin", "Admin123!");

        OutboxEvent older = new OutboxEvent("evt-failed-older", "reservation", "res-1", "inventory.reservation.created", "{\"ok\":true}");
        older.markFailed("older failure", Instant.now().minusSeconds(30), Duration.ofSeconds(5), 5);
        outboxEventRepository.saveAndFlush(older);

        Thread.sleep(10L);

        OutboxEvent newer = new OutboxEvent("evt-failed-newer", "reservation", "res-2", "inventory.reservation.created", "{\"ok\":true}");
        newer.markFailed("newer failure", Instant.now(), Duration.ofSeconds(5), 5);
        outboxEventRepository.saveAndFlush(newer);

        MvcResult result = mockMvc.perform(get("/api/v1/admin/ops/outbox/events")
                        .header(AUTHORIZATION, bearer(admin.accessToken())))
                .andExpect(status().isOk())
                .andReturn();

        List<OutboxEventSummaryResponse> events = objectMapper.readValue(
                result.getResponse().getContentAsByteArray(),
                new TypeReference<>() {
                }
        );

        assertThat(events).hasSize(2);
        assertThat(events.get(0).eventId()).isEqualTo("evt-failed-newer");
        assertThat(events.get(1).eventId()).isEqualTo("evt-failed-older");
        assertThat(events)
                .extracting(OutboxEventSummaryResponse::status)
                .allMatch(status -> status.name().equals("FAILED"));
    }

    @Test
    void shouldListRecentReconciliationRunsWithCreatedAtAndAllowOperatorAccess() throws Exception {
        AdminAuthResponse admin = login("admin", "Admin123!");
        AdminAuthResponse operator = login("operator", "Operator123!");

        var scheduledRun = inventoryReconciliationRunRepository.saveAndFlush(
                new com.codex.flashsale.channel.reconciliation.InventoryReconciliationRun(
                        "run-scheduled-001",
                        ReconciliationTriggerType.SCHEDULED
                )
        );
        scheduledRun.fail("synthetic failure", Instant.now());
        inventoryReconciliationRunRepository.saveAndFlush(scheduledRun);

        Thread.sleep(10L);

        mockMvc.perform(post("/api/v1/admin/ops/reconciliation/runs")
                        .header(AUTHORIZATION, bearer(admin.accessToken())))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/v1/admin/ops/reconciliation/runs")
                        .header(AUTHORIZATION, bearer(operator.accessToken())))
                .andExpect(status().isOk())
                .andReturn();

        List<ReconciliationRunResponse> runs = objectMapper.readValue(
                result.getResponse().getContentAsByteArray(),
                new TypeReference<>() {
                }
        );

        assertThat(runs).isNotEmpty();
        assertThat(runs.get(0).createdAt()).isNotNull();
        assertThat(runs.get(0).runId()).isNotEqualTo("run-scheduled-001");
        assertThat(runs)
                .extracting(ReconciliationRunResponse::runId)
                .contains("run-scheduled-001");
    }

    @Test
    void shouldExposeChannelHealthAndLatestReplaySummary() throws Exception {
        AdminAuthResponse admin = login("admin", "Admin123!");

        mockMvc.perform(post("/api/v1/admin/channels/tiktok/ingress/replay")
                        .header(AUTHORIZATION, bearer(admin.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminTikTokIngressReplayRequest(
                                "INVENTORY",
                                "replay-health-001",
                                BASE_SKU,
                                18,
                                2,
                                0,
                                null,
                                null,
                                Instant.now()
                        ))))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/v1/admin/channels/health")
                        .header(AUTHORIZATION, bearer(admin.accessToken())))
                .andExpect(status().isOk())
                .andReturn();

        List<ChannelHealthResponse> summaries = objectMapper.readValue(
                result.getResponse().getContentAsByteArray(),
                new TypeReference<>() {
                }
        );

        ChannelHealthResponse tikTokSummary = summaries.stream()
                .filter(summary -> summary.channel().equals("TIKTOK_SHOP"))
                .findFirst()
                .orElseThrow();

        assertThat(tikTokSummary.connectorMode()).isEqualTo("MOCK");
        assertThat(tikTokSummary.configValid()).isTrue();
        assertThat(tikTokSummary.latestIngressReceipt()).isNotNull();
        assertThat(tikTokSummary.latestIngressReceipt().externalReceiptId()).isEqualTo("replay-health-001");
        assertThat(tikTokSummary.latestReplay()).isNotNull();
        assertThat(tikTokSummary.latestReplay().action()).isEqualTo("TIKTOK_INGRESS_REPLAY_TRIGGERED");
        assertThat(tikTokSummary.latestReplay().resourceId()).isEqualTo("replay-health-001");
    }

    private AdminAuthResponse login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminLoginRequest(username, password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsByteArray(), AdminAuthResponse.class);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
