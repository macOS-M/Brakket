package com.coffeecommits.brakket.twitch.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "incidente_integracion_twitch")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IncidenteIntegracionTwitch {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "canal_id")
    private CanalOficialTwitch canal;
    @Column(nullable = false, length = 60)
    private String tipo;
    @Column(nullable = false, length = 500)
    private String detalle;
    @Column(name = "ocurrido_en", nullable = false)
    private LocalDateTime ocurridoEn;
}

