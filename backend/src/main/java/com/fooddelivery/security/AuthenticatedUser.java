package com.fooddelivery.security;

import com.fooddelivery.entity.Role;
import com.fooddelivery.entity.User;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** Authenticated identity copied from a database User for use by Spring Security. */
public final class AuthenticatedUser implements UserDetails {
    private final Long userId;
    private final String normalizedEmail;
    private final String passwordHash;
    private final Set<GrantedAuthority> authorities;

    private AuthenticatedUser(Long userId, String normalizedEmail, String passwordHash,
                              Set<GrantedAuthority> authorities) {
        this.userId = Objects.requireNonNull(userId, "userId");
        this.normalizedEmail = normalizeEmail(normalizedEmail);
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.authorities = Set.copyOf(authorities);
    }

    public static AuthenticatedUser from(User user) {
        Objects.requireNonNull(user, "user");
        Set<GrantedAuthority> authorities = user.getRoles().stream()
                .map(Role::getName)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .map(name -> new SimpleGrantedAuthority("ROLE_" + name.toUpperCase(Locale.ROOT)))
                .collect(Collectors.toUnmodifiableSet());

        return new AuthenticatedUser(
                user.getId(), user.getEmail(), user.getPasswordHashForAuthentication(), authorities);
    }

    static AuthenticatedUser fromVerifiedJwt(long userId, String email, Set<String> authorities) {
        Set<GrantedAuthority> grantedAuthorities = authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toUnmodifiableSet());
        return new AuthenticatedUser(userId, email, "", grantedAuthorities);
    }

    private static String normalizeEmail(String email) {
        return Objects.requireNonNull(email, "email").trim().toLowerCase(Locale.ROOT);
    }

    public Long getUserId() { return userId; }
    public String getEmail() { return normalizedEmail; }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return normalizedEmail; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
