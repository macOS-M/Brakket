package com.coffeecommits.brakket.twitch.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "metrica_chat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetricaChat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nullable desde V37 (RF-39): las muestras de chat del análisis de
    // sentimiento cuelgan de la transmisión, no de una cuenta_twitch de equipo.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_twitch_id")
    private CuentaTwitch cuentaTwitch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transmision_twitch_id")
    private TransmisionTwitch transmisionTwitch;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "mensajes_por_minuto", nullable = false)
    private Integer mensajesPorMinuto;

    @Column(name = "usuarios_activos", nullable = false)
    private Integer usuariosActivos;
}
