package com.MenuBank.MenuBank.integration.anotaai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnotaAICatalogResponse {

    private boolean success;
    private String message;
    private List<AnotaAICategory> categories;

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AnotaAICategory {
        private String title;
        private String id;
        private List<AnotaAIItem> itens;

        @JsonProperty("is_additional")
        private boolean isAdditional;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AnotaAIItem {
        private String id;
        private String title;
        private double price;
        private boolean out;

        @JsonProperty("external_id")
        private String externalId;
    }
}
