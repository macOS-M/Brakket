package com.coffeecommits.brakket.notification.model;

import com.coffeecommits.brakket.auth.model.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "tipo", nullable = false, length = 60)
    private String tipo;

    @Column(name = "mensaje", nullable = false, length = 500)
    private String mensaje;

    @Column(name = "entidad", length = 120)
    private String entidad;

    @Column(name = "entidad_id")
    private Long entidadId;

    @Column(name = "leida", nullable = false)
    private Boolean leida;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "estado_entrega", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EstadoEntrega estadoEntrega = EstadoEntrega.DISPONIBLE;

    @Column(name = "origen", nullable = false, length = 120)
    private String origen;

    @Column(name = "eliminada_bandeja", nullable = false)
    @Builder.Default
    private Boolean eliminadaBandeja = false;

    @PrePersist
    void completarDatosDeEntrega() {
        if (estadoEntrega == null) {
            estadoEntrega = EstadoEntrega.DISPONIBLE;
        }
        if (eliminadaBandeja == null) {
            eliminadaBandeja = false;
        }
        if (origen == null || origen.isBlank()) {
            origen = entidad == null || entidad.isBlank() ? "Sistema" : entidad;
        }
    }
}
