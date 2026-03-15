package com.codex.flashsale.admin;

import com.codex.flashsale.common.time.TimeProvider;
import com.codex.flashsale.config.ApplicationProperties;
import java.time.Instant;
import java.util.List;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class AdminJwtService {

    private final JwtEncoder jwtEncoder;
    private final ApplicationProperties applicationProperties;
    private final TimeProvider timeProvider;

    public AdminJwtService(JwtEncoder jwtEncoder, ApplicationProperties applicationProperties, TimeProvider timeProvider) {
        this.jwtEncoder = jwtEncoder;
        this.applicationProperties = applicationProperties;
        this.timeProvider = timeProvider;
    }

    public IssuedToken issueAccessToken(AdminUser user) {
        Instant issuedAt = timeProvider.now();
        Instant expiresAt = issuedAt.plus(applicationProperties.getSecurity().getJwt().getAccessTokenTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(applicationProperties.getSecurity().getJwt().getIssuer())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(user.getId())
                .claim("username", user.getUsername())
                .claim("displayName", user.getDisplayName())
                .claim("roles", List.of(user.getRole().name()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, expiresAt);
    }

    public record IssuedToken(String tokenValue, Instant expiresAt) {
    }
}
