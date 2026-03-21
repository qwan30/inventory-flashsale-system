package com.codex.flashsale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codex.flashsale.api.AdminAuthResponse;
import com.codex.flashsale.api.AdminLoginRequest;
import com.codex.flashsale.flashsale.CampaignStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
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
class AdminBrowserCookieAuthIntegrationTest extends AbstractIntegrationTest {

    @DynamicPropertySource
    static void browserCookieProperties(DynamicPropertyRegistry registry) {
        registry.add("app.security.jwt.refresh-cookie.enabled", () -> true);
        registry.add("app.security.jwt.refresh-cookie.name", () -> "admin_refresh_token");
        registry.add("app.security.jwt.refresh-cookie.path", () -> "/api/v1/admin/auth");
        registry.add("app.security.jwt.refresh-cookie.same-site", () -> "Strict");
    }

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
    void shouldRefreshAndLogoutUsingHttpOnlyRefreshCookie() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminLoginRequest("admin", "Admin123!"))))
                .andExpect(status().isOk())
                .andReturn();

        AdminAuthResponse loginResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsByteArray(),
                AdminAuthResponse.class
        );
        String loginCookie = loginResult.getResponse().getHeader("Set-Cookie");

        assertThat(loginCookie).contains("HttpOnly");
        assertThat(loginCookie).contains("SameSite=Strict");
        assertThat(loginCookie).contains("admin_refresh_token=");

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/admin/auth/refresh")
                        .cookie(loginResult.getResponse().getCookies()))
                .andExpect(status().isOk())
                .andReturn();

        AdminAuthResponse refreshed = objectMapper.readValue(
                refreshResult.getResponse().getContentAsByteArray(),
                AdminAuthResponse.class
        );

        assertThat(refreshed.refreshToken()).isNotBlank();
        assertThat(refreshed.refreshToken()).isNotEqualTo(loginResponse.refreshToken());

        MvcResult logoutResult = mockMvc.perform(post("/api/v1/admin/auth/logout")
                        .header(AUTHORIZATION, bearer(refreshed.accessToken()))
                        .cookie(refreshResult.getResponse().getCookies()))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(logoutResult.getResponse().getHeader("Set-Cookie")).contains("Max-Age=0");

        mockMvc.perform(post("/api/v1/admin/auth/refresh")
                        .cookie(refreshResult.getResponse().getCookies()))
                .andExpect(status().isUnauthorized());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
