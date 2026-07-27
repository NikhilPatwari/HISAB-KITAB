package com.hisabkitab.service;

import com.hisabkitab.domain.Attendance;
import com.hisabkitab.domain.AttendanceStatus;
import com.hisabkitab.domain.Employee;
import com.hisabkitab.domain.EmployeeStatus;
import com.hisabkitab.domain.Organization;
import com.hisabkitab.exception.ApiExceptions;
import com.hisabkitab.repository.AttendanceRepository;
import com.hisabkitab.repository.EmployeeRepository;
import com.hisabkitab.repository.OrganizationRepository;
import com.hisabkitab.repository.WageRateRepository;
import com.hisabkitab.web.dto.AttendanceDtos.AttendanceView;
import com.hisabkitab.web.dto.AttendanceDtos.BulkMarkRequest;
import com.hisabkitab.web.dto.AttendanceDtos.DayRoster;
import com.hisabkitab.web.dto.AttendanceDtos.EmployeeMonth;
import com.hisabkitab.web.dto.AttendanceDtos.MarkRequest;
import com.hisabkitab.web.dto.AttendanceDtos.RosterEntry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AttendanceService {

    private final AttendanceRepository attendance;
    private final EmployeeRepository employees;
    private final OrganizationRepository organizations;
    private final WageRateRepository wageRates;
    private final EmployeeService employeeService;
    private final OrganizationService organizationService;
    private final WageCalculator calculator;

    public AttendanceService(AttendanceRepository attendance,
                             EmployeeRepository employees,
                             OrganizationRepository organizations,
                             WageRateRepository wageRates,
                             EmployeeService employeeService,
                             OrganizationService organizationService,
                             WageCalculator calculator) {
        this.attendance = attendance;
        this.employees = employees;
        this.organizations = organizations;
        this.wageRates = wageRates;
        this.employeeService = employeeService;
        this.organizationService = organizationService;
        this.calculator = calculator;
    }

    /**
     * Records a deviation from a normal working day. A null status clears the
     * mark, putting the day back to "present", which is the default.
     */
    @Transactional
    public AttendanceView mark(Long organizationId, MarkRequest request) {
        Employee employee = employeeService.require(organizationId, request.employeeId());
        LocalDate date = request.workDate();

        if (date.isAfter(organizationService.today(organizationId))) {
            throw new ApiExceptions.BadRequestException("Cannot mark attendance for a future date");
        }
        if (!employee.isEmployedOn(date)) {
            throw new ApiExceptions.BadRequestException(
                    employee.getName() + " was not employed on " + date);
        }

        var existing = attendance.findByEmployeeIdAndWorkDate(employee.getId(), date);

        if (request.status() == null) {
            existing.ifPresent(attendance::delete);
            return new AttendanceView(null, employee.getId(), employee.getName(), date, null, null);
        }

        Attendance row = existing.orElseGet(() -> {
            Attendance fresh = new Attendance();
            fresh.setOrganization(organizations.getReferenceById(organizationId));
            fresh.setEmployee(employee);
            fresh.setWorkDate(date);
            return fresh;
        });
        row.setStatus(request.status());
        row.setNote(blankToNull(request.note()));

        return toView(attendance.save(row), employee.getName());
    }

    /** Marks the same status for several employees on one day — the usual morning routine. */
    @Transactional
    public List<AttendanceView> markBulk(Long organizationId, BulkMarkRequest request) {
        if (request.employeeIds().isEmpty()) {
            throw new ApiExceptions.BadRequestException("Select at least one employee");
        }
        return request.employeeIds().stream()
                .map(id -> mark(organizationId,
                        new MarkRequest(id, request.workDate(), request.status(), request.note())))
                .toList();
    }

    /** Who is on the farm today, and who has already been marked. */
    @Transactional(readOnly = true)
    public DayRoster roster(Long organizationId, LocalDate date) {
        Organization org = organizationService.require(organizationId);

        List<Employee> onBooks = employees.findEmployedDuring(organizationId, date, date).stream()
                .filter(e -> e.getStatus() == EmployeeStatus.ACTIVE || e.isEmployedOn(date))
                .toList();

        Map<Long, Attendance> marked = new HashMap<>();
        attendance.findForEmployeesBetween(onBooks.stream().map(Employee::getId).toList(), date, date)
                .forEach(a -> marked.put(a.getEmployee().getId(), a));

        List<RosterEntry> entries = onBooks.stream()
                .map(e -> {
                    Attendance a = marked.get(e.getId());
                    return new RosterEntry(e.getId(), e.getName(), e.getCode(),
                            a == null ? null : a.getStatus(),
                            a == null ? null : a.getNote());
                })
                .toList();

        return new DayRoster(date, org.isWorkingDay(date), entries);
    }

    /** One employee's month: the exception days plus the totals they imply. */
    @Transactional(readOnly = true)
    public EmployeeMonth month(Long organizationId, Long employeeId, YearMonth period) {
        Organization org = organizationService.require(organizationId);
        Employee employee = employeeService.require(organizationId, employeeId);

        LocalDate start = period.atDay(1);
        LocalDate end = period.atEndOfMonth();

        List<Attendance> rows =
                attendance.findByEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(employeeId, start, end);

        WageCalculator.Result result = calculator.compute(org, employee, start, end,
                WageCalculator.index(rows),
                wageRates.findByEmployeeIdOrderByEffectiveFromDesc(employeeId));

        return new EmployeeMonth(
                employee.getId(),
                employee.getName(),
                start,
                end,
                result.eligibleDays(),
                result.payableDays(),
                result.absentDays(),
                result.halfDays(),
                result.paidLeaveDays(),
                result.overtimeDays(),
                rows.stream().map(a -> toView(a, employee.getName())).toList());
    }

    @Transactional(readOnly = true)
    public List<AttendanceView> range(Long organizationId, LocalDate from, LocalDate to) {
        return attendance.findForOrganizationBetween(organizationId, from, to).stream()
                .map(a -> toView(a, a.getEmployee().getName()))
                .toList();
    }

    private static AttendanceView toView(Attendance a, String employeeName) {
        return new AttendanceView(a.getId(), a.getEmployee().getId(), employeeName,
                a.getWorkDate(), a.getStatus(), a.getNote());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
