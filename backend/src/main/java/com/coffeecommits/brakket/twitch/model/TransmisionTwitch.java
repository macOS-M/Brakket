package com.coffeecommits.brakket.twitch.model;

import com.coffeecommits.brakket.tournament.model.Partida;
import com.coffeecommits.brakket.tournament.model.Torneo;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transmision_twitch")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TransmisionTwitch {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "canal_id", nullable = false)
    private CanalOficialTwitch canal;
    @Column(name = "twitch_stream_id", length = 120)
    private String twitchStreamId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "torneo_id")
    private Torneo torneo;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "partida_id")
    private Partida partida;
    @Column(nullable = false, length = 30)
    private String estado;
    @Column(name = "iniciada_en")
    private LocalDateTime iniciadaEn;
    @Column(name = "finalizada_en")
    private LocalDateTime finalizadaEn;
    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;
    // RF-35 (V35): registro multiplataforma. Los @Builder.Default cubren los
    // NOT NULL de la migración cuando el builder no los establece.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PlataformaTransmision plataforma = PlataformaTransmision.TWITCH;
    /** Handle del canal; si es null se resuelve desde el canal oficial asociado. */
    @Column(name = "login_canal", length = 120)
    private String loginCanal;
    @Column(nullable = false)
    @Builder.Default
    private boolean destacada = false;
    @Column(nullable = false)
    @Builder.Default
    private boolean activa = true;
    @Column(name = "creada_por")
    private Long creadaPorId;
    @Column(name = "verificada_en")
    private LocalDateTime verificadaEn;
}

