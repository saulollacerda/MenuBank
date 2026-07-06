package com.MenuBank.MenuBank.integration.ifood;

import com.MenuBank.MenuBank.auth.AuthHelper;
import com.MenuBank.MenuBank.integration.ifood.dto.IfoodStatusResponse;
import com.MenuBank.MenuBank.integration.ifood.dto.IfoodSyncToggleRequest;
import com.MenuBank.MenuBank.integration.ifood.services.IfoodIntegrationSettingsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/integrations/ifood/sync")
public class IfoodSyncController {

    private final IfoodIntegrationSettingsService settingsService;
    private final AuthHelper authHelper;

    public IfoodSyncController(IfoodIntegrationSettingsService settingsService, AuthHelper authHelper) {
        this.settingsService = settingsService;
        this.authHelper = authHelper;
    }

    @PutMapping
    public ResponseEntity<IfoodStatusResponse> setOrderSync(
            @Valid @RequestBody IfoodSyncToggleRequest request, Authentication auth) {
        UUID merchantId = authHelper.getMerchantId(auth);
        return ResponseEntity.ok(IfoodStatusResponse.from(
                settingsService.setOrderSyncEnabled(merchantId, request.getEnabled())));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleNotConnected(IllegalStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Conecte sua conta do iFood antes de ativar a sincronia de pedidos.");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }
}
