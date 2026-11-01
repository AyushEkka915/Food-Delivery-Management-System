package com.fooddelivery.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fooddelivery.config.CsrfConfiguration;
import com.fooddelivery.config.RefreshCookieProperties;
import com.fooddelivery.config.SecurityConfig;
import com.fooddelivery.dto.common.PageResponse;
import com.fooddelivery.dto.restaurant.RestaurantResponse;
import com.fooddelivery.dto.restaurant.RestaurantSummaryResponse;
import com.fooddelivery.entity.Role;
import com.fooddelivery.entity.User;
import com.fooddelivery.entity.enums.RestaurantStatus;
import com.fooddelivery.security.ApiAuthenticationEntryPoint;
import com.fooddelivery.security.AuthenticatedUser;
import com.fooddelivery.security.JwtAuthenticationFilter;
import com.fooddelivery.security.JwtService;
import com.fooddelivery.service.restaurant.AdminRestaurantService;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
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

@WebMvcTest(AdminController.class)
@Import({SecurityConfig.class, CsrfConfiguration.class, JwtAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class, AdminControllerTest.CookieTestConfiguration.class})
class AdminControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private CookieCsrfTokenRepository csrfTokenRepository;
    @MockitoBean private AdminRestaurantService service;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    void adminCanListPendingRestaurants() throws Exception {
        var page = PageResponse.from(new org.springframework.data.domain.PageImpl<>(
                List.of(new RestaurantSummaryResponse(7L, "Spice Route", "Indian", "Rewa",
                        RestaurantStatus.PENDING, new BigDecimal("0.00"), 0))));
        when(service.getPendingRestaurants(0, 20)).thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/restaurants/pending").with(user(principal(90L, "ADMIN"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].name").value("Spice Route"));
    }

    @Test
    void adminCanRetrieveRestaurant() throws Exception {
        when(service.getRestaurant(7L)).thenReturn(response(RestaurantStatus.PENDING));
        mockMvc.perform(get("/api/v1/admin/restaurants/7").with(user(principal(90L, "ADMIN"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(7));
        verify(service).getRestaurant(7L);
    }

    @Test
    void adminCanChangeStatusAndServiceReceivesAuthenticatedPrincipal() throws Exception {
        AuthenticatedUser admin = principal(90L, "ADMIN");
        when(service.updateStatus(eq(7L), eq(RestaurantStatus.APPROVED), eq(admin)))
                .thenReturn(response(RestaurantStatus.APPROVED));
        mockMvc.perform(withCsrf(patch("/api/v1/admin/restaurants/7/status").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"APPROVED\"}")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("APPROVED"));
        verify(service).updateStatus(7L, RestaurantStatus.APPROVED, admin);
    }

    @Test
    void nonAdminsCannotAccessAdministrativeEndpoints() throws Exception {
        for (String role : List.of("RESTAURANT", "CUSTOMER", "DELIVERY_PARTNER")) {
            mockMvc.perform(get("/api/v1/admin/restaurants/pending").with(user(principal(41L, role))))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/admin/restaurants/7").with(user(principal(41L, role))))
                    .andExpect(status().isForbidden());
            mockMvc.perform(withCsrf(patch("/api/v1/admin/restaurants/7/status")
                            .with(user(principal(41L, role))).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"APPROVED\"}")))
                    .andExpect(status().isForbidden());
        }
        verify(service, never()).getPendingRestaurants(any(Integer.class), any(Integer.class));
    }

    @Test
    void unauthenticatedAdminRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/admin/restaurants/pending")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/restaurants/7")).andExpect(status().isUnauthorized());
        mockMvc.perform(withCsrf(patch("/api/v1/admin/restaurants/7/status")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"APPROVED\"}")))
                .andExpect(status().isUnauthorized());
    }

    private MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder request) {
        MockHttpServletRequest csrfRequest = new MockHttpServletRequest();
        MockHttpServletResponse csrfResponse = new MockHttpServletResponse();
        CsrfToken token = csrfTokenRepository.generateToken(csrfRequest);
        csrfTokenRepository.saveToken(token, csrfRequest, csrfResponse);
        Cookie cookie = csrfResponse.getCookie("XSRF-TOKEN");
        return request.cookie(cookie).header(token.getHeaderName(), token.getToken());
    }

    private static AuthenticatedUser principal(Long id, String roleName) {
        Role role = org.mockito.Mockito.mock(Role.class);
        when(role.getName()).thenReturn(roleName);
        User user = org.mockito.Mockito.mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getEmail()).thenReturn("admin@example.com");
        when(user.getPasswordHashForAuthentication()).thenReturn("test-hash");
        when(user.getRoles()).thenReturn(Set.of(role));
        return AuthenticatedUser.from(user);
    }

    private static RestaurantResponse response(RestaurantStatus status) {
        return new RestaurantResponse(7L, "Spice Route", "Indian food", "+919876543210", "Main Road",
                "Rewa", null, "Indian", status, LocalTime.of(10, 0), LocalTime.of(22, 0),
                new BigDecimal("0.00"), 0);
    }

    @TestConfiguration
    static class CookieTestConfiguration {
        @Bean
        RefreshCookieProperties testRefreshCookieProperties() {
            return new RefreshCookieProperties("refresh_token", true, true, "Strict", "/api/v1/auth");
        }
    }
}
