package com.MenuBank.MenuBank.integration.ifood;

import com.MenuBank.MenuBank.integration.ifood.dto.IfoodTokenResponse;
import com.MenuBank.MenuBank.integration.ifood.dto.IfoodUserCodeResponse;
import com.MenuBank.MenuBank.merchant.Merchant;
import com.MenuBank.MenuBank.merchant.MerchantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("IfoodTokenService")
class IfoodTokenServiceTest {

    @Mock private IfoodAuthClient authClient;
    @Mock private IfoodAppTokenRepository tokenRepository;
    @Mock private MerchantRepository merchantRepository;

    @InjectMocks private IfoodTokenService service;

    private UUID merchantId;
    private Merchant merchant;

    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();
        merchant = Merchant.builder().id(merchantId).merchantName("Test").build();
        service.setClientId("client-id");
        service.setClientSecret("client-secret");
    }

    @Test
    @DisplayName("startAuthorization chama requestUserCode e retorna resposta do iFood")
    void startAuthorization_shouldCallClientAndReturnResponse() {
        IfoodUserCodeResponse apiResponse = new IfoodUserCodeResponse();
        apiResponse.setUserCode("HJLX-LPSQ");
        apiResponse.setAuthorizationCodeVerifier("verifier123");
        apiResponse.setVerificationUrl("https://portal.ifood.com.br/apps/code");
        apiResponse.setExpiresIn(600);
        given(authClient.requestUserCode("client-id")).willReturn(apiResponse);

        IfoodUserCodeResponse result = service.startAuthorization(merchantId);

        assertThat(result.getUserCode()).isEqualTo("HJLX-LPSQ");
        assertThat(result.getExpiresIn()).isEqualTo(600);
        verify(authClient).requestUserCode("client-id");
    }

    @Test
    @DisplayName("connect troca código, persiste token e atualiza merchant")
    void connect_shouldExchangeCodePersistTokenAndUpdateMerchant() {
        IfoodTokenResponse tokenResponse = tokenResponse();
        given(merchantRepository.findById(merchantId)).willReturn(Optional.of(merchant));
        given(authClient.requestUserCode("client-id")).willReturn(userCodeResponse());
        given(authClient.exchangeCode(any(), any(), any(), any())).willReturn(tokenResponse);
        given(merchantRepository.save(any())).willReturn(merchant);

        service.startAuthorization(merchantId);
        service.connect(merchantId, "auth-code");

        ArgumentCaptor<IfoodAppToken> tokenCaptor = ArgumentCaptor.forClass(IfoodAppToken.class);
        verify(tokenRepository).deleteAll();
        verify(tokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getAccessToken()).isEqualTo("access.jwt");
        assertThat(tokenCaptor.getValue().getRefreshToken()).isEqualTo("refresh.jwt");

        ArgumentCaptor<Merchant> merchantCaptor = ArgumentCaptor.forClass(Merchant.class);
        verify(merchantRepository).save(merchantCaptor.capture());
        assertThat(merchantCaptor.getValue().getIfoodAuthorizedAt()).isNotNull();
    }

    @Test
    @DisplayName("getAccessToken retorna token válido sem renovar")
    void getAccessToken_shouldReturnValidTokenWithoutRefresh() {
        IfoodAppToken token = IfoodAppToken.builder()
                .accessToken("valid.jwt")
                .refreshToken("refresh.jwt")
                .expiresAt(LocalDateTime.now().plusHours(2))
                .refreshExpiresAt(LocalDateTime.now().plusDays(6))
                .updatedAt(LocalDateTime.now())
                .build();
        given(tokenRepository.findTopByOrderByUpdatedAtDesc()).willReturn(Optional.of(token));

        String result = service.getAccessToken();

        assertThat(result).isEqualTo("valid.jwt");
    }

    @Test
    @DisplayName("getAccessToken renova via refreshToken quando próximo da expiração")
    void getAccessToken_shouldRefreshWhenNearExpiry() {
        IfoodAppToken token = IfoodAppToken.builder()
                .accessToken("old.jwt")
                .refreshToken("refresh.jwt")
                .expiresAt(LocalDateTime.now().plusMinutes(3))
                .refreshExpiresAt(LocalDateTime.now().plusDays(6))
                .updatedAt(LocalDateTime.now())
                .build();
        IfoodTokenResponse refreshed = tokenResponse();
        refreshed.setAccessToken("new.jwt");
        given(tokenRepository.findTopByOrderByUpdatedAtDesc()).willReturn(Optional.of(token));
        given(authClient.refreshToken("client-id", "client-secret", "refresh.jwt")).willReturn(refreshed);
        given(tokenRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        String result = service.getAccessToken();

        assertThat(result).isEqualTo("new.jwt");
    }

    @Test
    @DisplayName("getAccessToken lança exceção quando refreshToken expirou")
    void getAccessToken_shouldThrowWhenRefreshTokenExpired() {
        IfoodAppToken token = IfoodAppToken.builder()
                .accessToken("old.jwt")
                .refreshToken("refresh.jwt")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .refreshExpiresAt(LocalDateTime.now().minusHours(1))
                .updatedAt(LocalDateTime.now())
                .build();
        given(tokenRepository.findTopByOrderByUpdatedAtDesc()).willReturn(Optional.of(token));

        assertThatThrownBy(() -> service.getAccessToken())
                .isInstanceOf(IfoodReauthorizationRequiredException.class);
    }

    @Test
    @DisplayName("revoke limpa campos do merchant e remove token se for o último")
    void revoke_shouldClearMerchantAndDeleteTokenWhenLastMerchant() {
        merchant.setIfoodMerchantId("ifood-merchant-id");
        merchant.setIfoodAuthorizedAt(LocalDateTime.now());
        given(merchantRepository.findById(merchantId)).willReturn(Optional.of(merchant));
        given(merchantRepository.countByIfoodMerchantIdIsNotNull()).willReturn(0L);

        service.revoke(merchantId);

        ArgumentCaptor<Merchant> captor = ArgumentCaptor.forClass(Merchant.class);
        verify(merchantRepository).save(captor.capture());
        assertThat(captor.getValue().getIfoodMerchantId()).isNull();
        assertThat(captor.getValue().getIfoodAuthorizedAt()).isNull();
        verify(tokenRepository).deleteAll();
    }

    private IfoodUserCodeResponse userCodeResponse() {
        IfoodUserCodeResponse r = new IfoodUserCodeResponse();
        r.setUserCode("HJLX-LPSQ");
        r.setAuthorizationCodeVerifier("verifier123");
        r.setVerificationUrl("https://portal.ifood.com.br/apps/code");
        r.setExpiresIn(600);
        return r;
    }

    private IfoodTokenResponse tokenResponse() {
        IfoodTokenResponse r = new IfoodTokenResponse();
        r.setAccessToken("access.jwt");
        r.setRefreshToken("refresh.jwt");
        r.setExpiresIn(10800);
        r.setRefreshTokenExpiresIn(604800);
        return r;
    }
}
