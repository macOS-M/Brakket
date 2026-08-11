package com.coffeecommits.brakket.team.model;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.game.model.Juego;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

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

    /** Banner de portada del perfil (V30, referencia Challenger Mode). */
    @Column(name = "banner_url", length = 500)
    private String bannerUrl;

    @Column(name = "sitio_web", length = 500)
    private String sitioWeb;

    /** Video de presentación del equipo (enlace a YouTube/Twitch/etc.). */
    @Column(name = "video_url", length = 500)
    private String videoUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "capitan_id", nullable = false)
    private Usuario capitan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "juego_id")
    private Juego juego;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "equipo_juego",
            joinColumns = @JoinColumn(name = "equipo_id"),
            inverseJoinColumns = @JoinColumn(name = "juego_id"))
    @Builder.Default
    private Set<Juego> juegos = new LinkedHashSet<>();

    /**
     * Ciclo de vida del equipo (RF-02/RF-03): ACTIVO, BLOQUEADO (disputa o
     * revisión administrativa activa), DISUELTO. La disolución es lógica —
     * el equipo nunca se borra de la base. El perfil público (RF-04) lo usa
     * para mostrar el aviso de equipo disuelto.
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

    @Column(name = "fecha_disolucion")
    private LocalDateTime fechaDisolucion;

    @Column(name = "motivo_disolucion", length = 500)
    private String motivoDisolucion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disuelto_por")
    private Usuario disueltoPor;
}
