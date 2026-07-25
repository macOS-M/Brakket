package com.coffeecommits.brakket.twitch.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "canal_oficial_twitch")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CanalOficialTwitch {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "twitch_usuario_id", length = 120)
    private String twitchUsuarioId;
    @Column(name = "login_canal", nullable = false, length = 120)
    private String loginCanal;
    @Column(name = "nombre_mostrado", length = 150)
    private String nombreMostrado;
    @Column(name = "url_canal", nullable = false, length = 300)
    private String urlCanal;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoIntegracionTwitch estado;
    @Column(nullable = false)
    private boolean activo;
    @Column(name = "ultimo_error", length = 500)
    private String ultimoError;
    @Column(name = "ultima_validacion")
    private LocalDateTime ultimaValidacion;
    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;
    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;
}

