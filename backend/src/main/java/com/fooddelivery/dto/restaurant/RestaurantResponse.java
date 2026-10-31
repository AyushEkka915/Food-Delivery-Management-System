package com.fooddelivery.dto.restaurant;

import com.fooddelivery.entity.enums.RestaurantStatus;
import java.math.BigDecimal;
import java.time.LocalTime;

public record RestaurantResponse(Long id, String name, String description, String phone, String addressLine,
        String city, Long locationId, String cuisine, RestaurantStatus status, LocalTime openingTime,
        LocalTime closingTime, BigDecimal ratingAverage, int ratingCount) { }
