package com.codex.flashsale.admin;

import java.util.List;
import org.springframework.security.oauth2.jwt.Jwt;

public record AdminActor(String username, AdminRole role) {

    public static AdminActor from(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        AdminRole role = roles == null || roles.isEmpty() ? AdminRole.OPERATOR : AdminRole.valueOf(roles.getFirst());
        return new AdminActor(jwt.getClaimAsString("username"), role);
    }
}
