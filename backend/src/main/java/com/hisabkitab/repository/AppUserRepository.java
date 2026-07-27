package com.hisabkitab.repository;

import com.hisabkitab.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    @Query("select u from AppUser u join fetch u.organization left join fetch u.employer where u.username = :username")
    Optional<AppUser> findByUsernameWithOrganization(String username);

    boolean existsByUsername(String username);
}
