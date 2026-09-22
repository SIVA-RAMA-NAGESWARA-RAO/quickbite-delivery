package com.quickbite.gateway.security;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

/**
 * Decides which incoming requests are allowed through without a JWT.
 * Everything else must present a valid Authorization header.
 */
@Component
public class RouteValidator {

    /** Paths that never require authentication. */
    public static final List<String> OPEN_ENDPOINTS = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/actuator/health"
    );

    public final Predicate<ServerHttpRequest> isSecured = request -> {
        String path = request.getURI().getPath();

        boolean isOpenPath = OPEN_ENDPOINTS.stream().anyMatch(path::startsWith);
        if (isOpenPath) {
            return false;
        }

        // Menu browsing and uploaded photos are public: anyone can look at
        // restaurants, menus and their images without an account (a plain
        // <img> tag never sends an Authorization header), but only
        // signed-in users can order or manage them.
        boolean isPublicMenuBrowse = request.getMethod() == HttpMethod.GET
                && (path.startsWith("/api/restaurants") || path.startsWith("/api/images"));

        return !isPublicMenuBrowse;
    };
}
