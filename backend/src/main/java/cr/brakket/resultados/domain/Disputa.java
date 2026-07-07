package cr.brakket.resultados.domain;

import cr.brakket.auth.domain.Usuario;
import cr.brakket.torneos.domain.Partida;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "disputa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Disputa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partida_id", nullable = false)
    private Partida partida;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "levantada_por", nullable = false)
    private Usuario levantadaPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "arbitro_id")
    private Usuario arbitro;

    @Column(name = "motivo", nullable = false, length = 500)
    private String motivo;

    @Column(name = "evidencia_url", length = 500)
    private String evidenciaUrl;

    @Column(name = "resolucion", length = 1000)
    private String resolucion;

    @Column(name = "sancion", length = 500)
    private String sancion;

    @Column(name = "estado", nullable = false, length = 40)
    private String estado;
}
