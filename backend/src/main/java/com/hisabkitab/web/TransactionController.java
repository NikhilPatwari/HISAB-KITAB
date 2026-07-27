package com.hisabkitab.web;

import com.hisabkitab.domain.EntryType;
import com.hisabkitab.security.AuthPrincipal;
import com.hisabkitab.service.LedgerService;
import com.hisabkitab.service.OrganizationService;
import com.hisabkitab.web.dto.LedgerDtos.CreateEntryRequest;
import com.hisabkitab.web.dto.LedgerDtos.EntryView;
import com.hisabkitab.web.dto.LedgerDtos.PagedEntries;
import com.hisabkitab.web.dto.LedgerDtos.StatementResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Arrays;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final LedgerService ledgerService;
    private final OrganizationService organizationService;

    public TransactionController(LedgerService ledgerService, OrganizationService organizationService) {
        this.ledgerService = ledgerService;
        this.organizationService = organizationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EntryView create(@AuthenticationPrincipal AuthPrincipal principal,
                            @Valid @RequestBody CreateEntryRequest request) {
        return ledgerService.create(principal, request);
    }

    @GetMapping
    public PagedEntries list(@AuthenticationPrincipal AuthPrincipal principal,
                             @RequestParam(required = false) Long employeeId,
                             @RequestParam(required = false) Long employerId,
                             @RequestParam(required = false) EntryType entryType,
                             @RequestParam(required = false)
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                             @RequestParam(required = false)
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                             @RequestParam(defaultValue = "false") boolean includeVoided,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "50") int size) {

        LocalDate today = organizationService.today(principal.organizationId());
        return ledgerService.search(
                principal.organizationId(),
                employeeId,
                employerId,
                entryType,
                from != null ? from : today.minusYears(5),
                to != null ? to : today,
                includeVoided,
                page,
                size);
    }

    /** Voids an entry. Nothing is ever deleted from the ledger. */
    @PostMapping("/{id}/void")
    public EntryView voidEntry(@AuthenticationPrincipal AuthPrincipal principal,
                               @PathVariable Long id) {
        return ledgerService.voidEntry(principal, id);
    }

    /** The employee's passbook for a window, with a running balance on every line. */
    @GetMapping("/statement/{employeeId}")
    public StatementResponse statement(@AuthenticationPrincipal AuthPrincipal principal,
                                       @PathVariable Long employeeId,
                                       @RequestParam(required = false)
                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                       @RequestParam(required = false)
                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                       @RequestParam(defaultValue = "false") boolean includeVoided) {

        LocalDate today = organizationService.today(principal.organizationId());
        return ledgerService.statement(
                principal.organizationId(),
                employeeId,
                from != null ? from : today.minusYears(5),
                to != null ? to : today,
                includeVoided);
    }

    /** The entry types the UI offers, so the picker never drifts from the backend. */
    @GetMapping("/types")
    public Object types() {
        return Arrays.stream(EntryType.values())
                .map(type -> java.util.Map.of(
                        "value", type.name(),
                        "label", type.label(),
                        "debit", type.isDebit(),
                        "signed", type.sign()))
                .toList();
    }
}
