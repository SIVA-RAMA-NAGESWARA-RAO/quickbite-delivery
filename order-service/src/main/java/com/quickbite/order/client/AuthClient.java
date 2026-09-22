package com.quickbite.order.client;

import com.quickbite.order.config.FeignClientConfig;
import com.quickbite.order.dto.external.UserSummaryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Used only to look up a customer's email/name for order notifications. */
@FeignClient(name = "auth-service", configuration = FeignClientConfig.class)
public interface AuthClient {

    @GetMapping("/auth/users/{id}")
    UserSummaryDto getUser(@PathVariable("id") Long id);
}
