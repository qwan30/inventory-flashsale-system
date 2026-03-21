package com.codex.flashsale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codex.flashsale.admin.AdminActivityAction;
import com.codex.flashsale.api.AdminActivityResponse;
import com.codex.flashsale.api.AdminAuthResponse;
import com.codex.flashsale.api.AdminLoginRequest;
import com.codex.flashsale.api.AdminLogoutRequest;
import com.codex.flashsale.api.AdminTokenRefreshRequest;
import com.codex.flashsale.flashsale.CampaignStatus;
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
class AdminSecurityIntegrationTest extends AbstractIntegrationTest {

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
    void shouldLoginRefreshAndRevokeRefreshTokenOnLogout() throws Exception {
        AdminAuthResponse loginResponse = login("admin", "Admin123!");

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/admin/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminTokenRefreshRequest(loginResponse.refreshToken()))))
                .andExpect(status().isOk())
                .andReturn();

        AdminAuthResponse refreshed = objectMapper.readValue(
                refreshResult.getResponse().getContentAsByteArray(),
                AdminAuthResponse.class
        );

        mockMvc.perform(post("/api/v1/admin/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminTokenRefreshRequest(loginResponse.refreshToken()))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/admin/auth/logout")
                        .header(AUTHORIZATION, bearer(refreshed.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminLogoutRequest(refreshed.refreshToken()))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminTokenRefreshRequest(refreshed.refreshToken()))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRequireAuthenticationForOpsEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/ops/alerts"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/ops/outbox/events"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/ops/reconciliation/runs"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/channels/health"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowOperatorOpsButRejectOperatorCampaignManagement() throws Exception {
        AdminAuthResponse operator = login("operator", "Operator123!");

        mockMvc.perform(get("/api/v1/admin/ops/alerts")
                        .header(AUTHORIZATION, bearer(operator.accessToken())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/ops/outbox/events")
                        .header(AUTHORIZATION, bearer(operator.accessToken())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/ops/reconciliation/runs")
                        .header(AUTHORIZATION, bearer(operator.accessToken())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/channels/health")
                        .header(AUTHORIZATION, bearer(operator.accessToken())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/campaigns/{campaignId}", BASE_CAMPAIGN_ID)
                        .header(AUTHORIZATION, bearer(operator.accessToken())))
                .andExpect(status().isForbidden());

        String requestBody = """
                {
                  "id": "campaign-operator-001",
                  "sku": "%s",
                  "startsAt": "%s",
                  "endsAt": "%s",
                  "quota": 10
                }
                """.formatted(
                BASE_SKU,
                Instant.now().plus(Duration.ofHours(2)),
                Instant.now().plus(Duration.ofHours(3))
        );

        mockMvc.perform(post("/api/v1/admin/campaigns")
                        .header(AUTHORIZATION, bearer(operator.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldCreateActivateEndAndAuditCampaignAsAdmin() throws Exception {
        AdminAuthResponse admin = login("admin", "Admin123!");
        Instant startsAt = Instant.now().plus(Duration.ofHours(2));
        Instant endsAt = startsAt.plus(Duration.ofHours(2));

        String createRequest = """
                {
                  "id": "campaign-admin-001",
                  "sku": "%s",
                  "startsAt": "%s",
                  "endsAt": "%s",
                  "quota": 15
                }
                """.formatted(BASE_SKU, startsAt, endsAt);

        mockMvc.perform(post("/api/v1/admin/campaigns")
                        .header(AUTHORIZATION, bearer(admin.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/campaigns/campaign-admin-001/activate")
                        .header(AUTHORIZATION, bearer(admin.accessToken())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/campaigns/campaign-admin-001/end")
                        .header(AUTHORIZATION, bearer(admin.accessToken())))
                .andExpect(status().isOk());

        MvcResult auditsResult = mockMvc.perform(get("/api/v1/admin/campaigns/campaign-admin-001/audits")
                        .header(AUTHORIZATION, bearer(admin.accessToken())))
                .andExpect(status().isOk())
                .andReturn();

        List<AdminActivityResponse> audits = objectMapper.readValue(
                auditsResult.getResponse().getContentAsByteArray(),
                new TypeReference<>() {
                }
        );

        assertThat(audits)
                .extracting(AdminActivityResponse::action)
                .contains(
                        AdminActivityAction.CAMPAIGN_CREATED,
                        AdminActivityAction.CAMPAIGN_ACTIVATED,
                        AdminActivityAction.CAMPAIGN_ENDED
                );
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
