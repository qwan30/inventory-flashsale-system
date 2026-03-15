package com.codex.flashsale.api;

import jakarta.validation.constraints.NotBlank;

public record AdminLogoutRequest(@NotBlank String refreshToken) {
}
