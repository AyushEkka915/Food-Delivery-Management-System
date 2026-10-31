package com.fooddelivery.repository;

import com.fooddelivery.entity.FoodItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {
    Page<FoodItem> findByRestaurant_Id(Long restaurantId, Pageable pageable);
    Page<FoodItem> findByRestaurant_IdAndAvailableTrue(Long restaurantId, Pageable pageable);
}
