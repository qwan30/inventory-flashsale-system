package com.codex.flashsale.controller;

import com.codex.flashsale.admin.AdminAuthService;
import com.codex.flashsale.admin.AdminRequestMetadata;
import com.codex.flashsale.api.AdminAuthResponse;
import com.codex.flashsale.api.AdminLoginRequest;
import com.codex.flashsale.api.AdminLogoutRequest;
import com.codex.flashsale.api.AdminLogoutResponse;
import com.codex.flashsale.api.AdminTokenRefreshRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/auth")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login")
    public AdminAuthResponse login(
            @Valid @RequestBody AdminLoginRequest request,
            HttpServletRequest servletRequest
    ) {
        return adminAuthService.login(request, AdminRequestMetadata.from(servletRequest));
    }

    @PostMapping("/refresh")
    public AdminAuthResponse refresh(
            @Valid @RequestBody AdminTokenRefreshRequest request,
            HttpServletRequest servletRequest
    ) {
        return adminAuthService.refresh(request, AdminRequestMetadata.from(servletRequest));
    }

    @PostMapping("/logout")
    public AdminLogoutResponse logout(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AdminLogoutRequest request,
            HttpServletRequest servletRequest
    ) {
        return adminAuthService.logout(
                jwt.getClaimAsString("username"),
                request,
                AdminRequestMetadata.from(servletRequest)
        );
    }
}
