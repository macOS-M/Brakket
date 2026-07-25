package com.coffeecommits.brakket.auth.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "rol")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_rol", nullable = false, length = 50, unique = true)
    private String nombreRol;

    /**
     * Jerarquía del rol: 1 = mayor autoridad (administrador).
     * Se usa para validar que un usuario no asigne roles con más
     * privilegios que el suyo (criterio RF-19).
     */
    @Column(name = "nivel", nullable = false)
    private Integer nivel;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "rol_permiso",
            joinColumns = @JoinColumn(name = "rol_id"),
            inverseJoinColumns = @JoinColumn(name = "permiso_id")
    )
    @Builder.Default
    private Set<Permiso> permisos = new HashSet<>();
}