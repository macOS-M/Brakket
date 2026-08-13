package com.coffeecommits.brakket.game.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "juego")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Juego {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 120, unique = true)
    private String nombre;

    @Column(name = "genero", nullable = false, length = 80)
    private String genero;

    @Column(name = "activo", nullable = false)
    private Boolean activo;

    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    @Column(name = "descripcion")
    private String descripcion;

    // ----- Ficha enriquecida desde RAWG (V28); todo opcional. -----

    @Column(name = "rawg_slug", length = 200)
    private String rawgSlug;

    @Column(name = "fecha_lanzamiento")
    private LocalDate fechaLanzamiento;

    /** Rating de la comunidad RAWG (0–5). */
    @Column(name = "rating")
    private Double rating;

    @Column(name = "metacritic")
    private Integer metacritic;

    /** Nombres de plataformas separados por " · ". */
    @Column(name = "plataformas", length = 300)
    private String plataformas;

    /** Tags de RAWG separados por coma (Multiplayer, PvP, eSports…). */
    @Column(name = "etiquetas", length = 500)
    private String etiquetas;

    @Column(name = "sitio_web", length = 300)
    private String sitioWeb;

    /** Identificador de YouTube del primer trailer publicado en IGDB. */
    @Column(name = "trailer_id", length = 32)
    private String trailerId;

    /** Evita consultar IGDB en cada arranque cuando un juego no tiene video. */
    @Column(name = "trailer_consultado", nullable = false)
    private boolean trailerConsultado;

    @Convert(converter = StringListConverter.class)
    @Column(name = "capturas")
    private List<String> capturas;
}
