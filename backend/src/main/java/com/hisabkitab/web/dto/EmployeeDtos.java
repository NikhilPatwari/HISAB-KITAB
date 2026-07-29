package com.hisabkitab.web.dto;

import com.hisabkitab.domain.EmployeeType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class EmployeeDtos {

    private EmployeeDtos() {
    }

    public record EmployeeRequest(
            @Size(max = 32, message = "Code is too long")
            String code,

            @NotBlank(message = "Name is required")
            @Size(max = 160, message = "Name is too long")
            String name,

            @Size(max = 32, message = "Phone is too long")
            String phone,

            @Size(max = 160, message = "Village is too long")
            String village,

            /** Defaults to PERMANENT when omitted, which is what every existing worker is. */
            EmployeeType employeeType,

            @NotNull(message = "Daily wage is required")
            @DecimalMin(value = "0.0", message = "Daily wage cannot be negative")
            BigDecimal dailyWageRate,

            @NotNull(message = "Joining date is required")
            LocalDate joinedOn,

            LocalDate exitedOn,

            @Size(max = 1000, message = "Notes are too long")
            String notes) {
    }

    /**
     * Row on the home list.
     *
     * @param balance       live and signed — negative means the employee owes the
     *                      farm. Already includes {@code unpostedWages}.
     * @param postedBalance the same figure counting only entries written to the
     *                      ledger, for when the split needs showing
     * @param unpostedWages earned since the last closed month, derived from
     *                      attendance rather than stored
     */
    public record EmployeeSummary(
            Long id,
            String code,
            String name,
            String phone,
            String village,
            EmployeeType employeeType,
            BigDecimal dailyWageRate,
            LocalDate joinedOn,
            LocalDate exitedOn,
            String status,
            BigDecimal balance,
            BigDecimal postedBalance,
            BigDecimal unpostedWages) {
    }

    public record EmployeeDetail(
            Long id,
            String code,
            String name,
            String phone,
            String village,
            EmployeeType employeeType,
            BigDecimal dailyWageRate,
            LocalDate joinedOn,
            LocalDate exitedOn,
            String status,
            String notes,
            BigDecimal balance,
            BigDecimal postedBalance,
            BigDecimal unpostedWages,
            List<WageRateView> rateHistory) {
    }

    public record WageRateView(Long id, BigDecimal dailyRate, LocalDate effectiveFrom, String note) {
    }

    public record ChangeWageRequest(
            @NotNull(message = "New daily wage is required")
            @DecimalMin(value = "0.0", message = "Daily wage cannot be negative")
            BigDecimal dailyRate,

            @NotNull(message = "Effective date is required")
            LocalDate effectiveFrom,

            @Size(max = 255, message = "Note is too long")
            String note) {
    }
}
