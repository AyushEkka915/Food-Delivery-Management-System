package com.fooddelivery.dto.restaurant;

import java.math.BigDecimal;

/** Public-facing summary fields; excludes manager identity and contact information. */
public record PublicRestaurantSummaryResponse(
        Long id,
        String name,
        String cuisine,
        String description,
        String city,
        String locationName,
        BigDecimal ratingAverage,
        int ratingCount) { }
