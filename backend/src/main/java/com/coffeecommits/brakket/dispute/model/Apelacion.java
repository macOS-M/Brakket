package com.coffeecommits.brakket.dispute.model;

import com.coffeecommits.brakket.auth.model.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "apelacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Apelacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disputa_id", nullable = false)
    private Disputa disputa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comisionado_id")
    private Usuario comisionado;

    @Column(name = "motivo", nullable = false, length = 500)
    private String motivo;

    @Column(name = "decision_final", length = 1000)
    private String decisionFinal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apelada_por_id")
    private Usuario apeladaPor;

    /** 'PENDIENTE' o 'RESUELTA'. */
    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @Column(name = "fecha_creacion", nullable = false)
    private java.time.LocalDateTime fechaCreacion;

    @Column(name = "fecha_resolucion")
    private java.time.LocalDateTime fechaResolucion;
}