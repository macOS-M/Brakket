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

    // V36: el muestreo real (RF-36) ancla en la transmisión; la cuenta por
    // equipo quedó opcional. El CHECK de BD exige al menos una de las dos.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_twitch_id")
    private CuentaTwitch cuentaTwitch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transmision_twitch_id")
    private TransmisionTwitch transmisionTwitch;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "espectadores", nullable = false)
    private Integer espectadores;

    /** RF-36: distinguir datos reales de simulados (contexto académico). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OrigenMetrica origen = OrigenMetrica.REAL;
}
