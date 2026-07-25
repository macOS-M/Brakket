package com.coffeecommits.brakket.tournament.model;

import com.coffeecommits.brakket.team.model.Equipo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "inscripcion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "torneo_id", nullable = false)
    private Torneo torneo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipo_id", nullable = false)
    private Equipo equipo;

    @Column(name = "estado", nullable = false, length = 40)
    private String estado;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDate fechaSolicitud;

    /**
     * Gamertag del capitán al inscribirse: identifica al equipo dentro del
     * juego (no hay API que verifique identidades; es dato declarado).
     * Nullable solo por inscripciones previas a V26.
     */
    @Column(name = "usuario_en_juego", length = 100)
    private String usuarioEnJuego;
}
