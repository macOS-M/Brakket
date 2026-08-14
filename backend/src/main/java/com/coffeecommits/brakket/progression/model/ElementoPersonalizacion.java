package com.coffeecommits.brakket.progression.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "elemento_personalizacion")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ElementoPersonalizacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 120) private String nombre;
    @Column(nullable = false, length = 500) private String descripcion;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private TipoPersonalizacion tipo;
    @Column(name = "costo_puntos", nullable = false) private Integer costoPuntos;
    @Column(nullable = false) @Builder.Default private Boolean activo = true;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "logro_requerido_id") private Logro logroRequerido;
}
