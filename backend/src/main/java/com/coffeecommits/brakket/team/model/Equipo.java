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

    // ACTIVO o DISUELTO; RF-03 lo cambia al disolver y RF-05 filtra por él.
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private String estado = "ACTIVO";
}