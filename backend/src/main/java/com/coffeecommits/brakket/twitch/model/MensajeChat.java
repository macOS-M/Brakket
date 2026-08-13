package com.coffeecommits.brakket.twitch.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mensaje del chat capturado durante una transmisión (RF-38).
 *
 * <p>Complementa a {@link MetricaChat}, que guarda los agregados por ventana:
 * acá queda el detalle que el requerimiento pide asociar a hora, transmisión y
 * contenido.</p>
 *
 * <p><b>No hay autor.</b> El nick se usa en memoria únicamente para contar
 * usuarios distintos y nunca llega a esta tabla; la restricción vive en el
 * esquema, no en una convención.</p>
 */
@Entity
@Table(name = "mensaje_chat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MensajeChat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transmision_twitch_id", nullable = false)
    private TransmisionTwitch transmisionTwitch;

    @Column(name = "texto", nullable = false, length = 500)
    private String texto;

    /** Hora de llegada del mensaje, no la del cierre de la ventana. */
    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;
}
