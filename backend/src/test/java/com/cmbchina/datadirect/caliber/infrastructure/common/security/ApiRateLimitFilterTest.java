package com.cmbchina.datadirect.caliber.infrastructure.common.security;

import com.cmbchina.datadirect.caliber.infrastructure.common.config.CaliberSecurityProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ApiRateLimitFilterTest {

    @Test
    void shouldRateLimitRepeatedGetRequests() throws Exception {
        ApiRateLimitFilter filter = createFilter(2, 1);

        MockHttpServletResponse firstResponse = perform(filter, "GET", "/api/scenes", "127.0.0.1");
        MockHttpServletResponse secondResponse = perform(filter, "GET", "/api/scenes", "127.0.0.1");
        MockHttpServletResponse thirdResponse = perform(filter, "GET", "/api/scenes", "127.0.0.1");

        assertThat(firstResponse.getStatus()).isEqualTo(200);
        assertThat(secondResponse.getStatus()).isEqualTo(200);
        assertThat(thirdResponse.getStatus()).isEqualTo(429);
        assertThat(thirdResponse.getContentAsString()).contains("RATE_LIMIT_EXCEEDED");
    }

    @Test
    void shouldCleanupExpiredCountersBeforeTrackingNewRequests() throws Exception {
        ApiRateLimitFilter filter = createFilter(2, 1);

        perform(filter, "GET", "/api/obsolete", "127.0.0.1");
        Map<String, ?> counters = extractCounters(filter);
        Map.Entry<String, ?> staleEntry = counters.entrySet().iterator().next();
        Field windowStartField = staleEntry.getValue().getClass().getDeclaredField("windowStartMs");
        windowStartField.setAccessible(true);
        windowStartField.setLong(staleEntry.getValue(), System.currentTimeMillis() - 120_000L);

        perform(filter, "GET", "/api/scenes", "127.0.0.1");

        assertThat(extractCounters(filter)).doesNotContainKey(staleEntry.getKey());
    }

    private ApiRateLimitFilter createFilter(int writePerMinute, int llmPerMinute) {
        CaliberSecurityProperties properties = new CaliberSecurityProperties();
        properties.setRateLimitEnabled(true);
        properties.setWritePerMinute(writePerMinute);
        properties.setLlmPerMinute(llmPerMinute);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return new ApiRateLimitFilter(properties, objectMapper, new SimpleMeterRegistry());
    }

    private MockHttpServletResponse perform(ApiRateLimitFilter filter, String method, String path, String remoteAddr) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr(remoteAddr);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    @SuppressWarnings("unchecked")
    private Map<String, ?> extractCounters(ApiRateLimitFilter filter) throws Exception {
        Field countersField = ApiRateLimitFilter.class.getDeclaredField("counters");
        countersField.setAccessible(true);
        return (Map<String, ?>) countersField.get(filter);
    }
}
