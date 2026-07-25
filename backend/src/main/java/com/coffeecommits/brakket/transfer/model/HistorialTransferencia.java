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
 * Registro histórico e inmutable de una transferencia completada (RF-14).
 * Se crea una sola vez, automáticamente, cuando SolicitudTransferencia pasa
 * a estado APROBADA (ver TransferResponseServiceImpl). Nadie lo edita ni
 * lo borra manualmente: no existen endpoints de escritura sobre esta tabla
 * fuera de esa creación automática.
 */
@Entity
@Table(name = "historial_transferencia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistorialTransferencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Solicitud que originó este registro; único por diseño (no duplicar). */
    @Column(name = "solicitud_id", nullable = false, unique = true)
    private Long solicitudId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jugador_id", nullable = false)
    private Usuario jugador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipo_origen_id", nullable = false)
    private Equipo equipoOrigen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipo_destino_id", nullable = false)
    private Equipo equipoDestino;

    @Column(name = "rol_asignado", nullable = false, length = 30)
    private String rolAsignado;

    /** Usuario cuya respuesta completó la transferencia (RF-14: trazabilidad). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_id", nullable = false)
    private Usuario responsable;

    @Column(name = "fecha_transferencia", nullable = false)
    @Builder.Default
    private LocalDateTime fechaTransferencia = LocalDateTime.now();
}