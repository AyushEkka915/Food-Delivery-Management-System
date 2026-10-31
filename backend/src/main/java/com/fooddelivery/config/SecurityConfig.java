
package com.fooddelivery.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.fooddelivery.security.ApiAuthenticationEntryPoint;
import com.fooddelivery.security.JwtAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration

public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ApiAuthenticationEntryPoint authenticationEntryPoint;
    private final CookieCsrfTokenRepository csrfTokenRepository;
    private final CsrfTokenRequestHandler csrfTokenRequestHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          ApiAuthenticationEntryPoint authenticationEntryPoint,
                          CookieCsrfTokenRepository csrfTokenRepository,
                          CsrfTokenRequestHandler csrfTokenRequestHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.csrfTokenRepository = csrfTokenRepository;
        this.csrfTokenRequestHandler = csrfTokenRequestHandler;
    }
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository)
                .csrfTokenRequestHandler(csrfTokenRequestHandler)
                .ignoringRequestMatchers(request -> {
                String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
                boolean bearerRequest = authorization != null
                        && authorization.regionMatches(true, 0, "Bearer ", 0, 7);
                boolean publicAuthPost = HttpMethod.POST.matches(request.getMethod())
                        && (request.getRequestURI().equals(request.getContextPath() + "/api/v1/auth/register")
                        || request.getRequestURI().equals(request.getContextPath() + "/api/v1/auth/login"));
                boolean refreshPost = HttpMethod.POST.matches(request.getMethod())
                        && request.getRequestURI().equals(request.getContextPath() + "/api/v1/auth/refresh");
                boolean logoutPost = HttpMethod.POST.matches(request.getMethod())
                        && request.getRequestURI().equals(request.getContextPath() + "/api/v1/auth/logout");
                return publicAuthPost || (bearerRequest && !refreshPost && !logoutPost);
            }))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/auth/csrf").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh", "/api/v1/auth/logout").permitAll()
                .requestMatchers(
                    "/api/v1/health",
                    "/actuator/health",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(errors -> errors.authenticationEntryPoint(authenticationEntryPoint))
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
