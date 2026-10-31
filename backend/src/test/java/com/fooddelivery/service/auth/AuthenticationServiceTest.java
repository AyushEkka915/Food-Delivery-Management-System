package com.fooddelivery.service.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthenticationServiceTest {
    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private PasswordEncoder passwordEncoder;
    private AuthenticationManager authenticationManager;
    private JwtService jwtService;
    private RefreshTokenIssuer refreshTokenIssuer;
    private AuthenticationService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        authenticationManager = mock(AuthenticationManager.class);
        jwtService = mock(JwtService.class);
        refreshTokenIssuer = mock(RefreshTokenIssuer.class);
        service = new AuthenticationService(userRepository, roleRepository, passwordEncoder,
                authenticationManager, jwtService, refreshTokenIssuer);
    }

    @Test
    void registrationNormalizesEmailHashesPasswordAndAssignsOnlyCustomerRole() {
        Role customer = role("CUSTOMER");
        when(userRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("+919123456789")).thenReturn(false);
        when(roleRepository.findByName("CUSTOMER")).thenReturn(Optional.of(customer));
        passwordEncoder = new BCryptPasswordEncoder(10);
        service = new AuthenticationService(userRepository, roleRepository, passwordEncoder,
                authenticationManager, jwtService, refreshTokenIssuer);
        AtomicReference<User> savedUser = new AtomicReference<>();
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            savedUser.set(user);
            return user;
        });

        CustomerResponse response = service.registerCustomer(new CustomerRegistrationRequest(
                " New Customer ", " New@Example.com ", " +919123456789 ", "strong-password-1"));

        assertEquals("new@example.com", response.email());
        assertEquals("New Customer", response.name());
        User saved = savedUser.get();
        org.junit.jupiter.api.Assertions.assertNotNull(saved);
        assertNotEquals("strong-password-1", saved.getPasswordHashForAuthentication());
        org.junit.jupiter.api.Assertions.assertTrue(
                passwordEncoder.matches("strong-password-1", saved.getPasswordHashForAuthentication()));
        assertEquals(UserStatus.ACTIVE, saved.getStatus());
        assertEquals(Set.of(customer), saved.getRoles());
        verify(roleRepository).findByName("CUSTOMER");
    }

    @Test
    void duplicateEmailReturnsConflictBeforeHashingOrSaving() {
        when(userRepository.existsByEmailIgnoreCase("known@example.com")).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, () -> service.registerCustomer(
                new CustomerRegistrationRequest("Customer", "Known@example.com", "+919123456789", "password123")));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void duplicatePhoneReturnsConflict() {
        when(userRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("+919123456789")).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, () -> service.registerCustomer(
                new CustomerRegistrationRequest("Customer", "new@example.com", "+919123456789", "password123")));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    void customerFactoryRejectsPrivilegedRole() {
        assertThrows(IllegalArgumentException.class, () -> User.registerCustomer(
                "Customer", "customer@example.com", "+919123456789", "encoded-hash", role("ADMIN")));
    }

    @Test
    void loginAuthenticatesNormalizedEmailAndIssuesAccessAndRefreshTokens() {
        AuthenticatedUser principal = principal();
        User databaseUser = mock(User.class);
        when(databaseUser.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(userRepository.findById(8L)).thenReturn(Optional.of(databaseUser));
        Authentication authenticated = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities());
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authenticated);
        when(jwtService.generateAccessToken(principal)).thenReturn("signed-access-token");
        when(refreshTokenIssuer.issueFor(databaseUser)).thenReturn("raw-refresh-token");

        LoginSession session = service.login(new LoginRequest(" CUSTOMER@Example.com ", "password123"));

        LoginResponse response = session.accessTokenResponse();
        assertEquals("signed-access-token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(900, response.expiresInSeconds());
        assertEquals("raw-refresh-token", session.refreshToken());
        assertFalse(session.toString().contains("raw-refresh-token"));
        assertFalse(session.toString().contains("signed-access-token"));
        verify(refreshTokenIssuer).issueFor(databaseUser);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void incorrectCredentialsReturnGenericUnauthorizedAndDoNotIssueToken() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("private detail"));

        ApiException exception = assertThrows(ApiException.class,
                () -> service.login(new LoginRequest("unknown@example.com", "wrong-password")));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals("Invalid email or password", exception.getMessage());
        verify(jwtService, never()).generateAccessToken(any());
        verify(refreshTokenIssuer, never()).issueFor(any());
    }

    @Test
    void rejectsBcryptUnsupportedUtf8PasswordLength() {
        String longUtf8Password = "😀".repeat(19);

        ApiException exception = assertThrows(ApiException.class, () -> service.login(
                new LoginRequest("customer@example.com", longUtf8Password)));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    private static Role role(String name) {
        Role role = mock(Role.class);
        when(role.getName()).thenReturn(name);
        return role;
    }

    private static AuthenticatedUser principal() {
        User user = mock(User.class);
        Role role = role("CUSTOMER");
        when(user.getId()).thenReturn(8L);
        when(user.getEmail()).thenReturn("customer@example.com");
        when(user.getPasswordHashForAuthentication()).thenReturn("stored-hash");
        when(user.getRoles()).thenReturn(Set.of(role));
        return AuthenticatedUser.from(user);
    }

}
