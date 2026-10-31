package com.fooddelivery.mapper;

import com.fooddelivery.dto.menu.FoodItemResponse;
import com.fooddelivery.entity.FoodItem;

public final class FoodItemMapper {
    private FoodItemMapper() { }

    public static FoodItemResponse toResponse(FoodItem item) {
        if (item == null) return null;
        var restaurant = item.getRestaurant();
        var category = item.getCategory();
        return new FoodItemResponse(item.getId(), restaurant == null ? null : restaurant.getId(),
                category == null ? null : category.getId(), category == null ? null : category.getName(),
                item.getName(), item.getDescription(), item.getPrice(),
                item.isVegetarian(), item.isAvailable(), item.getStockQuantity(), item.getPreparationTimeMinutes(),
                item.getImageUrl(), item.getRatingAverage(), item.getRatingCount());
    }

}
