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

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(DashboardController.class);

    private final DashboardService dashboardService;
    private final OrganizationService organizationService;
    private final com.hisabkitab.service.WageService wageService;

    public DashboardController(DashboardService dashboardService,
                               OrganizationService organizationService,
                               com.hisabkitab.service.WageService wageService) {
        this.dashboardService = dashboardService;
        this.organizationService = organizationService;
        this.wageService = wageService;
    }

    @GetMapping("/dashboard")
    public Dashboard dashboard(@AuthenticationPrincipal AuthPrincipal principal) {
        // Closing months is a bookkeeping detail, not something the employer
        // should have to remember, so it happens on the way to the home screen.
        // A failure here must never block the dashboard from rendering.
        try {
            wageService.autoCloseCompletedMonths(principal);
        } catch (RuntimeException ex) {
            log.warn("Auto-close of completed wage months failed for organization {}",
                    principal.organizationId(), ex);
        }
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
