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

    // Alcance: exactamente uno de los dos debe estar lleno (liga/torneo).
    // TEMPORADA se retiró del flujo de creación (V66): las temporadas no
    // tienen pantalla propia. Validado en el servicio.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "liga_id")
    private Liga liga;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "temporada_id")
    private Temporada temporada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "torneo_id")
    private Torneo torneo;

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