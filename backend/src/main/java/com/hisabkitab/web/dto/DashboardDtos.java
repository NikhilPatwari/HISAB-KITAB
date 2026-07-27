package com.hisabkitab.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    /**
     * Headline numbers for the home screen. All amounts are positive magnitudes;
     * the labels carry the direction.
     */
    public record Dashboard(
            int activeEmployees,
            /** Total still owed to the farm by employees who are in debt. */
            BigDecimal totalReceivable,
            /** Total unpaid wages the farm owes employees who are in credit. */
            BigDecimal totalPayable,
            /** totalReceivable minus totalPayable. Positive means the farm is net owed. */
            BigDecimal netPosition,
            int employeesInDebt,
            int employeesInCredit,
            LocalDate monthStart,
            BigDecimal advancesThisMonth,
            BigDecimal wagesThisMonth,
            BigDecimal repaymentsThisMonth,
            List<EmployerPosition> employers,
            List<TopDebtor> topDebtors) {
    }

    public record EmployerPosition(Long employerId, String name, BigDecimal outstanding) {
    }

    public record TopDebtor(Long employeeId, String name, BigDecimal owed) {
    }
}
