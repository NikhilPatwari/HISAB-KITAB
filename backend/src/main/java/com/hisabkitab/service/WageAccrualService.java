package com.hisabkitab.service;

import com.hisabkitab.domain.Attendance;
import com.hisabkitab.domain.AttendanceStatus;
import com.hisabkitab.domain.Employee;
import com.hisabkitab.domain.Organization;
import com.hisabkitab.domain.WageRate;
import com.hisabkitab.domain.WageRun;
import com.hisabkitab.domain.WageRunStatus;
import com.hisabkitab.repository.AttendanceRepository;
import com.hisabkitab.repository.EmployeeRepository;
import com.hisabkitab.repository.WageRateRepository;
import com.hisabkitab.repository.WageRunRepository;
import com.hisabkitab.repository.WorkRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wages earned since the last closed month, computed on demand rather than
 * written nightly.
 * <p>
 * Deriving instead of scheduling matters for this deployment: the server is an
 * office laptop that is not running at midnight, so a cron job would silently
 * skip days. Reading the answer from attendance means it is correct whenever
 * someone opens the app, and marking an absence corrects the balance instantly.
 * <p>
 * Deliberately depends only on repositories, so that {@link EmployeeService} can
 * use it without forming a cycle through {@link LedgerService}.
 */
@Service
public class WageAccrualService {

    private final WageRunRepository wageRuns;
    private final EmployeeRepository employees;
    private final AttendanceRepository attendance;
    private final WageRateRepository wageRates;
    private final WorkRecordRepository workRecords;
    private final OrganizationService organizationService;
    private final WageCalculator calculator;

    public WageAccrualService(WageRunRepository wageRuns,
                              EmployeeRepository employees,
                              AttendanceRepository attendance,
                              WageRateRepository wageRates,
                              WorkRecordRepository workRecords,
                              OrganizationService organizationService,
                              WageCalculator calculator) {
        this.wageRuns = wageRuns;
        this.employees = employees;
        this.attendance = attendance;
        this.wageRates = wageRates;
        this.workRecords = workRecords;
        this.organizationService = organizationService;
        this.calculator = calculator;
    }

    /** Last day covered by a posted wage run, or null when nothing is posted yet. */
    @Transactional(readOnly = true)
    public LocalDate closedThrough(Long organizationId) {
        return wageRuns
                .findTopByOrganizationIdAndStatusOrderByPeriodEndDesc(organizationId, WageRunStatus.POSTED)
                .map(WageRun::getPeriodEnd)
                .orElse(null);
    }

    /**
     * Unposted earnings per employee, from the day after the last closed month
     * up to today. Employees with nothing accrued are present with zero.
     */
    @Transactional(readOnly = true)
    public Map<Long, BigDecimal> unpostedByEmployee(Long organizationId) {
        Organization org = organizationService.require(organizationId);
        LocalDate today = organizationService.today(organizationId);
        LocalDate closedThrough = closedThrough(organizationId);

        List<Employee> staff = employees.findByOrganizationIdOrderByNameAsc(organizationId);
        Map<Long, BigDecimal> accrued = new HashMap<>();
        if (staff.isEmpty()) {
            return accrued;
        }

        // Nothing posted yet means accrual runs from each person's joining date.
        Map<Long, LocalDate> starts = new HashMap<>();
        LocalDate earliest = today;
        for (Employee employee : staff) {
            LocalDate start = closedThrough == null
                    ? employee.getJoinedOn()
                    : closedThrough.plusDays(1);
            if (start.isBefore(employee.getJoinedOn())) {
                start = employee.getJoinedOn();
            }
            starts.put(employee.getId(), start);
            if (start.isBefore(earliest)) {
                earliest = start;
            }
        }

        List<Long> ids = staff.stream().map(Employee::getId).toList();
        Map<Long, Map<LocalDate, AttendanceStatus>> marks = new HashMap<>();
        for (Attendance row : attendance.findForEmployeesBetween(ids, earliest, today)) {
            marks.computeIfAbsent(row.getEmployee().getId(), k -> new HashMap<>())
                    .put(row.getWorkDate(), row.getStatus());
        }

        Map<Long, List<WageRate>> rates = new HashMap<>();
        for (WageRate rate : wageRates.findApplicableRates(ids, today)) {
            rates.computeIfAbsent(rate.getEmployee().getId(), k -> new ArrayList<>()).add(rate);
        }

        for (Employee employee : staff) {
            LocalDate start = starts.get(employee.getId());
            if (start.isAfter(today)) {
                accrued.put(employee.getId(), Money.ZERO);
                continue;
            }
            // The calculator already skips weekly offs and days outside the
            // employment window, so today is a safe upper bound for everyone.
            WageCalculator.Result result = calculator.compute(
                    org, employee, start, today,
                    marks.getOrDefault(employee.getId(), Map.of()),
                    rates.getOrDefault(employee.getId(), List.of()));
            accrued.put(employee.getId(), result.amount());
        }

        // Piece work is additive on top of daily wages: anyone can log it,
        // whatever their employee type.
        LocalDate pieceWorkFrom = closedThrough == null ? earliest : closedThrough.plusDays(1);
        if (!pieceWorkFrom.isAfter(today)) {
            for (var row : workRecords.totalsByEmployee(organizationId, pieceWorkFrom, today)) {
                accrued.merge(row.getEmployeeId(), Money.nullToZero(row.getTotal()), BigDecimal::add);
            }
        }
        return accrued;
    }

    /** Unposted earnings for one employee. */
    @Transactional(readOnly = true)
    public BigDecimal unpostedFor(Long organizationId, Long employeeId) {
        return Money.nullToZero(unpostedByEmployee(organizationId).get(employeeId));
    }

    /** First day not yet covered by a posted run, for labelling the UI. */
    @Transactional(readOnly = true)
    public LocalDate accrualStart(Long organizationId) {
        LocalDate closed = closedThrough(organizationId);
        return closed == null ? null : closed.plusDays(1);
    }
}
