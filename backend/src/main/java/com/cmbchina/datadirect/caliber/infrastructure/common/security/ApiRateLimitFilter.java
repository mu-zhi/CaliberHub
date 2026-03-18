package com.cmbchina.datadirect.caliber.infrastructure.common.security;

import com.cmbchina.datadirect.caliber.adapter.web.ApiErrorDTO;
import com.cmbchina.datadirect.caliber.infrastructure.common.logging.RequestTraceContext;
import com.cmbchina.datadirect.caliber.infrastructure.common.config.CaliberSecurityProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ApiRateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = 60_000L;

    private final CaliberSecurityProperties caliberSecurityProperties;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final Map<String, RateCounter> counters;

    public ApiRateLimitFilter(CaliberSecurityProperties caliberSecurityProperties,
                              ObjectMapper objectMapper,
                              MeterRegistry meterRegistry) {
        this.caliberSecurityProperties = caliberSecurityProperties;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.counters = new ConcurrentHashMap<>();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!caliberSecurityProperties.isRateLimitEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        int limit = resolveLimit(request);
        if (limit <= 0) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = normalizePath(request);
        String key = request.getMethod() + "|" + resolveIdentity(request) + "|" + path;
        RateCounter counter = counters.computeIfAbsent(key, ignored -> new RateCounter(System.currentTimeMillis(), new AtomicInteger(0)));
        synchronized (counter) {
            long now = System.currentTimeMillis();
            if (now - counter.windowStartMs() >= WINDOW_MS) {
                counter.counter().set(0);
                counter.setWindowStartMs(now);
            }
            int value = counter.counter().incrementAndGet();
            if (value > limit) {
                response.setStatus(429);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                String requestId = RequestTraceContext.ensureRequestId(request, response);
                RequestTraceContext.markErrorCode(request, response, "RATE_LIMIT_EXCEEDED");
                Counter.builder("caliber.security.rate_limit.exceeded")
                        .tag("path", tagPath(path))
                        .tag("method", request.getMethod())
                        .register(meterRegistry)
                        .increment();
                ApiErrorDTO dto = new ApiErrorDTO("RATE_LIMIT_EXCEEDED", "请求过于频繁，请稍后再试", requestId, OffsetDateTime.now());
                response.getWriter().write(objectMapper.writeValueAsString(dto));
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private int resolveLimit(HttpServletRequest request) {
        String method = request.getMethod();
        String path = normalizePath(request);
        boolean writeApi = "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method);
        if (!writeApi) {
            return 0;
        }
        if (path.startsWith("/api/import/preprocess") || path.startsWith("/api/system/llm-preprocess-config")) {
            return Math.max(1, caliberSecurityProperties.getLlmPerMinute());
        }
        return Math.max(1, caliberSecurityProperties.getWritePerMinute());
    }

    private String normalizePath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri == null ? "/" : uri;
    }

    private String tagPath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        if (path.startsWith("/api/import/preprocess")) {
            return "/api/import/preprocess";
        }
        if (path.startsWith("/api/system/llm-preprocess-config")) {
            return "/api/system/llm-preprocess-config";
        }
        if (path.startsWith("/api/scenes")) {
            return "/api/scenes";
        }
        if (path.startsWith("/api/domains")) {
            return "/api/domains";
        }
        return path;
    }

    private String resolveIdentity(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "user:" + authentication.getName();
        }
        return "ip:" + request.getRemoteAddr();
    }

    private static final class RateCounter {
        private long windowStartMs;
        private final AtomicInteger counter;

        private RateCounter(long windowStartMs, AtomicInteger counter) {
            this.windowStartMs = windowStartMs;
            this.counter = counter;
        }

        public long windowStartMs() {
            return windowStartMs;
        }

        public void setWindowStartMs(long windowStartMs) {
            this.windowStartMs = windowStartMs;
        }

        public AtomicInteger counter() {
            return counter;
        }
    }
}
