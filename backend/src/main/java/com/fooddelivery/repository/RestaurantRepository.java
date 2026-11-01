package com.fooddelivery.repository;

import com.fooddelivery.entity.Restaurant;
import com.fooddelivery.entity.enums.RestaurantStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long>, RestaurantCatalogRepository {
    Optional<Restaurant> findByOwner_Id(Long ownerId);
    Page<Restaurant> findByStatus(RestaurantStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Restaurant r where r.id = :restaurantId")
    Optional<Restaurant> findByIdForUpdate(@Param("restaurantId") Long restaurantId);

    @EntityGraph(attributePaths = "location")
    @Query("select restaurant from Restaurant restaurant where restaurant.id = :restaurantId "
            + "and restaurant.status = com.fooddelivery.entity.enums.RestaurantStatus.APPROVED")
    Optional<Restaurant> findApprovedById(@Param("restaurantId") Long restaurantId);
}
