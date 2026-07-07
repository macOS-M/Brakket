package cr.brakket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Configuración de seguridad base de Brakket.
 *
 * <p>Punto de partida para EPIC-01 (Autenticación y Perfiles) y RNF-03 / RNF-12.
 * Habilita login con Google (OAuth2) y deja endpoints públicos mínimos. El equipo
 * deberá refinar la autorización por rol a medida que se implementen los módulos.</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(CorsConfigurationSource corsConfigurationSource) {
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            // API REST consumida por Angular: sin CSRF de formularios.
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                // Endpoints públicos: salud, documentación y datos públicos.
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/oauth2/**", "/login/**").permitAll()
                // Lecturas públicas (perfiles, catálogos) se pueden abrir aquí:
                .requestMatchers(HttpMethod.GET, "/api/juegos/**").permitAll()
                // Todo lo demás requiere autenticación.
                .anyRequest().authenticated()
            )
            // Login con Google. Ajustar defaultSuccessUrl al frontend en despliegue.
            .oauth2Login(oauth -> oauth
                .defaultSuccessUrl("http://localhost:4200/", true)
            )
            .logout(logout -> logout.logoutSuccessUrl("http://localhost:4200/"));

        return http.build();
    }
}
