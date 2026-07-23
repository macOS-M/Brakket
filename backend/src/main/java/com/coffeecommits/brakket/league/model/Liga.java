package com.coffeecommits.brakket.league.model;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.game.model.Juego;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "liga")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Liga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "juego_id", nullable = false)
    private Juego juego;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comisionado_id", nullable = false)
    private Usuario comisionado;

    @Column(name = "descripcion", length = 1000)
    private String descripcion;

    @Column(name = "reglas", length = 4000)
    private String reglas;

    /** Foto propia de la liga; si falta, el frontend usa el arte del juego. */
    @Column(name = "foto_url", length = 500)
    private String fotoUrl;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;
}
