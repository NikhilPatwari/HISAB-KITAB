package com.hisabkitab.web;

import com.hisabkitab.domain.EmployeeStatus;
import com.hisabkitab.security.AuthPrincipal;
import com.hisabkitab.service.EmployeeService;
import com.hisabkitab.web.dto.EmployeeDtos.ChangeWageRequest;
import com.hisabkitab.web.dto.EmployeeDtos.EmployeeDetail;
import com.hisabkitab.web.dto.EmployeeDtos.EmployeeRequest;
import com.hisabkitab.web.dto.EmployeeDtos.EmployeeSummary;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public List<EmployeeSummary> list(@AuthenticationPrincipal AuthPrincipal principal,
                                      @RequestParam(required = false) EmployeeStatus status,
                                      @RequestParam(required = false) String search) {
        return employeeService.list(principal.organizationId(), status, search);
    }

    @GetMapping("/{id}")
    public EmployeeDetail get(@AuthenticationPrincipal AuthPrincipal principal,
                              @PathVariable Long id) {
        return employeeService.detail(principal.organizationId(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OWNER')")
    public EmployeeDetail create(@AuthenticationPrincipal AuthPrincipal principal,
                                 @Valid @RequestBody EmployeeRequest request) {
        return employeeService.create(principal.organizationId(), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public EmployeeDetail update(@AuthenticationPrincipal AuthPrincipal principal,
                                 @PathVariable Long id,
                                 @Valid @RequestBody EmployeeRequest request) {
        return employeeService.update(principal.organizationId(), id, request);
    }

    /** Back-dates or forward-dates a wage change without touching past months already posted. */
    @PostMapping("/{id}/wage")
    @PreAuthorize("hasRole('OWNER')")
    public EmployeeDetail changeWage(@AuthenticationPrincipal AuthPrincipal principal,
                                     @PathVariable Long id,
                                     @Valid @RequestBody ChangeWageRequest request) {
        return employeeService.changeWage(principal.organizationId(), id, request);
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasRole('OWNER')")
    public EmployeeDetail setStatus(@AuthenticationPrincipal AuthPrincipal principal,
                                    @PathVariable Long id,
                                    @RequestParam EmployeeStatus status,
                                    @RequestParam(required = false)
                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate exitedOn) {
        return employeeService.setStatus(principal.organizationId(), id, status, exitedOn);
    }
}
