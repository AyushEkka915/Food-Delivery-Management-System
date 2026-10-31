package com.fooddelivery.dto.menu;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record FoodItemCreateRequest(
        @NotNull @Positive Long categoryId,
        @NotBlank @Size(max = 150) String name,
        @Size(max = 500) String description,
        @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal price,
        @NotNull Boolean vegetarian,
        @NotNull @Min(0) Integer stockQuantity,
        @NotNull @Min(1) Integer preparationTimeMinutes,
        @Size(max = 500) String imageUrl) { }
