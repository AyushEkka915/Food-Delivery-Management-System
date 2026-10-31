package com.fooddelivery.dto.restaurant;

import com.fooddelivery.entity.enums.RestaurantStatus;
import java.math.BigDecimal;

public record RestaurantSummaryResponse(Long id, String name, String cuisine, String city,
        RestaurantStatus status, BigDecimal ratingAverage, int ratingCount) { }
