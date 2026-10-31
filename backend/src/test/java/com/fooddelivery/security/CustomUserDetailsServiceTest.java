package com.fooddelivery.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fooddelivery.entity.Role;
import com.fooddelivery.entity.User;
import com.fooddelivery.entity.enums.UserStatus;
import com.fooddelivery.repository.UserRepository;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.EntityGraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class CustomUserDetailsServiceTest {
    private UserRepository userRepository;
    private CustomUserDetailsService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        service = new CustomUserDetailsService(userRepository);
    }

    @Test
    void normalizesEmailAndBuildsIdentityAndAuthoritiesFromDatabaseUser() {
        User user = activeUser();
        when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(user));

        AuthenticatedUser principal = (AuthenticatedUser) service.loadUserByUsername("  Owner@Example.COM ");

        assertEquals(42L, principal.getUserId());
        assertEquals("owner@example.com", principal.getUsername());
        assertEquals("owner@example.com", principal.getEmail());
        assertEquals(Set.of(new SimpleGrantedAuthority("ROLE_RESTAURANT")), principal.getAuthorities());
        assertEquals("$2a$10$test-hash", principal.getPassword());
        verify(userRepository).findByEmailIgnoreCase("owner@example.com");
    }

    @Test
    void rejectsInactiveUserWithGenericFailure() {
        User user = activeUser();
        when(user.getStatus()).thenReturn(UserStatus.SUSPENDED);
        when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(user));

        UsernameNotFoundException error = assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("owner@example.com"));

        assertEquals("Invalid email or password", error.getMessage());
    }

    @Test
    void rejectsMissingUserWithSameGenericFailure() {
        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        UsernameNotFoundException error = assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("missing@example.com"));

        assertEquals("Invalid email or password", error.getMessage());
    }

    @Test
    void rejectsBlankEmail() {
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("  "));
    }

    @Test
    void bcryptEncoderVerifiesBcryptFormattedPassword() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
        String encoded = encoder.encode("local-test-password");

        assertTrue(encoder.matches("local-test-password", encoded));
    }

    @Test
    void emailLookupFetchesRolesWithTheUser() throws Exception {
        EntityGraph entityGraph = UserRepository.class
                .getMethod("findByEmailIgnoreCase", String.class)
                .getAnnotation(EntityGraph.class);

        assertEquals(Set.of("roles"), Set.of(entityGraph.attributePaths()));
    }

    private static User activeUser() {
        User user = mock(User.class);
        Role role = mock(Role.class);
        when(role.getName()).thenReturn("RESTAURANT");
        when(user.getId()).thenReturn(42L);
        when(user.getEmail()).thenReturn("Owner@Example.com");
        when(user.getPasswordHashForAuthentication()).thenReturn("$2a$10$test-hash");
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(user.getRoles()).thenReturn(Set.of(role));
        return user;
    }
}
