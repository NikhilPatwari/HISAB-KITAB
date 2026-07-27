package com.hisabkitab.service;

import com.hisabkitab.domain.Employer;
import com.hisabkitab.exception.ApiExceptions;
import com.hisabkitab.repository.EmployerRepository;
import com.hisabkitab.repository.LedgerEntryRepository;
import com.hisabkitab.repository.OrganizationRepository;
import com.hisabkitab.web.dto.EmployerDtos.EmployerRequest;
import com.hisabkitab.web.dto.EmployerDtos.EmployerView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmployerService {

    private final EmployerRepository employers;
    private final OrganizationRepository organizations;
    private final LedgerEntryRepository ledger;

    public EmployerService(EmployerRepository employers,
                           OrganizationRepository organizations,
                           LedgerEntryRepository ledger) {
        this.employers = employers;
        this.organizations = organizations;
        this.ledger = ledger;
    }

    @Transactional(readOnly = true)
    public List<EmployerView> list(Long organizationId) {
        Map<Long, BigDecimal> balances = new HashMap<>();
        ledger.balancesByEmployer(organizationId)
                .forEach(row -> balances.put(row.getEmployerId(), row.getBalance()));

        return employers.findByOrganizationIdOrderByNameAsc(organizationId).stream()
                .map(e -> toView(e, balances.get(e.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public Employer require(Long organizationId, Long employerId) {
        return employers.findByIdAndOrganizationId(employerId, organizationId)
                .orElseThrow(() -> ApiExceptions.NotFoundException.of("Employer", employerId));
    }

    @Transactional
    public EmployerView create(Long organizationId, EmployerRequest request) {
        Employer employer = new Employer();
        employer.setOrganization(organizations.getReferenceById(organizationId));
        apply(employer, request);
        return toView(employers.save(employer), BigDecimal.ZERO);
    }

    @Transactional
    public EmployerView update(Long organizationId, Long employerId, EmployerRequest request) {
        Employer employer = require(organizationId, employerId);
        apply(employer, request);
        BigDecimal balance = ledger.balancesByEmployer(organizationId).stream()
                .filter(row -> row.getEmployerId().equals(employerId))
                .map(LedgerEntryRepository.EmployerBalanceRow::getBalance)
                .findFirst()
                .orElse(BigDecimal.ZERO);
        return toView(employers.save(employer), balance);
    }

    /** Employers are deactivated rather than deleted so historical entries keep their owner. */
    @Transactional
    public void deactivate(Long organizationId, Long employerId) {
        Employer employer = require(organizationId, employerId);
        employer.setActive(false);
        employers.save(employer);
    }

    private void apply(Employer employer, EmployerRequest request) {
        employer.setName(request.name().trim());
        employer.setPhone(blankToNull(request.phone()));
        employer.setNotes(blankToNull(request.notes()));
        if (request.active() != null) {
            employer.setActive(request.active());
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static EmployerView toView(Employer employer, BigDecimal signedBalance) {
        // Ledger balances are signed from the employee's side, so money the
        // employer is still owed shows up as a negative sum.
        BigDecimal outstanding = Money.nullToZero(signedBalance).negate();
        return new EmployerView(
                employer.getId(),
                employer.getName(),
                employer.getPhone(),
                employer.getNotes(),
                employer.isActive(),
                outstanding);
    }
}
