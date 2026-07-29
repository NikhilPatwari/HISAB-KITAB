package com.hisabkitab.service;

import com.hisabkitab.domain.AppUser;
import com.hisabkitab.domain.Employee;
import com.hisabkitab.domain.EntryType;
import com.hisabkitab.domain.LedgerEntry;
import com.hisabkitab.exception.ApiExceptions;
import com.hisabkitab.repository.AppUserRepository;
import com.hisabkitab.repository.LedgerEntryRepository;
import com.hisabkitab.repository.OrganizationRepository;
import com.hisabkitab.security.AuthPrincipal;
import com.hisabkitab.web.dto.LedgerDtos.CreateEntryRequest;
import com.hisabkitab.web.dto.LedgerDtos.EntryView;
import com.hisabkitab.web.dto.LedgerDtos.PagedEntries;
import com.hisabkitab.web.dto.LedgerDtos.StatementResponse;
import com.hisabkitab.web.dto.LedgerDtos.StatementRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class LedgerService {

    private final LedgerEntryRepository ledger;
    private final OrganizationRepository organizations;
    private final AppUserRepository users;
    private final EmployeeService employeeService;
    private final EmployerService employerService;
    private final OrganizationService organizationService;
    private final WageAccrualService accrualService;

    public LedgerService(LedgerEntryRepository ledger,
                         OrganizationRepository organizations,
                         AppUserRepository users,
                         EmployeeService employeeService,
                         EmployerService employerService,
                         OrganizationService organizationService,
                         WageAccrualService accrualService) {
        this.accrualService = accrualService;
        this.ledger = ledger;
        this.organizations = organizations;
        this.users = users;
        this.employeeService = employeeService;
        this.employerService = employerService;
        this.organizationService = organizationService;
    }

    @Transactional
    public EntryView create(AuthPrincipal principal, CreateEntryRequest request) {
        Long orgId = principal.organizationId();
        Employee employee = employeeService.require(orgId, request.employeeId());

        if (request.entryType() == EntryType.WAGE) {
            // Wage accruals come from the monthly run so they stay reconcilable
            // with attendance. A one-off payment should be BONUS or ADJUSTMENT.
            throw new ApiExceptions.BadRequestException(
                    "Wages are posted by the monthly wage run. Use Bonus or Adjustment for a one-off amount.");
        }

        LocalDate today = organizationService.today(orgId);
        if (request.entryDate().isAfter(today)) {
            throw new ApiExceptions.BadRequestException("Date cannot be in the future");
        }
        if (request.entryDate().isBefore(employee.getJoinedOn())) {
            throw new ApiExceptions.BadRequestException(
                    "Date is before " + employee.getName() + " joined on " + employee.getJoinedOn());
        }

        LedgerEntry entry = new LedgerEntry();
        entry.setOrganization(organizations.getReferenceById(orgId));
        entry.setEmployee(employee);
        if (request.employerId() != null) {
            entry.setEmployer(employerService.require(orgId, request.employerId()));
        }
        entry.setEntryType(request.entryType());
        entry.setAmount(Money.scale(request.amount()));
        entry.setSignedAmount(signedAmount(request));
        entry.setEntryDate(request.entryDate());
        entry.setNote(blankToNull(request.note()));
        entry.setCreatedBy(users.getReferenceById(principal.userId()));

        return toView(ledger.save(entry));
    }

    /**
     * Entries are voided rather than deleted, so a statement already shown to an
     * employee can still be reproduced line for line.
     */
    @Transactional
    public EntryView voidEntry(AuthPrincipal principal, Long entryId) {
        LedgerEntry entry = ledger.findByIdAndOrganizationId(entryId, principal.organizationId())
                .orElseThrow(() -> ApiExceptions.NotFoundException.of("Entry", entryId));

        if (entry.isVoided()) {
            return toView(entry);
        }
        if (entry.getWageRun() != null) {
            throw new ApiExceptions.BadRequestException(
                    "This is a posted wage entry. Void the whole wage run instead.");
        }

        entry.setVoided(true);
        entry.setVoidedAt(Instant.now());
        entry.setVoidedBy(users.getReferenceById(principal.userId()));
        return toView(ledger.save(entry));
    }

    @Transactional(readOnly = true)
    public PagedEntries search(Long organizationId,
                               Long employeeId,
                               Long employerId,
                               EntryType entryType,
                               LocalDate from,
                               LocalDate to,
                               boolean includeVoided,
                               int page,
                               int size) {

        Page<LedgerEntry> result = ledger.search(organizationId, employeeId, employerId, entryType,
                from, to, includeVoided, PageRequest.of(page, Math.min(size, 200)));

        return new PagedEntries(
                result.getContent().stream().map(LedgerService::toView).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    /**
     * The employee's passbook: an opening balance, then every entry with the
     * balance after it. This is what gets read out loud at settlement time.
     */
    @Transactional(readOnly = true)
    public StatementResponse statement(Long organizationId,
                                       Long employeeId,
                                       LocalDate from,
                                       LocalDate to,
                                       boolean includeVoided) {

        Employee employee = employeeService.require(organizationId, employeeId);
        BigDecimal opening = Money.nullToZero(ledger.balanceForEmployeeBefore(employeeId, from));

        BigDecimal running = opening;
        BigDecimal givenOut = Money.ZERO;
        BigDecimal earned = Money.ZERO;
        List<StatementRow> rows = new ArrayList<>();

        for (LedgerEntry entry : ledger.findStatementRows(employeeId, from, to, includeVoided)) {
            if (!entry.isVoided()) {
                running = running.add(entry.getSignedAmount());
                if (entry.getSignedAmount().signum() < 0) {
                    givenOut = givenOut.add(entry.getAmount());
                } else {
                    earned = earned.add(entry.getAmount());
                }
            }
            rows.add(new StatementRow(
                    entry.getId(),
                    entry.getEntryDate(),
                    entry.getEntryType(),
                    entry.getEntryType().label(),
                    entry.getNote(),
                    entry.getEmployer() == null ? null : entry.getEmployer().getName(),
                    entry.getAmount(),
                    entry.getSignedAmount(),
                    // A voided row keeps the balance it did not change, so the
                    // column still reads top to bottom.
                    Money.scale(running),
                    entry.isVoided(),
                    entry.getWageRun() == null ? null : entry.getWageRun().getId()));
        }

        // Wages since the last closed month are not rows yet, so they are
        // reported alongside rather than folded into the running balance.
        BigDecimal unposted = accrualService.unpostedFor(organizationId, employeeId);
        LocalDate unpostedSince = accrualService.accrualStart(organizationId);

        return new StatementResponse(
                employee.getId(),
                employee.getName(),
                from,
                to,
                Money.scale(opening),
                Money.scale(running),
                Money.scale(givenOut),
                Money.scale(earned),
                unposted,
                unpostedSince != null ? unpostedSince : employee.getJoinedOn(),
                Money.scale(running.add(unposted)),
                rows);
    }

    private BigDecimal signedAmount(CreateEntryRequest request) {
        BigDecimal magnitude = Money.scale(request.amount());
        if (request.entryType() == EntryType.ADJUSTMENT) {
            if (request.creditsEmployee() == null) {
                throw new ApiExceptions.BadRequestException(
                        "An adjustment must say whether it credits or debits the employee");
            }
            return request.creditsEmployee() ? magnitude : magnitude.negate();
        }
        return request.entryType().sign() < 0 ? magnitude.negate() : magnitude;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    static EntryView toView(LedgerEntry entry) {
        return new EntryView(
                entry.getId(),
                entry.getEmployee().getId(),
                entry.getEmployee().getName(),
                entry.getEmployer() == null ? null : entry.getEmployer().getId(),
                entry.getEmployer() == null ? null : entry.getEmployer().getName(),
                entry.getEntryType(),
                entry.getEntryType().label(),
                entry.getAmount(),
                entry.getSignedAmount(),
                entry.getEntryDate(),
                entry.getNote(),
                entry.isVoided(),
                entry.getWageRun() == null ? null : entry.getWageRun().getId(),
                entry.getCreatedAt());
    }

    /** Used by the wage run to attribute generated entries to the posting user. */
    AppUser userRef(Long userId) {
        return users.getReferenceById(userId);
    }
}
