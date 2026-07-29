package com.hisabkitab.repository;

import com.hisabkitab.domain.WageRun;
import com.hisabkitab.domain.WageRunStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WageRunRepository extends JpaRepository<WageRun, Long> {

    List<WageRun> findByOrganizationIdOrderByPeriodStartDesc(Long organizationId);

    Optional<WageRun> findByIdAndOrganizationId(Long id, Long organizationId);

    Optional<WageRun> findByOrganizationIdAndPeriodStartAndStatus(
            Long organizationId, LocalDate periodStart, WageRunStatus status);

    /** The most recently closed period, which is where daily accrual picks up. */
    Optional<WageRun> findTopByOrganizationIdAndStatusOrderByPeriodEndDesc(
            Long organizationId, WageRunStatus status);

    boolean existsByOrganizationIdAndPeriodStartAndStatus(
            Long organizationId, LocalDate periodStart, WageRunStatus status);
}
