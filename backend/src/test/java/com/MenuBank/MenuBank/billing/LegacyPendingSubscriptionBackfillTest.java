package com.MenuBank.MenuBank.billing;

import com.MenuBank.MenuBank.merchant.Merchant;
import com.MenuBank.MenuBank.merchant.MerchantRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("LegacyPendingSubscriptionBackfill")
class LegacyPendingSubscriptionBackfillTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private MerchantRepository merchantRepository;

    @InjectMocks
    private LegacyPendingSubscriptionBackfill backfill;

    @Test
    @DisplayName("não faz nada quando não há lojistas")
    void shouldDoNothingWhenNoMerchants() {
        given(merchantRepository.findAll()).willReturn(List.of());
        given(subscriptionRepository.findAll()).willReturn(List.of());

        backfill.run();

        then(subscriptionRepository).should(never()).saveAll(any());
    }

    @Test
    @DisplayName("cria assinatura PENDING para lojista legado sem assinatura")
    void shouldBackfillPendingForMerchantWithoutSubscription() {
        UUID legacyMerchantId = UUID.randomUUID();
        Merchant legacy = Merchant.builder().id(legacyMerchantId).build();

        given(merchantRepository.findAll()).willReturn(List.of(legacy));
        given(subscriptionRepository.findAll()).willReturn(List.of());

        backfill.run();

        ArgumentCaptor<Iterable<Subscription>> captor = ArgumentCaptor.captor();
        then(subscriptionRepository).should().saveAll(captor.capture());

        List<Subscription> saved = toList(captor.getValue());
        assertThat(saved).hasSize(1);
        Subscription created = saved.get(0);
        assertThat(created.getMerchantId()).isEqualTo(legacyMerchantId);
        assertThat(created.getStatus()).isEqualTo(SubscriptionStatus.PENDING);
        assertThat(created.getPlan()).isNull();
        assertThat(created.getTrialEndsAt()).isNull();
        assertThat(created.getCreatedAt()).isNotNull();
        assertThat(created.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("não duplica assinatura para lojista que já possui uma")
    void shouldNotDuplicateWhenSubscriptionAlreadyExists() {
        UUID withSubscriptionId = UUID.randomUUID();
        UUID legacyMerchantId = UUID.randomUUID();
        Merchant withSubscription = Merchant.builder().id(withSubscriptionId).build();
        Merchant legacy = Merchant.builder().id(legacyMerchantId).build();

        Subscription existing = Subscription.builder()
                .merchantId(withSubscriptionId)
                .status(SubscriptionStatus.ACTIVE)
                .build();

        given(merchantRepository.findAll()).willReturn(List.of(withSubscription, legacy));
        given(subscriptionRepository.findAll()).willReturn(List.of(existing));

        backfill.run();

        ArgumentCaptor<Iterable<Subscription>> captor = ArgumentCaptor.captor();
        then(subscriptionRepository).should().saveAll(captor.capture());

        List<Subscription> saved = toList(captor.getValue());
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getMerchantId()).isEqualTo(legacyMerchantId);
        assertThat(saved.get(0).getStatus()).isEqualTo(SubscriptionStatus.PENDING);
    }

    @Test
    @DisplayName("não faz nada quando todos os lojistas já possuem assinatura")
    void shouldDoNothingWhenAllMerchantsHaveSubscription() {
        UUID merchantId = UUID.randomUUID();
        Merchant merchant = Merchant.builder().id(merchantId).build();
        Subscription existing = Subscription.builder()
                .merchantId(merchantId)
                .status(SubscriptionStatus.ACTIVE)
                .build();

        given(merchantRepository.findAll()).willReturn(List.of(merchant));
        given(subscriptionRepository.findAll()).willReturn(List.of(existing));

        backfill.run();

        then(subscriptionRepository).should(never()).saveAll(any());
    }

    private static <T> List<T> toList(Iterable<T> iter) {
        java.util.ArrayList<T> list = new java.util.ArrayList<>();
        iter.forEach(list::add);
        return list;
    }
}
