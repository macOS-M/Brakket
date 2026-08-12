package com.coffeecommits.brakket.progression.model;

import lombok.*;
import java.io.Serializable;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class PersonalizacionPerfilId implements Serializable {
    private Long usuario;
    private TipoPersonalizacion tipo;
}
