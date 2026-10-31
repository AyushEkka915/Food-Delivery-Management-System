package com.fooddelivery.entity;

import com.fooddelivery.entity.enums.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "users")
public class User extends AuditableEntity {
    @Column(nullable = false, length = 120) private String name;
    @Column(nullable = false, unique = true, length = 190) private String email;
    @Column(nullable = false, unique = true, length = 20) private String phone;
    @Column(name = "password_hash", nullable = false, length = 80) private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private UserStatus status = UserStatus.ACTIVE;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    protected User() { }

    public static User registerCustomer(String name, String email, String phone, String passwordHash, Role customerRole) {
        Objects.requireNonNull(customerRole, "customerRole");
        if (!"CUSTOMER".equalsIgnoreCase(customerRole.getName())) {
            throw new IllegalArgumentException("Public registration can assign only the CUSTOMER role");
        }
        User user = new User();
        user.name = requireText(name, "name");
        user.email = requireText(email, "email").toLowerCase(Locale.ROOT);
        user.phone = requireText(phone, "phone");
        user.passwordHash = requireText(passwordHash, "passwordHash");
        user.status = UserStatus.ACTIVE;
        user.roles.add(customerRole);
        return user;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    /** For Spring Security credential verification only; never map this to an API DTO. */
    public String getPasswordHashForAuthentication() { return passwordHash; }
    public UserStatus getStatus() { return status; }
    public Set<Role> getRoles() { return Set.copyOf(roles); }
}
