package com.fooddelivery.mapper;

import com.fooddelivery.dto.restaurant.RestaurantCreateRequest;
import com.fooddelivery.dto.restaurant.RestaurantResponse;
import com.fooddelivery.dto.restaurant.RestaurantSummaryResponse;
import com.fooddelivery.entity.Location;
import com.fooddelivery.entity.Restaurant;
import com.fooddelivery.entity.User;

public final class RestaurantMapper {
    private RestaurantMapper() { }

    public static RestaurantResponse toResponse(Restaurant restaurant) {
        if (restaurant == null) return null;
        return new RestaurantResponse(restaurant.getId(), restaurant.getName(), restaurant.getDescription(),
                restaurant.getPhone(), restaurant.getAddressLine(), restaurant.getCity(),
                restaurant.getLocation() == null ? null : restaurant.getLocation().getId(), restaurant.getCuisine(),
                restaurant.getStatus(), restaurant.getOpeningTime(), restaurant.getClosingTime(),
                restaurant.getRatingAverage(), restaurant.getRatingCount());
    }

    public static RestaurantSummaryResponse toSummaryResponse(Restaurant restaurant) {
        if (restaurant == null) return null;
        return new RestaurantSummaryResponse(restaurant.getId(), restaurant.getName(), restaurant.getCuisine(),
                restaurant.getCity(), restaurant.getStatus(), restaurant.getRatingAverage(), restaurant.getRatingCount());
    }

    public static Restaurant toEntity(RestaurantCreateRequest request, User owner, Location location) {
        if (request == null) return null;
        return Restaurant.createProfile(owner, location, request.name(), request.description(), request.phone(),
                request.addressLine(), request.city(), request.cuisine(), request.openingTime(), request.closingTime());
    }
}
