package com.fooddelivery.dto.menu;

import jakarta.validation.constraints.NotNull;

public record FoodItemAvailabilityRequest(@NotNull Boolean available) { }
