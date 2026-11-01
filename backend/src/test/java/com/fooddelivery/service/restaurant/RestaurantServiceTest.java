
package com.fooddelivery.service.restaurant;

import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.hibernate.exception.ConstraintViolationException;
import java.sql.SQLException;

import com.fooddelivery.dto.restaurant.RestaurantCreateRequest;
import com.fooddelivery.dto.restaurant.RestaurantResponse;
import com.fooddelivery.entity.Restaurant;
import com.fooddelivery.entity.User;
import com.fooddelivery.entity.enums.RestaurantStatus;
import com.fooddelivery.entity.enums.UserStatus;
import com.fooddelivery.exception.ApiException;
import com.fooddelivery.repository.LocationRepository;
import com.fooddelivery.repository.RestaurantRepository;
import com.fooddelivery.repository.UserRepository;
import com.fooddelivery.security.AuthenticatedUser;

class RestaurantServiceTest {

    private RestaurantRepository restaurantRepository;
    private UserRepository userRepository;
    private LocationRepository locationRepository;
    private RestaurantService service;

    @BeforeEach
    void setUp() {
        restaurantRepository = mock(RestaurantRepository.class);
        userRepository = mock(UserRepository.class);
        locationRepository = mock(LocationRepository.class);

        service = new RestaurantService(
                restaurantRepository,
                userRepository,
                locationRepository);
    }

    @Test
    void createsRestaurantForAuthenticatedOwnerWithPendingStatus() {
        AuthenticatedUser principal = principal(8L);
        User owner = activeOwner();

        when(restaurantRepository.findByOwner_Id(8L)).thenReturn(Optional.empty());
        when(userRepository.findById(8L)).thenReturn(Optional.of(owner));
        when(restaurantRepository.saveAndFlush(any(Restaurant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RestaurantResponse response = service.createMyRestaurant(request(null), principal);

        assertEquals("New Restaurant", response.name());
        assertEquals(RestaurantStatus.PENDING, response.status());

        verify(restaurantRepository).saveAndFlush(any(Restaurant.class));
        verify(locationRepository, never()).findById(any());
    }

    @Test
    void duplicateRestaurantReturnsConflictWithoutSaving() {
        AuthenticatedUser principal = principal(8L);
        Restaurant existing = mock(Restaurant.class);

        when(restaurantRepository.findByOwner_Id(8L))
                .thenReturn(Optional.of(existing));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.createMyRestaurant(request(null), principal));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(userRepository, never()).findById(any());
        verify(restaurantRepository, never()).saveAndFlush(any(Restaurant.class));
    }

    @Test
    void nonexistentLocationReturnsNotFoundWithoutSaving() {
        AuthenticatedUser principal = principal(8L);
        User owner = activeOwner();

        when(restaurantRepository.findByOwner_Id(8L)).thenReturn(Optional.empty());
        when(userRepository.findById(8L)).thenReturn(Optional.of(owner));
        when(locationRepository.findById(99L)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.createMyRestaurant(request(99L), principal));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(restaurantRepository, never()).saveAndFlush(any(Restaurant.class));
    }

    @Test
    void missingAuthenticatedUserReturnsNotFoundWithoutSaving() {
        AuthenticatedUser principal = principal(8L);

        when(restaurantRepository.findByOwner_Id(8L))
            .thenReturn(Optional.empty());
        when(userRepository.findById(8L))
            .thenReturn(Optional.empty());

        ApiException exception = assertThrows(
            ApiException.class,
            () -> service.createMyRestaurant(request(null), principal));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());

        verify(locationRepository, never()).findById(any());
        verify(restaurantRepository, never()).saveAndFlush(any(Restaurant.class));
    }

    @Test
    void inactiveOwnerCannotRegisterRestaurant() {
        AuthenticatedUser principal = principal(8L);
        User owner = mock(User.class);
        when(owner.getStatus()).thenReturn(UserStatus.SUSPENDED);
        when(restaurantRepository.findByOwner_Id(8L)).thenReturn(Optional.empty());
        when(userRepository.findById(8L)).thenReturn(Optional.of(owner));

        ApiException exception = assertThrows(ApiException.class,
                () -> service.createMyRestaurant(request(null), principal));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals("ACCOUNT_INACTIVE", exception.getCode());
        verify(locationRepository, never()).findById(any());
        verify(restaurantRepository, never()).saveAndFlush(any(Restaurant.class));
    }

    @Test
    void confirmedOwnerUniqueConstraintViolationMapsToConflict() {
        AuthenticatedUser principal = principal(8L);
        User owner = activeOwner();
        when(restaurantRepository.findByOwner_Id(8L)).thenReturn(Optional.empty());
        when(userRepository.findById(8L)).thenReturn(Optional.of(owner));

        ConstraintViolationException hibernateViolation = new ConstraintViolationException(
                "duplicate owner", new SQLException("duplicate key", "23000"), "uk_restaurants_owner");
        when(restaurantRepository.saveAndFlush(any(Restaurant.class)))
                .thenThrow(new DataIntegrityViolationException("constraint violation", hibernateViolation));

        ApiException exception = assertThrows(ApiException.class,
                () -> service.createMyRestaurant(request(null), principal));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("RESTAURANT_ALREADY_EXISTS", exception.getCode());
    }

    @Test
    void unrelatedIntegrityViolationIsRethrownUnchanged() {
        AuthenticatedUser principal = principal(8L);
        User owner = activeOwner();
        when(restaurantRepository.findByOwner_Id(8L)).thenReturn(Optional.empty());
        when(userRepository.findById(8L)).thenReturn(Optional.of(owner));

        ConstraintViolationException hibernateViolation = new ConstraintViolationException(
                "other constraint", new SQLException("other constraint", "23000"), "uk_restaurants_phone");
        DataIntegrityViolationException integrityViolation =
                new DataIntegrityViolationException("constraint violation", hibernateViolation);
        when(restaurantRepository.saveAndFlush(any(Restaurant.class))).thenThrow(integrityViolation);

        DataIntegrityViolationException actual = assertThrows(DataIntegrityViolationException.class,
                () -> service.createMyRestaurant(request(null), principal));

        assertSame(integrityViolation, actual);
    }

    @Test
    void missingRestaurantProfileReturnsNotFound() {
        AuthenticatedUser principal = principal(8L);

        when(restaurantRepository.findByOwner_Id(8L)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.getMyRestaurant(principal));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    private static RestaurantCreateRequest request(Long locationId) {
        return new RestaurantCreateRequest(
                "New Restaurant",
                "Test description",
                "+919876543210",
                "Main Road",
                "Rewa",
                locationId,
                "North Indian",
                LocalTime.of(10, 0),
                LocalTime.of(22, 0));
    }

    private static AuthenticatedUser principal(Long userId) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(user.getEmail()).thenReturn("owner@example.com");
        when(user.getPasswordHashForAuthentication()).thenReturn("stored-hash");
        when(user.getRoles()).thenReturn(java.util.Set.of());

        return AuthenticatedUser.from(user);
    }

    private static User activeOwner() {
        User owner = mock(User.class);
        when(owner.getStatus()).thenReturn(UserStatus.ACTIVE);
        return owner;
    }
}
