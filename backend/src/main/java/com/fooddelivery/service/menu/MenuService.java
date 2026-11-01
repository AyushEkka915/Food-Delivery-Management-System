package com.fooddelivery.service.menu;

import com.fooddelivery.dto.common.PageResponse;
import com.fooddelivery.dto.menu.FoodCategoryResponse;
import com.fooddelivery.dto.menu.FoodItemAvailabilityRequest;
import com.fooddelivery.dto.menu.FoodItemCreateRequest;
import com.fooddelivery.dto.menu.FoodItemResponse;
import com.fooddelivery.dto.menu.FoodItemStockRequest;
import com.fooddelivery.dto.menu.FoodItemUpdateRequest;
import com.fooddelivery.entity.FoodCategory;
import com.fooddelivery.entity.FoodItem;
import com.fooddelivery.entity.Restaurant;
import com.fooddelivery.exception.ApiException;
import com.fooddelivery.mapper.FoodCategoryMapper;
import com.fooddelivery.mapper.FoodItemMapper;
import com.fooddelivery.repository.FoodCategoryRepository;
import com.fooddelivery.repository.FoodItemRepository;
import com.fooddelivery.repository.RestaurantRepository;
import com.fooddelivery.security.AuthenticatedUser;
import java.util.List;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MenuService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final String ITEM_NAME_CONSTRAINT = "uk_food_items_restaurant_name";

    private final FoodItemRepository foodItemRepository;
    private final FoodCategoryRepository foodCategoryRepository;
    private final RestaurantRepository restaurantRepository;

    public MenuService(FoodItemRepository foodItemRepository,
                       FoodCategoryRepository foodCategoryRepository,
                       RestaurantRepository restaurantRepository) {
        this.foodItemRepository = foodItemRepository;
        this.foodCategoryRepository = foodCategoryRepository;
        this.restaurantRepository = restaurantRepository;
    }

    @Transactional
    public FoodItemResponse createItem(FoodItemCreateRequest request, AuthenticatedUser principal) {
        Restaurant restaurant = findOwnedRestaurant(principal);
        FoodCategory category = findCategory(request.categoryId());
        FoodItem item = FoodItem.create(restaurant, category, request.name(), request.description(),
                request.price(), request.vegetarian(), request.stockQuantity(),
                request.preparationTimeMinutes(), request.imageUrl());
        try {
            return FoodItemMapper.toResponse(foodItemRepository.saveAndFlush(item));
        } catch (DataIntegrityViolationException | ConstraintViolationException exception) {
            if (violatesItemNameConstraint(exception)) {
                throw itemNameConflict(exception);
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<FoodItemResponse> listMyItems(int page, int size, AuthenticatedUser principal) {
        validatePage(page, size);
        Restaurant restaurant = findOwnedRestaurant(principal);
        return PageResponse.from(foodItemRepository.findByRestaurant_Id(restaurant.getId(), PageRequest.of(page, size))
                .map(FoodItemMapper::toResponse));
    }

    @Transactional
    public FoodItemResponse updateItem(Long itemId, FoodItemUpdateRequest request, AuthenticatedUser principal) {
        Restaurant restaurant = findOwnedRestaurant(principal);
        FoodItem item = findLockedOwnedItem(itemId, restaurant);
        FoodCategory category = request.categoryId().map(this::findCategory).orElse(item.getCategory());
        item.updateProfile(request.name().orElse(item.getName()),
                request.description().orElse(item.getDescription()),
                request.price().orElse(item.getPrice()),
                request.vegetarian().orElse(item.isVegetarian()),
                request.preparationTimeMinutes().orElse(item.getPreparationTimeMinutes()),
                request.imageUrl().orElse(item.getImageUrl()));
        item.changeCategory(category);
        try {
            return FoodItemMapper.toResponse(foodItemRepository.saveAndFlush(item));
        } catch (DataIntegrityViolationException | ConstraintViolationException exception) {
            if (violatesItemNameConstraint(exception)) {
                throw itemNameConflict(exception);
            }
            throw exception;
        }
    }

    @Transactional
    public void deleteItem(Long itemId, AuthenticatedUser principal) {
        Restaurant restaurant = findOwnedRestaurant(principal);
        foodItemRepository.delete(findLockedOwnedItem(itemId, restaurant));
    }

    @Transactional
    public FoodItemResponse updateStock(Long itemId, FoodItemStockRequest request, AuthenticatedUser principal) {
        Restaurant restaurant = findOwnedRestaurant(principal);
        FoodItem item = findLockedOwnedItem(itemId, restaurant);
        item.updateStockQuantity(request.stockQuantity());
        return FoodItemMapper.toResponse(item);
    }

    @Transactional
    public FoodItemResponse updateAvailability(Long itemId, FoodItemAvailabilityRequest request,
                                              AuthenticatedUser principal) {
        Restaurant restaurant = findOwnedRestaurant(principal);
        FoodItem item = findLockedOwnedItem(itemId, restaurant);
        item.changeAvailability(request.available());
        return FoodItemMapper.toResponse(item);
    }

    @Transactional(readOnly = true)
    public List<FoodCategoryResponse> listCategories() {
        return foodCategoryRepository.findAllByOrderByDisplayOrderAscNameAsc().stream()
                .map(FoodCategoryMapper::toResponse)
                .toList();
    }

    private Restaurant findOwnedRestaurant(AuthenticatedUser principal) {
        return restaurantRepository.findByOwner_Id(principal.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RESTAURANT_NOT_FOUND",
                        "You do not have a registered restaurant"));
    }

    private FoodItem findLockedOwnedItem(Long itemId, Restaurant restaurant) {
        return foodItemRepository.findByIdAndRestaurant_IdForUpdate(itemId, restaurant.getId())
                .orElseThrow(() -> itemNotFound());
    }

    private FoodCategory findCategory(Long categoryId) {
        return foodCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "FOOD_CATEGORY_NOT_FOUND",
                        "Food category was not found"));
    }

    private static ApiException itemNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "FOOD_ITEM_NOT_FOUND",
                "Food item was not found");
    }

    private static ApiException itemNameConflict(Throwable cause) {
        return new ApiException(HttpStatus.CONFLICT, "FOOD_ITEM_NAME_ALREADY_EXISTS",
                "A food item with this name already exists in your menu");
    }

    private static boolean violatesItemNameConstraint(Throwable failure) {
        Throwable cause = failure;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation
                    && ITEM_NAME_CONSTRAINT.equalsIgnoreCase(violation.getConstraintName())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private static void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PAGINATION",
                    "Page must be nonnegative and size must be between 1 and 100");
        }
    }
}
