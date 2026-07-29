package com.hisabkitab.service;

import com.hisabkitab.domain.Attendance;
import com.hisabkitab.domain.AttendanceStatus;
import com.hisabkitab.domain.Employee;
import com.hisabkitab.domain.EntryType;
import com.hisabkitab.domain.LedgerEntry;
import com.hisabkitab.domain.Organization;
import com.hisabkitab.domain.Role;
import com.hisabkitab.domain.WageRate;
import com.hisabkitab.domain.WageRun;
import com.hisabkitab.domain.WageRunStatus;
import com.hisabkitab.exception.ApiExceptions;
import com.hisabkitab.repository.AttendanceRepository;
import com.hisabkitab.repository.EmployeeRepository;
import com.hisabkitab.repository.LedgerEntryRepository;
import com.hisabkitab.repository.OrganizationRepository;
import com.hisabkitab.repository.WageRateRepository;
import com.hisabkitab.repository.WageRunRepository;
import com.hisabkitab.security.AuthPrincipal;
import com.hisabkitab.web.dto.WageDtos.PostWageRequest;
import com.hisabkitab.web.dto.WageDtos.WagePreview;
import com.hisabkitab.web.dto.WageDtos.WagePreviewLine;
import com.hisabkitab.web.dto.WageDtos.WageRunView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Monthly wage posting.
 * <p>
 * A preview is pure computation and writes nothing, so it can be opened as often
 * as needed. Posting writes one WAGE entry per employee and locks the month via
 * the unique constraint on (organization, periodStart), which is what prevents a
 * month being paid twice.
 */
@Service
public class WageService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WageService.class);

    private static final DateTimeFormatter MONTH_LABEL =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    private final WageRunRepository wageRuns;
    private final EmployeeRepository employees;
    private final AttendanceRepository attendance;
    private final WageRateRepository wageRates;
    private final LedgerEntryRepository ledger;
    private final OrganizationRepository organizations;
    private final OrganizationService organizationService;
    private final LedgerService ledgerService;
    private final WageCalculator calculator;
    private final WageAccrualService accrualService;
    private final com.hisabkitab.repository.WorkRecordRepository workRecords;

    public WageService(WageRunRepository wageRuns,
                       EmployeeRepository employees,
                       AttendanceRepository attendance,
                       WageRateRepository wageRates,
                       LedgerEntryRepository ledger,
                       OrganizationRepository organizations,
                       OrganizationService organizationService,
                       LedgerService ledgerService,
                       WageCalculator calculator,
                       WageAccrualService accrualService,
                       com.hisabkitab.repository.WorkRecordRepository workRecords) {
        this.accrualService = accrualService;
        this.workRecords = workRecords;
        this.wageRuns = wageRuns;
        this.employees = employees;
        this.attendance = attendance;
        this.wageRates = wageRates;
        this.ledger = ledger;
        this.organizations = organizations;
        this.organizationService = organizationService;
        this.ledgerService = ledgerService;
        this.calculator = calculator;
    }

    @Transactional(readOnly = true)
    public WagePreview preview(Long organizationId, YearMonth period) {
        Organization org = organizationService.require(organizationId);
        LocalDate start = period.atDay(1);
        LocalDate end = period.atEndOfMonth();

        List<Employee> staff = employees.findEmployedDuring(organizationId, start, end);
        Map<Long, WageCalculator.Result> results = computeAll(org, staff, start, end);

        List<WagePreviewLine> lines = new ArrayList<>();
        BigDecimal total = Money.ZERO;
        for (Employee employee : staff) {
            WageCalculator.Result r = results.get(employee.getId());
            if (r.eligibleDays() == 0) {
                continue;
            }
            lines.add(new WagePreviewLine(
                    employee.getId(),
                    employee.getName(),
                    employee.getCode(),
                    employee.getDailyWageRate(),
                    r.eligibleDays(),
                    r.payableDays(),
                    r.absentDays(),
                    r.halfDays(),
                    r.overtimeDays(),
                    r.amount()));
            total = total.add(r.amount());
        }

        var posted = wageRuns.findByOrganizationIdAndPeriodStartAndStatus(
                organizationId, start, WageRunStatus.POSTED);

        return new WagePreview(
                period,
                start,
                end,
                workingDays(org, start, end),
                Money.scale(total),
                posted.isPresent(),
                posted.map(WageRun::getId).orElse(null),
                lines);
    }

    @Transactional
    public WageRunView post(AuthPrincipal principal, PostWageRequest request) {
        return postInternal(principal, request.period(), request.employeeIds(), false);
    }

    /**
     * Closes every month that has fully ended and is not yet posted, so the
     * employer never has to remember to do it. Balances are already live via
     * {@link WageAccrualService}; this just makes the history permanent and
     * keeps the accrual window short.
     *
     * @return how many months were closed
     */
    @Transactional
    public int autoCloseCompletedMonths(AuthPrincipal principal) {
        Long organizationId = principal.organizationId();
        YearMonth current = YearMonth.from(organizationService.today(organizationId));

        LocalDate firstJoin = employees.findByOrganizationIdOrderByNameAsc(organizationId).stream()
                .map(Employee::getJoinedOn)
                .min(Comparator.naturalOrder())
                .orElse(null);
        if (firstJoin == null) {
            return 0;
        }

        // Every month already closed, fetched once. Scanning for gaps rather than
        // resuming after the newest run is what lets a voided month be picked up
        // and reposted — otherwise correcting an old month would drop it forever.
        Set<LocalDate> alreadyClosed = wageRuns
                .findByOrganizationIdOrderByPeriodStartDesc(organizationId).stream()
                .filter(run -> run.getStatus() == WageRunStatus.POSTED)
                .map(WageRun::getPeriodStart)
                .collect(Collectors.toSet());

        int closed = 0;
        int examined = 0;
        YearMonth month = YearMonth.from(firstJoin);
        // Bounded so a long-dormant database cannot spin through decades.
        while (month.isBefore(current) && examined < 120) {
            examined++;
            if (!alreadyClosed.contains(month.atDay(1))) {
                postInternal(principal, month, null, true);
                closed++;
            }
            month = month.plusMonths(1);
        }
        if (closed > 0) {
            log.info("Auto-closed {} wage month(s) for organization {}", closed, organizationId);
        }
        return closed;
    }

    /**
     * @param allowEmpty when true a month with nothing to pay still records a
     *                   run, so the month counts as closed and is not retried
     *                   on every request
     */
    private WageRunView postInternal(AuthPrincipal principal,
                                     YearMonth period,
                                     List<Long> employeeIds,
                                     boolean allowEmpty) {
        Long organizationId = principal.organizationId();
        Organization org = organizationService.require(organizationId);
        LocalDate start = period.atDay(1);
        LocalDate end = period.atEndOfMonth();

        if (start.isAfter(organizationService.today(organizationId))) {
            throw new ApiExceptions.BadRequestException("That month has not started yet");
        }
        if (wageRuns.existsByOrganizationIdAndPeriodStartAndStatus(
                organizationId, start, WageRunStatus.POSTED)) {
            throw new ApiExceptions.ConflictException(
                    MONTH_LABEL.format(start) + " has already been posted. Void that run first to repost.");
        }

        List<Employee> staff = employees.findEmployedDuring(organizationId, start, end);
        if (employeeIds != null && !employeeIds.isEmpty()) {
            staff = staff.stream()
                    .filter(e -> employeeIds.contains(e.getId()))
                    .toList();
        }
        if (staff.isEmpty() && !allowEmpty) {
            throw new ApiExceptions.BadRequestException("No employees were on the books that month");
        }

        Map<Long, WageCalculator.Result> results = computeAll(org, staff, start, end);

        WageRun run = new WageRun();
        run.setOrganization(organizations.getReferenceById(organizationId));
        run.setPeriodStart(start);
        run.setPeriodEnd(end);
        run.setStatus(WageRunStatus.POSTED);
        run.setPostedAt(Instant.now());
        run.setPostedBy(ledgerService.userRef(principal.userId()));
        run = wageRuns.save(run);

        BigDecimal total = Money.ZERO;
        int counted = 0;
        String label = MONTH_LABEL.format(start);

        // Piece-rate earnings for the same period. Anyone can log work, whatever
        // their employee type, so this is keyed off records rather than type.
        Map<Long, BigDecimal> pieceWork = new HashMap<>();
        for (var row : workRecords.totalsByEmployee(organizationId, start, end)) {
            pieceWork.put(row.getEmployeeId(), Money.nullToZero(row.getTotal()));
        }

        for (Employee employee : staff) {
            BigDecimal earned = pieceWork.getOrDefault(employee.getId(), Money.ZERO);
            if (earned.signum() > 0) {
                LedgerEntry entry = new LedgerEntry();
                entry.setOrganization(run.getOrganization());
                entry.setEmployee(employee);
                entry.setWageRun(run);
                entry.setEntryType(EntryType.PIECE_WORK);
                entry.setAmount(earned);
                entry.setSignedAmount(earned);
                entry.setEntryDate(end);
                entry.setNote("Work completed in " + label);
                entry.setCreatedBy(ledgerService.userRef(principal.userId()));
                ledger.save(entry);

                total = total.add(earned);
                counted++;
            }

            WageCalculator.Result r = results.get(employee.getId());
            if (r.amount().signum() <= 0) {
                continue;
            }
            LedgerEntry entry = new LedgerEntry();
            entry.setOrganization(run.getOrganization());
            entry.setEmployee(employee);
            entry.setWageRun(run);
            entry.setEntryType(EntryType.WAGE);
            entry.setAmount(r.amount());
            entry.setSignedAmount(r.amount());
            // Dated at month end so it lands after that month's advances.
            entry.setEntryDate(end);
            entry.setNote("Wages for " + label + " · " + r.payableDays().toPlainString() + " days");
            entry.setCreatedBy(ledgerService.userRef(principal.userId()));
            ledger.save(entry);

            total = total.add(r.amount());
            counted++;
        }

        if (counted == 0 && !allowEmpty) {
            throw new ApiExceptions.BadRequestException(
                    "Nothing to post for " + label + " — no payable days were found.");
        }

        run.setTotalAmount(Money.scale(total));
        run = wageRuns.save(run);

        return toView(run, counted, principal.displayName());
    }

    /**
     * Undoes a posting by voiding the run and all entries it created, so the
     * month can be corrected and posted again. Owners only.
     */
    @Transactional
    public WageRunView voidRun(AuthPrincipal principal, Long runId) {
        if (principal.role() != Role.OWNER) {
            throw new ApiExceptions.BadRequestException("Only an owner can void a posted wage run");
        }
        WageRun run = wageRuns.findByIdAndOrganizationId(runId, principal.organizationId())
                .orElseThrow(() -> ApiExceptions.NotFoundException.of("Wage run", runId));

        if (run.getStatus() == WageRunStatus.VOIDED) {
            return toView(run, 0, null);
        }

        List<LedgerEntry> entries = ledger.findByWageRunIdAndVoidedFalse(runId);
        Instant now = Instant.now();
        for (LedgerEntry entry : entries) {
            entry.setVoided(true);
            entry.setVoidedAt(now);
            entry.setVoidedBy(ledgerService.userRef(principal.userId()));
        }
        ledger.saveAll(entries);

        run.setStatus(WageRunStatus.VOIDED);
        wageRuns.save(run);
        return toView(run, entries.size(), principal.displayName());
    }

    @Transactional(readOnly = true)
    public List<WageRunView> list(Long organizationId) {
        return wageRuns.findByOrganizationIdOrderByPeriodStartDesc(organizationId).stream()
                .map(run -> toView(run,
                        ledger.findByWageRunIdAndVoidedFalse(run.getId()).size(),
                        run.getPostedBy() == null ? null : run.getPostedBy().getDisplayName()))
                .toList();
    }

    /** Loads attendance and rates for the whole cohort once, then prices each employee. */
    private Map<Long, WageCalculator.Result> computeAll(Organization org,
                                                        List<Employee> staff,
                                                        LocalDate start,
                                                        LocalDate end) {
        List<Long> ids = staff.stream().map(Employee::getId).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }

        Map<Long, Map<LocalDate, AttendanceStatus>> byEmployee = new HashMap<>();
        for (Attendance row : attendance.findForEmployeesBetween(ids, start, end)) {
            byEmployee.computeIfAbsent(row.getEmployee().getId(), k -> new HashMap<>())
                    .put(row.getWorkDate(), row.getStatus());
        }

        Map<Long, List<WageRate>> ratesByEmployee = new HashMap<>();
        for (WageRate rate : wageRates.findApplicableRates(ids, end)) {
            ratesByEmployee.computeIfAbsent(rate.getEmployee().getId(), k -> new ArrayList<>()).add(rate);
        }

        Map<Long, WageCalculator.Result> results = new HashMap<>();
        for (Employee employee : staff) {
            results.put(employee.getId(), calculator.compute(
                    org,
                    employee,
                    start,
                    end,
                    byEmployee.getOrDefault(employee.getId(), Map.of()),
                    ratesByEmployee.getOrDefault(employee.getId(), List.of())));
        }
        return results;
    }

    private static int workingDays(Organization org, LocalDate start, LocalDate end) {
        int days = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            if (org.isWorkingDay(d)) {
                days++;
            }
        }
        return days;
    }

    private static WageRunView toView(WageRun run, int employeeCount, String postedByName) {
        return new WageRunView(
                run.getId(),
                run.getPeriodStart(),
                run.getPeriodEnd(),
                run.getStatus().name(),
                run.getTotalAmount(),
                employeeCount,
                run.getPostedAt(),
                postedByName);
    }
}
