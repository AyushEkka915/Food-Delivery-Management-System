package com.fooddelivery.repository;

import com.fooddelivery.entity.FoodItem;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {
    @EntityGraph(attributePaths = "category")
    Page<FoodItem> findByRestaurant_Id(Long restaurantId, Pageable pageable);
    Page<FoodItem> findByRestaurant_IdAndAvailableTrue(Long restaurantId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from FoodItem item where item.id = :itemId and item.restaurant.id = :restaurantId")
    Optional<FoodItem> findByIdAndRestaurant_IdForUpdate(
            @Param("itemId") Long itemId, @Param("restaurantId") Long restaurantId);

    @Query(value = "select item from FoodItem item join item.restaurant restaurant "
            + "join fetch item.category where restaurant.id = :restaurantId "
            + "and restaurant.status = com.fooddelivery.entity.enums.RestaurantStatus.APPROVED "
            + "and item.available = true and item.stockQuantity > 0 "
            + "order by item.name asc, item.id asc",
            countQuery = "select count(item) from FoodItem item join item.restaurant restaurant "
                    + "where restaurant.id = :restaurantId "
                    + "and restaurant.status = com.fooddelivery.entity.enums.RestaurantStatus.APPROVED "
                    + "and item.available = true and item.stockQuantity > 0")
    Page<FoodItem> findPublicOrderableMenuByRestaurantId(@Param("restaurantId") Long restaurantId,
                                                         Pageable pageable);
}
