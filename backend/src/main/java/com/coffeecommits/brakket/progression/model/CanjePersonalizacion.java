package com.coffeecommits.brakket.progression.model;

import com.coffeecommits.brakket.auth.model.Usuario;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "canje_personalizacion", uniqueConstraints = @UniqueConstraint(columnNames={"usuario_id","elemento_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CanjePersonalizacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="usuario_id", nullable=false) private Usuario usuario;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="elemento_id", nullable=false) private ElementoPersonalizacion elemento;
    @Column(name="costo_puntos", nullable=false) private Integer costoPuntos;
    @Column(name="fecha_canje", nullable=false) private LocalDateTime fechaCanje;
}
