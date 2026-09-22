package com.quickbite.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Service registry for QuickBite Delivery.
 *
 * Every other microservice (API Gateway, Auth, Restaurant, Order, Payment)
 * registers itself here on startup and uses it to discover the current,
 * load-balanced location of the services it depends on. This lets us run
 * multiple instances of any service behind a single logical name
 * (e.g. "order-service") without hard-coding host/port anywhere.
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
