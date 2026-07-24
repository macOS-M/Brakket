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
}

