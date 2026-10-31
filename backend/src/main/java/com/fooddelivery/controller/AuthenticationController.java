package com.fooddelivery.controller;

import com.fooddelivery.dto.auth.CustomerRegistrationRequest;
import com.fooddelivery.dto.auth.CustomerResponse;
import com.fooddelivery.dto.auth.CsrfTokenResponse;
import com.fooddelivery.dto.auth.LoginRequest;
import com.fooddelivery.dto.auth.LoginResponse;
import com.fooddelivery.dto.common.ApiResponse;
import com.fooddelivery.service.auth.AuthenticationService;
import com.fooddelivery.service.auth.LoginSession;
import com.fooddelivery.config.RefreshCookieProperties;
import com.fooddelivery.config.RefreshTokenCookieFactory;
import com.fooddelivery.exception.ApiError;
import com.fooddelivery.service.auth.RefreshAttempt;
import com.fooddelivery.service.auth.RefreshOutcome;
import com.fooddelivery.service.auth.RefreshTokenLifecycleService;
import com.fooddelivery.security.SpaCsrfTokenRequestHandler;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.util.WebUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {
    private final AuthenticationService authenticationService;
    private final RefreshTokenCookieFactory refreshTokenCookieFactory;
    private final RefreshCookieProperties refreshCookieProperties;
    private final RefreshTokenLifecycleService refreshTokenLifecycleService;

    public AuthenticationController(AuthenticationService authenticationService,
                                    RefreshTokenCookieFactory refreshTokenCookieFactory,
                                    RefreshCookieProperties refreshCookieProperties,
                                    RefreshTokenLifecycleService refreshTokenLifecycleService) {
        this.authenticationService = authenticationService;
        this.refreshTokenCookieFactory = refreshTokenCookieFactory;
        this.refreshCookieProperties = refreshCookieProperties;
        this.refreshTokenLifecycleService = refreshTokenLifecycleService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<CustomerResponse>> register(
            @Valid @RequestBody CustomerRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Customer registered", authenticationService.registerCustomer(request)));
    }

    @GetMapping("/csrf")
    public ResponseEntity<ApiResponse<CsrfTokenResponse>> csrf(
            @RequestAttribute(SpaCsrfTokenRequestHandler.BOOTSTRAP_TOKEN_ATTRIBUTE) String token) {
        return ResponseEntity.ok(ApiResponse.ok("CSRF token issued",
                new CsrfTokenResponse(token, "X-XSRF-TOKEN")));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginSession session = authenticationService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieFactory.create(session.refreshToken()).toString())
                .body(ApiResponse.ok("Login successful", session.accessTokenResponse()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, refreshCookieProperties.name());
        String rawRefreshToken = cookie == null ? null : cookie.getValue();
        RefreshAttempt attempt = refreshTokenLifecycleService.rotate(rawRefreshToken);

        if (attempt.outcome() != RefreshOutcome.ROTATED) {
            ResponseCookie expiredCookie = refreshTokenCookieFactory.clear();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
                    .body(ApiError.of(HttpStatus.UNAUTHORIZED.value(), "INVALID_REFRESH_TOKEN",
                            "Authentication is required"));
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        refreshTokenCookieFactory.create(attempt.session().refreshToken()).toString())
                .body(ApiResponse.ok("Token refreshed", attempt.session().accessTokenResponse()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, refreshCookieProperties.name());
        refreshTokenLifecycleService.logout(cookie == null ? null : cookie.getValue());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieFactory.clear().toString())
                .body(ApiResponse.ok("Logout successful"));
    }
}
