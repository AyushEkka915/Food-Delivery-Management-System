package com.fooddelivery.service.restaurant;

import com.fooddelivery.dto.common.PageResponse;
import com.fooddelivery.dto.restaurant.RestaurantResponse;
import com.fooddelivery.dto.restaurant.RestaurantSummaryResponse;
import com.fooddelivery.entity.Restaurant;
import com.fooddelivery.entity.enums.RestaurantStatus;
import com.fooddelivery.exception.ApiException;
import com.fooddelivery.mapper.RestaurantMapper;
import com.fooddelivery.repository.RestaurantRepository;
import com.fooddelivery.security.AuthenticatedUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminRestaurantService {
    private static final int MAX_PAGE_SIZE = 100;

    private final RestaurantRepository restaurantRepository;

    public AdminRestaurantService(RestaurantRepository restaurantRepository) {
        this.restaurantRepository = restaurantRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<RestaurantSummaryResponse> getPendingRestaurants(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PAGINATION",
                    "Page must be nonnegative and size must be between 1 and 100");
        }
        return PageResponse.from(restaurantRepository.findByStatus(
                RestaurantStatus.PENDING, PageRequest.of(page, size))
                .map(RestaurantMapper::toSummaryResponse));
    }

    @Transactional(readOnly = true)
    public RestaurantResponse getRestaurant(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> restaurantNotFound(restaurantId));
        return RestaurantMapper.toResponse(restaurant);
    }

    @Transactional
    public RestaurantResponse updateStatus(Long restaurantId, RestaurantStatus nextStatus,
                                           AuthenticatedUser administrator) {
        Restaurant restaurant = restaurantRepository.findByIdForUpdate(restaurantId)
                .orElseThrow(() -> restaurantNotFound(restaurantId));

        Long ownerId = restaurant.getOwner().getId();
        if (administrator.getUserId().equals(ownerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "CANNOT_MANAGE_OWN_RESTAURANT",
                    "Administrators cannot change the status of their own restaurant");
        }

        RestaurantStatus currentStatus = restaurant.getStatus();
        if (currentStatus == RestaurantStatus.SUSPENDED && nextStatus == RestaurantStatus.APPROVED) {
            throw new ApiException(HttpStatus.CONFLICT, "RESTAURANT_REINSTATEMENT_NOT_SUPPORTED",
                    "A suspended restaurant cannot be reinstated through this endpoint");
        }
        if (!restaurant.canTransitionStatusTo(nextStatus)) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_RESTAURANT_STATUS_TRANSITION",
                    "The requested restaurant status transition is not allowed");
        }

        restaurant.transitionStatusTo(nextStatus);
        return RestaurantMapper.toResponse(restaurant);
    }

    private static ApiException restaurantNotFound(Long restaurantId) {
        return new ApiException(HttpStatus.NOT_FOUND, "RESTAURANT_NOT_FOUND",
                "Restaurant " + restaurantId + " was not found");
    }
}
