package com.coffeecommits.brakket.progression.model;

import com.coffeecommits.brakket.auth.model.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "logro_jugador")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogroJugador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logro_id", nullable = false)
    private Logro logro;

    @Column(name = "fecha_desbloqueo", nullable = false)
    private LocalDate fechaDesbloqueo;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "referencia_sistema", length = 180)
    private String referenciaSistema;

    @Column(name = "fecha_reversion")
    private LocalDateTime fechaReversion;
}
