package com.codex.flashsale.controller;

import com.codex.flashsale.admin.AdminAuthService;
import com.codex.flashsale.admin.AdminRefreshTokenCookieSupport;
import com.codex.flashsale.admin.AdminRequestMetadata;
import com.codex.flashsale.api.AdminAuthResponse;
import com.codex.flashsale.api.AdminLoginRequest;
import com.codex.flashsale.api.AdminLogoutRequest;
import com.codex.flashsale.api.AdminLogoutResponse;
import com.codex.flashsale.api.AdminTokenRefreshRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
    private final AdminRefreshTokenCookieSupport refreshTokenCookieSupport;

    public AdminAuthController(
            AdminAuthService adminAuthService,
            AdminRefreshTokenCookieSupport refreshTokenCookieSupport
    ) {
        this.adminAuthService = adminAuthService;
        this.refreshTokenCookieSupport = refreshTokenCookieSupport;
    }

    @PostMapping("/login")
    public AdminAuthResponse login(
            @Valid @RequestBody AdminLoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        AdminAuthResponse response = adminAuthService.login(request, AdminRequestMetadata.from(servletRequest));
        refreshTokenCookieSupport.writeSessionCookie(response, servletResponse);
        return response;
    }

    @PostMapping("/refresh")
    public AdminAuthResponse refresh(
            @RequestBody(required = false) AdminTokenRefreshRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        String refreshToken = refreshTokenCookieSupport.resolveRefreshToken(request != null ? request.refreshToken() : null, servletRequest);
        AdminAuthResponse response = adminAuthService.refresh(refreshToken, AdminRequestMetadata.from(servletRequest));
        refreshTokenCookieSupport.writeSessionCookie(response, servletResponse);
        return response;
    }

    @PostMapping("/logout")
    public AdminLogoutResponse logout(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody(required = false) AdminLogoutRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        String refreshToken = refreshTokenCookieSupport.resolveRefreshToken(request != null ? request.refreshToken() : null, servletRequest);
        AdminLogoutResponse response = adminAuthService.logout(
                jwt.getClaimAsString("username"),
                refreshToken,
                AdminRequestMetadata.from(servletRequest)
        );
        refreshTokenCookieSupport.clearCookie(servletResponse);
        return response;
    }
}
