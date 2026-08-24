package com.stayfinder.booking.security;

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

    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    public boolean isStaff() {
        return hasRole("ADMIN") || hasRole("HOTEL_OWNER");
    }

    private boolean hasRole(String role) {
        return roles.contains(role) || roles.contains("ROLE_" + role);
    }
}
