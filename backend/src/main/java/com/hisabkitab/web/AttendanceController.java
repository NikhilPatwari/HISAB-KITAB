package com.hisabkitab.web;

import com.hisabkitab.security.AuthPrincipal;
import com.hisabkitab.service.AttendanceService;
import com.hisabkitab.service.OrganizationService;
import com.hisabkitab.web.dto.AttendanceDtos.AttendanceView;
import com.hisabkitab.web.dto.AttendanceDtos.BulkMarkRequest;
import com.hisabkitab.web.dto.AttendanceDtos.DayRoster;
import com.hisabkitab.web.dto.AttendanceDtos.EmployeeMonth;
import com.hisabkitab.web.dto.AttendanceDtos.MarkRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final OrganizationService organizationService;

    public AttendanceController(AttendanceService attendanceService,
                                OrganizationService organizationService) {
        this.attendanceService = attendanceService;
        this.organizationService = organizationService;
    }

    /** Everyone on the books for a day, with any mark already recorded. */
    @GetMapping("/roster")
    public DayRoster roster(@AuthenticationPrincipal AuthPrincipal principal,
                            @RequestParam(required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate day = date != null ? date : organizationService.today(principal.organizationId());
        return attendanceService.roster(principal.organizationId(), day);
    }

    /** Sending status = null clears the mark and restores the default full day. */
    @PostMapping("/mark")
    public AttendanceView mark(@AuthenticationPrincipal AuthPrincipal principal,
                               @Valid @RequestBody MarkRequest request) {
        return attendanceService.mark(principal.organizationId(), request);
    }

    @PostMapping("/mark-bulk")
    public List<AttendanceView> markBulk(@AuthenticationPrincipal AuthPrincipal principal,
                                         @Valid @RequestBody BulkMarkRequest request) {
        return attendanceService.markBulk(principal.organizationId(), request);
    }

    @GetMapping("/employee/{employeeId}")
    public EmployeeMonth month(@AuthenticationPrincipal AuthPrincipal principal,
                               @PathVariable Long employeeId,
                               @RequestParam(required = false)
                               @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        YearMonth period = month != null
                ? month
                : YearMonth.from(organizationService.today(principal.organizationId()));
        return attendanceService.month(principal.organizationId(), employeeId, period);
    }

    @GetMapping
    public List<AttendanceView> range(@AuthenticationPrincipal AuthPrincipal principal,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return attendanceService.range(principal.organizationId(), from, to);
    }
}
