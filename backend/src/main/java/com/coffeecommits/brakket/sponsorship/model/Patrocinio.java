package com.coffeecommits.brakket.sponsorship.model;

import com.coffeecommits.brakket.league.model.Liga;
import com.coffeecommits.brakket.league.model.Temporada;
import com.coffeecommits.brakket.tournament.model.Torneo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "patrocinio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patrocinio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patrocinador_id", nullable = false)
    private Patrocinador patrocinador;

    // Alcance: exactamente uno de los tres debe estar lleno (liga/temporada/torneo).
    // Validado en el servicio y garantizado en BD por ck_patrocinio_alcance_unico (V54).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "liga_id")
    private Liga liga;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "temporada_id")
    private Temporada temporada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "torneo_id")
    private Torneo torneo;

    // Cubre también "tipo de asociación" del ERS (decisión de equipo: mismo campo).
    @Column(name = "nivel", nullable = false, length = 60)
    private String nivel;

    @Column(name = "condiciones", length = 500)
    private String condiciones;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "estado", nullable = false, length = 40)
    private String estado;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}