package tn.cafe.pos.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tn.cafe.pos.infrastructure.security.JwtAuthFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtAuthFilter jwtFilter;

    public SecurityConfig(JwtAuthFilter jwtFilter) { this.jwtFilter = jwtFilter; }

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    SecurityFilterChain chain(HttpSecurity http) throws Exception {
        return http
            .csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/api/v1/auth/login", "/api/v1/auth/pin", "/api/v1/auth/qr",
                    "/api/v1/auth/qr-key",
                    "/api/v1/notifications/stream",
                        "/swagger-ui.html", "/swagger-ui/**", "/api-docs/**",
                        "/v3/api-docs/**", "/actuator/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/categories/**", "/api/v1/products/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/orders", "/api/v1/payments/**").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
