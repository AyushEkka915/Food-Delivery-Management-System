package com.fooddelivery.dto.restaurant;

import com.fooddelivery.entity.enums.RestaurantStatus;
import jakarta.validation.constraints.NotNull;

public record RestaurantStatusRequest(@NotNull RestaurantStatus status) { }
