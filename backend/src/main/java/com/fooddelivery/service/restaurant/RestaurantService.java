
package com.fooddelivery.service.restaurant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.hibernate.exception.ConstraintViolationException;

import com.fooddelivery.dto.restaurant.RestaurantCreateRequest;
import com.fooddelivery.dto.restaurant.RestaurantResponse;
import com.fooddelivery.entity.Location;
import com.fooddelivery.entity.Restaurant;
import com.fooddelivery.entity.User;
import com.fooddelivery.entity.enums.UserStatus;
import com.fooddelivery.exception.ApiException;
import com.fooddelivery.mapper.RestaurantMapper;
import com.fooddelivery.repository.LocationRepository;
import com.fooddelivery.repository.RestaurantRepository;
import com.fooddelivery.repository.UserRepository;
import com.fooddelivery.security.AuthenticatedUser;

@Service
public class RestaurantService {
    private static final String OWNER_UNIQUE_CONSTRAINT = "uk_restaurants_owner";

    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;

    public RestaurantService(
            RestaurantRepository restaurantRepository,
            UserRepository userRepository,
            LocationRepository locationRepository) {
        this.restaurantRepository = restaurantRepository;
        this.userRepository = userRepository;
        this.locationRepository = locationRepository;
    }

    @Transactional
    public RestaurantResponse createMyRestaurant(
            RestaurantCreateRequest request,
            AuthenticatedUser authenticatedUser) {

        Long ownerId = authenticatedUser.getUserId();

        if (restaurantRepository.findByOwner_Id(ownerId).isPresent()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "RESTAURANT_ALREADY_EXISTS",
                    "You already have a registered restaurant");
        }

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "USER_NOT_FOUND",
                        "Authenticated user was not found"));

        if (owner.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "ACCOUNT_INACTIVE",
                    "Only active accounts can register a restaurant");
        }

        Location location = null;

        if (request.locationId() != null) {
            location = locationRepository.findById(request.locationId())
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.NOT_FOUND,
                            "LOCATION_NOT_FOUND",
                            "Selected location was not found"));
        }

        Restaurant restaurant = RestaurantMapper.toEntity(request, owner, location);

        try {
            Restaurant saved = restaurantRepository.saveAndFlush(restaurant);
            return RestaurantMapper.toResponse(saved);
        } catch (DataIntegrityViolationException | ConstraintViolationException exception) {
            if (violatesOwnerUniqueConstraint(exception)) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "RESTAURANT_ALREADY_EXISTS",
                        "You already have a registered restaurant");
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public RestaurantResponse getMyRestaurant(AuthenticatedUser authenticatedUser) {

        Restaurant restaurant = restaurantRepository.findByOwner_Id(authenticatedUser.getUserId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "RESTAURANT_NOT_FOUND",
                        "You do not have a registered restaurant"));

        return RestaurantMapper.toResponse(restaurant);
    }

    private static boolean violatesOwnerUniqueConstraint(Throwable failure) {
        Throwable cause = failure;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolation
                    && OWNER_UNIQUE_CONSTRAINT.equalsIgnoreCase(constraintViolation.getConstraintName())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
