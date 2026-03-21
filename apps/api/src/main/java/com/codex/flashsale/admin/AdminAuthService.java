package com.codex.flashsale.admin;

import com.codex.flashsale.api.AdminAuthResponse;
import com.codex.flashsale.api.AdminLoginRequest;
import com.codex.flashsale.api.AdminLogoutResponse;
import com.codex.flashsale.common.exception.UnauthorizedException;
import com.codex.flashsale.common.time.TimeProvider;
import com.codex.flashsale.config.ApplicationProperties;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AdminUserRepository adminUserRepository;
    private final AdminRefreshTokenRepository adminRefreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminJwtService adminJwtService;
    private final AdminTokenHashService adminTokenHashService;
    private final AdminActivityAuditService adminActivityAuditService;
    private final TimeProvider timeProvider;
    private final ApplicationProperties applicationProperties;

    public AdminAuthService(
            AdminUserRepository adminUserRepository,
            AdminRefreshTokenRepository adminRefreshTokenRepository,
            PasswordEncoder passwordEncoder,
            AdminJwtService adminJwtService,
            AdminTokenHashService adminTokenHashService,
            AdminActivityAuditService adminActivityAuditService,
            TimeProvider timeProvider,
            ApplicationProperties applicationProperties
    ) {
        this.adminUserRepository = adminUserRepository;
        this.adminRefreshTokenRepository = adminRefreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminJwtService = adminJwtService;
        this.adminTokenHashService = adminTokenHashService;
        this.adminActivityAuditService = adminActivityAuditService;
        this.timeProvider = timeProvider;
        this.applicationProperties = applicationProperties;
    }

    @Transactional
    public AdminAuthResponse login(AdminLoginRequest request, AdminRequestMetadata metadata) {
        AdminUser user = adminUserRepository.findByUsernameIgnoreCase(request.username())
                .orElseThrow(() -> invalidCredentials(request.username(), metadata));
        if (!user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials(request.username(), metadata);
        }

        revokeActiveTokens(user.getId(), timeProvider.now());
        AdminAuthResponse response = issueSession(user);
        adminActivityAuditService.record(
                user.getUsername(),
                user.getRole(),
                AdminActivityAction.AUTH_LOGIN_SUCCESS,
                AdminActivityResourceType.AUTH,
                user.getId(),
                AdminActivityOutcome.SUCCESS,
                metadata.correlationId(),
                metadata.asDetail()
        );
        return response;
    }

    @Transactional
    public AdminAuthResponse refresh(String refreshTokenValue, AdminRequestMetadata metadata) {
        Instant now = timeProvider.now();
        AdminRefreshToken refreshToken = adminRefreshTokenRepository.findByTokenHash(adminTokenHashService.hash(refreshTokenValue))
                .orElseThrow(() -> invalidRefreshToken());
        if (!refreshToken.isActive(now)) {
            throw invalidRefreshToken();
        }

        AdminUser user = adminUserRepository.findById(refreshToken.getUserId())
                .orElseThrow(this::invalidRefreshToken);
        refreshToken.revoke(now);
        AdminAuthResponse response = issueSession(user);
        adminActivityAuditService.record(
                user.getUsername(),
                user.getRole(),
                AdminActivityAction.AUTH_TOKEN_REFRESH,
                AdminActivityResourceType.AUTH,
                user.getId(),
                AdminActivityOutcome.SUCCESS,
                metadata.correlationId(),
                metadata.asDetail()
        );
        return response;
    }

    @Transactional
    public AdminLogoutResponse logout(String actorUsername, String refreshTokenValue, AdminRequestMetadata metadata) {
        Instant now = timeProvider.now();
        adminRefreshTokenRepository.findByTokenHash(adminTokenHashService.hash(refreshTokenValue))
                .ifPresent(token -> token.revoke(now));
        adminUserRepository.findByUsernameIgnoreCase(actorUsername).ifPresent(user ->
                adminActivityAuditService.record(
                        user.getUsername(),
                        user.getRole(),
                        AdminActivityAction.AUTH_LOGOUT,
                        AdminActivityResourceType.AUTH,
                        user.getId(),
                        AdminActivityOutcome.SUCCESS,
                        metadata.correlationId(),
                        metadata.asDetail()
                )
        );
        return new AdminLogoutResponse(true);
    }

    private AdminAuthResponse issueSession(AdminUser user) {
        Instant now = timeProvider.now();
        AdminJwtService.IssuedToken accessToken = adminJwtService.issueAccessToken(user);
        String rawRefreshToken = generateRefreshToken();
        Instant refreshTokenExpiresAt = now.plus(applicationProperties.getSecurity().getJwt().getRefreshTokenTtl());
        adminRefreshTokenRepository.save(AdminRefreshToken.issue(
                user.getId(),
                adminTokenHashService.hash(rawRefreshToken),
                refreshTokenExpiresAt
        ));
        return new AdminAuthResponse(
                accessToken.tokenValue(),
                accessToken.expiresAt(),
                rawRefreshToken,
                refreshTokenExpiresAt,
                user.getUsername(),
                user.getDisplayName(),
                user.getRole()
        );
    }

    private void revokeActiveTokens(String userId, Instant now) {
        adminRefreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(userId)
                .forEach(token -> token.revoke(now));
    }

    private UnauthorizedException invalidCredentials(String username, AdminRequestMetadata metadata) {
        adminActivityAuditService.record(
                username,
                null,
                AdminActivityAction.AUTH_LOGIN_FAILURE,
                AdminActivityResourceType.AUTH,
                username,
                AdminActivityOutcome.FAILURE,
                metadata.correlationId(),
                metadata.asDetail()
        );
        return new UnauthorizedException("ADMIN_INVALID_CREDENTIALS", "Invalid admin credentials");
    }

    private UnauthorizedException invalidRefreshToken() {
        return new UnauthorizedException("ADMIN_INVALID_REFRESH_TOKEN", "Invalid or expired refresh token");
    }

    private String generateRefreshToken() {
        byte[] buffer = new byte[32];
        SECURE_RANDOM.nextBytes(buffer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    }
}
