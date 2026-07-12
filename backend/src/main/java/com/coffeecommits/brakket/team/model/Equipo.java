package com.coffeecommits.brakket.team.model;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.game.model.Juego;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "equipo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Equipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 120, unique = true)
    private String nombre;

    @Column(name = "logo", length = 500)
    private String logo;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "capitan_id", nullable = false)
    private Usuario capitan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "juego_id")
    private Juego juego;

    /**
     * Ciclo de vida del equipo (RF-02/RF-03): ACTIVO, BLOQUEADO (disputa o
     * revisión administrativa activa), DISUELTO.
     */
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private String estado = "ACTIVO";

    /** Visibilidad del perfil público del equipo: PUBLIC / PRIVATE. */
    @Column(name = "estado_privacidad", nullable = false, length = 20)
    @Builder.Default
    private String estadoPrivacidad = "PUBLIC";

    /**
     * Control de concurrencia optimista. JPA la gestiona automáticamente:
     * incrementa en cada UPDATE y lanza OptimisticLockingFailureException
     * si la fila fue modificada por otra transacción entre medio.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}