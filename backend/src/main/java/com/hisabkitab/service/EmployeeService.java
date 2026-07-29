package com.hisabkitab.service;

import com.hisabkitab.domain.Employee;
import com.hisabkitab.domain.EmployeeStatus;
import com.hisabkitab.domain.EmployeeType;
import com.hisabkitab.domain.WageRate;
import com.hisabkitab.exception.ApiExceptions;
import com.hisabkitab.repository.EmployeeBalanceRow;
import com.hisabkitab.repository.EmployeeRepository;
import com.hisabkitab.repository.LedgerEntryRepository;
import com.hisabkitab.repository.OrganizationRepository;
import com.hisabkitab.repository.WageRateRepository;
import com.hisabkitab.web.dto.EmployeeDtos.ChangeWageRequest;
import com.hisabkitab.web.dto.EmployeeDtos.EmployeeDetail;
import com.hisabkitab.web.dto.EmployeeDtos.EmployeeRequest;
import com.hisabkitab.web.dto.EmployeeDtos.EmployeeSummary;
import com.hisabkitab.web.dto.EmployeeDtos.WageRateView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class EmployeeService {

    private final EmployeeRepository employees;
    private final OrganizationRepository organizations;
    private final WageRateRepository wageRates;
    private final LedgerEntryRepository ledger;
    private final WageAccrualService accrualService;

    public EmployeeService(EmployeeRepository employees,
                           OrganizationRepository organizations,
                           WageRateRepository wageRates,
                           LedgerEntryRepository ledger,
                           WageAccrualService accrualService) {
        this.employees = employees;
        this.organizations = organizations;
        this.wageRates = wageRates;
        this.ledger = ledger;
        this.accrualService = accrualService;
    }

    /**
     * Home list. {@code search} matches name, code or phone; {@code status} filters
     * by ACTIVE/INACTIVE and may be null for everyone.
     */
    @Transactional(readOnly = true)
    public List<EmployeeSummary> list(Long organizationId, EmployeeStatus status, String search) {
        List<Employee> rows = status == null
                ? employees.findByOrganizationIdOrderByNameAsc(organizationId)
                : employees.findByOrganizationIdAndStatusOrderByNameAsc(organizationId, status);

        Map<Long, BigDecimal> posted = postedBalanceMap(organizationId);
        Map<Long, BigDecimal> accrued = accrualService.unpostedByEmployee(organizationId);

        String needle = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        return rows.stream()
                .filter(e -> needle.isEmpty() || matches(e, needle))
                .map(e -> toSummary(e,
                        posted.getOrDefault(e.getId(), Money.ZERO),
                        accrued.getOrDefault(e.getId(), Money.ZERO)))
                .toList();
    }

    @Transactional(readOnly = true)
    public Employee require(Long organizationId, Long employeeId) {
        return employees.findByIdAndOrganizationId(employeeId, organizationId)
                .orElseThrow(() -> ApiExceptions.NotFoundException.of("Employee", employeeId));
    }

    @Transactional(readOnly = true)
    public EmployeeDetail detail(Long organizationId, Long employeeId) {
        Employee employee = require(organizationId, employeeId);
        BigDecimal postedBalance = Money.nullToZero(ledger.balanceForEmployee(employeeId));
        BigDecimal unposted = accrualService.unpostedFor(organizationId, employeeId);
        List<WageRateView> history = wageRates.findByEmployeeIdOrderByEffectiveFromDesc(employeeId).stream()
                .map(r -> new WageRateView(r.getId(), r.getDailyRate(), r.getEffectiveFrom(), r.getNote()))
                .toList();
        return new EmployeeDetail(
                employee.getId(),
                employee.getCode(),
                employee.getName(),
                employee.getPhone(),
                employee.getVillage(),
                employee.getEmployeeType(),
                employee.getDailyWageRate(),
                employee.getJoinedOn(),
                employee.getExitedOn(),
                employee.getStatus().name(),
                employee.getNotes(),
                Money.scale(postedBalance.add(unposted)),
                postedBalance,
                unposted,
                history);
    }

    @Transactional
    public EmployeeDetail create(Long organizationId, EmployeeRequest request) {
        validate(request);
        String code = blankToNull(request.code());
        if (code != null && employees.existsByOrganizationIdAndCode(organizationId, code)) {
            throw new ApiExceptions.ConflictException("Another employee already uses code " + code);
        }

        Employee employee = new Employee();
        employee.setOrganization(organizations.getReferenceById(organizationId));
        apply(employee, request, code);
        employee = employees.save(employee);

        // Seed the rate history from the joining date so wage runs for early
        // months price those days correctly.
        wageRates.save(new WageRate(employee, employee.getDailyWageRate(),
                employee.getJoinedOn(), "Rate at joining"));

        return detail(organizationId, employee.getId());
    }

    @Transactional
    public EmployeeDetail update(Long organizationId, Long employeeId, EmployeeRequest request) {
        validate(request);
        Employee employee = require(organizationId, employeeId);

        String code = blankToNull(request.code());
        if (code != null && !code.equals(employee.getCode())
                && employees.existsByOrganizationIdAndCode(organizationId, code)) {
            throw new ApiExceptions.ConflictException("Another employee already uses code " + code);
        }

        BigDecimal previousRate = employee.getDailyWageRate();
        apply(employee, request, code);
        employees.save(employee);

        // A rate edited here takes effect from today onward; use the dedicated
        // wage-change endpoint to back-date a raise.
        if (previousRate.compareTo(employee.getDailyWageRate()) != 0) {
            recordRate(employee, employee.getDailyWageRate(),
                    java.time.LocalDate.now(), "Updated from employee profile");
        }
        return detail(organizationId, employeeId);
    }

    @Transactional
    public EmployeeDetail changeWage(Long organizationId, Long employeeId, ChangeWageRequest request) {
        Employee employee = require(organizationId, employeeId);
        recordRate(employee, request.dailyRate(), request.effectiveFrom(), request.note());

        // Keep the denormalised rate pointing at whatever applies from today.
        wageRates.findByEmployeeIdOrderByEffectiveFromDesc(employeeId).stream()
                .filter(r -> !r.getEffectiveFrom().isAfter(java.time.LocalDate.now()))
                .max(Comparator.comparing(WageRate::getEffectiveFrom))
                .ifPresent(current -> {
                    employee.setDailyWageRate(current.getDailyRate());
                    employees.save(employee);
                });

        return detail(organizationId, employeeId);
    }

    @Transactional
    public EmployeeDetail setStatus(Long organizationId, Long employeeId,
                                    EmployeeStatus status, java.time.LocalDate exitedOn) {
        Employee employee = require(organizationId, employeeId);
        employee.setStatus(status);
        if (status == EmployeeStatus.INACTIVE) {
            employee.setExitedOn(exitedOn != null ? exitedOn : java.time.LocalDate.now());
            if (employee.getExitedOn().isBefore(employee.getJoinedOn())) {
                throw new ApiExceptions.BadRequestException("Exit date cannot be before the joining date");
            }
        } else {
            employee.setExitedOn(null);
        }
        employees.save(employee);
        return detail(organizationId, employeeId);
    }

    /** Ledger sums only — what has actually been written. */
    @Transactional(readOnly = true)
    public Map<Long, BigDecimal> postedBalanceMap(Long organizationId) {
        Map<Long, BigDecimal> balances = new HashMap<>();
        for (EmployeeBalanceRow row : ledger.balancesByEmployee(organizationId)) {
            balances.put(row.getEmployeeId(), Money.nullToZero(row.getBalance()));
        }
        return balances;
    }

    /**
     * Ledger sums plus wages earned since the last closed month. This is the
     * figure every screen shows, so a balance is never stale between postings.
     */
    @Transactional(readOnly = true)
    public Map<Long, BigDecimal> balanceMap(Long organizationId) {
        Map<Long, BigDecimal> posted = postedBalanceMap(organizationId);
        Map<Long, BigDecimal> accrued = accrualService.unpostedByEmployee(organizationId);

        Map<Long, BigDecimal> live = new HashMap<>(posted);
        accrued.forEach((employeeId, wages) ->
                live.merge(employeeId, wages, BigDecimal::add));
        return live;
    }

    private void recordRate(Employee employee, BigDecimal rate, java.time.LocalDate from, String note) {
        wageRates.findByEmployeeIdOrderByEffectiveFromDesc(employee.getId()).stream()
                .filter(r -> r.getEffectiveFrom().equals(from))
                .findFirst()
                .ifPresentOrElse(existing -> {
                    existing.setDailyRate(rate);
                    existing.setNote(note);
                    wageRates.save(existing);
                }, () -> wageRates.save(new WageRate(employee, rate, from, note)));
    }

    private void validate(EmployeeRequest request) {
        if (request.exitedOn() != null && request.exitedOn().isBefore(request.joinedOn())) {
            throw new ApiExceptions.BadRequestException("Exit date cannot be before the joining date");
        }
    }

    private void apply(Employee employee, EmployeeRequest request, String code) {
        EmployeeType type = request.employeeType() == null
                ? EmployeeType.PERMANENT
                : request.employeeType();

        employee.setCode(code);
        employee.setName(request.name().trim());
        employee.setPhone(blankToNull(request.phone()));
        employee.setVillage(blankToNull(request.village()));
        employee.setEmployeeType(type);
        // Contract workers earn per unit, so a daily rate would only be a
        // number nobody uses and the wage engine would silently pay it.
        employee.setDailyWageRate(type.usesDailyWage()
                ? Money.scale(request.dailyWageRate())
                : Money.ZERO);
        employee.setJoinedOn(request.joinedOn());
        employee.setExitedOn(request.exitedOn());
        employee.setNotes(blankToNull(request.notes()));
        employee.setStatus(request.exitedOn() != null && !request.exitedOn().isAfter(java.time.LocalDate.now())
                ? EmployeeStatus.INACTIVE
                : EmployeeStatus.ACTIVE);
    }

    private static boolean matches(Employee employee, String needle) {
        return contains(employee.getName(), needle)
                || contains(employee.getCode(), needle)
                || contains(employee.getPhone(), needle)
                || contains(employee.getVillage(), needle);
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    static EmployeeSummary toSummary(Employee employee, BigDecimal posted, BigDecimal accrued) {
        BigDecimal postedBalance = Money.nullToZero(posted);
        BigDecimal unposted = Money.nullToZero(accrued);
        return new EmployeeSummary(
                employee.getId(),
                employee.getCode(),
                employee.getName(),
                employee.getPhone(),
                employee.getVillage(),
                employee.getEmployeeType(),
                employee.getDailyWageRate(),
                employee.getJoinedOn(),
                employee.getExitedOn(),
                employee.getStatus().name(),
                Money.scale(postedBalance.add(unposted)),
                postedBalance,
                unposted);
    }
}
