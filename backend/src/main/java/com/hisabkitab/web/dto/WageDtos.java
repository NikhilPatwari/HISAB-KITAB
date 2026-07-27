package com.hisabkitab.web.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public final class WageDtos {

    private WageDtos() {
    }

    /** What a month would post, computed on the fly. Nothing is written. */
    public record WagePreview(
            YearMonth period,
            LocalDate periodStart,
            LocalDate periodEnd,
            int workingDaysInPeriod,
            BigDecimal totalAmount,
            boolean alreadyPosted,
            Long postedRunId,
            List<WagePreviewLine> lines) {
    }

    public record WagePreviewLine(
            Long employeeId,
            String employeeName,
            String code,
            BigDecimal dailyRate,
            int eligibleDays,
            BigDecimal payableDays,
            int absentDays,
            int halfDays,
            int overtimeDays,
            BigDecimal amount) {
    }

    public record PostWageRequest(
            @NotNull(message = "Month is required")
            YearMonth period,

            /** Leave empty to post for everyone employed during the month. */
            List<Long> employeeIds) {
    }

    public record WageRunView(
            Long id,
            LocalDate periodStart,
            LocalDate periodEnd,
            String status,
            BigDecimal totalAmount,
            int employeeCount,
            Instant postedAt,
            String postedByName) {
    }
}
