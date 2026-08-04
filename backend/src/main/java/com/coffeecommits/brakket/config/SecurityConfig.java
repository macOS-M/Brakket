package com.coffeecommits.brakket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Configuración de seguridad de Brakket (EPIC-01, RNF-03 / RNF-12).
 *
 * <p>Modelo de autenticación: <b>login con Google (OAuth2) → JWT propio</b>.
 * Tras autenticarse con Google, {@link OAuth2LoginSuccessHandler} emite un JWT y
 * redirige al frontend. A partir de ahí, la SPA envía el token en cada request y
 * {@link JwtAuthenticationFilter} lo valida (sesión stateless).</p>
 *
 * <p>{@code @EnableMethodSecurity} habilita {@code @PreAuthorize} en los
 * controladores (RF-19: control de roles y permisos).</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;

    public SecurityConfig(CorsConfigurationSource corsConfigurationSource,
                          JwtAuthenticationFilter jwtAuthenticationFilter,
                          OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler,
                          OAuth2LoginFailureHandler oAuth2LoginFailureHandler) {
        this.corsConfigurationSource = corsConfigurationSource;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
        this.oAuth2LoginFailureHandler = oAuth2LoginFailureHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   ClientRegistrationRepository clientRegistrationRepository)
            throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                // API REST con JWT: sin CSRF y sin sesión de servidor.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        // Públicos: salud, documentación Swagger, ping y flujo OAuth.
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/auth/**").permitAll()
                        // Registro y login locales (DD-04): públicos por diseño.
                        .requestMatchers(HttpMethod.POST, "/api/auth/registro", "/api/auth/login").permitAll()
                        .requestMatchers("/oauth2/**", "/login/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // Lecturas públicas: el catálogo, las ligas y la búsqueda de
                        // equipos son navegables sin sesión; el login se exige recién
                        // al actuar (crear, inscribir, invitar). El buscador externo
                        // queda fuera del permitAll: consume la API key de RAWG y
                        // exige sesión (primer match gana).
                        .requestMatchers(HttpMethod.GET, "/api/games/buscar-externo").authenticated()
                        // Las imágenes subidas se sirven como cualquier URL de logo
                        // externa: públicas en lectura; subirlas exige sesión.
                        .requestMatchers(HttpMethod.GET, "/api/uploads/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/games/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/leagues/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/teams/search").permitAll()
                        // RF-15: el historial del jugador se enlaza desde el perfil
                        // público del equipo; el service ya degrada a "solo perfiles
                        // PUBLIC" cuando no hay sesión.
                        .requestMatchers(HttpMethod.GET, "/api/players/*/historial-equipos").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/tournaments/*/equipos-elegibles").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/tournaments/mios").authenticated()
                        // RF-31: no es informacion publica de la partida, exigimos sesion
                        // aparte del @PreAuthorize del controller (no depender de una sola capa).
                        .requestMatchers(HttpMethod.GET, "/api/tournaments/partidas/*/disputas").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/tournaments/**").permitAll()
                        // RF-35: ver los directos no exige sesión, como el catálogo.
                        .requestMatchers(HttpMethod.GET, "/api/transmisiones").permitAll()
                        // Todo lo demás requiere JWT válido.
                        .anyRequest().authenticated()
                )
                // API REST: ante una request sin autenticar se responde 401 (no se
                // redirige a Google). El SPA inicia el login navegando explícitamente
                // a /oauth2/authorization/google; el resto de rutas /api/** deben
                // devolver 401 para que el interceptor del frontend reaccione.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                // Login con Google -> emite JWT y redirige al frontend.
                // prompt=select_account: Google muestra siempre el selector de
                // cuentas en vez de reutilizar en silencio la última sesión.
                .oauth2Login(oauth -> oauth
                        .authorizationEndpoint(endpoint -> endpoint
                                .authorizationRequestResolver(
                                        conSelectorDeCuentas(clientRegistrationRepository)))
                        .successHandler(oAuth2LoginSuccessHandler)
                        .failureHandler(oAuth2LoginFailureHandler))
                // Valida el JWT en cada request antes de la autenticación por usuario/clave.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private static OAuth2AuthorizationRequestResolver conSelectorDeCuentas(
            ClientRegistrationRepository clientRegistrationRepository) {
        DefaultOAuth2AuthorizationRequestResolver resolver = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository,
                OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI);
        resolver.setAuthorizationRequestCustomizer(builder ->
                builder.additionalParameters(params -> params.put("prompt", "select_account")));
        return resolver;
    }
}