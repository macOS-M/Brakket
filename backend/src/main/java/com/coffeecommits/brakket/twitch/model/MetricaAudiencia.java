package com.coffeecommits.brakket.twitch.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "metrica_audiencia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetricaAudiencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_twitch_id", nullable = false)
    private CuentaTwitch cuentaTwitch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transmision_twitch_id")
    private TransmisionTwitch transmisionTwitch;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "espectadores", nullable = false)
    private Integer espectadores;
}
