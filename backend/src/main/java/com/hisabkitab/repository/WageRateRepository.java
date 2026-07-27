package com.hisabkitab.repository;

import com.hisabkitab.domain.WageRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface WageRateRepository extends JpaRepository<WageRate, Long> {

    List<WageRate> findByEmployeeIdOrderByEffectiveFromDesc(Long employeeId);

    /**
     * Rates for a set of employees that could apply on or before the period end,
     * newest first, so a wage run can resolve each day's rate in memory.
     */
    @Query("""
            select r from WageRate r
            where r.employee.id in :employeeIds
              and r.effectiveFrom <= :periodEnd
            order by r.employee.id asc, r.effectiveFrom desc
            """)
    List<WageRate> findApplicableRates(@Param("employeeIds") List<Long> employeeIds,
                                       @Param("periodEnd") LocalDate periodEnd);
}
