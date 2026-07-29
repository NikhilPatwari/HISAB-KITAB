package com.hisabkitab.repository;

import com.hisabkitab.domain.WorkTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkTaskRepository extends JpaRepository<WorkTask, Long> {

    List<WorkTask> findByOrganizationIdOrderByNameAsc(Long organizationId);

    List<WorkTask> findByOrganizationIdAndActiveOrderByNameAsc(Long organizationId, boolean active);

    Optional<WorkTask> findByIdAndOrganizationId(Long id, Long organizationId);
}
