package com.MenuBank.MenuBank.integration.anotaai;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AnotaAISyncResult {

    private int ordersImported;
    private int ordersSkipped;
    private int categoriesCreated;
    private int categoriesUpdated;
    private int productsCreated;
    private int productsUpdated;
    private int ingredientCategoriesCreated;
    private int ingredientCategoriesUpdated;
    private int ingredientsCreated;
    private int ingredientsUpdated;
    private int productIngredientsCreated;
    private List<String> errors;
}
