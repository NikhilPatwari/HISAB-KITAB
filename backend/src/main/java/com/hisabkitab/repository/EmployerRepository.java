package com.hisabkitab.repository;

import com.hisabkitab.domain.Employer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployerRepository extends JpaRepository<Employer, Long> {

    List<Employer> findByOrganizationIdOrderByNameAsc(Long organizationId);

    Optional<Employer> findByIdAndOrganizationId(Long id, Long organizationId);

    long countByOrganizationId(Long organizationId);
}
