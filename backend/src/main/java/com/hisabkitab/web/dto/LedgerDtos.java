package com.hisabkitab.web.dto;

import com.hisabkitab.domain.EntryType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class LedgerDtos {

    private LedgerDtos() {
    }

    public record CreateEntryRequest(
            @NotNull(message = "Employee is required")
            Long employeeId,

            /** Whose money moved. Optional for wage-side entries. */
            Long employerId,

            @NotNull(message = "Entry type is required")
            EntryType entryType,

            @NotNull(message = "Amount is required")
            @DecimalMin(value = "0.01", message = "Amount must be more than zero")
            @Digits(integer = 12, fraction = 2, message = "Amount has too many digits")
            BigDecimal amount,

            @NotNull(message = "Date is required")
            LocalDate entryDate,

            /**
             * Only read for ADJUSTMENT, where the caller decides the direction.
             * True credits the employee, false debits them.
             */
            Boolean creditsEmployee,

            @Size(max = 500, message = "Note is too long")
            String note) {
    }

    public record EntryView(
            Long id,
            Long employeeId,
            String employeeName,
            Long employerId,
            String employerName,
            EntryType entryType,
            String typeLabel,
            BigDecimal amount,
            BigDecimal signedAmount,
            LocalDate entryDate,
            String note,
            boolean voided,
            Long wageRunId,
            Instant createdAt) {
    }

    /** One statement line with the balance after it was applied. */
    public record StatementRow(
            Long id,
            LocalDate entryDate,
            EntryType entryType,
            String typeLabel,
            String note,
            String employerName,
            BigDecimal amount,
            BigDecimal signedAmount,
            BigDecimal runningBalance,
            boolean voided,
            Long wageRunId) {
    }

    /**
     * @param closingBalance running total of the rows shown, ledger only
     * @param unpostedWages  earned since the last closed month and not yet a row
     * @param liveBalance    closingBalance plus unpostedWages — what the worker
     *                       actually stands at today
     */
    public record StatementResponse(
            Long employeeId,
            String employeeName,
            LocalDate from,
            LocalDate to,
            BigDecimal openingBalance,
            BigDecimal closingBalance,
            BigDecimal totalGivenOut,
            BigDecimal totalEarned,
            BigDecimal unpostedWages,
            LocalDate unpostedSince,
            BigDecimal liveBalance,
            List<StatementRow> rows) {
    }

    public record PagedEntries(List<EntryView> content, int page, int size, long totalElements, int totalPages) {
    }
}
