package com.kaikeMartins.barberapp_backend.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    public static final String SECURITY = "bearerAuth";
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http
                .cors(cors -> {})
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);

                        }))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/gemini/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/barbeiros/**", "/servicos/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/barbeiros/**", "/servicos/**").hasAnyAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/agendamentos/**").hasAnyAuthority("ROLE_CLIENTE", "ROLE_ADMIN", "ROLE_BARBEIRO")
                        .requestMatchers(HttpMethod.GET, "/agendamentos/**").hasAnyAuthority("ROLE_CLIENTE", "ROLE_ADMIN", "ROLE_BARBEIRO")
                        .requestMatchers(HttpMethod.PUT, "/agendamentos/**").hasAnyAuthority("ROLE_CLIENTE", "ROLE_ADMIN", "ROLE_BARBEIRO")
                        .requestMatchers(HttpMethod.PUT, "/barbeiros/**", "/servicos/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_BARBEIRO")
                        .requestMatchers(HttpMethod.DELETE, "/barbeiros/**", "/servicos/**").hasAnyAuthority("ROLE_ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) {
        return config.getAuthenticationManager();
    }
}
