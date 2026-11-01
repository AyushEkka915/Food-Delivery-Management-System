package com.fooddelivery.controller;

import com.fooddelivery.dto.common.ApiResponse;
import com.fooddelivery.dto.common.PageResponse;
import com.fooddelivery.dto.restaurant.RestaurantResponse;
import com.fooddelivery.dto.restaurant.RestaurantStatusRequest;
import com.fooddelivery.dto.restaurant.RestaurantSummaryResponse;
import com.fooddelivery.entity.enums.RestaurantStatus;
import com.fooddelivery.security.AuthenticatedUser;
import com.fooddelivery.service.restaurant.AdminRestaurantService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/restaurants")
public class AdminController {
    private final AdminRestaurantService adminRestaurantService;

    public AdminController(AdminRestaurantService adminRestaurantService) {
        this.adminRestaurantService = adminRestaurantService;
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<RestaurantSummaryResponse>>> getPendingRestaurants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok("Pending restaurants retrieved",
                adminRestaurantService.getPendingRestaurants(page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RestaurantResponse>> getRestaurant(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Restaurant retrieved",
                adminRestaurantService.getRestaurant(id)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RestaurantResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody RestaurantStatusRequest request,
            @AuthenticationPrincipal AuthenticatedUser administrator) {
        RestaurantStatus status = request.status();
        return ResponseEntity.ok(ApiResponse.ok("Restaurant status updated",
                adminRestaurantService.updateStatus(id, status, administrator)));
    }
}
