package com.malvinas.gateway.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fixed-window rate limiter by IP. In-memory, single-instance.
 * ponytail: global lock per IP window; bucket4j + Redis if multi-instance or stricter limits needed.
 */
@Component
@Order(0) // before JwtAuthFilter (@Order(1))
public class RateLimitFilter implements Filter {

    @Value("${ratelimit.requests-per-minute:120}")
    private int requestsPerMinute;

    private static final long WINDOW_MS = 60_000L;

    private record Window(long start, int count) {}

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest  request  = (HttpServletRequest)  req;
        HttpServletResponse response = (HttpServletResponse) res;

        // Exempt health/actuator paths
        String path = request.getRequestURI();
        if (path.startsWith("/actuator")) {
            chain.doFilter(req, res);
            return;
        }

        String ip = getClientIp(request);
        long now  = System.currentTimeMillis();

        Window current = windows.merge(ip, new Window(now, 1), (existing, fresh) -> {
            if (now - existing.start() >= WINDOW_MS) return fresh; // new window
            return new Window(existing.start(), existing.count() + 1);
        });

        // Evict stale windows periodically (1 in ~100 requests)
        if (Math.random() < 0.01) evictStale(now);

        if (current.count() > requestsPerMinute) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too Many Requests\",\"retryAfter\":60}");
            return;
        }

        chain.doFilter(req, res);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void evictStale(long now) {
        Iterator<Map.Entry<String, Window>> it = windows.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue().start() >= WINDOW_MS) it.remove();
        }
    }
}
