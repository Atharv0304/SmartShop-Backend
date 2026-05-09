package com.smartstore.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    private final List<String> PUBLIC_URLS = List.of(
        // Auth endpoints (no token needed)
        "/api/shopkeeper/login",
        "/api/shopkeeper/register",
        "/api/shopkeeper/send-otp",
        "/api/shopkeeper/verify-otp",
        "/api/shopkeeper/send-delete-otp",
        "/api/customer/login",
        "/api/customer/register",
        "/api/delivery/login",
        "/api/delivery/register",
        "/api/delivery/send-otp",
        "/api/delivery/verify-otp",
        "/api/admin/login",
        // All other API endpoints — protected by app-level logic, not JWT filter
        "/api/orders",
        "/api/products",
        "/api/shops",
        "/api/notifications",
        "/api/payment",
        "/api/customer/profile",
        "/api/shopkeeper/profile",
        "/api/shopkeeper/analytics",
        "/api/delivery/all",
        "/api/delivery/approve",
        "/api/delivery/reject",
        "/api/delivery/availability",
        "/api/delivery/profile",
        "/api/chat",
        "/api/disputes"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Skip filter for public URLs
        for (String url : PUBLIC_URLS) {
            if (path.startsWith(url)) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        // Validate JWT token
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtil.validateToken(token)) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        // Reject unauthorized requests
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"Unauthorized\"}");  // ← proper JSON response
    }
}