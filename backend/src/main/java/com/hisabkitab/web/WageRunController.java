package com.hisabkitab.web;

import com.hisabkitab.security.AuthPrincipal;
import com.hisabkitab.service.OrganizationService;
import com.hisabkitab.service.WageService;
import com.hisabkitab.web.dto.WageDtos.PostWageRequest;
import com.hisabkitab.web.dto.WageDtos.WagePreview;
import com.hisabkitab.web.dto.WageDtos.WageRunView;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/wage-runs")
public class WageRunController {

    private final WageService wageService;
    private final OrganizationService organizationService;

    public WageRunController(WageService wageService, OrganizationService organizationService) {
        this.wageService = wageService;
        this.organizationService = organizationService;
    }

    /** Computes the month without writing anything. Safe to open repeatedly. */
    @GetMapping("/preview")
    public WagePreview preview(@AuthenticationPrincipal AuthPrincipal principal,
                               @RequestParam(required = false)
                               @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        YearMonth period = month != null
                ? month
                : YearMonth.from(organizationService.today(principal.organizationId()));
        return wageService.preview(principal.organizationId(), period);
    }

    @GetMapping
    public List<WageRunView> list(@AuthenticationPrincipal AuthPrincipal principal) {
        return wageService.list(principal.organizationId());
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public WageRunView post(@AuthenticationPrincipal AuthPrincipal principal,
                            @Valid @RequestBody PostWageRequest request) {
        return wageService.post(principal, request);
    }

    /** Voids the run and every wage entry it created so the month can be redone. */
    @PostMapping("/{id}/void")
    @PreAuthorize("hasRole('OWNER')")
    public WageRunView voidRun(@AuthenticationPrincipal AuthPrincipal principal,
                               @PathVariable Long id) {
        return wageService.voidRun(principal, id);
    }
}
