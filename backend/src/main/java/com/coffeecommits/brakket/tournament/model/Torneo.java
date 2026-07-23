package com.coffeecommits.brakket.tournament.model;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.game.model.Juego;
import com.coffeecommits.brakket.league.model.Temporada;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Torneo con modelo abierto de organizadores (RF-24, decisión de diseño
 * inspirada en Challenger Mode): cuelga siempre de un juego; la temporada
 * de liga es opcional (torneo comunitario cuando falta). Quien lo crea es
 * su organizador y único gestor junto con los ADMIN.
 */
@Entity
@Table(name = "torneo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Torneo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "juego_id", nullable = false)
    private Juego juego;

    /** Solo cuando el torneo pertenece a una liga (trazabilidad RF-24). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "temporada_id")
    private Temporada temporada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizador_id", nullable = false)
    private Usuario organizador;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "descripcion", length = 1000)
    private String descripcion;

    @Column(name = "formato", nullable = false, length = 60)
    private String formato;

    /** Jugadores por lado: el "5" de un 5v5. */
    @Column(name = "tamano_equipo", nullable = false)
    private Integer tamanoEquipo;

    /** Cupo de equipos del torneo. */
    @Column(name = "max_equipos", nullable = false)
    private Integer maxEquipos;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 40)
    private EstadoTorneo estado;

    /** Público: se lista y acepta inscripciones; privado: solo lo ve su organizador. */
    @Column(name = "publico", nullable = false)
    private Boolean publico;

    /** Premio personalizado (texto libre del organizador). */
    @Column(name = "premio", length = 200)
    private String premio;
}
