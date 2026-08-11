package com.coffeecommits.brakket.sponsorship.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "patrocinador")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patrocinador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "logo", length = 500)
    private String logo;

    @Column(name = "contacto", nullable = false, length = 180)
    private String contacto;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    // RF-44 (V53): vincula el patrocinador con la cuenta que inicia sesion
    // como PATROCINADOR, para resolver "mi" panel comercial sin recibir
    // el id por parametro. Nullable: un patrocinador puede existir antes
    // de que se le asocie una cuenta real (ver migracion V53).
    @Column(name = "usuario_id")
    private Long usuarioId;
}