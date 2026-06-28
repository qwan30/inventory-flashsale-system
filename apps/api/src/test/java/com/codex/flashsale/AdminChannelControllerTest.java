package com.codex.flashsale;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codex.flashsale.api.ChannelHealthDetailResponse;
import com.codex.flashsale.api.ChannelHealthIngressReceiptResponse;
import com.codex.flashsale.api.ChannelHealthReplayResponse;
import com.codex.flashsale.api.ChannelSyncFailureDetailResponse;
import com.codex.flashsale.api.ReconciliationRunResponse;
import com.codex.flashsale.application.OpsApplicationService;
import com.codex.flashsale.common.domain.SalesChannel;
import com.codex.flashsale.controller.AdminChannelController;
import com.codex.flashsale.security.ApiAccessDeniedHandler;
import com.codex.flashsale.security.ApiAuthenticationEntryPoint;
import com.codex.flashsale.security.SecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminChannelController.class)
@Import({SecurityConfiguration.class, ApiAuthenticationEntryPoint.class, ApiAccessDeniedHandler.class})
@TestPropertySource(properties = "app.security.jwt.secret=change-me-change-me-change-me-1234")
class AdminChannelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OpsApplicationService opsApplicationService;

    @Test
    void operatorShouldReadChannelHealthDetail() throws Exception {
        when(opsApplicationService.getChannelHealthDetail(SalesChannel.TIKTOK_SHOP)).thenReturn(
                new ChannelHealthDetailResponse(
                        "TIKTOK_SHOP",
                        "DEGRADED",
                        "MOCK",
                        true,
                        2,
                        1,
                        1,
                        new ChannelSyncFailureDetailResponse("sync-1", "TRANSIENT", "timeout", 2, java.time.Instant.parse("2026-03-16T07:00:00Z")),
                        new ChannelHealthIngressReceiptResponse("INVENTORY", "receipt-1", "PROCESSED", java.time.Instant.parse("2026-03-16T07:01:00Z")),
                        new ChannelHealthReplayResponse("TIKTOK_INGRESS_REPLAY_TRIGGERED", "receipt-1", "SUCCESS", java.time.Instant.parse("2026-03-16T07:02:00Z"), "kind=INVENTORY"),
                        new ReconciliationRunResponse("run-1", "SCHEDULED", "COMPLETED", 3, 3, 1, null, java.time.Instant.parse("2026-03-16T07:03:00Z"), java.time.Instant.parse("2026-03-16T07:03:05Z"))
                )
        );

        mockMvc.perform(get("/api/v1/admin/channels/health/TIKTOK_SHOP")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.channel").value("TIKTOK_SHOP"))
                .andExpect(jsonPath("$.latestSyncFailure.attemptId").value("sync-1"))
                .andExpect(jsonPath("$.recentReconciliationRun.runId").value("run-1"));
    }
}
