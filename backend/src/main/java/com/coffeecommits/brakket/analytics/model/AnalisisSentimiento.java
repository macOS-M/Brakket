package com.coffeecommits.brakket.analytics.model;

import com.coffeecommits.brakket.twitch.model.MetricaChat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "analisis_sentimiento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalisisSentimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metrica_chat_id", nullable = false)
    private MetricaChat metricaChat;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "clasificacion", nullable = false, length = 40)
    private String clasificacion;

    @Column(name = "puntaje", nullable = false, precision = 5, scale = 2)
    private BigDecimal puntaje;
}
