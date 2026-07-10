package com.MenuBank.MenuBank.billing;

import com.MenuBank.MenuBank.integration.abacatepay.AbacatePayClient;
import com.MenuBank.MenuBank.integration.abacatepay.dto.AbacatePayCheckoutData;
import com.MenuBank.MenuBank.integration.abacatepay.dto.AbacatePayCheckoutItem;
import com.MenuBank.MenuBank.integration.abacatepay.dto.AbacatePayCheckoutRequest;
import com.MenuBank.MenuBank.integration.abacatepay.dto.AbacatePayCustomer;
import com.MenuBank.MenuBank.integration.abacatepay.dto.AbacatePayProductRequest;
import com.MenuBank.MenuBank.merchant.Merchant;
import com.MenuBank.MenuBank.merchant.MerchantNotFoundException;
import com.MenuBank.MenuBank.merchant.MerchantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AbacatePayBillingService {

    private static final String EXTERNAL_ID_PREFIX = "menubank";
    private static final Logger log = LoggerFactory.getLogger(AbacatePayBillingService.class);

    private final AbacatePayClient abacatePayClient;
    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final MerchantRepository merchantRepository;
    private final String frontendBaseUrl;

    public AbacatePayBillingService(AbacatePayClient abacatePayClient,
                                    PlanRepository planRepository,
                                    SubscriptionRepository subscriptionRepository,
                                    InvoiceRepository invoiceRepository,
                                    MerchantRepository merchantRepository,
                                    @Value("${app.frontend-base-url}") String frontendBaseUrl) {
        this.abacatePayClient = abacatePayClient;
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.invoiceRepository = invoiceRepository;
        this.merchantRepository = merchantRepository;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Transactional
    public CheckoutResponse createCheckout(UUID merchantId, UUID planId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new PlanNotFoundException(planId));
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException(merchantId));

        String productId = ensureAbacatePayProduct(plan);

        AbacatePayCheckoutData checkout = abacatePayClient.createCheckout(AbacatePayCheckoutRequest.builder()
                .items(List.of(new AbacatePayCheckoutItem(productId, 1)))
                .customer(AbacatePayCustomer.builder()
                        .name(merchant.getMerchantName())
                        .cellphone(merchant.getPhone())
                        .email(merchant.getEmail())
                        .taxId(merchant.getCnpj())
                        .build())
                .externalId(buildExternalId(merchantId, planId))
                .metadata(Map.of(
                        "merchantId", merchantId.toString(),
                        "planId", planId.toString()))
                .returnUrl(frontendBaseUrl + "/settings?section=billing")
                .completionUrl(frontendBaseUrl + "/settings?section=billing&paid=1")
                .build());

        return CheckoutResponse.builder()
                .url(checkout.getUrl())
                .build();
    }

    @Transactional
    public void handleBillingPaid(String billingId, String externalId, Long amountCents) {
        if (invoiceRepository.findByAbacatepayBillingId(billingId).isPresent()) {
            log.info("AbacatePay billing {} já processado — ignorando", billingId);
            return;
        }

        UUID[] ids = parseExternalId(externalId);
        if (ids == null) {
            log.warn("AbacatePay billing {} com externalId desconhecido '{}' — ignorando", billingId, externalId);
            return;
        }
        UUID merchantId = ids[0];
        UUID planId = ids[1];

        Subscription subscription = subscriptionRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new SubscriptionNotFoundException(merchantId));
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new PlanNotFoundException(planId));

        LocalDateTime now = LocalDateTime.now();
        subscription.setPlan(plan);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodStart(now);
        subscription.setCurrentPeriodEnd(now.plusMonths(1));
        subscription.setUpdatedAt(now);
        subscriptionRepository.save(subscription);

        BigDecimal amount = amountCents != null
                ? BigDecimal.valueOf(amountCents).movePointLeft(2)
                : plan.getPriceMonthly();
        invoiceRepository.save(Invoice.builder()
                .subscription(subscription)
                .amount(amount)
                .status(InvoiceStatus.PAID)
                .abacatepayBillingId(billingId)
                .paidAt(now)
                .dueAt(now)
                .createdAt(now)
                .build());

        log.info("Assinatura do merchant {} ativada no plano '{}' até {}",
                merchantId, plan.getName(), subscription.getCurrentPeriodEnd());
    }

    private String ensureAbacatePayProduct(Plan plan) {
        if (plan.getAbacatepayProductId() != null) {
            return plan.getAbacatepayProductId();
        }

        long priceCents = plan.getPriceMonthly().movePointRight(2).longValueExact();
        String productId = abacatePayClient.createProduct(AbacatePayProductRequest.builder()
                .externalId("plan-" + plan.getId())
                .name("MenuBank — Plano " + plan.getName())
                .description("Assinatura mensal do plano " + plan.getName())
                .price(priceCents)
                .currency("BRL")
                .build());

        plan.setAbacatepayProductId(productId);
        planRepository.save(plan);
        return productId;
    }

    private String buildExternalId(UUID merchantId, UUID planId) {
        return EXTERNAL_ID_PREFIX + ":" + merchantId + ":" + planId;
    }

    private UUID[] parseExternalId(String externalId) {
        if (externalId == null) {
            return null;
        }
        String[] parts = externalId.split(":");
        if (parts.length != 3 || !EXTERNAL_ID_PREFIX.equals(parts[0])) {
            return null;
        }
        try {
            return new UUID[]{UUID.fromString(parts[1]), UUID.fromString(parts[2])};
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
