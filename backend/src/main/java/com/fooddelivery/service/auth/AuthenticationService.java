package com.fooddelivery.service.auth;

import com.fooddelivery.dto.auth.CustomerRegistrationRequest;
import com.fooddelivery.dto.auth.CustomerResponse;
import com.fooddelivery.dto.auth.LoginRequest;
import com.fooddelivery.dto.auth.LoginResponse;
import com.fooddelivery.entity.Role;
import com.fooddelivery.entity.User;
import com.fooddelivery.entity.enums.UserStatus;
import com.fooddelivery.exception.ApiException;
import com.fooddelivery.repository.RoleRepository;
import com.fooddelivery.repository.UserRepository;
import com.fooddelivery.security.AuthenticatedUser;
import com.fooddelivery.security.JwtService;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {
    private static final String CUSTOMER_ROLE = "CUSTOMER";
    private static final String GENERIC_LOGIN_FAILURE = "Invalid email or password";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenIssuer refreshTokenIssuer;

    public AuthenticationService(UserRepository userRepository, RoleRepository roleRepository,
                                 PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
                                 JwtService jwtService, RefreshTokenIssuer refreshTokenIssuer) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenIssuer = refreshTokenIssuer;
    }

    @Transactional
    public CustomerResponse registerCustomer(CustomerRegistrationRequest request) {
        String email = normalizeEmail(request.email());
        String phone = request.phone().trim();
        validateBcryptPasswordLength(request.password());

        if (userRepository.existsByEmailIgnoreCase(email) || userRepository.existsByPhone(phone)) {
            throw new ApiException(HttpStatus.CONFLICT, "ACCOUNT_ALREADY_EXISTS",
                    "An account with that email or phone already exists");
        }

        Role customerRole = roleRepository.findByName(CUSTOMER_ROLE)
                .orElseThrow(() -> new IllegalStateException("Required CUSTOMER role is missing from the database"));
        String passwordHash = passwordEncoder.encode(request.password());
        User user = User.registerCustomer(request.name(), email, phone, passwordHash, customerRole);

        try {
            User saved = userRepository.saveAndFlush(user);
            return new CustomerResponse(saved.getId(), saved.getName(), saved.getEmail(), saved.getPhone(),
                    saved.getCreatedAt());
        } catch (DataIntegrityViolationException exception) {
            // Covers concurrent duplicate registrations that pass the pre-checks.
            if (isDuplicateKey(exception)) {
                throw new ApiException(HttpStatus.CONFLICT, "ACCOUNT_ALREADY_EXISTS",
                        "An account with that email or phone already exists");
            }
            throw exception;
        }
    }

    @Transactional
    public LoginSession login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        validateBcryptPasswordLength(request.password());
        final Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email, request.password()));
        } catch (AuthenticationException exception) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", GENERIC_LOGIN_FAILURE);
        }

        if (!(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new AuthenticationServiceException("Authentication provider returned an unexpected principal");
        }

        User databaseUser = userRepository.findById(user.getUserId())
                .filter(found -> found.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                        GENERIC_LOGIN_FAILURE));
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenIssuer.issueFor(databaseUser);
        LoginResponse accessResponse = new LoginResponse(accessToken, "Bearer",
                JwtService.ACCESS_TOKEN_LIFETIME.toSeconds());
        return new LoginSession(accessResponse, refreshToken);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static void validateBcryptPasswordLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                    "Password exceeds the maximum supported length");
        }
    }

    private static boolean isDuplicateKey(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException && sqlException.getErrorCode() == 1062) {
                return true;
            }
        }
        return false;
    }
}
