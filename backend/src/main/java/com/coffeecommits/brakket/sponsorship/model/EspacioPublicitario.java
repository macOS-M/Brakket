package com.coffeecommits.brakket.sponsorship.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "espacio_publicitario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EspacioPublicitario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // El espacio cuelga de un patrocinio y hereda su vigencia (fechaInicio/
    // fechaFin de Patrocinio). No se duplica vigencia aqui por diseno (V55).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patrocinio_id", nullable = false)
    private Patrocinio patrocinio;

    // Enum cerrado a nivel de BD (ck_espacio_publicitario_ubicacion, V55).
    // Validado tambien en el servicio contra UBICACIONES_VALIDAS.
    @Column(name = "ubicacion", nullable = false, length = 30)
    private String ubicacion;

    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    @Column(name = "enlace_url", length = 500)
    private String enlaceUrl;

    @Column(name = "estado", nullable = false, length = 40)
    private String estado;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}