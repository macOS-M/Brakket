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
 * Historial de cambios de rol de un integrante dentro de un equipo (RF-09).
 */
@Entity
@Table(name = "equipo_rol_historial")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipoRolHistorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipo_id", nullable = false)
    private Equipo equipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "rol_anterior", nullable = false, length = 30)
    private String rolAnterior;

    @Column(name = "rol_nuevo", nullable = false, length = 30)
    private String rolNuevo;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_id", nullable = false)
    private Usuario responsable;
}