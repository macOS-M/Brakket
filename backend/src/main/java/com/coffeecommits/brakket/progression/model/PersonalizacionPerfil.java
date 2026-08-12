package com.coffeecommits.brakket.progression.model;

import com.coffeecommits.brakket.auth.model.Usuario;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="personalizacion_perfil")
@IdClass(PersonalizacionPerfilId.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PersonalizacionPerfil {
    @Id @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="usuario_id") private Usuario usuario;
    @Id @Enumerated(EnumType.STRING) @Column(length=20) private TipoPersonalizacion tipo;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="elemento_id", nullable=false) private ElementoPersonalizacion elemento;
}
