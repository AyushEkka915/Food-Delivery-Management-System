package com.fooddelivery.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fooddelivery.config.SecurityConfig;
import com.fooddelivery.dto.auth.CustomerResponse;
import com.fooddelivery.dto.auth.LoginResponse;
import com.fooddelivery.exception.ApiException;
import com.fooddelivery.config.RefreshTokenCookieFactory;
import com.fooddelivery.config.RefreshCookieProperties;
import com.fooddelivery.config.CsrfConfiguration;
import com.fooddelivery.security.ApiAuthenticationEntryPoint;
import com.fooddelivery.security.InvalidAccessTokenException;
import com.fooddelivery.security.JwtAuthenticationFilter;
import com.fooddelivery.security.JwtService;
import com.fooddelivery.service.auth.AuthenticationService;
import com.fooddelivery.service.auth.LoginSession;
import com.fooddelivery.service.auth.RefreshAttempt;
import com.fooddelivery.service.auth.RefreshOutcome;
import com.fooddelivery.service.auth.RefreshTokenLifecycleService;
import java.time.LocalDateTime;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(AuthenticationController.class)
@Import({SecurityConfig.class, CsrfConfiguration.class, JwtAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class, AuthenticationControllerSecurityTest.CookieTestConfiguration.class})
class AuthenticationControllerSecurityTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private AuthenticationService authenticationService;
    @MockitoBean private RefreshTokenCookieFactory refreshTokenCookieFactory;
    @MockitoBean private RefreshTokenLifecycleService refreshTokenLifecycleService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;


    @TestConfiguration
    static class CookieTestConfiguration {
        @Bean
        RefreshCookieProperties testRefreshCookieProperties() {
            return new RefreshCookieProperties("refresh_token", true, true, "Strict", "/api/v1/auth");
        }
    }

    @Test
    void registrationIsPublicAndUnknownPrivilegeFieldsAreNotBound() throws Exception {
        when(authenticationService.registerCustomer(any())).thenReturn(
                new CustomerResponse(91L, "Customer", "customer@example.com", "+919000000099",
                        LocalDateTime.parse("2026-05-01T12:00:00")));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Customer","email":"customer@example.com",
                                 "phone":"+919000000099","password":"secretpass123",
                                 "role":"ADMIN","status":"ACTIVE","userId":1}
                                """))
                .andExpect(status().isCreated());

        verify(authenticationService).registerCustomer(any());
    }

    @Test
    void loginIsPublic() throws Exception {
        String rawRefreshToken = "raw-refresh-token-not-in-json";
        when(authenticationService.login(any())).thenReturn(
                new LoginSession(new LoginResponse("access-token-visible", "Bearer", 900), rawRefreshToken));
        when(refreshTokenCookieFactory.create(rawRefreshToken)).thenReturn(ResponseCookie.from(
                        "refresh_token", rawRefreshToken)
                .httpOnly(true).secure(true).sameSite("Strict").path("/api/v1/auth")
                .maxAge(Duration.ofDays(7)).build());

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"customer@example.com\",\"password\":\"secretpass123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access-token-visible"))
                .andExpect(content().string(not(containsString(rawRefreshToken))))
                .andReturn();
        String refreshSetCookie = result.getResponse().getHeaders("Set-Cookie").stream()
                .filter(value -> value.startsWith("refresh_token="))
                .findFirst().orElse("");
        assertTrue(refreshSetCookie.contains("refresh_token=" + rawRefreshToken));
        assertTrue(refreshSetCookie.contains("HttpOnly"));
        assertTrue(refreshSetCookie.contains("Secure"));
        assertTrue(refreshSetCookie.contains("SameSite=Strict"));
        assertTrue(refreshSetCookie.contains("Path=/api/v1/auth"));
        assertTrue(refreshSetCookie.contains("Max-Age=604800"));
    }

    @Test
    void failedLoginDoesNotSetRefreshCookie() throws Exception {
        when(authenticationService.login(any())).thenThrow(new ApiException(
                HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password"));

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"customer@example.com\",\"password\":\"bad-password\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn();
        assertFalse(result.getResponse().getHeaders("Set-Cookie").stream()
                .anyMatch(value -> value.startsWith("refresh_token=")));
    }

    @Test
    void invalidRegistrationIsRejectedByValidation() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"invalid\",\"phone\":\"\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).registerCustomer(any());
    }

    @Test
    void otherRoutesStillRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/restaurants/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void invalidBearerTokenRemainsRejected() throws Exception {
        when(jwtService.validateAccessToken("bogus-token")).thenThrow(new InvalidAccessTokenException());

        mockMvc.perform(get("/api/v1/restaurants")
                        .header("Authorization", "Bearer bogus-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshReadsConfiguredCookieAndReturnsReplacementCookieWithoutRawTokenInJson() throws Exception {
        String oldRawToken = "old-refresh-secret";
        String replacementRawToken = "new-refresh-secret";
        when(refreshTokenLifecycleService.rotate(oldRawToken)).thenReturn(new RefreshAttempt(
                RefreshOutcome.ROTATED,
                new LoginSession(new LoginResponse("new-access-token", "Bearer", 900), replacementRawToken)));
        when(refreshTokenCookieFactory.create(replacementRawToken)).thenReturn(ResponseCookie.from(
                        "refresh_token", replacementRawToken)
                .httpOnly(true).secure(true).sameSite("Strict").path("/api/v1/auth")
                .maxAge(Duration.ofDays(7)).build());

        TestCsrf csrf = csrfFromBootstrap();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", oldRawToken), csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
                .andExpect(content().string(not(containsString(oldRawToken))))
                .andExpect(content().string(not(containsString(replacementRawToken))))
                .andExpect(header().string("Set-Cookie", containsString("refresh_token=" + replacementRawToken)));

        verify(refreshTokenLifecycleService).rotate(oldRawToken);
    }

    @Test
    void refreshFailureUsesGenericResponseAndClearsCookie() throws Exception {
        when(refreshTokenLifecycleService.rotate("unknown-secret"))
                .thenReturn(new RefreshAttempt(RefreshOutcome.REPLAY_DETECTED, null));
        when(refreshTokenCookieFactory.clear()).thenReturn(ResponseCookie.from("refresh_token", "")
                .httpOnly(true).secure(true).sameSite("Strict").path("/api/v1/auth")
                .maxAge(Duration.ZERO).build());

        TestCsrf csrf = csrfFromBootstrap();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "unknown-secret"), csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"))
                .andExpect(jsonPath("$.message").value("Authentication is required"))
                .andExpect(content().string(not(containsString("unknown-secret"))))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));
    }

    @Test
    void refreshRequiresCsrfEvenWhenBearerHeaderIsPresent() throws Exception {

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("Authorization", "Bearer syntactically-valid-access-token"))
                .andExpect(status().isForbidden());

        verify(refreshTokenLifecycleService, never()).rotate(any());
    }

    @Test
    void missingRefreshCookieIsRejectedAndCleared() throws Exception {
        when(refreshTokenLifecycleService.rotate(null)).thenReturn(new RefreshAttempt(RefreshOutcome.REJECTED, null));
        when(refreshTokenCookieFactory.clear()).thenReturn(ResponseCookie.from("refresh_token", "")
                .httpOnly(true).secure(true).sameSite("Strict").path("/api/v1/auth")
                .maxAge(Duration.ZERO).build());

        TestCsrf csrf = csrfFromBootstrap();
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .param("refresh_token", "query-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"body-token\"}")
                        .cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(not(containsString("query-token"))))
                .andExpect(content().string(not(containsString("body-token"))));

        verify(refreshTokenLifecycleService).rotate(null);
    }

    @Test
    void csrfBootstrapReturnsTheReadableCookieTokenAndExpectedHeader() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.headerName").value("X-XSRF-TOKEN"))
                .andExpect(header().string("Set-Cookie", containsString("XSRF-TOKEN=")))
                .andExpect(header().string("Set-Cookie", containsString("Secure")))
                .andExpect(header().string("Set-Cookie", containsString("Path=/api/v1/auth")))
                .andReturn();

        String responseToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("token").asText();
        jakarta.servlet.http.Cookie csrfCookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(csrfCookie);
        org.junit.jupiter.api.Assertions.assertEquals(responseToken, csrfCookie.getValue());
        assertFalse(csrfCookie.isHttpOnly());
        assertTrue(csrfCookie.getSecure());
        org.junit.jupiter.api.Assertions.assertEquals("Strict", csrfCookie.getAttribute("SameSite"));
        org.junit.jupiter.api.Assertions.assertEquals("/api/v1/auth", csrfCookie.getPath());
    }

    @Test
    void logoutRevokesPresentedCookieAndClearsItWithMatchingAttributes() throws Exception {
        when(refreshTokenCookieFactory.clear()).thenReturn(ResponseCookie.from("refresh_token", "")
                .httpOnly(true).secure(true).sameSite("Strict").path("/api/v1/auth")
                .maxAge(Duration.ZERO).build());

        TestCsrf csrf = csrfFromBootstrap();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "logout-secret"),
                                csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout successful"))
                .andExpect(content().string(not(containsString("logout-secret"))))
                .andReturn();
        String clearCookie = result.getResponse().getHeaders("Set-Cookie").stream()
                .filter(value -> value.startsWith("refresh_token="))
                .findFirst().orElse("");
        assertTrue(clearCookie.contains("HttpOnly"));
        assertTrue(clearCookie.contains("Secure"));
        assertTrue(clearCookie.contains("SameSite=Strict"));
        assertTrue(clearCookie.contains("Path=/api/v1/auth"));
        assertTrue(clearCookie.contains("Max-Age=0"));

        verify(refreshTokenLifecycleService).logout("logout-secret");
    }

    @Test
    void logoutRequiresCsrfEvenWhenBearerHeaderIsPresent() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer syntactically-valid-access-token"))
                .andExpect(status().isForbidden());

        verify(refreshTokenLifecycleService, never()).logout(any());
    }

    private TestCsrf csrfFromBootstrap() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf")).andReturn();
        String token = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("token").asText();
        return new TestCsrf(result.getResponse().getCookie("XSRF-TOKEN"), token);
    }

    private record TestCsrf(jakarta.servlet.http.Cookie cookie, String token) { }
}
