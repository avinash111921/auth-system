package com.identity.timing;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
    Per-request time spent in each dependency, reported to the client
    as a Server-Timing header.
    A no-op unless {@link ServerTimingFilter} switched it on for the \
    current request.
 */
public final class ServerTiming {

    static final String ATTRIBUTE = ServerTiming.class.getName();

    private ServerTiming() {
    }

    /**
        The current request, captured on the caller's thread for callbacks
        that finish elsewhere.
     */
    public static RequestAttributes currentRequest() {
        return RequestContextHolder.getRequestAttributes();
    }

    public static void record(RequestAttributes request, String metric, long nanos) {
        Map<String, Totals> totals = totals(request);
        if (totals != null) {
            synchronized (totals) {
                totals.computeIfAbsent(metric, name -> new Totals()).add(nanos);
            }
        }
    }

    public static <T> T time(String metric, Supplier<T> work) {
        RequestAttributes request = currentRequest();
        if (totals(request) == null) {
            return work.get();
        }
        long start = System.nanoTime();
        try {
            return work.get();
        } finally {
            record(request, metric, System.nanoTime() - start);
        }
    }

    // Request-scope attributes live on the servlet request, so every RequestAttributes view of it sees this map.
    static Map<String, Totals> start(HttpServletRequest request) {
        Map<String, Totals> totals = new LinkedHashMap<>();
        request.setAttribute(ATTRIBUTE, totals);
        return totals;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Totals> totals(RequestAttributes request) {
        return request == null ? null : (Map<String, Totals>) request.getAttribute(ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
    }

    static final class Totals {
        long nanos;
        int calls;

        void add(long elapsed) {
            nanos += elapsed;
            calls++;
        }
    }
}
