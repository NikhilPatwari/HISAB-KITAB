package com.hisabkitab.repository;

import com.hisabkitab.domain.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByEmployeeIdAndWorkDate(Long employeeId, LocalDate workDate);

    List<Attendance> findByEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(
            Long employeeId, LocalDate from, LocalDate to);

    @Query("""
            select a from Attendance a
            join fetch a.employee
            where a.organization.id = :orgId
              and a.workDate between :from and :to
            order by a.workDate asc, a.employee.name asc
            """)
    List<Attendance> findForOrganizationBetween(@Param("orgId") Long organizationId,
                                                @Param("from") LocalDate from,
                                                @Param("to") LocalDate to);

    @Query("""
            select a from Attendance a
            where a.employee.id in :employeeIds
              and a.workDate between :from and :to
            """)
    List<Attendance> findForEmployeesBetween(@Param("employeeIds") List<Long> employeeIds,
                                             @Param("from") LocalDate from,
                                             @Param("to") LocalDate to);

    void deleteByEmployeeIdAndWorkDate(Long employeeId, LocalDate workDate);
}
