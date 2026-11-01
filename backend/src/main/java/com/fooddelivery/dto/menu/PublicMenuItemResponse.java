package com.fooddelivery.dto.menu;

import java.math.BigDecimal;

/** Public menu fields; exact inventory quantities are intentionally omitted. */
public record PublicMenuItemResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        boolean vegetarian,
        int preparationTimeMinutes,
        String categoryName,
        String imageUrl) { }
