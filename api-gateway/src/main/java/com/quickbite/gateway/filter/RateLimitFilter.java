package com.quickbite.gateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A deliberately simple, dependency-free fixed-window rate limiter that runs
 * at the edge, before any request is routed to a backend service. This is
 * what protects QuickBite's services from being overwhelmed during a lunch
 * or dinner traffic spike - each client (identified by user id once
 * authenticated, or IP address before that) gets a bounded number of
 * requests per window; anyone over the limit gets a fast, cheap 429 instead
 * of competing for the same threads real orders need.
 *
 * This intentionally avoids Redis (or any other external store) so the
 * whole platform keeps running with nothing but a JVM per service - correct
 * for a single-gateway-instance deployment. A real multi-instance rollout
 * would swap this for Spring Cloud Gateway's Redis-backed RequestRateLimiter
 * so every gateway instance shares the same counters.
 */
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final class Window {
        volatile long windowStartMillis;
        final AtomicLong count = new AtomicLong(0);
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    private final int maxRequestsPerWindow;
    private final long windowSizeMillis;

    public RateLimitFilter(
            @Value("${rate-limit.requests-per-window:60}") int maxRequestsPerWindow,
            @Value("${rate-limit.window-seconds:10}") long windowSeconds) {
        this.maxRequestsPerWindow = maxRequestsPerWindow;
        this.windowSizeMillis = windowSeconds * 1000;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Never rate-limit the health check Docker/Eureka/humans use to see if the gateway is alive.
        if (path.startsWith("/actuator/health")) {
            return chain.filter(exchange);
        }

        String clientKey = resolveClientKey(request);
        Window window = windows.computeIfAbsent(clientKey, k -> newWindow());

        long now = System.currentTimeMillis();
        synchronized (window) {
            if (now - window.windowStartMillis > windowSizeMillis) {
                window.windowStartMillis = now;
                window.count.set(0);
            }
        }

        long currentCount = window.count.incrementAndGet();
        if (currentCount > maxRequestsPerWindow) {
            return reject(exchange);
        }

        return chain.filter(exchange);
    }

    private Window newWindow() {
        Window w = new Window();
        w.windowStartMillis = System.currentTimeMillis();
        return w;
    }

    /** Rate-limit per signed-in user once we can tell who they are, otherwise per IP. */
    private String resolveClientKey(ServerHttpRequest request) {
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        InetSocketAddress remote = request.getRemoteAddress();
        return remote != null ? remote.getAddress().getHostAddress() : "unknown";
    }

    private Mono<Void> reject(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        exchange.getResponse().getHeaders().add("Retry-After", String.valueOf(windowSizeMillis / 1000));
        String body = "{\"status\":429,\"error\":\"Too Many Requests\","
                + "\"message\":\"Rate limit exceeded. Please slow down and try again shortly.\"}";
        var buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -2; // run before JWT validation, so abusive traffic is rejected as early as possible
    }
}
