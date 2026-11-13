package com.identity.timing;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

/**
    Adds {@code Server-Timing: risk;dur=..., db;dur=..., app;dur=...}
    to every response, so a client can split its measured latency into
    time spent in each dependency.
    Off by default (auth.server-timing.enabled): it tells callers which
    dependencies a request touched, which is fine for a test environment
    and not something to publish in production.
 */

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ServerTimingFilter extends OncePerRequestFilter {

    private static final Map<String, String> DESCRIPTIONS = Map.of(
            "risk", "risk-service gRPC",
            "token", "token-service gRPC",
            "otp", "otp gRPC",
            "db", "PostgreSQL",
            "bcrypt", "password hashing",
            "app", "identity-service total");

    private final boolean enabled;

    public ServerTimingFilter(@Value("${auth.server-timing.enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // Buffer the body so the header can still be added once the handler has finished.
        var buffered = new ContentCachingResponseWrapper(response);
        Map<String, ServerTiming.Totals> totals = ServerTiming.start(request);
        long start = System.nanoTime();
        try {
            chain.doFilter(request, buffered);
        } finally {
            if (!buffered.isCommitted()) {
                buffered.setHeader("Server-Timing", header(totals, System.nanoTime() - start));
            }
            buffered.copyBodyToResponse();
        }
    }

    private static String header(Map<String, ServerTiming.Totals> totals, long appNanos) {
        StringJoiner header = new StringJoiner(", ");
        synchronized (totals) {
            totals.forEach((metric, t) -> header.add(entry(metric, t.nanos, t.calls)));
        }
        header.add(entry("app", appNanos, 1));
        return header.toString();
    }

    private static String entry(String metric, long nanos, int calls) {
        String description = DESCRIPTIONS.getOrDefault(metric, metric) + (calls > 1 ? " x" + calls : "");
        return String.format(Locale.ROOT, "%s;dur=%.1f;desc=\"%s\"", metric, nanos / 1_000_000.0, description);
    }
}
