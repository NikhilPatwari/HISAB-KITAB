package com.hisabkitab.service;

import com.hisabkitab.domain.Attendance;
import com.hisabkitab.domain.AttendanceStatus;
import com.hisabkitab.domain.Employee;
import com.hisabkitab.domain.Organization;
import com.hisabkitab.domain.WageRate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Turns attendance exceptions into money.
 * <p>
 * Presence is the default: a working day inside the employment window with no
 * attendance row earns a full day at the rate that applied on that day. Only
 * absences, half days and overtime are recorded, so the common case costs
 * nothing to store.
 */
@Component
public class WageCalculator {

    /**
     * @param eligibleDays working days the employee was on the books for
     * @param payableDays  eligible days weighted by attendance, e.g. 25.5
     * @param amount       payableDays priced at the rate applicable to each day
     */
    public record Result(
            int eligibleDays,
            BigDecimal payableDays,
            int absentDays,
            int halfDays,
            int paidLeaveDays,
            int overtimeDays,
            BigDecimal amount,
            BigDecimal effectiveRate) {
    }

    public Result compute(Organization organization,
                          Employee employee,
                          LocalDate periodStart,
                          LocalDate periodEnd,
                          Map<LocalDate, AttendanceStatus> exceptions,
                          List<WageRate> rateHistory) {

        List<WageRate> rates = rateHistory.stream()
                .sorted(Comparator.comparing(WageRate::getEffectiveFrom).reversed())
                .toList();

        int eligibleDays = 0;
        int absent = 0;
        int half = 0;
        int paidLeave = 0;
        int overtime = 0;
        BigDecimal payableDays = BigDecimal.ZERO;
        BigDecimal amount = BigDecimal.ZERO;

        for (LocalDate day = periodStart; !day.isAfter(periodEnd); day = day.plusDays(1)) {
            if (!organization.isWorkingDay(day) || !employee.isEmployedOn(day)) {
                continue;
            }
            eligibleDays++;

            AttendanceStatus status = exceptions.get(day);
            BigDecimal fraction = status == null ? BigDecimal.ONE : status.dayFraction();

            if (status != null) {
                switch (status) {
                    case ABSENT -> absent++;
                    case HALF_DAY -> half++;
                    case PAID_LEAVE -> paidLeave++;
                    case OVERTIME -> overtime++;
                }
            }

            payableDays = payableDays.add(fraction);
            amount = amount.add(fraction.multiply(rateOn(day, rates, employee)));
        }

        BigDecimal total = Money.scale(amount);
        BigDecimal effectiveRate = payableDays.signum() == 0
                ? Money.ZERO
                : Money.scale(total.divide(payableDays, 6, java.math.RoundingMode.HALF_UP));

        // Scale 1 keeps half days readable as 25.5 and avoids BigDecimal's
        // exponent notation leaking into JSON.
        return new Result(eligibleDays, payableDays.setScale(1, java.math.RoundingMode.HALF_UP),
                absent, half, paidLeave, overtime, total, effectiveRate);
    }

    /** The rate in force on {@code day}: the newest one that started on or before it. */
    private BigDecimal rateOn(LocalDate day, List<WageRate> ratesNewestFirst, Employee employee) {
        for (WageRate rate : ratesNewestFirst) {
            if (!rate.getEffectiveFrom().isAfter(day)) {
                return rate.getDailyRate();
            }
        }
        // The day predates every recorded rate — fall back to the oldest one on
        // file, and to the employee's current rate if there is no history at all.
        if (!ratesNewestFirst.isEmpty()) {
            return ratesNewestFirst.get(ratesNewestFirst.size() - 1).getDailyRate();
        }
        return employee.getDailyWageRate();
    }

    /** Attendance rows keyed by date, for a single employee. */
    public static Map<LocalDate, AttendanceStatus> index(List<Attendance> rows) {
        return rows.stream().collect(java.util.stream.Collectors.toMap(
                Attendance::getWorkDate,
                Attendance::getStatus,
                (a, b) -> b));
    }
}
