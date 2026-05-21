package com.MenuBank.MenuBank.integration.anotaai;

import com.MenuBank.MenuBank.common.UserContext;
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
    private final UserContext userContext;

    public AnotaAIController(AnotaAISyncService syncService, UserContext userContext) {
        this.syncService = syncService;
        this.userContext = userContext;
    }

    @PostMapping("/orders")
    public ResponseEntity<AnotaAISyncResult> syncOrders() {
        UUID ownerId = userContext.getUserId();
        return ResponseEntity.ok(syncService.syncOrders(ownerId));
    }

    @PostMapping("/catalog")
    public ResponseEntity<AnotaAISyncResult> syncCatalog(
            @RequestParam(name = "clearRecipes", defaultValue = "false") boolean clearRecipes) {
        UUID ownerId = userContext.getUserId();
        return ResponseEntity.ok(syncService.syncCatalog(ownerId, clearRecipes));
    }
}
