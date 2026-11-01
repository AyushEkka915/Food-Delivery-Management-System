package com.fooddelivery.repository;

import com.fooddelivery.dto.restaurant.RestaurantCatalogQuery;
import com.fooddelivery.entity.Restaurant;
import org.springframework.data.domain.Page;

/** Custom database-side operations used by the public restaurant catalog. */
public interface RestaurantCatalogRepository {
    Page<Restaurant> searchApproved(RestaurantCatalogQuery query);
}
