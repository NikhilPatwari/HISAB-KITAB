package com.hisabkitab.web.dto;

import jakarta.validation.constraints.NotBlank;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "Username is required") String username,
            @NotBlank(message = "Password is required") String password) {
    }

    public record LoginResponse(String token, long expiresInSeconds, Me user) {
    }

    /** The signed-in user plus the organization settings the UI needs on boot. */
    public record Me(
            Long id,
            String username,
            String displayName,
            String role,
            Long employerId,
            Long organizationId,
            String organizationName,
            String currencyCode) {
    }
}
