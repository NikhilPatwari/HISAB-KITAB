package com.hisabkitab.security;

import com.hisabkitab.domain.AppUser;
import com.hisabkitab.domain.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * The authenticated user. Carries the organization id so that every query can be
 * scoped without a second lookup — no request may ever read another farm's data.
 */
public record AuthPrincipal(
        Long userId,
        Long organizationId,
        Long employerId,
        String username,
        String displayName,
        Role role,
        String passwordHash,
        boolean enabled
) implements UserDetails {

    public static AuthPrincipal from(AppUser user) {
        return new AuthPrincipal(
                user.getId(),
                user.getOrganization().getId(),
                user.getEmployer() == null ? null : user.getEmployer().getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole(),
                user.getPasswordHash(),
                user.isEnabled()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
