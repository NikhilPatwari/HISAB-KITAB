package com.hisabkitab.service;

import com.hisabkitab.domain.Organization;
import com.hisabkitab.security.AuthPrincipal;
import com.hisabkitab.security.JwtService;
import com.hisabkitab.web.dto.AuthDtos.LoginRequest;
import com.hisabkitab.web.dto.AuthDtos.LoginResponse;
import com.hisabkitab.web.dto.AuthDtos.Me;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final OrganizationService organizationService;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       OrganizationService organizationService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.organizationService = organizationService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username().trim(), request.password()));

        AuthPrincipal principal = (AuthPrincipal) auth.getPrincipal();
        return new LoginResponse(
                jwtService.issue(principal),
                jwtService.expirySeconds(),
                me(principal));
    }

    @Transactional(readOnly = true)
    public Me me(AuthPrincipal principal) {
        Organization org = organizationService.require(principal.organizationId());
        return new Me(
                principal.userId(),
                principal.username(),
                principal.displayName(),
                principal.role().name(),
                principal.employerId(),
                org.getId(),
                org.getName(),
                org.getCurrencyCode());
    }
}
