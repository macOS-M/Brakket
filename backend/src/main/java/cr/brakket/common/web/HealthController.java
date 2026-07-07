package cr.brakket.common.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoints básicos para verificar que la API responde y para conocer al usuario
 * autenticado. Sirven de humo/smoke test mientras se construyen los módulos.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    /** Endpoint público de estado (no requiere autenticación). */
    @GetMapping("/public/ping")
    public Map<String, String> ping() {
        return Map.of("status", "ok", "app", "brakket-backend");
    }

    /** Devuelve los datos del usuario autenticado vía Google OAuth2. */
    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return Map.of("authenticated", false);
        }
        return Map.of(
                "authenticated", true,
                "nombre", principal.getAttribute("name"),
                "correo", principal.getAttribute("email"),
                "foto", principal.getAttribute("picture")
        );
    }
}
