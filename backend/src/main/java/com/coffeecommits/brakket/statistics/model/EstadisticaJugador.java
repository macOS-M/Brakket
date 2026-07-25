package com.coffeecommits.brakket.statistics.model;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.game.model.Juego;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "estadistica_jugador")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstadisticaJugador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "juego_id", nullable = false)
    private Juego juego;

    @Column(name = "victorias", nullable = false)
    private Integer victorias;

    @Column(name = "derrotas", nullable = false)
    private Integer derrotas;

    @Column(name = "torneos_jugados", nullable = false)
    private Integer torneosJugados;
}
