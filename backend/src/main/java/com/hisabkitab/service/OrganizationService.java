package com.hisabkitab.service;

import com.hisabkitab.domain.Organization;
import com.hisabkitab.exception.ApiExceptions;
import com.hisabkitab.repository.OrganizationRepository;
import com.hisabkitab.web.dto.OrganizationDtos.OrganizationView;
import com.hisabkitab.web.dto.OrganizationDtos.UpdateOrganizationRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.Set;

@Service
public class OrganizationService {

    private final OrganizationRepository organizations;

    public OrganizationService(OrganizationRepository organizations) {
        this.organizations = organizations;
    }

    @Transactional(readOnly = true)
    public Organization require(Long organizationId) {
        return organizations.findById(organizationId)
                .orElseThrow(() -> ApiExceptions.NotFoundException.of("Organization", organizationId));
    }

    @Transactional(readOnly = true)
    public OrganizationView view(Long organizationId) {
        return toView(require(organizationId));
    }

    @Transactional
    public OrganizationView update(Long organizationId, UpdateOrganizationRequest request) {
        Organization org = require(organizationId);
        org.setName(request.name().trim());
        if (request.currencyCode() != null && !request.currencyCode().isBlank()) {
            org.setCurrencyCode(request.currencyCode().trim().toUpperCase());
        }
        if (request.timeZone() != null && !request.timeZone().isBlank()) {
            try {
                ZoneId.of(request.timeZone());
            } catch (RuntimeException ex) {
                throw new ApiExceptions.BadRequestException("Unknown time zone: " + request.timeZone());
            }
            org.setTimeZone(request.timeZone());
        }
        Set<DayOfWeek> offs = EnumSet.noneOf(DayOfWeek.class);
        if (request.weeklyOffDays() != null) {
            offs.addAll(request.weeklyOffDays());
        }
        org.setWeeklyOffDays(offs);
        return toView(organizations.save(org));
    }

    /** Today according to the farm's own clock, not the server's. */
    @Transactional(readOnly = true)
    public LocalDate today(Long organizationId) {
        return LocalDate.now(ZoneId.of(require(organizationId).getTimeZone()));
    }

    public static OrganizationView toView(Organization org) {
        return new OrganizationView(
                org.getId(),
                org.getName(),
                org.getCurrencyCode(),
                org.getTimeZone(),
                org.getWeeklyOffDays());
    }
}
