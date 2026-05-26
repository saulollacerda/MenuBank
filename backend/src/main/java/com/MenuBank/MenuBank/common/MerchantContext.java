package com.MenuBank.MenuBank.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Provides access to the currently authenticated merchant.
 * <p>
 * In MenuBank, the JWT subject ("sub") is the merchant's UUID.
 */
@Component
public class MerchantContext {

    public UUID getMerchantId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new IllegalStateException("No authenticated merchant in SecurityContext");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof Jwt jwt) {
            return parseMerchantId(jwt.getSubject());
        }

        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            return parseMerchantId(jwtAuthenticationToken.getToken().getSubject());
        }

        if (principal instanceof String s) {
            // Best-effort support for non-JWT principals (mainly for tests/dev).
            return parseMerchantId(s);
        }

        throw new IllegalStateException("Authenticated principal is not a JWT");
    }

    private UUID parseMerchantId(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("JWT subject is not a valid UUID", ex);
        }
    }
}
