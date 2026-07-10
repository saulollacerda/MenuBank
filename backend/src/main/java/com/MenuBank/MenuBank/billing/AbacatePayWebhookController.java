package com.MenuBank.MenuBank.billing;

import com.MenuBank.MenuBank.integration.abacatepay.dto.AbacatePayWebhookBilling;
import com.MenuBank.MenuBank.integration.abacatepay.dto.AbacatePayWebhookEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/webhooks/abacatepay")
public class AbacatePayWebhookController {

    private static final String BILLING_PAID_EVENT = "billing.paid";
    private static final Logger log = LoggerFactory.getLogger(AbacatePayWebhookController.class);

    private final AbacatePayBillingService billingService;
    private final String webhookSecret;

    public AbacatePayWebhookController(AbacatePayBillingService billingService,
                                       @Value("${abacatepay.webhook-secret}") String webhookSecret) {
        this.billingService = billingService;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping
    public ResponseEntity<Void> handleWebhook(
            @RequestParam("webhookSecret") String receivedSecret,
            @RequestBody AbacatePayWebhookEvent event) {
        if (!isSecretValid(receivedSecret)) {
            log.warn("Webhook AbacatePay rejeitado: secret inválido");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!BILLING_PAID_EVENT.equals(event.getEvent())) {
            log.info("Webhook AbacatePay ignorado: evento '{}' não tratado", event.getEvent());
            return ResponseEntity.ok().build();
        }

        AbacatePayWebhookBilling billing = event.getData() != null ? event.getData().getBilling() : null;
        if (billing == null || billing.getId() == null) {
            log.warn("Webhook AbacatePay billing.paid sem dados de billing — ignorando");
            return ResponseEntity.ok().build();
        }

        Long amountCents = billing.getAmount() != null
                ? billing.getAmount()
                : (event.getData().getPayment() != null ? event.getData().getPayment().getAmount() : null);

        billingService.handleBillingPaid(billing.getId(), billing.getExternalId(), amountCents);
        return ResponseEntity.ok().build();
    }

    private boolean isSecretValid(String receivedSecret) {
        if (receivedSecret == null || webhookSecret == null || webhookSecret.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                receivedSecret.getBytes(StandardCharsets.UTF_8),
                webhookSecret.getBytes(StandardCharsets.UTF_8));
    }
}
