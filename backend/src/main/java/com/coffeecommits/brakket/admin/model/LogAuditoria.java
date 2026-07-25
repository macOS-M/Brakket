package com.coffeecommits.brakket.admin.model;

import com.coffeecommits.brakket.auth.model.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "log_auditoria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "accion", nullable = false, length = 180)
    private String accion;

    @Column(name = "entidad", nullable = false, length = 120)
    private String entidad;

    @Column(name = "entidad_id", nullable = false)
    private Long entidadId;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;
}
