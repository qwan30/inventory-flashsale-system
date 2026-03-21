package com.codex.flashsale.admin;

import com.codex.flashsale.api.AdminAuthResponse;
import com.codex.flashsale.common.exception.UnauthorizedException;
import com.codex.flashsale.config.ApplicationProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class AdminRefreshTokenCookieSupport {

    private final ApplicationProperties.RefreshCookie refreshCookieProperties;
    private final ApplicationProperties.Jwt jwtProperties;

    public AdminRefreshTokenCookieSupport(ApplicationProperties applicationProperties) {
        this.jwtProperties = applicationProperties.getSecurity().getJwt();
        this.refreshCookieProperties = jwtProperties.getRefreshCookie();
    }

    public void writeSessionCookie(AdminAuthResponse response, HttpServletResponse servletResponse) {
        if (!refreshCookieProperties.isEnabled()) {
            return;
        }
        servletResponse.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(refreshCookieProperties.getName(), response.refreshToken())
                .httpOnly(true)
                .secure(refreshCookieProperties.isSecure())
                .sameSite(refreshCookieProperties.getSameSite())
                .path(refreshCookieProperties.getPath())
                .maxAge(jwtProperties.getRefreshTokenTtl())
                .build()
                .toString());
    }

    public void clearCookie(HttpServletResponse servletResponse) {
        if (!refreshCookieProperties.isEnabled()) {
            return;
        }
        servletResponse.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(refreshCookieProperties.getName(), "")
                .httpOnly(true)
                .secure(refreshCookieProperties.isSecure())
                .sameSite(refreshCookieProperties.getSameSite())
                .path(refreshCookieProperties.getPath())
                .maxAge(0)
                .build()
                .toString());
    }

    public String resolveRefreshToken(String requestToken, HttpServletRequest servletRequest) {
        if (requestToken != null && !requestToken.isBlank()) {
            return requestToken;
        }
        if (!refreshCookieProperties.isEnabled() || servletRequest.getCookies() == null) {
            throw new UnauthorizedException("ADMIN_INVALID_REFRESH_TOKEN", "Invalid or expired refresh token");
        }
        for (Cookie cookie : servletRequest.getCookies()) {
            if (refreshCookieProperties.getName().equals(cookie.getName())
                    && cookie.getValue() != null
                    && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        throw new UnauthorizedException("ADMIN_INVALID_REFRESH_TOKEN", "Invalid or expired refresh token");
    }
}
