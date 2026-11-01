package com.fooddelivery.controller;

import com.fooddelivery.dto.common.ApiResponse;
import com.fooddelivery.dto.common.PageResponse;
import com.fooddelivery.dto.menu.FoodCategoryResponse;
import com.fooddelivery.dto.menu.FoodItemAvailabilityRequest;
import com.fooddelivery.dto.menu.FoodItemCreateRequest;
import com.fooddelivery.dto.menu.FoodItemResponse;
import com.fooddelivery.dto.menu.FoodItemStockRequest;
import com.fooddelivery.dto.menu.FoodItemUpdateRequest;
import com.fooddelivery.security.AuthenticatedUser;
import com.fooddelivery.service.menu.MenuService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MenuController {
    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @PostMapping("/api/v1/restaurants/me/menu")
    @PreAuthorize("hasRole('RESTAURANT')")
    public ResponseEntity<ApiResponse<FoodItemResponse>> createItem(
            @Valid @RequestBody FoodItemCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Food item created successfully", menuService.createItem(request, principal)));
    }

    @GetMapping("/api/v1/restaurants/me/menu")
    @PreAuthorize("hasRole('RESTAURANT')")
    public ResponseEntity<ApiResponse<PageResponse<FoodItemResponse>>> listMyItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(ApiResponse.ok("Menu retrieved successfully",
                menuService.listMyItems(page, size, principal)));
    }

    @PatchMapping("/api/v1/restaurants/me/menu/{itemId}")
    @PreAuthorize("hasRole('RESTAURANT')")
    public ResponseEntity<ApiResponse<FoodItemResponse>> updateItem(
            @PathVariable Long itemId,
            @Valid @RequestBody FoodItemUpdateRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(ApiResponse.ok("Food item updated successfully",
                menuService.updateItem(itemId, request, principal)));
    }

    @DeleteMapping("/api/v1/restaurants/me/menu/{itemId}")
    @PreAuthorize("hasRole('RESTAURANT')")
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            @PathVariable Long itemId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        menuService.deleteItem(itemId, principal);
        return ResponseEntity.ok(ApiResponse.ok("Food item deleted successfully"));
    }

    @PatchMapping("/api/v1/restaurants/me/menu/{itemId}/stock")
    @PreAuthorize("hasRole('RESTAURANT')")
    public ResponseEntity<ApiResponse<FoodItemResponse>> updateStock(
            @PathVariable Long itemId,
            @Valid @RequestBody FoodItemStockRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(ApiResponse.ok("Food item stock updated successfully",
                menuService.updateStock(itemId, request, principal)));
    }

    @PatchMapping("/api/v1/restaurants/me/menu/{itemId}/availability")
    @PreAuthorize("hasRole('RESTAURANT')")
    public ResponseEntity<ApiResponse<FoodItemResponse>> updateAvailability(
            @PathVariable Long itemId,
            @Valid @RequestBody FoodItemAvailabilityRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(ApiResponse.ok("Food item availability updated successfully",
                menuService.updateAvailability(itemId, request, principal)));
    }

    @GetMapping("/api/v1/menu/categories")
    public ResponseEntity<ApiResponse<List<FoodCategoryResponse>>> listCategories() {
        return ResponseEntity.ok(ApiResponse.ok("Food categories retrieved successfully",
                menuService.listCategories()));
    }
}
