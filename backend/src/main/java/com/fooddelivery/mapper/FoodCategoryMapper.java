package com.fooddelivery.mapper;

import com.fooddelivery.dto.menu.FoodCategoryResponse;
import com.fooddelivery.entity.FoodCategory;

public final class FoodCategoryMapper {
    private FoodCategoryMapper() { }

    public static FoodCategoryResponse toResponse(FoodCategory category) {
        if (category == null) return null;
        return new FoodCategoryResponse(category.getId(), category.getName(), category.getDisplayOrder());
    }
}
