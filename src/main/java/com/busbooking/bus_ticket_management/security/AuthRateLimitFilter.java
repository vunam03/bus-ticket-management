package com.busbooking.bus_ticket_management.security;


import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

/** Bounded, single-instance rate limiting for the coursework deployment. */
public class AuthRateLimitFilter extends OncePerRequestFilter {
    private record Window(long startedAt, int count) {}
    private final Map<String, Window> windows = new HashMap<>();
    private final Clock clock;

    public AuthRateLimitFilter(Clock clock) {
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String route = request.getRequestURI().substring(request.getContextPath().length());
        if ("POST".equals(request.getMethod())
                && (route.startsWith("/auth/") || route.equals("/user/change-password"))) {
            int limit = route.equals("/auth/forgot-password") ? 5 : 20;
            if (!allow(request.getRemoteAddr() + ":" + route, limit)) {
                response.setStatus(429);
                response.setHeader("Retry-After", "60");
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("Bạn thao tác quá nhanh. Vui lòng thử lại sau một phút.");
                return;
            }
            if (route.equals("/auth/login")) {
                String password = request.getParameter("password");
                if (password != null && password.getBytes(StandardCharsets.UTF_8).length > 72) {
                    response.sendRedirect(request.getContextPath() + "/auth/login?error");
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }

    private synchronized boolean allow(String key, int limit) {
        long now = clock.millis();
        windows.entrySet().removeIf(entry -> now - entry.getValue().startedAt() >= 60_000);
        Window current = windows.get(key);
        if (current == null) {
            if (windows.size() >= 10_000) {
                return false;
            }
            windows.put(key, new Window(now, 1));
            return true;
        }
        if (current.count() >= limit) {
            return false;
        }
        windows.put(key, new Window(current.startedAt(), current.count() + 1));
        return true;
    }
}
