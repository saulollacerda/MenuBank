package com.MenuBank.MenuBank.integration.ifood.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/** One requested availability change: a MenuBank product and its target iFood status. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IfoodCatalogStatusChange(UUID productId, String status) {
}
