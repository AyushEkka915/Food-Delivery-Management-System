package com.fooddelivery.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fooddelivery.config.CsrfConfiguration;
import com.fooddelivery.config.RefreshCookieProperties;
import com.fooddelivery.config.SecurityConfig;
import com.fooddelivery.dto.common.PageResponse;
import com.fooddelivery.dto.menu.FoodCategoryResponse;
import com.fooddelivery.dto.menu.FoodItemCreateRequest;
import com.fooddelivery.dto.menu.FoodItemResponse;
import com.fooddelivery.dto.menu.FoodItemUpdateRequest;
import com.fooddelivery.entity.Role;
import com.fooddelivery.entity.User;
import com.fooddelivery.security.ApiAuthenticationEntryPoint;
import com.fooddelivery.security.AuthenticatedUser;
import com.fooddelivery.security.JwtAuthenticationFilter;
import com.fooddelivery.security.JwtService;
import com.fooddelivery.service.menu.MenuService;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
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
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@WebMvcTest(MenuController.class)
@Import({SecurityConfig.class, CsrfConfiguration.class, JwtAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class, MenuControllerTest.CookieTestConfiguration.class})
class MenuControllerTest {
    private static final String CREATE_JSON = """
            {"categoryId":4,"name":"Paneer Curry","description":"A curry","price":12.00,
             "vegetarian":true,"stockQuantity":0,"preparationTimeMinutes":15,"ownerId":999}
            """;

    @Autowired private MockMvc mockMvc;
    @Autowired private CookieCsrfTokenRepository csrfTokenRepository;
    @Autowired private RequestMappingHandlerMapping handlerMapping;
    @MockitoBean private MenuService menuService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    void restaurantManagerCanCreateItemUsingAuthenticatedPrincipal() throws Exception {
        AuthenticatedUser manager = principal(41L, "RESTAURANT");
        when(menuService.createItem(any(FoodItemCreateRequest.class), same(manager))).thenReturn(itemResponse());

        mockMvc.perform(withCsrf(post("/api/v1/restaurants/me/menu").with(user(manager))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_JSON)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.restaurantId").value(7))
                .andExpect(jsonPath("$.data.name").value("Paneer Curry"));

        verify(menuService).createItem(any(FoodItemCreateRequest.class), same(manager));
    }

    @Test
    void managerCanListOwnMenu() throws Exception {
        var page = PageResponse.from(new org.springframework.data.domain.PageImpl<>(List.of(itemResponse())));
        when(menuService.listMyItems(eq(0), eq(20), any(AuthenticatedUser.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/restaurants/me/menu").with(user(principal(41L, "RESTAURANT"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content[0].name").value("Paneer Curry"));
    }

    @Test
    void managerCanUpdateDeleteAndChangeStockAndAvailability() throws Exception {
        AuthenticatedUser manager = principal(41L, "RESTAURANT");
        when(menuService.updateItem(eq(11L), any(FoodItemUpdateRequest.class), same(manager)))
                .thenReturn(itemResponse());
        when(menuService.updateStock(eq(11L), any(), same(manager))).thenReturn(itemResponse());
        when(menuService.updateAvailability(eq(11L), any(), same(manager))).thenReturn(itemResponse());

        mockMvc.perform(withCsrf(patch("/api/v1/restaurants/me/menu/11").with(user(manager))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"price\":13.50}")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(11));
        mockMvc.perform(withCsrf(patch("/api/v1/restaurants/me/menu/11/stock").with(user(manager))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"stockQuantity\":3}")))
                .andExpect(status().isOk());
        mockMvc.perform(withCsrf(patch("/api/v1/restaurants/me/menu/11/availability").with(user(manager))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"available\":false}")))
                .andExpect(status().isOk());
        mockMvc.perform(withCsrf(delete("/api/v1/restaurants/me/menu/11").with(user(manager))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));

        verify(menuService).updateItem(eq(11L), any(FoodItemUpdateRequest.class), same(manager));
        verify(menuService).updateStock(eq(11L), any(), same(manager));
        verify(menuService).updateAvailability(eq(11L), any(), same(manager));
        verify(menuService).deleteItem(11L, manager);
    }

    @Test
    void customerAndDeliveryPartnerCannotAccessManagerMenu() throws Exception {
        for (String role : List.of("CUSTOMER", "DELIVERY_PARTNER")) {
            mockMvc.perform(get("/api/v1/restaurants/me/menu").with(user(principal(41L, role))))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
            mockMvc.perform(withCsrf(post("/api/v1/restaurants/me/menu").with(user(principal(41L, role)))
                            .contentType(MediaType.APPLICATION_JSON).content(CREATE_JSON)))
                    .andExpect(status().isForbidden());
        }
        verify(menuService, never()).createItem(any(), any());
    }

    @Test
    void unauthenticatedManagerRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/restaurants/me/menu")).andExpect(status().isUnauthorized());
        mockMvc.perform(withCsrf(post("/api/v1/restaurants/me/menu")
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_JSON)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void categoryListingIsPublicAndUsesResponseDto() throws Exception {
        when(menuService.listCategories()).thenReturn(List.of(new FoodCategoryResponse(4L, "Main Course", 2)));

        mockMvc.perform(get("/api/v1/menu/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Main Course"));
        verify(menuService).listCategories();
    }

    @Test
    void invalidItemProfileIsRejectedBeforeCallingService() throws Exception {
        AuthenticatedUser manager = principal(41L, "RESTAURANT");
        mockMvc.perform(withCsrf(patch("/api/v1/restaurants/me/menu/11").with(user(manager))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"   \"}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        verify(menuService, never()).updateItem(any(), any(), any());
    }

    @Test
    void itemUpdateHasExactlyOnePatchMappingAndNoPutMapping() {
        var updateMappings = handlerMapping.getHandlerMethods().entrySet().stream()
                .filter(entry -> entry.getValue().getBeanType() == MenuController.class)
                .filter(entry -> entry.getValue().getMethod().getName().equals("updateItem"))
                .toList();

        assertEquals(1, updateMappings.size());
        assertEquals(Set.of("/api/v1/restaurants/me/menu/{itemId}"),
                updateMappings.getFirst().getKey().getPatternValues());
        assertEquals(Set.of(RequestMethod.PATCH), updateMappings.getFirst().getKey().getMethodsCondition().getMethods());
        assertTrue(updateMappings.getFirst().getKey().getMethodsCondition().getMethods()
                .stream().noneMatch(method -> method == RequestMethod.PUT));
    }

    @Test
    void putToItemUpdateReturnsMethodNotAllowedWithStandardApiError() throws Exception {
        AuthenticatedUser manager = principal(41L, "RESTAURANT");
        mockMvc.perform(withCsrf(put("/api/v1/restaurants/me/menu/11").with(user(manager))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"price\":13.50}")))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message").value("The HTTP method is not supported for this endpoint"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
        verify(menuService, never()).updateItem(any(), any(), any());
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
        when(user.getEmail()).thenReturn("manager@example.com");
        when(user.getPasswordHashForAuthentication()).thenReturn("test-hash");
        when(user.getRoles()).thenReturn(Set.of(role));
        return AuthenticatedUser.from(user);
    }

    private static FoodItemResponse itemResponse() {
        return new FoodItemResponse(11L, 7L, 4L, "Main Course", "Paneer Curry", "A curry",
                new BigDecimal("12.00"), true, true, 0, 15, null, new BigDecimal("0.00"), 0);
    }

    @TestConfiguration
    static class CookieTestConfiguration {
        @Bean
        RefreshCookieProperties testRefreshCookieProperties() {
            return new RefreshCookieProperties("refresh_token", true, true, "Strict", "/api/v1/auth");
        }
    }
}
