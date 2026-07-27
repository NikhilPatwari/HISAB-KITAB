package com.hisabkitab.repository;

import com.hisabkitab.domain.Employee;
import com.hisabkitab.domain.EmployeeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    List<Employee> findByOrganizationIdOrderByNameAsc(Long organizationId);

    List<Employee> findByOrganizationIdAndStatusOrderByNameAsc(Long organizationId, EmployeeStatus status);

    Optional<Employee> findByIdAndOrganizationId(Long id, Long organizationId);

    boolean existsByOrganizationIdAndCode(Long organizationId, String code);

    long countByOrganizationIdAndStatus(Long organizationId, EmployeeStatus status);

    /** Everyone who was on the books for at least part of the period. */
    @Query("""
            select e from Employee e
            where e.organization.id = :orgId
              and e.joinedOn <= :periodEnd
              and (e.exitedOn is null or e.exitedOn >= :periodStart)
            order by e.name asc
            """)
    List<Employee> findEmployedDuring(@Param("orgId") Long organizationId,
                                      @Param("periodStart") LocalDate periodStart,
                                      @Param("periodEnd") LocalDate periodEnd);
}
