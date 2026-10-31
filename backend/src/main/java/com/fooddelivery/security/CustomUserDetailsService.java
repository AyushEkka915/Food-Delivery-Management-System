package com.fooddelivery.security;

import com.fooddelivery.entity.User;
import com.fooddelivery.entity.enums.UserStatus;
import com.fooddelivery.repository.UserRepository;
import java.util.Locale;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private static final String GENERIC_AUTHENTICATION_FAILURE = "Invalid email or password";

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.isBlank()) {
            throw authenticationFailure();
        }

        String normalizedEmail = username.trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .filter(found -> found.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(CustomUserDetailsService::authenticationFailure);

        return AuthenticatedUser.from(user);
    }

    private static UsernameNotFoundException authenticationFailure() {
        return new UsernameNotFoundException(GENERIC_AUTHENTICATION_FAILURE);
    }
}
