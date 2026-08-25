package com.stayfinder.food.security;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

public record CurrentUser(Long userId, String email, List<String> roles) {

    public static CurrentUser from(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return new CurrentUser(
                Long.parseLong(jwt.getSubject()),
                jwt.getClaimAsString("email"),
                roles == null ? List.of() : roles
        );
    }

    public boolean isStaff() {
        return roles.contains("ADMIN")
                || roles.contains("HOTEL_OWNER")
                || roles.contains("ROLE_ADMIN")
                || roles.contains("ROLE_HOTEL_OWNER");
    }
}
