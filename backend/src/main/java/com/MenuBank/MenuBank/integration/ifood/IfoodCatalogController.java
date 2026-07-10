package com.MenuBank.MenuBank.integration.ifood;

import com.MenuBank.MenuBank.auth.AuthHelper;
import com.MenuBank.MenuBank.integration.ifood.dto.IfoodCatalogImportResult;
import com.MenuBank.MenuBank.integration.ifood.services.IfoodCatalogImportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/integrations/ifood/catalog")
public class IfoodCatalogController {

    private final IfoodCatalogImportService importService;
    private final AuthHelper authHelper;

    public IfoodCatalogController(IfoodCatalogImportService importService, AuthHelper authHelper) {
        this.importService = importService;
        this.authHelper = authHelper;
    }

    @PostMapping("/import")
    public ResponseEntity<IfoodCatalogImportResult> importCatalog(Authentication auth) {
        UUID merchantId = authHelper.getMerchantId(auth);
        return ResponseEntity.ok(importService.importCatalog(merchantId));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleNotConnected(IllegalStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Conecte sua conta do iFood antes de importar o catálogo.");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(IfoodReauthorizationRequiredException.class)
    public ResponseEntity<ProblemDetail> handleReauthorizationRequired(
            IfoodReauthorizationRequiredException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "A autorização com o iFood expirou. Reconecte sua conta e tente novamente.");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }
}
