package com.cmbchina.datadirect.caliber.infrastructure.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.stream.Collectors;

@Component
public class RequestAuditFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestAuditFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startNs = System.nanoTime();
        String requestId = RequestTraceContext.ensureRequestId(request, response);
        MDC.put("traceId", requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            String api = request.getMethod() + " " + request.getRequestURI();
            String errorCode = resolveErrorCode(request);
            int status = response.getStatus();
            long latencyMs = Math.max(0, (System.nanoTime() - startNs) / 1_000_000);
            String userId = resolveUserId();
            String role = resolveRole();

            MDC.put("userId", userId);
            MDC.put("role", role);
            MDC.put("api", api);
            MDC.put("status", String.valueOf(status));
            MDC.put("errorCode", errorCode);

            log.info("request_audit latencyMs={}", latencyMs);

            MDC.remove("errorCode");
            MDC.remove("status");
            MDC.remove("api");
            MDC.remove("role");
            MDC.remove("userId");
            MDC.remove("traceId");
        }
    }

    private String resolveErrorCode(HttpServletRequest request) {
        Object value = request.getAttribute(RequestTraceContext.ATTR_ERROR_CODE);
        if (value instanceof String text && !text.isBlank()) {
            return text;
        }
        return "NONE";
    }

    private String resolveUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "anonymous";
        }
        String name = authentication.getName();
        return (name == null || name.isBlank()) ? "anonymous" : name.trim();
    }

    private String resolveRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "ANONYMOUS";
        }
        String value = authentication.getAuthorities().stream()
                .map(granted -> granted == null ? "" : granted.getAuthority())
                .filter(authority -> authority != null && !authority.isBlank())
                .collect(Collectors.joining("|"));
        return value.isBlank() ? "UNKNOWN" : value;
    }
}

