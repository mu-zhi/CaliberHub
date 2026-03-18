package com.cmbchina.datadirect.caliber.infrastructure.common.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityOperator {

    private SecurityOperator() {
    }

    public static String currentOperator(String fallback) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "system";
        }
        String name = authentication.getName();
        if (name == null || name.isBlank()) {
            return "system";
        }
        return name.trim();
    }
}
