package com.hisabkitab.repository;

import com.hisabkitab.domain.EntryType;
import com.hisabkitab.domain.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    Optional<LedgerEntry> findByIdAndOrganizationId(Long id, Long organizationId);

    /** Balance for one employee. Positive: farm owes them. Negative: they owe the farm. */
    @Query("""
            select coalesce(sum(l.signedAmount), 0)
            from LedgerEntry l
            where l.employee.id = :employeeId and l.voided = false
            """)
    BigDecimal balanceForEmployee(@Param("employeeId") Long employeeId);

    /** Balance as of a date, used for the opening balance of a statement. */
    @Query("""
            select coalesce(sum(l.signedAmount), 0)
            from LedgerEntry l
            where l.employee.id = :employeeId
              and l.voided = false
              and l.entryDate < :date
            """)
    BigDecimal balanceForEmployeeBefore(@Param("employeeId") Long employeeId,
                                        @Param("date") LocalDate date);

    /** One row per employee that has any ledger activity. */
    @Query("""
            select l.employee.id as employeeId, coalesce(sum(l.signedAmount), 0) as balance
            from LedgerEntry l
            where l.organization.id = :orgId and l.voided = false
            group by l.employee.id
            """)
    List<EmployeeBalanceRow> balancesByEmployee(@Param("orgId") Long organizationId);

    @Query("""
            select l from LedgerEntry l
            left join fetch l.employer
            where l.employee.id = :employeeId
              and l.entryDate between :from and :to
              and (:includeVoided = true or l.voided = false)
            order by l.entryDate asc, l.id asc
            """)
    List<LedgerEntry> findStatementRows(@Param("employeeId") Long employeeId,
                                        @Param("from") LocalDate from,
                                        @Param("to") LocalDate to,
                                        @Param("includeVoided") boolean includeVoided);

    @Query(value = """
            select l from LedgerEntry l
            join fetch l.employee
            left join fetch l.employer
            where l.organization.id = :orgId
              and (:employeeId is null or l.employee.id = :employeeId)
              and (:employerId is null or l.employer.id = :employerId)
              and (:entryType is null or l.entryType = :entryType)
              and l.entryDate between :from and :to
              and (:includeVoided = true or l.voided = false)
            order by l.entryDate desc, l.id desc
            """,
            countQuery = """
            select count(l) from LedgerEntry l
            where l.organization.id = :orgId
              and (:employeeId is null or l.employee.id = :employeeId)
              and (:employerId is null or l.employer.id = :employerId)
              and (:entryType is null or l.entryType = :entryType)
              and l.entryDate between :from and :to
              and (:includeVoided = true or l.voided = false)
            """)
    Page<LedgerEntry> search(@Param("orgId") Long organizationId,
                             @Param("employeeId") Long employeeId,
                             @Param("employerId") Long employerId,
                             @Param("entryType") EntryType entryType,
                             @Param("from") LocalDate from,
                             @Param("to") LocalDate to,
                             @Param("includeVoided") boolean includeVoided,
                             Pageable pageable);

    /** Totals per entry type across a date window, for the dashboard. */
    @Query("""
            select l.entryType as type, coalesce(sum(l.amount), 0) as total
            from LedgerEntry l
            where l.organization.id = :orgId
              and l.voided = false
              and l.entryDate between :from and :to
            group by l.entryType
            """)
    List<TypeTotalRow> totalsByType(@Param("orgId") Long organizationId,
                                    @Param("from") LocalDate from,
                                    @Param("to") LocalDate to);

    /** Net outstanding funded by each employer. */
    @Query("""
            select l.employer.id as employerId, coalesce(sum(l.signedAmount), 0) as balance
            from LedgerEntry l
            where l.organization.id = :orgId
              and l.voided = false
              and l.employer is not null
            group by l.employer.id
            """)
    List<EmployerBalanceRow> balancesByEmployer(@Param("orgId") Long organizationId);

    List<LedgerEntry> findByWageRunIdAndVoidedFalse(Long wageRunId);

    interface TypeTotalRow {
        EntryType getType();

        BigDecimal getTotal();
    }

    interface EmployerBalanceRow {
        Long getEmployerId();

        BigDecimal getBalance();
    }
}
