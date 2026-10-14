package com.smartmove.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class SessionCsrfFilter extends OncePerRequestFilter {
    public static final String TOKEN_KEY = "SMARTMOVE_CSRF";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String method = request.getMethod();
        String path = request.getServletPath();
        boolean safe = method.equals("GET") || method.equals("HEAD") || method.equals("OPTIONS");
        boolean publicAuth = path.equals("/api/auth/login") || path.equals("/api/auth/register");
        if (!safe && !publicAuth) {
            HttpSession session = request.getSession(false);
            String expected = session == null ? null : (String) session.getAttribute(TOKEN_KEY);
            String actual = request.getHeader("X-CSRF-Token");
            if (expected == null || actual == null ||
                !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                                       actual.getBytes(StandardCharsets.UTF_8))) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
