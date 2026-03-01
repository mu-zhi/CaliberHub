package com.cmbchina.datadirect.caliber.infrastructure.common.config;

import com.cmbchina.datadirect.caliber.adapter.web.ApiErrorDTO;
import com.cmbchina.datadirect.caliber.infrastructure.common.logging.RequestAuditFilter;
import com.cmbchina.datadirect.caliber.infrastructure.common.logging.RequestTraceContext;
import com.cmbchina.datadirect.caliber.infrastructure.common.security.ApiRateLimitFilter;
import com.cmbchina.datadirect.caliber.infrastructure.common.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.OffsetDateTime;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final CaliberSecurityProperties caliberSecurityProperties;
    private final RequestAuditFilter requestAuditFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ApiRateLimitFilter apiRateLimitFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(CaliberSecurityProperties caliberSecurityProperties,
                          RequestAuditFilter requestAuditFilter,
                          JwtAuthenticationFilter jwtAuthenticationFilter,
                          ApiRateLimitFilter apiRateLimitFilter,
                          ObjectMapper objectMapper) {
        this.caliberSecurityProperties = caliberSecurityProperties;
        this.requestAuditFilter = requestAuditFilter;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.apiRateLimitFilter = apiRateLimitFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        if (!caliberSecurityProperties.isEnabled()) {
            http.csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(requests -> requests.anyRequest().permitAll());
            return http.build();
        }

        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, ex) -> writeError(request, response, 401, "UNAUTHORIZED", "需要登录后访问"))
                        .accessDeniedHandler((request, response, ex) -> writeError(request, response, 403, "FORBIDDEN", "当前角色无权限访问"))
                )
                .authorizeHttpRequests(requests -> {
                    requests.requestMatchers("/", "/index.html", "/favicon.svg", "/assets/**", "/business_categories.json").permitAll();
                    requests.requestMatchers("/actuator/health", "/actuator/info").permitAll();
                    requests.requestMatchers("/api/system/auth/token").permitAll();
                    requests.requestMatchers("/actuator/**").hasRole("ADMIN");
                    if (caliberSecurityProperties.isRequireWriteAuth()) {
                        requests.requestMatchers("/api/system/**").hasRole("ADMIN");
                        requests.requestMatchers(HttpMethod.POST, "/api/import/**").hasAnyRole("SUPPORT", "EXPERT", "GOVERNANCE", "ADMIN");
                        requests.requestMatchers(HttpMethod.POST, "/api/scenes/**").hasAnyRole("SUPPORT", "EXPERT", "GOVERNANCE", "ADMIN");
                        requests.requestMatchers(HttpMethod.PUT, "/api/scenes/**").hasAnyRole("SUPPORT", "EXPERT", "GOVERNANCE", "ADMIN");
                        requests.requestMatchers(HttpMethod.DELETE, "/api/scenes/**").hasAnyRole("SUPPORT", "EXPERT", "GOVERNANCE", "ADMIN");
                        requests.requestMatchers(HttpMethod.POST, "/api/domains/**").hasAnyRole("SUPPORT", "EXPERT", "GOVERNANCE", "ADMIN");
                        requests.requestMatchers(HttpMethod.PUT, "/api/domains/**").hasAnyRole("SUPPORT", "EXPERT", "GOVERNANCE", "ADMIN");
                        requests.requestMatchers(HttpMethod.POST, "/api/**").authenticated();
                        requests.requestMatchers(HttpMethod.PUT, "/api/**").authenticated();
                        requests.requestMatchers(HttpMethod.DELETE, "/api/**").authenticated();
                    } else {
                        requests.requestMatchers("/api/system/**").permitAll();
                        requests.requestMatchers(HttpMethod.POST, "/api/**").permitAll();
                        requests.requestMatchers(HttpMethod.PUT, "/api/**").permitAll();
                        requests.requestMatchers(HttpMethod.DELETE, "/api/**").permitAll();
                    }
                    requests.requestMatchers(HttpMethod.GET, "/api/**").permitAll();
                    requests.anyRequest().permitAll();
                })
                .addFilterBefore(requestAuditFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(jwtAuthenticationFilter, RequestAuditFilter.class)
                .addFilterAfter(apiRateLimitFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    private void writeError(HttpServletRequest request, HttpServletResponse response, int status, String code, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String requestId = RequestTraceContext.ensureRequestId(request, response);
        RequestTraceContext.markErrorCode(request, response, code);
        ApiErrorDTO dto = new ApiErrorDTO(code, message, requestId, OffsetDateTime.now());
        response.getWriter().write(objectMapper.writeValueAsString(dto));
    }
}
