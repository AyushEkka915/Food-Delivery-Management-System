package com.fooddelivery.repository;

import com.fooddelivery.entity.FoodCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodCategoryRepository extends JpaRepository<FoodCategory, Long> {
    List<FoodCategory> findAllByOrderByDisplayOrderAscNameAsc();
}
