package com.server.server.middlewares.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.server.server.config.properties.AppProperties;
import com.server.server.middlewares.auth.SecurityErrorResponder;

class AuthRateLimitFilterTest {

    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-11-05T10:00:00Z"));
    private final Clock clock = new Clock() {
        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now.get();
        }
    };
    private final AuthRateLimitFilter filter = new AuthRateLimitFilter(new AppProperties.RateLimit(3, 60),
            new SecurityErrorResponder(new ObjectMapper().registerModule(new JavaTimeModule())), clock);

    @Test
    void blocksAttemptsOverTheLimitUntilTheWindowEnds() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertThat(post("/api/auth/login", "10.0.0.1").getStatus()).isEqualTo(200);
        }
        MockHttpServletResponse blocked = post("/api/auth/login", "10.0.0.1");
        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getHeader("Retry-After")).isEqualTo("60");
        assertThat(blocked.getContentAsString()).contains("TOO_MANY_REQUESTS");

        now.set(now.get().plusSeconds(61));
        assertThat(post("/api/auth/login", "10.0.0.1").getStatus()).isEqualTo(200);
    }

    @Test
    void countsEachAddressAndPathSeparately() throws Exception {
        for (int i = 0; i < 3; i++) {
            post("/api/auth/login", "10.0.0.1");
        }
        assertThat(post("/api/auth/login", "10.0.0.2").getStatus()).isEqualTo(200);
        assertThat(post("/api/auth/register", "10.0.0.1").getStatus()).isEqualTo(200);
    }

    @Test
    void ignoresOtherRequests() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertThat(post("/api/tours", "10.0.0.1").getStatus()).isEqualTo(200);
        }
    }

    private MockHttpServletResponse post(String path, String address) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr(address);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
