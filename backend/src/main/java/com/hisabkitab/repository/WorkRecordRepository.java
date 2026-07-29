package com.hisabkitab.repository;

import com.hisabkitab.domain.WorkRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WorkRecordRepository extends JpaRepository<WorkRecord, Long> {

    Optional<WorkRecord> findByIdAndOrganizationId(Long id, Long organizationId);

    /** At most one row per worker per task per day — recording overwrites. */
    Optional<WorkRecord> findByEmployeeIdAndWorkTaskIdAndWorkDate(
            Long employeeId, Long workTaskId, LocalDate workDate);

    @Query("""
            select r from WorkRecord r
            join fetch r.workTask
            join fetch r.employee
            where r.organization.id = :orgId
              and r.workDate between :from and :to
            order by r.workDate desc, r.id desc
            """)
    List<WorkRecord> findForOrganizationBetween(@Param("orgId") Long organizationId,
                                                @Param("from") LocalDate from,
                                                @Param("to") LocalDate to);

    @Query("""
            select r from WorkRecord r
            join fetch r.workTask
            where r.employee.id = :employeeId
              and r.workDate between :from and :to
            order by r.workDate desc, r.id desc
            """)
    List<WorkRecord> findForEmployeeBetween(@Param("employeeId") Long employeeId,
                                            @Param("from") LocalDate from,
                                            @Param("to") LocalDate to);

    /** Piece-work earnings per employee over a window, for accrual and posting. */
    @Query("""
            select r.employee.id as employeeId, coalesce(sum(r.amount), 0) as total
            from WorkRecord r
            where r.organization.id = :orgId
              and r.workDate between :from and :to
            group by r.employee.id
            """)
    List<EmployeeAmountRow> totalsByEmployee(@Param("orgId") Long organizationId,
                                             @Param("from") LocalDate from,
                                             @Param("to") LocalDate to);

    /** Units and money for one employee on one task, for the statement note. */
    @Query("""
            select coalesce(sum(r.quantity), 0)
            from WorkRecord r
            where r.employee.id = :employeeId
              and r.workDate between :from and :to
            """)
    BigDecimal totalQuantityForEmployee(@Param("employeeId") Long employeeId,
                                        @Param("from") LocalDate from,
                                        @Param("to") LocalDate to);

    /** Everything logged against a task, filtered and newest first. */
    @Query("""
            select r from WorkRecord r
            join fetch r.workTask
            join fetch r.employee
            where r.organization.id = :orgId
              and (:employeeId is null or r.employee.id = :employeeId)
              and (:taskId is null or r.workTask.id = :taskId)
              and r.workDate between :from and :to
            order by r.workDate desc, r.id desc
            """)
    List<WorkRecord> findFiltered(@Param("orgId") Long organizationId,
                                  @Param("employeeId") Long employeeId,
                                  @Param("taskId") Long taskId,
                                  @Param("from") LocalDate from,
                                  @Param("to") LocalDate to);

    /** Per-worker running totals on one task, most recently active first. */
    @Query("""
            select r.employee.id as employeeId,
                   r.employee.name as employeeName,
                   coalesce(sum(r.quantity), 0) as quantity,
                   coalesce(sum(r.amount), 0) as amount,
                   count(r) as entries,
                   max(r.workDate) as lastWorkedOn
            from WorkRecord r
            where r.workTask.id = :taskId
            group by r.employee.id, r.employee.name
            order by max(r.workDate) desc
            """)
    List<TaskWorkerRow> totalsByEmployeeForTask(@Param("taskId") Long taskId);

    long countByWorkTaskId(Long workTaskId);

    interface TaskWorkerRow {
        Long getEmployeeId();

        String getEmployeeName();

        BigDecimal getQuantity();

        BigDecimal getAmount();

        long getEntries();

        LocalDate getLastWorkedOn();
    }

    interface EmployeeAmountRow {
        Long getEmployeeId();

        BigDecimal getTotal();
    }
}
