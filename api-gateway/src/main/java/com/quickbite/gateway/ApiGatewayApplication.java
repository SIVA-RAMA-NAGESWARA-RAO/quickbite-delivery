package com.quickbite.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Single public entry point for the platform.
 *
 * Every client request (from the React app, Postman, or a mobile client)
 * comes through here first. The gateway resolves the target service via
 * Eureka, applies JWT validation for protected routes, and forwards the
 * request to a healthy instance chosen by the load balancer.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
