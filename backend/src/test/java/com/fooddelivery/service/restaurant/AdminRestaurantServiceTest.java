package com.fooddelivery.service.restaurant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fooddelivery.dto.common.PageResponse;
import com.fooddelivery.dto.restaurant.RestaurantResponse;
import com.fooddelivery.dto.restaurant.RestaurantSummaryResponse;
import com.fooddelivery.entity.Restaurant;
import com.fooddelivery.entity.User;
import com.fooddelivery.entity.enums.RestaurantStatus;
import com.fooddelivery.exception.ApiException;
import com.fooddelivery.repository.RestaurantRepository;
import com.fooddelivery.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

class AdminRestaurantServiceTest {
    private RestaurantRepository restaurantRepository;
    private AdminRestaurantService service;

    @BeforeEach
    void setUp() {
        restaurantRepository = mock(RestaurantRepository.class);
        service = new AdminRestaurantService(restaurantRepository);
    }

    @Test
    void listsOnlyPendingRestaurantsWithPageMetadata() {
        Restaurant pending = restaurant(51L, RestaurantStatus.PENDING);
        when(restaurantRepository.findByStatus(RestaurantStatus.PENDING, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(pending), PageRequest.of(0, 20), 1));

        PageResponse<RestaurantSummaryResponse> page = service.getPendingRestaurants(0, 20);

        assertEquals(1, page.totalElements());
        assertEquals(20, page.size());
        assertEquals(RestaurantStatus.PENDING, page.content().getFirst().status());
        verify(restaurantRepository).findByStatus(RestaurantStatus.PENDING, PageRequest.of(0, 20));
    }

    @Test
    void retrievesRestaurantDetails() {
        Restaurant restaurant = restaurant(51L, RestaurantStatus.PENDING);
        when(restaurantRepository.findById(51L)).thenReturn(Optional.of(restaurant));

        RestaurantResponse response = service.getRestaurant(51L);

        assertEquals("Restaurant 51", response.name());
        assertEquals(RestaurantStatus.PENDING, response.status());
    }

    @Test
    void approvesPendingRestaurant() {
        Restaurant restaurant = restaurant(51L, RestaurantStatus.PENDING);
        when(restaurantRepository.findByIdForUpdate(51L)).thenReturn(Optional.of(restaurant));

        RestaurantResponse response = service.updateStatus(51L, RestaurantStatus.APPROVED, admin(99L));

        assertEquals(RestaurantStatus.APPROVED, restaurant.getStatus());
        assertEquals(RestaurantStatus.APPROVED, response.status());
        verify(restaurantRepository).findByIdForUpdate(51L);
    }

    @Test
    void suspendsPendingRestaurant() {
        Restaurant restaurant = restaurant(51L, RestaurantStatus.PENDING);
        when(restaurantRepository.findByIdForUpdate(51L)).thenReturn(Optional.of(restaurant));

        service.updateStatus(51L, RestaurantStatus.SUSPENDED, admin(99L));

        assertEquals(RestaurantStatus.SUSPENDED, restaurant.getStatus());
    }

    @Test
    void suspendsApprovedRestaurant() {
        Restaurant restaurant = restaurant(51L, RestaurantStatus.APPROVED);
        when(restaurantRepository.findByIdForUpdate(51L)).thenReturn(Optional.of(restaurant));

        service.updateStatus(51L, RestaurantStatus.SUSPENDED, admin(99L));

        assertEquals(RestaurantStatus.SUSPENDED, restaurant.getStatus());
    }

    @Test
    void rejectsUnsupportedTransitionsWithoutChangingStatus() {
        Restaurant suspended = restaurant(51L, RestaurantStatus.SUSPENDED);
        when(restaurantRepository.findByIdForUpdate(51L)).thenReturn(Optional.of(suspended));

        ApiException reinstatement = assertThrows(ApiException.class,
                () -> service.updateStatus(51L, RestaurantStatus.APPROVED, admin(99L)));

        assertEquals(HttpStatus.CONFLICT, reinstatement.getStatus());
        assertEquals("RESTAURANT_REINSTATEMENT_NOT_SUPPORTED", reinstatement.getCode());
        assertEquals(RestaurantStatus.SUSPENDED, suspended.getStatus());

        Restaurant pending = restaurant(52L, RestaurantStatus.PENDING);
        when(restaurantRepository.findByIdForUpdate(52L)).thenReturn(Optional.of(pending));
        ApiException invalid = assertThrows(ApiException.class,
                () -> service.updateStatus(52L, RestaurantStatus.CLOSED, admin(99L)));

        assertEquals(HttpStatus.CONFLICT, invalid.getStatus());
        assertEquals("INVALID_RESTAURANT_STATUS_TRANSITION", invalid.getCode());
        assertEquals(RestaurantStatus.PENDING, pending.getStatus());
    }

    @Test
    void administratorCannotChangeTheirOwnRestaurant() {
        Restaurant restaurant = restaurant(51L, RestaurantStatus.PENDING);
        when(restaurantRepository.findByIdForUpdate(51L)).thenReturn(Optional.of(restaurant));

        ApiException exception = assertThrows(ApiException.class,
                () -> service.updateStatus(51L, RestaurantStatus.APPROVED, admin(51L)));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals("CANNOT_MANAGE_OWN_RESTAURANT", exception.getCode());
        assertEquals(RestaurantStatus.PENDING, restaurant.getStatus());
    }

    @Test
    void missingRestaurantReturnsNotFoundWithoutChangingAnything() {
        when(restaurantRepository.findByIdForUpdate(404L)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class,
                () -> service.updateStatus(404L, RestaurantStatus.APPROVED, admin(99L)));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("RESTAURANT_NOT_FOUND", exception.getCode());
    }

    @Test
    void rejectsInvalidPagination() {
        ApiException exception = assertThrows(ApiException.class,
                () -> service.getPendingRestaurants(0, 101));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("size"));
    }

    private static Restaurant restaurant(Long ownerId, RestaurantStatus status) {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(ownerId);
        Restaurant restaurant = Restaurant.createProfile(owner, null, "Restaurant " + ownerId, null,
                "+919876543210", "Main Road", "Rewa", "North Indian",
                LocalTime.of(10, 0), LocalTime.of(22, 0));
        if (status == RestaurantStatus.APPROVED) {
            restaurant.transitionStatusTo(RestaurantStatus.APPROVED);
        } else if (status == RestaurantStatus.SUSPENDED) {
            restaurant.transitionStatusTo(RestaurantStatus.SUSPENDED);
        }
        return restaurant;
    }

    private static AuthenticatedUser admin(Long userId) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(user.getEmail()).thenReturn("admin@example.com");
        when(user.getPasswordHashForAuthentication()).thenReturn("internal-test-hash");
        when(user.getRoles()).thenReturn(java.util.Set.of());
        return AuthenticatedUser.from(user);
    }
}
