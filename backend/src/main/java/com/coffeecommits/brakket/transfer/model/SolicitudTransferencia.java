package com.coffeecommits.brakket.transfer.model;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.team.model.Equipo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Solicitud de transferencia de un jugador entre equipos (RF-12). Se crea
 * PENDIENTE y se resuelve (RF-13) cuando el jugador y el capitán del equipo
 * origen registran su respuesta: ambos aceptan → APROBADA (el jugador cambia
 * de plantilla); cualquiera rechaza → RECHAZADA (todo queda como estaba).
 */
@Entity
@Table(name = "solicitud_transferencia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudTransferencia {

    public static final String ESTADO_PENDIENTE = "PENDIENTE";
    public static final String ESTADO_APROBADA = "APROBADA";
    public static final String ESTADO_RECHAZADA = "RECHAZADA";

    public static final String APROBACION_PENDIENTE = "PENDIENTE";
    public static final String APROBACION_ACEPTADA = "ACEPTADA";
    public static final String APROBACION_RECHAZADA = "RECHAZADA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jugador_id", nullable = false)
    private Usuario jugador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipo_origen_id", nullable = false)
    private Equipo equipoOrigen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipo_destino_id", nullable = false)
    private Equipo equipoDestino;

    /** Capitán del equipo destino que inició la solicitud. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitante_id", nullable = false)
    private Usuario solicitante;

    @Column(name = "rol_propuesto", nullable = false, length = 30)
    private String rolPropuesto;

    @Column(name = "justificacion", length = 500)
    private String justificacion;

    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private String estado = ESTADO_PENDIENTE;

    @Column(name = "aprobacion_jugador", nullable = false, length = 20)
    @Builder.Default
    private String aprobacionJugador = APROBACION_PENDIENTE;

    @Column(name = "aprobacion_capitan_origen", nullable = false, length = 20)
    @Builder.Default
    private String aprobacionCapitanOrigen = APROBACION_PENDIENTE;

    @Column(name = "fecha_solicitud", nullable = false)
    @Builder.Default
    private LocalDateTime fechaSolicitud = LocalDateTime.now();

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    /** Usuario cuya respuesta resolvió la solicitud (última aprobación o rechazo). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resuelta_por")
    private Usuario resueltaPor;
}
