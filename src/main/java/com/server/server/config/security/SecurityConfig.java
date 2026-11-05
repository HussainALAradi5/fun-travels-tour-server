package com.server.server.config.security;

import com.server.server.middlewares.logging.RequestIdFilter;
import com.server.server.middlewares.security.AuthRateLimitFilter;
import com.server.server.middlewares.errors.SecurityErrorResponder;
import com.server.server.config.properties.AppProperties;
import com.server.server.middlewares.auth.JwtAuthenticationFilter;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    private final SecurityErrorResponder securityErrorResponder;
    private final AuthRateLimitFilter authRateLimitFilter;
    private final AuthenticationProvider authenticationProvider;
    private final AppProperties appProperties;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(api("/auth/**")).permitAll()
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        .requestMatchers("/ws-notifications/**").permitAll()
                        .requestMatchers(HttpMethod.GET, api("/tours/**")).permitAll()
                        .requestMatchers(HttpMethod.GET, api("/meals/**")).permitAll()
                        .requestMatchers(HttpMethod.GET, api("/countries/**")).permitAll()
                        .requestMatchers(HttpMethod.GET, api("/ports/**")).permitAll()
                        .requestMatchers(HttpMethod.GET, api("/cities/**")).permitAll()
                        .requestMatchers(HttpMethod.GET, api("/wallet/config")).permitAll()
                        .requestMatchers(api("/tickets/**")).hasAnyAuthority("CUSTOMER", "ADMIN", "MANAGER", "EMPLOYEE", "OWNER")
                        .requestMatchers(api("/reservations/**")).hasAnyAuthority("CUSTOMER", "ADMIN", "MANAGER", "EMPLOYEE", "OWNER")
                        .requestMatchers(api("/notifications/**")).hasAnyAuthority("CUSTOMER", "ADMIN", "MANAGER", "EMPLOYEE", "OWNER")
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(securityErrorResponder)
                        .accessDeniedHandler(securityErrorResponder))
                .headers(headers -> headers
                        .contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(frame -> frame.deny()))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(authRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** A path under the configured API prefix, such as {@code /api/v1/auth/**}. */
    private String api(String path) {
        return appProperties.apiPrefix() + path;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(appProperties.corsAllowedOrigins());
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", RequestIdFilter.HEADER));
        config.setExposedHeaders(Arrays.asList(RequestIdFilter.HEADER, "Retry-After"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}