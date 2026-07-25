package com.coffeecommits.brakket.team.model;

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
@Table(name = "miembro_equipo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MiembroEquipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipo_id", nullable = false)
    private Equipo equipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "estado", nullable = false, length = 40)
    private String estado;

    @Column(name = "fecha_union", nullable = false)
    private LocalDate fechaUnion;

    @Column(name = "rol", nullable = false, length = 30)
    private String rol;

    // RF-10: trazabilidad de la baja cuando el integrante es expulsado.
    @Column(name = "fecha_baja")
    private LocalDateTime fechaBaja;

    @Column(name = "causa_baja", length = 500)
    private String causaBaja;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_baja_id")
    private Usuario responsableBaja;
}