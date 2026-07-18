package com.coffeecommits.brakket.game.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "formato_competitivo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormatoCompetitivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String nombre;

    @Column(nullable = false)
    private Boolean activo;
}