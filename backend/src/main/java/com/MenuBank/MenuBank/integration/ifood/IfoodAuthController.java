package com.MenuBank.MenuBank.integration.ifood;

import com.MenuBank.MenuBank.auth.AuthHelper;
import com.MenuBank.MenuBank.integration.ifood.dto.IfoodConnectRequest;
import com.MenuBank.MenuBank.integration.ifood.dto.IfoodStartAuthResponse;
import com.MenuBank.MenuBank.integration.ifood.dto.IfoodStatusResponse;
import com.MenuBank.MenuBank.integration.ifood.services.IfoodIntegrationSettingsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import java.util.UUID;

@RestController
@RequestMapping("/api/integrations/ifood/auth")
public class IfoodAuthController {

    private final IfoodTokenService tokenService;
    private final IfoodIntegrationSettingsService settingsService;
    private final AuthHelper authHelper;

    public IfoodAuthController(IfoodTokenService tokenService,
                               IfoodIntegrationSettingsService settingsService,
                               AuthHelper authHelper) {
        this.tokenService = tokenService;
        this.settingsService = settingsService;
        this.authHelper = authHelper;
    }

    @GetMapping("/status")
    public ResponseEntity<IfoodStatusResponse> status(Authentication auth) {
        UUID merchantId = authHelper.getMerchantId(auth);
        return ResponseEntity.ok(IfoodStatusResponse.from(settingsService.getStatus(merchantId)));
    }

    @PostMapping("/start")
    public ResponseEntity<IfoodStartAuthResponse> start(Authentication auth) {
        UUID merchantId = authHelper.getMerchantId(auth);
        return ResponseEntity.ok(IfoodStartAuthResponse.from(tokenService.startAuthorization(merchantId)));
    }

    @PostMapping("/connect")
    public ResponseEntity<Void> connect(@Valid @RequestBody IfoodConnectRequest request, Authentication auth) {
        UUID merchantId = authHelper.getMerchantId(auth);
        tokenService.connect(merchantId, request.getAuthorizationCode());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/revoke")
    public ResponseEntity<Void> revoke(Authentication auth) {
        UUID merchantId = authHelper.getMerchantId(auth);
        tokenService.revoke(merchantId);
        return ResponseEntity.noContent().build();
    }

    // Pending verifier is kept in memory only — a backend restart mid-flow loses it and the
    // merchant must generate a new userCode. Surface that as 409 instead of a generic 500.
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleNoPendingAuthorization(IllegalStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Nenhuma autorização pendente encontrada. Gere um novo código de vínculo e tente novamente.");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<ProblemDetail> handleIfoodRejectedCode(HttpClientErrorException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Código de autorização inválido ou expirado. Confira o código copiado do portal do iFood.");
        return ResponseEntity.badRequest().body(problem);
    }
}
