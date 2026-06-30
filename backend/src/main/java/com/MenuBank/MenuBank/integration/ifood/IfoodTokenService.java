package com.MenuBank.MenuBank.integration.ifood;

import com.MenuBank.MenuBank.integration.ifood.dto.IfoodTokenResponse;
import com.MenuBank.MenuBank.integration.ifood.dto.IfoodUserCodeResponse;
import com.MenuBank.MenuBank.merchant.Merchant;
import com.MenuBank.MenuBank.merchant.MerchantRepository;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IfoodTokenService {

    private final IfoodAuthClient authClient;
    private final IfoodAppTokenRepository tokenRepository;
    private final MerchantRepository merchantRepository;

    @Setter
    @Value("${ifood.client-id:}")
    private String clientId;

    @Setter
    @Value("${ifood.client-secret:}")
    private String clientSecret;

    private final Map<UUID, String> pendingVerifiers = new ConcurrentHashMap<>();

    public IfoodTokenService(IfoodAuthClient authClient,
                             IfoodAppTokenRepository tokenRepository,
                             MerchantRepository merchantRepository) {
        this.authClient = authClient;
        this.tokenRepository = tokenRepository;
        this.merchantRepository = merchantRepository;
    }

    public IfoodUserCodeResponse startAuthorization(UUID merchantId) {
        IfoodUserCodeResponse response = authClient.requestUserCode(clientId);
        pendingVerifiers.put(merchantId, response.getAuthorizationCodeVerifier());
        return response;
    }

    @Transactional
    public void connect(UUID merchantId, String authorizationCode) {
        String verifier = pendingVerifiers.remove(merchantId);
        if (verifier == null) {
            throw new IllegalStateException("No pending authorization for merchant " + merchantId);
        }

        IfoodTokenResponse tokenResponse = authClient.exchangeCode(clientId, clientSecret, authorizationCode, verifier);
        persistToken(tokenResponse);

        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found: " + merchantId));
        merchant.setIfoodAuthorizedAt(LocalDateTime.now());
        merchantRepository.save(merchant);
    }

    public String getAccessToken() {
        IfoodAppToken token = tokenRepository.findTopByOrderByUpdatedAtDesc()
                .orElseThrow(() -> new IfoodReauthorizationRequiredException());

        if (token.getRefreshExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IfoodReauthorizationRequiredException();
        }

        if (token.getExpiresAt().isBefore(LocalDateTime.now().plusMinutes(5))) {
            token = doRefresh(token);
        }

        return token.getAccessToken();
    }

    @Transactional
    public String handleUnauthorized() {
        IfoodAppToken token = tokenRepository.findTopByOrderByUpdatedAtDesc()
                .orElseThrow(() -> new IfoodReauthorizationRequiredException());

        if (token.getRefreshExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IfoodReauthorizationRequiredException();
        }

        return doRefresh(token).getAccessToken();
    }

    @Transactional
    public void revoke(UUID merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found: " + merchantId));
        merchant.setIfoodMerchantId(null);
        merchant.setIfoodAuthorizedAt(null);
        merchantRepository.save(merchant);
        pendingVerifiers.remove(merchantId);

        if (merchantRepository.countByIfoodMerchantIdIsNotNull() == 0) {
            tokenRepository.deleteAll();
        }
    }

    private IfoodAppToken doRefresh(IfoodAppToken current) {
        IfoodTokenResponse refreshed = authClient.refreshToken(clientId, clientSecret, current.getRefreshToken());
        return persistToken(refreshed);
    }

    private IfoodAppToken persistToken(IfoodTokenResponse tokenResponse) {
        tokenRepository.deleteAll();
        IfoodAppToken token = IfoodAppToken.builder()
                .accessToken(tokenResponse.getAccessToken())
                .refreshToken(tokenResponse.getRefreshToken())
                .expiresAt(LocalDateTime.now().plusSeconds(tokenResponse.getExpiresIn()))
                .refreshExpiresAt(LocalDateTime.now().plusSeconds(tokenResponse.getRefreshTokenExpiresIn()))
                .updatedAt(LocalDateTime.now())
                .build();
        return tokenRepository.save(token);
    }
}
