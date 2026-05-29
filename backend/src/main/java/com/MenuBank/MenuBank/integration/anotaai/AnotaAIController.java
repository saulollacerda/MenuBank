package com.MenuBank.MenuBank.integration.anotaai;

import com.MenuBank.MenuBank.auth.AuthHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/integrations/anotaai")
public class AnotaAIController {

    private final AnotaAISyncService syncService;
    private final AuthHelper authHelper;

    public AnotaAIController(AnotaAISyncService syncService, AuthHelper authHelper) {
        this.syncService = syncService;
        this.authHelper = authHelper;
    }

    @PostMapping("/orders")
    public ResponseEntity<AnotaAISyncResult> syncOrders(Authentication auth) {
        UUID merchantId = authHelper.getMerchantId(auth);
        return ResponseEntity.ok(syncService.syncOrders(merchantId));
    }

    @PostMapping("/catalog")
    public ResponseEntity<AnotaAISyncResult> syncCatalog(
            Authentication auth,
            @RequestParam(name = "clearRecipes", defaultValue = "false") boolean clearRecipes) {
        UUID merchantId = authHelper.getMerchantId(auth);
        return ResponseEntity.ok(syncService.syncCatalog(merchantId, clearRecipes));
    }
}
