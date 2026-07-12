package com.MenuBank.MenuBank.billing;

import com.MenuBank.MenuBank.merchant.Merchant;
import com.MenuBank.MenuBank.merchant.MerchantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Merchants registered before the billing feature existed have no row in the
 * subscriptions table, because subscription creation only happens on sign-up
 * ({@link com.MenuBank.MenuBank.merchant.MerchantService#create}). For those
 * legacy merchants {@code GET /api/subscription/me} throws
 * {@link SubscriptionNotFoundException}, which maps to HTTP 404.
 *
 * <p>This backfill assigns a PENDING subscription (no plan yet) to every
 * merchant that lacks one, so they hit the "choose a plan" gate instead of a
 * 404. It is idempotent: merchants that already have a subscription are skipped.
 */
@Component
class LegacyPendingSubscriptionBackfill implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LegacyPendingSubscriptionBackfill.class);

    private final SubscriptionRepository subscriptionRepository;
    private final MerchantRepository merchantRepository;

    LegacyPendingSubscriptionBackfill(SubscriptionRepository subscriptionRepository,
                                      MerchantRepository merchantRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.merchantRepository = merchantRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Set<UUID> merchantsWithSubscription = subscriptionRepository.findAll().stream()
                .map(Subscription::getMerchantId)
                .collect(Collectors.toSet());

        LocalDateTime now = LocalDateTime.now();
        List<Subscription> toCreate = merchantRepository.findAll().stream()
                .map(Merchant::getId)
                .filter(merchantId -> !merchantsWithSubscription.contains(merchantId))
                .map(merchantId -> Subscription.builder()
                        .merchantId(merchantId)
                        .plan(null)
                        .status(SubscriptionStatus.PENDING)
                        .createdAt(now)
                        .updatedAt(now)
                        .build())
                .toList();

        if (toCreate.isEmpty()) {
            return;
        }

        subscriptionRepository.saveAll(toCreate);
        log.info("Backfill: {} lojistas legados receberam assinatura PENDING", toCreate.size());
    }
}
