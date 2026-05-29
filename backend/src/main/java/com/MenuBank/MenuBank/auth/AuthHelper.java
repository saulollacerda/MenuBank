package com.MenuBank.MenuBank.auth;

import com.MenuBank.MenuBank.identity.Identity;
import com.MenuBank.MenuBank.identity.IdentityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Resolves the authenticated merchant from the Supabase user id placed in the
 * {@link Authentication} principal by {@code JwtAuthFilter}.
 * <p>
 * The {@code uuid -> merchantId} mapping is cached (Caffeine, 10 min TTL). The cache
 * key is the principal (the Supabase user id). The 403 thrown for a not-yet-provisioned
 * user is an exception, so it is never cached.
 */
@Component
public class AuthHelper {

    private static final String CURRENT_PROVIDER = "supabase";

    @Autowired
    private IdentityRepository identityRepository;

    @Cacheable(value = "merchantIdByProviderUser", key = "#auth.principal")
    public UUID getMerchantId(Authentication auth) {
        String uuid = (String) auth.getPrincipal();
        return identityRepository
                .findByProviderAndProviderUserId(CURRENT_PROVIDER, uuid)
                .map(Identity::getMerchantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Nenhum merchant encontrado para este usuário"));
    }
}
