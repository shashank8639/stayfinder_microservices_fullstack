package com.stayfinder.food.client;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class FeignConfig {

    @Bean
    RequestInterceptor authorizationRelayInterceptor() {
        return template -> {
            if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
                return;
            }
            String authorization = attrs.getRequest().getHeader("Authorization");
            if (authorization != null && !authorization.isBlank()) {
                template.header("Authorization", authorization);
            }
        };
    }
}
