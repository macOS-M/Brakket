package com.coffeecommits.brakket.game.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "perfil_competitivo_juego")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerfilCompetitivoJuego {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "juego_id", nullable = false, unique = true)
    private Juego juego;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModalidadCompetitiva modalidad;

    @Column(nullable = false)
    private Integer plantillaMinima;

    @Column(nullable = false)
    private Integer plantillaMaxima;

    @Column(nullable = false)
    private Boolean activo;

    @ManyToMany
    @JoinTable(
            name = "perfil_formato",
            joinColumns = @JoinColumn(name = "perfil_id"),
            inverseJoinColumns = @JoinColumn(name = "formato_id")
    )
    private List<FormatoCompetitivo> formatosCompatibles;

    @ManyToMany
    @JoinTable(
            name = "perfil_estadistica",
            joinColumns = @JoinColumn(name = "perfil_id"),
            inverseJoinColumns = @JoinColumn(name = "estadistica_id")
    )
    private List<EstadisticaJuego> estadisticas;
}