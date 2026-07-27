package com.hisabkitab.web;

import com.hisabkitab.security.AuthPrincipal;
import com.hisabkitab.service.DashboardService;
import com.hisabkitab.service.OrganizationService;
import com.hisabkitab.web.dto.DashboardDtos.Dashboard;
import com.hisabkitab.web.dto.OrganizationDtos.OrganizationView;
import com.hisabkitab.web.dto.OrganizationDtos.UpdateOrganizationRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DashboardController {

    private final DashboardService dashboardService;
    private final OrganizationService organizationService;

    public DashboardController(DashboardService dashboardService,
                               OrganizationService organizationService) {
        this.dashboardService = dashboardService;
        this.organizationService = organizationService;
    }

    @GetMapping("/dashboard")
    public Dashboard dashboard(@AuthenticationPrincipal AuthPrincipal principal) {
        return dashboardService.build(principal.organizationId());
    }

    @GetMapping("/organization")
    public OrganizationView organization(@AuthenticationPrincipal AuthPrincipal principal) {
        return organizationService.view(principal.organizationId());
    }

    @PutMapping("/organization")
    @PreAuthorize("hasRole('OWNER')")
    public OrganizationView updateOrganization(@AuthenticationPrincipal AuthPrincipal principal,
                                               @Valid @RequestBody UpdateOrganizationRequest request) {
        return organizationService.update(principal.organizationId(), request);
    }
}
