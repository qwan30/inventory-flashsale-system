package com.codex.flashsale.admin;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRefreshTokenRepository extends JpaRepository<AdminRefreshToken, String> {

    Optional<AdminRefreshToken> findByTokenHash(String tokenHash);

    List<AdminRefreshToken> findAllByUserIdAndRevokedAtIsNull(String userId);
}
