package com.hisabkitab.web.dto;

import com.hisabkitab.domain.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class AttendanceDtos {

    private AttendanceDtos() {
    }

    /** Marks a deviation from a normal full day. Sending status null clears the mark. */
    public record MarkRequest(
            @NotNull(message = "Employee is required")
            Long employeeId,

            @NotNull(message = "Date is required")
            LocalDate workDate,

            AttendanceStatus status,

            @Size(max = 255, message = "Note is too long")
            String note) {
    }

    public record BulkMarkRequest(
            @NotNull(message = "Date is required")
            LocalDate workDate,

            @NotNull(message = "Select at least one employee")
            List<Long> employeeIds,

            AttendanceStatus status,

            @Size(max = 255, message = "Note is too long")
            String note) {
    }

    public record AttendanceView(
            Long id,
            Long employeeId,
            String employeeName,
            LocalDate workDate,
            AttendanceStatus status,
            String note) {
    }

    /** Month view for one employee: only the exception days, plus the derived totals. */
    public record EmployeeMonth(
            Long employeeId,
            String employeeName,
            LocalDate periodStart,
            LocalDate periodEnd,
            int workingDaysInPeriod,
            BigDecimal payableDays,
            int absentDays,
            int halfDays,
            int paidLeaveDays,
            int overtimeDays,
            List<AttendanceView> exceptions) {
    }

    /** Day view across the farm, for the "mark today's absentees" screen. */
    public record DayRoster(
            LocalDate workDate,
            boolean workingDay,
            List<RosterEntry> employees) {
    }

    /**
     * @param employeeType the UI needs this to know whether an unmarked day
     *                     means "present" or "did not work"
     */
    public record RosterEntry(
            Long employeeId,
            String employeeName,
            String code,
            com.hisabkitab.domain.EmployeeType employeeType,
            AttendanceStatus status,
            String note) {
    }
}
