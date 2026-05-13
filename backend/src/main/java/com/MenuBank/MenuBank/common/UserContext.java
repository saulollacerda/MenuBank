package com.MenuBank.MenuBank.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Provides access to the currently authenticated user.
 * <p>
 * In MenuBank, the JWT subject ("sub") is the user's UUID.
 */
@Component
public class UserContext {

    public UUID getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new IllegalStateException("No authenticated user in SecurityContext");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof Jwt jwt) {
            return parseUserId(jwt.getSubject());
        }

        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            return parseUserId(jwtAuthenticationToken.getToken().getSubject());
        }

        if (principal instanceof String s) {
            // Best-effort support for non-JWT principals (mainly for tests/dev).
            return parseUserId(s);
        }

        throw new IllegalStateException("Authenticated principal is not a JWT");
    }

    private UUID parseUserId(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("JWT subject is not a valid UUID", ex);
        }
    }
}

