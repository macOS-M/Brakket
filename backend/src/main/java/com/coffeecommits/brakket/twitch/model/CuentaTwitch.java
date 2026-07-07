package com.coffeecommits.brakket.twitch.model;

import com.coffeecommits.brakket.team.model.Equipo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cuenta_twitch")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CuentaTwitch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipo_id", nullable = false)
    private Equipo equipo;

    @Column(name = "canal_id", nullable = false, length = 120)
    private String canalId;

    @Column(name = "token_acceso", nullable = false, length = 500)
    private String tokenAcceso;
}
