package com.hisabkitab.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;

public final class OrganizationDtos {

    private OrganizationDtos() {
    }

    public record OrganizationView(
            Long id,
            String name,
            String currencyCode,
            String timeZone,
            Set<DayOfWeek> weeklyOffDays) {
    }

    public record UpdateOrganizationRequest(
            @NotBlank(message = "Name is required")
            @Size(max = 160, message = "Name is too long")
            String name,

            @Size(max = 3, message = "Use a 3 letter currency code")
            String currencyCode,

            @Size(max = 64, message = "Time zone is too long")
            String timeZone,

            /** Days that are unpaid weekly offs. Empty means every day is a working day. */
            List<DayOfWeek> weeklyOffDays) {
    }
}
