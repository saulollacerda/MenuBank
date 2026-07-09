package com.MenuBank.MenuBank.integration.ifood.dto;

import com.MenuBank.MenuBank.integration.ifood.IfoodIntegrationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class IfoodStatusResponse {
    private boolean connected;
    private LocalDateTime catalogImportedAt;
    private boolean orderSyncEnabled;

    public static IfoodStatusResponse from(IfoodIntegrationStatus status) {
        return new IfoodStatusResponse(
                status.connected(), status.catalogImportedAt(), status.orderSyncEnabled());
    }
}
