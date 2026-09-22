package com.quickbite.order.client;

import com.quickbite.order.dto.external.UserSummaryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "auth-service")
public interface AuthClient {

    @GetMapping("/auth/users/{id}")
    UserSummaryDto getUser(@PathVariable("id") Long id);
}
