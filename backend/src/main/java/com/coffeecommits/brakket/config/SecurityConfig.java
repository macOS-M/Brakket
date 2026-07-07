package com.coffeecommits.brakket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Configuración de seguridad de Brakket (EPIC-01, RNF-03 / RNF-12).
 *
 * <p>Modelo de autenticación: <b>login con Google (OAuth2) → JWT propio</b>.
 * Tras autenticarse con Google, {@link OAuth2LoginSuccessHandler} emite un JWT y
 * redirige al frontend. A partir de ahí, la SPA envía el token en cada request y
 * {@link JwtAuthenticationFilter} lo valida (sesión stateless).</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    public SecurityConfig(CorsConfigurationSource corsConfigurationSource,
                          JwtAuthenticationFilter jwtAuthenticationFilter,
                          OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler) {
        this.corsConfigurationSource = corsConfigurationSource;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            // API REST con JWT: sin CSRF y sin sesión de servidor.
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Públicos: salud, documentación Swagger, ping y flujo OAuth.
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/oauth2/**", "/login/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // Lecturas públicas (catálogo de juegos):
                .requestMatchers(HttpMethod.GET, "/api/games/**").permitAll()
                // Todo lo demás requiere JWT válido.
                .anyRequest().authenticated()
            )
            // Login con Google -> emite JWT y redirige al frontend.
            .oauth2Login(oauth -> oauth.successHandler(oAuth2LoginSuccessHandler))
            // Valida el JWT en cada request antes de la autenticación por usuario/clave.
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
