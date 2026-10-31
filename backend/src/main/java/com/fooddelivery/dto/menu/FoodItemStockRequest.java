package com.fooddelivery.dto.menu;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record FoodItemStockRequest(@NotNull @Min(0) Integer stockQuantity) { }
