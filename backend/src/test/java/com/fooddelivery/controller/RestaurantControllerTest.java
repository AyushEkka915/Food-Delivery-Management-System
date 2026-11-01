package com.fooddelivery.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fooddelivery.config.CsrfConfiguration;
import com.fooddelivery.config.RefreshCookieProperties;
import com.fooddelivery.config.SecurityConfig;
import com.fooddelivery.dto.restaurant.RestaurantCreateRequest;
import com.fooddelivery.dto.restaurant.RestaurantResponse;
import com.fooddelivery.entity.Role;
import com.fooddelivery.entity.User;
import com.fooddelivery.entity.enums.RestaurantStatus;
import com.fooddelivery.security.ApiAuthenticationEntryPoint;
import com.fooddelivery.security.AuthenticatedUser;
import com.fooddelivery.security.InvalidAccessTokenException;
import com.fooddelivery.security.JwtAuthenticationFilter;
import com.fooddelivery.security.JwtService;
import com.fooddelivery.service.restaurant.RestaurantService;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(RestaurantController.class)
@Import({SecurityConfig.class, CsrfConfiguration.class, JwtAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class, RestaurantControllerTest.CookieTestConfiguration.class})
class RestaurantControllerTest {
    private static final String CREATE_JSON = """
            {"name":"New Restaurant","description":"A test restaurant","phone":"+919876543210",
             "addressLine":"Main Road","city":"Rewa","cuisine":"North Indian",
             "openingTime":"10:00:00","closingTime":"22:00:00","ownerId":999}
            """;

    @Autowired private MockMvc mockMvc;
    @Autowired private CookieCsrfTokenRepository csrfTokenRepository;
    @MockitoBean private RestaurantService restaurantService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    void restaurantRoleCanRegisterAndPrincipalIdentityOverridesNoClientIdentity() throws Exception {
        AuthenticatedUser principal = principal(41L, "RESTAURANT");
        when(restaurantService.createMyRestaurant(any(RestaurantCreateRequest.class), same(principal)))
                .thenReturn(response());

        mockMvc.perform(withCsrf(post("/api/v1/restaurants")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_JSON)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Restaurant registered successfully"))
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        verify(restaurantService).createMyRestaurant(any(RestaurantCreateRequest.class), same(principal));
    }

    @ParameterizedTest
    @ValueSource(strings = {"CUSTOMER", "DELIVERY_PARTNER"})
    void nonRestaurantRolesCannotRegister(String roleName) throws Exception {
        AuthenticatedUser principal = principal(41L, roleName);

        mockMvc.perform(withCsrf(post("/api/v1/restaurants")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_JSON)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        verify(restaurantService, never()).createMyRestaurant(any(), any());
    }

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        mockMvc.perform(withCsrf(post("/api/v1/restaurants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_JSON)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/restaurants/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void restaurantRoleCanReadOwnProfile() throws Exception {
        AuthenticatedUser principal = principal(41L, "RESTAURANT");
        when(restaurantService.getMyRestaurant(same(principal))).thenReturn(response());

        mockMvc.perform(get("/api/v1/restaurants/me").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(7));

        verify(restaurantService).getMyRestaurant(same(principal));
    }

    @Test
    void getMyProfileIsRestrictedToRestaurantRole() throws Exception {
        AuthenticatedUser principal = principal(41L, "CUSTOMER");

        mockMvc.perform(get("/api/v1/restaurants/me").with(user(principal)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        verify(restaurantService, never()).getMyRestaurant(any());
    }

    private MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder request) {
        MockHttpServletRequest csrfRequest = new MockHttpServletRequest();
        MockHttpServletResponse csrfResponse = new MockHttpServletResponse();
        CsrfToken token = csrfTokenRepository.generateToken(csrfRequest);
        csrfTokenRepository.saveToken(token, csrfRequest, csrfResponse);
        Cookie cookie = csrfResponse.getCookie("XSRF-TOKEN");
        assertNotNull(cookie);
        return request.cookie(cookie).header(token.getHeaderName(), token.getToken());
    }

    private static AuthenticatedUser principal(Long userId, String roleName) {
        Role role = org.mockito.Mockito.mock(Role.class);
        when(role.getName()).thenReturn(roleName);
        User user = org.mockito.Mockito.mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(user.getEmail()).thenReturn("owner@example.com");
        when(user.getPasswordHashForAuthentication()).thenReturn("internal-test-hash");
        when(user.getRoles()).thenReturn(Set.of(role));
        return AuthenticatedUser.from(user);
    }

    private static RestaurantResponse response() {
        return new RestaurantResponse(7L, "New Restaurant", "A test restaurant", "+919876543210",
                "Main Road", "Rewa", null, "North Indian", RestaurantStatus.PENDING,
                LocalTime.of(10, 0), LocalTime.of(22, 0), new BigDecimal("0.00"), 0);
    }

    @TestConfiguration
    static class CookieTestConfiguration {
        @Bean
        RefreshCookieProperties testRefreshCookieProperties() {
            return new RefreshCookieProperties("refresh_token", true, true, "Strict", "/api/v1/auth");
        }
    }
}
