package com.fooddelivery.dto.menu;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Optional;

public record FoodItemUpdateRequest(
        Optional<@Positive Long> categoryId,
        Optional<@Size(min = 1, max = 150) String> name,
        Optional<@Size(max = 500) String> description,
        Optional<@DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal> price,
        Optional<Boolean> vegetarian,
        Optional<@Min(1) Integer> preparationTimeMinutes,
        Optional<@Size(max = 500) String> imageUrl) {
    public FoodItemUpdateRequest {
        categoryId = normalize(categoryId);
        name = normalize(name);
        description = normalize(description);
        price = normalize(price);
        vegetarian = normalize(vegetarian);
        preparationTimeMinutes = normalize(preparationTimeMinutes);
        imageUrl = normalize(imageUrl);
    }

    private static <T> Optional<T> normalize(Optional<T> field) {
        return field == null ? Optional.empty() : field;
    }
}
