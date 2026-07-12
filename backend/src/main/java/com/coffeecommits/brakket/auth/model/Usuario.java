package com.coffeecommits.brakket.auth.model;

import com.coffeecommits.brakket.game.model.Juego;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "correo", nullable = false, length = 180, unique = true)
    private String correo;

    @Column(name = "google_id", nullable = false, length = 180, unique = true)
    private String googleId;

    @Column(name = "foto_url", length = 500)
    private String fotoUrl;

    @Column(name = "biografia", columnDefinition = "TEXT")
    private String biografia;

    @Column(name = "redes_sociales", columnDefinition = "TEXT")
    private String redesSociales;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibilidad_perfil", nullable = false, length = 20)
    @Builder.Default
    private VisibilidadPerfil visibilidadPerfil = VisibilidadPerfil.PUBLIC;

    @Column(name = "bloqueado", nullable = false)
    @Builder.Default
    private Boolean bloqueado = false;

    @Column(name = "perfil_completo", nullable = false)
    @Builder.Default
    private Boolean perfilCompleto = false;

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "usuario_juego_preferido",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "juego_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "juego_id"})
    )
    private Set<Juego> juegosPreferidos = new LinkedHashSet<>();
}