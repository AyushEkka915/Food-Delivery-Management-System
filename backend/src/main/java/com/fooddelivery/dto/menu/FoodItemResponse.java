package com.fooddelivery.dto.menu;

import java.math.BigDecimal;

public record FoodItemResponse(Long id, Long restaurantId, Long categoryId, String categoryName, String name,
        String description, BigDecimal price, boolean vegetarian, boolean available, int stockQuantity,
        int preparationTimeMinutes, String imageUrl, BigDecimal ratingAverage, int ratingCount) { }
