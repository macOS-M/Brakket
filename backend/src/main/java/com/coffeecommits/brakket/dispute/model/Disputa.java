package com.coffeecommits.brakket.dispute.model;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.tournament.model.Partida;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "disputa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Disputa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partida_id", nullable = false)
    private Partida partida;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "levantada_por", nullable = false)
    private Usuario levantadaPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "arbitro_id")
    private Usuario arbitro;

    @Column(name = "motivo", nullable = false, length = 500)
    private String motivo;

    @Column(name = "evidencia_url", length = 500)
    private String evidenciaUrl;

    @Column(name = "resolucion", length = 1000)
    private String resolucion;

    @Column(name = "sancion", length = 500)
    private String sancion;

    @Column(name = "estado", nullable = false, length = 40)
    private String estado;

    /** Texto largo del reclamo; el motivo es la categoría corta. */
    @Column(name = "descripcion", length = 1000)
    private String descripcion;

    @Column(name = "fecha_creacion", nullable = false)
    private java.time.LocalDateTime fechaCreacion;

    /** 'MANTENER' o 'REVERTIR', lo que decidió quien resolvió. */
    @Column(name = "decision", length = 20)
    private String decision;

    @Column(name = "justificacion_resolucion", length = 1000)
    private String justificacionResolucion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resuelta_por_id")
    private Usuario resueltaPor;

    @Column(name = "fecha_resolucion")
    private java.time.LocalDateTime fechaResolucion;
}