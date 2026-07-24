package com.coffeecommits.brakket.sponsorship.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "patrocinador_enlace")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatrocinadorEnlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patrocinador_id", nullable = false)
    private Patrocinador patrocinador;

    @Column(name = "url", nullable = false, length = 300)
    private String url;
}
