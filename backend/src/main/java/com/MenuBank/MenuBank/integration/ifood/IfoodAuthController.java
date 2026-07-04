package com.MenuBank.MenuBank.integration.ifood;

import com.MenuBank.MenuBank.auth.AuthHelper;
import com.MenuBank.MenuBank.integration.ifood.dto.IfoodConnectRequest;
import com.MenuBank.MenuBank.integration.ifood.dto.IfoodStatusResponse;
import com.MenuBank.MenuBank.integration.ifood.dto.IfoodUserCodeResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/integrations/ifood/auth")
public class IfoodAuthController {

    private final IfoodTokenService tokenService;
    private final AuthHelper authHelper;

    public IfoodAuthController(IfoodTokenService tokenService, AuthHelper authHelper) {
        this.tokenService = tokenService;
        this.authHelper = authHelper;
    }

    @GetMapping("/status")
    public ResponseEntity<IfoodStatusResponse> status(Authentication auth) {
        UUID merchantId = authHelper.getMerchantId(auth);
        return ResponseEntity.ok(new IfoodStatusResponse(tokenService.isConnected(merchantId)));
    }

    @PostMapping("/start")
    public ResponseEntity<IfoodUserCodeResponse> start(Authentication auth) {
        UUID merchantId = authHelper.getMerchantId(auth);
        return ResponseEntity.ok(tokenService.startAuthorization(merchantId));
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
}
