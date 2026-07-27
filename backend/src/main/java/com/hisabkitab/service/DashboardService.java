package com.hisabkitab.service;

import com.hisabkitab.domain.Employee;
import com.hisabkitab.domain.EmployeeStatus;
import com.hisabkitab.domain.EntryType;
import com.hisabkitab.repository.EmployeeRepository;
import com.hisabkitab.repository.EmployerRepository;
import com.hisabkitab.repository.LedgerEntryRepository;
import com.hisabkitab.web.dto.DashboardDtos.Dashboard;
import com.hisabkitab.web.dto.DashboardDtos.EmployerPosition;
import com.hisabkitab.web.dto.DashboardDtos.TopDebtor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final EmployeeRepository employees;
    private final EmployerRepository employers;
    private final LedgerEntryRepository ledger;
    private final EmployeeService employeeService;
    private final OrganizationService organizationService;

    public DashboardService(EmployeeRepository employees,
                            EmployerRepository employers,
                            LedgerEntryRepository ledger,
                            EmployeeService employeeService,
                            OrganizationService organizationService) {
        this.employees = employees;
        this.employers = employers;
        this.ledger = ledger;
        this.employeeService = employeeService;
        this.organizationService = organizationService;
    }

    @Transactional(readOnly = true)
    public Dashboard build(Long organizationId) {
        LocalDate today = organizationService.today(organizationId);
        LocalDate monthStart = today.withDayOfMonth(1);

        Map<Long, BigDecimal> balances = employeeService.balanceMap(organizationId);
        List<Employee> staff = employees.findByOrganizationIdOrderByNameAsc(organizationId);

        BigDecimal receivable = Money.ZERO;
        BigDecimal payable = Money.ZERO;
        int inDebt = 0;
        int inCredit = 0;

        for (Employee employee : staff) {
            BigDecimal balance = balances.getOrDefault(employee.getId(), Money.ZERO);
            int sign = balance.signum();
            if (sign < 0) {
                receivable = receivable.add(balance.negate());
                inDebt++;
            } else if (sign > 0) {
                payable = payable.add(balance);
                inCredit++;
            }
        }

        Map<EntryType, BigDecimal> monthTotals = new EnumMap<>(EntryType.class);
        ledger.totalsByType(organizationId, monthStart, today)
                .forEach(row -> monthTotals.put(row.getType(), Money.nullToZero(row.getTotal())));

        Map<Long, String> employerNames = employers.findByOrganizationIdOrderByNameAsc(organizationId)
                .stream()
                .collect(java.util.stream.Collectors.toMap(e -> e.getId(), e -> e.getName()));

        List<EmployerPosition> employerPositions = ledger.balancesByEmployer(organizationId).stream()
                .map(row -> new EmployerPosition(
                        row.getEmployerId(),
                        employerNames.getOrDefault(row.getEmployerId(), "Unknown"),
                        Money.nullToZero(row.getBalance()).negate()))
                .sorted(Comparator.comparing(EmployerPosition::outstanding).reversed())
                .toList();

        List<TopDebtor> topDebtors = staff.stream()
                .map(e -> new TopDebtor(e.getId(), e.getName(),
                        balances.getOrDefault(e.getId(), Money.ZERO).negate()))
                .filter(d -> d.owed().signum() > 0)
                .sorted(Comparator.comparing(TopDebtor::owed).reversed())
                .limit(5)
                .toList();

        return new Dashboard(
                (int) employees.countByOrganizationIdAndStatus(organizationId, EmployeeStatus.ACTIVE),
                Money.scale(receivable),
                Money.scale(payable),
                Money.scale(receivable.subtract(payable)),
                inDebt,
                inCredit,
                monthStart,
                monthTotals.getOrDefault(EntryType.ADVANCE, Money.ZERO),
                monthTotals.getOrDefault(EntryType.WAGE, Money.ZERO),
                monthTotals.getOrDefault(EntryType.REPAYMENT, Money.ZERO),
                employerPositions,
                topDebtors);
    }
}
