package com.server.server.middlewares.security;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.server.server.config.properties.AppProperties;
import com.server.server.constants.errors.ErrorCodes;
import com.server.server.middlewares.auth.SecurityErrorResponder;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Limits sign-in, registration and password-reset attempts per client address: at most
 * {@code app.rate-limit.auth-requests} POSTs to {@code <api-prefix>/auth/**} in each window. Excess requests get 429.
 * Counts are kept in memory, so each server instance limits on its own.
 */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final int CLEANUP_THRESHOLD = 10_000;

    private final String authPath;
    private final AppProperties.RateLimit limits;
    private final SecurityErrorResponder errorResponder;
    private final Clock clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Autowired
    public AuthRateLimitFilter(AppProperties appProperties, SecurityErrorResponder errorResponder) {
        this(appProperties.apiPrefix(), appProperties.rateLimit(), errorResponder, Clock.systemUTC());
    }

    AuthRateLimitFilter(String apiPrefix, AppProperties.RateLimit limits, SecurityErrorResponder errorResponder,
            Clock clock) {
        this.authPath = apiPrefix + "/auth/";
        this.limits = limits;
        this.errorResponder = errorResponder;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !request.getRequestURI().startsWith(authPath);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain chain) throws ServletException, IOException {
        long now = clock.millis();
        long windowMillis = limits.windowSeconds() * 1000L;
        if (windows.size() > CLEANUP_THRESHOLD) {
            windows.values().removeIf(window -> window.isExpired(now));
        }
        Window window = windows.compute(request.getRemoteAddr() + " " + request.getRequestURI(),
                (key, current) -> current == null || current.isExpired(now) ? new Window(now + windowMillis, 1)
                        : new Window(current.resetsAt(), current.count() + 1));
        if (window.count() > limits.authRequests()) {
            long retryAfter = Math.max(1, (window.resetsAt() - now + 999) / 1000);
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter));
            errorResponder.write(request, response, HttpStatus.TOO_MANY_REQUESTS, ErrorCodes.TOO_MANY_REQUESTS,
                    "Too many attempts. Please wait " + retryAfter + " seconds and try again.");
            return;
        }
        chain.doFilter(request, response);
    }

    private record Window(long resetsAt, int count) {
        boolean isExpired(long now) {
            return now >= resetsAt;
        }
    }
}
