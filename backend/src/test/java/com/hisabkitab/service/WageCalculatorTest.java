package com.hisabkitab.service;

import com.hisabkitab.domain.AttendanceStatus;
import com.hisabkitab.domain.Employee;
import com.hisabkitab.domain.Organization;
import com.hisabkitab.domain.WageRate;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The wage engine is the one place a mistake quietly costs someone money, so the
 * cases below pin down the rules: presence by default, weekly offs, part months
 * and dated rate changes.
 * <p>
 * July 2026 starts on a Wednesday and has 31 days, four of them Sundays.
 */
class WageCalculatorTest {

    private static final LocalDate JULY_START = LocalDate.of(2026, 7, 1);
    private static final LocalDate JULY_END = LocalDate.of(2026, 7, 31);

    private final WageCalculator calculator = new WageCalculator();

    @Test
    void payFullMonthWhenNothingIsMarked() {
        Organization org = organization();
        Employee employee = employee(LocalDate.of(2026, 1, 1), 400);

        WageCalculator.Result result = calculator.compute(
                org, employee, JULY_START, JULY_END, Map.of(), rates(employee, 400));

        assertThat(result.eligibleDays()).isEqualTo(31);
        assertThat(result.payableDays()).isEqualByComparingTo("31");
        assertThat(result.amount()).isEqualByComparingTo("12400.00");
    }

    @Test
    void subtractAbsencesAndHalveHalfDays() {
        Organization org = organization();
        Employee employee = employee(LocalDate.of(2026, 1, 1), 400);

        Map<LocalDate, AttendanceStatus> marks = new HashMap<>();
        marks.put(LocalDate.of(2026, 7, 6), AttendanceStatus.ABSENT);
        marks.put(LocalDate.of(2026, 7, 7), AttendanceStatus.ABSENT);
        marks.put(LocalDate.of(2026, 7, 8), AttendanceStatus.ABSENT);
        marks.put(LocalDate.of(2026, 7, 9), AttendanceStatus.HALF_DAY);

        WageCalculator.Result result = calculator.compute(
                org, employee, JULY_START, JULY_END, marks, rates(employee, 400));

        assertThat(result.absentDays()).isEqualTo(3);
        assertThat(result.halfDays()).isEqualTo(1);
        // 31 days - 3 absent - half of one day
        assertThat(result.payableDays()).isEqualByComparingTo("27.5");
        assertThat(result.amount()).isEqualByComparingTo("11000.00");
    }

    @Test
    void payPaidLeaveInFullAndOvertimeAtOneAndAHalf() {
        Organization org = organization();
        Employee employee = employee(LocalDate.of(2026, 1, 1), 400);

        Map<LocalDate, AttendanceStatus> marks = new HashMap<>();
        marks.put(LocalDate.of(2026, 7, 6), AttendanceStatus.PAID_LEAVE);
        marks.put(LocalDate.of(2026, 7, 7), AttendanceStatus.OVERTIME);

        WageCalculator.Result result = calculator.compute(
                org, employee, JULY_START, JULY_END, marks, rates(employee, 400));

        // Paid leave is a normal day, overtime adds half a day on top.
        assertThat(result.payableDays()).isEqualByComparingTo("31.5");
        assertThat(result.amount()).isEqualByComparingTo("12600.00");
    }

    @Test
    void skipWeeklyOffDaysEntirely() {
        Organization org = organization();
        org.setWeeklyOffDays(EnumSet.of(DayOfWeek.SUNDAY));
        Employee employee = employee(LocalDate.of(2026, 1, 1), 400);

        // An absence marked on a Sunday must not be counted twice.
        Map<LocalDate, AttendanceStatus> marks =
                Map.of(LocalDate.of(2026, 7, 5), AttendanceStatus.ABSENT);

        WageCalculator.Result result = calculator.compute(
                org, employee, JULY_START, JULY_END, marks, rates(employee, 400));

        assertThat(result.eligibleDays()).isEqualTo(27);
        assertThat(result.absentDays()).isZero();
        assertThat(result.payableDays()).isEqualByComparingTo("27");
        assertThat(result.amount()).isEqualByComparingTo("10800.00");
    }

    @Test
    void payOnlyFromTheJoiningDate() {
        Organization org = organization();
        Employee employee = employee(LocalDate.of(2026, 7, 20), 500);

        WageCalculator.Result result = calculator.compute(
                org, employee, JULY_START, JULY_END, Map.of(),
                rates(employee, 500, LocalDate.of(2026, 7, 20)));

        assertThat(result.eligibleDays()).isEqualTo(12);
        assertThat(result.amount()).isEqualByComparingTo("6000.00");
    }

    @Test
    void stopPayingAfterTheExitDate() {
        Organization org = organization();
        Employee employee = employee(LocalDate.of(2026, 1, 1), 400);
        employee.setExitedOn(LocalDate.of(2026, 7, 10));

        WageCalculator.Result result = calculator.compute(
                org, employee, JULY_START, JULY_END, Map.of(), rates(employee, 400));

        assertThat(result.eligibleDays()).isEqualTo(10);
        assertThat(result.amount()).isEqualByComparingTo("4000.00");
    }

    @Test
    void priceEachDayAtTheRateInForceOnThatDay() {
        Organization org = organization();
        Employee employee = employee(LocalDate.of(2026, 1, 1), 500);

        List<WageRate> history = List.of(
                new WageRate(employee, new BigDecimal("400"), LocalDate.of(2026, 1, 1), "joining"),
                new WageRate(employee, new BigDecimal("500"), LocalDate.of(2026, 7, 16), "raise"));

        WageCalculator.Result result = calculator.compute(
                org, employee, JULY_START, JULY_END, Map.of(), history);

        // 15 days at 400, then 16 days at 500.
        assertThat(result.amount()).isEqualByComparingTo("14000.00");
    }

    @Test
    void fallBackToTheEmployeeRateWhenNoHistoryExists() {
        Organization org = organization();
        Employee employee = employee(LocalDate.of(2026, 1, 1), 450);

        WageCalculator.Result result = calculator.compute(
                org, employee, JULY_START, JULY_END, Map.of(), List.of());

        assertThat(result.amount()).isEqualByComparingTo("13950.00");
    }

    @Test
    void earnNothingWhenTheWholeMonthIsAbsent() {
        Organization org = organization();
        Employee employee = employee(LocalDate.of(2026, 1, 1), 400);

        Map<LocalDate, AttendanceStatus> marks = new HashMap<>();
        for (LocalDate day = JULY_START; !day.isAfter(JULY_END); day = day.plusDays(1)) {
            marks.put(day, AttendanceStatus.ABSENT);
        }

        WageCalculator.Result result = calculator.compute(
                org, employee, JULY_START, JULY_END, marks, rates(employee, 400));

        assertThat(result.payableDays()).isEqualByComparingTo("0");
        assertThat(result.amount()).isEqualByComparingTo("0.00");
        assertThat(result.effectiveRate()).isEqualByComparingTo("0.00");
    }

    private static Organization organization() {
        Organization org = new Organization();
        org.setName("Test Farm");
        return org;
    }

    private static Employee employee(LocalDate joinedOn, int rate) {
        Employee employee = new Employee();
        employee.setName("Test Worker");
        employee.setJoinedOn(joinedOn);
        employee.setDailyWageRate(BigDecimal.valueOf(rate));
        return employee;
    }

    private static List<WageRate> rates(Employee employee, int rate) {
        return rates(employee, rate, employee.getJoinedOn());
    }

    private static List<WageRate> rates(Employee employee, int rate, LocalDate from) {
        return List.of(new WageRate(employee, BigDecimal.valueOf(rate), from, null));
    }
}
