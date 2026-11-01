package com.fooddelivery.service.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fooddelivery.dto.menu.FoodItemAvailabilityRequest;
import com.fooddelivery.dto.menu.FoodItemCreateRequest;
import com.fooddelivery.dto.menu.FoodItemStockRequest;
import com.fooddelivery.dto.menu.FoodItemUpdateRequest;
import com.fooddelivery.entity.FoodCategory;
import com.fooddelivery.entity.FoodItem;
import com.fooddelivery.entity.Restaurant;
import com.fooddelivery.entity.Role;
import com.fooddelivery.entity.User;
import com.fooddelivery.exception.ApiException;
import com.fooddelivery.repository.FoodCategoryRepository;
import com.fooddelivery.repository.FoodItemRepository;
import com.fooddelivery.repository.RestaurantRepository;
import com.fooddelivery.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

class MenuServiceTest {
    private FoodItemRepository itemRepository;
    private FoodCategoryRepository categoryRepository;
    private RestaurantRepository restaurantRepository;
    private MenuService service;
    private final AuthenticatedUser principal = principal(8L);

    @BeforeEach
    void setUp() {
        itemRepository = mock(FoodItemRepository.class);
        categoryRepository = mock(FoodCategoryRepository.class);
        restaurantRepository = mock(RestaurantRepository.class);
        service = new MenuService(itemRepository, categoryRepository, restaurantRepository);
    }

    @Test
    void createsItemUsingAuthenticatedOwnersRestaurantAndCategory() {
        Restaurant restaurant = restaurant(20L);
        FoodCategory category = category(4L, "Main Course");
        when(restaurantRepository.findByOwner_Id(8L)).thenReturn(Optional.of(restaurant));
        when(categoryRepository.findById(4L)).thenReturn(Optional.of(category));
        when(itemRepository.saveAndFlush(any(FoodItem.class))).thenAnswer(call -> call.getArgument(0));

        var response = service.createItem(createRequest(), principal);

        assertEquals(20L, response.restaurantId());
        assertEquals(4L, response.categoryId());
        assertEquals("Paneer Curry", response.name());
        assertEquals(true, response.available());
        assertEquals(0, response.ratingCount());
        verify(restaurantRepository).findByOwner_Id(8L);
        verify(itemRepository).saveAndFlush(any(FoodItem.class));
    }

    @Test
    void listsAllItemsForOnlyTheAuthenticatedOwnersRestaurant() {
        Restaurant restaurant = restaurant(20L);
        FoodItem item = item(restaurant, category(4L, "Main Course"));
        when(restaurantRepository.findByOwner_Id(8L)).thenReturn(Optional.of(restaurant));
        when(itemRepository.findByRestaurant_Id(20L, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1));

        var page = service.listMyItems(0, 20, principal);

        assertEquals(1, page.totalElements());
        assertEquals("Paneer Curry", page.content().getFirst().name());
        verify(itemRepository).findByRestaurant_Id(20L, PageRequest.of(0, 20));
    }

    @Test
    void updatesProfileAndCategoryWithoutReplacingOmittedFields() {
        Restaurant restaurant = restaurant(20L);
        FoodCategory oldCategory = category(4L, "Main Course");
        FoodCategory newCategory = category(5L, "Dessert");
        FoodItem item = item(restaurant, oldCategory);
        when(restaurantRepository.findByOwner_Id(8L)).thenReturn(Optional.of(restaurant));
        when(itemRepository.findByIdAndRestaurant_IdForUpdate(30L, 20L)).thenReturn(Optional.of(item));
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(newCategory));
        when(itemRepository.saveAndFlush(any(FoodItem.class))).thenAnswer(call -> call.getArgument(0));

        var response = service.updateItem(30L, new FoodItemUpdateRequest(Optional.of(5L), Optional.empty(),
                Optional.empty(), Optional.of(new BigDecimal("15.50")), Optional.empty(), Optional.empty(),
                Optional.empty()), principal);

        assertEquals("Paneer Curry", response.name());
        assertEquals("A curry", response.description());
        assertEquals(new BigDecimal("15.50"), response.price());
        assertEquals(5L, response.categoryId());
        assertEquals(6, response.stockQuantity());
        assertEquals(true, response.available());
        verify(itemRepository).findByIdAndRestaurant_IdForUpdate(30L, 20L);
    }

    @Test
    void deletesOwnedItem() {
        Restaurant restaurant = restaurant(20L);
        FoodItem item = item(restaurant, category(4L, "Main Course"));
        when(restaurantRepository.findByOwner_Id(8L)).thenReturn(Optional.of(restaurant));
        when(itemRepository.findByIdAndRestaurant_IdForUpdate(30L, 20L)).thenReturn(Optional.of(item));

        service.deleteItem(30L, principal);

        verify(itemRepository).findByIdAndRestaurant_IdForUpdate(30L, 20L);
        verify(itemRepository).delete(item);
    }

    @Test
    void stockUpdateDoesNotChangeAvailabilityOrPrice() {
        Restaurant restaurant = restaurant(20L);
        FoodItem item = item(restaurant, category(4L, "Main Course"));
        item.changeAvailability(false);
        when(restaurantRepository.findByOwner_Id(8L)).thenReturn(Optional.of(restaurant));
        when(itemRepository.findByIdAndRestaurant_IdForUpdate(30L, 20L)).thenReturn(Optional.of(item));

        var response = service.updateStock(30L, new FoodItemStockRequest(12), principal);

        assertEquals(12, response.stockQuantity());
        assertEquals(false, response.available());
        assertEquals(new BigDecimal("12.00"), response.price());
        verify(itemRepository).findByIdAndRestaurant_IdForUpdate(30L, 20L);
    }

    @Test
    void availabilityUpdateDoesNotChangeStockOrPrice() {
        Restaurant restaurant = restaurant(20L);
        FoodItem item = item(restaurant, category(4L, "Main Course"));
        when(restaurantRepository.findByOwner_Id(8L)).thenReturn(Optional.of(restaurant));
        when(itemRepository.findByIdAndRestaurant_IdForUpdate(30L, 20L)).thenReturn(Optional.of(item));

        var response = service.updateAvailability(30L, new FoodItemAvailabilityRequest(false), principal);

        assertEquals(false, response.available());
        assertEquals(6, response.stockQuantity());
        assertEquals(new BigDecimal("12.00"), response.price());
        verify(itemRepository).findByIdAndRestaurant_IdForUpdate(30L, 20L);
    }

    @Test
    void itemOwnedByAnotherRestaurantReturnsSameNotFoundError() {
        Restaurant ownRestaurant = restaurant(20L);
        when(restaurantRepository.findByOwner_Id(8L)).thenReturn(Optional.of(ownRestaurant));
        when(itemRepository.findByIdAndRestaurant_IdForUpdate(30L, 20L)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class,
                () -> service.updateStock(30L, new FoodItemStockRequest(2), principal));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("FOOD_ITEM_NOT_FOUND", exception.getCode());
        verify(itemRepository).findByIdAndRestaurant_IdForUpdate(30L, 20L);
        verify(itemRepository, never()).delete(any());
    }

    @Test
    void missingRestaurantReturnsEstablishedNotFoundError() {
        when(restaurantRepository.findByOwner_Id(8L)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class,
                () -> service.listMyItems(0, 20, principal));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("RESTAURANT_NOT_FOUND", exception.getCode());
    }

    @Test
    void missingItemAndCategoryReturnNotFound() {
        Restaurant restaurant = restaurant(20L);
        when(restaurantRepository.findByOwner_Id(8L)).thenReturn(Optional.of(restaurant));
        when(itemRepository.findByIdAndRestaurant_IdForUpdate(30L, 20L)).thenReturn(Optional.empty());
        ApiException missingItem = assertThrows(ApiException.class,
                () -> service.updateStock(30L, new FoodItemStockRequest(1), principal));
        assertEquals("FOOD_ITEM_NOT_FOUND", missingItem.getCode());

        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());
        ApiException missingCategory = assertThrows(ApiException.class,
                () -> service.createItem(new FoodItemCreateRequest(99L, "Dish", null,
                        BigDecimal.ONE, true, 0, 10, null), principal));
        assertEquals(HttpStatus.NOT_FOUND, missingCategory.getStatus());
        assertEquals("FOOD_CATEGORY_NOT_FOUND", missingCategory.getCode());
    }

    @Test
    void categoriesUseRepositoryDisplayOrder() {
        List<FoodCategory> categories = List.of(category(1L, "Starter"), category(2L, "Main"));
        when(categoryRepository.findAllByOrderByDisplayOrderAscNameAsc())
                .thenReturn(categories);

        var result = service.listCategories();

        assertEquals(List.of("Starter", "Main"), result.stream().map(response -> response.name()).toList());
    }

    private static FoodItemCreateRequest createRequest() {
        return new FoodItemCreateRequest(4L, "Paneer Curry", "A curry", new BigDecimal("12.00"),
                true, 0, 15, null);
    }

    private static Restaurant restaurant(Long id) {
        Restaurant restaurant = mock(Restaurant.class);
        when(restaurant.getId()).thenReturn(id);
        return restaurant;
    }

    private static FoodCategory category(Long id, String name) {
        FoodCategory category = mock(FoodCategory.class);
        when(category.getId()).thenReturn(id);
        when(category.getName()).thenReturn(name);
        when(category.getDisplayOrder()).thenReturn(1);
        return category;
    }

    private static FoodItem item(Restaurant restaurant, FoodCategory category) {
        return FoodItem.create(restaurant, category, "Paneer Curry", "A curry", new BigDecimal("12.00"),
                true, 6, 15, null);
    }

    private static AuthenticatedUser principal(Long id) {
        Role role = mock(Role.class);
        when(role.getName()).thenReturn("RESTAURANT");
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getEmail()).thenReturn("manager@example.com");
        when(user.getPasswordHashForAuthentication()).thenReturn("test-hash");
        when(user.getRoles()).thenReturn(Set.of(role));
        return AuthenticatedUser.from(user);
    }
}
