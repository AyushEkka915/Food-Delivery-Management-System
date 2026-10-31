package com.fooddelivery.config;

import com.fooddelivery.security.SpaCsrfTokenRequestHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;

@Configuration
public class CsrfConfiguration {
    @Bean
    public CookieCsrfTokenRepository csrfTokenRepository(RefreshCookieProperties refreshCookieProperties) {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieName("XSRF-TOKEN");
        repository.setHeaderName("X-XSRF-TOKEN");
        repository.setCookiePath(refreshCookieProperties.path());
        repository.setSecure(refreshCookieProperties.secure());
        repository.setCookieCustomizer(cookie -> cookie.sameSite(refreshCookieProperties.sameSite()));
        return repository;
    }

    @Bean
    public CsrfTokenRequestHandler csrfTokenRequestHandler() {
        return new SpaCsrfTokenRequestHandler();
    }
}
