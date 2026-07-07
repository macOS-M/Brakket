package cr.brakket.ligas.domain;

import cr.brakket.auth.domain.Usuario;
import cr.brakket.juegos.domain.Juego;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "liga")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Liga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "juego_id", nullable = false)
    private Juego juego;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comisionado_id", nullable = false)
    private Usuario comisionado;
}
