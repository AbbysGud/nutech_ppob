package com.nutech.ppob.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

import java.util.List;

@Configuration
public class SecurityConfig {

    private static final String[] PUBLIC_POST = { "/registration", "/login" };
    private static final String[] PUBLIC_GET  = { "/actuator/**", "/ping", "/db/check", "/banner" };

    private final JwtAuthFilter jwtFilter;
    private final RestAuthEntryPoint entryPoint;

    public SecurityConfig(JwtAuthFilter jwtFilter, RestAuthEntryPoint entryPoint) {
        this.jwtFilter = jwtFilter;
        this.entryPoint = entryPoint;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
          .csrf(csrf -> csrf.disable())
          .cors(Customizer.withDefaults())
          .sessionManagement(sm -> sm.sessionCreationPolicy(
              org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
          .headers(h -> h.frameOptions(f -> f.sameOrigin()))
          .authorizeHttpRequests(auth -> auth
              .requestMatchers(HttpMethod.POST, PUBLIC_POST).permitAll()
              .requestMatchers(HttpMethod.GET,  PUBLIC_GET).permitAll()
              .anyRequest().authenticated()
          )
          .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint))
          .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
          .httpBasic(httpBasic -> httpBasic.disable())
          .formLogin(form -> form.disable());

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        var cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of("*"));
        cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setExposedHeaders(List.of("Authorization","Content-Type"));
        var src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }
}
