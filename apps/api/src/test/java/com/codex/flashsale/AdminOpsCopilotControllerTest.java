package com.codex.flashsale;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codex.flashsale.ai.OpsCopilotService;
import com.codex.flashsale.admin.AdminActivityAuditService;
import com.codex.flashsale.api.OpsCopilotAnalyzeRequest;
import com.codex.flashsale.api.OpsCopilotAnalyzeResponse;
import com.codex.flashsale.api.OpsCopilotCapabilitiesResponse;
import com.codex.flashsale.api.OpsCopilotCitationResponse;
import com.codex.flashsale.api.OpsCopilotFindingResponse;
import com.codex.flashsale.api.OpsCopilotActionResponse;
import com.codex.flashsale.api.OpsCopilotProviderMetadataResponse;
import com.codex.flashsale.controller.AdminOpsCopilotController;
import com.codex.flashsale.security.ApiAccessDeniedHandler;
import com.codex.flashsale.security.ApiAuthenticationEntryPoint;
import com.codex.flashsale.security.SecurityConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminOpsCopilotController.class)
@Import({SecurityConfiguration.class, ApiAuthenticationEntryPoint.class, ApiAccessDeniedHandler.class})
@TestPropertySource(properties = "app.security.jwt.secret=change-me-change-me-change-me-1234")
class AdminOpsCopilotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OpsCopilotService opsCopilotService;

    @MockBean
    private AdminActivityAuditService adminActivityAuditService;

    @Test
    void shouldRequireAuthenticationForCapabilities() throws Exception {
        mockMvc.perform(get("/api/v1/admin/ops/copilot/capabilities"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void operatorShouldAccessCapabilities() throws Exception {
        when(opsCopilotService.getCapabilities()).thenReturn(new OpsCopilotCapabilitiesResponse(
                true,
                true,
                "gemini",
                "gemini-2.5-flash",
                List.of("OPS_OVERVIEW"),
                "ready"
        ));

        mockMvc.perform(get("/api/v1/admin/ops/copilot/capabilities")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.provider").value("gemini"));
    }

    @Test
    void adminShouldAnalyze() throws Exception {
        when(opsCopilotService.analyze(org.mockito.ArgumentMatchers.any())).thenReturn(new OpsCopilotAnalyzeResponse(
                "OPS_OVERVIEW",
                "ops-overview",
                "Operator review recommended.",
                List.of(new OpsCopilotFindingResponse("WARN", "Backlog", "Failed events are accumulating.", List.of("alerts-current"))),
                List.of(new OpsCopilotActionResponse("Open remediation", "/ops/remediation", "Review retry backlog.", List.of("alerts-current"))),
                List.of(new OpsCopilotCitationResponse("alerts-current", "Alerts", "Current alert surface")),
                new OpsCopilotProviderMetadataResponse("gemini", "gemini-2.5-flash", true, "req-1")
        ));

        mockMvc.perform(post("/api/v1/admin/ops/copilot/analyze")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(new OpsCopilotAnalyzeRequest("OPS_OVERVIEW", null, null, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjectId").value("ops-overview"))
                .andExpect(jsonPath("$.providerMetadata.provider").value("gemini"))
                .andExpect(jsonPath("$.recommendedActions[0].href").value("/ops/remediation"));
    }
}
