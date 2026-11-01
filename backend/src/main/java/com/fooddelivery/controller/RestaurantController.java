package com.fooddelivery.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fooddelivery.dto.common.ApiResponse;
import com.fooddelivery.dto.restaurant.RestaurantCreateRequest;
import com.fooddelivery.dto.restaurant.RestaurantResponse;
import com.fooddelivery.security.AuthenticatedUser;
import com.fooddelivery.service.restaurant.RestaurantService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/restaurants")
public class RestaurantController {

    private final RestaurantService restaurantService;

    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @PostMapping
    @PreAuthorize("hasRole('RESTAURANT')")
    public ResponseEntity<ApiResponse<RestaurantResponse>> createMyRestaurant(
            @Valid @RequestBody RestaurantCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {

        RestaurantResponse response =
                restaurantService.createMyRestaurant(request, authenticatedUser);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Restaurant registered successfully", response));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('RESTAURANT')")
    public ResponseEntity<ApiResponse<RestaurantResponse>> getMyRestaurant(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {

        RestaurantResponse response =
                restaurantService.getMyRestaurant(authenticatedUser);

        return ResponseEntity.ok(
                ApiResponse.ok("Restaurant profile retrieved successfully", response));
    }
}
