package com.coffeecommits.brakket.tournament.model;

import com.coffeecommits.brakket.league.model.Temporada;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

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
    @JoinColumn(name = "temporada_id", nullable = false)
    private Temporada temporada;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "formato", nullable = false, length = 60)
    private String formato;

    @Column(name = "max_equipos", nullable = false)
    private Integer maxEquipos;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "estado", nullable = false, length = 40)
    private String estado;
}
