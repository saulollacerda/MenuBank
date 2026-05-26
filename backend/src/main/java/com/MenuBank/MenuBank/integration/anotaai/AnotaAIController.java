package com.MenuBank.MenuBank.integration.anotaai;

import com.MenuBank.MenuBank.common.MerchantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/integrations/anotaai")
public class AnotaAIController {

    private final AnotaAISyncService syncService;
    private final MerchantContext merchantContext;

    public AnotaAIController(AnotaAISyncService syncService, MerchantContext merchantContext) {
        this.syncService = syncService;
        this.merchantContext = merchantContext;
    }

    @PostMapping("/orders")
    public ResponseEntity<AnotaAISyncResult> syncOrders() {
        UUID merchantId = merchantContext.getMerchantId();
        return ResponseEntity.ok(syncService.syncOrders(merchantId));
    }

    @PostMapping("/catalog")
    public ResponseEntity<AnotaAISyncResult> syncCatalog(
            @RequestParam(name = "clearRecipes", defaultValue = "false") boolean clearRecipes) {
        UUID merchantId = merchantContext.getMerchantId();
        return ResponseEntity.ok(syncService.syncCatalog(merchantId, clearRecipes));
    }
}
