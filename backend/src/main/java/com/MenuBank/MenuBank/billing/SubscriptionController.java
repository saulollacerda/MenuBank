package com.MenuBank.MenuBank.billing;

import com.MenuBank.MenuBank.auth.AuthHelper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/subscription")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final AbacatePayBillingService abacatePayBillingService;
    private final AuthHelper authHelper;

    public SubscriptionController(SubscriptionService subscriptionService,
                                  AbacatePayBillingService abacatePayBillingService,
                                  AuthHelper authHelper) {
        this.subscriptionService = subscriptionService;
        this.abacatePayBillingService = abacatePayBillingService;
        this.authHelper = authHelper;
    }

    @GetMapping("/me")
    public ResponseEntity<SubscriptionResponse> getMySubscription(Authentication auth) {
        UUID merchantId = authHelper.getMerchantId(auth);
        return ResponseEntity.ok(subscriptionService.getMySubscription(merchantId));
    }

    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> createCheckout(
            Authentication auth,
            @Valid @RequestBody CheckoutRequest request) {
        UUID merchantId = authHelper.getMerchantId(auth);
        return ResponseEntity.ok(abacatePayBillingService.createCheckout(merchantId, request.getPlanId()));
    }

    @PostMapping("/revenue-report")
    public ResponseEntity<RevenueReportResponse> submitRevenueReport(
            Authentication auth,
            @Valid @RequestBody RevenueReportRequest request) {
        UUID merchantId = authHelper.getMerchantId(auth);
        RevenueReportResponse response = subscriptionService.submitRevenueReport(merchantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
