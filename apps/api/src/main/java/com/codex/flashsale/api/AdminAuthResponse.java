package com.codex.flashsale.api;

import com.codex.flashsale.admin.AdminRole;
import java.time.Instant;

public record AdminAuthResponse(
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt,
        String username,
        String displayName,
        AdminRole role
) {
}
