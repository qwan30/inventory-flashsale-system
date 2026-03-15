package com.codex.flashsale.api;

import jakarta.validation.constraints.NotBlank;

public record AdminTokenRefreshRequest(@NotBlank String refreshToken) {
}
