package com.coffeecommits.brakket.team.model;

import com.coffeecommits.brakket.auth.model.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Solicitud de unión a un equipo: el flujo inverso a la invitación. Un
 * jugador la crea sobre un equipo ajeno y la responde el capitán.
 */
@Entity
@Table(name = "solicitud_union")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudUnion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipo_id", nullable = false)
    private Equipo equipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jugador_id", nullable = false)
    private Usuario jugador;

    @Column(name = "mensaje", length = 300)
    private String mensaje;

    /** PENDIENTE / ACEPTADA / RECHAZADA. */
    @Column(name = "estado", nullable = false, length = 40)
    private String estado;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta;
}
