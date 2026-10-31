package com.fooddelivery.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.config.JwtProperties;
import com.fooddelivery.entity.Role;
import com.fooddelivery.entity.User;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockFilterChain;

class JwtAuthenticationFilterTest {
    private JwtService jwtService;
    private JwtAuthenticationFilter filter;
    private AuthenticatedUser user;

    @BeforeEach
    void setUp() {
        byte[] key = new byte[32];
        java.util.Arrays.fill(key, (byte) 7);
        jwtService = new JwtService(new JwtProperties(java.util.Base64.getEncoder().encodeToString(key), 15, 7),
                Clock.fixed(java.time.Instant.parse("2026-02-03T04:05:06Z"), ZoneOffset.UTC));
        filter = new JwtAuthenticationFilter(jwtService,
                new ApiAuthenticationEntryPoint(new ObjectMapper().findAndRegisterModules()));

        User entity = mock(User.class);
        Role role = mock(Role.class);
        when(entity.getId()).thenReturn(9L);
        when(entity.getEmail()).thenReturn("delivery@example.com");
        when(entity.getPasswordHashForAuthentication()).thenReturn("unused-test-hash");
        when(entity.getRoles()).thenReturn(Set.of(role));
        when(role.getName()).thenReturn("DELIVERY_PARTNER");
        user = AuthenticatedUser.from(entity);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validBearerTokenSetsAuthenticatedPrincipalAndContinuesChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/private");
        request.addHeader("Authorization", "Bearer " + jwtService.generateAccessToken(user));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertTrue(authentication.isAuthenticated());
        assertInstanceOf(AuthenticatedUser.class, authentication.getPrincipal());
        assertEquals(9L, ((AuthenticatedUser) authentication.getPrincipal()).getUserId());
        assertEquals("ROLE_DELIVERY_PARTNER", authentication.getAuthorities().iterator().next().getAuthority());
        assertInstanceOf(org.springframework.security.web.authentication.WebAuthenticationDetails.class,
                authentication.getDetails());
        assertSame(request, chain.getRequest());
    }

    @Test
    void missingBearerTokenLeavesRequestUnauthenticatedForSecurityEntryPoint() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/private");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertSame(request, chain.getRequest());
    }

    @Test
    void invalidBearerTokenReturnsGenericUnauthorizedAndStopsChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/private");
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Authentication is required"));
        assertNull(chain.getRequest());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void emptyBearerValueIsRejected() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/private");
        request.addHeader("Authorization", "Bearer ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
    }
}
