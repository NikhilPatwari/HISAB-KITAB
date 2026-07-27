package com.hisabkitab.web;

import com.hisabkitab.security.AuthPrincipal;
import com.hisabkitab.service.EmployerService;
import com.hisabkitab.web.dto.EmployerDtos.EmployerRequest;
import com.hisabkitab.web.dto.EmployerDtos.EmployerView;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/employers")
public class EmployerController {

    private final EmployerService employerService;

    public EmployerController(EmployerService employerService) {
        this.employerService = employerService;
    }

    @GetMapping
    public List<EmployerView> list(@AuthenticationPrincipal AuthPrincipal principal) {
        return employerService.list(principal.organizationId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OWNER')")
    public EmployerView create(@AuthenticationPrincipal AuthPrincipal principal,
                               @Valid @RequestBody EmployerRequest request) {
        return employerService.create(principal.organizationId(), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public EmployerView update(@AuthenticationPrincipal AuthPrincipal principal,
                               @PathVariable Long id,
                               @Valid @RequestBody EmployerRequest request) {
        return employerService.update(principal.organizationId(), id, request);
    }

    /** Deactivates rather than deletes, so historical entries keep their owner. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('OWNER')")
    public void deactivate(@AuthenticationPrincipal AuthPrincipal principal,
                           @PathVariable Long id) {
        employerService.deactivate(principal.organizationId(), id);
    }
}
