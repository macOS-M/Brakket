package com.coffeecommits.brakket.league.model;

import com.coffeecommits.brakket.game.model.FormatoCompetitivo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "temporada")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Temporada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "liga_id", nullable = false)
    private Liga liga;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "reglas", nullable = false, columnDefinition = "TEXT")
    private String reglas;

    @Column(name = "estado", nullable = false, length = 30)
    private String estado;

    @Column(name = "cupo_equipos", nullable = false)
    private Integer cupoEquipos;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "formato_id")
    private FormatoCompetitivo formato;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
