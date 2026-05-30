package com.MenuBank.MenuBank.ingredient;

import java.util.UUID;

public record IngredientCreatedEvent(UUID merchantId, UUID ingredientId, String canonicalName) {}
