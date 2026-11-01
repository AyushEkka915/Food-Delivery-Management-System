package com.fooddelivery.dto.restaurant;

import java.math.BigDecimal;
import java.time.LocalTime;

/** Public restaurant profile fields without manager contact or internal identifiers. */
public record PublicRestaurantDetailResponse(
        Long id,
        String name,
        String description,
        String addressLine,
        String city,
        String locationName,
        String cuisine,
        LocalTime openingTime,
        LocalTime closingTime,
        BigDecimal ratingAverage,
        int ratingCount) { }
