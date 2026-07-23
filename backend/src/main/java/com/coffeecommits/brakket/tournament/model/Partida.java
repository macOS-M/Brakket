package com.coffeecommits.brakket.tournament.model;

import com.coffeecommits.brakket.team.model.Equipo;
import jakarta.persistence.*;
import org.hibernate.annotations.DynamicUpdate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Enfrentamiento del bracket (RF-26/27). Los equipos pueden ser null
 * mientras las rondas previas no terminan; una partida con un solo equipo
 * es un bye y se finaliza sola al generar la llave.
 *
 * <p>La lobby (nombre + clave) es el puente con el juego real: Brakket no
 * se conecta al juego, genera las credenciales de la partida privada que
 * ambos capitanes usan dentro del juego.</p>
 */
@Entity
@Table(name = "partida")
@DynamicUpdate // dos semifinales concurrentes escriben slots distintos de la final sin pisarse
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Partida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "torneo_id", nullable = false)
    private Torneo torneo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipo_a_id")
    private Equipo equipoA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipo_b_id")
    private Equipo equipoB;

    /** Ronda 1 = primera; la final es la ronda más alta. */
    @Column(name = "ronda", nullable = false)
    private Integer ronda;

    /** Posición dentro de la ronda (0-based); define el slot de avance. */
    @Column(name = "orden", nullable = false)
    private Integer orden;

    @Column(name = "marcador_a")
    private Integer marcadorA;

    @Column(name = "marcador_b")
    private Integer marcadorB;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ganador_id")
    private Equipo ganador;

    /** Equipo cuyo capitán reportó el marcador (el rival confirma). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reportado_por_equipo_id")
    private Equipo reportadoPor;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 40)
    private EstadoPartida estado;

    @Column(name = "lobby_nombre", length = 80)
    private String lobbyNombre;

    @Column(name = "lobby_clave", length = 40)
    private String lobbyClave;

    /** Enlace de avance: el ganador ocupa un slot de esta partida. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "siguiente_partida_id")
    private Partida siguiente;

    /** Bye: solo hay un equipo y no habrá rival en esta ronda. */
    public boolean esBye() {
        return equipoA != null && equipoB == null && estado == EstadoPartida.FINALIZADA;
    }
}
