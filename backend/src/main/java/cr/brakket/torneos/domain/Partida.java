package cr.brakket.torneos.domain;

import cr.brakket.equipos.domain.Equipo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "partida")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Partida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "torneo_id", nullable = false)
    private Torneo torneo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipo_a_id", nullable = false)
    private Equipo equipoA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipo_b_id", nullable = false)
    private Equipo equipoB;

    @Column(name = "ronda", nullable = false)
    private Integer ronda;

    @Column(name = "marcador_a")
    private Integer marcadorA;

    @Column(name = "marcador_b")
    private Integer marcadorB;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ganador_id")
    private Equipo ganador;

    @Column(name = "estado", nullable = false, length = 40)
    private String estado;
}
