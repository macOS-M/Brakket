package com.coffeecommits.brakket.analytics.controller;

import com.coffeecommits.brakket.analytics.dto.AsistentePreguntaRequest;
import com.coffeecommits.brakket.analytics.dto.AsistenteRespuesta;
import com.coffeecommits.brakket.analytics.dto.ClasificacionInmediataResponse;
import com.coffeecommits.brakket.analytics.dto.SeriesTransmisionResponse;
import com.coffeecommits.brakket.analytics.dto.TransmisionAnalizableResponse;
import com.coffeecommits.brakket.analytics.service.AsistenteTermometroService;
import com.coffeecommits.brakket.analytics.service.MetricasTransmisionService;
import com.coffeecommits.brakket.twitch.service.MuestreoChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/** RF-37: panel analítico de transmisiones. */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class MetricasTransmisionController {

    private final MetricasTransmisionService service;
    private final AsistenteTermometroService asistente;
    private final MuestreoChatService muestreo;

    /** Transmisiones que el usuario puede consultar; sin esto no hay forma de obtener un id. */
    @GetMapping("/transmisiones")
    @PreAuthorize("hasAnyRole('ADMIN','COMISIONADO')")
    public ResponseEntity<List<TransmisionAnalizableResponse>> transmisiones(Authentication authentication) {
        return ResponseEntity.ok(service.catalogo(authentication.getName(), esAdmin(authentication)));
    }

    @GetMapping("/transmisiones/{transmisionId}/series")
    @PreAuthorize("hasAnyRole('ADMIN','COMISIONADO')")
    public ResponseEntity<SeriesTransmisionResponse> series(
            @PathVariable Long transmisionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(required = false) String agrupacion,
            Authentication authentication) {
        return ResponseEntity.ok(service.series(transmisionId, desde, hasta, agrupacion,
                authentication.getName(), esAdmin(authentication)));
    }

    /**
     * Asistente del termómetro (RF-40). Solo ADMIN: a diferencia de las series,
     * esta consulta sale hacia un proveedor externo y consume cuota, así que no
     * se abre a comisionados ni patrocinadores.
     */
    @PostMapping("/transmisiones/{transmisionId}/asistente")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AsistenteRespuesta> asistente(
            @PathVariable Long transmisionId,
            @Valid @RequestBody AsistentePreguntaRequest peticion,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(required = false) String agrupacion,
            Authentication authentication) {
        return ResponseEntity.ok(asistente.responder(transmisionId, peticion.pregunta(), desde, hasta,
                agrupacion, authentication.getName(), esAdmin(authentication)));
    }

    /**
     * Clasifica el chat acumulado sin esperar al bloque (RF-39). Solo ADMIN.
     *
     * <p>Vive acá y no bajo {@code /transmisiones} porque no lleva id: el
     * muestreo atiende una transmisión a la vez y cierra el bloque que tenga en
     * curso. La petición espera al proveedor, así que puede tardar unos segundos.</p>
     */
    @PostMapping("/muestreo/sentimiento")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClasificacionInmediataResponse> clasificarAhora() {
        int mensajes = muestreo.clasificarAhora();
        return ResponseEntity.ok(mensajes == 0
                ? ClasificacionInmediataResponse.sinChat()
                : ClasificacionInmediataResponse.de(mensajes));
    }

    private static boolean esAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
